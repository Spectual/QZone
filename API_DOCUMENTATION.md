# Survey System API Documentation

## Basic Information

API URL: http://52.14.58.34:8082/swagger-ui/index.html
- **Base URL**: `http://52.14.58.34:8082`
- **Content-Type**: `application/json`
- **Authentication**: Bearer Token (Add `Authorization: Bearer {accessToken}` in request header)

### Unified Response Format

All APIs return a unified `Result<T>` format:

```json
{
  "code": 1, // 1 indicates success, 0 or other numbers indicate failure
  "msg": "success", // Error message (when failed)
  "data": {}, // Response data (when successful)
  "success": true // Whether the operation was successful
}
```

---

## I. User APIs (`/api/user`) ⭐ Core APIs

### 1.1 User Login

**Endpoint**: `POST /api/user/login`

**Description**: User login (Firebase authentication supported)

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
    "refreshToken": "uuid-refresh-token"
  },
  "success": true
}
```

---

### 1.2 User Registration

**Endpoint**: `POST /api/user/register`

**Description**: User registration (Firebase authentication supported)

**Request Body**:

```json
{
  "firebaseToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userName": "user",
  "email": "user@example.com"
}
```

**Response**: Same as login endpoint (New users start with 0 points and "Bronze" tier)

---

### 1.3 Third-Party Login (Google/Facebook/Twitter Support)

**Endpoint**: `POST /api/user/third-party`

**Description**: Login via third-party accounts (Google, Facebook, Twitter supported through Firebase)

**Request Body**:

```json
{
  "tokenId": "firebase-id-token"
}
```

**Notes**:

- Supports Google, Facebook, Twitter, and other third-party logins
- All third-party logins return a unified ID Token through Firebase
- New users are automatically registered on first login
- User information (email, username, avatar) is automatically extracted from the token

**Response**: Same as login endpoint

---

### 1.4 Send Forgot Password Email (Missing forgot password UI)

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

**Request Header**: `Authorization: Bearer {accessToken}`

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

**Description**: Update email address

**Request Header**: `Authorization: Bearer {accessToken}`

**Request Body**:

```json
"newemail@example.com"
```

**Response**: Standard response format

---

### 1.9 Change Password (Not Implemented)

**Endpoint**: `PUT /api/user/password`

**Description**: Change password (Note: This feature is disabled after Firebase integration)

**Request Header**: `Authorization: Bearer {accessToken}`

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

**Request Header**: `Authorization: Bearer {accessToken}`

**Content Type**: `multipart/form-data`

**Request Parameter**: `file` (MultipartFile)

**Response**: Standard response format

---

### 1.11 User Logout

**Endpoint**: `POST /api/user/logout`

**Description**: User logout

**Request Header**: `Authorization: Bearer {accessToken}`

**Response**: Standard response format

---

### 1.12 Get User Profile

**Endpoint**: `GET /api/user/user/me`

**Description**: Get user profile information (including points and rank)

**Request Header**: `Authorization: Bearer {accessToken}`

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

**Description**: Verify if a user exists

**Path Parameter**: `userId` (String) - User documentId

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

**Path Parameter**: `id` (String) - User documentId

**Response**: User entity object

---

## II. Admin APIs (`/api/admin`) ⭐ Core APIs

### 2.1 Admin Login

**Endpoint**: `POST /api/admin/login`

**Description**: Admin login (Firebase authentication supported)

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

**Description**: Admin registration (Firebase authentication supported)

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

### 2.3 Third-Party Login (Google/Facebook/Twitter Support)

**Endpoint**: `POST /api/admin/third-party`

**Description**: Login via third-party accounts (Google, Facebook, Twitter supported through Firebase)

**Request Body**:

```json
{
  "tokenId": "firebase-id-token"
}
```

**Notes**:

- Supports Google, Facebook, Twitter, and other third-party logins
- All third-party logins return a unified ID Token through Firebase
- New admin users are automatically registered on first login
- User information (email, username, avatar) is automatically extracted from the token

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

**Request Header**: `Authorization: Bearer {accessToken}`

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

**Description**: Update admin email address

**Request Header**: `Authorization: Bearer {accessToken}`

**Request Body**:

```json
"newemail@example.com"
```

**Response**: Standard response format

---

### 2.9 Change Admin Password

**Endpoint**: `PUT /api/admin/password`

**Description**: Change admin password (Note: This feature is disabled after Firebase integration)

**Request Header**: `Authorization: Bearer {accessToken}`

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

**Request Header**: `Authorization: Bearer {accessToken}`

**Content Type**: `multipart/form-data`

**Request Parameter**: `file` (MultipartFile)

**Response**: Standard response format

---

### 2.11 Admin Logout

**Endpoint**: `POST /api/admin/logout`

**Description**: Admin logout

**Request Header**: `Authorization: Bearer {accessToken}`

**Response**: Standard response format

---

### 2.12 Get Admin Profile

**Endpoint**: `GET /api/admin/admin/me`

**Description**: Get admin profile information

**Request Header**: `Authorization: Bearer {accessToken}`

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

## III. Survey APIs (`/api/survey`) ⭐ Core APIs

### 3.1 Create Survey

**Endpoint**: `POST /api/survey/create`

**Description**: Create a survey (including questions and options)

**Request Body**:

```json
{
  "title": "Campus Dining Hall Satisfaction Survey",
  "description": "We sincerely invite you to participate in this satisfaction survey to improve our campus dining services.",
  "latitude": 42.3505,
  "longitude": -71.1054,
  "points": 10, // Survey points (optional, default is 0)
  "questions": [
    {
      "type": "single",
      "content": "How satisfied are you with the overall dining hall experience?",
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
      "content": "Which aspects of the dining hall need improvement? (Multiple choices allowed)",
      "required": false,
      "options": [
        { "content": "Food Taste", "label": "A" },
        { "content": "Food Variety", "label": "B" },
        { "content": "Price Reasonability", "label": "C" }
      ]
    },
    {
      "type": "text",
      "content": "Please provide your suggestions for dining hall improvement:",
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

### 3.2 Update Survey

**Endpoint**: `PUT /api/survey/update`

**Description**: Update survey information

**Request Body**:

```json
{
  "id": 123,
  "title": "Updated Survey Title",
  "description": "Updated Description",
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

**Path Parameter**: `id` (String) - Survey documentId

**Response**: Standard response format

---

### 3.4 Get Survey by ID

**Endpoint**: `GET /api/survey/{id}`

**Description**: Get survey details by documentId (including questions and options)

**Path Parameter**: `id` (String) - Survey documentId

**Response Example**:

```json
{
  "code": 1,
  "msg": null,
  "data": {
    "documentId": "survey-doc-id",
    "title": "Campus Dining Hall Satisfaction Survey",
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

**Description**: Get multiple surveys by their documentIds

**Request Body**:

```json
["survey-doc-id-1", "survey-doc-id-2", "survey-doc-id-3"]
```

**Response**: Array of Survey objects

---

### 3.6 Paginated Survey Query (Currently Unavailable)

**Endpoint**: `POST /api/survey/page`

**Description**: Get paginated list of surveys

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
        "description": "Survey Description",
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

**Path Parameter**: `id` (String) - Survey documentId

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

**Description**: Delete multiple surveys

**Request Body**:

```json
["survey-doc-id-1", "survey-doc-id-2"]
```

**Response**: Standard response format

---

### 3.9 Change Survey Status

**Endpoint**: `PUT /api/survey/{id}/status`

**Description**: Change survey status

**Path Parameter**: `id` (String) - Survey documentId

**Query Parameter**: `status` (String) - New status (e.g., ACTIVE, INACTIVE)

**Response**: Standard response format

---

### 3.10 Get Survey Questions

**Endpoint**: `GET /api/survey/question/{id}`

**Description**: Get list of questions by question documentId

**Path Parameter**: `id` (String) - Question documentId

**Response**: Array of Question objects

---

### 3.11 Get Question Options

**Endpoint**: `GET /api/survey/option/{id}`

**Description**: Get all options for a question by question documentId

**Path Parameter**: `id` (String) - Question documentId

**Response**: Array of Option objects

---

## IV. Response APIs (`/api/response`) ⭐ Core APIs

### 4.1 Submit Survey Response

**Endpoint**: `POST /api/response/`

**Description**: Submit survey response (user points will be automatically increased after completing the survey)

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
    "content": "Here are some suggestions..."
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

**Note**:

- Points will be automatically added to the user when completing a survey for the first time and if the survey has points set
- See the points system explanation at the beginning of the document for point rules

---

### 4.2 Query Response Details

**Endpoint**: `POST /api/response/detail/{id}`

**Description**: Query response details by response documentId

**Path Parameter**: `id` (String) - Response documentId

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

**Description**: Get paginated list of user responses

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

## V. Location APIs (`/api/location`) (Helper Functions)

### 5.1 Get Nearby Locations (POST)

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

### 5.2 Query Nearby Surveys (GET)

**Endpoint**: `GET /api/location/nearby`

**Description**: Query nearby surveys based on user location (GET method)

**Query Parameters**:

- `userLat` (Double) - User latitude
- `userLng` (Double) - User longitude
- `radiusKm` (Double) - Search radius (kilometers)

**Response**: Same as POST endpoint

---

### 5.3 Cache Survey Location Information

**Endpoint**: `POST /api/location/cache`

**Description**: Cache survey location information

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

### 5.4 Remove Survey Location Information

**Endpoint**: `DELETE /api/location/{surveyId}`

**Description**: Remove survey location information from cache

**Path Parameter**: `surveyId` (String) - Survey documentId

**Response**: Standard response format

---

### 5.5 Update Survey Location Information

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

### 5.6 Get Survey Location Information

**Endpoint**: `GET /api/location/{surveyId}`

**Description**: Get survey location information

**Path Parameter**: `surveyId` (String) - Survey documentId

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

## VI. Test APIs (`/api/test`) (Development Testing)

### 6.1 Create Test User

**Endpoint**: `POST /api/test/create-test-user`

**Description**: Create Firebase test user and get custom token (development testing only)

**Query Parameters**:

- `email` (String) - User email
- `name` (String) - Username
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
    "instructions": "Use customToken to call signInWithCustomToken() in frontend to get ID Token"
  },
  "success": true
}
```

---

### 6.2 Verify Firebase Token

**Endpoint**: `POST /api/test/verify-token`

**Description**: Verify Firebase ID Token (development testing only)

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

### 6.3 Get User Information

**Endpoint**: `GET /api/test/user/{uid}`

**Description**: Get user information by Firebase UID (development testing only)

**Path Parameter**: `uid` (String) - Firebase user UID

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

## VII. Points System Description

### 7.1 Points Rules

- **Points Earning**: When users complete a survey for the first time, if the survey has points set (in the `points` field), the system will automatically add the corresponding points to the user's account
- **Points Calculation**: After each points addition, the system automatically calculates the user's rank and points needed for the next rank

### 7.2 Ranking Rules

| Rank | Points Range | Points for Next Rank |
| ---- | ----------- | ------------------- |
| Bronze | 0-99 | 100 |
| Silver | 100-299 | 300 |
| Gold | 300-599 | 600 |
| Platinum | 600-999 | 1000 |
| Diamond | 1000-1999 | 2000 |
| Master | 2000-4999 | 5000 |
| Grandmaster | 5000+ | 0 (Highest Rank) |

### 7.3 Points-Related Fields

User entity includes the following points-related fields:

- `currentPoints` (Integer) - Current points
- `rank` (String) - Current rank
- `pointsToNextRank` (Integer) - Points needed for next rank

---

## VIII. Error Code Description

- `code: 1` - Request successful
- `code: 0` - Request failed (see `msg` field for specific error message)

### Common Errors

1. **401 Unauthorized**: Token expired or invalid, need to login again
2. **404 Not Found**: Resource does not exist
3. **400 Bad Request**: Invalid request parameters
4. **500 Internal Server Error**: Server internal error

---

## IX. Usage Examples

### 9.1 User Login Process

```javascript
// 1. Get Firebase ID Token from frontend
const idToken = await firebase.auth().currentUser.getIdToken();

// 2. Call login API
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

// 3. Save tokens
localStorage.setItem("accessToken", accessToken);
localStorage.setItem("refreshToken", refreshToken);
```

### 9.2 Third-Party Login Process (Google/Facebook/Twitter)

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

// 3. Call third-party login API
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

// 4. Save Token
localStorage.setItem("accessToken", accessToken);
localStorage.setItem("refreshToken", refreshToken);
```

**Notes**:

- Supports third-party login such as Google, Facebook, Twitter, etc.
- All third-party logins return a unified ID Token through Firebase
- New users are automatically registered on first login
- User information (email, username, avatar) is automatically extracted from the token

---

### 9.3 Create Survey and Submit Answers

```javascript
// 1. Create survey
const createResponse = await fetch("http://localhost:8082/api/survey/create", {
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
          { content: "Very satisfied", label: "A" },
          { content: "Satisfied", label: "B" },
        ],
      },
    ],
  }),
});

const { data: surveyId } = await createResponse.json();

// 2. Submit answers
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

## 10. Notes

1. **Token Management**:

   - Access Token validity period is 30 minutes
   - Refresh Token validity period is 7 days
   - After token expiration, Refresh Token must be used to refresh

2. **Authentication Requirements**:

   - Most interfaces require Bearer Token authentication
   - Login, registration, and other interfaces do not require authentication
   - TestController can be used to create test users during testing

3. **Data Types**:

   - All ID fields are String type (Firestore documentId)
   - Latitude and longitude use Double type
   - Time format: `yyyy-MM-dd HH:mm:ss`

4. **Points System**:
   - Points are only awarded when completing a survey for the first time
   - Surveys must have the `points` field set to award points
   - Point increases automatically update rank information

---

## 11. Swagger UI

The project has integrated Swagger UI, which can be accessed at the following address:

- **Swagger UI**: `http://localhost:8082/swagger-ui/index.html`
- **API Documentation**: `http://localhost:8082/v3/api-docs`

---

**Documentation Version**: 1.0  
**Last Updated**: 2024-01-01  
**Maintainer**: Survey Team
