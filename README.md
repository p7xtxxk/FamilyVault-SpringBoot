# Family Document Vault

A secure, private web application for storing and sharing important family PDF documents — passports, insurance papers, property documents, etc. — with AES-256 encryption at rest and OTP-based login.

## Demonstration

<img src="demo.gif" alt="Demonstration Video" width="100%" autoplay loop />

---

## Features

- **Striking Neo-Brutalism Theme** — Features a bold, high-contrast, and interactive modern web design
- **Email OTP login** — no passwords, 6-digit codes valid for 5 minutes
- **AES-256 encryption** — all PDFs encrypted before touching disk
- **Protected document access** — files never served directly; decrypted on-the-fly per authenticated request
- **Admin-only uploads** — only the designated admin email can add/delete documents
- **Rate-limited OTPs** — max 5 OTP requests per email per hour
- **10 document limit** — perfect for a small family vault
- **First-login username** — family members choose a display name on first login

---

## Quick Start

### 1. Clone & configure

```bash
git clone <your-repo>
cd family-doc-vault
cp .env.example .env
# Edit .env with your values
```

### 2. Start Backend (Spring Boot)

```bash
cd backend
./mvnw spring-boot:run
```
Backend runs on: http://localhost:8080

### 3. Start Frontend (React/Vite)

```bash
cd frontend
npm install
npm run dev
```
Frontend runs on: http://localhost:5173 (or as configured by Vite)

---

## Environment Variables

| Variable | Description | Example |
|---|---|---|
| `MONGO_URI` | MongoDB connection string | `mongodb+srv://...` |
| `JWT_SECRET` | Secret for JWT signing (random string) | `abc123xyz...` |
| `AES_KEY` | 32-character AES-256 key | `MyFamilyVaultKey1234567890ABCD!` |
| `ADMIN_EMAIL` | Email that can upload/delete documents | `dad@family.com` |
| `SMTP_HOST` | SMTP server | `smtp.gmail.com` |
| `SMTP_PORT` | SMTP port | `587` |
| `SMTP_USER` | Gmail address | `vault@gmail.com` |
| `SMTP_PASS` | Gmail App Password | `xxxx xxxx xxxx xxxx` |
| `APP_NAME` | Display name in emails | `Family Document Vault` |

### Gmail Setup
1. Enable 2-Factor Authentication on your Gmail
2. Go to **Google Account → Security → App Passwords**
3. Generate a new App Password for "Mail"
4. Use that 16-character password as `SMTP_PASS`

---

## Deployment

This application consists of a decoupled frontend and backend. 
- **Backend**: Can be deployed to platforms like Railway, Render, or Heroku using the Maven wrapper (`./mvnw clean package` to build the JAR). Ensure `secure_docs/` has a persistent disk mount.
- **Frontend**: Can be deployed easily to Vercel, Netlify, or Render Static Sites using `npm run build`.

---

## MongoDB Atlas Setup (Free Tier)

1. Create a free cluster at [mongodb.com/atlas](https://www.mongodb.com/atlas)
2. Create a database user
3. Whitelist all IPs: `0.0.0.0/0` (required for Render)
4. Copy the connection string to `MONGO_URI`

The app auto-creates these collections with indexes:
- `users` — family member accounts
- `documents` — document metadata (no file content)
- `otp_codes` — TTL-indexed (auto-deletes after expiry)

---

## Project Structure

```text
FamilyVault/
├── .env                          # Environment variables configuration
├── .gitignore                    # Git ignore rules
├── README.md                     # Project documentation
├── secure_docs/                  # Directory where AES-encrypted (.enc) files are stored
├── backend/                      # Java Spring Boot Backend
│   ├── pom.xml                   # Maven dependencies and build configuration
│   └── src/main/
│       ├── java/com/familyvault/
│       │   ├── FamilyVaultApplication.java  # Main application entry point
│       │   ├── config/           # Configuration classes (e.g., Security, CORS)
│       │   ├── controller/       # REST API Endpoints (Auth, Documents)
│       │   ├── model/            # MongoDB Document Entities (User, OtpCode, VaultDocument)
│       │   ├── repository/       # Spring Data MongoDB Repositories
│       │   └── service/          # Core Business Logic (Auth, Document, Encryption, Jwt, Otp)
│       └── resources/
│           └── application.properties # Spring Boot configuration properties
└── frontend/                     # React + Vite Frontend
    ├── package.json              # NPM dependencies and scripts
    ├── vite.config.js            # Vite bundler configuration
    ├── index.html                # Main HTML template
    └── src/
        ├── api.js                # Axios instance configuration
        ├── index.css             # Global Neo-Brutalism styles
        ├── main.jsx              # React application entry point
        ├── pages/                # React Components (LoginPage.jsx, DashboardPage.jsx, UploadPage.jsx)
        └── styles/               # Modular CSS files
```

---

## Security Notes

- `secure_docs/` contains encrypted `.enc` files; even if accessed directly they are unreadable without the AES key
- Documents are **only** accessible via `/api/get_document/{id}` which requires a valid JWT cookie
- JWT tokens expire after 72 hours
- OTPs expire after 5 minutes and are single-use
- Set `secure=True` on cookies when deploying with HTTPS (update `COOKIE_SETTINGS` in `auth_routes.py`)

---

## API Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/request-otp` | None | Send OTP to email |
| POST | `/api/auth/verify-otp` | None | Verify OTP, receive session cookie |
| POST | `/api/auth/set-username` | None | Set username (first login) |
| POST | `/api/auth/logout` | None | Clear session cookie |
| GET | `/api/auth/me` | User | Get current user info |
| GET | `/api/documents` | User | List all document metadata |
| GET | `/api/get_document/{id}` | User | Decrypt & stream PDF |
| POST | `/api/upload-document` | Admin | Upload & encrypt PDF |
| DELETE | `/api/documents/{id}` | Admin | Delete document & file |

## Project Directory Analysis

- **Backend (`backend/`)**: Contains the core application logic built with Java and Spring Boot. Key components include:
  - `AuthService.java` & `JwtService.java` – JWT creation/verification and authentication logic.
  - `OtpService.java` – OTP generation, MongoDB storage, and email sending via JavaMailSender.
  - `EncryptionService.java` – In-memory AES‑256-CBC encrypt/decrypt logic with random IV generation.
  - `DocumentService.java` – File upload, retrieval, and deletion.
  - `controller/` – REST API endpoints for frontend consumption.
- **Frontend (`frontend/`)**: A modern Single Page Application built with React and Vite. It utilizes React Router for navigation (`LoginPage.jsx`, `DashboardPage.jsx`, `UploadPage.jsx`) and Axios for API requests, styled under a **Neo-Brutalism** design theme.
- **Encrypted Storage (`secure_docs/`)**: Holds AES‑encrypted `.enc` files. Files are never stored or processed on disk in plaintext.
- **Configuration**: Environment variables are defined in `.env` and consumed by Spring Boot's `application.properties`.
- **Security**: Encrypted documents are only decrypted in memory on-the-fly per authenticated request; JWTs expire after 72 hours; OTPs are single‑use and expire after 5 minutes.

---

## Authors

[Prateek Priyanshu](https://github.com/p7xtxxk)