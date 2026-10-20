# 第二轮实施账本（2026-10-21）

审计参考：`8bfd28c4`。本轮工作提交以 `docs/oxygenos-ui/` 内记录与 `git log` 为准。

状态定义：**待处理 / 进行中 / 已实现待验证 / 验证失败 / 已验证 / 受外部条件阻塞**。
"已实现待验证"不得写作 ✅；无设备/模拟器时动态项一律标记"已实现待设备验证"。

## 一、需求追踪（附录章节 → 实现 → 状态）

| ID | 来源段落 | 目标行为（摘要） | 实际文件/函数 | 验收步骤 | 状态 |
|---|---|---|---|---|---|
| R-1.1 | 附录 1.1 | 三种设计语言分层：设置深灰、主功能一加蓝、MD1 组件 | `PageRole.kt`、`ThemeUtil.mainToolbarColor/settingsToolbarColor`、`Toolbar.kt rememberRoleColors` | 设置页深灰顶栏；主页/帖子/消息/搜索为主功能顶栏 | 已实现待验证 |
| R-1.2 | 附录 1.2 | 主功能经典一加蓝 #0071CD；设置深灰 #212121 | `ThemeUtil.kt` MAIN_TOOLBAR_* | 主题页切顶栏配色后逐页核对 | 已实现待验证 |
| R-2.1 | 附录 2.1 | 颜色令牌（#212121/#1E1E1E/#0071CD/#0067BC/#2196F3/#FAFAFA/#757575/#E6E6E6/#BDBDBD） | `colors.xml` oxygen_* 令牌 + `ThemeColorMath` | 取色脚本核对 | 已实现待验证 |
| R-2.2 | 附录 2.2 | 尺寸/文字基线（56dp Toolbar、20sp Medium 左标题、56/72dp 行、16dp 边距、48dp Tabs/2dp 线、56dp FAB、310dp Dialog、1dp 分割线） | `Tabs.kt ClassicTabIndicator`、`Dialogs.kt`、`prefs/**`、`Type.kt` | 逐页截图比对 | 部分实现待验证（Toolbar 标题左对齐本轮未做，见 PENDING） |
| R-2.3 | 附录 2.3 | 标题栏三类型；状态栏比顶栏略深 | `Toolbar.kt`（状态栏 spacer=角色状态栏色）、`ThemeUtil.mainStatusBarColor` | 深浅主题 + 角色截图 | 已实现待验证 |
| R-3.1 | 附录 3.1 | 首页双/单列保留；48dp 搜索入口；长按菜单锚定 | `home/HomePage.kt`、`Menu.kt` | 首页走查 | 待处理 |
| R-3.2 | 附录 3.2 | 贴吧详情细进度、锚定菜单、单选排序轻状态 | `forum/**`、`FeedCard.kt` | 贴吧页走查 | 待处理 |
| R-3.3 | 附录 3.3 | 帖子页头像层级、楼中楼 2–4dp、输入栏小圆角、底部面板紧凑、倒序轻量强调 | `thread/ThreadPage.kt`、`reply/ReplyPage.kt`、`subposts/SubPostsPage.kt` | 帖子页走查 | 部分实现待验证（面板紧凑化见 PENDING） |
| R-3.4 | 附录 3.4 | 消息页标题+48dp Tabs 分层、2dp 线连续、空/加载/失败态 | `notifications/**`、`Tabs.kt` | 消息页走查 | 部分实现待验证（Tabs 已改；空态见 PENDING） |
| R-3.5 | 附录 3.5 | 个人中心平面统计区 + 列表 | `user/UserPage.kt`（本轮去统计大卡片） | 个人页截图 | 已实现待验证 |
| R-3.6 | 附录 3.6 | 收藏/历史共用条目、日期次色、时间分组小标题 | `threadstore/**`、`history/**` | 收藏/记录走查 | 待处理 |
| R-3.7 | 附录 3.7 | 主题页普通入口 + 预设分组 + 单选 + 即时预览 | `settings/theme/AppThemePage.kt`（本轮重写） | 主题页走查 | 已实现待验证 |
| R-3.8 | 附录 3.8 | 设置中心 OxygenOS 化；破坏性操作 AlertDialog | `settings/**`（角色+分割线）、`Dialogs.kt` | 设置页走查 | 部分实现待验证（图标精简见 PENDING） |
| R-3.9 | 附录 3.9 | 居中选项 Dialog 310dp + 细 Slider | `Dialogs.kt`、`AnyPopDialog`、`AppFontSizeActivity`、`RulerSeekBar` | 弹窗/字体页走查 | 部分实现待验证（Dialog 按钮已改；AnyPopDialog 形态见 PENDING） |
| R-3.10 | 附录 3.10 | 习惯/签到/关于/缓存统一设置行；禁用态真禁用；关于图标 80–96dp | `settings/{habit,oksign,about,more}/**` | 各设置页走查 | 待处理 |
| R-3.11 | 附录 3.11 | 搜索子系统：经典 SearchView、轻历史、三类结果共享顶栏/Tabs、保状态 | `search/**` | 搜索路径走查（含 B03） | 部分实现待验证 |
| R-4.1 | 附录 4.1 | 三插值器，禁弹簧/回弹 | `AnimTokens` + 既有 tween | 录屏核对 | 部分实现待验证 |
| R-4.2 | 附录 4.2 | 13 项动画时长表纳入统一令牌 | `AnimTokens`（当前 3 档） | 逐项核对 | 部分实现待验证 |
| R-4.3 | 附录 4.3 | 有界 Ripple、从触点扩散 | 既有 Material Ripple（默认） | 列表点击录屏 | 待处理 |
| R-4.4 | 附录 4.4 | 页面切换空间关系；预测返回保留 | Navigation 2.8.6 + `PredictiveBack.kt` | 录屏核对 | 已实现待验证 |
| R-4.5 | 附录 4.5 | PopupMenu 锚定触发按钮 | `Menu.kt`/既有菜单 | 菜单走查 | 待处理 |
| R-4.6 | 附录 4.6 | Tab 线连续、FAB 沿用、Snackbar 避让 | `Tabs.kt`（本轮细线化）、Scaffold Snackbar | 走查 | 部分实现待验证 |
| R-5 | 附录 5 | 图片四状态（加载/成功淡入/失败重试/真空） | `Images.kt`、`Avatars.kt`、`Placeholder.kt` | 断网/弱网走查 | 待处理 |
| R-6 | 附录 6 | 13 类公共组件统一 | `widgets/compose/**`、`prefs/**` | 组件走查 | 部分实现待验证 |
| R-7 | 附录 7 | P0→P4 优先级与功能保留 | 全部改动保持业务接口不变 | 功能回归 | 部分实现待验证 |
| R-8 | 附录 8 | 12 项验收检查表 | `acceptance.md` | 逐项 | 进行中 |
| B01 | 审计 4 | 导航级预测返回、单一处理者、稳定注册、响应式开关 | `PredictiveBack.kt`、`MainActivityV2.kt`、依赖 2.8.6/1.11.9 | 录屏 | 已实现待验证 |
| B02 | 审计 5 | 二/三项回首页连续预览 | `MainPage.kt`（pager 连续滚动） | 录屏 | 已实现待验证 |
| B03 | 审计/任务 B03 | 搜索→吧/帖/用户→原结果路径 | `search/**` + 导航级预测 | 路径录屏 | 已实现待验证 |
| B04 | 任务 B04 | 全部路径/业务拦截；WebView 历史优先 | `WebViewPage.kt`、`ThreadPage.kt`、`ReplyPage.kt` | 路径走查 | 部分实现待验证 |
| T01 | 任务 T01 | 40 ID 一致解析、迁移按 key | `ThemePresets.kt`、`ThemeUtil.kt`、`App.kt` | 自动化/手工 | 已实现待验证 |
| T02 | 任务 T02 | 系统栏表面驱动 | `SystemBars.kt`、`BaseComposeActivity.kt`、`UserPage.kt` | 截图 | 已实现待验证 |
| T03 | 任务 T03 | 角色/顶栏/用途色/消费者 | 见 consumer-table.md | 三预设对比 | 已实现待验证 |
| T04 | 任务 T04 | 主题页可操作性 | `AppThemePage.kt` | 走查 | 已实现待验证 |
| 文案 | 用户补充 | Material 颜色名去 M 前缀；氧气蓝→晴空蓝 | `ThemePresets.kt`、`arrays.xml` | 主题页截图 | 已实现待验证 |

## 二、PENDING（本轮未完成，明确遗留）

1. Toolbar 标题左对齐 20sp Medium（`TitleCentredToolbar` 仍居中加粗）——影响面大，需单独一批 + 走查。
2. 底部操作面板紧凑化（ThreadMenu：圆角/手柄/大块灰按钮/整块选中态）。
3. 长按菜单/PopupMenu 锚定与进出动画。
4. 图片四状态（失败重试、淡入 120–180ms）与空状态低重量化。
5. 设置项装饰性图标精简、禁用态真禁用（签到页等）。
6. 关于页图标 80–96dp 与版本/开源入口靠近。
7. 搜索历史药丸标签轻量化。
8. 动效全量表（R-4.2）与插值器落到具体控件。
9. 字体大小页 Slider 细轨道化。
10. 需求 R-3.1/3.2/3.6/3.10 整页走查。

## 三、本轮用户三类反馈的解决方式

| 反馈 | 解决 |
|---|---|
| 选主题后部分交互控件仍黑色、颜色覆盖错误 | T03 用途色体系 + 消费者接入（Switch/Tabs/Buttons/Dialogs/GroupHeader/进度）；顶栏角色分离；Material2 显式配对 |
| 预测返回只有部分路径有效 | B01 升级 Navigation 2.8.6 + Destinations 1.11.9 获得 NavHost 级预测返回；页面级 handler 仅用于局部状态；统一优先级 |
| 搜索进入吧/帖/用户后缺完整预测返回；底栏二/三项回首页动画异常 | B03 依赖导航级预览；B02 MainPage 连续滚动预览（真实首页为目标、取消完整恢复、不进中间页） |
