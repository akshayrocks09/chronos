# Chronos: Enterprise-Grade Distributed Job Scheduler

**Chronos** is a high-performance, distributed-ready job scheduling system designed for reliability, transparency, and developer productivity. It combines a robust **Spring Boot** backend with a premium **React** interface, enabling real-time monitoring and management of complex background tasks.

---

## Table of Contents
- [Problem Statement](#problem-statement)
- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [System Architecture](#system-architecture)
- [Project Structure](#project-structure)
- [Installation & Setup](#installation--setup)
- [Environment Variables](#environment-variables)
- [API Endpoints](#api-endpoints)
- [Database Schema](#database-schema)
- [Security](#security)
- [Optimizations](#optimizations)
- [Challenges Faced](#challenges-faced)
- [Future Scope](#future-scope)
- [License](#license)
- [Contact](#contact)

---

## Problem Statement

Traditional cron systems and simple task queues often fail to provide:
1. **Visibility**: No real-time dashboard to monitor job status or execution history.
2. **Reliability**: Lack of built-in retry mechanisms with exponential backoff.
3. **Persistence**: Jobs are often lost if the server restarts.
4. **Ease of Management**: Modifying or pausing jobs requires code changes or direct DB access.

**Chronos** solves these by providing a centralized, persistent, and highly observable scheduling engine.

---

## Key Features

- **Advanced Scheduling**: Support for one-time (timestamp-based) and recurring (Cron-based) jobs.
- **Fault Tolerance**: Automatic retries with **Exponential Backoff** (10s, 20s, 40s...).
- **Premium Dashboard**: Glassmorphic UI for real-time monitoring of job lifecycles.
- **Secure by Design**: JWT-based authentication and Role-Based Access Control (RBAC).
- **Execution Logs**: Detailed audit trails for every job execution, including failure stack traces.
- **Containerized**: Fully Dockerized for seamless deployment across environments.
- **Smart Resync**: Quartz-powered persistence ensures missed triggers are handled after downtime.

---

## Tech Stack

### Frontend
- **Framework**: React 18 (Vite)
- **Styling**: Tailwind CSS + Glassmorphism
- **Routing**: HashRouter (for GH Pages compatibility)
- **State Management**: React Context API
- **Icons**: Lucide React

### Backend
- **Framework**: Spring Boot 3.2.3
- **Language**: Java 21
- **Scheduler**: Quartz Scheduler (Clustered)
- **Security**: Spring Security + JWT
- **ORM**: Spring Data JPA

### Infrastructure & DevOps
- **Database**: MySQL 8.0
- **CI/CD**: GitHub Actions
- **Containerization**: Docker & Docker Compose
- **Hosting**: GitHub Pages (Frontend), Render (Recommended for Backend)

---

## System Architecture

<p align="center">
  <img src="./assets/architecture.png" alt="Chronos Architecture" width="700" />
</p>

Chronos follows a classic **N-tier architecture**:
1. **Presentation Layer**: React-based SPA communicating via REST.
2. **Security Layer**: Stateless JWT filtering for all API requests.
3. **Service Layer**: Business logic for job management and validation.
4. **Scheduling Layer**: Quartz handles trigger management and thread pooling.
5. **Persistence Layer**: MySQL stores job definitions, logs, and user data.

---

## Project Structure

```bash
chronos/
├── chronos/                    # Backend Source
│   ├── src/
│   │   ├── main/java/com/chronos/
│   │   │   ├── config/         # App & Security Configurations
│   │   │   ├── controller/     # REST Endpoints
│   │   │   ├── dto/            # Data Transfer Objects
│   │   │   ├── entity/         # Database Models
│   │   │   ├── repository/     # Data Access Layer
│   │   │   ├── service/        # Business Logic
│   │   │   └── security/       # JWT & Auth Logic
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/changelog/   # Liquibase Migrations
│   └── pom.xml
├── chronos-ui/                 # Frontend Source
│   ├── src/
│   │   ├── api/                # Axios Client & Interceptors
│   │   ├── components/         # Reusable UI Components
│   │   ├── context/            # Auth & Global State
│   │   └── pages/              # View Components
│   ├── package.json
│   └── vite.config.js
├── .github/workflows/          # CI/CD Pipelines
├── assets/                     # Project Media
├── docker-compose.yml
└── README.md
```

---

## Installation & Setup

### Prerequisites
- **Java 21**
- **Node.js 18+**
- **Docker Desktop**

### 1. Clone the Repository
```bash
git clone https://github.com/akshayrocks09/chronos.git
cd chronos
```

### 2. Start the Database
```bash
docker-compose up -d
```

### 3. Setup Backend
```bash
cd chronos/chronos
# Build and run
mvn spring-boot:run
```

### 4. Setup Frontend
```bash
cd chronos/chronos/chronos-ui
npm install
npm run dev
```
Navigate to `http://localhost:5173`.

---

## API Endpoints

The system exposes a RESTful API secured by JWT. Base URL: `/api`

### 1. Authentication (`/auth`)
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/register` | Register a new user account | No |
| POST | `/login` | Authenticate and receive JWT | No |

### 2. Jobs (`/jobs`)
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/` | List all jobs for current user (paginated) | Yes |
| POST | `/` | Create a new One-time or Cron job | Yes |
| GET | `/{id}` | Get detailed status of a specific job | Yes |
| PUT | `/{id}` | Update/Reschedule a job | Yes |
| PATCH | `/{id}/cancel` | Cancel a running/scheduled job | Yes |
| DELETE | `/{id}` | Permanently delete a job and its logs | Yes |
| POST | `/{id}/trigger` | Manually trigger a job to run immediately | Yes |

### 3. Monitoring & Logs (`/jobs`)
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/logs` | Get execution history for all my jobs | Yes |
| GET | `/{id}/logs` | Get execution history for a specific job | Yes |
| GET | `/stats` | Get personal job success/failure statistics | Yes |

### 4. Admin (`/admin`)
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/jobs` | View every job in the system (all users) | ADMIN |
| GET | `/logs` | View every execution log in the system | ADMIN |
| GET | `/stats` | System-wide health and throughput stats | ADMIN |

---

## Database Schema

Chronos uses a relational schema optimized for high-concurrency scheduling.

1. **Users**: Stores credentials (BCrypt hashed) and roles.
2. **Jobs**: Stores job definitions (payloads, cron expressions, target timestamps).
3. **Job Execution Logs**: Stores audit trails, execution durations, and failure stack traces.
4. **Quartz Tables**: Internal tables managed by Quartz for clustering and misfire handling (prefixed with `QRTZ_`).

---

## How to Use

### 1. Access the Dashboard
Navigate to `http://localhost:5173`. Register a new account or login with your admin credentials.

### 2. Create a One-Time Job
1. Click **"New Job"**.
2. Select **"One-Time"** type.
3. Enter the Job Name, Description, and a future **Timestamp**.
4. Set the **Target URL** (the endpoint Chronos will hit when the time comes).
5. Click **Schedule**.

### 3. Create a Recurring Job
1. Click **"New Job"**.
2. Select **"Recurring"** type.
3. Provide a standard **Cron Expression** (e.g., `0 0/5 * * * ?` for every 5 minutes).
4. Chronos will automatically calculate and display the **"Next Run Time"**.

### 4. Monitor Execution
Watch the **Status Icons** on the dashboard:
- 🔵 **Scheduled**: Waiting for the next trigger.
- 🟡 **Running**: Currently being executed by a worker.
- ✅ **Completed**: Successfully finished.
- ❌ **Failed**: Execution failed; check logs for the stack trace.

---

## Environment Variables

### Backend (.env)
```env
DB_URL=jdbc:mysql://localhost:3307/chronos_db
DB_USERNAME=root
DB_PASSWORD=root
JWT_SECRET=your_base64_encoded_secret_key_here
ADMIN_PASSWORD=your_secure_admin_password
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
```

### Frontend (.env)
```env
VITE_API_URL=http://localhost:8080/api
```

---

## Design Decisions

### 1. Quartz Scheduler vs. Spring `@Scheduled`
We chose **Quartz** because standard Spring scheduling is in-memory and non-persistent. Quartz allows us to:
- Persist jobs in a database so they survive server restarts.
- Support **Clustering**: Multiple backend instances can share the same job pool without duplicate executions.
- Handle **Misfires**: If the server is down during a scheduled time, Quartz can catch up once it reboots.

### 2. Stateless Security (JWT)
To support horizontal scaling, we use stateless JWT authentication. This ensures that any backend node can verify a user request without needing session sharing (Redis/Sticky Sessions).

### 3. Fail-Fast Startup
The system validates critical environment variables (like `JWT_SECRET` and `DB_URL`) at boot time. If these are missing, the application refuses to start, preventing insecure or broken deployments.

---

## Security

- **Stateless Authentication**: Uses JWT (JSON Web Tokens) for all secured routes.
- **Password Hashing**: BCrypt with a high cost factor.
- **RBAC**: Role-Based Access Control (ADMIN/USER) for job and log visibility.
- **Input Validation**: Strict DTO validation to prevent injection attacks.
- **Fail-Fast Policy**: System won't boot without a secure `JWT_SECRET`, `ADMIN_PASSWORD`, and `MAIL_` credentials.

---

## Optimizations

- **Quartz Clustering**: Prepared for horizontal scaling with database-backed job store.
- **Lazy Loading**: React components are lazy-loaded to minimize initial bundle size.
- **Transactional Consistency**: Complex transaction management ensuring logs are persisted even if jobs fail.
- **Interceptor Logic**: Centralized JWT management and error handling in Axios.

---

## Challenges Faced

1. **Transactional Integrity**: Ensuring that job execution logs were saved correctly even when the primary job transaction failed.
2. **Scheduling Sync**: Handling "Misfires" when the application is down and syncing Quartz triggers with the database state.
3. **Responsive UI**: Designing a glassmorphic dashboard that remains functional and beautiful across different screen sizes.

---

## Future Scope

- **Kubernetes Orchestration**: Auto-scaling worker nodes based on job queue depth.
- **Webhook Integration**: Triggering external services directly from the scheduler.
- **Email/Slack Alerts**: Real-time alerting for failed critical jobs.
- **AI Analytics**: Predictive scheduling based on job duration history.

---

## License

Distributed under the MIT License. See `LICENSE` for more information.

---

## Contact

**Akshay Thapa**  
- **LinkedIn**: [akshaythapa](https://www.linkedin.com/in/akshaythapa/)
- **Email**: [akshay1509thapa@gmail.com](mailto:akshay1509thapa@gmail.com)
