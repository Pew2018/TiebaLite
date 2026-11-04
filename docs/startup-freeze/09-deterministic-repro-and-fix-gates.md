# 09｜确定性复现、对照实验与最小修复门槛

本报告用于下一阶段编写测试和修复，当前没有执行编译或真机实验；所有用例均标为计划中。不得把静态推导当成运行测量值。

## Ⅰ. 不依赖百度服务器就能验证的代码级测试

### T01 — Flow.empty 不会给首页产生完整终态

按 HomeViewModel.kt:74-99 的现有 zip + onStart + catch 结构，构造历史流空完成、远程流正常 emit，期望观察仅有 Refresh.Start，没有 Refresh.Success/Failure。若出现此现象，便证明**至少一个可构造的永久 Loading 状态机路径**；这不等于真机 LitePal 正常空查询：LitePal.findFlow 正常会 emit(emptyList())，两者必须区分。

修复通过：所有有限结束但未产生业务值的数据源都产生明确的 Empty/Failure/Success 终态，isLoading=false。

### T02 — 首次 Intent 没有接收确认

对 BaseViewModel.kt:37-68 的无 replay MutableSharedFlow，通过 kotlinx-coroutines-test 的 TestScope 控制以下时序：
1. emit 在订阅之前：普通默认 SharedFlow 可能直接丢弃；单独验证此语义。
2. 先建立 stateIn(SharingStarted.Eagerly) 再派发 intent：通常应成功。
3. 调度异常、导航重建或生命周期切换：检查 initialized 标记是否提早导致永远不再重试。
4. 连续刷新：HomeViewModel.flatMapLatest 应取消旧请求，并保证只有最新请求写最终结果。

须覆盖“现有实际生产者启动时序”，不能仅用裸 SharedFlow 的实验就宣称现场初始化意图确实丢了。

### T03 — Read-only preference helper 的初值回写

模拟持久化 hideExplore=true，但初始传入默认 false，使用受控 DataStore 首项延迟；观察 rememberPreferenceAsState 的 LaunchedEffect(state.value) 是否在数据加载前向 DataStore 写 false。这个实验针对**通用 helper 的危险设计**；实际 MainPage 的 defaultValue 来自 appPreferences.hideExplore，同一存储在正常情况下通常会先读取 true，不能直接套用“默认 false 一定覆盖 true”。

修复通过：纯观察不触发 DataStore 编辑；显式用户操作才写入；原始存储值在首次渲染、系统重建后不丢失。

### T04 — 主题持久化慢操作不得阻塞主线程

准备隔离的 fake theme-store/受控延迟，模拟：
- Application.onCreate 主题恢复首次读取和 pending rollback；
- themeState 惰性初始化额外访问；
- 一帧后 markThemeApplied 事务停顿；
- 系统深浅模式切换、主题重复选择、存储异常。

通过标准：主题恢复逻辑仍保证 theme/last_known_good_theme/pending_theme 原子状态，Activity 前台不因 I/O 等待阻塞输入；单个失败不会造成永久无响应。主线程是否响应要使用仪器测试/Perfetto，不能只依赖单元测试。

### T05 — ViewModel equals 契约和重建

实例 A、B 类型相同且 initialized 相同，验证不会错误地把 A 与 B 视为同一个 ViewModel；hashCode/equals 契约成立；DisposableEffect 及不同 ViewModel 生命周期订阅正确替换。现有实现会错误地判 A == B（同类、initialized 相同），这是一项独立、可确定性测试的缺陷。

### T06 — 网络异常/无法结束时首页应可重试

Fake repository 模拟：
- 远程 401/403/500、空响应、错误 Protobuf；
- 无 DNS、TLS 握手错误、连接中断；
- 响应持续有字节但永不完成（区分 readTimeout 与 callTimeout）；
- 本地 LitePal 查询持续等待或抛异常；
- 刷新中退出页面/返回前台、新旧请求交叉。

通过：最长等待时间由明确设计定义，任何状态都有可操作的终态或重试路径；重复请求不能交叉覆盖；Loading 不屏蔽顶部搜索、布局切换、底栏导航。

## Ⅱ. 历史提交的安全对照方法

GitHub 历史显示 2026-10-21 的 d549b2577 增加 ThemeUtil 自愈、首帧后确认及首页 flatMapLatest。可在**隔离环境**中通过 Git worktree 建立旧/新代码的两个独立构建，固定 Gradle、SDK 和网络条件后比较：
- 首次启动的 theme selfHeal 调用次数、DataStore I/O 时间和主线程等待；
- 首页正常/网络故障/重建情况下的 Refresh 终态；
- 主题切换前后、前后台恢复时按钮响应。

重要：不能用旧 SHA 覆盖当前 fix 分支；旧版应用无法保证可直接在现有用户数据上降级，**不可为了对照而要求卸载、清除现有数据或强行降级用户手机**。优先用另一台测试机、模拟器或 applicationIdSuffix 隔离安装；迁移测试使用经许可且脱敏的样本数据。

## Ⅲ. 最小改动的先后顺序（实现前须再读最新 SHA）

**批次 A：可独立修复的代码缺陷**
- BaseViewModel equals/hashCode 恢复对象身份契约；补测试。
- 无条件的偏好回写从只读 rememberPreferenceAsState 中分离；保留需要的双向设置控件，并验证设置值不倒退。
- 将 UI 上的 DataStore.runBlocking 读/写改为基于协程的安全访问及缓存；不要直接删除 theme pending rollback 机制。

**批次 B：首页生命周期保护**
- 初次 Refresh 必须被确认接收，并有独立的超时/空完成终态；不要一概为所有 SharedFlow 命令添加 replay=1，因为某些命令有副作用。
- 使本地历史与关注吧服务端数据有独立故障范围；支持保存已有列表、错误提示、重试；保留用户历史/置顶排序。
- 为具体 API 设置合理的 end-to-end 超时和错误分类，网络依赖仍采用可取消协程；不要通过 Thread.sleep 或全局主线程锁解决。

**批次 C：需真机输入证据才能定点修复**
- 主线程 ANR 堆栈确认 ThemeUtil/DataStore/数据库/IPC 时针对实际阻塞点优化。
- 仅在窗口焦点/输入目标证明 Dialog 或 BottomSheet 抢触摸时改对应窗口生命周期。
- 测试首页显示 Loading 时顶部搜索及底栏在真实设备上仍能响应。

## Ⅳ. 可以宣称修复的最低验收

1. 受影响 APK 与本次验证 APK 的版本/提交来源明确。
2. 有“原问题 → 复现实验 → 代码变更 → 同一实验通过”的闭环，针对代码缺陷至少单元/仪器测试通过。
3. 主页在网络正常、无网络、超时、异常、空 Flow、登录过期等情况下都不会永久骨架。
4. 即使首页在 Loading、Failure，顶部/底部至少可进入响应并导航。
5. 主题持久化保持原有原子性，OxygenOS 7.1.1 UI、登录信息、历史与置顶行为无回归。
6. GitHub Actions 编译及 APK 产物真实完成，真机冷启动与后台恢复反复验证；如尚无真机设备，则必须明确“静态和自动化测试通过，尚未验证实机 ANR”。
7. 不能把正常网络测试、一次性开机成功、无 ANR 日志、Actions 绿色任一单独作为“完全修复”证明。

后续用户可先提供一次**脱敏后的** logcat 时间窗、bugreport 中应用 main 栈、mCurrentFocus 以及安装 APK versionName 供进一步因果裁决。