# QZone — 代码概览（用于生成架构图）

说明：此文档总结当前仓库中主要模块、关键类与文件路径、组件职责和典型数据流。目标是为后续生成架构图（PPT 使用）提供所有必要信息与文案。

---

## 一、项目概览
- 名称：QZone（Android）
- 技术栈：Kotlin、Jetpack Compose、Navigation Compose、MVVM（ViewModel + StateFlow）、Retrofit + Moshi、Firebase Auth、Play Services Location
- 构建：Gradle（Kotlin DSL），AGP 8.5.2，Kotlin 1.9.24，Compose BOM
- 模块：单一 `:app` 模块（项目根 `settings.gradle.kts` include ":app"）

## 二、关键配置文件与入口
- 根：`Qzone/build.gradle.kts`（插件版本声明）
- 模块：`Qzone/app/build.gradle.kts`（compileSdk、依赖、Compose 设置）
- Manifest：`Qzone/app/src/main/AndroidManifest.xml`（权限、Application、MainActivity）
- Application：`Qzone/app/src/main/java/com/qzone/QzoneApp.kt`（Application 子类 + AppContainer）
- 入口 Activity：`Qzone/app/src/main/java/com/qzone/MainActivity.kt`（设置 Compose 内容、获取 container）

## 三、主要包与文件（按职责）
- Application / DI
  - `com.qzone.QzoneApp` — 在 `onCreate()` 初始化：
    - 调用 `FirebaseUserRepository.ensureFirebaseInitialized(this)`
    - 创建 `AppContainer` 并注入实现：
      - `surveyRepository = PlaceholderSurveyRepository()`
      - `rewardRepository = PlaceholderRewardRepository()`
      - `userRepository = FirebaseUserRepository()`
      - `locationRepository = LocationRepositoryImpl(this)`
  - `AppContainer` — 轻量 service locator，持有 repository 实例并作为全局访问点（`(application as QzoneApp).container`）

- UI 层（Compose）
  - `com.qzone.ui.navigation.*` — 包含 `QzoneApp` 顶层 Composable、导航、`QzoneDestinations.kt`（路由定义，已查看）
  - `MainActivity` 调用 `rememberQzoneAppState(...)` 并传入 container 中的 repository，为顶层 UI 提供依赖
  - 各 screen 的 Composable 位于 `feature/*` 或 `ui/*`（按 IMPLEMENTATION_NOTES 的建议分层），例如 Feed、Survey、Profile、Rewards、Auth

- ViewModel（MVVM）
  - 每个功能（Auth、Survey、Reward、Profile、Location）应有对应的 ViewModel，使用 StateFlow 公开 UI 状态
  - ViewModels 从 `AppContainer` 提供的 repository 获取数据并执行业务逻辑

- 数据层（domain / data）
  - `domain.repository` — 定义接口：`SurveyRepository`, `RewardRepository`, `UserRepository`, `LocationRepository`
  - `data.repository` — 提供实现：
    - `PlaceholderSurveyRepository` — 占位实现（本地 seed 数据）
    - `PlaceholderRewardRepository` — 占位实现
    - `FirebaseUserRepository` — 负责 Firebase Auth（并且包含 `ensureFirebaseInitialized` 静态方法）
    - `LocationRepositoryImpl` — 通过 Play Services 提供位置信息
  - 网络栈（依赖）：Retrofit、OkHttp、Moshi（但仓库当前多为占位实现，待替换为实际 Retrofit 服务）

- 资源/工具
  - `app/src/main/res` — 颜色/样式/字符串资源
  - DataStore（preferences）依赖已加入，用于本地持久化占位数据或缓存

- 文档
  - `QZone/API_DOCUMENTATION.md` — 后端 REST API 说明（统一响应格式、用户/管理员等接口）
  - `QZone/README.md`, `QZone/Qzone/IMPLEMENTATION_NOTES.md` — 产品需求与实现笔记

## 四、典型初始化与运行时调用顺序（用于架构图的时序要点）
1. Android 启动进程并创建 `Application` 实例：`QzoneApp.onCreate()`
   - 初始化 Firebase（`FirebaseUserRepository.ensureFirebaseInitialized(this)`）
   - 创建并填充 `AppContainer`（将 repository 实例放入）
2. 系统启动 `MainActivity`（在 Manifest 中声明）
   - `MainActivity.onCreate()` 中：
     - `val container = (application as QzoneApp).container`
     - `setContent { QzoneTheme { val appState = rememberQzoneAppState(...container.surveyRepository, ...) QzoneApp(appState) }}`
3. Compose 顶层 `QzoneApp` 渲染导航图与初始 Screen（Feed / SignIn 等）
4. UI 层触发事件（例如加载 Feed）→ 对应 ViewModel 调用 repository（接口）
5. Repository（占位或网络）返回数据 → ViewModel 更新 StateFlow → UI 重新组合并渲染
6. 用户操作触发提交（如提交 survey）→ Repository 负责持久化/同步到后端（或占位实现）

在架构图上，这些要点可表现为：Application -> AppContainer -> Repositories ; MainActivity -> AppState -> UI -> ViewModel -> Repository -> External Services

## 五、外部依赖与服务
- Firebase (Auth / Firestore) — `FirebaseUserRepository`（用户鉴权与 profile）
- Backend API（Retrofit） — 尚未大规模接入，API 文档在 `API_DOCUMENTATION.md`
- Google Play Services Location — `LocationRepositoryImpl` 使用（定位权限需运行时申请）

## 六、重要文件路径索引（便于在图中标注）
- Application / DI
  - `Qzone/app/src/main/java/com/qzone/QzoneApp.kt` (Application + AppContainer)
- Activity
  - `Qzone/app/src/main/java/com/qzone/MainActivity.kt`
- Navigation / Destinations
  - `Qzone/app/src/main/java/com/qzone/ui/navigation/QzoneDestinations.kt`
  - `Qzone/app/src/main/java/com/qzone/ui/navigation/QzoneApp.kt` (顶层 Composable，若存在)
- Repositories
  - `Qzone/app/src/main/java/com/qzone/data/repository/PlaceholderSurveyRepository.kt`
  - `Qzone/app/src/main/java/com/qzone/data/repository/PlaceholderRewardRepository.kt`
  - `Qzone/app/src/main/java/com/qzone/data/repository/FirebaseUserRepository.kt`
  - `Qzone/app/src/main/java/com/qzone/data/repository/LocationRepositoryImpl.kt`
- Network / API docs
  - `QZone/API_DOCUMENTATION.md`
- Gradle
  - `Qzone/settings.gradle.kts`
  - `Qzone/build.gradle.kts`
  - `Qzone/app/build.gradle.kts`

（如果某路径在仓库中名称略有差异，请告诉我，我会帮你精确定位）

## 七、绘图建议（图中元素与说明文本，直接可用于 PPT）
建议在架构图中绘制以下图形与连线，便于观众理解：

- Box：Application（`QzoneApp`） — 注：初始化 AppContainer（标注：Firebase 初始化）
- Box：MainActivity — 指向 Application.container（箭头/注释：获取 container）
- Box：UI（Compose） — 顶层 `QzoneApp`、NavGraph、Screens（Feed/Survey/Profile）
- Box：ViewModels — Feature ViewModels（Auth/Survey/Reward）
- Box：AppContainer — service locator，放置 repository 实例
- Row：Repositories — PlaceholderSurveyRepository、PlaceholderRewardRepository、FirebaseUserRepository、LocationRepositoryImpl
- Row：External Services — Backend API (Retrofit)、Firebase、Google Play Services (Location)

箭头与标签示例：
- MainActivity -> UI (setContent)
- UI -> ViewModel (事件/渲染)
- ViewModel -> Repository (调用接口)
- Repository -> External Services (Retrofit / Firebase / Play Services)
- Application -> AppContainer (初始化)
- AppContainer -> Repositories (注入/提供实例)

额外说明（在 PPT 下方或备注中）：
- "占位实现" 表示当前为本地 seed 数据，后续将替换为 Retrofit/后端实现。
- 权限说明：需要定位权限以展示附近的 survey（请在 UI 中处理拒绝情况）。

## 八、为架构图准备的简短文本（可直接放在幻灯片中）
- QZone 使用单模块 Android 架构（Kotlin + Compose）。
- 通过 `QzoneApp` 初始化全局 `AppContainer`，容器向 ViewModels/顶层状态提供 `Repository` 实例。
- 业务流：UI → ViewModel → Repository → (Backend / Firebase / Play Services)。
- 当前有占位仓库用于快速 UI 开发，计划逐步替换为基于 Retrofit 的后端实现。

## 九、后续工作建议（供架构图后续版本参考）
- 在 `AppContainer` 中根据构建变体（debug/release）选择 wiring（占位 vs 实际实现）。
- 为关键接口（SurveyRepository, UserRepository）添加单元测试与契约测试。
- 补充监控/日志层（例如 OkHttp logging、Crashlytics）以便调试与错误追踪。

---

如果需要，我可以：
- 1) 基于上文自动生成一张更精细的架构图（SVG / PNG / PPTX）；
- 2) 将本文件内容压缩为幻灯片备注或直接生成一页 PPTX（插入之前生成的 SVG）；
- 3) 扫描代码仓库以精确列出所有 ViewModel 与 Composable 的文件路径（如果你想要更具体的类级别图）。

请选择下一步（例如：生成 PPTX / 自动绘图 / 列出 ViewModel 文件等），我就继续执行。