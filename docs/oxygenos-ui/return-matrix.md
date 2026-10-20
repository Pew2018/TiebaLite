# TiebaLite OxygenOS 改造 — 逐页返回矩阵（阶段 A2）

> 基线提交：`2885b2aabbbf47aba7bf12b1cd7cbc03b1f5ec15`（4.0-dev / feature/oxygenos-ui 同点）
> 记录时间：2026-10-21。审计依据：MainActivityV2、MyBackHandler、各页面源码。

## 0. 通用返回顺序（本轮实现的统一优先级）

1. 输入法 / 输入面板按现有行为关闭；
2. 页面内临时面板、菜单、可关闭面板先关闭；
3. 对话框 / 底部弹层按本页面既有规则关闭或询问；
4. 页面内状态（搜索关键词、临时选择、阅读标记确认）优先处理；
5. 再返回导航栈上一页面；
6. 根页面交由系统返回，不吞掉返回事件。

预测返回（应用内预览）通过 `Modifier.predictiveBack` 接入：手势进行时页面按进度缩放位移，
取消回弹，完成时执行与普通返回完全相同的动作；默认开启、可在「个性化 → 预测返回手势」关闭，
仅 Android 14（API 34+）生效，旧系统自动降级普通返回。

## 1. 页面矩阵

| 页面 | 正常进入来源 | 返回优先级 | 手势预览目标 | 取消手势结果 | 返回副作用 | 降级（普通返回） |
|---|---|---|---|---|---|---|
| MainPage（主页/4 tab） | 启动 / 深链 / 各页 navigateUp 回栈 | 非首页 tab → 先回首页 tab；首页 tab → 上层/系统 | 首页 tab 内容 | 保持当前 tab、滚动位置 | 无 | BackHandler 已有同样逻辑 |
| MainPage 子页签（home/explore/notification/user） | tab 切换 | tab 内自有刷新/面板优先，再回首页 tab | 首页 tab | 保持页签与滚动 | 无 | 同上 |
| ForumPage（吧列表） | 主页 tab / 深链 | 页面无临时态 → 直接 navigateUp | 上一页 | 保持滚动 | 无 | navigateUp |
| ForumDetailPage | 吧列表点击 | 直接 navigateUp | 上一页 | 保持 | 无 | navigateUp |
| ForumRuleDetailPage | 吧详情 | 直接 navigateUp | 上一页 | 保持 | 无 | navigateUp |
| ForumSearchPostPage | 吧详情 | 直接 navigateUp | 上一页 | 保持 | 无 | navigateUp |
| HotTopicListPage | 主页/热门 | 直接 navigateUp | 上一页 | 保持 | 无 | navigateUp |
| NotificationsPage | 主页 tab | 直接 navigateUp | 上一页 | 保持 | 无 | navigateUp |
| ThreadPage（帖子） | 吧列表/搜索/收藏/深链 | ① 底部面板可见 → 收面板；② 收藏帖且面板不可见 → 阅读位置确认对话框（确认后 addStoreFlow）；③ 否则离开 | 父页面（列表/搜索） | 保持滚动与面板状态 | 收藏阅读标记更新 | 两处 MyBackHandler 保持原逻辑 |
| SubPostsPage（楼中楼） | 帖子页楼中楼 | 直接 navigateUp 回父帖 | 父帖子页 | 保持滚动 | 无 | navigateUp |
| SubPostsSheetPage（弹层形态） | 帖子页 | dismiss 弹层 | 父帖子页 | 保持 | 无 | dismiss |
| ReplyPage（回复） | 帖子/楼中楼回复入口 | ① 表情/图片面板或键盘 → 收起；② 再离开弹层 | 父页面 | 不丢输入、草稿、图片选择 | 草稿保留 | MyBackHandler 保持原逻辑 |
| ReplyDialog（对话框形态） | 同上 | 同上 | 父页面 | 同上 | 同上 | dismiss |
| SearchPage（搜索） | 主页搜索入口 | ① 有关键词 → 清空搜索状态；② 无关键词 → 离开 | 搜索主页（历史/建议） | 保留关键词、键盘、排序、建议列表 | 清空关键词 | MyBackHandler + 顶栏返回逻辑 |
| LoginPage（登录） | 未登录入口 / 账号菜单 | 直接 navigateUp | 上一页 | 保持表单 | 无 | navigateUp |
| UserProfilePage（用户主页） | 头像/用户名点击 | 直接 navigateUp | 上一页 | 保持 | 无 | navigateUp |
| HistoryPage（浏览记录） | 我的页 | 直接 navigateUp | 上一页 | 保持 | 无 | navigateUp |
| ThreadStorePage（收藏） | 我的页 | 直接 navigateUp | 上一页 | 保持 | 无 | navigateUp |
| MonetTestPage | 设置实验入口 | 直接 navigateUp | 上一页 | 保持 | 无 | navigateUp |
| CopyDialogPage（复制弹层） | 帖子长按菜单 | 直接 dismiss | 父页面 | 保持 | 无 | dismiss |
| SettingsPage 及全部设置子页（关于/账号/屏蔽/黑白名单/个性化/习惯/更多/一键签到/主题） | 我的页 / 上级设置页 | 直接 navigateUp 回上一设置页 | 上一设置页 | 保持；主题切换后色彩一致 | 设置项持久化 | navigateUp |
| WebViewPage | 帖子外链 / 深链 | 保持既有语义：返回即离开 WebView（工具栏按钮、系统键、手势一致） | 上一页 | 保持网页状态 | 无 | navigateUp |
| PhotoViewActivity（独立 Activity） | 图片点击 | 系统返回关闭查看器；切图/缩放手势不与边缘返回冲突 | 桌面/上一页 | 保持 | 无 | Activity 默认 onBackPressed |
| EditProfileActivity（独立 Activity） | 我的页编辑资料 | 保留表单/裁剪语义；取消手势不丢更改 | 上一页 | 保留表单 | 保存后 finish | Activity 默认 onBackPressed |
| AppFontSizeActivity / TranslucentThemeActivity / OKSignActivity / UCropActivity | 设置入口 | 各 Activity 既有返回 | 上一页 | 保留 | 无 | Activity 默认 onBackPressed |
| 底部 Sheet（Reply/SubPosts sheet 形态） | 页面内唤起 | 先收面板/键盘，再 dismiss | 父页面 | 保留输入 | 无 | BackHandler + dismiss |
| 对话框（ConfirmDialog/PromptDialog/自定义颜色对话框等） | 页面内 | 按对话框既有规则 | 父页面 | 保留 | 无 | 既有行为 |
| 深链入口（tblite scheme / com.baidu.tieba） | 外部 | 按目标页面规则，根页面交系统 | 目标页 | 保留 | 无 | 既有行为 |

## 2. 兼容性结论（依赖未升级的依据）

- 现状：Navigation Compose 2.7.6 + Compose Destinations 1.10.0 + Kotlin 1.9.22 + Compose Compiler 1.5.8 + AGP 8.2.2 + compileSdk 34。
- Navigation 2.8.x 的官方预测返回 NavHost 转场需要 Compose Destinations ≥ 1.11 与 Kotlin/编译器配套验证，本轮不做单点升级（避免破坏 KSP 导航代码生成）。
- 采用替代实现：activity-compose 1.8.2 自带 `PredictiveBackHandler` + 页面级 `Modifier.predictiveBack`，根级在 MainActivityV2 兜底；旧系统与非支持场景行为完全回退到既有返回链路。
- `AndroidManifest.xml` 已加 `android:enableOnBackInvokedCallback="true"`（application 级，minSdk 21 旧系统忽略该属性）。
