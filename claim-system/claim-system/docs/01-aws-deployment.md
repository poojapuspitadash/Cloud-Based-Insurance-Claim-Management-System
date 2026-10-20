# Deploying to AWS (EC2 + RDS + S3 + IAM + CloudWatch)

Use one region (e.g. `ap-south-1` Mumbai). Stay inside the Free Tier where possible (t3.micro / db.t3.micro) and
**delete everything when you are done** to avoid charges. Set a billing alarm first (Billing -> Budgets).

## 1. Build the JAR
```bash
mvn clean package -DskipTests      # -> target/claim-system-1.0.0.jar
```

## 2. S3 bucket (documents)
1. S3 -> Create bucket, e.g. `claimsys-docs-<yourname>-<random>`.
2. Keep **Block all public access = ON** (documents are served through the API after an authorisation check).
3. Default encryption: SSE-S3 (the app also requests it per upload).

## 3. IAM role for EC2 (no access keys!)
1. IAM -> Policies -> Create policy -> JSON -> paste `aws/iam-ec2-policy.json` (replace the bucket name). Name: `ClaimSystemS3Access`.
2. IAM -> Roles -> Create role -> AWS service -> EC2. Attach `ClaimSystemS3Access` and the AWS-managed `CloudWatchAgentServerPolicy`.
   Name: `ClaimSystemEC2Role`.
Principle of least privilege: the app may only put/get objects under `claims/*` of this one bucket.

## 4. RDS MySQL
1. RDS -> Create database -> MySQL -> Free tier template, `db.t3.micro`.
2. DB identifier `claimsdb`, master user `admin`, strong password. **Public access = No.**
3. Security group `rds-sg`: inbound MySQL 3306 **only from `ec2-sg`** (create `ec2-sg` first in step 5, then edit).
4. Initial database name: `claimsdb`.

## 5. EC2 instance
1. EC2 -> Launch instance -> Amazon Linux 2023, `t3.micro`, attach `ClaimSystemEC2Role` (Advanced -> IAM instance profile).
2. Security group `ec2-sg`: SSH 22 from **your IP only**; custom TCP 8080 from anywhere (use 443 via a load balancer for real use).
3. Connect and install Java:
```bash
sudo dnf install -y java-17-amazon-corretto-headless
sudo mkdir -p /opt/claim-system /var/log/claim-system
sudo chown ec2-user /opt/claim-system /var/log/claim-system
```
4. Copy the JAR: `scp -i key.pem target/claim-system-1.0.0.jar ec2-user@<public-ip>:/opt/claim-system/claim-system.jar`
5. Create the env file from `aws/claim-system.env.example` -> `/etc/claim-system.env` (fill in RDS endpoint, bucket, passwords), then `sudo chmod 600 /etc/claim-system.env`.
6. Install the service:
```bash
sudo cp claim-system.service /etc/systemd/system/     # from the aws/ folder
sudo systemctl daemon-reload
sudo systemctl enable --now claim-system
sudo systemctl status claim-system
curl http://localhost:8080/actuator/health            # {"status":"UP"}
```
Open `http://<ec2-public-ip>:8080` in a browser and log in with the `ADMIN_EMAIL` / `ADMIN_PASSWORD` you set.
`Restart=always` means systemd restarts the app if it crashes or the instance reboots.

## 6. CloudWatch
```bash
sudo dnf install -y amazon-cloudwatch-agent
sudo cp cloudwatch-agent-config.json /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -s \
  -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
```
Then in the console:
- **Logs -> Log groups -> `/claim-system/application`**: live application logs (claim created, status changes, upload errors).
- **Metrics**: EC2 CPUUtilization (built in), plus memory and disk from the agent.
- **Alarms**: CPU > 80% for 5 min; EC2 StatusCheckFailed; (optional) a metric filter on the word `ERROR` in the log group -> alarm -> SNS email.

## 7. Verify checklist
- [ ] Register a customer, submit a claim, upload a photo -> object appears in the S3 bucket under `claims/<id>/`.
- [ ] Rows appear in RDS (connect from EC2 with `mysql -h <endpoint> -u admin -p`).
- [ ] Employee can change status; log lines appear in CloudWatch.
- [ ] RDS is not reachable from the internet; the S3 bucket is not public.

## Clean-up (important)
Terminate EC2, delete RDS (skip final snapshot if just practising), empty and delete the S3 bucket, delete CloudWatch log group.
