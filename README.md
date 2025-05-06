# 📬 Outlook API Integration - Java Spring Boot

This project is an **API developed with Java Spring Boot**, designed to integrate with **Microsoft Outlook (Microsoft Graph API)** for:

✅ Authenticate using **Authorization Code Flow**  
✅ Obtain **Access Token** and **Refresh Token**  
✅ List emails  
✅ Search specific emails by text or ID  
✅ Retrieve complete email threads (conversation)  
✅ Swagger/OpenAPI documentation

---

## 📁 Project Structure (MVC)

```
├── controllers       // Responsible for exposing REST endpoints
├── services          // Contains business logic and Graph API integration
├── repository        // Utility class for Microsoft Graph API calls
├── dto               // Data Transfer Objects
└── config            // Swagger and WebClient configurations
```

---

## 🔐 OAuth2 Authentication Flow (Authorization Code Flow)
1. Frontend redirects to Microsoft Login.
2. User authorizes permissions.
3. Frontend receives `code` and `redirect_uri`.
4. API receives the `code` and returns `access_token` and `refresh_token`.

---

## 📡 Available Endpoints

### 🔑 AuthController
```
POST /oauth/token
Body: {
  "code": "...",
  "redirectUri": "...",
  "clientId": "...",
  "clientSecret": "..."
}
```
➡ Returns access_token and refresh_token

---
### 📥 EmailController
```
GET /emails/latest?accessToken=...              // List latest emails
GET /emails/search?accessToken=...&text=...     // Search emails by text
GET /emails/read?accessToken=...&messageId=...  // Read email by ID
GET /emails/thread?accessToken=...&messageId=...// Get full conversation thread
```

---

## 📑 Swagger UI
Access the interactive documentation via browser:
```
http://localhost:8080/swagger-ui.html
or
http://localhost:8080/swagger-ui/index.html
```

If it does not open, check that you have added the correct dependency in `pom.xml`:
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.2.0</version>
</dependency>
```

---

## 🚀 How to Run Locally
```bash
# Clone the project
git clone https://github.com/Tercilio/outlook-api-integration.git

# Enter the project directory
cd outlook-api-integration

# Compile and run
./mvnw spring-boot:run
```

---

## ✍ About the Author
**Developer:** [Tercilio](https://github.com/Tercilio) 💻

---

If this project was helpful to you, ⭐ give it a star on GitHub and feel free to contribute improvements! 😄

"# outlook-api" 
