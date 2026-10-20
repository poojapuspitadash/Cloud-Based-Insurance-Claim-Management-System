# Optional extensions (do these after the core works)

1. **Python ML priority service** - train a scikit-learn classifier (features: amount, claim type, days to report), expose
   `POST /predict -> {"risk": 82, "priority": "HIGH"}` with FastAPI, and call it from `RiskScoringService.score()` with
   `RestClient`, falling back to the existing rules if the service is down.
2. **Pagination** - change `search` to return `Page<Claim>` (`Pageable`).
3. **JWT login** - replace HTTP Basic with a login endpoint that issues short-lived JWTs.
4. **Email notifications** - Amazon SES when status changes.
5. **CI/CD** - GitHub Actions: `mvn test` on every push, then upload the JAR to S3 and restart the service.
6. **Infrastructure as code** - recreate the AWS setup with Terraform or CloudFormation.
7. **Swagger/OpenAPI** - add `springdoc-openapi-starter-webmvc-ui` for interactive API docs.
