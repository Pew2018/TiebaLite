# TiebaLite OxygenOS 7.1.1 UI 第二阶段公共组件实施记录（2026-10-21）

目标：`feature/oxygenos-ui`，基线 HEAD：`37afb24e2c1a2d4099def1c7b9ce50cad506702c`。本记录仅描述源码改动，不代替设备验收。

| 组件 | 本阶段修改 |
|---|---|
| Typography / Shape | `Type.kt` 明确 h6 20sp Medium、subtitle1 16sp Normal、body1 16sp、body2 14sp、caption 12sp、button 14sp Medium；共享中圆角缩至4dp。 |
| Toolbar | Action 与返回图标24dp、使用页面角色前景色；TitleCentredToolbar 加标题左边距；保留原 statusBars inset、双击回顶。 |
| Tabs | 48dp 最小高度、2dp 平底线、主题语义分割线；Pager 指示线直接插值去掉叠加动画；旧短圆角 TabIndicator 统一。 |
| Ripple / Button / Chip / Lists / Menu | 小圆角按钮和标签，降低普通列表和菜单文字粗细；保留 Material 2 Ripple 与原有交互。 |
| Dialog | 标题使用 h6 Medium，删除多余尺寸动画；第一阶段的按钮与关闭逻辑完全保留。 |
| Prefs / Divider | 单双行56/72dp最小高度，图标项文字从72dp起，无图标从16dp起；主副标题3dp间距，分组间距、语义分割线和禁用项语义统一。 |
| Checkbox / Slider | 显式使用 ExtendedTheme.colors 的填充、前景及关闭轨道颜色，原数据持久化 key 不变。 |
| Navigation | Rail 图标24dp、Drawer平面选中、BottomNavigation深浅主题都用1dp分割线，选中颜色采用accentText。 |
| Settings 首页 | 删除 LeadingIcon 重复空白，标题减重，列表恢复1dp分割线。 |

**未修改** `PullToRefresh.kt`、`ReplyPage.kt`、Hex 校验、主题 ID、导航/业务逻辑及传统 View 组件；既有 Material 2 Switch、Radio 与 FAB 保持。

**待验证和局限**：没有真实设备测试结果；深色/AMOLED、字体放大、不同宽度、60/120Hz、IME、下拉刷新、可预测返回需真机复核。Slider轨道是否必须另绘2dp、复杂 Dialog 长内容自适应是否有边界问题，未在本轮重写交互实现。工作流 `build.yml` 负责执行 `testDebugUnitTest` 与 `assembleRelease`；应以对应 Actions 日志为准。
