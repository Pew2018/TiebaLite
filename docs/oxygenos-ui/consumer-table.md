# 主题消费者表（令牌 → 控件 → 预期颜色）

> 状态：**已实现待验证**。验证方法：任选差异明显的 3 个预设（如 晴空蓝/青草绿/M 橙），
> 在浅色与深色各截一图，比对下表"预期"列。

## 令牌来源

| 令牌 | 来源 | 浅色预期 | 深色预期 |
|---|---|---|---|
| `topBar` | 主功能顶栏风格偏好（默认经典应用蓝） | #0071CD | #005FA3 |
| `statusBar` | 主功能顶栏压暗 8% | #0067BC 一档 | 深一档 |
| `settingsTopBar` | 固定 | #212121 | #1E1E1E |
| `onTopBar` | 顶栏对比度自动选择 | 白 | 白 |
| `accentFill` | 种子色 | 种子 | 种子 |
| `onAccentFill` | 对比度自动 | 白/深 | 白/深 |
| `accentText` | 种子在内容背景上可读变体（≥4.5:1） | 种子或压深变体 | 种子或提亮变体 |
| `accentMuted` | 种子 + 内容背景混合 | 极淡种子 | 极暗种子 |
| `onAccentMuted` | 在 accentMuted 上可读 | 深色文字 | 浅色文字 |
| `progressColor` | = accentFill | 种子 | 种子 |
| `switchOn/switchOff` | accentFill / #C4C4C4（深色 #5A5A5A） | 见左 | 见左 |
| `text/textSecondary/textDisabled` | #212121 / #757575 / #BDBDBD | 见左 | #E6E6E6 / #9E9E9E / #616161 |
| `divider` | #E6E6E6 | 见左 | #2A2A2A |

## 消费者清单

| 消费者 | 文件 | 使用令牌 | 预期 |
|---|---|---|---|
| 状态栏 spacer | `Toolbar.kt TopAppBarContainer` | 角色 statusBar | 比顶栏略深 |
| 顶栏背景/标题/图标 | `Toolbar.kt rememberRoleColors` | 角色 topBar/onTopBar | 设置深灰白字；主功能风格色 |
| Tab 选中指示线 | `Tabs.kt ClassicTabIndicator/PagerTabIndicator` | progressColor | 2dp 细线贯通整格，随滑动连续移动 |
| 实心按钮 | `Buttons.kt Button` | accentFill/onAccentFill | 种子底 + 可读前景 |
| 文字按钮 | `Buttons.kt TextButton` | accentText | 无底、主题色文字 |
| Dialog 操作按钮 | `Dialogs.kt DialogPositiveButton/NegativeButton` | accentText / text | 经典文字按钮 |
| 分组标题 | `prefs/GroupHeader.kt` | accentText | 12sp Medium 靠左 |
| Switch | `widgets/compose/Switch.kt` | switchOn/switchOff | 窄轨道小圆滑块，白滑块 |
| 进度/刷新指示 | Material 默认（primary=accentFill） | accentFill | 种子色 |
| 底部导航选中 | `NavigationComponents.kt`（既有选中色） | 需核对 | 走查项 |
| FAB | `Fab` 调用点（primary=accentFill） | accentFill | 56dp 正圆种子色 |
| 链接/可点击文字 | 调用点（secondary=accentText） | accentText | 内容背景上可读 |
| 帖子底部操作选中 | `ThreadPage.kt ThreadMenu` | 待接入 selectedBg | PENDING |
| 阅读标记 | `ThreadPage.kt` | 待核对 | PENDING |
| 菜单选中态 | `Menu.kt` | 待接入 | PENDING |
| Snackbar | Material 默认（surface） | card | 跟随卡片色 |
| 个人统计数字/说明 | `UserPage.kt StatCardItem` | text / textSecondary | 20sp / 12sp |

## 保持中性的项（不属于"错误黑色"）

- 正文/标题文字 `text`（#212121）
- 设置页深灰顶栏及其白字
- 浅色强调底上的深色前景（onAccentFill 自动选择）
- 等级/业务语义色（内容图片、错误提示等）
