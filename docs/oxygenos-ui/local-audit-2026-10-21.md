# 本地审计与复审（2026-10-21）

工作树：`feature/oxygenos-ui`。开始前已有未提交改动：`Images.kt`、`gradle.properties`；本次未修改或回退二者。

## 结论

| 项目 | 结论 | 依据 / 本次处理 |
|---|---|---|
| 预测返回真实上一页 | 确认未实现 | 原 `PredictiveBack.kt` 仅对当前页 `graphicsLayer` 缩放、位移、淡出；已删除该伪预览，取消无副作用、完成仍走原局部返回。Navigation 2.7.7 / Destinations 1.10 无可用的上一目的地合成实现，真实预览待依赖升级和设备回归。 |
| 返回路径 | 部分成立 | `return-matrix.md` 已列 Main、搜索、吧、设置子项、收藏、历史、主题、关于、帖子、资料、WebView、Activity/弹层。局部规则由既有 `MyBackHandler` 保留；真实栈预览未覆盖。 |
| 搜索自动焦点 | 确认 | `SearchPage.kt` 原先只创建 `FocusRequester`；现于首次挂载后请求焦点并调用 IME。吧内搜索已有 requester，但未在本次无设备环境实际触发验证。 |
| 个人页统计 | 不成立 | `4.0-dev` 的 `UserPage.kt` 同样没有关注/粉丝/回帖目标路由或点击处理，故未新增业务入口。 |
| 动态取色 / Hex | 确认 | 主题页有动态入口，Hex 模式无 `TextField`。已移除可见动态入口、旧偏好访问时回写 false、Compose 主题不再使用动态色；Hex 现有实际输入、校验、确认按钮和错误提示。 |
| 设置深灰顶栏 | 确认 | `Toolbar.kt` 按 `PageRole.SETTINGS` 强制 settings 颜色；现统一使用顶栏语义色。 |
| 快速主页刷新 | 确认 | `HomeViewModel` 的 Refresh 为 `flatMapConcat`，会排队；改为 `flatMapLatest`，最后一次刷新取代旧请求。 |
| 普通滚动掉帧 | 未证实 | 相对 4.0-dev 的主页/列表/图片/分页路径未显示本分支运行时差异；无 Pixel 8 Pro、Perfetto/FrameTimeline/JankStats 数据，不能归因。删除了逐事件 `Animatable.snapTo` 与全页 graphicsLayer 风险。 |

## 本地构建复审

- 命令：`./gradlew :app:assembleDebug --console=plain --stacktrace`
- 结果：`BUILD SUCCESSFUL`；variant `debug`。
- 包名：`com.huanchengfly.tieba.post`；版本：`4.0.0-beta.1` / `390100`。
- 动态和 UI 手工验收均未实测：当前环境没有连接的 Pixel 8 Pro 或帧时间采集条件。

## 未完成项

1. 真实上一页面的预测返回预览，需升级导航/代码生成器并做全路径回归，不能由当前页面图层动画替代。
2. 所有页面的视觉、刷新失败重试与搜索/键盘真实设备回归仍需进行；本次只完成静态核验和可编译修复。
3. Pixel 8 Pro 120 Hz、同构建类型的基线 APK 对比和帧时间分布未采集，故没有性能达标结论。
