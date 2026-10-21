# OxygenOS 7.1.1 经典 UI 第一阶段实现记录（2026-10-21）

基线：`d65ef8cc14a404181ae0e73d40a16d24131f714d`，分支：`feature/oxygenos-ui`。本文件记录代码实现，不代表真机验收通过。

## A — 下拉刷新
- `PullToRefresh.kt`：将 Material 2 `PullRefreshState` 作为唯一手势基础；新增 `ClassicRefreshCycle` 区分用户触发与初始化加载，避免一轮下拉触发多个刷新请求；无加载状态回传时具有限时释放逻辑。
- 40dp 圆形由 -40dp 隐藏位置根据触摸进度平移至 36dp 停靠处，190ms 无弹簧收回；内部 22dp 进度绘制与旋转弧线使用 140ms Crossfade 过渡，2dp 线宽，主题令牌取色。
- 修改页面：主页、我的、关注、热榜、个性化推荐、消息列表、搜索贴吧/帖子/用户。
- 贴吧：`ForumPage` 成为唯一手势所有者，`ForumThreadListPage` 只通过回调上报当前选中 Tab 的真实 `isRefreshing`，不再持有重复指示器；旧 `PullToRefreshLayout` API 保留兼容，但不在该页面挂载。
- 刷新、分页、首次加载仍沿用原 ViewModel 事件与状态；用户下拉动画不会由初始化网络加载触发。
- 风险：当 ViewModel 未公开独立的用户刷新/自动加载状态时，只能以组件本地触发记录区分；请求可能在极短时间内完成并未发出 loading 帧，这类情况通过 2.5 秒兜底解除。实际拖动、帧时间、失败重试需真机复核。

## B / C — Dialog
- 回复风险弹窗将官方客户端操作移至说明区单独一行，标准底栏只保留取消（左）与继续（右），保留原三个行为。
- `Dialogs.kt`：TimePicker、Confirm、Prompt 调整为取消在左/确认在右；正向按钮提供向后兼容的 `enabled` 与 `dismissOnClick` 参数。DialogScope 拒绝重复调用 dismiss。
- 未改变用户业务回调的返回类型，因此异步保存场景仍需由调用方选择 `dismissOnClick=false` 并在成功后自行关闭。

## D — 回复面板、IME
- 保留打开回复面板不主动弹出键盘；编辑器焦点变化只切换面板状态，不再递归请求键盘。
- 去除可能永久保留的 closingPanel 空白占位，IME inset 仅在编辑状态应用；成功返回仅处理一次，失败保留内容。
- 待 Android 16 真机核对手势返回、Emoji/图片面板切换、IME 动画与横竖屏。

## E — 自定义颜色
- `normalizedHexRgb` 只接受 `#RRGGBB` 并规范化大小写，保留原有存储格式。
- 主色 Hex 草稿提升至 Dialog 按钮同级；只有有效文本才可保存，预览失效回退已保存颜色。
- 顶栏颜色无效时确认按钮禁用且 Dialog 不会因误点关闭；取消恢复持久化值。
- 原主题预设、AMOLED、主题安全回退、系统栏和 DataStore 键均未变动。

## 测试和剩余项
- 新增 `ClassicRefreshCycleTest` 和 `HexColorInputTest` 的纯 JVM 测试。
- GitHub Actions 仍执行原有 `assembleRelease` 流程；是否成功、是否执行单元测试，以实际 workflow 日志为准。
- 无本地真机或模拟器，因此所有视觉时序均为“已实现，待设备验证”。
