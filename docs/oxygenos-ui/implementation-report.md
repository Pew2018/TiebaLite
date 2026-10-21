# TiebaLite 经典 Material Design 改造 — 本轮交付报告

- 分支：`feature/oxygenos-ui`（从 `4.0-dev` @ `2885b2aa` 切出）
- 交付提交：`18d0fe8b`（含 6 个提交：主改造 + 5 次 CI/资源修复）
- 构建：GitHub Actions「Build, Deploy」✅ 成功（run 39476813571）
- 产物：`release-4.0.0-beta.1+18d0fe8(390100).apk`（10.9 MB，versionCode 390100，minSdk 21 / targetSdk 34，纯 JVM 通用包，**arm64 可用**，无 native 库、无 ABI 限制）

## 1. 本轮改动概述

### 主题体系（阶段 B）
- **20 个预设**全部落地，单一数据源 `ThemePresets.kt`（列表、预览、页面取色同源）：
  - 经典配色列：晴空蓝 #42A5F5 / 流金橙 #CC6F4E / 柠檬黄 #E6A545 / 青草绿 #7DC22F / 魅力紫 #9575CD / 天空蓝 #26C6DA / 活力红 #F06292 / 时尚粉 #BA68C8
  - Material 风格列：M 蓝 #2196F3 / M 青绿 #009688 / M 绿 #4CAF50 / M 红 #F44336 / M 橙 #FF9800 / M 紫 #9C27B0 / M 青 #00BCD4 / M 靛蓝 #3F51B5 / M 粉 #E91E63 / M 蓝灰 #607D8B / M 深橙 #FF5722 / M 浅绿 #8BC34A
  - 经典配色列 Blue 与 Material 列 Blue 为两个不同色值的独立选项；onAccent 按对比度自动选白字/深字
- 语义角色：`ExtendedColors` 扩展 `statusBar/switchOn/switchOff/fab/selected`；浅色基座 `TiebaLite.Light`（工具栏 #212121、内容 #FAFAFA、分割线 #E6E6E6）、深色基座 `TiebaLite.Dark.Oxygen`、纯黑 `TiebaLite.Dark.Amoled` 重制
- `App.ThemeDelegate` 预设优先解析 `theme_color_*_<preset>` 命名资源，Compose 与 View 两条链路取色一致
- **旧主题迁移**：tieba→material_blue、blue→oxygen_blue、black→material_blue_grey、pink→oxygen_fashion_pink、red→material_red、purple→material_purple、blue_dark→oxygen_blue_dark、grey_dark→material_blue_grey_dark、dark→oxygen_blue_dark；读取时一次性迁移并写回（含夜间模式选择值），升级用户不崩溃、不空色
- 浅色回退令牌同步经典 Material Design 基线（背景 #FAFAFA、次文字 #757575、导航栏、chip 等）

### 组件与设置页（阶段 C）
- 经典窄轨道 Switch 默认色改用 `switchOn/switchOff` 语义色（白滑块 + 主题色/浅灰轨道）
- 主题页重写：两列分组 + 分组标题 + 种子色色块 + 选中态 + 配色关系示意图（随主题/深色模式实时更新）
- 工具栏体系沿用现有 0  elevation 平直 TopAppBar（已是 MD1 平直风格），深色工具栏白字由令牌保证

### 预测返回（阶段 E）
- `Modifier.predictiveBack`（activity-compose 1.8.2 `PredictiveBackHandler`）：手势缩放预览、取消回弹、完成执行既有返回动作
- 「个性化 → 预测返回手势」持久化开关（默认开；API 34+ 生效，旧系统显示不支持说明并自动降级）
- 清单 `android:enableOnBackInvokedCallback="true"`
- 接入：MainActivityV2 根级兜底（可 pop 时）+ MainPage / SearchPage / ThreadPage / ReplyPage / SubPostsPage / WebViewPage 页面级，优先级与既有 `MyBackHandler` 完全一致
- 依赖结论：Navigation 2.7.6 + Destinations 1.10.0 **不升级**（2.8 需 Destinations ≥1.11 与编译器配套验证，风险大于收益），采用页面级应用内预览替代方案
- 返回矩阵：`docs/oxygenos-ui/return-matrix.md`（30 个 destination + 独立 Activity + 弹层/对话框/深链全覆盖）

## 2. 20 色预设核对表

| # | 列 | 预设 | 色值 | onAccent | 浅色样式 | 深色样式 |
|---|---|---|---|---|---|---|
| 1 | 经典配色 | 晴空蓝 | #42A5F5 | 深字 | 已实现待设备验证 | 已实现待设备验证 |
| 2 | 经典配色 | 流金橙 | #CC6F4E | 深字 | 已实现待设备验证 | 已实现待设备验证 |
| 3 | 经典配色 | 柠檬黄 | #E6A545 | 深字 | 已实现待设备验证 | 已实现待设备验证 |
| 4 | 经典配色 | 青草绿 | #7DC22F | 深字 | 已实现待设备验证 | 已实现待设备验证 |
| 5 | 经典配色 | 魅力紫 | #9575CD | 深字 | 已实现待设备验证 | 已实现待设备验证 |
| 6 | 经典配色 | 天空蓝 | #26C6DA | 深字 | 已实现待设备验证 | 已实现待设备验证 |
| 7 | 经典配色 | 活力红 | #F06292 | 深字 | 已实现待设备验证 | 已实现待设备验证 |
| 8 | 经典配色 | 时尚粉 | #BA68C8 | 深字 | 已实现待设备验证 | 已实现待设备验证 |
| 9 | Material | M 蓝 | #2196F3 | 深字 | ✅ | ✅ |
| 10 | Material | M 青绿 | #009688 | 深字 | ✅ | ✅ |
| 11 | Material | M 绿 | #4CAF50 | 深字 | ✅ | ✅ |
| 12 | Material | M 红 | #F44336 | 白字 | ✅ | ✅ |
| 13 | Material | M 橙 | #FF9800 | 深字 | ✅ | ✅ |
| 14 | Material | M 紫 | #9C27B0 | 白字 | ✅ | ✅ |
| 15 | Material | M 青 | #00BCD4 | 深字 | ✅ | ✅ |
| 16 | Material | M 靛蓝 | #3F51B5 | 白字 | ✅ | ✅ |
| 17 | Material | M 粉 | #E91E63 | 白字 | ✅ | ✅ |
| 18 | Material | M 蓝灰 | #607D8B | 白字 | ✅ | ✅ |
| 19 | Material | M 深橙 | #FF5722 | 深字 | ✅ | ✅ |
| 20 | Material | M 浅绿 | #8BC34A | 深字 | ✅ | ✅ |

深色变体为种子色提亮 34%（夜间可读性），夜间模式列表提供 20 个深色预设 + 纯黑。
色值来源：用户提供的配色图，逐一核对无偏差。

## 3. 构建与测试结果

| 项 | 结果 |
|---|---|
| GitHub Actions assembleRelease | ✅ 成功（run 39476813571，约 12 分钟） |
| arm64 可用产物 | ✅ 通用 APK（无 native 库，arm64-v8a 直接可用）；artifact `release-4.0.0-beta.1+18d0fe8(390100)` |
| 单元测试 | 未执行（工作流不含 test 任务；本轮无测试改动） |
| 真机/模拟器验证 | **未执行**（本环境无可用模拟器，见「未完成项」） |

### CI 修复记录（均为基线问题，非本轮 UI 引入）
1. `saurav-aggarwal/appcenter-cli-action` 仓库已下线 → action 解析失败使 job 在 setup 阶段即失败（第 1、2 次运行）；改为提示步骤
2. workflow YAML：`secrets` 上下文不能用于步骤级 `if`；echo 值中 `: ` 触发 YAML 映射解析 → 已修正
3. keystore 步骤在 fork 未配置 `vars.RELEASE_KEYSTORE` 时跳过（debug 签名回退，产物仍可安装）
4. 资源错误：预设颜色值漏 `#` 前缀、`@color/#hex` 非法引用、基座引用未定义的 `oxygen_blue_dark` → 已全部修复并加本地校验脚本逻辑

## 4. 旧主题迁移结果

- 迁移表见上；`migrateLegacyTheme()` 在 `themeState` 初始化、`getOldTheme()`、`switchNightMode()`、`switchToNightMode()` 四条读取路径生效，一次性写回 DataStore
- 自定义颜色、动态颜色、半透明主题（含背景图）、状态栏文字模式、旧主题恢复行为均保留
- 全仓搜索确认旧样式（TiebaLite_Tieba/TiebaLite_White/TiebaLite_Pink 等）仅作为迁移目标父样式保留，不再作为可选入口

## 5. 尚未完成 / 无法验证（如实记录）

1. **真机验证未做**：20 个预设的浅/深色逐项截图核对、预测手势（完成/取消/快速重复/嵌套弹层/键盘）实机回归、Android 7.1.1 普通返回路径——本环境无模拟器且沙箱禁止安装 SDK 组件，**未验证**，需在设备上按 F2 清单过一遍
2. **逐页逐状态核查（阶段 D）未全部完成**：本轮通过基座令牌完成全局视觉统一，并完成了主题页/设置页/搜索页/帖子页返回逻辑改造；论坛列表、帖子卡片、用户页等页面的「加载中/空/失败/刷新/字体放大/窄屏」逐项状态走查未做
3. **组件逐项基线核对（阶段 C）未全部完成**：Tabs 48dp+2dp 选中线、FAB 56dp、Dialog 文字按钮、Slider 细轨道等以现有实现为准做了核对，但未逐项重构；列表未全面卡片化（符合规范要求）
4. **本地构建不可用**：本机缺 Android SDK Platform 34 且沙箱拦截 SDK 安装，所有编译验证均通过 GitHub Actions 完成
5. 编译产物为 debug 签名（fork 未配置 release keystore 变量/密钥）；如需 release 签名，在 fork 配置 `RELEASE_KEYSTORE` 变量与 `KEYSTORE/RELEASEKEYPASSWORD` 等密钥后重新触发即可

## 6. 复现方式

- 分支：`git clone -b feature/oxygenos-ui https://github.com/Pew2018/TiebaLite.git`
- 本地重新生成主题资源：`python3 tools/gen_oxygen_theme.py && python3 tools/gen_theme_colors.py`（输出被 .gitignore 忽略，仅生成器入库）
- Actions：push 即触发；artifact 在 run 页面下载
