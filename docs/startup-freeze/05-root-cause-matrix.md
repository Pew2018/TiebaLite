# 05｜故障根因候选与诊断裁决矩阵

针对同一次真机故障，不应把“显示骨架”与“顶部/底部不能点击”直接强行解释为同一原因。本文件给出由高到低的候选、直接证据要求和反证。对应代码细节见 [04 源码审计](04-deep-source-audit.md)。

## 候选 A：UI 主线程被同步 DataStore/数据库访问阻塞（优先检查）

**支持源码**：App.onCreate -> ThemeUtil.selfHealTheme -> runBlocking DataStore.edit；首帧后 TiebaLiteTheme.LaunchedEffect(theme) -> ThemeUtil.markThemeApplied -> runBlocking DataStore.edit；DataStore.getString/getBoolean/getInt 等通过 runBlocking 等待 data.first；主线程上的偏好/颜色计算可能调用这些路径。App 的其他同步初始化及账号 DB 读取也需测量。

**若发生**：屏幕能够停留在最后成功绘制的首页骨架，点击事件和 Compose 协程都无法及时调度，Refresh.Start 也可能没被记录。

**证实**：ANR traces / bugreport 的 main 线程堆栈停于 kotlinx.coroutines.runBlocking、ThemeUtil、DataStore、DataStore actor 等；卡顿期间堆栈重复一致，输入派发超时。或者 Perfetto 显示 UI 线程在上述同步调用长时间阻塞。

**证伪 / 降级**：卡住时 main Looper/Compose 点击回调仍在正常执行，触控反馈及时，且主线程未显示相关等待。

**注意**：同步调用路径确定，不代表这次实际发生主线程阻塞；尤其 App.onCreate 的第一次 edit 位于首帧之前，和截图时序要区分。

## 候选 B：无可见弹窗的覆盖窗口或 BottomSheet 截获事件

**支持源码**：MainActivityV2 的剪贴板/自动签到 Dialog；AnyPopDialog 的独立整屏窗口和填满窗口的输入背景；ModalBottomSheetLayout。

**证实**：点击时 InputDispatcher 的目标窗口或 dumpsys window 的 mCurrentFocus 指向 Dialog/Popup 而非主 Activity；关闭该弹窗后按钮恢复，且 main 线程没有 ANR。

**证伪**：当前焦点明确在主 Activity，无覆盖层，点击事件在主 Activity 收到但业务未执行。

**重要反证**：截图没有可见暗色遮罩，不应直接断言是透明 Dialog。查焦点比盲改窗口透明度可靠。

## 候选 C：初始事件丢失或挂起，主页没有收到刷新终态

**支持源码**：HomeUiState.isLoading=true；pageViewModel 使用无 replay SharedFlow 异步发送第一次 Refresh；HomeViewModel.zip 等待历史与服务端接口；没有业务截止时间；Loading 期间 pullRefresh 拒绝重试。

**证实（按顺序）**：

| 记录 | 更可能的问题 |
| --- | --- |
| HomePage 建立，但没有 Refresh 派发 | 首次 LaunchedEffect/生命周期/主线程阻塞 |
| 有 Intent 派发，未有 Refresh.Start | SharedFlow 订阅调度/取消或 ViewModel 实例变化 |
| 有 Refresh.Start，没有终态 | zip 一侧挂起 / 无发射 / 协程取消路径 |
| 历史已返回、远程一直 pending | 特定 API/代理/TLS/凭据/业务超时 |
| 远程已返回、历史一直 pending | LitePal 查询/数据库锁竞争 |
| 有 Refresh.Failure、状态仍 Loading | reducer/StateFlow/Compose 显示或主线程冻结 |
| 有 Refresh.Success、屏幕仍骨架 | UI 收集对象不一致、重组/主线程问题 |

**反证**：单纯远程 401、TLS/超时抛异常应进入 catch -> Refresh.Failure -> isLoading=false（如果 UI 活着）；不能把“有网络错误”直接等价于“骨架永不结束”。

**关键限制**：此候选单独不足以解释底栏和搜索按钮不能点击。如果它们真的完全无响应，需同时检查 A/B，或证明存在独立原因。

## 候选 D：ViewModel 等价/重建问题

**支持源码**：BaseViewModel.equals 把两个不同实例同类同 initialized 当作相等，没有 hashCode；Extensions.kt 的 DisposableEffect(key1=this) 依赖对象键。

**证实**：Activity/ViewModelStore 重建后，订阅仍指向旧 ViewModel，或 ViewModel 实例不相同但 Compose 的效果键比较认定相等；对比新旧实例 ID、onCleared、发送/接收记录。

**反证**：同一屏、同一 ViewModel 实例未发生重建，或所有订阅随新实例正确重建。此时它仍是独立代码缺陷，但不是这次事故根因。

## 推荐的最短取证顺序

1. 先记录正在运行的 APK versionName/versionCode/构建 commit，避免用 main 静态源码推断来自别的产物。
2. 停留在异常画面 10–20 秒，录屏连续点击搜索、列表切换、底部通知/我的。是否有涟漪、导航、Android 返回响应，明确是什么“不能点”。
3. 立刻抓 main/system/crash logcat、dumpsys input、window、activity；如为 ANR，抓 bugreport 中主线程栈。
4. 若主线程健康而骨架不消失，才添加少量不含敏感数据的 Refresh 发送/收取/完成和 zip 两端耗时埋点。
5. 最后按证据修补 1–2 个核心点。优先最小修复，不要同时移除弹窗、替换网络库、重构全部导航。

## 不允许作为根因证明的材料

- 仅有静态截图（不证明输入分发）。
- “其他 app 有网”（不证明 tiebac.baidu.com 的接口可用）。
- 仅清理缓存无效（不证明持久化偏好/数据库损坏，也不证明网络正常）。
- 仅在源码找到 runBlocking（证明存在阻塞可能，不证明其卡在现场）。
- 仅看到 60 秒 readTimeout（不等于调用总时长有 60 秒截止）。
- 单次未复现（不等于 bug 不存在）。

本轮状态：尚未取得用户设备日志或可重复测试，以上均为结构性/行为性根因候选，不能对单次真实事故做最终归因。