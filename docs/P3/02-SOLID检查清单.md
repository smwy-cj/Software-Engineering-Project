# SOLID 检查清单

> 实验流程：让 AI 根据 P1 需求文档和 P2 架构设计生成初始类图，然后团队对照 SOLID 原则逐条审查，记录违规项并给出修正方案。

---

## 一、实验说明

本实验模拟"AI 设计缺陷注入"场景：首先向 AI 提供完整的 P1 需求规格说明书和 P2 架构设计（含分层架构 ADR-001、模块划分），让 AI 生成"第一版"类图设计；然后团队按照 SOLID 检查清单逐条审查，识别 AI 设计中的违规问题，并给出修正方案。所有修正后的设计必须与 P2 的架构决策保持一致（单体分层、单库 MySQL、前后端分离 RESTful + JWT、暂不引入 Redis/消息队列）。

**实验目的：** 体验 AI 在设计层面的能力边界，理解人类工程师在设计审查中的不可替代性。同时验证 P2 架构约束能否有效"框定"AI 的设计空间，减少越界输出。

---

## 二、AI 原始设计（第一版）摘要

AI 在第一版设计中给出了如下关键结构：

1. 所有模块直接继承自一个巨大的 `User` 类，该类包含了普通用户和管理员的全部属性和方法
2. `TreeHoleService` 类中同时包含了动态发布、评论管理、内容审核、消息推送四个职责
3. 定义了 `IContentManager` 接口，包含了 `publishPost()`, `publishComment()`, `publishPartnerRequest()`, `sendLoveHeart()`, `reviewContent()`, `deleteContent()`, `pushNotification()` 共 7 个方法
4. `PartnerMatchService` 直接依赖具体类 `MySQLDatabase` 进行数据操作
5. `NormalUser` 子类重写了 `login()` 方法，增加了"仅校内用户可登录"的额外前置条件（违反父类契约）
6. 课程设计中只使用了简单的继承关系，没有应用任何设计模式
7. 没有设计 `PrivacySettings` 类，用户隐私设置以散落属性形式存在于 User 类中

---

## 三、SOLID 逐条检查清单

### 3.1 S — 单一职责原则 (Single Responsibility Principle)

| 检查项 | 分析 |
|--------|------|
| **AI 原始设计** | `TreeHoleService` 类同时承担：动态 CRUD、评论管理、敏感词过滤、内容审核流转、消息通知推送 |
| **是否违反** | **是 — 严重违反** |
| **违反说明** | 该类承担了至少 3 个不同职责：①业务逻辑（发布/评论）②审核逻辑（敏感词过滤）③通知逻辑（消息推送）。任何一个职责变更都需要修改该类，且该类会因为多个原因而变化。例如：修改敏感词规则需要改这个类、修改通知模板也需要改这个类。当类超过 500 行时，单元测试难度急剧上升。 |
| **修正方案** | 拆分为三个独立的 Service 类：<br>① `TreeHoleService` — 仅负责树洞动态和评论的业务逻辑<br>② `ContentReviewService` — 负责敏感词过滤和审核流转（可被所有模块复用）<br>③ `NotificationService` — 负责消息通知的创建和推送（可被所有模块复用）<br>每个类仅有一个引起变化的原因。 |

### 3.2 O — 开闭原则 (Open/Closed Principle)

| 检查项 | 分析 |
|--------|------|
| **AI 原始设计** | 内容审核逻辑直接硬编码在 `TreeHoleService.publish()` 方法中：`if (checkSensitiveWords(content)) reject();` |
| **是否违反** | **是 — 中等违反** |
| **违反说明** | 当需要新增审核方式时（如从纯关键词匹配升级为 AI 语义审核），必须修改 `TreeHoleService.publish()` 方法的源码。同样，`PartnerService` 和 `LoveService` 中也重复编写了相同的审核逻辑代码。任何审核规则的变更都需要修改至少 3 个 Service 类。 |
| **修正方案** | 引入策略模式（Strategy Pattern）：<br>① 定义 `ReviewStrategy` 接口，含 `review(content): ReviewResult` 方法<br>② 实现 `TextReviewStrategy`（敏感词）、`ImageReviewStrategy`（图片审核）等具体策略<br>③ `ReviewContext` 类通过组合持有策略对象，运行时动态选择<br>扩展新策略时只需新增类，无需修改已有 Service 代码。 |

### 3.3 L — 里氏替换原则 (Liskov Substitution Principle)

| 检查项 | 分析 |
|--------|------|
| **AI 原始设计** | `NormalUser` 子类重写 `login()` 方法，增加了"必须为校内用户"的前置条件校验（父类 `BaseUser` 没有此限制）。代码：`if (!isUniversityStudent()) throw new NotUniversityStudentException();` |
| **是否违反** | **是 — 严重违反** |
| **违反说明** | 父类 `BaseUser.login()` 的契约是：提供正确的凭证即可登录。子类 `NormalUser` 强化了前置条件（增加了"必须为校内用户"的要求），导致在期望 `BaseUser` 的场景下，替换为 `NormalUser` 后行为不一致。如果系统某处使用了多态，将 `NormalUser` 当作 `BaseUser` 调用 `login()`，会因为额外的异常而失败。 |
| **修正方案** | 两种修正方式（任选其一）：<br>① **方案A**：不在 `login()` 中做校内身份校验，将身份校验放在注册/认证阶段（`certify()` 方法）。`login()` 恢复为纯粹的凭证验证。<br>② **方案B**：如果确实需要在登录时区分，则不使用继承，改为使用组合：`BaseUser` 持有 `LoginPolicy` 接口，校内用户使用 `UniversityLoginPolicy`，管理员使用 `AdminLoginPolicy`。 |

### 3.4 I — 接口隔离原则 (Interface Segregation Principle)

| 检查项 | 分析 |
|--------|------|
| **AI 原始设计** | 定义了 `IContentManager` 接口，包含 7 个方法：`publishPost()`, `publishComment()`, `publishPartnerRequest()`, `sendLoveHeart()`, `reviewContent()`, `deleteContent()`, `pushNotification()` |
| **是否违反** | **是 — 严重违反** |
| **违反说明** | 这是一个"胖接口"的典型案例。实现该接口的类被迫实现所有方法：<br>① `NormalUser` 实现了 `reviewContent()` 方法但内部为空（普通用户不需要审核功能）<br>② `Admin` 实现了 `publishPost()` 和 `sendLoveHeart()` 但内部为空（管理员不需要发布内容）<br>接口的使用者被迫依赖他们不需要的方法，增加了耦合和误用风险。 |
| **修正方案** | 将"胖接口"拆分为 3 个"瘦接口"：<br>① `IPublisher` — `publishPost()`, `publishComment()`, `publishPartnerRequest()`（面向普通用户）<br>② `IReviewer` — `reviewContent()`, `deleteContent()`（面向审核员/管理员）<br>③ `INotifiable` — `sendHeart()`（面向交友模块用户）<br>每个实现类只实现自己真正需要的接口，调用方也只依赖需要的接口。 |

### 3.5 D — 依赖倒转原则 (Dependency Inversion Principle)

| 检查项 | 分析 |
|--------|------|
| **AI 原始设计** | `PartnerMatchService` 中直接实例化和使用具体类：`private MySQLDatabase db = new MySQLDatabase();` |
| **是否违反** | **是 — 严重违反** |
| **违反说明** | 高层模块（业务逻辑 Service）直接依赖低层模块（具体数据库实现）：<br>① 如果需要更换数据库（MySQL → PostgreSQL），必须修改所有 Service 类的源码<br>② 单元测试无法对 Service 进行隔离测试（必须连接真实数据库）<br>③ `SmsService` 中也直接依赖了具体第三方 SDK，更换短信提供商同样困难 |
| **修正方案** | ① 定义 `UserRepository` 等持久化接口（面向接口编程）<br>② Service 通过构造函数注入方式依赖接口，而非具体实现<br>③ 通过依赖注入容器（如 Spring IoC）管理对象创建和注入<br>④ 同样的方法应用于 `SmsService` — 定义 `ISmsProvider` 接口，具体提供商作为实现注入 |

---

## 四、AI 设计违规汇总

| 序号 | 违反的原则 | 严重程度 | 违规描述 | 是否已修正 |
|------|-----------|---------|---------|-----------|
| 1 | S - 单一职责 | 严重 | TreeHoleService 包含业务+审核+通知三个职责 | ✅ 已拆分为 3 个独立 Service |
| 2 | S - 单一职责 | 中等 | User 类包含隐私设置属性（散落字段） | ✅ 已提取 PrivacySettings 组合类 |
| 3 | O - 开闭原则 | 中等 | 审核逻辑硬编码，新增审核方式需改源码 | ✅ 已引入策略模式 |
| 4 | L - 里氏替换 | 严重 | NormalUser.login() 强化了父类前置条件 | ✅ 校验移至认证阶段 |
| 5 | I - 接口隔离 | 严重 | IContentManager 胖接口含 7 个方法 | ✅ 已拆分为 3 个瘦接口 |
| 6 | D - 依赖倒转 | 严重 | Service 直接依赖具体 MySQLDatabase 类 | ✅ 已引入 Repository 接口 |
| 7 | D - 依赖倒转 | 中等 | SmsService 直接依赖具体第三方 SDK | ✅ 已引入 ISmsProvider 接口 |

**统计：AI 原始设计中共有 7 处违反 SOLID 原则，其中 4 处为严重级别，3 处为中等级别。**

---

## 五、关键发现与总结

### 5.1 AI 设计的主要问题类型

1. **职责边界模糊：** AI 倾向于把相关功能"堆"进同一个类，缺乏对"引起变化的原因"的深入分析
2. **过度依赖具体实现：** AI 不擅长主动应用"面向接口编程"，往往走到具体类
3. **接口设计粗糙：** AI 倾向于创建大而全的接口，而非小而专的接口
4. **继承滥用：** AI 在没有充分验证 LSP 的情况下使用继承，容易制造不合理的父子关系

### 5.2 团队审查经验

- SOLID 原则的检查顺序建议：**S → D → I → L → O**（从最容易被 AI 违反的开始）
- 单一职责（S）是最容易被违反的，应作为重点检查项
- AI 生成的类图中约 40%~60% 的类存在或多或少的 SOLID 违规
- 需要人工审查来发现并修正这些问题，人类在设计层面具有不可替代性
