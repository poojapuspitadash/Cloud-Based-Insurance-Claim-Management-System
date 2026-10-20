-- Reference MySQL schema (Hibernate creates these automatically with ddl-auto=update).
CREATE DATABASE IF NOT EXISTS claimsdb CHARACTER SET utf8mb4;
USE claimsdb;

CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255),
  email VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,          -- BCrypt hash, never plain text
  policy_number VARCHAR(255),
  role ENUM('CUSTOMER','EMPLOYEE')
);

CREATE TABLE claims (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  claim_number VARCHAR(255) UNIQUE,
  customer_id BIGINT NOT NULL,
  type VARCHAR(255),
  description VARCHAR(2000),
  amount DECIMAL(19,2),
  incident_date DATE,
  status ENUM('SUBMITTED','UNDER_REVIEW','APPROVED','REJECTED'),
  priority ENUM('LOW','MEDIUM','HIGH'),
  risk_score INT,
  reviewer_note VARCHAR(255),
  created_at DATETIME(6),
  updated_at DATETIME(6),
  FOREIGN KEY (customer_id) REFERENCES users(id)
);

CREATE TABLE claim_documents (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  claim_id BIGINT NOT NULL,
  file_name VARCHAR(255),
  content_type VARCHAR(255),
  storage_key VARCHAR(255),                -- S3 object key
  uploaded_at DATETIME(6),
  FOREIGN KEY (claim_id) REFERENCES claims(id)
);
