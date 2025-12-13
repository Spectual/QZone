# Survey System API Documentation

## Basic Information

API URL: http://52.14.58.34:8082/swagger-ui/index.html

- **Base URL**: `http://52.14.58.34:8082`
- **Content-Type**: `application/json`
- **Authentication**: Bearer Token (add `Authorization: Bearer {accessToken}` in request headers)

### Unified Response Format

All endpoints return a unified `Result<T>` format:

```json
{
  "code": 1, // 1 indicates success, 0 or other numbers indicate failure
  "msg": "Success", // Error message (when failed)
  "data": {}, // Response data (when successful)
  "success": true // Whether successful
}
```

---

## I. User APIs (`/api/user`) ⭐ Core API

### 1.1 User Login

**Endpoint**: `POST /api/user/login`

**Description**: User login (supports Firebase authentication)

**Request Body**:

```json
{
  "firebaseToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response Example**:

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
    "rank": "Silver",
    "pointsToNextRank": 150
  },
  "success": true
}
```

---

### 1.2 User Registration

**Endpoint**: `POST /api/user/register`

**Description**: User registration (supports Firebase authentication)

**Request Body**:

```json
{
  "firebaseToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userName": "user",
  "email": "user@example.com"
}
```

**Response**: Same as login endpoint (new users default to 0 points, rank "Bronze")

---

### 1.3 Third-Party Login (Supports Google/Facebook/Twitter)

**Endpoint**: `POST /api/user/third-party`

**Description**: Login via third-party account (supports Google, Facebook, Twitter through Firebase)

**Request Body**:

```json
{
  "tokenId": "firebase-id-token"
}
```

**Notes**:

- Supports third-party login via Google, Facebook, Twitter, etc.
- All third-party logins use Firebase to return a unified ID Token
- Automatically registers new users on first login
- Automatically extracts user information (email, username, avatar) from token

**Response**: Same as login endpoint

---

### 1.4 Send Forgot Password Email (Missing Forgot Password UI)

**Endpoint**: `POST /api/user/forgetSend`

**Description**: Send forgot password email

**Request Body**:

```json
{
  "email": "user@example.com"
}
```

**Response**:

```json
{
  "code": 1,
  "msg": null,
  "data": "success",
  "success": true
}
```

---

### 1.5 Reset Password

**Endpoint**: `POST /api/user/resetPassword`

**Description**: Reset password

**Request Body**:

```json
{
  "token": "reset-token-from-email",
  "password": "newPassword123"
}
```

**Response**: Standard response format

---

### 1.6 Refresh Token

**Endpoint**: `POST /api/user/refresh`

**Description**: Refresh access token

**Request Body**:

```json
{
  "refreshToken": "uuid-refresh-token"
}
```

**Response**: Same as login endpoint

---

### 1.7 Update Username

**Endpoint**: `PUT /api/user/name`

**Description**: Update username

**Request Headers**: `Authorization: Bearer {accessToken}`

**Request Body**:

```json
{
  "userName": "newUserName"
}
```

**Response**: Standard response format

---

### 1.8 Update Email

**Endpoint**: `PUT /api/user/email`

**Description**: Update email

**Request Headers**: `Authorization: Bearer {accessToken}`

**Request Body**:

```json
"newemail@example.com"
```

**Response**: Standard response format

---

### 1.9 Update Password (Not Implemented)

**Endpoint**: `PUT /api/user/password`

**Description**: Update password (Note: This feature is disabled after using Firebase)

**Request Headers**: `Authorization: Bearer {accessToken}`

**Request Body**:

```json
{
  "oldPassword": "oldPassword123",
  "newPassword": "newPassword123"
}
```

**Response**: Standard response format

---

### 1.10 Upload Avatar

**Endpoint**: `POST /api/user/avatar`

**Description**: Upload user avatar

**Request Headers**: `Authorization: Bearer {accessToken}`

**Content-Type**: `multipart/form-data`

**Request Parameters**: `file` (MultipartFile)

**Response**: Standard response format

---

### 1.11 User Logout

**Endpoint**: `POST /api/user/logout`

**Description**: User logout

**Request Headers**: `Authorization: Bearer {accessToken}`

**Response**: Standard response format

---

### 1.12 Get User Information

**Endpoint**: `GET /api/user/user/me`

**Description**: Get user personal information (including points and rank)

**Request Headers**: `Authorization: Bearer {accessToken}`

**Response Example**:

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
    "rank": "Silver",
    "pointsToNextRank": 150,
    "createTime": "2024-01-01 10:00:00",
    "updateTime": "2024-01-01 10:00:00"
  },
  "success": true
}
```

---

### 1.13 Verify User Existence

**Endpoint**: `POST /api/user/{userId}/exist`

**Description**: Verify if user exists

**Path Parameters**: `userId` (String) - User documentId

**Response Example**:

```json
{
  "code": 1,
  "msg": null,
  "data": true,
  "success": true
}
```

---

### 1.14 Query User (Internal Use)

**Endpoint**: `GET /api/user/{id}`

**Description**: Query user by documentId (internal use)

**Path Parameters**: `id` (String) - User documentId

**Response**: User entity object

---

## II. Admin APIs (`/api/admin`) ⭐ Core API

### 2.1 Admin Login

**Endpoint**: `POST /api/admin/login`

**Description**: Admin login (supports Firebase authentication)

**Request Body**:

```json
{
  "firebaseToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response Example**:

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

### 2.2 Admin Registration

**Endpoint**: `POST /api/admin/register`

**Description**: Admin registration (supports Firebase authentication)

**Request Body**:

```json
{
  "firebaseToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userName": "admin",
  "email": "admin@example.com"
}
```

**Response**: Same as login endpoint

---

### 2.3 Third-Party Login (Supports Google/Facebook/Twitter)

**Endpoint**: `POST /api/admin/third-party`

**Description**: Login via third-party account (supports Google, Facebook, Twitter through Firebase)

**Request Body**:

```json
{
  "tokenId": "firebase-id-token"
}
```

**Notes**:

- Supports third-party login via Google, Facebook, Twitter, etc.
- All third-party logins use Firebase to return a unified ID Token
- Automatically registers new admin on first login
- Automatically extracts user information (email, username, avatar) from token

**Response**: Same as login endpoint

---

### 2.4 Send Forgot Password Email

**Endpoint**: `POST /api/admin/forgetSend`

**Description**: Send forgot password email

**Request Body**:

```json
{
  "email": "user@example.com"
}
```

**Response**: Standard response format

---

### 2.5 Reset Password

**Endpoint**: `POST /api/admin/resetPassword`

**Description**: Reset password

**Request Body**:

```json
{
  "token": "reset-token-from-email",
  "password": "newPassword123"
}
```

**Response**: Standard response format

---

### 2.6 Refresh Token

**Endpoint**: `POST /api/admin/refresh`

**Description**: Refresh access token

**Request Body**:

```json
{
  "refreshToken": "uuid-refresh-token"
}
```

**Response**: Same as login endpoint

---

### 2.7 Update Admin Username

**Endpoint**: `PUT /api/admin/name`

**Description**: Update admin username

**Request Headers**: `Authorization: Bearer {accessToken}`

**Request Body**:

```json
{
  "userName": "newAdminName"
}
```

**Response**: Standard response format

---

### 2.8 Update Admin Email

**Endpoint**: `PUT /api/admin/email`

**Description**: Update admin email

**Request Headers**: `Authorization: Bearer {accessToken}`

**Request Body**:

```json
"newemail@example.com"
```

**Response**: Standard response format

---

### 2.9 Update Admin Password

**Endpoint**: `PUT /api/admin/password`

**Description**: Update admin password (Note: This feature is disabled after using Firebase)

**Request Headers**: `Authorization: Bearer {accessToken}`

**Request Body**:

```json
{
  "oldPassword": "oldPassword123",
  "newPassword": "newPassword123"
}
```

**Response**: Standard response format

---

### 2.10 Upload Admin Avatar

**Endpoint**: `POST /api/admin/avatar`

**Description**: Upload admin avatar

**Request Headers**: `Authorization: Bearer {accessToken}`

**Content-Type**: `multipart/form-data`

**Request Parameters**: `file` (MultipartFile)

**Response**: Standard response format

---

### 2.11 Admin Logout

**Endpoint**: `POST /api/admin/logout`

**Description**: Admin logout

**Request Headers**: `Authorization: Bearer {accessToken}`

**Response**: Standard response format

---

### 2.12 Get Admin Information

**Endpoint**: `GET /api/admin/admin/me`

**Description**: Get admin personal information

**Request Headers**: `Authorization: Bearer {accessToken}`

**Response Example**:

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

## III. Survey APIs (`/api/survey`) ⭐ Core API

### 3.1 Create Survey

**Endpoint**: `POST /api/survey/create`

**Description**: Create survey (including questions and options)

**Request Body**:

```json
{
  "title": "Campus Restaurant Satisfaction Survey",
  "description": "To improve the service quality of campus restaurants, we sincerely invite you to participate in this satisfaction survey.",
  "latitude": 42.3505,
  "longitude": -71.1054,
  "points": 10, // Survey points (optional, default 0)
  "questions": [
    {
      "type": "single",
      "content": "How satisfied are you with the campus restaurant overall?",
      "required": true,
      "options": [
        { "content": "Very Satisfied", "label": "A" },
        { "content": "Satisfied", "label": "B" },
        { "content": "Neutral", "label": "C" },
        { "content": "Dissatisfied", "label": "D" }
      ]
    },
    {
      "type": "multiple",
      "content": "Which aspects of the restaurant do you think need improvement? (Multiple choice)",
      "required": false,
      "options": [
        { "content": "Food Taste", "label": "A" },
        { "content": "Food Variety", "label": "B" },
        { "content": "Price Reasonableness", "label": "C" }
      ]
    },
    {
      "type": "text",
      "content": "Please provide your suggestions for restaurant improvement:",
      "required": false,
      "options": null
    }
  ]
}
```

**Response Example**:

```json
{
  "code": 1,
  "msg": null,
  "data": "survey-document-id",
  "success": true
}
```

---

### 3.1.1 Get Presigned Upload URL (Direct Upload to S3)

Frontend can use this endpoint to get `uploadUrl`, then use HTTP PUT to upload images directly to S3. After upload, use the `publicUrl` in the `imageUrl` field when creating/updating surveys.

- Endpoint: `POST /api/survey/upload-url`
- Authentication: `Authorization: Bearer {accessToken}`
- Content-Type: `application/json` or `application/x-www-form-urlencoded`
- Parameters:
  - `filename` Example: `cover.jpg`
  - `contentType` Example: `image/jpeg`
  - `expiresSeconds` Optional, default 300, max 3600

Request Example:

```bash
curl -X POST 'http://52.14.58.34:8082/api/survey/upload-url' \
  -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' \
  -d '{"filename":"cover.jpg","contentType":"image/jpeg","expiresSeconds":300}'
```

Response Example:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "key": "surveys/2025-11-17/3b1f...a2.jpg",
    "uploadUrl": "https://bucket.s3.us-east-1.amazonaws.com/surveys/...&X-Amz-SignedHeaders=...",
    "publicUrl": "https://bucket.s3.us-east-1.amazonaws.com/surveys/2025-11-17/3b1f...a2.jpg",
    "expiresIn": 300,
    "contentType": "image/jpeg"
  },
  "success": true
}
```

Direct Upload (PUT to uploadUrl):

```bash
curl -X PUT '<uploadUrl>' \
  -H 'Content-Type: image/jpeg' \
  --data-binary '@cover.jpg'
```

Notes:

- `Content-Type` in PUT must match the one used when requesting presigned URL
- Presigned URL has expiration time, need to request again if expired

---

### 3.2 Update Survey

**Endpoint**: `PUT /api/survey/update`

**Description**: Update survey information

**Request Body**:

```json
{
  "id": 123,
  "title": "Updated Survey Title",
  "description": "Updated description",
  "latitude": 42.3505,
  "longitude": -71.1054,
  "points": 20
}
```

**Response**: Standard response format

---

### 3.3 Delete Survey

**Endpoint**: `DELETE /api/survey/{id}`

**Description**: Delete survey by documentId

**Path Parameters**: `id` (String) - Survey documentId

**Response**: Standard response format

---

### 3.4 Get Survey by ID

**Endpoint**: `GET /api/survey/{id}`

**Description**: Get survey details by documentId (including questions and options)

**Path Parameters**: `id` (String) - Survey documentId

**Response Example**:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "documentId": "survey-doc-id",
    "title": "Campus Restaurant Satisfaction Survey",
    "description": "Survey description",
    "latitude": 42.3505,
    "longitude": -71.1054,
    "questionNumber": 3,
    "status": "ACTIVE",
    "points": 10,
    "questions": [
      {
        "documentId": "question-doc-id",
        "type": "single",
        "content": "Question content",
        "required": true,
        "options": [
          {
            "documentId": "option-doc-id",
            "label": "A",
            "content": "Option content"
          }
        ]
      }
    ]
  },
  "success": true
}
```

---

### 3.5 Batch Get Surveys

**Endpoint**: `POST /api/survey/list`

**Description**: Get surveys by multiple documentIds

**Request Body**:

```json
["survey-doc-id-1", "survey-doc-id-2", "survey-doc-id-3"]
```

**Response**: Array of Survey objects

---

### 3.6 Paginated Query Surveys (Temporarily Unavailable)

**Endpoint**: `POST /api/survey/page`

**Description**: Paginated query survey list

**Request Body**:

```json
{
  "page": 1,
  "pageSize": 10,
  "title": "Survey Title (optional, fuzzy search)",
  "status": "ACTIVE"
}
```

**Response Example**:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "total": 100,
    "records": [
      {
        "documentId": "survey-doc-id",
        "title": "Survey Title",
        "description": "Survey description",
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

### 3.7 Get Survey Statistics

**Endpoint**: `GET /api/survey/{id}/stats`

**Description**: Get survey statistics

**Path Parameters**: `id` (String) - Survey documentId

**Response Example**:

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

### 3.8 Batch Delete Surveys

**Endpoint**: `DELETE /api/survey/batch`

**Description**: Batch delete surveys

**Request Body**:

```json
["survey-doc-id-1", "survey-doc-id-2"]
```

**Response**: Standard response format

---

### 3.9 Update Survey Status

**Endpoint**: `PUT /api/survey/{id}/status`

**Description**: Update survey status

**Path Parameters**: `id` (String) - Survey documentId

**Query Parameters**: `status` (String) - New status (e.g., ACTIVE, INACTIVE)

**Response**: Standard response format

---

### 3.10 Get Survey Questions

**Endpoint**: `GET /api/survey/question/{id}`

**Description**: Get question list by question documentId

**Path Parameters**: `id` (String) - Question documentId

**Response**: Array of Question objects

---

### 3.11 Get Question Options

**Endpoint**: `GET /api/survey/option/{id}`

**Description**: Get all options for a question by question documentId

**Path Parameters**: `id` (String) - Question documentId

**Response**: Array of Option objects

---

## IV. Response APIs (`/api/response`) ⭐ Core API

### 4.1 Submit Survey Response

**Endpoint**: `POST /api/response/`

**Description**: Submit survey response (user points will be automatically increased after completing survey)

**Request Body**:

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
    "content": "Some suggestions..."
  }
]
```

**Response Example**:

```json
{
  "code": 1,
  "msg": null,
  "data": "Survey response submitted successfully",
  "success": true
}
```

**Notes**:

- When completing a survey for the first time and the survey has points set, user points will be automatically increased
- Point rules are explained in the points system section at the beginning of the document

---

### 4.2 Query Response Details

**Endpoint**: `POST /api/response/detail/{id}`

**Description**: Query response details by response documentId

**Path Parameters**: `id` (String) - Response documentId

**Response Example**:

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
        "content": "Question content",
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

### 4.3 Query User Response List

**Endpoint**: `POST /api/response/list`

**Description**: Paginated query user response list

**Request Body**:

```json
{
  "page": 1,
  "pageSize": 10,
  "surveyId": "survey-doc-id",
  "userName": "user",
  "userEmail": "user@example.com"
}
```

**Response Example**:

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

## V. Coupon APIs (`/api/coupon`) ⭐ Core API

### 5.1 Redeem Coupon with Points

**Endpoint**: `POST /api/coupon/redeem`

**Description**: User redeems coupon using points. The system will deduct user points and save the coupon record to the table associated with the user.

**Request Headers**: `Authorization: Bearer {accessToken}`

**Request Body**:

```json
{
  "requiredPoints": 100,
  "couponName": "$10 Coupon"
}
```

**Request Parameter Description**:

| Parameter Name   | Type    | Required | Description                        |
| ---------------- | ------- | -------- | ---------------------------------- |
| `requiredPoints` | Integer | Yes      | Points to deduct, must be greater than 0 |
| `couponName`     | String  | Yes      | Coupon name, cannot be empty       |

**Response Example**:

```json
{
  "code": 1,
  "msg": null,
  "data": 850,
  "success": true
}
```

**Notes**:

- `data` field returns remaining points after redemption (Integer type)
- After successful redemption, a record will be created in the `user_coupon` collection in Firestore
- After deducting points, user rank will be automatically recalculated

**Error Messages**:

| Error Message                              | Description                                    |
| ------------------------------------------ | ---------------------------------------------- |
| `"Not logged in"`                          | User not logged in                             |
| `"Required points must be greater than 0"` | Points must be greater than 0                  |
| `"Coupon name cannot be empty"`            | Coupon name cannot be empty                    |
| `"Insufficient points"`                    | User's current points are less than required    |
| `"User not found"`                         | User does not exist                            |
| `"User service unavailable"`               | User service unavailable                       |
| `"Redemption failed: ..."`                 | Redemption failed (other exceptions, includes details) |

---

## VI. Location APIs (`/api/location`) (Auxiliary Feature)

### 6.1 Get Nearby Location Information (POST)

**Endpoint**: `POST /api/location/nearby`

**Description**: Get nearby surveys based on user location

**Request Body**:

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

**Response Example**:

```json
{
  "code": 1,
  "msg": null,
  "data": [
    {
      "documentId": "survey-doc-id",
      "title": "Nearby Survey",
      "description": "Survey description",
      "latitude": 42.3505,
      "longitude": -71.1054,
      "distance": 0.5
    }
  ],
  "success": true
}
```

---

### 6.2 Query Nearby Surveys (GET)

**Endpoint**: `GET /api/location/nearby`

**Description**: Query nearby surveys based on user location (GET method)

**Query Parameters**:

- `userLat` (Double) - User latitude
- `userLng` (Double) - User longitude
- `radiusKm` (Double) - Query radius (kilometers)

**Response**: Same as POST endpoint

---

### 6.3 Cache Survey Location Information

**Endpoint**: `POST /api/location/cache`

**Description**: Cache survey location information

**Request Body**:

```json

```

**Response**: Standard response format

---

### 6.4 Remove Survey Location Information

**Endpoint**: `DELETE /api/location/{surveyId}`

**Description**: Remove survey location cache

**Path Parameters**: `surveyId` (String) - Survey documentId

**Response**: Standard response format

---

### 6.5 Update Survey Location Information

**Endpoint**: `PUT /api/location/update`

**Description**: Update survey location information

**Request Body**:

```json
{
  "documentId": "survey-doc-id",
  "latitude": 42.3505,
  "longitude": -71.1054
}
```

**Response**: Standard response format

---

### 6.6 Get Survey Location Information

**Endpoint**: `GET /api/location/{surveyId}`

**Description**: Get survey location information

**Path Parameters**: `surveyId` (String) - Survey documentId

**Response Example**:

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

## VII. Test APIs (`/api/test`) (Development Testing)

### 7.1 Create Test User

**Endpoint**: `POST /api/test/create-test-user`

**Description**: Create Firebase test user and get custom token (for development testing only)

**Query Parameters**:

- `email` (String) - User email
- `name` (String) - User name
- `photoUrl` (String, optional) - Avatar URL

**Response Example**:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "uid": "firebase-user-uid",
    "email": "test@example.com",
    "name": "Test User",
    "customToken": "firebase-custom-token",
    "instructions": "Use customToken to call signInWithCustomToken() on frontend to get ID Token"
  },
  "success": true
}
```

---

### 7.2 Verify Firebase Token

**Endpoint**: `POST /api/test/verify-token`

**Description**: Verify Firebase ID Token (for development testing only)

**Request Body**:

```json
{
  "idToken": "firebase-id-token"
}
```

**Response Example**:

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

### 7.3 Get User Information

**Endpoint**: `GET /api/test/user/{uid}`

**Description**: Get user information by Firebase UID (for development testing only)

**Path Parameters**: `uid` (String) - Firebase user UID

**Response Example**:

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

## VIII. Points System Description

### 8.1 Points Rules

- **Points Earning**: When a user completes a survey for the first time, if the survey has points set (the `points` field), the system will automatically add corresponding points to the user
- **Points Calculation**: After each points increase, the system will automatically calculate the user's rank and points needed for next rank

### 8.2 Rank Rules

| Rank      | Points Range | Points to Next Rank |
| --------- | ------------ | ------------------- |
| Bronze    | 0-99         | 100                 |
| Silver    | 100-299      | 300                 |
| Gold      | 300-599      | 600                 |
| Platinum  | 600-999      | 1000                |
| Diamond   | 1000-1999    | 2000                |
| Master    | 2000-4999    | 5000                |
| Grandmaster | 5000+      | 0 (Highest rank)    |

### 8.3 Points Related Fields

User entity includes the following points-related fields:

- `currentPoints` (Integer) - Current points
- `rank` (String) - Rank
- `pointsToNextRank` (Integer) - Points needed for next rank

---

### 8.4 Points Redemption

- **Points Redemption**: Users can use points to redeem coupons
- **Redemption Process**:
  1. Verify user login status
  2. Verify points parameters and coupon name
  3. Check if user has sufficient points
  4. Deduct user points and recalculate rank
  5. Save coupon record to `user_coupon` collection
- **Coupon Records**: Coupon records are saved in the `user_coupon` collection in Firestore, including user ID, coupon name, deducted points, etc.

---

## IX. Error Code Description

- `code: 1` - Request successful
- `code: 0` - Request failed (see `msg` field for specific error information)

### Common Errors

1. **401 Unauthorized**: Token expired or invalid, need to login again
2. **404 Not Found**: Resource does not exist
3. **400 Bad Request**: Request parameter error
4. **500 Internal Server Error**: Internal server error

---

## X. Usage Examples

### 10.1 User Login Flow

```javascript
// 1. Frontend gets Firebase ID Token
const idToken = await firebase.auth().currentUser.getIdToken();

// 2. Call login endpoint
const response = await fetch("http://52.14.58.34:8082/api/user/login", {
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

// 3. Save Token
localStorage.setItem("accessToken", accessToken);
localStorage.setItem("refreshToken", refreshToken);
```

### 10.2 Third-Party Login Flow (Google/Facebook/Twitter)

```javascript
// 1. Use Firebase for third-party login (supports Google, Facebook, Twitter)
// Google login example
const googleProvider = new firebase.auth.GoogleAuthProvider();
const googleResult = await firebase.auth().signInWithPopup(googleProvider);

// Facebook login example
// const facebookProvider = new firebase.auth.FacebookAuthProvider();
// const facebookResult = await firebase.auth().signInWithPopup(facebookProvider);

// Twitter login example
// const twitterProvider = new firebase.auth.TwitterAuthProvider();
// const twitterResult = await firebase.auth().signInWithPopup(twitterProvider);

// 2. Get Firebase ID Token
const idToken = await googleResult.user.getIdToken();

// 3. Call third-party login endpoint
const response = await fetch("http://52.14.58.34:8082/api/user/third-party", {
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

// 4. Save Token
localStorage.setItem("accessToken", accessToken);
localStorage.setItem("refreshToken", refreshToken);
```

**Notes**:

- Supports third-party login via Google, Facebook, Twitter, etc.
- All third-party logins use Firebase to return a unified ID Token
- Automatically registers new users on first login
- Automatically extracts user information (email, username, avatar) from token

---

### 10.3 Create Survey and Submit Response

```javascript
// 1. Create survey
const createResponse = await fetch(
  "http://52.14.58.34:8082/api/survey/create",
  {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({
      title: "Test Survey",
      description: "Survey description",
      latitude: 42.3505,
      longitude: -71.1054,
      points: 10,
      questions: [
        {
          type: "single",
          content: "How satisfied are you with the survey?",
          required: true,
          options: [
            { content: "Very Satisfied", label: "A" },
            { content: "Satisfied", label: "B" },
          ],
        },
      ],
    }),
  }
);

const { data: surveyId } = await createResponse.json();

// 2. Submit response
const submitResponse = await fetch("http://52.14.58.34:8082/api/response/", {
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

### 10.4 Redeem Coupon with Points

```javascript
// 1. Get user accessToken (from login endpoint)
const accessToken = localStorage.getItem("accessToken");

// 2. Call points redemption endpoint
const response = await fetch("http://52.14.58.34:8082/api/coupon/redeem", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    Authorization: `Bearer ${accessToken}`,
  },
  body: JSON.stringify({
    requiredPoints: 100,
    couponName: "$10 Coupon",
  }),
});

const result = await response.json();

if (result.code === 1) {
  console.log("Redemption successful! Remaining points:", result.data);
} else {
  console.error("Redemption failed:", result.msg);
}
```

---

## XI. Important Notes

1. **Token Management**:

   - Access Token validity: 30 minutes
   - Refresh Token validity: 7 days
   - After token expiration, use Refresh Token to refresh

2. **Authentication Requirements**:

   - Most endpoints require Bearer Token authentication
   - Login, registration, and other endpoints do not require authentication
   - For testing, you can use TestController to create test users

3. **Data Types**:

   - All ID fields are String type (Firestore documentId)
   - Latitude and longitude use Double type
   - Time format: `yyyy-MM-dd HH:mm:ss`

4. **Points System**:
   - Points are only awarded when completing a survey for the first time
   - Survey must have `points` field set to award points
   - Points increase will automatically update rank information

---

## XII. Swagger UI

The project has integrated Swagger UI, accessible at:

- **Swagger UI**: `http://52.14.58.34:8082/swagger-ui/index.html`
- **API Documentation**: `http://52.14.58.34:8082/v3/api-docs`

---

**Document Version**: 1.0  
**Last Updated**: 2024-01-01  
**Maintainer**: Survey Team
