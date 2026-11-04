# 10｜第一轮定点修复实施记录（2026-11-04）

- 工作分支：fix/startup-freeze-loading
- 修复前 HEAD：5cc618a81ec4069f1deaabf1795c6a635233b66e
- 目标：基于现有 docs 对已证实问题实施小范围修复，不修改 main，不创建 PR。
- 这是第一轮保护性修复，并不宣称已经通过真机确认“全部按钮不响应”的唯一根因。

## 已实施的修改

### A. 首页长期 Loading 与无法恢复

1. 在 ui/page/main/home/HomeRefreshDeadline.kt 新增 firstOrFailWithin()，默认最长 25 秒等待首次结果。上游空完成则明确抛出错误。
2. HomeViewModel 的 zip 后接入该保护；正常输出保持原有用户关注贴吧/本地浏览历史的合并策略。异常通过现有 catch -> Refresh.Failure -> isLoading=false，进入已有错误页重试逻辑。
3. 25 秒是第一轮产品级上限，非百度 API 的真实耗时统计。加载历史数据过慢同样受该上限约束；需真机测量后调整。

### B. 首次命令与 ViewModel 身份

4. BaseViewModel.send 在无 replay MutableSharedFlow 中等待 subscriptionCount > 0 再发出事件，避免首次订阅尚未建立时直接丢失，且不对有副作用操作全局设置 replay=1。
5. 删除 BaseViewModel 错误的 equals 覆盖，恢复 ViewModel 实例身份和 hashCode 一致性。

### C. DataStore/UI 阻塞与偏好安全

6. AppPreferencesUtils 里的 5 种偏好委托移除首次 getValue 内同步 data.first()，用后台 Flow 订阅 + Compose mutableStateOf 更新。ThemeUtil.selfHealTheme 启动阶段提交事务后缓存不可变的 Preferences 快照，委托首次读取先命中快照，避免原设置暂时回退为默认值且不再新增磁盘读取；后续仍由 Flow 追踪变化。
7. ThemeUtil 的主顶栏风格、保存的旧主题、深色表面与透明主题读取使用非阻塞委托快照；补充 mainToolbarStyle 偏好属性。
8. DataStore.rememberPreferenceAsState 是只读观察器，删去初次组合时无条件执行 dataStore.edit 的写回任务。双向 mutable helper 本轮未改。
9. ThemeUtil.markThemeApplied 改为 suspend + Dispatchers.IO。TiebaLiteTheme 的 LaunchedEffect 原本已是挂起上下文，不再在绘制第一帧后阻塞主线程等待磁盘写入。
10. Application.onCreate 仅自愈恢复主题一次，直接将恢复结果写入 themeState。消除以前在 themeState 惰性初始化时重复做一次 runBlocking 恢复事务的路径，同时保留启动时的完整原子恢复行为。

## 自动化验证增补

- HomeRefreshDeadlineTest：正常首个成功结果、上游不发值即完成、上游一直挂起。
- InitialIntentDeliveryTest：订阅建立之前发出的事件经过门闩后可可靠发送。
- GitHub Actions 已配置 :app:testDebugUnitTest assembleRelease；需要等提交后实际结果，不能预写通过。

## 尚未覆盖的真实故障边界

- ThemeUtil.selfHealTheme 在 Application 启动时仍通过 runBlocking 完成原子事务，该路径是在初始 Activity 绘制前运行；若磁盘 I/O 真正停滞，仍可能阻塞冷启动。将其安全异步化需要启动就绪/主题应用一致性设计，列为第二轮候选。
- 用户主动切换主题时 switchTheme 和 setDarkSurface 仍有同步事务，应以真机栈和主题压力测试决定下一轮改造。
- 启动时首份已提交 DataStore 快照已加入偏好委托的同步内存读取，正常 Application 初始化路径不会因异步 Flow 首次加载而暂时使用错误的持久化值；仍需验证首次启动迁移、非标准初始化顺序和后台写入竞态。
- 不能保证旧版安装 APK 对应本分支 SHA。必须采集构建指纹。
- 未修改任何弹窗的触摸拦截逻辑，因为目前没有 InputDispatcher 或窗口焦点证据。
- UI/设备 ANR、窗口层级、账号过期等仍无运行时证据，不可声称全局点击无法响应已完全修复。

## 下一步验收

1. 先在 Actions 确认单测、编译及 APK；失败仅在同一 fix 分支追加最小修复提交。
2. 真机安装新产物之前记录原有版本、保留数据；升级后多次冷启动、前后台切换、网络断开、主题切换。
3. 卡死现场采集 ANR main 线程、dumpsys input/window，以及首页刷新开始/结束日志；按 docs/08 和 docs/09 的流程确认残余问题。
4. 只有同时验证“主页不再永久骨架”和“顶部/底部可以响应”才允许宣布用户问题全部修复。


## 补充：修复后的实际验证状态

- 2026-11-04：已在隔离 Kotlin 1.9.0 + kotlinx-coroutines-core 环境中，将 HomeRefreshDeadline 逻辑与四种纯 Kotlin 流场景进行编译执行；正常首值、空 Flow、永远挂起和 SharedFlow 首次订阅四项检查均成功。该检查不等于项目 Gradle 单元测试或 Android APK 编译通过。
- GitHub Actions：首次修复提交 4903046fa353107f6beafaf44398051afc984c27 后，通过 GitHub workflow-runs 列表没有发现该分支的运行记录。暂不能宣称 Actions 编译或产物成功。
- 该文档状态记录不改变第一轮源码的修复范围，下一步仍需真实 Android CI 及真机回归。

## 回归审计补丁（同一 Fix 分支）

- 异步化委托初读可能影响 AppIconUtil.setIcon、自动签到及隐藏“发现”等首次读取的行为，已发现并补充防护：App.onCreate 改为后台读取已提交 Preferences 后再应用桌面图标，避免将自定义图标覆盖成默认图标；自动签到、升级检查、导航和主题委托改为先使用 ThemeUtil.selfHealTheme 同一事务产生的内存快照。
- 保持单次启动自愈事务的整体原子性；新增的快照只承载已经提交的持久化值，不做任何数据重写。
- 该补丁依然没有消除 Application.onCreate 本身的启动前 runBlocking（相关风险与改进设计留待下一轮）。