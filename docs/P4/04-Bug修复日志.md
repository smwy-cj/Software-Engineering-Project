# Bug 修复日志

> 项目：CampusHub（青隅） | 阶段：P4 编码开发 | 更新日期：2026-06-06

---

## 一、修复记录

### BUG-001：JWT Token 验证失败导致认证接口返回 401

| 项目 | 内容 |
|------|------|
| 发现时间 | 2026-05-14 |
| 问题现象 | 登录成功后，前端携带 `Authorization: Bearer <token>` 请求需要认证的接口，后端返回401 |
| 复现步骤 | 登录获取token；请求 `/api/v1/partner/requests` 或 `/api/v1/notifications`；观察响应状态 |
| 根因分析 | 开发密钥长度不足时，HS256无法安全生成HMAC签名密钥，导致token校验失败 |
| 修复方案 | 在 `application.yml` 中使用长度满足256 bit要求的开发密钥 |
| 修复文件 | `backend/src/main/resources/application.yml` |
| 验证结果 | 登录后受保护接口可正常访问；后端集成测试通过 |
| AI 辅助 | AI快速指出HS256密钥长度要求，人工确认并验证 |

---

### BUG-002：管理员接口权限校验失败

| 项目 | 内容 |
|------|------|
| 发现时间 | 2026-05-14 |
| 问题现象 | 管理员账号登录后访问 `/api/v1/admin/**` 返回403 |
| 复现步骤 | 使用 `13800138000 / Admin123` 登录；进入管理后台；请求待审核列表 |
| 根因分析 | `SecurityConfig` 使用 `hasRole("ADMIN")`，需要认证对象具有 `ROLE_ADMIN`；过滤器初稿只写入用户ID，未写入角色权限 |
| 修复方案 | JWT中保留role claim；`JwtAuthenticationFilter` 解析role并写入 `SimpleGrantedAuthority("ROLE_" + role)` |
| 修复文件 | `backend/src/main/java/com/campushub/security/JwtAuthenticationFilter.java`、`backend/src/main/java/com/campushub/config/SecurityConfig.java` |
| 验证结果 | 管理员可访问后台；普通用户访问被拒绝；前端路由守卫测试通过 |
| AI 辅助 | AI提醒 `hasRole` 与 `ROLE_` 前缀的关系，人工补齐实现 |

---

### BUG-003：Controller 无法获取当前用户 ID

| 项目 | 内容 |
|------|------|
| 发现时间 | 2026-05-14 |
| 问题现象 | Controller中 `@RequestAttribute("userId")` 获取到空值，业务接口报错 |
| 复现步骤 | 携带有效token请求发布树洞、发布搭子、通知已读等接口 |
| 根因分析 | JWT过滤器只设置了 `SecurityContextHolder`，但Controller的实现依赖request attribute |
| 修复方案 | 在 `JwtAuthenticationFilter#doFilterInternal` 中增加 `request.setAttribute("userId", userId)` |
| 修复文件 | `backend/src/main/java/com/campushub/security/JwtAuthenticationFilter.java` |
| 验证结果 | Controller可稳定获取用户ID；相关接口集成测试通过 |
| AI 辅助 | 人工定位，AI辅助确认修复方式 |

---

### BUG-004：搭子匹配申请可重复提交

| 项目 | 内容 |
|------|------|
| 发现时间 | 2026-05-14 |
| 问题现象 | 同一用户可对同一搭子需求发起多次申请 |
| 复现步骤 | 用户B对用户A的同一搭子需求提交申请；再次提交同一申请 |
| 根因分析 | Service层缺少重复申请检查；仅依赖数据库唯一约束时，错误提示不友好且并发边界不清晰 |
| 修复方案 | `PartnerService.applyMatch` 增加重复申请拦截；保留数据库唯一约束兜底 |
| 修复文件 | `backend/src/main/java/com/campushub/service/PartnerService.java`、`PartnerMatch` 相关实体约束 |
| 验证结果 | 重复申请返回“您已申请过该需求，请勿重复申请”；`PartnerServiceTest` 通过 |
| AI 辅助 | AI建议应用层检查，人工补充数据库兜底思路 |

---

### BUG-005：前端搭子申请使用原生 prompt，交互不可控

| 项目 | 内容 |
|------|------|
| 发现时间 | 2026-05-27 |
| 问题现象 | 搭子申请入口使用浏览器原生 `prompt`，无法做字数提示、取消状态和统一视觉反馈 |
| 复现步骤 | 进入搭子列表，点击申请加入 |
| 根因分析 | 初版页面为了快速联调直接调用原生弹窗，未复用应用内表单状态 |
| 修复方案 | 改为页面内申请表单/弹层，由Vue状态控制输入、提交、取消和Toast提示 |
| 修复文件 | `frontend/src/views/Partner.vue`、`frontend/src/views/user-experience-regressions.test.js` |
| 验证结果 | 前端体验回归测试覆盖“不使用原生prompt” |
| AI 辅助 | AI生成回归测试断言，人工确认交互路径 |

---

### BUG-006：审核结果通知无法稳定跳转到详情

| 项目 | 内容 |
|------|------|
| 发现时间 | 2026-06-01 |
| 问题现象 | 通知中心点击审核相关通知时，结果文案和跳转目标不一致 |
| 复现步骤 | 管理员审核内容；用户进入通知中心查看审核结果并点击查看 |
| 根因分析 | 前端对审核结果通知类型和目标路径处理分散，缺少统一归一化 |
| 修复方案 | 在通知页面归一化审核通知展示文本和目标路由；补充体验回归测试 |
| 修复文件 | `frontend/src/views/Notifications.vue`、`frontend/src/views/user-experience-regressions.test.js` |
| 验证结果 | 前端回归测试覆盖通知归一化与详情入口 |
| AI 辅助 | AI辅助生成测试，人工核对真实路由 |

---

## 二、统计汇总

| 指标 | 数值 |
|------|------|
| 总修复Bug数 | 6 |
| AI辅助发现/定位 | 5 |
| AI方案可直接采用 | 2 |
| 需要人工补充或重构 | 4 |
| 覆盖测试 | 后端51个测试用例；前端16个测试用例 |

---

## 三、经验总结

- 框架类问题适合先交给AI缩小范围，例如JWT、Spring Security、CI配置。
- 业务类问题必须由人工确认根因，例如重复申请、权限边界、审核状态流转。
- 每个修复都需要对应验证方式：后端服务规则放入单元测试，接口链路放入集成测试，前端交互问题放入Vitest回归测试。
