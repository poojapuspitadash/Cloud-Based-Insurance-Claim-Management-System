# Cloud-Based Insurance Claim Management System

Customers submit and track insurance claims online; insurance employees review, approve or reject them.
Built with **Java 17, Spring Boot 3, REST APIs, MySQL (AWS RDS), AWS S3, EC2, IAM and CloudWatch**.

```
Browser (HTML/JS/Bootstrap)
        |  HTTPS / REST (JSON)
        v
Spring Boot on AWS EC2 ---- logs ----> CloudWatch Logs
   |             |
   v             v
AWS RDS MySQL   AWS S3 (claim photos / PDFs)      Access to S3 via IAM role on the EC2 instance
```

## Features
| Customer | Employee |
|---|---|
| Register / login | View all claims, search and filter by status |
| Submit claim (type, date, amount, description) | Review uploaded documents |
| Upload PDF/JPG/PNG evidence | Move claims through the workflow with a note |
| Track status, view past claims | Dashboard counts per status |

**Workflow (enforced by the server):** `SUBMITTED -> UNDER_REVIEW -> APPROVED / REJECTED`. Approved/Rejected are final.

**Priority scoring:** every claim gets a risk score (0-100) and LOW/MEDIUM/HIGH priority from rules in
`RiskScoringService` (amount, claim type, late reporting). It is the hook for the optional Python ML service (see `docs/03-extensions.md`).

## Run it locally (no AWS needed)
Requirements: JDK 17+ and Maven 3.9+ (or open the folder in IntelliJ / VS Code / Eclipse).

```bash
mvn spring-boot:run          # starts on http://localhost:8080
mvn test                     # runs the end-to-end workflow tests
```
Local mode uses an in-memory H2 database and saves uploads to `./uploads`.

Demo logins (local only):
- Customer: `rahul@example.com` / `Customer@123`
- Employee: `employee@claims.com` / `Employee@123`

## API (HTTP Basic auth; test with Postman or curl)
| Method | Path | Who | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | public | Create customer account |
| GET | `/api/auth/me` | any | Current user |
| POST | `/api/claims` | customer | Create claim |
| GET | `/api/claims` | customer | My claims |
| GET | `/api/claims/{id}` | owner / employee | One claim |
| POST | `/api/claims/{id}/documents` | owner / employee | Upload file (`multipart`, field `file`) |
| GET | `/api/claims/{id}/documents/{docId}` | owner / employee | Download file |
| GET | `/api/employee/claims?status=&q=` | employee | Search/filter all claims |
| PATCH | `/api/employee/claims/{id}/status` | employee | Change status `{ "status": "UNDER_REVIEW", "note": "..." }` |
| GET | `/api/employee/claims/stats` | employee | Counts per status |
| GET | `/actuator/health` | public | Health check |

```bash
curl -u rahul@example.com:Customer@123 -H "Content-Type: application/json" \
  -d '{"type":"Accident","description":"Rear-ended","amount":50000,"incidentDate":"2026-10-01"}' \
  http://localhost:8080/api/claims
```

## Deploy on AWS
Follow `docs/01-aws-deployment.md` (RDS -> S3 -> IAM role -> EC2 -> CloudWatch).

## Project layout
```
src/main/java/com/claimsys
  controller/   REST endpoints + error handling
  service/      business rules, S3/local storage, risk scoring
  repository/   Spring Data JPA
  model/        entities + enums
  config/       security, AWS client, seed data
src/main/resources   application.yml (local) + application-aws.yml, logback, static frontend
aws/            IAM policy, CloudWatch agent config, systemd unit, env template
database/       reference MySQL schema
docs/           deployment guide, resume + interview notes, extensions
```

## Security notes
- Passwords are BCrypt-hashed; public sign-up can only create CUSTOMER accounts.
- Customers can only see their own claims (others return 404); employee endpoints require ROLE_EMPLOYEE.
- Uploads: type allow-list (PDF/JPEG/PNG), 5 MB limit, sanitised file names, random S3 keys, S3 server-side encryption.
- No AWS keys in code: the EC2 instance uses an IAM role. Secrets come from environment variables.
- **Before real use:** put the app behind HTTPS (ALB + ACM certificate or Nginx + Let's Encrypt). HTTP Basic sends credentials on every request, so it is only safe over HTTPS; a production version would use JWT/OAuth2.
