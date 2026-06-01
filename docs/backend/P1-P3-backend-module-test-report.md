# 后端模块 P1-P3 功能测试核对报告

测试日期：2026-05-30

## 1. 测试范围与依据

本次核对范围为 `backend/src/main/java/com/campushub` 下 8 个后端模块：

- 用户认证与实名认证：Auth / User
- 树洞：TreeHole
- 找搭子：Partner
- 恋爱互助：Love
- 消息通知：Notification
- 内容审核与管理员：Admin / Review
- 反馈建议：Feedback
- 通用安全、异常、分页、认证

需求依据：

- `docs/P1/需求规格说明书.md`
- `docs/P1/三个核心用例描述.md`
- `docs/P2/架构设计文档.md`
- `docs/P3/03-API规范文档.md`
- `docs/P3/05-详细设计文档-整合版.md`

## 2. 已执行测试

### 2.1 服务层单元测试

命令：

```powershell
mvn '-Dtest=UserServiceTest,TreeHoleServiceTest,PartnerServiceTest' test
```

结果：

- `UserServiceTest`：7 个通过
- `TreeHoleServiceTest`：3 个通过
- `PartnerServiceTest`：5 个通过
- 合计：15 个通过，0 失败，0 错误

结论：当前已有单元测试覆盖了用户注册/登录少量路径、树洞发帖/点赞少量路径、搭子发布/申请/评价少量路径，但未覆盖恋爱、通知、管理员审核、反馈、接口权限、参数校验、内容审核、匿名隐私、状态流转等大量 P1/P2 规则。

### 2.2 全量后端测试

命令：

```powershell
mvn test
```

结果：

- 单元测试通过：15 个
- 集成测试错误：2 个
- 总计：18 个测试，0 失败，2 错误

失败根因：

- `CoreFlowIntegrationTest` 的 `@BeforeEach` 每个用例都插入固定手机号 `13900139001` / `13900139002`。
- H2 内存库在同一个 Spring 测试上下文中复用，前一个用例数据未清理。
- 后续用例插入相同手机号，触发 `tb_user.phone` 唯一约束。

修改方案：

- 在集成测试 `@BeforeEach` 前执行仓库清理，至少清理 `partner_match`、`partner_req`、`user_cert`、`user` 等关联数据。
- 或给测试用户手机号追加随机后缀/当前测试名，避免唯一键冲突。
- 或在集成测试类上使用 `@DirtiesContext`，但成本更高，不建议作为首选。

## 3. 逐模块结论

| 模块 | P1/P2/P3 符合度 | 主要结论 |
|---|---:|---|
| 用户认证 | 部分符合 | 基础注册、密码登录、JWT 返回存在；短信、图形验证码、实名认证、密码找回、多端挤下线等需求多数为 mock 或缺失 |
| 树洞 | 部分符合 | 列表、发布、点赞、评论存在；实名认证、禁言、字数、内容审核、图片审核、匿名一致性、删除/编辑等需求缺失 |
| 找搭子 | 部分符合 | 发布、列表、申请、匹配详情、评价存在；发布状态直接 `PUBLISHED`，未进入审核；缺少同校/同专业可见性、条件筛选、接受/拒绝匹配、关系结束、聊天 |
| 恋爱互助 | 部分符合 | 资料、需求、心动、匹配列表存在；缺少资料审核、照片审核、交友需求审核、双向心动确认、聊天、礼物、安全提醒、举报 |
| 通知 | 部分符合 | 通知创建、列表、已读存在；类型筛选在内存分页后处理，分页总数不准；缺少消息设置、免打扰、多端推送 |
| 内容审核 | 不符合 P1 | 有 `ReviewRecord` 和管理员审核接口，但业务发布未生成待审核记录，审核结果也不会回写业务内容状态 |
| 管理员 | 部分符合且权限有阻塞 | Service 有管理员检查，但 Spring Security 使用 `hasRole("ADMIN")`，JWT 过滤器没有注入角色权限，管理员接口可能被安全层提前拒绝 |
| 反馈建议 | 部分符合 P2 | 提交、查询、处理存在；缺少截图上传、反馈编号唯一性保障、处理通知、过滤分页准确性 |

## 4. 关键问题与修改方案

### P0 阻塞：全量测试不可稳定运行

证据：

- `CoreFlowIntegrationTest.setUp` 重复插入固定手机号，触发唯一键冲突。

影响：

- 无法用全量测试验证后端回归，后续修改风险较高。

修改方案：

1. 为集成测试增加数据清理方法，按依赖顺序删除匹配、需求、认证、用户。
2. 或使用唯一测试数据生成器，按时间戳/UUID 生成手机号和用户名。
3. 为核心接口补充集成测试，覆盖 Auth、TreeHole、Partner、Love、Admin、Notification、Feedback。

### P1：内容审核流程未接入业务发布

证据：

- 树洞发布直接 `post.setStatus("PUBLISHED")`。
- 搭子发布直接 `entity.setStatus("PUBLISHED")`。
- 恋爱需求发布直接 `req.setStatus("PUBLISHED")`。
- 管理员审核只保存 `ReviewRecord`，未回写对应业务内容状态。

影响：

- 不符合 P1 “自动审核 + 人工审核 + 审核通过后发布”的核心要求。
- 违规内容可以绕过审核直接前台展示。

修改方案：

1. 新增 `ContentReviewService`，封装敏感词检测、图片审核占位、创建审核记录。
2. 发布树洞/搭子/恋爱内容时状态先置为 `PENDING`。
3. 管理员提交 `PASSED/REJECTED/WARNING` 后，根据 `contentType/contentId` 回写业务表状态。
4. 列表查询只返回 `PUBLISHED` 内容。
5. 增加回归测试：敏感词拦截、正常内容进入待审核、审核通过后可见、驳回后不可见。

### P1：认证与权限规则不完整

证据：

- 登录接口定义了 `captcha/captchaId` 字段，但 `UserService.login` 未校验图形验证码。
- 登录失败锁定阈值实现为 5 次，需求为 3 次。
- `rememberMe` 字段未影响 token 有效期。
- `JwtAuthenticationFilter` 创建认证对象时权限集合为空，而 `SecurityConfig` 对 `/api/v1/admin/**` 使用 `hasRole("ADMIN")`。

影响：

- 登录防刷、管理员后台、记住登录均不符合文档。

修改方案：

1. 增加验证码存储与校验组件，至少在当前 H2/MVP 中用内存 TTL Map。
2. 将登录失败阈值统一为配置值并按文档设为 3。
3. `rememberMe=true` 时签发 7 天 token 或 refresh token。
4. JWT 中加入角色声明，过滤器解析后注入 `ROLE_ADMIN`；或取消 URL 层 `hasRole`，统一由 `AdminService.checkAdmin` 控制，二选一。

### P1：树洞匿名与互动规则存在偏差

证据：

- 发布可选择 `anonymous=false` 并显示用户名。
- 评论匿名名使用真实用户名。
- 未检查实名认证、禁言状态、内容长度、是否允许评论/点赞。
- 热度排序只按 `likeCount`，未包含评论数。

影响：

- 违反树洞全程匿名和互动匿名要求。
- 未实名认证用户可发布/互动，禁言用户也可能发布/互动。

修改方案：

1. 树洞发布和评论强制生成匿名昵称，不接受前端 `anonymous=false`。
2. 发布、点赞、评论前检查 `UserCert.CERTIFIED` 和 `User.accountStatus != MUTED/BANNED`。
3. 增加内容长度校验：发帖 10-800 字，评论 1-100 字。
4. 增加 `allowComment/allowLike/visibility` 字段并在操作前校验。
5. 热度排序改为 `likeCount + commentCount`，可用派生字段或查询排序。

### P1：搭子匹配流程只实现了申请，没有接受/拒绝闭环

证据：

- 有 `applyMatch`，但没有发布者同意/拒绝接口。
- 评价接口未校验匹配状态是否 `ENDED` 或至少 `ACCEPTED`。
- 发布后直接 `PUBLISHED`，未进入审核队列。

影响：

- 文档要求的“拒绝匹配申请、确认搭子关系、结束关系、评价”无法形成完整业务闭环。

修改方案：

1. 增加 `PUT /api/v1/partner/matches/{id}`，支持 `ACCEPTED/REJECTED/CANCELED/ENDED` 状态流转。
2. 状态流转检查操作者身份：发布者可接受/拒绝，双方可结束。
3. 评价限制为 `ENDED` 或文档确认的可评价状态。
4. 搭子需求发布先进入 `PENDING`，审核通过后再进入匹配池。

### P1/P2：恋爱互助缺少双向匹配与安全能力

证据：

- `sendHeart` 每次创建单向 `PENDING`，没有检测反向心动并转为 `ACCEPTED`。
- `createLoveRequest` 直接 `PUBLISHED`。
- 资料照片、需求内容未审核。
- 缺少举报、安全提醒、聊天、礼物、解除关系接口。

影响：

- P1 的匹配逻辑和 P2 的交友安全要求未闭环。

修改方案：

1. 建模双向心动：发现双方互相 heart 后将匹配状态设为 `ACCEPTED`，并给双方通知。
2. 交友资料和交友需求接入审核状态。
3. 增加举报接口与管理员处理流。
4. 聊天/礼物可按 P4 范围拆分，当前至少补 HTTP 轮询消息接口或明确暂不实现。

### P1/P2：通知与反馈的分页过滤不准确

证据：

- 通知先查当前页，再在内存中过滤 `type/isRead`。
- 我的反馈先查当前页，再在内存中过滤 `status`。
- 管理反馈先按 `status` 查询，再在内存中过滤 `type`。

影响：

- 返回 `content` 数量、`totalElements` 与筛选条件不一致。
- 前端分页会出现空页、总数错误。

修改方案：

1. 在 Repository 层增加组合条件查询或 `JpaSpecificationExecutor`。
2. 所有筛选条件进入数据库查询，再分页。
3. 增加测试：多类型/多状态数据下分页总数必须准确。

### P1/P2：参数校验与错误响应存在缺陷

证据：

- `GlobalExceptionHandler.handleValidation` 构造了错误体，但返回 `body(null)`。
- TreeHole 和 Love 的部分请求使用 `Map<String,Object>`，缺少 DTO 校验。
- Partner 描述限制为 10-200 字，而 P1 需求为 10-800 字，P3 API 写的是 10-200 字，文档之间存在不一致。

影响：

- 参数错误时前端拿不到规范错误体。
- 多数接口可传入空内容或非法枚举，导致 500 或脏数据。

修改方案：

1. 修复 `handleValidation` 返回实际错误体。
2. 为树洞发布/评论、恋爱需求创建、管理员审核、处罚等请求补 DTO 和 `@Valid`。
3. 对 P1 与 P3 文档冲突处先统一需求口径，再改代码和测试；建议搭子描述、树洞正文按 P1 10-800，API 文档同步修正。

## 5. 建议修复顺序

1. 先修全量测试稳定性，保证 `mvn test` 可作为回归门禁。
2. 修异常响应和 DTO 校验，避免接口层脏数据进入业务。
3. 修认证/权限，尤其是管理员接口角色注入问题。
4. 接入统一内容审核流，覆盖树洞、搭子、恋爱三类核心发布。
5. 补齐树洞匿名、实名认证、禁言、长度、热度排序。
6. 补齐搭子和恋爱状态流转。
7. 修通知/反馈分页过滤。
8. 再扩展 P2/P3 功能：隐私设置、心理健康专栏、推荐、聊天、消息免打扰、账号注销等。

## 6. 后续测试建议

建议新增以下测试类：

- `AuthControllerIntegrationTest`：注册、登录、验证码、锁定、管理员权限。
- `TreeHoleFlowIntegrationTest`：认证用户发布、待审核、通过后可见、点赞评论匿名、禁言不可互动。
- `PartnerFlowIntegrationTest`：发布、审核、申请、同意、拒绝、结束、评价。
- `LoveFlowIntegrationTest`：资料完整度、需求审核、单向心动、双向匹配。
- `AdminReviewIntegrationTest`：待审核列表、审核详情、提交审核、状态回写。
- `NotificationFeedbackIntegrationTest`：通知已读、筛选分页、反馈处理通知。

