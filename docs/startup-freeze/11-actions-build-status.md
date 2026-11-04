# 11｜第一轮修复：编译检查与 CI 触发阻碍

日期：2026-11-04；分支：fix/startup-freeze-loading。

## 已核实

- 工作流 .github/workflows/build.yml 在 push、pull_request 时触发；执行 Gradle :app:testDebugUnitTest assembleRelease；成功后上传 Release Package。
- 第一轮源码修复提交 4903046fa353107f6beafaf44398051afc984c27，补充兼容性修复至 47b8c80c1b4d5dd5051ccad4f621b4176bc49394。
- GitHub Actions runs 接口显示该 Fix 分支运行数为 0；仓库最近可见运行来自 2026-10-22 main。即编译未启动，并非编译失败。
- 当前 GitHub 连接器可以查询和重跑已有 Job，但没有创建新 workflow run/dispatch 的操作。重跑 main 的旧 Job 不能测试 Fix 分支。
- 本地执行环境没有 Android SDK，无法完成 assembleRelease；对 Flow 核心逻辑已使用 Kotlin 1.9/coroutines 做 4 项可执行 smoke checks，结果 4/4 通过，这不等于项目 Gradle 单元测试。

## 推荐的最小触发方法（不改 main、不建 PR）

GitHub 网页中打开 docs/startup-freeze/ci-trigger.txt（确认当前选中 fix/startup-freeze-loading），点击铅笔编辑，将 request=0 改为 request=1，选择直接提交到 fix/startup-freeze-loading 分支。此操作会由用户账号产生一次真实 push。随后打开仓库 Actions > Build, Deploy 检查是否出现目标分支运行。

如果仍无运行，排查仓库 Settings > Actions > General 是否允许工作流运行。若运行失败，查看 Gradle 具体步骤日志；若运行成功，检查 Upload Release Package 的 APK artifact。

备选：在本地已有克隆仓库、当前位于 Fix 分支时运行：

    git pull --ff-only origin fix/startup-freeze-loading
    git commit --allow-empty -m 'ci: trigger first-pass build [skip post]'
    git push origin fix/startup-freeze-loading

当前默认 main 的 build.yml 不含 workflow_dispatch。GitHub 要求手动派发的工作流存在于默认分支，因此只在 Fix 分支添加 workflow_dispatch 并不能可靠启用 Run workflow 按钮；本轮不改 main。

## 核验门槛

- 第一轮源码已在 Fix 分支：是。
- 核心 Flow 纯 Kotlin 逻辑检查：4/4。
- GitHub Actions 构建 run：未出现。
- Android Gradle 单测：未执行。
- Release APK：未生成。
- 真实设备卡死与按钮问题：尚未实测。

不得将无 Actions 运行记录称为构建通过。