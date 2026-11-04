# 06｜定向回归与可观测性清单（审计产物，尚未执行）

目标：以后实际修复前后都能明确证明「内容正常收敛」和「全局交互正常」。只能在 fix/startup-freeze-loading 修改源代码；本轮文档提交不修改测试/业务实现。

## I. 单元测试：Home 状态机

| Case | 输入/故障注入 | 期望 |
| --- | --- | --- |
| HOME-01 | 初始 Refresh 在订阅前/期间/之后发出（受控调度） | 唯一且可靠发出 Start；无永久 Loading |
| HOME-02 | 历史立刻回空列表，远程成功 | 展示 forums，非错误 |
| HOME-03 | 历史很慢但远程已成功 | 合理时限内收敛；不锁住可独立显示的内容 |
| HOME-04 | 远程长期不发射 | 终止等待，生成可重试错误状态 |
| HOME-05 | 一边 Flow.empty，另一边正常 | 不可只产生 Start 后无终态 |
| HOME-06 | 远程 401/403/500、网络断开、DNS、TLS、错误 Protobuf | 生成恰当 Failure，isLoading=false，加载中断可恢复 |
| HOME-07 | 第一次网络慢、第二次快，连续刷新 | flatMapLatest 取消旧结果，避免旧结果覆盖最新 |
| HOME-08 | 主页已有内容，刷新失败 | 保留旧内容和可操作导航，不强制转全骨架 |
| HOME-09 | Activity 重新创建、ViewModel 重建 | 初始化恰好一次，当前实例订阅正确 |
| HOME-10 | 加载中执行下拉刷新 | 不能永久失去重试渠道；有明确的超时/错误回退 |
| HOME-11 | 帐号失效、退出登录再进入 | 不再无提示永久等待，保留登录路径 |

测试设计应先抽出可替换的历史、网络、时间策略，再用 kotlinx-coroutines-test 模拟延迟/取消。不建议仅靠提高超时掩盖 Flow 空完成缺陷。

## II. 主线程/主题风险回归

| Case | 注入 | 期望 |
| --- | --- | --- |
| UI-01 | DataStore 初次读取慢、数据库读慢 | 首帧之后主线程不发生长期 runBlocking 等待 |
| UI-02 | DataStore 编辑持久排队 / 失败 | 不锁死输入；日志可定位，必要时保护已有主题 |
| UI-03 | 第一次安装、已有设置、pending 主题恢复 | 回退语义正确；账号、草稿、收藏不受影响 |
| UI-04 | 日间/夜间与自定义主题反复切换 | 无同步主线程长期阻塞、无设置丢失 |
| UI-05 | 首次打开即点搜索、底部通知/我的 | 即使 Home 加载中也可导航 |
| UI-06 | 先显示剪贴板对话框/自动签到提醒再关闭 | 当前输入窗口回到 MainActivity，无遮罩残留 |
| UI-07 | 从后台恢复、旋转或系统回收重建 | 新 ViewModel 不被误判与旧实例相同 |
| UI-08 | 选择主题/导航时不断组合刷新 | 缓存/异步偏好读写不造成输入冻结 |

测试可采用 Android instrumentation + Compose UI test；必要时用 Perfetto/StrictMode 和 ANR traces 支持实际耗时判断。不要把单元测试通过等价于触摸问题修复。

## III. 最小诊断埋点契约（只允许脱敏）

建议在未来临时诊断构建中记录同一个 screenInstanceId 与 requestId，日志形如：

~~~text
HomeTrace screen=7 request=12 stage=created t=0
HomeTrace screen=7 request=12 stage=intent_emitted t=3
HomeTrace screen=7 request=12 stage=refresh_start t=5
HomeTrace screen=7 request=12 stage=history_done elapsed_ms=18
HomeTrace screen=7 request=12 stage=api_done elapsed_ms=820 result=success
HomeTrace screen=7 request=12 stage=reducer_done loading=false
UiTrace event=bottom_navigation result=handled main_thread=true
ThemeTrace stage=mark_applied elapsed_ms=12
~~~

这些只是日志格式例子，数字**不是测量结果**。不能记录 UID、BDUSS、STOKEN、完整 Cookie、请求 body、响应原文或论坛名称。不能在高频组合路径无节制地写日志。收集日志后及时移除调试信息或采用受控 Debug 开关。

## IV. 执行顺序 / 验收

1. 锁定实际受影响 APK 与现有 fix 分支对应代码；收集 logcat / ANR / 窗口焦点。
2. 先修确认会阻塞主线程的存储调用路径，保留原本主题自愈原子性。
3. 修首次事件可靠派发、zip 空完成与整体超时，补独立数据源状态和错误/重试。
4. 针对已证实的窗口截获问题（如有）定点修改 Dialog/BottomSheet，而不是先删弹窗。
5. 建立上述可重复回归后统一提交源码，GitHub Actions 验证编译、报告产物，真机验证上下文已覆盖。
6. 严禁用一次正常启动、仅有 Actions 通过或仅有网络连通替代真实故障验收。

**当前执行结果**：只完成静态代码审计与测试设计；没有真实执行上述 Case；尚无 ANR 栈与 APK 构建指纹。