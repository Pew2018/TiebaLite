# 08｜从静态风险到真实根因：真机取证及最小埋点协议

适用于 fix/startup-freeze-loading。本文新增的是操作方案，**未在任何设备实际执行**。优先保留原始现场，再诊断；不要在采集前执行清除存储、卸载、强制重置或替换账号。

## A. 第一次复现应取得哪些信息

请记录：日期/时区、Android 版本、设备型号、受影响 APK versionCode/versionName、联网方式、是否代理/VPN、上次成功使用的 APK 版本、是否刚升级/切换主题/账号/剪贴板有贴吧链接。只记录“是否登录”，不可上传登录凭据和个人身份信息。

识别 APK 构建源：
- app/build.gradle.kts:14-26：CI 构建时 versionName 可能包含 +<七位提交 SHA>；如果没有该尾缀，必须用产物来源/构建记录核对，不能冒充 SHA。
- GitHub 分支的 HEAD 与设备 APK 的提交未必相同，特别是 APK 安装于不同日期时。
- 显示的主页截图是状态分支证据，不等于同时证明按钮失效。附 10–30 秒显示触摸位置的录屏更好。

## B. 非破坏性 ADB 流程（macOS / Linux）

连接设备，先检查包名和版本（无需 root）：

~~~sh
adb devices
adb shell pidof com.huanchengfly.tieba.post
adb shell dumpsys package com.huanchengfly.tieba.post | grep -E 'versionCode|versionName'
adb shell getprop ro.build.version.release
adb shell getprop ro.product.model
~~~

**在启动应用或准备复现之前**先开启完整日志采集，建议新终端保持该命令运行直至卡死后至少 10 秒：

~~~sh
adb logcat -b main,system,crash -v threadtime > tieba-full.log
~~~

另一终端打开应用（若已运行则在复现时直接操作，不需重启），记录本机系统时间；持续观察至少 5、20、60、120 秒。依次点击首页搜索、列表切换、底部通知/我的，并尝试系统返回；是否有触摸反馈、水波纹、页面切换，分别记录。

在卡住的现场再采集这些（命令会打印敏感设备信息，应保留本地）：

~~~sh
adb shell pidof com.huanchengfly.tieba.post > tieba-pid.txt
adb shell dumpsys input > tieba-input.txt
adb shell dumpsys window windows > tieba-windows.txt
adb shell dumpsys activity activities > tieba-activities.txt
adb shell dumpsys activity exit-info com.huanchengfly.tieba.post > tieba-exit-info.txt
~~~

注意：exit-info 的支持和内容随 Android 版本变化；命令报不支持时，不应把其缺失当作“无 ANR”。dumpsys 是快照，建议在仍卡顿时立即采集，并在 5–10 秒后重复一次以比较。

若屏幕真正不响应，采用 Android 的系统 bugreport 收集 ANR traces/线程信息（可能需授权，内含大量个人信息，**不要把原始 bugreport 上传公共 GitHub**）：

~~~sh
adb bugreport tieba-device-bugreport.zip
~~~

不要默认可以从未 root 设备使用 shell 向应用进程 kill -3：Android SELinux 权限可能阻止该操作。没有进程调用栈就把原因标为未确认，而不是臆造栈。

### 现场证据判读

| 信息 | 如果出现 | 排查方向 |
| --- | --- | --- |
| ANR：Input dispatching timed out | 应用长期未处理输入 | 读取 bugreport 的 main 线程栈；先分清 Binder、DataStore/runBlocking、LitePal、CPU 密集 |
| main 线程位于 runBlocking/ThemeUtil.markThemeApplied | 主题存储调用阻塞 Compose 主线程 | 检查 DataStore actor 排队/迁移/文件 I/O |
| main 线程位于 DataStoreDelegates.getValue/data.first | 初始化偏好同步读阻塞 | 查首次读取、读取耗时、缓存及委托生命周期 |
| 没有 ANR 且 main 线程可处理日志、按钮无反应 | 可能是触摸落到别的窗口或事件路径 | 分析 mCurrentFocus、InputDispatcher 目标、Dialog/scrim |
| Input focus 指向 Dialog 窗口 | 可能被窗口截获点击 | 检查 AnyPopDialog 与启动时剪贴板/自动签到提醒 |
| Search 点击回调已记录，但没有导航完成 | 事件到达 UI；问题在导航/主线程后续 | 不应诊断为单纯输入拦截 |
| Home 一直 Start 无终态 | zip 数据流挂起或退出未发射 | 增加端到端请求时间线 |
| 已记录 Failure 并 reduce loading=false，UI 仍骨架 | 应查 Compose/主线程/实例与状态一致性 | 单独修改网络无效 |

Android bugreport 可能含用户账号、通知、网络配置、应用栈与其他敏感信息。上传前仅摘取必要的脱敏主线程栈、时间戳和通用错误类型；不要附完整 Cookie/BDUSS/STOKEN、手机号、UID 或 response body。

## C. 建议在 fix 分支以后添加的临时低噪声诊断点（此轮未改源码）

**要求：每项只打开始与结束事件，前后使用 SystemClock.elapsedRealtime() 计算耗时，记录当前线程名和唯一但非个人化的 screenId/requestId；不要记录用户名、接口参数或请求体。**

| 文件和锚点 | 事件 | 意义 |
| --- | --- | --- |
| App.kt:85-95 | app_onCreate_start / theme_selfheal_begin/end / account_init_begin/end | 判断应用初始化是否停在同步 I/O |
| ThemeUtil.kt:48-115 | lazy_themeState_init_start/end、selfHealTheme_enter/exit、markThemeApplied_enter/exit | 判断是否重复事务、首帧后同步阻塞 |
| AppPreferencesUtils.kt:249-446 | pref_first_read_begin/end（仅偏好键名白名单） | 确认页面组合首次读取 DataStore 的时间 |
| DataStore.kt:77-95 | pref_collect_start/first_value、pref_write_begin/end | 定位默认值写回竞争 |
| BaseComposeActivity.kt:58-99 | compose_content_start/first_frame_reached | 区分首帧前/首帧后卡住 |
| arch/Extensions.kt:176-194、BaseViewModel.kt:37-68 | vm_created、initialIntent_scheduled、intent_emit_begin/end、intent_collected | 判断 SharedFlow 首次订阅是否实际收到 |
| HomeViewModel.kt:55-99 | refresh_start、history_first_value/complete、api_first_value/complete、refresh_success/failure/cancel/empty | 定位 zip 无终态的确切一侧 |
| HomeViewModel.kt:178-205 | reducer_before/after（isLoading 布尔值） | 判断状态机是否完成 |
| HomePage.kt:339-470 | home_render_phase(initial/loading/empty/error/content)、search_click、list_toggle_click | 判断 UI 是否还活着 |
| MainPage.kt:241-280、NavigationComponents.kt:373-428 | bottom_click_enter、scrollToPage_begin/end | 确认底栏输入是否到达 |
| Dialogs.kt:318-380、AnyPopDialog.kt:105-190 | dialog_show/dismiss begin/end、window focus | 仅在输入焦点异常时启用，避免污染日志 |

### 诊断原则

1. 使用 SystemClock.elapsedRealtime() 而不是 wall clock 做耗时统计；wall clock 只用于和 logcat 时间戳对照。
2. begin 和 end 必须在 finally 中成对；取消单独标 CANCELLED；请求 id 与页面实例 id 分开。
3. 对于可能卡死的同步调用，要在调用前写 begin，不能只在完成后记录耗时，否则根本无法发现“无 end”的挂起。
4. 初始事件的 send() 调用不是“收到”：须把发送与订阅收集两种日志分别记录。
5. 对网络请求至少区分 DNS、TCP、TLS、请求体发送、响应头、解码以及转换 Flow 成功；如需要细分，使用 debug-only OkHttp EventListener，不打印 URL 参数/请求头。
6. UI 点击需要观察 onClick 入口/出口和导航完成，避免将“看到 Ripple”误作成功进入业务回调。
7. 不要在 production release 启用每帧/每次重组输出；高频日志本身可能产生卡顿。

## D. 有设备日志后，按什么规则推进

- 若卡死时主线程在 ThemeUtil/DataStore：先做主题 I/O 修复及缓慢存储的压力回归。网络症状在 UI 恢复后单独复测。
- 若主线程健康，触摸被 Dialog 抢焦点：优先处理具体窗口生命周期，不改所有弹窗与全局导航。
- 若顶部/底部正常响应但骨架永远不结束：调查 HomeRefresh / SharedFlow / zip / 请求超时。
- 若确实发现两个独立问题：分别给出最小修复提交和各自测试，不做无因果证据的“一把梭”重构。

本文件描述可实施的采集方案，没有声称已经采集到真机日志。