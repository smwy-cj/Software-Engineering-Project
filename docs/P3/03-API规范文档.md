# API 规范文档

> "青隅"校园互助服务平台 RESTful API 接口规范 v1.0
> 基于 P2 ADR-003（前后端分离 + RESTful + JWT 身份认证）设计

---

## 一、API 设计总则

### 1.1 基础信息

| 项目 | 说明 |
|------|------|
| 协议 | HTTPS |
| 基础路径 | `/api/v1` |
| 数据格式 | JSON |
| 字符编码 | UTF-8 |
| 认证方式 | Bearer Token (JWT)，对齐 P2 ADR-003 |
| 部署方式 | 本地 JAR 包，单服务器（对齐 P2 最简架构决策） |

### 1.2 通用规范

**请求头：**
```
Authorization: Bearer <token>         # 需要认证的请求
Content-Type: application/json         # JSON 请求体
Accept: application/json               # 期望 JSON 响应
```

**分页参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | integer | 否 | 1 | 页码（从 1 开始） |
| size | integer | 否 | 20 | 每页条数（最大 50） |
| sort | string | 否 | "createdAt" | 排序字段 |
| order | string | 否 | "desc" | 排序方向（asc/desc） |

**通用响应格式：**
```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1700000000000
}
```

**通用错误响应格式：**
```json
{
  "code": 40001,
  "message": "参数校验失败",
  "errors": [
    { "field": "phone", "message": "手机号格式不正确" }
  ],
  "timestamp": 1700000000000
}
```

### 1.3 错误码定义

| 错误码 | HTTP状态码 | 说明 |
|--------|-----------|------|
| 200 | 200 | 请求成功 |
| 40001 | 400 | 参数校验失败 |
| 40002 | 400 | 请求参数格式错误 |
| 40003 | 400 | 业务规则校验失败 |
| 40101 | 401 | 未登录 / Token 无效 |
| 40102 | 401 | Token 已过期 |
| 40103 | 401 | 账号已被锁定 |
| 40301 | 403 | 权限不足 |
| 40302 | 403 | 账号未实名认证 |
| 40303 | 403 | 账号被禁言 |
| 40401 | 404 | 资源不存在 |
| 40901 | 409 | 资源冲突（如重复注册） |
| 41301 | 413 | 上传文件过大 |
| 42201 | 422 | 内容审核不通过 |
| 42901 | 429 | 请求过于频繁 |
| 50001 | 500 | 服务器内部错误 |
| 50002 | 500 | 第三方服务异常 |

---

## 二、核心接口定义

### 2.1 用户注册

**接口描述：** 新用户通过手机号注册平台账号。

| 项目 | 说明 |
|------|------|
| URL | `/api/v1/auth/register` |
| Method | POST |
| 认证 | 否 |

**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| phone | string | 是 | 手机号（11位） |
| smsCode | string | 是 | 短信验证码（6位） |
| username | string | 是 | 用户名（4-16位，中英文数字组合） |
| password | string | 是 | 密码（≥8位，含字母和数字） |
| agreeTerms | boolean | 是 | 是否同意用户协议和隐私政策 |

**请求示例：**
```json
{
  "phone": "13800138000",
  "smsCode": "123456",
  "username": "小明同学",
  "password": "Abc12345",
  "agreeTerms": true
}
```

**成功响应 (201)：**
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": 10001,
    "username": "小明同学",
    "phone": "138****8000",
    "registerTime": "2026-05-13T10:30:00Z"
  }
}
```

**失败响应示例：**
```json
// 手机号已注册 (409)
{
  "code": 40901,
  "message": "该手机号已注册",
  "errors": [{ "field": "phone", "message": "手机号已绑定现有账号" }]
}

// 验证码错误 (400)
{
  "code": 40003,
  "message": "短信验证码错误或已过期"
}

// 用户名已存在 (409)
{
  "code": 40901,
  "message": "该用户名已被占用",
  "errors": [{ "field": "username", "message": "请更换用户名" }]
}
```

---

### 2.2 用户登录

**接口描述：** 用户通过手机号+密码或手机号+验证码登录。

| 项目 | 说明 |
|------|------|
| URL | `/api/v1/auth/login` |
| Method | POST |
| 认证 | 否 |

**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| phone | string | 是 | 手机号 |
| loginType | string | 是 | 登录方式：`password` / `smsCode` |
| password | string | 否 | 密码（loginType=password 时必填） |
| smsCode | string | 否 | 短信验证码（loginType=smsCode 时必填） |
| captcha | string | 是 | 图形验证码 |
| captchaId | string | 是 | 图形验证码 ID |
| rememberMe | boolean | 否 | 是否记住登录（默认 false，有效期 7 天） |

**请求示例：**
```json
{
  "phone": "13800138000",
  "loginType": "password",
  "password": "Abc12345",
  "captcha": "A3Bx",
  "captchaId": "captcha-uuid-123",
  "rememberMe": false
}
```

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 7200,
    "userInfo": {
      "userId": 10001,
      "username": "小明同学",
      "avatar": "https://cdn.example.com/avatars/default.png",
      "certStatus": "CERTIFIED",
      "accountStatus": "NORMAL"
    }
  }
}
```

**失败响应示例：**
```json
// 登录失败 (401)
{
  "code": 40101,
  "message": "手机号或密码错误，还剩 2 次尝试机会"
}

// 账号已锁定 (401)
{
  "code": 40103,
  "message": "账号已因多次登录失败被锁定，请 15 分钟后重试"
}
```

---

### 2.3 发布搭子需求

**接口描述：** 已实名认证用户发布搭子需求。

| 项目 | 说明 |
|------|------|
| URL | `/api/v1/partner/requests` |
| Method | POST |
| 认证 | 是（需实名认证） |

**请求头：**
```
Authorization: Bearer <token>
```

**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 是 | 搭子类型：`study` / `sport` / `meal` / `exam` / `travel` / `other` |
| description | string | 是 | 需求描述（10-200字） |
| conditions | object | 是 | 匹配条件 |
| conditions.grade | string | 否 | 年级要求 |
| conditions.major | string | 否 | 专业要求 |
| conditions.gender | string | 否 | 性别要求：`male` / `female` / `any` |
| conditions.timeSlots | array | 否 | 空余时间段 |
| conditions.location | string | 否 | 线下地点 |
| validDays | integer | 否 | 有效天数（1-7，默认 3） |
| maxMembers | integer | 否 | 最大人数（1-10，默认 5） |
| visibility | string | 否 | 可见范围：`all` / `sameSchool` / `sameMajor`（默认 sameSchool） |
| images | array(file) | 否 | 相关图片（最多 2 张，≤5MB/张） |

**请求示例：**
```json
{
  "type": "study",
  "description": "寻找一起备考六级的学习搭子，每天下午图书馆自习，可以互相监督打卡。",
  "conditions": {
    "grade": "2024级",
    "major": "计算机科学与技术",
    "gender": "any",
    "timeSlots": ["14:00-17:00", "19:00-21:00"],
    "location": "图书馆三楼自习区"
  },
  "validDays": 5,
  "maxMembers": 3,
  "visibility": "sameSchool"
}
```

**成功响应 (201)：**
```json
{
  "code": 200,
  "message": "搭子需求发布成功，待审核",
  "data": {
    "requestId": 20001,
    "status": "PENDING",
    "publishTime": "2026-05-13T11:00:00Z",
    "expireTime": "2026-05-18T11:00:00Z"
  }
}
```

**失败响应示例：**
```json
// 未实名认证 (403)
{
  "code": 40302,
  "message": "请先完成实名认证后再发布搭子需求"
}

// 内容违规 (422)
{
  "code": 42201,
  "message": "内容包含违规信息，请修改后重新发布",
  "errors": [{ "field": "description", "message": "包含敏感词汇" }]
}
```

---

### 2.4 浏览需求列表（含筛选）

**接口描述：** 分页浏览搭子需求列表，支持多条件筛选和排序。

| 项目 | 说明 |
|------|------|
| URL | `/api/v1/partner/requests` |
| Method | GET |
| 认证 | 是 |

**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 否 | 搭子类型筛选：`study` / `sport` / `meal` / `exam` / `travel` / `other` |
| grade | string | 否 | 年级筛选 |
| major | string | 否 | 专业筛选 |
| gender | string | 否 | 性别要求筛选 |
| keyword | string | 否 | 关键词搜索（匹配描述内容） |
| status | string | 否 | 状态筛选：`published` / `expired`（默认 published） |
| sortBy | string | 否 | 排序字段：`publishTime` / `matchCount`（默认 publishTime） |
| page | integer | 否 | 页码（默认 1） |
| size | integer | 否 | 每页条数（默认 20，最大 50） |

**请求示例：**
```
GET /api/v1/partner/requests?type=study&grade=2024级&keyword=六级&sortBy=publishTime&page=1&size=20
```

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "requestId": 20001,
        "type": "study",
        "description": "寻找一起备考六级的学习搭子...",
        "conditions": {
          "grade": "2024级",
          "major": "计算机科学与技术",
          "gender": "any"
        },
        "publisherInfo": {
          "nickname": "小明同学",
          "grade": "2024级",
          "major": "计算机科学与技术",
          "avatar": "https://cdn.example.com/avatars/u10001.png"
        },
        "maxMembers": 3,
        "currentMatches": 1,
        "validDays": 5,
        "publishTime": "2026-05-13T11:00:00Z",
        "expireTime": "2026-05-18T11:00:00Z"
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 47,
    "totalPages": 3
  }
}
```

---

### 2.5 发起匹配申请（接单）

**接口描述：** 用户浏览搭子需求后，向发布者发起匹配申请。

| 项目 | 说明 |
|------|------|
| URL | `/api/v1/partner/requests/{requestId}/apply` |
| Method | POST |
| 认证 | 是（需实名认证） |

**路径参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| requestId | integer | 是 | 搭子需求 ID |

**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message | string | 否 | 申请附言（10-100字） |

**请求示例：**
```json
{
  "message": "我也是计算机专业2024级的，六级目标550+，一起加油！"
}
```

**成功响应 (201)：**
```json
{
  "code": 200,
  "message": "匹配申请已发送",
  "data": {
    "matchId": 30001,
    "requestId": 20001,
    "status": "PENDING",
    "applyTime": "2026-05-13T12:00:00Z"
  }
}
```

**失败响应示例：**
```json
// 不能申请自己的需求 (400)
{
  "code": 40003,
  "message": "不能向自己发布的搭子需求发起申请"
}

// 需求已过期 (400)
{
  "code": 40003,
  "message": "该搭子需求已过期，无法申请"
}

// 人数已满 (400)
{
  "code": 40003,
  "message": "该搭子需求匹配人数已满"
}
```

---

### 2.6 查看匹配详情

**接口描述：** 查看某个匹配申请的详细信息，包括双方基本信息和状态。

| 项目 | 说明 |
|------|------|
| URL | `/api/v1/partner/matches/{matchId}` |
| Method | GET |
| 认证 | 是 |

**路径参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| matchId | integer | 是 | 匹配记录 ID |

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "matchId": 30001,
    "status": "ACCEPTED",
    "request": {
      "requestId": 20001,
      "type": "study",
      "description": "寻找一起备考六级的学习搭子...",
      "conditions": {
        "grade": "2024级",
        "major": "计算机科学与技术",
        "timeSlots": ["14:00-17:00"]
      }
    },
    "publisher": {
      "userId": 10001,
      "nickname": "小明同学",
      "grade": "2024级",
      "major": "计算机科学与技术"
    },
    "applicant": {
      "userId": 10002,
      "nickname": "小红同学",
      "grade": "2024级",
      "major": "软件工程"
    },
    "applyMessage": "我也是计算机专业2024级的，六级目标550+，一起加油！",
    "applyTime": "2026-05-13T12:00:00Z",
    "responseTime": "2026-05-13T13:00:00Z",
    "canChat": true
  }
}
```

**失败响应示例：**
```json
// 无权查看 (403)
{
  "code": 40301,
  "message": "您不是该匹配的参与方，无权查看详情"
}
```

---

### 2.7 提交评价

**接口描述：** 搭子匹配结束后，双方可互相评价。

| 项目 | 说明 |
|------|------|
| URL | `/api/v1/partner/matches/{matchId}/reviews` |
| Method | POST |
| 认证 | 是 |

**路径参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| matchId | integer | 是 | 匹配记录 ID |

**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| rating | integer | 是 | 评分（1-5 星） |
| content | string | 是 | 评价内容（10-100字） |

**请求示例：**
```json
{
  "rating": 5,
  "content": "非常靠谱的学习搭子！每天准时到图书馆，互相监督效率很高。"
}
```

**成功响应 (201)：**
```json
{
  "code": 200,
  "message": "评价提交成功",
  "data": {
    "reviewId": 40001,
    "rating": 5,
    "content": "非常靠谱的学习搭子！每天准时到图书馆，互相监督效率很高。",
    "createTime": "2026-05-20T15:00:00Z"
  }
}
```

**失败响应示例：**
```json
// 重复评价 (400)
{
  "code": 40003,
  "message": "您已经对该搭子进行过评价"
}
```

---

### 2.8 树洞动态列表

**接口描述：** 分页获取树洞公开动态列表。

| 项目 | 说明 |
|------|------|
| URL | `/api/v1/treehole/posts` |
| Method | GET |
| 认证 | 否（未登录仅可浏览，不可互动） |

**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| category | string | 否 | 分类：`emotion` / `study` / `life` / `fun` / `other` |
| keyword | string | 否 | 关键词搜索 |
| sortBy | string | 否 | 排序：`publishTime` / `hot`（热度=点赞+评论数，默认 publishTime） |
| page | integer | 否 | 页码（默认 1） |
| size | integer | 否 | 每页条数（默认 20） |

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      {
        "postId": 50001,
        "anonymousName": "匿名小友327",
        "content": "期末周太累了...图书馆位置都抢不到",
        "category": "study",
        "images": [],
        "likeCount": 42,
        "commentCount": 8,
        "publishTime": "2026-05-13T09:00:00Z",
        "likedByMe": false
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 230,
    "totalPages": 12
  }
}
```

---

### 2.9 发送短信验证码

**接口描述：** 向指定手机号发送短信验证码。

| 项目 | 说明 |
|------|------|
| URL | `/api/v1/auth/sms-code` |
| Method | POST |
| 认证 | 否 |

**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| phone | string | 是 | 手机号（11位） |
| scene | string | 是 | 使用场景：`register` / `login` / `resetPassword` |

**请求示例：**
```json
{
  "phone": "13800138000",
  "scene": "register"
}
```

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "验证码已发送",
  "data": {
    "expiresIn": 300,
    "retryAfter": 60
  }
}
```

---

### 2.10 获取图形验证码

**接口描述：** 获取登录用图形验证码。

| 项目 | 说明 |
|------|------|
| URL | `/api/v1/auth/captcha` |
| Method | GET |
| 认证 | 否 |

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "captchaId": "captcha-uuid-123",
    "captchaImage": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."
  }
}
```

---

## 三、AI 辅助实验：API 审查记录

### 3.1 AI 生成的原始 API 问题记录

我们对 AI 根据需求文档自动生成的 OpenAPI 规范进行了审查，发现以下问题：

| 序号 | 问题类型 | 问题描述 | 严重程度 | 修正方案 |
|------|---------|---------|---------|---------|
| 1 | 命名不一致 | AI 使用了驼峰和下划线混用：`/partnerRequests` 和 `/partner_requests` 同时存在 | 中等 | 统一使用短横线命名：`/partner-requests` |
| 2 | 缺少错误处理 | AI 生成的响应只定义了 200，未定义 400/401/403/404 等错误响应 | 严重 | 为每个接口补充完整的错误码和错误响应格式 |
| 3 | 参数校验不完整 | 注册接口未校验密码强度（AI 未设置最小长度） | 严重 | 增加密码复杂度校验：≥8位，含字母+数字 |
| 4 | 参数校验不完整 | 发布接口未限制内容长度（可发布任意长文本，存在攻击风险） | 中等 | 增加字符串长度限制（minLength/maxLength） |
| 5 | 安全问题 | 登录接口未要求验证码，存在暴力破解风险 | 严重 | 增加图形验证码参数，连续失败锁定机制 |
| 6 | 安全问题 | AI 设计的响应中直接返回了用户手机号明文 | 严重 | 敏感字段脱敏处理：`138****8000` |
| 7 | 缺少分页 | 列表接口未设计分页，可能一次返回海量数据 | 中等 | 统一增加 page/size 分页参数 |
| 8 | 认证缺失 | 发布内容的接口未标注需要认证 Token | 严重 | 标注认证要求，明确 Token 传递方式 |

### 3.2 审查总结

AI 生成的 API 规范在**接口覆盖完整性**方面表现良好（覆盖了需求文档中的所有核心功能），但在以下三个方面存在明显不足：
1. **安全性考虑不足**（无认证标注、无验证码、明文返回敏感信息）
2. **错误处理不完整**（只定义了成功路径）
3. **命名和格式一致性差**
