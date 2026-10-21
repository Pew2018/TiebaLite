# OxygenOS 7.1.1 UI 第五阶段：主题修复与验收账本

- 工作分支：`feature/oxygenos-ui`
- 审计基线：`0e5cd9246a205be2b35bb57ad3590c266f5bb02a`（2026-10-21）
- **执行约束**：保留第一至第四阶段的既有实现与 UI，不合并主分支；不删除旧主题 ID 和用户资料；主题切换错误恢复只写主题相关键。
- **验证级别**：本报告记录的是代码变更及待执行矩阵。Android 视觉/帧时间/冷启动必须由真机或模拟器验证；不能因构建通过宣称这些测试通过。

## 状态模型（A、C、I）

| 独立维度 | 当前存储/解析 | 约束 |
|---|---|---|
| 配色 | `theme`（日间有效主题），夜间备份为 `old_theme`；`selectedPaletteTheme` 读取当前家族 | 切换配色保留夜间外观，AMOLED 中更换配色只更新存储的家族 |
| 日夜 | `themeState` 有效主题、`follow_system_night` 控制跟随；`LocalConfiguration` 监控日夜 | 手动切换不与自动跟随同时生效 |
| 深色表面 | `dark_theme`：`amoled_dark` 纯黑；历史 `*_dark` 值代表经典深灰 | 旧 ID 保留，经典深色根据当前家族选择对应深色预设 |
| 主页面顶栏 | `custom_toolbar_primary_color` 开关；`custom_top_bar_color` 独立色 | 关闭跟随时切种子不覆盖顶栏色；设置页使用固定经典深色 |
| 恢复 | `pending_theme` / `last_known_good_theme` | 渲染一帧后确认新主题；启动回退保留非主题资料 |

主题状态图：

```text
selected palette ───────┬─ light: selected id
                        ├─ classic dark: selected preset's *_dark (custom: neutral dark + custom accent)
                        └─ AMOLED dark: amoLED surface + selected palette's accent
follow_system_night ───── controls only light/dark branch
dark_theme ────────────── controls only classic/AMOLED surface
toolbar follow/custom ─── controls MAIN toolbar, not the palette
```

## 变更与边界（B、D、E、F、G、H、J、K）

- 主题选择页保留 56dp 平面列表、真实种子色圆点、Radio、单击整行。为 LazyColumn 增加可保存滚动状态，Radio 显式消费 `accentText` 和次要文字令牌；自定义顶栏入口不再显示无意义的未选中 Radio。
- 顶栏浅色预设不再被一律强制白字：仅保留深海蓝预设的白色前景，其他颜色依据实际对比度选白/深色。设置页固定 #212121，状态栏 #1E1E1E，AMOLED 为黑。
- 修复 `App.ThemeDelegate` 中 `colorWindowBackground` 使用无参全局夜间状态的问题；Toolbar 次要图标与选中图标改由同一顶栏前景派生，避免落后于主题状态。
- AMOLED Window/导航/页面背景使用真正 #000000，保留弹窗/卡片的轻度深灰层级；种子强调色由所选主题的深色变体或自定义色派生。
- 自定义主色编辑不再覆写独立顶栏色；合法 Hex 保存与第一阶段验证路径保持不变。深色样式选项通过一次持久化事务更新，随后切换有效主题。
- 回归补修：`notifyCustomThemeChanged()` 现在只使 Compose 用途色重新解析，不再将活动主题强制改成 `custom` 浅色；异步保存返回时保留 AMOLED/经典深色及当前手动/跟随系统外观。对应仪器测试 `customColourNotificationDoesNotForceLightTheme` 已新增，需设备运行。
- 不增加夸张主题切换动画、Activity 全局 recreate、网络重新加载或额外大型主题状态框架。

## 消费者审查（代码静态级别）

| 控件 | 语义取色 | 当前结论 |
|---|---|---|
| Toolbar/状态栏 | `topBar/onTopBar/settingsTopBar/statusBar` | 本阶段统一及修正前景 |
| Tabs | `progressColor`（ClassicTabIndicator） | 保留前阶段实现，待真机 |
| Switch | `switchOn/switchOff` | 保留，待真机 |
| Radio | `accentText/textSecondary`（主题列表） | 已改，其他页面待走查 |
| Button/Dialog actions | `accentFill/onAccentFill/accentText` | 保留既有令牌 |
| Divider | `divider` | 保留浅/深/AMOLED 各自资源 |
| FAB | `fab` 或 Material primary | 保留既有令牌，待全页面 |
| Checkbox/Slider | Material theme / 具体调用点 | 待逐页视觉复核 |
| Snackbar/Popup | Material Surface / Dialog `windowBackground` | 待设备视觉复核 |
| ProgressIndicator/刷新 | `progressColor` / `pullRefreshIndicator` | 保留第一阶段状态机与动画 |

## 回归矩阵

1. **资源契约**：深海蓝、碧玉绿、靛青蓝浅/深两套 View 样式、颜色资源和数组；旧主题 ID 迁移；资源 ID=0 时保护。
2. **日夜**：浅色→经典深色→AMOLED→浅色；`follow_system_night` 随系统变化，保留配色。
3. **AMOLED**：依次选择不同配色，Radio 表示家族且主题 accent 更新；纯黑窗口、导航、正文背景和非纯黑浮层区别。
4. **自定义**：#000000/#FFFFFF/#2196F3/#FFFF00/#00FF00/#FF0000；输入非法、取消、保存、冷启动；独立顶栏色保持。
5. **快速切换**：十余次切换，多页面 Compose 消费者一致、位置与动作不丢失；无掉帧结论需真机帧时间。
6. **恢复**：强杀在 `pending_theme` 存在时恢复、无效 ID 回退；仅改变主题 key。

## 验证与完成度

- A、B、C、E、F、G、I、J：**已实现待编译与设备验证**（部分 View 页面/极端视觉效果仍需验证）。
- D、K：**部分实现**。公共令牌和主要入口已审阅/修复，但全面跨页面静态与截图覆盖尚不充分。
- H：**部分实现**。滚动保存与状态同步已改，无设备帧时间与快速多次点击测量。
- 单元测试：新增纯 JVM 外观状态解析覆盖；扩展资源/黑色契约。
- Instrumentation：扩展颜色对比度测试，尚需真实设备执行。
- Actions 结果、最终 SHA、artifact 与失败记录以 GitHub 实际运行页面为准另行记录。

> 按照既有 `docs/oxygenos-ui/requirements-tracking.md` 标准，“已实现待验证”不能写作“已验证”。
