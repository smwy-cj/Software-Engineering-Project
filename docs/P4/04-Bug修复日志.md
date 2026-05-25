# Bug 修复日志

> 项目：CampusHub（青隅）| 阶段：P4 编码开发

---

## Bug 修复记录

### BUG-001：JWT Token 验证失败导致认证接口全部返回401

| 项目 | 内容 |
|------|------|
| **发现时间** | 2026-05-14 |
| **问题现象** | 前端带着Bearer Token请求任何需要认证的接口，均返回401 Unauthorized |
| **复现步骤** | 1. 登录获取token 2. 携带token请求 /api/v1/partner/requests 3. 返回401 |
| **根因分析** | `application.yml` 中 `app.jwt.secret` 配置的密钥字符串长度不足32字节（256位），导致 `Keys.hmacShaKeyFor()` 无法生成有效的HMAC密钥，JWT签名验证失败 |
| **修复方案** | 将密钥长度增加到至少256位（32字节）的随机字符串 |
| **修复文件** | `backend/src/main/resources/application.yml` |
| **验证结果** | 修复后JWT token正常签发和验证，接口返回200 ✅ |
| **AI辅助** | AI直接指出密钥长度问题，人工确认后修复 |

---

### BUG-002：SecurityConfig 路径配置导致登录接口403

| 项目 | 内容 |
|------|------|
| **发现时间** | 2026-05-14 |
| **问题现象** | POST /api/v1/auth/login 返回403状态码，提示"权限不足" |
| **复现步骤** | 直接发送登录请求，无需token |
| **根因分析** | SecurityConfig中`.requestMatchers("/api/v1/auth/**").permitAll()`正确配置了，但Spring Security的过滤器链中JwtAuthenticationFilter在未登录时也尝试解析token，导致认证失败转为403 |
| **修复方案** | 确认permitAll路径配置正确，调整SecurityConfig中异常处理，对未认证请求返回401而非403 |
| **修复文件** | `backend/src/main/java/com/campushub/config/SecurityConfig.java` |
| **验证结果** | 修复后登录接口正常返回200，携带正确token ✅ |
| **AI辅助** | AI提示检查SecurityConfig路径匹配和异常处理，人工修正 |

---

### BUG-003：搭子匹配申请可重复提交

| 项目 | 内容 |
|------|------|
| **发现时间** | 2026-05-14 |
| **问题现象** | 用户对同一搭子需求可成功发起多次匹配申请，数据库中产生重复记录 |
| **复现步骤** | 1. 用户A浏览搭子需求 2. 对需求#1发起申请（成功）3. 再次对需求#1发起申请（也应成功，产生重复记录） |
| **根因分析** | Service层的`applyMatch()`方法缺少重复申请校验，Entity中虽有`@UniqueConstraint`但实际表结构未生效（H2 DDL自动创建时唯一约束名可能不同） |
| **修复方案** | 1. Service层增加`existsByRequestIdAndApplicantId`检查 2. 确认数据库唯一约束生效 |
| **修复文件** | `backend/src/main/java/com/campushub/service/PartnerService.java` |
| **验证结果** | 修复后重复申请返回"您已申请过该需求，请勿重复申请"错误 ✅ |
| **AI辅助** | AI仅建议应用层检查，人工补充确认数据库约束 |

---

### BUG-004：Spring Security "role" 验证导致Admin接口无法访问

| 项目 | 内容 |
|------|------|
| **发现时间** | 2026-05-14 |
| **问题现象** | 管理员用户访问 /api/v1/admin/** 接口始终返回403 |
| **复现步骤** | 1. 使用管理员账号登录 2. 请求 /api/v1/admin/reviews/pending 3. 返回403 |
| **根因分析** | SecurityConfig中`.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")`要求用户有ROLE_ADMIN权限，但JwtAuthenticationFilter中创建Authentication时未设置任何authority，导致所有用户都没有ADMIN角色 |
| **修复方案** | 暂时放宽Admin接口的权限控制为`authenticated()`，在Controller层通过`AdminRepository.existsByUserId()`进行权限校验 |
| **修复文件** | `backend/src/main/java/com/campushub/config/SecurityConfig.java`、`AdminService.java` |
| **验证结果** | 修复后管理员可正常访问Admin接口，普通用户被Service层拦截 ✅ |
| **AI辅助** | AI建议修改SecurityConfig，人工实施并提供Service层权限校验 |

---

### BUG-005：Controller中@RequestAttribute无法获取userId

| 项目 | 内容 |
|------|------|
| **发现时间** | 2026-05-14 |
| **问题现象** | Controller中通过`@RequestAttribute("userId")`获取当前用户ID始终为null |
| **复现步骤** | 请求需要认证的接口，Controller中获取不到userId |
| **根因分析** | JwtAuthenticationFilter中通过`SecurityContextHolder`设置了Authentication，但没有通过`request.setAttribute()`设置userId |
| **修复方案** | 在JwtAuthenticationFilter的`doFilterInternal`中添加`request.setAttribute("userId", userId)` |
| **修复文件** | `backend/src/main/java/com/campushub/security/JwtAuthenticationFilter.java` |
| **验证结果** | 修复后Controller可以正常获取userId ✅ |
| **AI辅助** | 人工发现此问题，AI建议添加request.setAttribute |

---

## 统计汇总

| 指标 | 数值 |
|------|------|
| 总修复Bug数 | 5 |
| AI辅助发现/定位 | 4 (80%) |
| AI方案直接可用 | 2 (40%) |
| 人工补充修改 | 3 (60%) |
| 平均修复耗时 | ~20min/Bug |
