# MQTT Dash 手机应用克隆项目技术评估与架构规划报告

本报告旨在评估在当前本地开发环境下，开发一款与 Google Play 上的 **MQTT Dash (IoT Smart Home)** 功能和体验完全一致的独立手机应用的可行性、技术栈选型及架构设计。

---

## 1. MQTT Dash 核心功能与体验对齐

为了实现“一模一样”的体验，我们需要将 MQTT Dash 的核心功能进行解构：

### 1.1 连接与配置管理 (Connections)
- **多代理支持 (Multi-Broker)**：支持配置并保存多个 MQTT Broker（如家庭、办公室、测试服务器等）。
- **连接参数**：
  - 地址、端口、协议（TCP `tcp://`、SSL/TLS `ssl://`、WebSockets `ws://` / `wss://`）。
  - 安全认证：用户名/密码、客户端证书（Client Certificate）双向认证。
  - 会话管理：Client ID（自动或自定义）、Clean Session、Keep Alive、遗嘱主题与内容（LWT Topic & Payload）。
  - 主题前缀（Topic Prefix，用于统一命名空间）。

### 1.2 仪表盘与布局 (Dashboard & Grid Layout)
- **多屏切换**：在同一个 Broker 下可以创建多个 Dashboard 页面（如“客厅”、“厨房”、“传感器总览”）。
- **网格布局 (Grid System)**：组件呈卡片状排列在网格中，支持自定义尺寸（例如 1x1, 2x1, 2x2, 3x2 等）。
- **拖拽重排 (Drag-and-Drop)**：进入编辑模式后，长按并拖拽卡片可重新调整位置和大小。
- **自定义样式**：每个卡片可独立配置背景色、前景色、字体大小、图标（支持内置图标库或自定义图片）。

### 1.3 核心组件库 (Widget Toolkit)
- **文本/数值卡片 (Text / Value)**：
  - 订阅 Topic，实时渲染 payload（支持 JSONPath 提取子段，如 `$.temperature`）。
  - 可配置点击事件，点击时向指定 Publish Topic 发送预设 Payload。
- **开关/按钮 (Switch / Button)**：
  - 订阅状态 Topic，基于 payload 判断当前状态（如 `"on"`/`"off"`，`1`/`0`）。
  - 针对不同状态显示不同的图标、颜色。
  - 点击时翻转状态并向 Publish Topic 发送对应 payload。
- **范围滑块 (Range / Slider)**：
  - 订阅数值 Topic，展示为进度条或滑块。
  - 拖拽滑块实时或释放时向 Publish Topic 发送当前数值（支持设置 Min, Max, Step）。
- **颜色选择器 (Color Picker)**：
  - 订阅颜色 Topic（支持 RGB `255,128,0`、HEX `#FF8000` 或 HSV 格式）。
  - 点击卡片弹出色盘选择器，选择后向 Publish Topic 发送颜色代码。
- **多选列表 (Multi-Choice)**：
  - 配置选项对（如 `{"关闭": 0, "低档": 1, "高档": 2}`）。
  - 点击弹出下拉框或对话框供用户选择，并发送对应的值。
- **历史折线图 (Chart / Graph)**：
  - 订阅数值 Topic，在本地数据库缓存历史数据，或向时序数据接口拉取。
  - 绘制动态平滑的折线图/柱状图，支持选择时间范围（如 1小时、24小时、7天）。
- **图像监控 (Image)**：
  - 订阅图片 URL、本地路径或直接接收 Base64 编码的图片流（用于摄像头画面或动态状态图）。

### 1.4 高级系统功能
- **后台运行与重连**：应用处于后台时保持 MQTT 长连接（可选，或仅在打开时保持）。
- **通知告警 (Notifications)**：支持在后台收到特定 Topic 的报警 Payload（如 `fire_alarm: true`）时发出系统通知。
- **配置导入/导出**：一键将所有 Broker 和卡片配置导出为 JSON 文件，并在另一台设备上导入。

---

## 2. 技术栈可行性评估 (Tech Stack Evaluation)

结合探测到的本地主机环境，我们有两个主要的开发方向：

| 评估维度 | 方案一：原生 Android (Kotlin + Jetpack Compose) | 方案二：跨平台 Web 混合应用 (Vite + React/Vue + Capacitor) |
| :--- | :--- | :--- |
| **本地环境就绪度** | **极高**。已具备 JDK 21 和完整的 Android SDK，开箱即用。 | **中等**。本地未配置 Node.js/npm，需先下载安装并配置全局 Path。 |
| **界面美观与动效** | **极佳**。Jetpack Compose 原生支持质感极佳的 Dynamic UI、磨砂玻璃效果（RenderEffect）、平滑的弹簧物理动效（Spring Physics）。 | **极佳**。Web 技术栈有海量的 CSS 动效库和成熟的 UI 框架（如 Tailwind, Ionic），可控度极高。 |
| **MQTT 协议支持** | 采用 Eclipse Paho Java 库，对 TCP、SSL/TLS 双向证书、KeepAlive 拥有底层原生支持，非常稳定。 | 在纯 Web 环境下仅支持 WebSockets，若要支持 TCP 需依赖 Capacitor 原生插件进行桥接。 |
| **后台服务与通知** | **极强**。Android 系统的 Foreground Service、JobScheduler 和 NotificationManager 可以无缝工作，保证后台接收消息和告警。 | **一般**。混合应用在后台容易被系统杀掉进程，需要编写原生 Java 插件来实现常驻 Service，开发割裂。 |
| **拖拽布局实现难度** | 中等。需要利用 Compose 的手势检测和 `LazyVerticalGrid` 编写自定义的网格拖拽逻辑，或者引入三方库。 | 较低。Web 端有非常成熟的拖拽网格库（如 `react-grid-layout` 或 `svelte-grid`）。 |
| **打包体积与性能** | 原生编译，体积小（约 5-15MB），无 WebView 内存开销，滑动和图表极其流畅。 | 包含 WebView 容器，体积稍大（约 30-50MB），在低端手机上滑动网格和渲染复杂图表可能轻微卡顿。 |

### 💡 评估结论与推荐路线：
如果您希望在**当前的 Windows 环境下最快速度开启开发**，并且应用拥有**极佳的性能、支持 TCP/SSL 直连 Broker、支持可靠的后台消息通知**，**“方案一：原生 Android (Kotlin + Jetpack Compose)”是绝对的首选方案。**
它能够百分之百利用您已有的 Android SDK 和 JDK 21 编译环境，无需安装任何 Node.js 依赖，并且可以使用最现代的 Jetpack 库构建极具质感的 UI 界面。

---

## 3. 架构设计草案 (Architectural Design Draft)

以下是基于 **方案一（原生 Android）** 设计的高层架构，确保系统高度模块化且性能优异。

```mermaid
graph TD
    subgraph UI 展现层 (Jetpack Compose)
        A[MainActivity] --> B[DashboardScreen]
        A --> C[BrokerConfigScreen]
        B --> D[WidgetGridView]
        D --> D1[TextWidget]
        D --> D2[SwitchWidget]
        D --> D3[SliderWidget]
        D --> D4[ColorWidget]
        D --> D5[ChartWidget]
    end

    subgraph 逻辑与数据流动层
        E[MainViewModel] -->|管理状态| B
        F[MQTTConnectionManager] -->|分发数据/处理发布| E
        G[Room Database] -->|数据持久化| E
    end

    subgraph 底层驱动与协议
        F -->|Paho Client| H[MQTT Broker]
        G -->|SQLite| I[本地磁盘存储]
    end
```

### 3.1 数据库结构设计 (Database Schema)

为了保存用户配置的 Broker 以及每个屏幕上的组件，我们需要设计两个核心实体表：

#### 表 1：`mqtt_brokers`（代理连接配置表）
| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| `id` | Long (PK) | 自增 ID |
| `name` | String | 简短名称，例如 "My Smart Home" |
| `server_uri` | String | 地址，例如 `tcp://192.168.1.100` 或 `ssl://broker.hivemq.com` |
| `port` | Int | 端口，例如 `1883` / `8883` |
| `client_id` | String | 客户端 ID |
| `username` | String? | 用户名（可空） |
| `password` | String? | 密码（可空） |
| `clean_session`| Boolean | 是否清空会话 |
| `keep_alive` | Int | 心跳包间隔（秒） |
| `use_ssl` | Boolean | 是否启用 SSL/TLS |
| `ssl_key_store`| String? | 客户端证书本地存储路径或 Base64 编码 |

#### 表 2：`dashboard_widgets`（卡片组件配置表）
| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| `id` | Long (PK) | 组件唯一 ID |
| `broker_id` | Long (FK) | 关联的 Broker ID |
| `dashboard_name`| String | 所属屏幕名称，如 "Living Room" |
| `title` | String | 组件标题，如 "客厅吊灯" |
| `widget_type` | String | 组件类型：`TEXT`, `SWITCH`, `SLIDER`, `COLOR`, `CHART`, `IMAGE` |
| `sub_topic` | String? | 订阅的主题（用于接收状态） |
| `pub_topic` | String? | 发布的主题（用于控制） |
| `json_path` | String? | JSON 解析表达式，如 `$.temp` |
| `qos` | Int | QOS 级别 (0, 1, 2) |
| `retained` | Boolean | 是否保留消息 |
| `col_span` | Int | 卡片宽度占比（如网格总共 4 列，它占 2 列） |
| `row_span` | Int | 卡片高度占比（行高） |
| `grid_pos` | Int | 网格中的排序索引（用于拖拽保存顺序） |
| `style_config` | String (JSON) | 保存卡片颜色、图标、ON/OFF 对应 Payload 等特定样式的 JSON 字符串 |

---

## 4. 核心功能点攻关方案

为了做到和 MQTT Dash 体验一致，有三个核心技术点需要特别设计：

### 4.1 动态连接管理 (Connection Multiplexing)
- **挑战**：在同一个 Dashboard 中可能有很多卡片订阅了不同的 Topic。如果每个卡片都建立一个连接，会极度消耗网络和 Broker 资源。
- **方案**：
  1. 设计一个单例 `MQTTConnectionService`。
  2. 根据当前选中的 `broker_id` 建立**唯一一条**物理 MQTT 连接。
  3. 当加载 Dashboard 时，`MQTTConnectionService` 自动收集当前页面所有卡片配置的 `sub_topic`，批量向 Broker 发起 `subscribe`。
  4. 当收到消息（`messageArrived`）时，根据 Topic 进行哈希映射，利用 Kotlin Flow (如 `SharedFlow`) 或 LiveData 广播给对应的 UI 卡片，实现“一连多用”。

### 4.2 零延迟拖拽重排网格 (Drag & Drop Compose Grid)
- **挑战**：Jetpack Compose 的官方网格并没有内置非常流畅的拖拽重排。
- **方案**：
  1. 使用 Compose 的手势检测器（`detectDragGesturesAfterLongPress`）捕捉长按与移动距离。
  2. 计算当前拖拽的卡片在物理像素网格中的坐标，实时匹配与它重叠或相邻的卡片。
  3. 通过动态计算并更新各卡片的 `grid_pos`（排序索引），配合 `animateItemPlacement()` 产生丝滑的重排过渡动画。
  4. 手势释放时，触发 Room 数据库的批量更新操作，持久化最新的顺序。

### 4.3 离线历史图表与时序缓存 (Offline Telemetry Cache)
- **挑战**：折线图组件需要展示历史数据，但 MQTT 是无状态的即时协议。
- **方案**：
  1. 为折线图卡片指定本地缓存大小（例如最多保留 1000 个数据点）。
  2. 每当该卡片订阅的主题收到新数值时，启动后台轻量线程将 `(Timestamp, Value)` 插入 Room 的 `widget_history` 数据表。
  3. 在图表 UI 中，使用轻量高性能的 Compose 绘图库（如 `vico` 或直接在 `Canvas` 上自定义手绘）将这些历史点连接成流畅的贝塞尔曲线折线图。

---

## 5. 后续规划与工作量评估 (Roadmap)

如果此评估符合您的预期，我们可以将整个克隆项目划分为以下阶段：

### 阶段 1：项目骨架与本地数据库 (1-2 天)
- 初始化 Android Compose 独立项目骨架。
- 引入 Room 数据库并建立 `Broker` 和 `Widget` 的增删改查表。
- 建立主界面框架（抽屉导航、设置页面、多 Broker 列表）。

### 阶段 2：MQTT 核心服务 (2 天)
- 集成 Eclipse Paho MQTT 依赖。
- 编写 `MQTTConnectionManager` 负责后台状态维护、断线重连、多主题订阅路由及 QOS 保证。
- 编写 JSONPath 提取解析器。

### 阶段 3：UI 网格与组件渲染 (3天)
- 编写大网格视图，支持不同尺寸卡片的自适应排列。
- 依次实现 `TextWidget`、`SwitchWidget`、`SliderWidget`、`ColorWidget` 等卡片的设计。
- 加入编辑模式，实现长按拖拽卡片调整顺序的核心交互。

### 阶段 4：图表与高级功能集成 (2天)
- 增加历史数据本地时序存储。
- 引入色盘选择器、自定义图标选择器。
- 实现 JSON 配置文件的导入与导出。
- 整体优化 UI 视觉（如深色模式、玻璃拟态卡片效果、点击微震动反馈）。
