# TiebaLite — 启动后卡死与首页无法加载：调查索引

- 仓库：Pew2018/TiebaLite
- 工作分支：fix/startup-freeze-loading
- 基线：main @ f91059cbffe453266b74ef06188793e5e82ce780（2026-10-22）
- 建档日期：2026-11-04
- 阶段：证据收集 / 尚未实施修复
- 改动边界：只在本 fix 分支调查；不改 main、不新建 PR、不做无证据的大规模重构。

## 用户提供的现象

用户在正常使用一段时间后重新打开 TiebaLite；互联网可正常使用，但首页的关注吧等项目持续显示灰色骨架占位，清除缓存无效；用户报告全部控件（包括页面按钮）无法点击。提供的截图显示首页标题、顶部工具栏、底部三个导航图标和两组灰色占位，但单张静态截图不能验证触控是否实际失灵，也不能判断等待了多久。

应将两个症状分别记录：
A. 加载状态无法退出或数据请求没有产出；
B. 顶部 / 底部交互同时无效（可能是主线程、输入窗口或覆盖层问题）。

## 深入审计补充（2026-11-04）

- [04-deep-source-audit.md](04-deep-source-audit.md)：本轮对真实 fix 分支的主线程 DataStore 阻塞、ViewModel equals/hashCode、首次事件、zip、超时、刷新自救及全局触摸路径的系统性审计。为此轮**最新、最完整的源码证据记录**。
- [05-root-cause-matrix.md](05-root-cause-matrix.md)：根因候选、可证伪条件、分支裁决与反证，明确“不把静态风险等同于真机已复现”。
- [06-audit-regression-plan.md](06-audit-regression-plan.md)：可观测性、定向回归测试与后续修复验收清单（本轮未执行）。
- 上述文件均以 2026-11-04 起始 fix HEAD 940260400f814701689c71e07dd222bbab8f9301 及未改动的应用源码 f91059cbffe453266b74ef06188793e5e82ce780 为审计基线。
- 重点结论：从源码确认 UI 主线程同步 DataStore 的阻塞机制、BaseViewModel 相等性契约缺陷、zip 数据耦合、业务总超时缺口及 Loading 无重试入口；用户设备上的真实故障根因**仍待运行时证据确认**。

## 本次调查材料

1. [01-code-evidence.md](01-code-evidence.md)：逐项列出可定位源码、事实、推测与反证，明确哪些问题还不能认定为根因。
2. [02-device-capture.md](02-device-capture.md)：真机复现矩阵、非破坏性 ADB 取证、日志解释、敏感信息保护。
3. [03-fix-plan-and-tests.md](03-fix-plan-and-tests.md)：证据驱动的最小修复次序、建议测试与验收标准。

## 已知边界

- 目前只有截图与用户文字反馈，没有真机 logcat、ANR traces、服务端响应、APK 所对应的构建 SHA。
- 本文不声明已经复现，也不声明已证实网络不可用、账号过期、数据库损坏或触控拦截。
- 本阶段不修改 Kotlin、Java、XML、Gradle 或签名相关文件。
- 此目录仅记录诊断证据；待拿到设备日志后更新证据优先级。

## 快速导航与下一步

先在真机卡住期间点击顶部搜索、布局切换、底部通知/我的，并观察 Android 系统返回是否响应；同时保留完整 logcat 和界面卡住时的线程/输入焦点记录。以 01 的分支判定树区分「未发 Refresh / 已发未开始 / 开始但未结束 / 已结束但 UI 不变 / 窗口截获触控」。

只有确认上述阶段后，再在该分支集中提交必要的定点修复与自动化回归测试。

## 证据充分性复核与验证执行方法（追加审计）

- [07-evidence-readiness-and-history.md](07-evidence-readiness-and-history.md)：明确哪些问题可直接开始修复、哪些尚需真机证据；增加偏好初读、主题事务、只读 helper 回写和关键提交历史。
- [08-observability-and-adb-protocol.md](08-observability-and-adb-protocol.md)：非破坏性 ADB 采集、版本指纹、ANR/窗口焦点裁决与最小脱敏诊断点。
- [09-deterministic-repro-and-fix-gates.md](09-deterministic-repro-and-fix-gates.md)：可构造的状态机问题、受控协程测试、主题 I/O 压力测试和修复验收门槛。

2026-11-04 结论：现有证据足以推进主线程同步 DataStore、ViewModel 相等性等确定性缺陷的测试和预防性修复，但**不足以确定用户真机本次全局点击无响应的唯一根因，也不足以承诺完全解决故障**。尚缺受影响 APK 构建指纹、完整 Refresh 时间线、ANR 主线程栈与 InputDispatcher 当前窗口证据。本次新增的是调查和验证文档，未进行业务代码修改或真机实验。

## 第一轮代码修复（2026-11-04）

- [10-first-pass-implementation.md](10-first-pass-implementation.md)：本轮主线程偏好初读、首帧后主题确认、ViewModel 身份/首次 intent、首页空流/超时终态的实际源码改动、测试与剩余风险。
- 该阶段与 01–09 的审计文档并存；保留事故原始证据等级，不以保护性修复替代 ANR/窗口焦点因果验证。
- Actions 验证以 GitHub 实际执行状态为准。项目仍只允许在 fix/startup-freeze-loading 修改。
