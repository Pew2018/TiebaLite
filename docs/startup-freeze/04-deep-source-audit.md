# 04｜启动卡死与首页永久骨架：定点源码审计（2026-11-04）

## 审计结论 / 范围

工作分支：fix/startup-freeze-loading；审计起始 HEAD：940260400f814701689c71e07dd222bbab8f9301；应用源码基线与 main 相同：f91059cbffe453266b74ef06188793e5e82ce780。此轮为静态审计，没有接入真机，也没有修改业务源码、运行 Android 单元/仪器测试或验证 APK；以下“确认”只表示代码路径或逻辑缺陷能够从源码直接证明，不表示已复现用户设备故障。

用户已报告：曾经正常使用，之后启动显示首页灰色骨架内容，网络可用、清除缓存无效，且顶部工具栏、底部导航等控件也不响应。截图中看得到整个主页界面及占位符，但静态截图不能证实触摸、等待时长及进程状态。

**总体判断**：至少存在两条不同的失效路径。A 是初始刷新事件或异步数据流停在 Loading；B 是主线程或输入窗口失效导致所有按钮不响应。源码已确认若干可以进入这些路径的薄弱点，其中“同步 DataStore 操作运行在主线程”对 B 最有解释力；缺少 ANR 栈、网络耗时和 APK SHA，尚不能宣称已经查明本次真机事故的唯一根因。

## 审计覆盖

- 生命周期/启动：App.kt、MainActivityV2.kt、activities/BaseActivity.kt、arch/BaseComposeActivity.kt、components/ClipBoardLinkDetector.kt、components/OAIDGetter.kt。
- UI：ui/page/main/MainPage.kt、NavigationComponents.kt、home/HomePage.kt、ui/widgets/compose/states/States.kt、PullToRefresh.kt、Toolbar.kt、Dialogs.kt、dialogs/AnyPopDialog.kt、LazyLoad.kt。
- 状态：arch/BaseViewModel.kt、arch/Extensions.kt、home/HomeViewModel.kt。
- 数据：utils/HistoryUtil.kt、utils/extension/LitePalFlowExt.kt、utils/AccountUtil.kt、DataStore.kt、utils/ThemeUtil.kt、ui/common/theme/compose/Theme.kt。
- 网络：api/interfaces/impls/MixedTiebaApiImpl.kt、api/retrofit/RetrofitTiebaApi.kt、api/retrofit/interfaces/OfficialProtobufTiebaApi.kt、api/retrofit/adapter/FlowCallAdapter.kt、api/retrofit/interceptors/ProtoFailureResponseInterceptor.kt。
- 既有测试目录 app/src/test 和 app/src/androidTest。没有全仓库逐行审查、设备 Perfetto/ANR 栈或真实服务器流量。

## F01｜UI 主线程上的同步 DataStore：已确认阻塞机制，实际事故关联待验证（P0）

**源码锚点**

1. App.kt:85-115：Application.onCreate 在主线程调用 ThemeUtil.selfHealTheme()。
2. utils/ThemeUtil.kt:48-53：themeState 延迟初始化时，App.isInitialized=true 还会调用 selfHealTheme()；通常冷启动可能触发第二次同步事务。
3. utils/ThemeUtil.kt:78-115：selfHealTheme() / markThemeApplied() 使用 runBlocking { dataStore.edit { ... } }；没有针对主线程等待的业务截止时间。
4. ui/common/theme/compose/Theme.kt:600-632：TiebaLiteTheme 在 LaunchedEffect(theme) 中先 withFrameNanos 等待真实一帧，再直接调用 ThemeUtil.markThemeApplied(theme)。LaunchedEffect 默认在组合协程上下文执行，此代码来自主界面 Compose 路径，故同步 runBlocking 可以发生于首帧之后。
5. DataStore.kt:134-225：getInt/getString/getStringSet/getBoolean/getFloat/getLong 均通过 runBlocking { data.first { ... } } 同步等待。调用者若位于主线程也会阻塞。
6. utils/ThemeUtil.kt:187、382-399、416-445、568-585：theme/toolbar 偏好与主题切换存在同步读取及同步编辑；App.kt:424-580 的颜色解析会调用部分主题方法。具体哪些颜色分支会调用同步 getter，取决于主题类型，不能从截图颜色断定。
7. ui/common/theme/compose/Theme.kt:392-560：组合期间解析 ExtendedColors，可能触发上述偏好读取路径。

**确认的技术问题**：耗时或受阻的 DataStore 事务/首个数据读取可占住 Android 主线程。主线程不能处理点击、页面调度或下一帧时，用户就会看见上一帧留下的骨架布局，且底栏与顶栏都没响应。该故障模式无需同时存在服务端故障。

**必须保留的限定**：发现 runBlocking 不等于证明当前设备正卡在它上面；正常设备上这些事务也可能很快结束。Application 的第一次 selfHealTheme 发生在首帧前，单独不能解释“首帧已显示之后卡住”；首帧后的 markThemeApplied / 组合中的同步 getter 更能解释该时序，必须由 ANR 栈确认。

**修复方向（未实施）**：将主题存储改为一次异步启动加载、缓存快照与挂起式写入；Compose UI 调用处不执行 runBlocking；仅在核验自愈恢复语义后移除重复启动事务；保证 pending/lastGood 恢复事务原子性及首次坏主题自动回退。

## F02｜ViewModel equals() 破坏对象身份及 hashCode 契约：已确认独立缺陷（P1）

- arch/BaseViewModel.kt:71-80：equals() 比较 javaClass 和 initialized，而不是引用身份；没有对应的 hashCode() 实现。
- 相同类型的两个不同实例只要 initialized 相同即被判为相等，且通常有不同 hashCode；违反 Kotlin/Java 相等性契约。
- arch/Extensions.kt:155-170：存在 DisposableEffect(key1=this)；不恰当的实例相等可能使依赖键比较的生命周期订阅未按预期重新启动（是否发生取决于 Compose 保存/恢复和新实例替换的具体路径）。

结论：代码缺陷已确认，可能造成错误的 ViewModel 订阅复用或生命周期行为。没有证据表明此次首页骨架/触控故障直接由它引发；不能把它写成唯一根因。后续应删除该不必要的 equals 覆盖或建立契约测试，并验证 ViewModel 身份/重建行为。

## F03｜初次 Refresh 事件没有接收确认、可能丢失（P1，调度竞态假设）

- home/HomePage.kt:339-341：初始 HomeUiIntent.Refresh 通过 pageViewModel 派发。
- arch/BaseViewModel.kt:37、46-68：内部 MutableSharedFlow 无 replay；send() 只是创建 viewModelScope.launch 然后 emit。
- arch/Extensions.kt:176-194：初始事件由 LaunchedEffect 启动异步 Flow；initialized=true 在真正完成/确认第一次加载之前设置。
- home/HomeViewModel.kt:268-275：初始 isLoading=true；主页无独立自动超时或“未收到 Start”的兜底。
- 反证：BaseViewModel.stateIn(... SharingStarted.Eagerly ...) 使生产者通常很早开始收集，降低实际丢事件的概率。不能凭代码就声称本事故确实发生首事件丢失。

可能时序：构建 HomeViewModel，HomePage 显示默认 Loading，初始化意图被发射但无有效订阅/收集因生命周期取消而未落到 Start；页面缺少重试/终止分支，可能长期保持骨架。需要以 ViewModel 实例 ID 和 Refresh 序号区分不同实例，不能仅凭裸字符串“send Refresh”。

## F04｜zip 使本地历史查询与远程关注吧加载形成单点等待（P1，确认结构性问题）

- home/HomeViewModel.kt:74-99：HistoryUtil.getFlow(TYPE_FORUM,0).zip(TiebaApi.getInstance().forumRecommendNewFlow()) 后才产生 Refresh.Success。
- utils/HistoryUtil.kt:45-54 与 utils/extension/LitePalFlowExt.kt:26-31：本地历史为 IO 上的 LitePal 查询。
- api/interfaces/impls/MixedTiebaApiImpl.kt:1020-1038：远程实现是 OFFICIAL_PROTOBUF_TIEBA_API.forumRecommendFlow。
- api/retrofit/RetrofitTiebaApi.kt:226-252 及 api/retrofit/interfaces/OfficialProtobufTiebaApi.kt:47-52：请求 https://tiebac.baidu.com/c/f/forum/forumrecommend?cmd=303011，POST Protobuf。

只要一边一直不发射结果，zip 就无法完成首个 Success。网络普通访问正常≠这个域名/端点/账号鉴权正常。需要记录两边独立耗时；当前实现只记录合成的结果，无法直接看出挂起侧。

**更严格的边界**：若请求或数据库抛异常，下游 catch 通常会产生 Refresh.Failure，reducer 设置 isLoading=false。故普通超时、401 或 5xx 不一定使首页永久骨架；持续 Loading 更偏向“没有任何终态”或主线程无响应。若一边正常完成但零发射（Flow.empty），zip 可直接完成而不触发 Success/Failure，此时也只留下 Start。LitePal.findFlow 的正常路径会发射一个列表，即使为空，因此该边界需单元模拟，而不是宣称数据库空结果就会这样。

- HomeViewModel.kt:88-90 还在合成结果里同步查置顶表；因 BaseViewModel 的 flowOn(Dispatchers.IO)，这一路通常在 IO 线程执行，不应误判成首页主线程数据库读取。

## F05｜网络整体超时缺口（P1，已确认配置）

- api/retrofit/RetrofitTiebaApi.kt:54-57、363-415：OkHttp 读取、连接、写入超时均设 60 秒；未设置 callTimeout。
- HomeViewModel.kt:74-99：没有业务层 withTimeout 或“加载最长时限”。
- api/retrofit/adapter/FlowCallAdapter.kt:18-60：使用异步 Call.enqueue 并在取消时 call.cancel，说明普通请求不应天然同步阻塞 UI；也说明“有网络请求”本身不足以解释全屏点击无响应。

读取/连接/写入各自的超时不构成一次请求端到端的硬截止。只有真实耗时和 endpoint 结果才能证明该缺口参与事故。修复时不可把超时错误吞掉，必须生成终态和可操作重试。

## F06｜Loading 期间下拉刷新被拒绝，缺少自救入口（P1，确认交互缺口）

- ui/widgets/compose/PullToRefresh.kt:33-39：ClassicRefreshCycle.begin(alreadyLoading) 在 alreadyLoading=true 时直接返回 false。
- 同文件:85-111：latestLoading 来自 HomeUiState.isLoading，因此持续 Loading 会禁止下拉触发新的 Refresh。
- ui/page/main/home/HomePage.kt:438-470：骨架屏状态没有可见的手动重试按钮，重试仅在 ErrorScreen 等终态存在。
- ui/widgets/compose/states/States.kt:63-96：StateScreen 在空且 Loading 时优先展示 loadingScreen。

所以，只要状态永久处于 Loading，即使主线程仍可点击，主页列表区域也没有可靠的用户自救入口。这能解释“拉刷新不再起作用”，**不能解释搜索图标和底栏全都点不动**。

## F07｜所有控件无法点击：目前未在导航中找到由 Loading 导致的主动禁用（重要反证）

- home/HomePage.kt:407-435：搜索、签到、列表布局切换的 ActionItem 与 isLoading 不关联。
- ui/widgets/compose/Toolbar.kt:191-205：ActionItem 是 IconButton(onClick)，未随加载状态 disabled。
- ui/page/main/NavigationComponents.kt:373-428：底栏 BottomNavigationItem 通过独立 onClick 导航。
- ui/page/main/MainPage.kt:246-283：pagerState.scrollToPage 由 BottomNavigation 回调触发；userScrollEnabled=false 只禁用手势横滑，**不禁用底栏按钮点击**。
- ui/widgets/compose/states/States.kt:63-96：骨架在主页内容 Box 中，不是整个 Activity 的输入屏障。
- ui/widgets/compose/Toolbar.kt:390-401：顶栏父容器有 combinedClickable(onClick={})，但不能仅凭这一点判定子 IconButton 被父节点吃掉，更无法解释底栏。

优先排查主线程阻塞/ANR、独立 Dialog 窗口抢焦点、全屏输入覆盖，以及系统 UI/渲染线程异常。当前代码没有证据支持“首页请求失败主动禁用了全局控件”。

## F08｜全屏 Dialog / ModalBottomSheet 可截获触摸：真实窗口状态未证实（P2）

- MainActivityV2.kt:355-460：剪贴板预览、自动签到电池提醒 Dialog 具有启动时展示路径；MainActivityV2.kt:494-530：ModalBottomSheetLayout。
- ui/widgets/compose/Dialogs.kt:318-380：showDialog/isActiveClose/dismissRequested 的异步状态及退出动画。
- ui/widgets/compose/dialogs/AnyPopDialog.kt:107-180：Android Compose Dialog 独立窗口内含 fillMaxSize 背景 Spacer 和 pointerInput，显示时覆盖整屏。
- components/ClipBoardLinkDetector.kt:69-134：启动时读取剪贴板并可能显示预览。

若 Dialog 因恢复/隐藏状态异常一直留在最上层，可能截获 Activity 点击，即使视觉内容有异常透明阶段；但截图未见明显 scrim，且没有 window dumpsys 或层级树，**不能确认该故障真实发生**。不建议现在删除 Dialog 或默认移除所有点击拦截。

## F09｜其他需保留的次级风险

- DataStore.kt:77-93：rememberPreferenceAsState 同时建立读取协程和 LaunchedEffect(state.value) 写入协程，默认值可能与保存值竞争；这是存储一致性风险，并非当前卡死的充分证据。
- App.kt:85-115：启动同步初始化还有 AccountUtil.init、LitePal.initialize 等；utils/AccountUtil.kt:69-80 中 LitePal.findAll 属于同步磁盘数据库访问，需从主线程 trace 测耗时。
- MainActivityV2.kt:290-335：注册服务、JobScheduler、initAutoSign 等均是生命周期启动路径；没有测量前不能定为根因。
- api/retrofit/interceptors/ProtoFailureResponseInterceptor.kt:9-33：同步完全读取并解析响应，运行于请求链，而非直接在主页 Compose 主线程；可以作为网络慢的检查点，但不能直接解释触摸冻结。
- BaseViewModel.kt:46-60 会 Log.i 输出整个 partialChange 对象，可能含用户内容；生产环境诊断应记录安全标记，不应把原始业务对象与凭据上传到公开仓库。

## 故障分级（含证据级别）

| ID | 代码问题 | 事实等级 | 解释长骨架 | 解释全局触摸不响应 |
| --- | --- | --- | --- | --- |
| F01 | Compose/启动主线程同步 DataStore | 已确认代码会同步等待；事故未证实 | 可能（首帧后阻塞） | 可能且优先 |
| F02 | ViewModel equals / hashCode 契约违规 | 缺陷已确认 | 间接可能 | 无直接证据 |
| F03 | 初始事件缺接收确认 | 竞态风险 | 可能 | 不能单独解释 |
| F04 | zip 等待历史 + 远端 | 结构已确认 | 可能 | 不能单独解释 |
| F05 | 缺端到端超时 | 配置已确认 | 可能 | 不能单独解释 |
| F06 | Loading 下拉刷新不可重试 | 行为已确认 | 放大卡住持续时间 | 只能解释刷新手势 |
| F07 | 全局按钮与首页数据加载独立 | 架构已确认 | 不适用 | 反证“网络失败=全局禁用” |
| F08 | 全屏 Dialog/BottomSheet | 潜在触摸拦截路径 | 无直接证据 | 待窗口证据 |
| F09 | 偏好/启动其他风险 | 部分逻辑已确认 | 待确认 | 待确认 |

**审计结论不是最终根因认定**。用户提供的“正常互联网”和“清缓存无效”与本地同步阻塞或特定 API 失败均相容。下阶段用 [05 根因裁决](05-root-cause-matrix.md) 和 [06 测试计划](06-audit-regression-plan.md) 逐项验证，避免以低证据的假设直接重构网络或禁用 UI。