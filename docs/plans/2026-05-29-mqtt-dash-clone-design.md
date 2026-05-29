# MQTT Dash Clone — 设计文档

**日期**：2026-05-29  
**状态**：已确认，待实现

---

## 项目目标

开发一款面向 **IoT 入门用户** 的 Android MQTT 仪表盘应用，体验对齐 Google Play 上的 MQTT Dash，但以更友好的首次上手流程和更现代的视觉设计超越原版。

### 定位决策摘要

| 维度 | 决策 |
|:--|:--|
| 目标用户 | 友好、面向入门 IoT 用户，向导式配置 |
| MVP 卡片类型 | Text、Switch、Slider、Image |
| 平台 | Android Only |
| 创新空间 | 保留（但 MVP 阶段不实现） |
| 首次体验 | Quick Connect（单输入框填地址，默认参数） |
| 整体导航 | 方案 B：上下文感知主屏 |

---

## 整体导航与状态机

App 围绕主状态流转：

```
[未连接态] ──连接成功──▶ [已连接态] ──断开/切换──▶ [未连接态]
```

### 未连接态（主屏）
- 居中展示 App Logo + 单行醒目输入框（`broker.example.com:1883`）
- 底部折叠"高级选项"区（用户名/密码、TLS、Client ID）
- 「连接」大按钮，连接中变为加载动画
- 左上角抽屉 → 历史连接列表（最近使用的 Broker）

### 已连接态（仪表板主屏）
- 顶部 AppBar：Broker 名称 + 绿色呼吸灯（连接状态）+ 右侧「✏️ 编辑」按钮
- 中间：Dashboard 网格（空态引导：「点击 ＋ 添加第一个卡片」）
- 右下角 FAB「＋」→ Bottom Sheet，展示 4 种卡片类型图标选择器
- 左抽屉：多 Dashboard 切换（客厅/卧室等）、管理连接、设置

### 编辑态（覆盖在已连接态上）
- AppBar 变色，区分普通模式
- 卡片出现拖拽手柄，支持长按拖拽排序
- 卡片右上角出现「✕」删除按钮
- 点击卡片本身 → 弹出配置 Bottom Sheet

---

## 技术选型

| 层次 | 选择 | 原因 |
|:--|:--|:--|
| UI 框架 | Jetpack Compose + Material 3 | 现代动效，动态主题支持 |
| MQTT 库 | **HiveMQ MQTT Client**（替换 Paho） | Paho 停止维护；HiveMQ 积极维护，支持 MQTT 5 |
| 本地数据库 | Room + SQLite | 配置持久化与历史数据缓存 |
| 响应式数据流 | Kotlin Flow + StateFlow | 与 Compose 天然集成 |
| 图像加载 | Coil 3 | 专为 Compose 设计，支持 URL/Base64 |
| 网格拖拽 | Reorderable 库 | 专为 Compose LazyGrid，避免自研拖拽的坑 |
| 依赖注入 | Hilt | 管理 MQTTService、Repository 生命周期 |

---

## 数据模型

### `brokers` 表
```
id, name, host, port, username?, password?, use_tls, client_id, last_connected_at
```

### `dashboards` 表
```
id, broker_id, name, position
```

### `widgets` 表
```
id, dashboard_id,
type            -- TEXT | SWITCH | SLIDER | IMAGE
title,
sub_topic?,
pub_topic?,
qos,            -- 默认 1
col_span,       -- 1 或 2
row_span,       -- 1 或 2
sort_order,
config_json     -- 卡片私有配置（各类型 schema 不同，扩展点）
```

> `config_json` 为未来新增卡片类型保留扩展点，不需要改动表结构。

---

## MQTT 服务架构

```
App 进程
 └── MQTTService（Foreground Service，保后台连接）
       ├── HiveMQ Client（单一物理连接）
       ├── TopicRouter（topic → SharedFlow<String> 映射表）
       │     ├── "home/light" → Flow<String>
       │     └── "home/temp"  → Flow<String>
       └── PublishQueue（发布队列，断线时缓冲）

各 Widget Composable 直接 collect 对应 Flow，状态自动更新
```

---

## 四种卡片详细设计

### Text 文本卡片
| | 说明 |
|:--|:--|
| 显示 | 大号数值居中 + 单位后缀（如 `36.5 °C`），数值变化时有滚动动画 |
| 交互 | 只读 |
| 配置 | sub_topic、单位后缀、JSONPath（如 `$.temp`，可留空）、字体颜色 |

### Switch 开关卡片
| | 说明 |
|:--|:--|
| 显示 | 左侧图标（内置图标库可选）+ 标题 + 右侧 Toggle；ON 态泛暖光，OFF 态偏灰 |
| 交互 | 点击切换 → 乐观更新 UI → 向 pub_topic 发布；sub_topic 回报不一致时自动纠偏 |
| 配置 | sub_topic、pub_topic、ON payload（默认 `"1"`）、OFF payload（默认 `"0"`）、图标 |

### Slider 滑块卡片
| | 说明 |
|:--|:--|
| 显示 | 顶部标题 + 当前值，满宽滑块轨道，两端标注 min/max |
| 交互 | 拖动实时显示数值，**松手后发布**（避免消息洪水）；长按可弹出数字键盘精确输入 |
| 配置 | sub_topic、pub_topic、min、max、step（默认 1）、单位后缀 |

### Image 图片卡片
| | 说明 |
|:--|:--|
| 显示 | 全卡片填充图片，标题悬浮左下角（半透明黑底），加载中显示骨架屏动画 |
| 交互 | 点击全屏查看 |
| 配置 | sub_topic（payload 为 URL 或 Base64）、刷新频率限制（最快 1次/秒） |

### 卡片通用配置
- 标题
- 尺寸：`1×1` / `2×1` / `2×2`
- QoS（默认 1）

---

## 创新预留空间（MVP 之后）
- MQTT v5 协议支持（Message Expiry、Topic Alias）
- Material You 动态取色（Android 12+）
- AI 告警分析（异常数值检测）
- Chart 历史折线图卡片
- Color Picker 卡片
- JSON 配置导入/导出
