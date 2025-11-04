# 问卷系统 API 接口文档

## 基础信息

接口网址：http://52.14.58.34:8082/swagger-ui/index.html
- **Base URL**: `http://52.14.58.34:8082`
- **Content-Type**: `application/json`
- **认证方式**: Bearer Token (在请求头中添加 `Authorization: Bearer {accessToken}`)

### 统一响应格式

所有接口返回统一的 `Result<T>` 格式：

```json
{
  "code": 1, // 1表示成功，0或其他数字表示失败
  "msg": "成功", // 错误信息（失败时）
  "data": {}, // 响应数据（成功时）
  "success": true // 是否成功
}
```

---

## 一、用户接口 (`/api/user`) ⭐ 核心 API

### 1.1 用户登录

**接口**: `POST /api/user/login`

**描述**: 用户登录（支持 Firebase 认证）

**请求体**:

```json
{
  "firebaseToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**响应示例**:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "accessToken": "uuid-access-token",
    "refreshToken": "uuid-refresh-token",
    "userName": "user",
    "email": "user@example.com",
    "avatarUrl": "/avatars/xxx.jpg",
    "currentPoints": 150,
    "rank": "白银",
    "pointsToNextRank": 150
  },
  "success": true
}
```

---

### 1.2 用户注册

**接口**: `POST /api/user/register`

**描述**: 用户注册（支持 Firebase 认证）

**请求体**:

```json
{
  "firebaseToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userName": "user",
  "email": "user@example.com"
}
```

**响应**: 同登录接口（新用户默认积分 0，段位"青铜"）

---

### 1.3 第三方登录（支持 Google/Facebook/Twitter）

**接口**: `POST /api/user/third-party`

**描述**: 通过第三方账号登录（支持 Google、Facebook、Twitter，通过 Firebase）

**请求体**:

```json
{
  "tokenId": "firebase-id-token"
}
```

**说明**:

- 支持 Google、Facebook、Twitter 等第三方登录
- 所有第三方登录都通过 Firebase 返回统一的 ID Token
- 首次登录时自动注册新用户
- 从 token 中自动提取用户信息（邮箱、用户名、头像）

**响应**: 同登录接口

---

### 1.4 发送忘记密码邮件（缺少忘记密码界面）

**接口**: `POST /api/user/forgetSend`

**描述**: 发送忘记密码邮件

**请求体**:

```json
{
  "email": "user@example.com"
}
```

**响应**:

```json
{
  "code": 1,
  "msg": null,
  "data": "success",
  "success": true
}
```

---

### 1.5 重置密码

**接口**: `POST /api/user/resetPassword`

**描述**: 重置密码

**请求体**:

```json
{
  "token": "reset-token-from-email",
  "password": "newPassword123"
}
```

**响应**: 标准响应格式

---

### 1.6 刷新 Token

**接口**: `POST /api/user/refresh`

**描述**: 刷新访问令牌

**请求体**:

```json
{
  "refreshToken": "uuid-refresh-token"
}
```

**响应**: 同登录接口

---

### 1.7 修改用户名

**接口**: `PUT /api/user/name`

**描述**: 修改用户名

**请求头**: `Authorization: Bearer {accessToken}`

**请求体**:

```json
{
  "userName": "newUserName"
}
```

**响应**: 标准响应格式

---

### 1.8 修改邮箱

**接口**: `PUT /api/user/email`

**描述**: 修改邮箱

**请求头**: `Authorization: Bearer {accessToken}`

**请求体**:

```json
"newemail@example.com"
```

**响应**: 标准响应格式

---

### 1.9 修改密码（没做）

**接口**: `PUT /api/user/password`

**描述**: 修改密码（注意：使用 Firebase 后此功能已禁用）

**请求头**: `Authorization: Bearer {accessToken}`

**请求体**:

```json
{
  "oldPassword": "oldPassword123",
  "newPassword": "newPassword123"
}
```

**响应**: 标准响应格式

---

### 1.10 上传头像

**接口**: `POST /api/user/avatar`

**描述**: 上传用户头像

**请求头**: `Authorization: Bearer {accessToken}`

**请求类型**: `multipart/form-data`

**请求参数**: `file` (MultipartFile)

**响应**: 标准响应格式

---

### 1.11 用户登出

**接口**: `POST /api/user/logout`

**描述**: 用户登出

**请求头**: `Authorization: Bearer {accessToken}`

**响应**: 标准响应格式

---

### 1.12 获取个人信息

**接口**: `GET /api/user/user/me`

**描述**: 获取用户个人信息（包括积分和段位）

**请求头**: `Authorization: Bearer {accessToken}`

**响应示例**:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "documentId": "user-doc-id",
    "userName": "user",
    "email": "user@example.com",
    "avatarUrl": "/avatars/xxx.jpg",
    "currentPoints": 150,
    "rank": "白银",
    "pointsToNextRank": 150,
    "createTime": "2024-01-01 10:00:00",
    "updateTime": "2024-01-01 10:00:00"
  },
  "success": true
}
```

---

### 1.13 验证用户存在

**接口**: `POST /api/user/{userId}/exist`

**描述**: 验证用户是否存在

**路径参数**: `userId` (String) - 用户 documentId

**响应示例**:

```json
{
  "code": 1,
  "msg": null,
  "data": true,
  "success": true
}
```

---

### 1.14 查询用户（内部使用）

**接口**: `GET /api/user/{id}`

**描述**: 根据 documentId 查询用户（内部使用）

**路径参数**: `id` (String) - 用户 documentId

**响应**: User 实体对象

---

## 二、管理员接口 (`/api/admin`) ⭐ 核心 API

### 2.1 管理员登录

**接口**: `POST /api/admin/login`

**描述**: 管理员登录（支持 Firebase 认证）

**请求体**:

```json
{
  "firebaseToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**响应示例**:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "accessToken": "uuid-access-token",
    "refreshToken": "uuid-refresh-token",
    "userName": "admin",
    "email": "admin@example.com",
    "avatarUrl": "/avatars/xxx.jpg"
  },
  "success": true
}
```

---

### 2.2 管理员注册

**接口**: `POST /api/admin/register`

**描述**: 管理员注册（支持 Firebase 认证）

**请求体**:

```json
{
  "firebaseToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userName": "admin",
  "email": "admin@example.com"
}
```

**响应**: 同登录接口

---

### 2.3 第三方登录（支持 Google/Facebook/Twitter）

**接口**: `POST /api/admin/third-party`

**描述**: 通过第三方账号登录（支持 Google、Facebook、Twitter，通过 Firebase）

**请求体**:

```json
{
  "tokenId": "firebase-id-token"
}
```

**说明**:

- 支持 Google、Facebook、Twitter 等第三方登录
- 所有第三方登录都通过 Firebase 返回统一的 ID Token
- 首次登录时自动注册新管理员
- 从 token 中自动提取用户信息（邮箱、用户名、头像）

**响应**: 同登录接口

---

### 2.4 发送忘记密码邮件

**接口**: `POST /api/admin/forgetSend`

**描述**: 发送忘记密码邮件

**请求体**:

```json
{
  "email": "user@example.com"
}
```

**响应**: 标准响应格式

---

### 2.5 重置密码

**接口**: `POST /api/admin/resetPassword`

**描述**: 重置密码

**请求体**:

```json
{
  "token": "reset-token-from-email",
  "password": "newPassword123"
}
```

**响应**: 标准响应格式

---

### 2.6 刷新 Token

**接口**: `POST /api/admin/refresh`

**描述**: 刷新访问令牌

**请求体**:

```json
{
  "refreshToken": "uuid-refresh-token"
}
```

**响应**: 同登录接口

---

### 2.7 修改管理员用户名

**接口**: `PUT /api/admin/name`

**描述**: 修改管理员用户名

**请求头**: `Authorization: Bearer {accessToken}`

**请求体**:

```json
{
  "userName": "newAdminName"
}
```

**响应**: 标准响应格式

---

### 2.8 修改管理员邮箱

**接口**: `PUT /api/admin/email`

**描述**: 修改管理员邮箱

**请求头**: `Authorization: Bearer {accessToken}`

**请求体**:

```json
"newemail@example.com"
```

**响应**: 标准响应格式

---

### 2.9 修改管理员密码

**接口**: `PUT /api/admin/password`

**描述**: 修改管理员密码（注意：使用 Firebase 后此功能已禁用）

**请求头**: `Authorization: Bearer {accessToken}`

**请求体**:

```json
{
  "oldPassword": "oldPassword123",
  "newPassword": "newPassword123"
}
```

**响应**: 标准响应格式

---

### 2.10 上传管理员头像

**接口**: `POST /api/admin/avatar`

**描述**: 上传管理员头像

**请求头**: `Authorization: Bearer {accessToken}`

**请求类型**: `multipart/form-data`

**请求参数**: `file` (MultipartFile)

**响应**: 标准响应格式

---

### 2.11 管理员登出

**接口**: `POST /api/admin/logout`

**描述**: 管理员登出

**请求头**: `Authorization: Bearer {accessToken}`

**响应**: 标准响应格式

---

### 2.12 获取管理员个人信息

**接口**: `GET /api/admin/admin/me`

**描述**: 获取管理员个人信息

**请求头**: `Authorization: Bearer {accessToken}`

**响应示例**:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "documentId": "admin-doc-id",
    "userName": "admin",
    "email": "admin@example.com",
    "avatarUrl": "/avatars/xxx.jpg",
    "createTime": "2024-01-01 10:00:00",
    "updateTime": "2024-01-01 10:00:00"
  },
  "success": true
}
```

---

## 三、问卷接口 (`/api/survey`) ⭐ 核心 API

### 3.1 创建问卷

**接口**: `POST /api/survey/create`

**描述**: 创建问卷（包含问题、选项）

**请求体**:

```json
{
  "title": "校园餐厅满意度调查",
  "description": "为了提升校园餐厅的服务质量，我们诚挚邀请您参与本次满意度调查。",
  "latitude": 42.3505,
  "longitude": -71.1054,
  "points": 10, // 问卷积分（可选，默认为0）
  "questions": [
    {
      "type": "single",
      "content": "您对校园餐厅的整体满意度如何？",
      "required": true,
      "options": [
        { "content": "非常满意", "label": "A" },
        { "content": "满意", "label": "B" },
        { "content": "一般", "label": "C" },
        { "content": "不满意", "label": "D" }
      ]
    },
    {
      "type": "multiple",
      "content": "您认为餐厅哪些方面需要改进？（可多选）",
      "required": false,
      "options": [
        { "content": "菜品口味", "label": "A" },
        { "content": "菜品多样性", "label": "B" },
        { "content": "价格合理性", "label": "C" }
      ]
    },
    {
      "type": "text",
      "content": "请提供您对餐厅的改进建议：",
      "required": false,
      "options": null
    }
  ]
}
```

**响应示例**:

```json
{
  "code": 1,
  "msg": null,
  "data": "survey-document-id",
  "success": true
}
```

---

### 3.2 更新问卷

**接口**: `PUT /api/survey/update`

**描述**: 更新问卷信息

**请求体**:

```json
{
  "id": 123,
  "title": "更新的问卷标题",
  "description": "更新的描述",
  "latitude": 42.3505,
  "longitude": -71.1054,
  "points": 20
}
```

**响应**: 标准响应格式

---

### 3.3 删除问卷

**接口**: `DELETE /api/survey/{id}`

**描述**: 根据 documentId 删除问卷

**路径参数**: `id` (String) - 问卷 documentId

**响应**: 标准响应格式

---

### 3.4 根据 ID 获取问卷

**接口**: `GET /api/survey/{id}`

**描述**: 根据 documentId 获取问卷详情（包含问题和选项）

**路径参数**: `id` (String) - 问卷 documentId

**响应示例**:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "documentId": "survey-doc-id",
    "title": "校园餐厅满意度调查",
    "description": "问卷描述",
    "latitude": 42.3505,
    "longitude": -71.1054,
    "questionNumber": 3,
    "status": "ACTIVE",
    "points": 10,
    "questions": [
      {
        "documentId": "question-doc-id",
        "type": "single",
        "content": "问题内容",
        "required": true,
        "options": [
          {
            "documentId": "option-doc-id",
            "label": "A",
            "content": "选项内容"
          }
        ]
      }
    ]
  },
  "success": true
}
```

---

### 3.5 批量获取问卷

**接口**: `POST /api/survey/list`

**描述**: 根据多个 documentId 批量获取问卷

**请求体**:

```json
["survey-doc-id-1", "survey-doc-id-2", "survey-doc-id-3"]
```

**响应**: Survey 对象数组

---

### 3.6 分页查询问卷（暂时用不了）

**接口**: `POST /api/survey/page`

**描述**: 分页查询问卷列表

**请求体**:

```json
{
  "page": 1,
  "pageSize": 10,
  "title": "问卷标题（可选，模糊查询）",
  "status": "ACTIVE"
}
```

**响应示例**:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "total": 100,
    "records": [
      {
        "documentId": "survey-doc-id",
        "title": "问卷标题",
        "description": "问卷描述",
        "questionNumber": 5,
        "status": "ACTIVE",
        "points": 10
      }
    ]
  },
  "success": true
}
```

---

### 3.7 获取问卷统计信息

**接口**: `GET /api/survey/{id}/stats`

**描述**: 获取问卷统计数据

**路径参数**: `id` (String) - 问卷 documentId

**响应示例**:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "totalResponses": 50,
    "completeResponses": 45,
    "partialResponses": 5,
    "completionRate": 90.0
  },
  "success": true
}
```

---

### 3.8 批量删除问卷

**接口**: `DELETE /api/survey/batch`

**描述**: 批量删除问卷

**请求体**:

```json
["survey-doc-id-1", "survey-doc-id-2"]
```

**响应**: 标准响应格式

---

### 3.9 变更问卷状态

**接口**: `PUT /api/survey/{id}/status`

**描述**: 变更问卷状态

**路径参数**: `id` (String) - 问卷 documentId

**查询参数**: `status` (String) - 新状态（如：ACTIVE, INACTIVE）

**响应**: 标准响应格式

---

### 3.10 获取问卷问题

**接口**: `GET /api/survey/question/{id}`

**描述**: 根据问题 documentId 获取问题列表

**路径参数**: `id` (String) - 问题 documentId

**响应**: Question 对象数组

---

### 3.11 获取问题选项

**接口**: `GET /api/survey/option/{id}`

**描述**: 根据问题 documentId 获取问题的所有选项

**路径参数**: `id` (String) - 问题 documentId

**响应**: Option 对象数组

---

## 四、回答接口 (`/api/response`) ⭐ 核心 API

### 4.1 提交问卷回答

**接口**: `POST /api/response/`

**描述**: 提交问卷回答（完成问卷后会自动增加用户积分）

**请求体**:

```json
[
  {
    "questionId": "question-doc-id-1",
    "selected": "A",
    "content": null
  },
  {
    "questionId": "question-doc-id-2",
    "selected": "A,C",
    "content": null
  },
  {
    "questionId": "question-doc-id-3",
    "selected": null,
    "content": "这是一些建议..."
  }
]
```

**响应示例**:

```json
{
  "code": 1,
  "msg": null,
  "data": "问卷回答提交成功",
  "success": true
}
```

**注意**:

- 首次完成问卷且问卷设置了积分时，会自动给用户增加积分
- 积分规则见文档开头的积分系统说明

---

### 4.2 查询回答详情

**接口**: `POST /api/response/detail/{id}`

**描述**: 根据回答 documentId 查询回答详情

**路径参数**: `id` (String) - 回答 documentId

**响应示例**:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "responseId": "response-doc-id",
    "surveyId": "survey-doc-id",
    "userId": "user-doc-id",
    "status": "COMPLETE",
    "answeredQuestions": 5,
    "totalQuestions": 5,
    "completionRate": 100.0,
    "questionAnswers": [
      {
        "documentId": "question-doc-id",
        "type": "single",
        "content": "问题内容",
        "answered": true,
        "selectedOptions": ["A"],
        "textContent": null
      }
    ]
  },
  "success": true
}
```

---

### 4.3 查询用户回答列表

**接口**: `POST /api/response/list`

**描述**: 分页查询用户回答列表

**请求体**:

```json
{
  "page": 1,
  "pageSize": 10,
  "surveyId": "survey-doc-id",
  "userName": "user",
  "userEmail": "user@example.com"
}
```

**响应示例**:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "total": 50,
    "records": [
      {
        "responseId": "response-doc-id",
        "surveyId": "survey-doc-id",
        "userId": "user-doc-id",
        "userName": "user",
        "userEmail": "user@example.com",
        "status": "COMPLETE",
        "answeredQuestions": 5,
        "totalQuestions": 5,
        "completionRate": 100.0,
        "responseTime": "2024-01-01 10:00:00",
        "isComplete": true
      }
    ]
  },
  "success": true
}
```

---

## 五、地理位置接口 (`/api/location`) （辅助功能）

### 5.1 获取附近位置信息（POST）

**接口**: `POST /api/location/nearby`

**描述**: 根据用户位置获取附近的问卷

**请求体**:

```json
{
  "userLat": 42.3505,
  "userLng": -71.1054,
  "radiusKm": 5.0,
  "precision": 7,
  "maxResults": 200,
  "includeDistance": true,
  "sortByDistance": true
}
```

**响应示例**:

```json
{
  "code": 1,
  "msg": null,
  "data": [
    {
      "documentId": "survey-doc-id",
      "title": "附近问卷",
      "description": "问卷描述",
      "latitude": 42.3505,
      "longitude": -71.1054,
      "distance": 0.5
    }
  ],
  "success": true
}
```

---

### 5.2 查询附近问卷（GET）

**接口**: `GET /api/location/nearby`

**描述**: 根据用户位置查询附近的问卷（GET 方式）

**查询参数**:

- `userLat` (Double) - 用户纬度
- `userLng` (Double) - 用户经度
- `radiusKm` (Double) - 查询半径（公里）

**响应**: 同 POST 接口

---

### 5.3 缓存问卷地理位置信息

**接口**: `POST /api/location/cache`

**描述**: 缓存问卷地理位置信息

**请求体**:

```json
{
  "documentId": "survey-doc-id",
  "latitude": 42.3505,
  "longitude": -71.1054
}
```

**响应**: 标准响应格式

---

### 5.4 移除问卷地理位置信息

**接口**: `DELETE /api/location/{surveyId}`

**描述**: 移除问卷的地理位置缓存

**路径参数**: `surveyId` (String) - 问卷 documentId

**响应**: 标准响应格式

---

### 5.5 更新问卷地理位置信息

**接口**: `PUT /api/location/update`

**描述**: 更新问卷地理位置信息

**请求体**:

```json
{
  "documentId": "survey-doc-id",
  "latitude": 42.3505,
  "longitude": -71.1054
}
```

**响应**: 标准响应格式

---

### 5.6 获取问卷地理位置信息

**接口**: `GET /api/location/{surveyId}`

**描述**: 获取问卷的地理位置信息

**路径参数**: `surveyId` (String) - 问卷 documentId

**响应示例**:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "latitude": 42.3505,
    "longitude": -71.1054,
    "geoHash": "dr5r..."
  },
  "success": true
}
```

---

## 六、测试接口 (`/api/test`) （开发测试用）

### 6.1 创建测试用户

**接口**: `POST /api/test/create-test-user`

**描述**: 创建 Firebase 测试用户并获取自定义 Token（仅用于开发测试）

**查询参数**:

- `email` (String) - 用户邮箱
- `name` (String) - 用户名称
- `photoUrl` (String, 可选) - 头像 URL

**响应示例**:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "uid": "firebase-user-uid",
    "email": "test@example.com",
    "name": "Test User",
    "customToken": "firebase-custom-token",
    "instructions": "使用 customToken 在前端调用 signInWithCustomToken() 获取 ID Token"
  },
  "success": true
}
```

---

### 6.2 验证 Firebase Token

**接口**: `POST /api/test/verify-token`

**描述**: 验证 Firebase ID Token（仅用于开发测试）

**请求体**:

```json
{
  "idToken": "firebase-id-token"
}
```

**响应示例**:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "uid": "firebase-user-uid",
    "email": "test@example.com",
    "name": "Test User",
    "picture": "https://...",
    "valid": true
  },
  "success": true
}
```

---

### 6.3 获取用户信息

**接口**: `GET /api/test/user/{uid}`

**描述**: 根据 Firebase UID 获取用户信息（仅用于开发测试）

**路径参数**: `uid` (String) - Firebase 用户 UID

**响应示例**:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "uid": "firebase-user-uid",
    "email": "test@example.com",
    "name": "Test User",
    "photoUrl": "https://...",
    "emailVerified": true
  },
  "success": true
}
```

---

## 七、积分系统说明

### 7.1 积分规则

- **积分获取**: 用户首次完成问卷时，如果问卷设置了积分（`points` 字段），系统会自动给用户增加相应积分
- **积分计算**: 每次增加积分后，系统会自动计算用户的段位和升段所需积分

### 7.2 段位规则

| 段位 | 积分范围  | 升段所需积分  |
| ---- | --------- | ------------- |
| 青铜 | 0-99      | 100           |
| 白银 | 100-299   | 300           |
| 黄金 | 300-599   | 600           |
| 白金 | 600-999   | 1000          |
| 钻石 | 1000-1999 | 2000          |
| 大师 | 2000-4999 | 5000          |
| 宗师 | 5000+     | 0（最高段位） |

### 7.3 积分相关字段

用户实体中包含以下积分相关字段：

- `currentPoints` (Integer) - 当前积分
- `rank` (String) - 段位
- `pointsToNextRank` (Integer) - 升段所需积分

---

## 八、错误码说明

- `code: 1` - 请求成功
- `code: 0` - 请求失败（具体错误信息见 `msg` 字段）

### 常见错误

1. **401 Unauthorized**: Token 过期或无效，需要重新登录
2. **404 Not Found**: 资源不存在
3. **400 Bad Request**: 请求参数错误
4. **500 Internal Server Error**: 服务器内部错误

---

## 九、使用示例

### 9.1 用户登录流程

```javascript
// 1. 前端获取 Firebase ID Token
const idToken = await firebase.auth().currentUser.getIdToken();

// 2. 调用登录接口
const response = await fetch("http://localhost:8082/api/user/login", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
  },
  body: JSON.stringify({
    firebaseToken: idToken,
  }),
});

const data = await response.json();
const { accessToken, refreshToken } = data.data;

// 3. 保存 Token
localStorage.setItem("accessToken", accessToken);
localStorage.setItem("refreshToken", refreshToken);
```

### 9.2 第三方登录流程（Google/Facebook/Twitter）

```javascript
// 1. 使用 Firebase 进行第三方登录（支持 Google、Facebook、Twitter）
// Google 登录示例
const googleProvider = new firebase.auth.GoogleAuthProvider();
const googleResult = await firebase.auth().signInWithPopup(googleProvider);

// Facebook 登录示例
// const facebookProvider = new firebase.auth.FacebookAuthProvider();
// const facebookResult = await firebase.auth().signInWithPopup(facebookProvider);

// Twitter 登录示例
// const twitterProvider = new firebase.auth.TwitterAuthProvider();
// const twitterResult = await firebase.auth().signInWithPopup(twitterProvider);

// 2. 获取 Firebase ID Token
const idToken = await googleResult.user.getIdToken();

// 3. 调用第三方登录接口
const response = await fetch("http://localhost:8082/api/user/third-party", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
  },
  body: JSON.stringify({
    tokenId: idToken,
  }),
});

const data = await response.json();
const { accessToken, refreshToken } = data.data;

// 4. 保存 Token
localStorage.setItem("accessToken", accessToken);
localStorage.setItem("refreshToken", refreshToken);
```

**说明**:

- 支持 Google、Facebook、Twitter 等第三方登录
- 所有第三方登录都通过 Firebase 返回统一的 ID Token
- 首次登录时会自动注册新用户
- 从 token 中自动提取用户信息（邮箱、用户名、头像）

---

### 9.3 创建问卷并提交回答

```javascript
// 1. 创建问卷
const createResponse = await fetch("http://localhost:8082/api/survey/create", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    Authorization: `Bearer ${accessToken}`,
  },
  body: JSON.stringify({
    title: "测试问卷",
    description: "问卷描述",
    latitude: 42.3505,
    longitude: -71.1054,
    points: 10,
    questions: [
      {
        type: "single",
        content: "您对问卷的满意度如何？",
        required: true,
        options: [
          { content: "非常满意", label: "A" },
          { content: "满意", label: "B" },
        ],
      },
    ],
  }),
});

const { data: surveyId } = await createResponse.json();

// 2. 提交回答
const submitResponse = await fetch("http://localhost:8082/api/response/", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    Authorization: `Bearer ${accessToken}`,
  },
  body: JSON.stringify([
    {
      questionId: "question-doc-id",
      selected: "A",
      content: null,
    },
  ]),
});
```

---

## 十、注意事项

1. **Token 管理**:

   - Access Token 有效期为 30 分钟
   - Refresh Token 有效期为 7 天
   - Token 过期后需要使用 Refresh Token 刷新

2. **认证要求**:

   - 大部分接口需要 Bearer Token 认证
   - 登录、注册等接口不需要认证
   - 测试时可以使用 TestController 创建测试用户

3. **数据类型**:

   - 所有 ID 字段都是 String 类型（Firestore documentId）
   - 经纬度使用 Double 类型
   - 时间格式：`yyyy-MM-dd HH:mm:ss`

4. **积分系统**:
   - 只有首次完成问卷才会获得积分
   - 问卷必须设置 `points` 字段才会给积分
   - 积分增加会自动更新段位信息

---

## 十一、Swagger UI

项目已集成 Swagger UI，可以通过以下地址访问：

- **Swagger UI**: `http://localhost:8082/swagger-ui/index.html`
- **API 文档**: `http://localhost:8082/v3/api-docs`

---

**文档版本**: 1.0  
**最后更新**: 2024-01-01  
**维护者**: Survey Team
