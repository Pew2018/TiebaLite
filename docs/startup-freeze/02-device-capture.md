# 02｜真机复现与日志采集手册（不清除应用数据）

## 收集目标

把「内容没有加载」与「按钮不响应」分别记录，标注精确开始时间、当前设备/系统、APK 版本及包名、是否登录、网络类型（Wi-Fi/蜂窝网络）、代理/VPN、是否刚切换账号/主题、进入前是否复制贴吧链接、是否启用自动签到。不要在 GitHub issue 或仓库提交用户名、手机号、BDUSS、STOKEN、Cookie、私钥、日志中完整的接口请求报文。

### 复现矩阵

| 场景 | 观察点 | 用途 |
| --- | --- | --- |
| 首次冷启动 | 5 / 20 / 60 / 120 秒分别记录骨架状态 | 分辨启动失败与单纯请求慢 |
| 正常使用一段时间后返回 | 前后台切换与恢复时能否点击搜索和底栏 | 分辨生命周期/恢复故障 |
| 首页骨架期间 | 分别点搜索、切换布局、底部通知、我的 | 判断是否仅内容卡住 |
| 系统返回与系统手势 | Android 返回/主屏手势是否正常 | 辅助判断系统输入与应用窗口问题 |
| 点击后无反应 | 是否出现触摸反馈、水波纹或短暂切换 | 区分事件未分发和 UI 无法绘制 |
| 飞行模式 / 重连 | 是否出现 Failure、错误页或重试 | 检查网络失败是否收敛 |
| 强制停止再启动（先记录现场） | 是否恢复 | 检查进程内状态或悬挂任务 |
| 账号切换、最近剪贴板内容、自动签到 | 与异常是否相关 | 排查登录/弹窗/后台服务交互 |

单次截图不能证明按钮无法点击，建议附 15–30 秒录屏（触摸位置可见）；不要在录屏中暴露账号或通知隐私。

## ADB 非破坏性取证（示例为 macOS/Linux）

先连接设备并确认：

~~~sh
adb devices
adb shell pm path com.huanchengfly.tieba.post
adb shell pidof com.huanchengfly.tieba.post
adb shell dumpsys package com.huanchengfly.tieba.post | grep -E 'versionName|versionCode'
~~~

在再次启动前，电脑终端开始完整记录（Ctrl+C 停止，别只过滤单一 Tag）：

~~~sh
adb logcat -b main,system,crash -v threadtime > tieba-freeze-raw.log
~~~

另开终端启动应用，实际观察并记录时间：

~~~sh
adb shell monkey -p com.huanchengfly.tieba.post -c android.intent.category.LAUNCHER 1
~~~

发生卡顿时，在记录现场后保存输入焦点与 Activity 状态：

~~~sh
adb shell dumpsys input > tieba-input.txt
adb shell dumpsys activity activities > tieba-activities.txt
adb shell dumpsys window windows > tieba-windows.txt
adb shell pidof com.huanchengfly.tieba.post
~~~

如果确定 App 进程仍存在且无响应，可选采集线程栈。Android 不同版本对 SIGQUIT、ANR traces 的访问有限；请在了解设备权限限制后执行，并保留此前 logcat：

~~~sh
adb shell "kill -3 $(pidof com.huanchengfly.tieba.post)"
~~~

如确实触发 ANR、日志无法给出主线程调用栈，优先使用 adb bugreport 手动取得系统报告（包含大量敏感设备信息，严禁直接公开分享未脱敏原件）。

## 关键日志过滤方法（离线分析）

~~~sh
grep -Ei 'ViewModel|send Refresh|partialChange|ANR in|Input dispatching timed out|FATAL EXCEPTION|SocketTimeoutException|SSLHandshakeException|TiebaApiException|SQLite|LitePal|DeadObjectException|OpenGLRenderer|Choreographer' tieba-freeze-raw.log
~~~

注意：已有 ViewModel 日志会打印 partialChange 对象，这些对象可能包含业务数据，上传日志之前必须做脱敏。没有匹配结果也可能是 release 构建过滤日志，不等于不存在问题。

### 事件判定

- 出现 send Refresh，但没有 Refresh.Start：检查首次订阅、intentFlow 发射时序、ViewModel 是否被销毁。
- Start 后一直没有 Success/Failure：记录距开始的时长，检查 zip 网络与 LitePal 两个分支。
- Failure 已产生但 UI 仍骨架：检查 reduce / 状态流订阅与 Compose 是否执行重组。
- Success 已产生但 UI 仍骨架：核对 ViewModel 实例和 Compose 渲染/前台窗口。
- 顶栏和底栏同时失效：检查 ANR 栈是否存在主线程 Binder / DB / 设备标识 / Compose 事件循环等待；若主线程正常，检查独立 Dialog、BottomSheet、输入焦点与触摸窗口。

## 切勿执行的操作

在保存现场与确认备份前，不执行 pm clear、不卸载、不删除数据库或 DataStore、不删除账号 Cookie、不在公共仓库上传完整 HTTP/系统日志。清除应用缓存未奏效，本身不能证明缓存无关，也不构成清除应用数据的理由。