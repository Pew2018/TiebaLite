# 07｜证据充分性判定、追加代码证据与提交历史

- 审计日期：2026-11-04
- 唯一工作分支：fix/startup-freeze-loading
- 本轮审计起始 HEAD：e4d4c18a8f9d6fedab691d749ae3c261a79391cd
- 应用代码（未修改）基线：f91059cbffe453266b74ef06188793e5e82ce780
- 性质：静态审计 + Git 历史核对 + 验证方法；未取得用户设备的 logcat、ANR、InputDispatcher、网络响应、安装 APK 版本指纹或可重复实测。

## 直接回答：现有证据足够吗？

**不足以证明这次真机故障已经具备唯一确定根因，因此不足以承诺“修复卡死和无法加载”可以一次性完成。**

但是，已足够实施一些独立、可测试、不会依赖真机猜测的预防性修复：移除主线程同步偏好 I/O、修复 BaseViewModel 相等性契约、给首页 Flow 补确定性终态和超时/重试。正式改动之前需准备对应测试，防止解决骨架但误伤主题恢复、账号状态、历史内容和按钮交互。

| 目标 | 代码证据够否 | 尚缺证据 | 修复执行门槛 |
| --- | --- | --- | --- |
| 清除 UI 主线程 runBlocking DataStore | 是，调用链可证 | 真机 ANR 栈和 I/O 耗时用于确认事故归因 | 可先写回归测试和实施小范围预防修复，但不得宣称已修复真机 ANR |
| BaseViewModel.equals 与 hashCode 契约 | 是，确定代码错误 | 重建场景与本次事故的关联 | 可以最小修复和单元测试 |
| 首次 Refresh 无确认机制 | 存在风险，非必然丢失 | 订阅时序、ViewModel ID、首次意图到达日志 | 先做确定性受控调度测试 |
| zip 的单点等待与无业务截止时间 | 是，结构已证实 | 在线/离线、历史/网络哪端未完成，具体耗时 | 可以补终态与分支隔离测试，然后最小改动 |
| 骨架期间下拉刷新无自救 | 是，代码确认 | 是否用户确实尝试下拉、持续多久 | 可以补 Loading 无终态保护和可重试路径 |
| 顶栏/底栏全部点击失效 | 否；没有显式按 Loading 禁用 | ANR main 栈、输入焦点、点击回调是否进入 | 不可仅凭静态源码认定是某个 Dialog 或 API |
| 透明遮罩抢输入 | 仅有可疑窗口实现 | mCurrentFocus、InputDispatcher 目标窗口、层级 | 必须先采集窗口证据，不能盲目改 UI |

## 追加证据 E08｜主线程首次偏好读取比此前文档描述更广

相关代码：

- utils/AppPreferencesUtils.kt:249-446：DataStoreDelegates.int/string/float/long/boolean 的 getValue() 在首次访问时同步调用 DataStore.kt 的 getX()；而 getX() 均使用 runBlocking { data.first ... }。
- ui/page/main/MainPage.kt:118-123：首次组合计算 hideExplore 默认值时直接访问 LocalContext.current.appPreferences.hideExplore。这条读取路径从 Compose 主线程到委托首次初始化，再到同步 DataStore 读取，**可在主线程发生**。
- ui/page/main/home/HomePage.kt:374-377：首页组合读取 homePageShowHistoryForum、listSingle；首次访问时同样具有同步委托读取的风险。
- utils/ThemeUtil.kt:207-235 与 App.kt:321-580：主题色计算链路会访问 appPreferences 和其他同步的 DataStore getter，可能在组合阶段进一步触发首次加载。
- AppPreferencesUtils.kt:249-446：委托后续通过 CoroutineScope(Dispatchers.IO + SupervisorJob()) 订阅更新；缓存变量跨线程读写缺少显式同步，是另一项可观测性/可见性风险，但暂不能认定卡死根因。

**含义**：不是只有主题切换才可能主线程等待 DataStore；初次渲染导航与首页也会首次读取持久化偏好。因此需要在一次复现中分别测量 theme 初始化、偏好委托首次读取和首帧后 markThemeApplied。

## 追加证据 E09｜同一启动过程可能有多次主题存储事务

- App.kt:85-92：Application.onCreate 明确调用 ThemeUtil.selfHealTheme()。
- utils/ThemeUtil.kt:48-53：ThemeUtil.themeState 首次惰性初始化时，若 App.isInitialized，再调用 selfHealTheme()。
- ui/common/theme/compose/Theme.kt:600-632：Compose 首次读取 themeState 后，LaunchedEffect(theme) 等待一帧，再调用 markThemeApplied(theme)。
- utils/ThemeUtil.kt:105-115：markThemeApplied() 即使 pendingKey 不匹配，也会进入一次 runBlocking + dataStore.edit 调用；只是编辑 lambda 没有实际修改属性。

因此，根据调用时序，正常 UI 生命周期可能需要“应用启动自愈事务 → 惰性 themeState 自愈事务 → 绘制一帧后的确认事务”。这不是每台设备必然卡死的证据，但提供了可定位、可量化的重复 I/O 热点。优化前必须保留 pending/last_good_theme 原子恢复语义。

## 追加证据 E10｜所谓只读偏好 Composable 会主动写回

- DataStore.kt:57-95：
  - rememberPreferenceAsState() 把 State<T> 暴露给调用方（看起来只读），却开启 LaunchedEffect(state.value) 执行 dataStore.edit 写回。
  - 与另一个 LaunchedEffect(Unit) 读取持久化值并发启动；初始化顺序没有“读取完成后才允许写入”的显式屏障。
- MainPage.kt:118-123：导航 hideExplore 调用该函数；这个具体调用的 defaultValue 预先从 appPreferences.hideExplore 读取，通常与已保存值一致，因此**不能说它必然覆写 hideExplore**。
- 但通用 API 对其他 defaultValue 与存储值不一致的调用者存在写入旧默认值、无意义事务和潜在回写竞争。该问题是可证实的结构缺陷；实际是否导致此次冻结未证实。

修复建议：分离 collectPreferenceAsState（只读）与显式设置操作，初始化的读取及写入需明确先后；如需要双向绑定，增加 initialLoaded 标志与单向数据流，避免无条件写回。

## E11｜提交历史证据：主题恢复阻塞路径相对较新，但不足以证明回归

经 GitHub 文件提交历史核对：
- 2026-10-21，d549b2577（feat: complete OxygenOS classic UI fixes）在 App.onCreate 增加 ThemeUtil.selfHealTheme；在 ThemeUtil 增加 runBlocking 自愈/确认事务；在 Compose TiebaLiteTheme 首帧后增加 markThemeApplied；并把首页 Refresh 从 flatMapConcat 改为 flatMapLatest。
  - https://github.com/Pew2018/TiebaLite/commit/d549b2577
- 2026-10-20 至 2026-10-22，主题文件有多次调整：182a70851、8638ed8fa、f77c3555a、239d6cd49 等；需要把受影响 APK 构建 SHA 对应到某次实际改变。
- DataStore.kt 的 rememberPreferenceAsState 读写式 helper 在 2023-07-22 的 dbd07caae 提交即已存在（不是 2026-10 新引入）。
  - https://github.com/Pew2018/TiebaLite/commit/dbd07caae

这条时间线支持“2026 新增主题自愈链优先回归验证”，**但用户仅说使用一段时间后出问题，未提供从哪个 APK 升级到哪个 APK；不能直接认定 d549 就是本次 bug 引入提交。** 也不能以旧 SHA 覆盖当前 fix 分支。

## E12｜确定性反证：普通网络异常不等于永久骨架

- HomeViewModel.kt:74-99：zip 上游真正抛异常时由 catch 转为 Refresh.Failure。
- HomeViewModel.kt:178-205：Failure 将 isLoading 设置为 false；Start 设置为 true。
- HomePage.kt:448-470、states/States.kt:63-97：forums 为空且 isLoading=false 时应进入 Error 或 Empty，而非继续 Loading。

因此：如果日志确认有 Refresh.Failure，界面仍是骨架，必须查 reducer 是否执行、Compose State 是否发射或主线程是否已经停止处理；此时继续把问题归咎于“百度网络错误”不充分。

## 不能从仓库静态分析取得的关键缺口

G1. 受影响 APK 的构建 commit 与版本/设备系统：app/build.gradle.kts:14-26 表示 CI=true 且提供 GITHUB_SHA 时，versionName 追加 +7 位 SHA；本地构建未必携带该尾缀。获取安装版本后再关联 GitHub 提交，不要凭 main HEAD 猜测。

G2. 真机无响应期间 main 线程堆栈/ANR trace、InputDispatcher 目标、WindowManager 当前焦点：决定“主线程阻塞”与“透明窗口抢焦点”的优先级。

G3. 单次首页刷新链完整时间线：是否发出 Initial Refresh、是否有 Refresh.Start、LitePal 历史是否返回、Protobuf 是否返回、Success/Failure 是否进入 reducer、Compose 是否收到新状态。

G4. 影响范围：顶部搜索、布局切换、底部标签、系统 Back 是否全失效；是否只在系统恢复前台或登录状态变更后出现。

G5. 实验验证：慢 DataStore、Flow.empty、网络延迟/挂起、Activity 重建/主题恢复、覆盖窗口生命周期的定向回归；现有 docs 中列出用例，但未执行。

## 判定标准

- **可进入代码修复阶段**：对确定的独立设计缺陷，先写针对性测试后实施最小变更，并明确只是预防性修复。
- **可宣称修复首页 Loading**：新测试确认任何成功/异常/挂起/空完成均在规定时限内产生终态，离线和登录失效真机行为一致。
- **可宣称修复“所有控件无法点击”**：现场 ANR 栈或窗口焦点证实的阻塞/拦截根因已修复，真机复测正常；仅凭 APK 编译通过绝对不够。
- **可宣称本次事故全部修复**：以上两个症状同时覆盖、无新回归且能说明为什么缓存清理无效。