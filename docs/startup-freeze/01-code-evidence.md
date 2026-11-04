# 01｜源码证据与待证伪假设

审计基线：f91059cbffe453266b74ef06188793e5e82ce780。以下文件定位均指该提交，未经实际设备运行证明的表述标记为「假设」或「风险」，不得当作已证实根因。

## E01 — 截图中的骨架屏可准确映射到首页的加载状态（已证实代码路径）

- HomePage.kt:339-378：初始 isLoading=true、forums 为空、error=null，并分别 collectPartialAsState；首页未直接持有独立加载计时。
- HomePage.kt:438-470：StateScreen 在 isLoading 时使用 HomePageSkeletonScreen。
- HomePage.kt:646-700：HomePageSkeletonScreen 生成两组灰色占位，与截图相符。
- states/States.kt:63-97：仅 isEmpty=true 且 isLoading=true 时使用 loadingScreen；当 isEmpty=false 时展示已有 content，而不管后台是否正在请求。

含义：截图可确认当时走到骨架屏分支；无法仅由截图确认网络、数据库、首次事件或 UI 线程哪个环节有问题。

## E02 — 首次 Refresh 发出时存在需测试的调度竞态（风险，未证实发生）

- arch/BaseViewModel.kt:37-68：intentFlow 为无 replay 的 MutableSharedFlow；send(intent) 在 viewModelScope.launch 中 emit。
- arch/BaseViewModel.kt:46-60：uiState 使用 SharingStarted.Eagerly，因此订阅通常在 ViewModel 构建后很快建立（这降低但不能逻辑上消除事件顺序问题）。
- arch/Extensions.kt:176-194：pageViewModel 的初始 intent 在 LaunchedEffect 中启动异步发送；initialized 在真正完成 UI 加载前即被置 true。
- HomePage.kt:339-342：首次 intent 仅 HomeUiIntent.Refresh。
- HomeViewModel.kt:268-275：最初 HomeUiState.isLoading=true。

可验证的失败路径：若页面收到初始化状态但 Refresh 事件没有到达 reducer，isLoading 可能永远保持 true，且没有 Failure。不能简单宣布该竞态就是本次故障：先收集 ViewModel 的 send Refresh / partialChange Start 日志，并写人工控制订阅时序的单元测试。

## E03 — 本地历史与在线关注吧请求被 zip 绑定（已证实设计；是否挂起待日志）

- HomeViewModel.kt:74-99：HistoryUtil.getFlow(TYPE_FORUM, 0).zip(TiebaApi.getInstance().forumRecommendNewFlow())；两个数据源都产生值后才 emit 成功。
- utils/HistoryUtil.kt:45-54 与 utils/extension/LitePalFlowExt.kt:26-31：历史来自 LitePal 的 find()，且使用 Dispatchers.IO。
- api/interfaces/impls/MixedTiebaApiImpl.kt:1020-1038：forumRecommendNewFlow 委托 OFFICIAL_PROTOBUF_TIEBA_API.forumRecommendFlow。
- api/retrofit/RetrofitTiebaApi.kt:226-252：所用客户端的 baseUrl 为 https://tiebac.baidu.com/ 。
- api/retrofit/interfaces/OfficialProtobufTiebaApi.kt:47-52：路径为 /c/f/forum/forumrecommend?cmd=303011（POST）。

含义：正常互联网连接不能证明该特定域名、协议和认证请求正常。任一数据源长期没有首项，zip 不会产生 Success。异常通常会通过下游 catch 变成 Failure；不能将所有网络错误等同于永久加载。若任何上游不抛错但空完成，zip 也可能无 Success/Failure 且留在 Start，需要测试空 Flow 情况；当前数据库 findFlow 正常设计上会发射一个 List，即使为空。

## E04 — 缺少整体加载时间上界（已证实）

- api/retrofit/RetrofitTiebaApi.kt:54-57：READ/CONNECT/WRITE_TIMEOUT 各为 60 秒。
- api/retrofit/RetrofitTiebaApi.kt:363-415：设置上述三个 timeout，但未设置 OkHttp callTimeout。
- HomeViewModel.kt:74-99：zip 加载链条上没有 withTimeout 或其他业务截止时间。
- HomeViewModel.kt:178-205：Success/Failure 都会使 isLoading=false，Start 设为 true。

含义：普通读取超时不是整个 HTTP 调用的硬截止时间；多阶段请求、重复进度或其他挂起可能延长用户等待。记录真实耗时后再确定截止值和超时回退策略，避免臆定网络故障。

## E05 — 「所有按钮不能点」无法由首页加载本身直接解释（已证实架构独立性）

- HomePage.kt:407-435：顶部搜索跳转、签到服务、列表布局切换是独立 IconButton/ActionItem 回调；未以 isLoading 禁用。
- ui/widgets/compose/Toolbar.kt:191-205：ActionItem 使用 IconButton(onClick)，没有绑定首页加载状态。
- ui/page/main/NavigationComponents.kt:373-428：BottomNavigationItem 有独立 onClick，并调用 onChangePosition/onReselected。
- ui/page/main/MainPage.kt:246-283：底部选项借助 coroutineScope.launch { pagerState.scrollToPage(...) } 导航。
- ui/widgets/compose/states/States.kt:63-97：骨架屏位于主页内容容器内，不是覆盖整个 Activity 的触摸屏障。

结论：如果搜索和底栏完全无响应，优先检查 UI 主线程是否 ANR、窗口覆盖或输入事件被拦截，不要仅修改 API 或状态机就宣称触控问题解决。

## E06 — 启动/恢复期间值得测量的主线程路径（假设，尚无 ANR traces）

- App.kt:85-115：Application.onCreate 调用 ThemeUtil.selfHealTheme、LitePal.initialize、AccountUtil.init、Config.init、AppIconUtil.setIcon 等同步初始化；AccountUtil.init 会从 LitePal 加载已保存账号。
- MainActivityV2.kt:267-335：onStart 注册 receiver、启动 NotifyJobService 并安排 JobScheduler；onCreateContent 调用 fetchAccount、initAutoSign；fetchAccount 的网络流有 flowOn(Dispatchers.IO)，不是证实 UI 阻塞的依据。
- components/ClipBoardLinkDetector.kt:69-134：Activity 启动后通过 decorView.post 在主线程检查/匹配剪贴板内容，并可能开始网络预览。
- components/OAIDGetter.kt:16-25：onActivityResumed 调用设备标识相关 SDK。
- ui/widgets/compose/dialogs/AnyPopDialog.kt:105-170：弹窗使用独立全屏 Dialog 窗口、覆盖整屏的 Spacer 及手势检测；弹窗窗口异常滞留时有拦截触摸的可能。
- ui/widgets/compose/Dialogs.kt:318-380：Dialog 的 show / dismiss 与动画状态解耦。
- MainActivityV2.kt:355-380、430-460：启动阶段可能出现剪贴板预览弹窗或自动签到提醒。
- MainActivityV2.kt:480-530：应用采用 ModalBottomSheetLayout；状态或 scrim 异常时也需排除输入覆盖路径。

反证/限制：提供的截图没有明显暗色 scrim 或可见弹窗，不能声称存在透明弹窗 bug；截图已显示主页骨架，说明至少首帧能够绘制，也不能直接把 Application 启动同步操作认定为当前卡死根因。

## E07 — 缺少本问题定向回归测试（已证实）

app/src/test 当前覆盖主题、控件、刷新节奏、回复关闭等，未发现 HomeViewModel 首次订阅/超时、首页故障恢复、导航可点击或 ANR 输入测试。这是测试覆盖缺口，不等于已有功能必定有 bug。

## 排除误判的证据树

1. 缺少日志 send Refresh：检查初始 LaunchedEffect 是否运行、生命周期与 UI 主线程。
2. 有 send Refresh、缺少 partialChange Start：检查 MutableSharedFlow 订阅/收集/取消以及 ViewModel 状态机。
3. 有 Start，无 Success/Failure：检查 zip 两边、Retrofit 挂起、LitePal 查询和取消路径；记录每个阶段时钟。
4. 有 Failure 而 UI 仍显示骨架：检查 reducer/collectPartialAsState/StateScreen 组合状态及 UI 是否停止重组。
5. 有 Success 而 UI 仍显示骨架：检查状态发布、日志是否属于当前 ViewModel 实例，以及 Compose 是否停止绘制。
6. Search/Bottom 无法响应：先查 ANR main thread、输入焦点和 Dialog/BottomSheet 的独立窗口；不要默认它与 E02/E03 是同一根因。

## 结论

截至基线审计，能确定的不是「唯一根因」，而是至少两类待分别诊断的故障：A. 网络/状态机中的永久等待风险；B. 全局输入失效。需要真机时间线、请求结果、当前窗口及 ANR 栈支持最终定点修复。