# ⏳ Chronos: Enterprise-Grade Job Scheduler

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue)](https://reactjs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-orange)](https://www.mysql.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Chronos** is a powerful, distributed-ready job scheduling system designed for reliability and ease of use. It combines a robust Spring Boot backend with a premium, modern React interface to give you full control over your background tasks.

---

## ✨ Key Features

### 🚀 Advanced Scheduling
- **One-Time Jobs**: Schedule tasks to run at a specific timestamp.
- **Recurring Jobs**: Use powerful **Cron expressions** (standard Quartz format) for complex repetition logic.
- **Payload Flexibility**: Attach JSON payloads (HTTP requests, log messages, etc.) to any job.

### 🛡️ Reliability & Fault Tolerance
- **Exponential Backoff**: Failed jobs automatically retry with increasing delays (10s, 20s, 40s...).
- **Quartz Persistence**: Jobs are stored in the database, ensuring no tasks are lost if the server restarts.
- **Smart Resync**: Automatically detects and handles missed triggers during system downtime.
- **Transactional Consistency**: Multi-layered transaction design ensures logs and status updates are never rolled back on failure.

### 💎 Premium User Experience
- **Real-Time Notifications**: Floating UI notifications for job successes, failures, and retries.
- **Admin Dashboard**: Global view for administrators to monitor and manage jobs across the entire system.
- **Interactive UI**: Modern, glassmorphic design built with React and TailwindCSS.

---

## 🛠️ Tech Stack

- **Backend**: Java 21, Spring Boot 3, Spring Security (JWT), Spring Data JPA.
- **Database**: MySQL 8.0, Liquibase (Schema Versioning).
- **Scheduler**: Quartz Scheduler (Clustered-ready).
- **Frontend**: React 18, Vite, TailwindCSS, HeroIcons.
- **Infrastructure**: Docker & Docker Compose.

---

## 🚦 Getting Started

### Prerequisites
- **Java 21** or higher
- **Node.js 18+**
- **Docker Desktop**

### 1. Database Setup
Start the MySQL instance using Docker:
```bash
docker compose up -d
```
*Note: The database will be available on `localhost:3307`.*

### 2. Backend Startup
Configure the environment variables and run the application:

**Windows (PowerShell):**
```powershell
$env:SPRING_PROFILES_ACTIVE="mysql"
$env:JWT_SECRET="YOUR_BASE64_SECRET"
$env:ADMIN_PASSWORD="SecureAdminPassword123!"
$env:DB_URL="jdbc:mysql://localhost:3307/chronos_db?useSSL=false"
mvn spring-boot:run
```

### 3. Frontend Startup
Install dependencies and start the development server:
```bash
cd chronos-ui
npm install
npm run dev
```
Navigate to `http://localhost:5173`.

---

## 🌐 Deployment

### Frontend (GitHub Pages)
The frontend is configured for automatic deployment to GitHub Pages via GitHub Actions.
1. Push changes to the `main` branch.
2. GitHub Actions will build the UI and deploy it to the `gh-pages` branch.
3. The site will be available at `https://<your-username>.github.io/chronos/`.

*Note: Since GitHub Pages hosts static files, you must configure the `VITE_API_URL` environment variable in your deployment to point to your live backend.*

---

## 🔒 Security Configuration

Chronos enforces a **fail-fast** security policy. You MUST provide the following environment variables for the system to boot:

- `JWT_SECRET`: A Base64 encoded string (min 32 bytes) for token signing.
- `ADMIN_PASSWORD`: To initialize the default `admin` account safely.

---

## 📖 API at a Glance

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/api/auth/login` | `POST` | Authenticate and receive JWT. |
| `/api/jobs` | `POST` | Create a new One-Time or Recurring job. |
| `/api/jobs` | `GET` | List all jobs for the current user. |
| `/api/admin/jobs` | `GET` | (Admin Only) List all jobs in the system. |
| `/api/jobs/{id}/logs` | `GET` | View execution history and failure reasons. |

---

## 🤝 Contributing

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---

*Built with ❤️ by the Chronos.*
