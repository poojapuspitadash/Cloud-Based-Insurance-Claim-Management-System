# Resume bullets and interview preparation (Guidewire Associate Consultant Intern)

## Resume entry (edit to match what you actually built and deployed)
**Cloud-Based Insurance Claim Management System** | Java, Spring Boot, MySQL, AWS (EC2, RDS, S3, IAM, CloudWatch) | GitHub link
- Built a REST API in Java/Spring Boot for the full property-and-casualty claim lifecycle (submit -> review -> approve/reject) with server-enforced status transitions and role-based access for customers and claims employees.
- Deployed on AWS EC2 with MySQL on Amazon RDS and claim documents in Amazon S3, using an IAM role with least-privilege access instead of stored credentials.
- Added claim risk scoring (LOW/MEDIUM/HIGH) to help employees prioritise reviews; wrote integration tests covering the workflow and authorisation rules.
- Monitored with CloudWatch Logs and alarms for CPU and application errors.

Only claim "deployed on AWS" if you actually did step 5 of the deployment guide. Interviewers at Guidewire do in-person interviews and will ask you to explain details.

## Resume gaps for this internship (from the job description)
- Put **Java / OOP** first in your skills; list REST, JSON, HTTP, SQL, AWS, Git, Agile/Scrum.
- Mention **agile**: you can honestly say you worked in small iterations/user stories on this project (see a README "backlog" if you add one).
- Add any **P&C insurance** awareness: policy, claim, premium, FNOL (first notice of loss), adjuster, underwriting.
- Show **communication/consulting** (clubs, presentations, team projects).

## Likely questions and short answers
**What is the project?** A web app where customers file insurance claims and employees process them; Spring Boot REST backend on EC2, MySQL on RDS, documents on S3.

**Why EC2 / RDS / S3 / IAM / CloudWatch?**
- EC2: full control of the server running the Java app. (Alternative: Elastic Beanstalk or ECS to reduce ops work.)
- RDS: managed MySQL with backups, patching and optional Multi-AZ; I don't administer a DB server.
- S3: cheap, durable object storage for files; keeps large binaries out of the database.
- IAM: the EC2 role grants only `PutObject/GetObject` on one bucket, so no keys in code and a small blast radius.
- CloudWatch: central logs, metrics and alarms so I can see errors and CPU without logging in to the server.

**What is a REST API?** Resources identified by URLs, manipulated with HTTP verbs (GET/POST/PATCH), stateless, JSON bodies, meaningful status codes (201 created, 400 validation, 401/403 auth, 404, 409 conflict).

**Where is OOP used?** Entities (`Claim`, `AppUser`), enum with behaviour (`ClaimStatus.canMoveTo`), an interface with two implementations (`FileStorageService` -> local disk / S3) chosen by Spring profile, dependency injection, single-responsibility services.

**How is the claim workflow enforced?** In `ClaimStatus.canMoveTo` and `ClaimService.updateStatus`; invalid jumps return 409 Conflict. The UI only shows valid buttons, but the server is the source of truth.

**How did you secure it?** BCrypt passwords, role-based authorisation, ownership checks (customers can't read others' claims), upload type/size limits, sanitised file names, private S3 bucket, private RDS, IAM role, secrets in env vars, HTTPS in production (known limitation: HTTP Basic -> would move to JWT/OAuth2).

**How did you test the APIs?** `ClaimWorkflowTest` (MockMvc integration test) + Postman/curl manually.

**How would you scale it?** Put 2+ EC2 instances in an Auto Scaling group behind an Application Load Balancer; RDS Multi-AZ plus read replicas; app is stateless and files live in S3, so instances are interchangeable; add pagination to list endpoints; cache hot reads.

**What if the server goes down?** systemd restarts the process; with an ASG + ALB health checks (`/actuator/health`) a failed instance is replaced automatically; data is safe in RDS/S3 (backups, Multi-AZ).

**Honest limitations to mention:** single instance, HTTP Basic auth, rule-based (not trained) risk scoring, no pagination yet. Naming limits shows maturity.

## Insurance vocabulary
Policy, policyholder, premium, deductible, FNOL (first notice of loss), claim adjuster, reserve, settlement, subrogation, underwriting, P&C (property and casualty). Guidewire builds ClaimCenter (claims), PolicyCenter (policies) and BillingCenter (billing); this project is a small version of the ClaimCenter idea.
