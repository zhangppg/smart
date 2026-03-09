# Photo Manager (Java + Vue2)

A photo management app with authentication, category/tag management, and paginated search.

## Stack
- Backend: Spring Boot (Java 17)
- Database: MySQL 8+
- Frontend: Vue 2 + Axios

## Features
- User register/login (token auth)
- User-level data isolation (each user only sees own photos)
- Photo upload with category and tags
- Photo preview/download/delete
- Search by keyword (title/category/tag)
- Filter by category and tag
- Pagination
- Local file + metadata persistence (photos)

## Project structure
- `backend`: Spring Boot API (`http://localhost:8080`)
- `frontend`: Vue2 app (`http://localhost:8081`)

## 1) Create MySQL database

```sql
CREATE DATABASE IF NOT EXISTS photo_manager DEFAULT CHARACTER SET utf8mb4;
```

## 2) Configure backend database

Default config in `backend/src/main/resources/application.yml`:
- DB URL: `jdbc:mysql://localhost:3306/photo_manager?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai`
- Username: `root`
- Password: `root`

You can override with env vars:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

On startup, backend executes `schema.sql` automatically and creates:
- `user`
- `user_rule`

## 3) Run backend

Requirements:
- Java 17+
- Maven 3.9+

```bash
cd backend
mvn spring-boot:run
```

Backend storage defaults:
- Files: `backend/data/uploads`
- Photos metadata: `backend/data/photos.json`

## 4) Run frontend

Requirements:
- Node.js 16+
- npm

```bash
cd frontend
npm install
npm run serve
```

Optional API host override:

```bash
# frontend/.env.local
VUE_APP_API_BASE_URL=http://localhost:8080
```

## Auth API
- `POST /api/auth/register` body: `{ "username": "demo", "password": "123456" }`
- `POST /api/auth/login` body: `{ "username": "demo", "password": "123456" }`

Response:
- `{ "token": "...", "username": "demo" }`

## Photos API (require token)
Send header:
- `Authorization: Bearer <token>`

Endpoints:
- `POST /api/photos` (multipart: `file`, optional `title`, `category`, `tags`)
- `GET /api/photos?q=&category=&tag=&page=1&size=12`
- `GET /api/photos/{id}/view`
- `GET /api/photos/{id}/download`
- `DELETE /api/photos/{id}`
