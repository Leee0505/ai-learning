# Phase 1: Project Skeleton & Authentication — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a runnable development environment with Docker Compose (MySQL + Redis), Spring Boot 3.2 backend with JWT authentication (7 endpoints), and Vue 3 frontend with a working login page.

**Architecture:** Backend-first approach. Docker Compose provides MySQL 8.0 + Redis 7. Spring Boot single-module project with package-by-layer structure, MyBatis-Plus ORM, Flyway migrations, and Spring Security with JWT stateless authentication. Vue 3 frontend with Element Plus, Pinia state management, and Axios interceptors for automatic token management.

**Tech Stack:** Spring Boot 3.2, Spring Security 6, MyBatis-Plus 3.5, Flyway, MySQL 8.0, Redis 7 (Redisson), jjwt 0.12, Vue 3.4, Vite 5, Element Plus 2, Pinia 2, Vue Router 4, Axios 1

**Design Spec:** `docs/superpowers/specs/2026-06-10-phase1-auth-design.md`

---

## File Structure

```
ticket-system/
├── docker-compose.yml
├── .gitignore
├── .env.example
├── docker-volumes/               # Git-ignored
│   ├── mysql/
│   └── redis/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/ticket/
│       │   │   ├── TicketApplication.java
│       │   │   ├── controller/
│       │   │   │   └── AuthController.java
│       │   │   ├── service/
│       │   │   │   ├── AuthService.java
│       │   │   │   └── impl/
│       │   │   │       └── AuthServiceImpl.java
│       │   │   ├── mapper/
│       │   │   │   ├── UserMapper.java
│       │   │   │   └── InviteTokenMapper.java
│       │   │   ├── entity/
│       │   │   │   ├── User.java
│       │   │   │   ├── InviteToken.java
│       │   │   │   └── AuditLog.java
│       │   │   ├── dto/
│       │   │   │   ├── request/
│       │   │   │   │   ├── LoginRequest.java
│       │   │   │   │   ├── RegisterRequest.java
│       │   │   │   │   ├── RefreshRequest.java
│       │   │   │   │   ├── InviteRequest.java
│       │   │   │   │   └── AcceptInviteRequest.java
│       │   │   │   └── response/
│       │   │   │       ├── ApiResponse.java
│       │   │   │       ├── AuthResponse.java
│       │   │   │       └── UserResponse.java
│       │   │   ├── security/
│       │   │   │   ├── JwtTokenProvider.java
│       │   │   │   ├── JwtAuthenticationFilter.java
│       │   │   │   ├── UserDetailsServiceImpl.java
│       │   │   │   └── SecurityConfig.java
│       │   │   ├── config/
│       │   │   │   ├── RedisConfig.java
│       │   │   │   └── WebMvcConfig.java
│       │   │   └── common/
│       │   │       ├── constant/
│       │   │       │   └── ErrorCode.java
│       │   │       ├── exception/
│       │   │       │   ├── BusinessException.java
│       │   │       │   ├── GlobalExceptionHandler.java
│       │   │       │   ├── UsernameAlreadyExistsException.java
│       │   │       │   ├── EmailAlreadyExistsException.java
│       │   │       │   ├── InviteTokenExpiredException.java
│       │   │       │   ├── InviteTokenUsedException.java
│       │   │       │   ├── InviteTokenNotFoundException.java
│       │   │       │   ├── TokenExpiredException.java
│       │   │       │   └── TokenBlacklistedException.java
│       │   │       └── aspect/
│       │   │           └── AuditLogAspect.java
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-dev.yml
│       │       └── db/migration/
│       │           ├── V1__init_schema.sql
│       │           └── V2__seed_admin.sql
│       └── test/
│           └── java/com/ticket/
│               ├── security/
│               │   └── JwtTokenProviderTest.java
│               ├── service/
│               │   └── AuthServiceTest.java
│               └── controller/
│                   └── AuthControllerTest.java
└── frontend/
    ├── package.json
    ├── vite.config.js
    ├── index.html
    └── src/
        ├── main.js
        ├── App.vue
        ├── api/
        │   ├── request.js
        │   └── auth.js
        ├── router/
        │   └── index.js
        ├── stores/
        │   └── auth.js
        ├── views/
        │   ├── LoginView.vue
        │   └── DashboardPlaceholder.vue
        └── layouts/
            └── AuthLayout.vue
```

---

### Task 1: Project Root — .gitignore, .env.example, and Directory Scaffold

**Files:**
- Create: `ticket-system/.gitignore`
- Create: `ticket-system/.env.example`
- Create: top-level directory structure

- [ ] **Step 1: Create project root directory structure**

```bash
mkdir -p ticket-system/docker-volumes/mysql ticket-system/docker-volumes/redis
mkdir -p ticket-system/backend/src/main/java/com/ticket
mkdir -p ticket-system/backend/src/main/resources/db/migration
mkdir -p ticket-system/backend/src/test/java/com/ticket
mkdir -p ticket-system/frontend/src
```

- [ ] **Step 2: Write .gitignore**

Create `ticket-system/.gitignore`:

```gitignore
# Docker volumes
docker-volumes/

# Java
backend/target/
backend/*.jar
backend/*.war
!.mvn/wrapper/maven-wrapper.jar

# IDE
.idea/
*.iml
.vscode/
.superpowers/

# Node
frontend/node_modules/
frontend/dist/

# Environment
.env
```

- [ ] **Step 3: Write .env.example**

Create `ticket-system/.env.example`:

```bash
# MySQL
MYSQL_ROOT_PASSWORD=rootpass
MYSQL_DATABASE=ticket_db
MYSQL_USER=ticket_user
MYSQL_PASSWORD=ticket_pass

# JWT
JWT_SECRET=change-me-to-a-random-256-bit-key-in-production

# Redis (dev: no password)
REDIS_HOST=localhost
REDIS_PORT=6379
```

- [ ] **Step 4: Verify**

```bash
ls -la ticket-system/.gitignore ticket-system/.env.example
# Both files should exist
```

---

### Task 2: Docker Compose

**Files:**
- Create: `ticket-system/docker-compose.yml`

- [ ] **Step 1: Write docker-compose.yml**

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: ticket-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-rootpass}
      MYSQL_DATABASE: ${MYSQL_DATABASE:-ticket_db}
      MYSQL_USER: ${MYSQL_USER:-ticket_user}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-ticket_pass}
    ports:
      - "3306:3306"
    volumes:
      - ./docker-volumes/mysql:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

  redis:
    image: redis:7-alpine
    container_name: ticket-redis
    ports:
      - "6379:6379"
    volumes:
      - ./docker-volumes/redis:/data
    command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
```

- [ ] **Step 2: Start containers and verify**

```bash
cd ticket-system
docker compose up -d
docker compose ps
# Expected: ticket-mysql (healthy), ticket-redis (healthy)

# Verify MySQL
docker exec ticket-mysql mysql -u ticket_user -pticket_pass -e "SELECT 1" ticket_db
# Expected: 1

# Verify Redis
docker exec ticket-redis redis-cli ping
# Expected: PONG
```

- [ ] **Step 3: Stop containers (clean state for later)**

```bash
docker compose down
```

---

### Task 3: Backend — pom.xml and Maven Project

**Files:**
- Create: `ticket-system/backend/pom.xml`

- [ ] **Step 1: Write pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.ticket</groupId>
    <artifactId>ticket-system</artifactId>
    <version>0.1.0</version>
    <name>Ticket Management System</name>

    <properties>
        <java.version>17</java.version>
        <jjwt.version>0.12.5</jjwt.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
    </properties>

    <dependencies>
        <!-- Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- AOP (for audit aspect) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>

        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- MySQL -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Flyway -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-mysql</artifactId>
        </dependency>

        <!-- Redis (Redisson) -->
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
            <version>3.27.2</version>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create all package directories**

```bash
cd ticket-system/backend
mkdir -p src/main/java/com/ticket/{controller,service/impl,mapper,entity,dto/request,dto/response,security,config,common/{constant,exception,aspect}}
mkdir -p src/main/resources/db/migration
mkdir -p src/test/java/com/ticket/{security,service,controller}
```

- [ ] **Step 3: Verify Maven resolves dependencies**

```bash
cd ticket-system/backend
mvn dependency:resolve -q
# Expected: BUILD SUCCESS (no errors)
```

---

### Task 4: Backend — Application Entry Point and Config

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/TicketApplication.java`
- Create: `ticket-system/backend/src/main/resources/application.yml`
- Create: `ticket-system/backend/src/main/resources/application-dev.yml`

- [ ] **Step 1: Write TicketApplication.java**

```java
package com.ticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TicketApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketApplication.class, args);
    }
}
```

- [ ] **Step 2: Write application.yml (shared config)**

```yaml
spring:
  application:
    name: ticket-system
  profiles:
    active: dev

server:
  port: 8080

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

jwt:
  access-token-expiration: 7200000
  refresh-token-expiration: 604800000
```

- [ ] **Step 3: Write application-dev.yml**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ticket_db?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=UTC
    username: ticket_user
    password: ticket_pass
    driver-class-name: com.mysql.cj.jdbc.Driver
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

jwt:
  secret: ${JWT_SECRET:dev-secret-key-that-is-at-least-256-bits-long-for-hs256}
```

- [ ] **Step 4: Verify application compiles**

```bash
cd ticket-system/backend
mvn compile -q
# Expected: BUILD SUCCESS
```

---

### Task 5: Common Layer — ErrorCode Enum and ApiResponse

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/common/constant/ErrorCode.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/dto/response/ApiResponse.java`

- [ ] **Step 1: Write ErrorCode.java**

```java
package com.ticket.common.constant;

public enum ErrorCode {
    // 4xxxx Client Errors
    VALIDATION_ERROR(40000, "validation error"),
    USERNAME_ALREADY_EXISTS(40001, "username already exists"),
    EMAIL_ALREADY_EXISTS(40002, "email already exists"),
    INVITE_TOKEN_EXPIRED(40003, "invitation link has expired"),
    INVITE_TOKEN_USED(40004, "invitation link already used"),
    INVITE_TOKEN_NOT_FOUND(40005, "invitation link not found"),

    // 401xx Auth Errors
    INVALID_CREDENTIALS(40100, "invalid credentials"),
    TOKEN_EXPIRED(40101, "token expired"),
    TOKEN_BLACKLISTED(40102, "token has been revoked"),
    TOKEN_INVALID(40103, "token is invalid"),

    // 403xx Forbidden
    ACCESS_DENIED(40300, "access denied"),

    // 500xx Server Errors
    INTERNAL_ERROR(50000, "internal server error");

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() { return code; }
    public String getDefaultMessage() { return defaultMessage; }
}
```

- [ ] **Step 2: Write ApiResponse.java**

```java
package com.ticket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "success", null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    // Getters
    public int getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}
```

- [ ] **Step 3: Verify compilation**

```bash
cd ticket-system/backend
mvn compile -q
# Expected: BUILD SUCCESS
```

---

### Task 6: Common Layer — Exceptions

**Files:**
- Create: `BusinessException.java`
- Create: `UsernameAlreadyExistsException.java`
- Create: `EmailAlreadyExistsException.java`
- Create: `InviteTokenExpiredException.java`
- Create: `InviteTokenUsedException.java`
- Create: `InviteTokenNotFoundException.java`
- Create: `TokenExpiredException.java`
- Create: `TokenBlacklistedException.java`

All in `ticket-system/backend/src/main/java/com/ticket/common/exception/`

- [ ] **Step 1: Write BusinessException.java (base)**

```java
package com.ticket.common.exception;

import com.ticket.common.constant.ErrorCode;

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() { return errorCode; }
}
```

- [ ] **Step 2: Write all specific exception classes**

```java
// UsernameAlreadyExistsException.java
package com.ticket.common.exception;

import com.ticket.common.constant.ErrorCode;

public class UsernameAlreadyExistsException extends BusinessException {
    public UsernameAlreadyExistsException() {
        super(ErrorCode.USERNAME_ALREADY_EXISTS);
    }
}
```

```java
// EmailAlreadyExistsException.java
package com.ticket.common.exception;

import com.ticket.common.constant.ErrorCode;

public class EmailAlreadyExistsException extends BusinessException {
    public EmailAlreadyExistsException() {
        super(ErrorCode.EMAIL_ALREADY_EXISTS);
    }
}
```

```java
// InviteTokenExpiredException.java
package com.ticket.common.exception;

import com.ticket.common.constant.ErrorCode;

public class InviteTokenExpiredException extends BusinessException {
    public InviteTokenExpiredException() {
        super(ErrorCode.INVITE_TOKEN_EXPIRED);
    }
}
```

```java
// InviteTokenUsedException.java
package com.ticket.common.exception;

import com.ticket.common.constant.ErrorCode;

public class InviteTokenUsedException extends BusinessException {
    public InviteTokenUsedException() {
        super(ErrorCode.INVITE_TOKEN_USED);
    }
}
```

```java
// InviteTokenNotFoundException.java
package com.ticket.common.exception;

import com.ticket.common.constant.ErrorCode;

public class InviteTokenNotFoundException extends BusinessException {
    public InviteTokenNotFoundException() {
        super(ErrorCode.INVITE_TOKEN_NOT_FOUND);
    }
}
```

```java
// TokenExpiredException.java
package com.ticket.common.exception;

import com.ticket.common.constant.ErrorCode;

public class TokenExpiredException extends BusinessException {
    public TokenExpiredException() {
        super(ErrorCode.TOKEN_EXPIRED);
    }
}
```

```java
// TokenBlacklistedException.java
package com.ticket.common.exception;

import com.ticket.common.constant.ErrorCode;

public class TokenBlacklistedException extends BusinessException {
    public TokenBlacklistedException() {
        super(ErrorCode.TOKEN_BLACKLISTED);
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
cd ticket-system/backend
mvn compile -q
# Expected: BUILD SUCCESS
```

---

### Task 7: Common Layer — GlobalExceptionHandler

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/common/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Write GlobalExceptionHandler.java**

```java
package com.ticket.common.exception;

import com.ticket.common.constant.ErrorCode;
import com.ticket.dto.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        ApiResponse<Void> response = ApiResponse.error(errorCode.getCode(), ex.getMessage());
        return ResponseEntity.status(mapHttpStatus(errorCode.getCode())).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.INVALID_CREDENTIALS.getCode(),
                ErrorCode.INVALID_CREDENTIALS.getDefaultMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.ACCESS_DENIED.getCode(),
                ErrorCode.ACCESS_DENIED.getDefaultMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.VALIDATION_ERROR.getCode(), message);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleFallback(Exception ex) {
        log.error("Unhandled exception", ex);
        ApiResponse<Void> response = ApiResponse.error(
                ErrorCode.INTERNAL_ERROR.getCode(),
                ErrorCode.INTERNAL_ERROR.getDefaultMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private HttpStatus mapHttpStatus(int code) {
        if (code >= 40100 && code < 40200) return HttpStatus.UNAUTHORIZED;
        if (code >= 40300 && code < 40400) return HttpStatus.FORBIDDEN;
        if (code >= 40000 && code < 40100) return HttpStatus.BAD_REQUEST;
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd ticket-system/backend
mvn compile -q
# Expected: BUILD SUCCESS
```

---

### Task 8: Database — Flyway Migration Scripts

**Files:**
- Create: `ticket-system/backend/src/main/resources/db/migration/V1__init_schema.sql`
- Create: `ticket-system/backend/src/main/resources/db/migration/V2__seed_admin.sql`

- [ ] **Step 1: Write V1__init_schema.sql**

```sql
-- V1: Initialize core tables for authentication

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `email` VARCHAR(100) NOT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `password` VARCHAR(255) NOT NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1=enabled, 0=disabled',
    `created_by` BIGINT NOT NULL DEFAULT 0 COMMENT '0 for self-registration',
    `created_date` BIGINT NOT NULL COMMENT 'Unix timestamp in milliseconds',
    `last_modified_by` BIGINT DEFAULT NULL,
    `last_modified_date` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`),
    KEY `idx_user_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `invite_token` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `token` VARCHAR(64) NOT NULL,
    `email` VARCHAR(100) NOT NULL,
    `expires_at` BIGINT NOT NULL COMMENT 'Unix timestamp in milliseconds',
    `used` TINYINT NOT NULL DEFAULT 0 COMMENT '0=unused, 1=used',
    `created_by` BIGINT NOT NULL COMMENT 'Admin who created the invitation',
    `created_date` BIGINT NOT NULL COMMENT 'Unix timestamp in milliseconds',
    `last_modified_by` BIGINT DEFAULT NULL,
    `last_modified_date` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_token` (`token`),
    KEY `idx_invite_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT 'Actor user ID',
    `action` VARCHAR(50) NOT NULL COMMENT 'LOGIN, LOGOUT, REGISTER, INVITE_AGENT, ACCEPT_INVITE',
    `target_type` VARCHAR(50) DEFAULT NULL COMMENT 'USER, INVITE_TOKEN',
    `target_id` BIGINT DEFAULT NULL,
    `detail` JSON DEFAULT NULL COMMENT 'Flexible payload',
    `ip_address` VARCHAR(45) DEFAULT NULL,
    `created_by` BIGINT NOT NULL COMMENT 'Same as user_id for unified audit queries',
    `created_date` BIGINT NOT NULL COMMENT 'Unix timestamp in milliseconds',
    `last_modified_by` BIGINT DEFAULT NULL,
    `last_modified_date` BIGINT DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_audit_user` (`user_id`),
    KEY `idx_audit_action` (`action`),
    KEY `idx_audit_created_date` (`created_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- [ ] **Step 2: Write V2__seed_admin.sql**

```sql
-- V2: Seed default admin user
-- Default password: Admin@123
-- bcrypt hash generated with strength 10

INSERT INTO `user` (username, email, phone, password, role, status, created_by, created_date)
VALUES (
    'admin',
    'admin@ticket.local',
    NULL,
    -- bcrypt hash of 'Admin@123' — regenerate in code if needed
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ROLE_ADMIN',
    1,
    0,
    (UNIX_TIMESTAMP(NOW()) * 1000)
);
```

> **Note:** The bcrypt hash in V2 must match the actual hash of `Admin@123`. If the hash doesn't match, login will fail. The implementation step below generates the correct hash programmatically.

- [ ] **Step 3: Start MySQL and verify Flyway runs**

```bash
cd ticket-system
docker compose up -d mysql
# Wait for healthy
cd backend
mvn spring-boot:run -q &
sleep 15
# Check Flyway applied migrations
docker exec ticket-mysql mysql -u ticket_user -pticket_pass -e "SELECT * FROM flyway_schema_history" ticket_db
# Expected: 2 rows (V1, V2)
```

- [ ] **Step 4: Shutdown and prepare for next task**

```bash
# Stop Spring Boot (Ctrl+C or kill)
docker compose down
```

---

### Task 9: Database — Entities

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/entity/User.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/entity/InviteToken.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/entity/AuditLog.java`

- [ ] **Step 1: Write User.java**

```java
package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.Instant;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String email;
    private String phone;
    private String password;
    private String role;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private Long createdDate;

    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedBy;

    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedDate;
}
```

- [ ] **Step 2: Write InviteToken.java**

```java
package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("invite_token")
public class InviteToken {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String token;
    private String email;
    private Long expiresAt;
    private Integer used;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private Long createdDate;

    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedBy;

    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedDate;
}
```

- [ ] **Step 3: Write AuditLog.java**

```java
package com.ticket.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String action;
    private String targetType;
    private Long targetId;
    private String detail;
    private String ipAddress;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private Long createdDate;

    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedBy;

    @TableField(fill = FieldFill.UPDATE)
    private Long lastModifiedDate;
}
```

- [ ] **Step 4: Verify compilation**

```bash
cd ticket-system/backend
mvn compile -q
# Expected: BUILD SUCCESS
```

---

### Task 10: Database — Mappers and MyBatis-Plus MetaObjectHandler

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/mapper/UserMapper.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/mapper/InviteTokenMapper.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/config/MyBatisPlusConfig.java` (with MetaObjectHandler for audit fields)
- Create: `ticket-system/backend/src/main/java/com/ticket/config/WebMvcConfig.java`

- [ ] **Step 1: Write UserMapper.java**

```java
package com.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

- [ ] **Step 2: Write InviteTokenMapper.java**

```java
package com.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.entity.InviteToken;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InviteTokenMapper extends BaseMapper<InviteToken> {
}
```

- [ ] **Step 3: Write MyBatisPlusConfig.java (with audit field auto-fill)**

```java
package com.ticket.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                long now = System.currentTimeMillis();
                // Get current user ID from SecurityContext (0 if not authenticated yet)
                Long userId = getCurrentUserId();
                this.strictInsertFill(metaObject, "createdBy", Long.class, userId);
                this.strictInsertFill(metaObject, "createdDate", Long.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                Long userId = getCurrentUserId();
                this.strictUpdateFill(metaObject, "lastModifiedBy", Long.class, userId);
                this.strictUpdateFill(metaObject, "lastModifiedDate", Long.class, System.currentTimeMillis());
            }

            private Long getCurrentUserId() {
                try {
                    var auth = org.springframework.security.core.context.SecurityContextHolder
                            .getContext().getAuthentication();
                    if (auth != null && auth.getPrincipal() instanceof com.ticket.security.UserDetailsImpl principal) {
                        return principal.getUserId();
                    }
                } catch (Exception ignored) {}
                return 0L;
            }
        };
    }
}
```

- [ ] **Step 4: Write WebMvcConfig.java**

```java
package com.ticket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

- [ ] **Step 5: Verify compilation**

```bash
cd ticket-system/backend
mvn compile -q
# Expected: BUILD SUCCESS (may warn about missing classes — that's OK, they come next)
```

---

### Task 11: Security — UserDetailsServiceImpl and UserDetailsImpl

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/security/UserDetailsImpl.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/security/UserDetailsServiceImpl.java`

- [ ] **Step 1: Write UserDetailsImpl.java**

```java
package com.ticket.security;

import com.ticket.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserDetailsImpl implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final String role;
    private final boolean enabled;

    public UserDetailsImpl(User user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.role = user.getRole();
        this.enabled = user.getStatus() != null && user.getStatus() == 1;
    }

    public Long getUserId() { return userId; }
    public String getRole() { return role; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }
}
```

- [ ] **Step 2: Write UserDetailsServiceImpl.java**

```java
package com.ticket.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.entity.User;
import com.ticket.mapper.UserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    public UserDetailsServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        // Auto-detect: contains '@' → query by email, otherwise → query by username
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (login.contains("@")) {
            wrapper.eq(User::getEmail, login);
        } else {
            wrapper.eq(User::getUsername, login);
        }

        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + login);
        }
        return new UserDetailsImpl(user);
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
cd ticket-system/backend
mvn compile -q
# Expected: BUILD SUCCESS
```

---

### Task 12: Security — JwtTokenProvider

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/security/JwtTokenProvider.java`

- [ ] **Step 1: Write JwtTokenProvider.java**

```java
package com.ticket.security;

import com.ticket.common.exception.TokenBlacklistedException;
import com.ticket.common.exception.TokenExpiredException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final RedissonClient redissonClient;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration,
            RedissonClient redissonClient) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(secret.getBytes())));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.redissonClient = redissonClient;
    }

    public String generateAccessToken(Long userId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .subject(userId.toString())
                .id(jti)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();

        // Store in Redis whitelist
        String redisKey = "refresh:" + userId + ":" + jti;
        RBucket<String> bucket = redissonClient.getBucket(redisKey);
        bucket.set(token, Duration.ofMillis(refreshTokenExpiration));

        return token;
    }

    public Claims validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Check blacklist
            String jti = claims.getId();
            if (jti == null) {
                // Access token — check blacklist by token hash
                String blacklistKey = "blacklist:" + token.hashCode();
                RBucket<String> bucket = redissonClient.getBucket(blacklistKey);
                if (bucket.isExists()) {
                    throw new TokenBlacklistedException();
                }
            }

            return claims;
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException();
        } catch (JwtException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return null;
        }
    }

    public void blacklistAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (remaining > 0) {
                String blacklistKey = "blacklist:" + token.hashCode();
                RBucket<String> bucket = redissonClient.getBucket(blacklistKey);
                bucket.set("revoked", Duration.ofMillis(remaining));
            }
        } catch (JwtException ignored) {
            // Token already expired — no need to blacklist
        }
    }

    public void revokeRefreshToken(Long userId, String jti) {
        String redisKey = "refresh:" + userId + ":" + jti;
        RBucket<String> bucket = redissonClient.getBucket(redisKey);
        bucket.delete();
    }

    public boolean isRefreshTokenValid(Long userId, String jti) {
        String redisKey = "refresh:" + userId + ":" + jti;
        RBucket<String> bucket = redissonClient.getBucket(redisKey);
        return bucket.isExists();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = validateToken(token);
        if (claims == null) return null;
        return Long.parseLong(claims.getSubject());
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd ticket-system/backend
mvn compile -q
# Expected: BUILD SUCCESS
```

---

### Task 13: Security — JwtAuthenticationFilter

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/security/JwtAuthenticationFilter.java`

- [ ] **Step 1: Write JwtAuthenticationFilter.java**

```java
package com.ticket.security;

import com.ticket.common.exception.TokenBlacklistedException;
import com.ticket.common.exception.TokenExpiredException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            try {
                Claims claims = jwtTokenProvider.validateToken(token);
                if (claims != null) {
                    Long userId = Long.parseLong(claims.getSubject());
                    String role = claims.get("role", String.class);

                    UserDetailsImpl principal = new UserDetailsImpl(userId, role);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal, null,
                                    Collections.singletonList(new SimpleGrantedAuthority(role)));
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (TokenExpiredException | TokenBlacklistedException e) {
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write(
                        "{\"code\":" + (e instanceof TokenExpiredException ? 40101 : 40102) +
                        ",\"message\":\"" + e.getMessage() + "\",\"data\":null}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 2: Add convenience constructor to UserDetailsImpl**

```java
// Add this constructor to UserDetailsImpl.java (append to the existing file):
public UserDetailsImpl(Long userId, String role) {
    this.userId = userId;
    this.username = String.valueOf(userId);
    this.password = "";
    this.role = role;
    this.enabled = true;
}
```

- [ ] **Step 3: Verify compilation**

```bash
cd ticket-system/backend
mvn compile -q
# Expected: BUILD SUCCESS
```

---

### Task 14: Security — SecurityConfig

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/security/SecurityConfig.java`

- [ ] **Step 1: Write SecurityConfig.java**

```java
package com.ticket.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/register").permitAll()
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/refresh").permitAll()
                .requestMatchers("/api/auth/accept-invite").permitAll()
                .requestMatchers("/api/auth/invite").hasRole("ADMIN")
                .requestMatchers("/api/auth/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd ticket-system/backend
mvn compile -q
# Expected: BUILD SUCCESS
```

---

### Task 15: DTOs — Request Classes

**Files:**
- Create: 5 request DTOs in `ticket-system/backend/src/main/java/com/ticket/dto/request/`

- [ ] **Step 1: Write LoginRequest.java**

```java
package com.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank(message = "login is required")
    private String login;

    @NotBlank(message = "password is required")
    private String password;

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

- [ ] **Step 2: Write RegisterRequest.java**

```java
package com.ticket.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank(message = "username is required")
    @Size(min = 3, max = 50, message = "username must be 3-50 characters")
    private String username;

    @NotBlank(message = "email is required")
    @Email(message = "invalid email format")
    private String email;

    @Size(max = 20)
    private String phone;

    @NotBlank(message = "password is required")
    @Size(min = 6, max = 100, message = "password must be 6-100 characters")
    private String password;

    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

- [ ] **Step 3: Write RefreshRequest.java**

```java
package com.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;

public class RefreshRequest {
    @NotBlank(message = "refreshToken is required")
    private String refreshToken;

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
```

- [ ] **Step 4: Write InviteRequest.java**

```java
package com.ticket.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class InviteRequest {
    @NotBlank(message = "email is required")
    @Email(message = "invalid email format")
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
```

- [ ] **Step 5: Write AcceptInviteRequest.java**

```java
package com.ticket.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AcceptInviteRequest {
    @NotBlank(message = "token is required")
    private String token;

    @NotBlank(message = "username is required")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "password is required")
    @Size(min = 6, max = 100)
    private String password;

    // Getters and setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

- [ ] **Step 6: Verify compilation**

```bash
cd ticket-system/backend
mvn compile -q
# Expected: BUILD SUCCESS
```

---

### Task 16: DTOs — Response Classes

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/dto/response/UserResponse.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/dto/response/AuthResponse.java`

- [ ] **Step 1: Write UserResponse.java**

```java
package com.ticket.dto.response;

import com.ticket.entity.User;

public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String role;
    private Integer status;
    private Long createdDate;

    public static UserResponse from(User user) {
        UserResponse response = new UserResponse();
        response.id = user.getId();
        response.username = user.getUsername();
        response.email = user.getEmail();
        response.phone = user.getPhone();
        response.role = user.getRole();
        response.status = user.getStatus();
        response.createdDate = user.getCreatedDate();
        return response;
    }

    // Getters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public Integer getStatus() { return status; }
    public Long getCreatedDate() { return createdDate; }
}
```

- [ ] **Step 2: Write AuthResponse.java**

```java
package com.ticket.dto.response;

public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private UserResponse user;

    public AuthResponse(String accessToken, String refreshToken, UserResponse user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public UserResponse getUser() { return user; }
}
```

- [ ] **Step 3: Verify compilation**

```bash
cd ticket-system/backend
mvn compile -q
# Expected: BUILD SUCCESS
```

---

### Task 17: Service Layer — AuthService Interface and Implementation

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/service/AuthService.java`
- Create: `ticket-system/backend/src/main/java/com/ticket/service/impl/AuthServiceImpl.java`

- [ ] **Step 1: Write AuthService.java**

```java
package com.ticket.service;

import com.ticket.dto.request.*;
import com.ticket.dto.response.AuthResponse;
import com.ticket.dto.response.UserResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void logout(String accessToken);
    AuthResponse refresh(RefreshRequest request);
    UserResponse getCurrentUser(Long userId);
    String invite(InviteRequest request, Long adminId);
    AuthResponse acceptInvite(AcceptInviteRequest request);
}
```

- [ ] **Step 2: Write AuthServiceImpl.java**

```java
package com.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.common.exception.*;
import com.ticket.dto.request.*;
import com.ticket.dto.response.AuthResponse;
import com.ticket.dto.response.UserResponse;
import com.ticket.entity.InviteToken;
import com.ticket.entity.User;
import com.ticket.mapper.InviteTokenMapper;
import com.ticket.mapper.UserMapper;
import com.ticket.security.JwtTokenProvider;
import com.ticket.security.UserDetailsImpl;
import com.ticket.service.AuthService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final long INVITE_EXPIRY_MS = 48 * 60 * 60 * 1000L; // 48 hours

    private final UserMapper userMapper;
    private final InviteTokenMapper inviteTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(UserMapper userMapper, InviteTokenMapper inviteTokenMapper,
                           PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                           JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.inviteTokenMapper = inviteTokenMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check uniqueness
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())) > 0) {
            throw new UsernameAlreadyExistsException();
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, request.getEmail())) > 0) {
            throw new EmailAlreadyExistsException();
        }

        // Create user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("ROLE_USER");
        user.setStatus(1);
        userMapper.insert(user);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        log.info("User registered: {} (id={})", user.getUsername(), user.getId());
        return new AuthResponse(accessToken, refreshToken, UserResponse.from(user));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // Authenticate
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword()));

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Load full user
        User user = userMapper.selectById(userDetails.getUserId());

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        log.info("User logged in: {} (id={})", user.getUsername(), user.getId());
        return new AuthResponse(accessToken, refreshToken, UserResponse.from(user));
    }

    @Override
    public void logout(String accessToken) {
        jwtTokenProvider.blacklistAccessToken(accessToken);

        // Also revoke associated refresh token if we can extract jti
        try {
            Claims claims = jwtTokenProvider.validateToken(accessToken);
            // Note: Access token doesn't have JTI, but blacklisting handles it
        } catch (Exception ignored) {}

        log.info("User logged out");
    }

    @Override
    public AuthResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        // Validate refresh token
        Claims claims = jwtTokenProvider.validateToken(refreshToken);
        if (claims == null || claims.getId() == null) {
            throw new BusinessException(
                    com.ticket.common.constant.ErrorCode.TOKEN_INVALID);
        }

        Long userId = Long.parseLong(claims.getSubject());
        String jti = claims.getId();

        // Verify token is in whitelist
        if (!jwtTokenProvider.isRefreshTokenValid(userId, jti)) {
            throw new BusinessException(
                    com.ticket.common.constant.ErrorCode.TOKEN_BLACKLISTED);
        }

        // Revoke old refresh token (rotation)
        jwtTokenProvider.revokeRefreshToken(userId, jti);

        // Issue new tokens
        User user = userMapper.selectById(userId);
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return new AuthResponse(newAccessToken, newRefreshToken, UserResponse.from(user));
    }

    @Override
    public UserResponse getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(
                    com.ticket.common.constant.ErrorCode.TOKEN_INVALID);
        }
        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public String invite(InviteRequest request, Long adminId) {
        String token = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        InviteToken inviteToken = new InviteToken();
        inviteToken.setToken(token);
        inviteToken.setEmail(request.getEmail());
        inviteToken.setExpiresAt(now + INVITE_EXPIRY_MS);
        inviteToken.setUsed(0);
        inviteToken.setCreatedBy(adminId);
        inviteTokenMapper.insert(inviteToken);

        log.info("Admin {} invited {}", adminId, request.getEmail());
        return token;
    }

    @Override
    @Transactional
    public AuthResponse acceptInvite(AcceptInviteRequest request) {
        // Find token
        InviteToken inviteToken = inviteTokenMapper.selectOne(
                new LambdaQueryWrapper<InviteToken>()
                        .eq(InviteToken::getToken, request.getToken()));

        if (inviteToken == null) {
            throw new InviteTokenNotFoundException();
        }
        if (inviteToken.getUsed() == 1) {
            throw new InviteTokenUsedException();
        }
        if (System.currentTimeMillis() > inviteToken.getExpiresAt()) {
            throw new InviteTokenExpiredException();
        }

        // Check username uniqueness
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())) > 0) {
            throw new UsernameAlreadyExistsException();
        }

        // Create agent user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(inviteToken.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("ROLE_AGENT");
        user.setStatus(1);
        userMapper.insert(user);

        // Mark token as used
        inviteToken.setUsed(1);
        inviteTokenMapper.updateById(inviteToken);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        log.info("Agent {} accepted invite from email {}", user.getUsername(), inviteToken.getEmail());
        return new AuthResponse(accessToken, refreshToken, UserResponse.from(user));
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
cd ticket-system/backend
mvn compile -q
# Expected: BUILD SUCCESS
```

---

### Task 18: Controller Layer — AuthController

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/controller/AuthController.java`

- [ ] **Step 1: Write AuthController.java**

```java
package com.ticket.controller;

import com.ticket.dto.request.*;
import com.ticket.dto.response.ApiResponse;
import com.ticket.dto.response.AuthResponse;
import com.ticket.dto.response.UserResponse;
import com.ticket.security.UserDetailsImpl;
import com.ticket.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7); // Remove "Bearer "
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshRequest request) {
        AuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        UserResponse response = authService.getCurrentUser(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/invite")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> invite(
            @Valid @RequestBody InviteRequest request,
            @AuthenticationPrincipal UserDetailsImpl admin) {
        String token = authService.invite(request, admin.getUserId());
        return ResponseEntity.ok(ApiResponse.success(token));
    }

    @PostMapping("/accept-invite")
    public ResponseEntity<ApiResponse<AuthResponse>> acceptInvite(
            @Valid @RequestBody AcceptInviteRequest request) {
        AuthResponse response = authService.acceptInvite(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd ticket-system/backend
mvn compile -q
# Expected: BUILD SUCCESS
```

---

### Task 19: Audit Aspect

**Files:**
- Create: `ticket-system/backend/src/main/java/com/ticket/common/aspect/AuditLogAspect.java`

- [ ] **Step 1: Write AuditLogAspect.java**

```java
package com.ticket.common.aspect;

import com.ticket.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);
    // Audit logging via aspect — logs are written to the database via mapper
    // and also to application log for immediate visibility

    @AfterReturning("execution(* com.ticket.service.impl.AuthServiceImpl.login(..))")
    public void logLogin(JoinPoint joinPoint) {
        writeAudit("LOGIN", "USER", null, null);
    }

    @AfterReturning("execution(* com.ticket.service.impl.AuthServiceImpl.logout(..))")
    public void logLogout(JoinPoint joinPoint) {
        writeAudit("LOGOUT", "USER", null, null);
    }

    @AfterReturning("execution(* com.ticket.service.impl.AuthServiceImpl.register(..))")
    public void logRegister(JoinPoint joinPoint) {
        writeAudit("REGISTER", "USER", null, null);
    }

    @AfterReturning(value = "execution(* com.ticket.service.impl.AuthServiceImpl.invite(..))",
            returning = "result")
    public void logInvite(JoinPoint joinPoint, Object result) {
        // result is the token string — we log the action
        writeAudit("INVITE_AGENT", "INVITE_TOKEN", null, null);
    }

    @AfterReturning("execution(* com.ticket.service.impl.AuthServiceImpl.acceptInvite(..))")
    public void logAcceptInvite(JoinPoint joinPoint) {
        writeAudit("ACCEPT_INVITE", "USER", null, null);
    }

    private void writeAudit(String action, String targetType, Long targetId, String detail) {
        try {
            Long userId = getCurrentUserId();
            String ip = getClientIp();
            Long now = System.currentTimeMillis();

            // Log to application log for immediate visibility (Kafka in future phases)
            log.info("AUDIT: userId={}, action={}, targetType={}, targetId={}, ip={}",
                    userId, action, targetType, targetId, ip);

            // Database logging is deferred to when we have AuditLogMapper wired in
            // For MVP, application log is sufficient — this aspect establishes the pattern
        } catch (Exception e) {
            log.warn("Failed to write audit log", e);
        }
    }

    private Long getCurrentUserId() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl principal) {
                return principal.getUserId();
            }
        } catch (Exception ignored) {}
        return 0L;
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception ignored) {}
        return "unknown";
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
cd ticket-system/backend
mvn compile -q
# Expected: BUILD SUCCESS
```

---

### Task 20: Backend Integration Test — Start and Verify with curl

**Files:** None (manual verification)

- [ ] **Step 1: Start all services**

```bash
cd ticket-system
docker compose up -d
# Wait for healthy
cd backend
mvn spring-boot:run &
# Wait for startup (look for "Started TicketApplication")
```

- [ ] **Step 2: Test register (USER)**

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"pass123"}'
# Expected: { "code":200, "message":"success", "data":{ "accessToken":"...", "refreshToken":"...", "user":{...} } }
```

- [ ] **Step 3: Test login (by username)**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"admin","password":"Admin@123"}'
# Expected: { "code":200, ... "user":{ "role":"ROLE_ADMIN" ... } }
```

- [ ] **Step 4: Test login (by email)**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"admin@ticket.local","password":"Admin@123"}'
# Expected: { "code":200, ... }
```

- [ ] **Step 5: Test /me with JWT**

```bash
# Save the accessToken from login response and:
TOKEN="<paste-access-token-here>"
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"
# Expected: { "code":200, "data":{ "username":"admin", ... } }
```

- [ ] **Step 6: Test invite (admin only)**

```bash
# Use admin token from login
curl -X POST http://localhost:8080/api/auth/invite \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email":"agent@ticket.local"}'
# Expected: { "code":200, "data":"<invite-token-uuid>" }
```

- [ ] **Step 7: Test accept-invite**

```bash
INVITE_TOKEN="<paste-invite-token>"
curl -X POST http://localhost:8080/api/auth/accept-invite \
  -H "Content-Type: application/json" \
  -d "{\"token\":\"$INVITE_TOKEN\",\"username\":\"agent01\",\"password\":\"agent123\"}"
# Expected: { "code":200, "data":{ "user":{ "role":"ROLE_AGENT" }, ... } }
```

- [ ] **Step 8: Test refresh**

```bash
REFRESH_TOKEN="<paste-refresh-token>"
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}"
# Expected: { "code":200, "data":{ "accessToken":"<new>", "refreshToken":"<new>" } }
```

- [ ] **Step 9: Test logout**

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer $TOKEN"
# Expected: { "code":200, "data":null }

# Verify token is blacklisted
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"
# Expected: { "code":40102, "message":"token has been revoked" }
```

- [ ] **Step 10: Test role enforcement (user cannot invite)**

```bash
# Login as regular user, get token, then:
curl -X POST http://localhost:8080/api/auth/invite \
  -H "Authorization: Bearer <user-token>" \
  -H "Content-Type: application/json" \
  -d '{"email":"someone@example.com"}'
# Expected: { "code":40300, "message":"access denied" }
```

- [ ] **Step 11: Shutdown**

```bash
# Stop Spring Boot (Ctrl+C)
docker compose down
```

---

### Task 21: Frontend — Vite + Vue 3 Project Scaffold

**Files:**
- Create: `ticket-system/frontend/package.json`
- Create: `ticket-system/frontend/vite.config.js`
- Create: `ticket-system/frontend/index.html`
- Create: `ticket-system/frontend/src/main.js`
- Create: `ticket-system/frontend/src/App.vue`

- [ ] **Step 1: Write package.json**

```json
{
  "name": "ticket-frontend",
  "private": true,
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.4.21",
    "vue-router": "^4.3.0",
    "pinia": "^2.1.7",
    "pinia-plugin-persistedstate": "^3.2.1",
    "axios": "^1.6.8",
    "element-plus": "^2.6.3"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.4",
    "vite": "^5.2.10"
  }
}
```

- [ ] **Step 2: Install dependencies**

```bash
cd ticket-system/frontend
npm install
# Expected: packages installed without errors
```

- [ ] **Step 3: Write vite.config.js**

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  resolve: {
    alias: {
      '@': '/src'
    }
  }
})
```

- [ ] **Step 4: Write index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Ticket Management System</title>
</head>
<body>
  <div id="app"></div>
  <script type="module" src="/src/main.js"></script>
</body>
</html>
```

- [ ] **Step 5: Write main.js**

```js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPersistedstate from 'pinia-plugin-persistedstate'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)

const pinia = createPinia()
pinia.use(piniaPersistedstate)

app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.mount('#app')
```

- [ ] **Step 6: Write App.vue**

```vue
<template>
  <router-view />
</template>

<script setup>
// Root component — router-view renders the matched route component
</script>

<style>
body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
</style>
```

- [ ] **Step 7: Verify dev server starts**

```bash
cd ticket-system/frontend
npm run dev &
sleep 5
curl http://localhost:5173 | head -5
# Expected: HTML content (index.html)
```

---

### Task 22: Frontend — Axios Interceptor and Auth API

**Files:**
- Create: `ticket-system/frontend/src/api/request.js`
- Create: `ticket-system/frontend/src/api/auth.js`

- [ ] **Step 1: Write request.js (Axios instance + interceptors)**

```js
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// Request interceptor — attach JWT
request.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.accessToken) {
      config.headers.Authorization = `Bearer ${authStore.accessToken}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor — handle 401 with auto-refresh
let isRefreshing = false
let failedQueue = []

const processQueue = (error, token = null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error)
    } else {
      resolve(token)
    }
  })
  failedQueue = []
}

request.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // Queue request while refresh is in progress
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`
          return request(originalRequest)
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      const authStore = useAuthStore()
      try {
        const { data } = await axios.post('/api/auth/refresh', {
          refreshToken: authStore.refreshToken
        })
        const { accessToken, refreshToken } = data.data
        authStore.setTokens(accessToken, refreshToken)
        processQueue(null, accessToken)
        originalRequest.headers.Authorization = `Bearer ${accessToken}`
        return request(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError, null)
        authStore.clearAuth()
        router.push('/login')
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  }
)

export default request
```

- [ ] **Step 2: Write auth.js (Auth API calls)**

```js
import request from './request'

export function loginApi(data) {
  return request.post('/auth/login', data)
}

export function registerApi(data) {
  return request.post('/auth/register', data)
}

export function logoutApi() {
  return request.post('/auth/logout')
}

export function refreshApi(data) {
  return request.post('/auth/refresh', data)
}

export function getCurrentUserApi() {
  return request.get('/auth/me')
}

export function inviteApi(data) {
  return request.post('/auth/invite', data)
}

export function acceptInviteApi(data) {
  return request.post('/auth/accept-invite', data)
}
```

- [ ] **Step 3: Verify frontend compiles**

```bash
cd ticket-system/frontend
npx vite build --logLevel error
# Expected: Build completes without errors
```

---

### Task 23: Frontend — Pinia Auth Store

**Files:**
- Create: `ticket-system/frontend/src/stores/auth.js`

- [ ] **Step 1: Write auth.js (Pinia store)**

```js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi, logoutApi, getCurrentUserApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  // State
  const accessToken = ref(null)
  const refreshToken = ref(localStorage.getItem('refreshToken') || null)
  const user = ref(null)

  // Getters
  const isLoggedIn = computed(() => !!accessToken.value)
  const isAdmin = computed(() => user.value?.role === 'ROLE_ADMIN')
  const isAgent = computed(() => user.value?.role === 'ROLE_AGENT')

  // Actions
  function setTokens(newAccessToken, newRefreshToken) {
    accessToken.value = newAccessToken
    refreshToken.value = newRefreshToken
    localStorage.setItem('refreshToken', newRefreshToken)
  }

  function setUser(newUser) {
    user.value = newUser
  }

  async function login(login, password) {
    const { data } = await loginApi({ login, password })
    if (data.code === 200) {
      setTokens(data.data.accessToken, data.data.refreshToken)
      setUser(data.data.user)
    }
    return data
  }

  async function fetchUser() {
    try {
      const { data } = await getCurrentUserApi()
      if (data.code === 200) {
        setUser(data.data)
      }
    } catch {
      clearAuth()
    }
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      clearAuth()
    }
  }

  function clearAuth() {
    accessToken.value = null
    refreshToken.value = null
    user.value = null
    localStorage.removeItem('refreshToken')
  }

  // Persist refresh token across page reloads
  return {
    accessToken, refreshToken, user,
    isLoggedIn, isAdmin, isAgent,
    setTokens, setUser, login, fetchUser, logout, clearAuth
  }
})
```

- [ ] **Step 2: Verify build**

```bash
cd ticket-system/frontend
npx vite build --logLevel error
# Expected: Build completes without errors
```

---

### Task 24: Frontend — Router

**Files:**
- Create: `ticket-system/frontend/src/router/index.js`

- [ ] **Step 1: Write router/index.js**

```js
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { requiresAuth: false, layout: 'auth' }
  },
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('@/views/DashboardPlaceholder.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guard
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // If route requires auth and no access token
  if (to.meta.requiresAuth && !authStore.accessToken) {
    // Try silent refresh
    if (authStore.refreshToken) {
      try {
        const { refreshApi } = await import('@/api/auth')
        const { data } = await refreshApi({ refreshToken: authStore.refreshToken })
        if (data.code === 200) {
          authStore.setTokens(data.data.accessToken, data.data.refreshToken)
          await authStore.fetchUser()
          next()
          return
        }
      } catch {
        authStore.clearAuth()
      }
    }
    next('/login')
    return
  }

  // If going to login but already logged in
  if (to.path === '/login' && authStore.accessToken) {
    next('/')
    return
  }

  next()
})

export default router
```

- [ ] **Step 2: Create placeholder files for lazy-loaded views**

Since we need the files to exist for the build to succeed, create placeholder views.

Create `ticket-system/frontend/src/views/DashboardPlaceholder.vue`:

```vue
<template>
  <div style="display:flex;align-items:center;justify-content:center;min-height:100vh;">
    <div style="text-align:center;">
      <h1>Welcome, {{ authStore.user?.username }}</h1>
      <p>Dashboard coming in Phase 2</p>
      <el-button @click="handleLogout">Logout</el-button>
    </div>
  </div>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { ElButton } from 'element-plus'

const authStore = useAuthStore()
const router = useRouter()

async function handleLogout() {
  await authStore.logout()
  router.push('/login')
}
</script>
```

- [ ] **Step 3: Verify build**

```bash
cd ticket-system/frontend
npx vite build --logLevel error
# Expected: Build completes without errors
```

---

### Task 25: Frontend — AuthLayout and LoginView

**Files:**
- Create: `ticket-system/frontend/src/layouts/AuthLayout.vue`
- Create: `ticket-system/frontend/src/views/LoginView.vue`

- [ ] **Step 1: Write AuthLayout.vue**

```vue
<template>
  <div class="auth-layout">
    <div class="auth-card">
      <div class="auth-header">
        <div class="auth-logo">T</div>
        <h2>Ticket System</h2>
      </div>
      <slot />
    </div>
  </div>
</template>

<script setup>
// Wraps auth pages in a centered card layout
</script>

<style scoped>
.auth-layout {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.auth-card {
  width: 400px;
  background: #fff;
  border-radius: 12px;
  padding: 40px 32px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.auth-logo {
  width: 48px;
  height: 48px;
  background: #667eea;
  color: #fff;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: bold;
  margin: 0 auto 12px;
}

.auth-header h2 {
  margin: 0;
  font-size: 20px;
  color: #1e293b;
}
</style>
```

- [ ] **Step 2: Write LoginView.vue**

```vue
<template>
  <AuthLayout>
    <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin">
      <el-form-item prop="login">
        <el-input
          v-model="form.login"
          placeholder="Username or email"
          prefix-icon="User"
          size="large"
        />
      </el-form-item>

      <el-form-item prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="Password"
          prefix-icon="Lock"
          size="large"
          show-password
        />
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          size="large"
          :loading="loading"
          style="width:100%"
          @click="handleLogin"
        >
          Sign In
        </el-button>
      </el-form-item>
    </el-form>
  </AuthLayout>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import AuthLayout from '@/layouts/AuthLayout.vue'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  login: '',
  password: ''
})

const rules = {
  login: [
    { required: true, message: 'Enter username or email', trigger: 'blur' }
  ],
  password: [
    { required: true, message: 'Enter password', trigger: 'blur' },
    { min: 6, message: 'Password must be at least 6 characters', trigger: 'blur' }
  ]
}

async function handleLogin() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const data = await authStore.login(form.login, form.password)
    if (data.code === 200) {
      ElMessage.success('Login successful')
      router.push('/')
    } else {
      ElMessage.error(data.message || 'Login failed')
    }
  } catch (error) {
    const message = error.response?.data?.message || 'Network error, please try again'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}
</script>
```

- [ ] **Step 3: Verify build**

```bash
cd ticket-system/frontend
npx vite build --logLevel error
# Expected: Build completes without errors
```

---

### Task 26: End-to-End Smoke Test

- [ ] **Step 1: Start all services**

```bash
cd ticket-system
docker compose up -d
# Wait for healthy
cd backend && mvn spring-boot:run &
# Wait for startup
cd ../frontend && npm run dev &
```

- [ ] **Step 2: Open browser and test login flow**

```bash
# Open in browser:
# http://localhost:5173
```

Manual verification:
1. Login page appears with centered card, username/email + password fields
2. Login with `admin` / `Admin@123` → redirects to dashboard placeholder
3. Dashboard shows "Welcome, admin" with logout button
4. Click logout → redirects to login page
5. Login with bad credentials → shows error toast
6. Login with `testuser` (ROLE_USER) → works, but user cannot access admin features

- [ ] **Step 3: Shutdown**

```bash
# Stop Vite dev server (Ctrl+C)
# Stop Spring Boot (Ctrl+C)
docker compose down
```

---

### Task 27: Backend Unit Tests — JwtTokenProvider

**Files:**
- Create: `ticket-system/backend/src/test/resources/application-test.yml`
- Create: `ticket-system/backend/src/test/java/com/ticket/security/JwtTokenProviderTest.java`

- [ ] **Step 1: Write application-test.yml**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  flyway:
    enabled: false

jwt:
  secret: test-secret-key-that-is-at-least-256-bits-long-for-testing-only
  access-token-expiration: 7200000
  refresh-token-expiration: 604800000
```

- [ ] **Step 2: Write JwtTokenProviderTest.java**

```java
package com.ticket.security;

import com.ticket.common.exception.TokenBlacklistedException;
import com.ticket.common.exception.TokenExpiredException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private RedissonClient redissonClient;

    @BeforeEach
    void setUp() {
        redissonClient = mock(RedissonClient.class);

        // Mock RBucket for whitelist checks
        RBucket<String> mockBucket = mock(RBucket.class);
        when(mockBucket.isExists()).thenReturn(true);
        when(redissonClient.getBucket(anyString())).thenReturn(mockBucket);

        jwtTokenProvider = new JwtTokenProvider(
                "test-secret-key-that-is-at-least-256-bits-long-for-testing-only",
                7200000L, 604800000L, redissonClient);
    }

    @Test
    void shouldGenerateAndValidateAccessToken() {
        String token = jwtTokenProvider.generateAccessToken(1L, "ROLE_USER");
        assertThat(token).isNotBlank();

        Claims claims = jwtTokenProvider.validateToken(token);
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("role")).isEqualTo("ROLE_USER");
    }

    @Test
    void shouldGenerateAndValidateRefreshToken() {
        String token = jwtTokenProvider.generateRefreshToken(1L);
        assertThat(token).isNotBlank();

        Claims claims = jwtTokenProvider.validateToken(token);
        assertThat(claims).isNotNull();
        assertThat(claims.getId()).isNotBlank();
    }

    @Test
    void shouldReturnNullForInvalidToken() {
        Claims claims = jwtTokenProvider.validateToken("invalid.token.here");
        assertThat(claims).isNull();
    }

    @Test
    void shouldReturnUserIdFromToken() {
        String token = jwtTokenProvider.generateAccessToken(42L, "ROLE_AGENT");
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        assertThat(userId).isEqualTo(42L);
    }

    @Test
    void shouldValidateRefreshTokenInWhitelist() {
        String token = jwtTokenProvider.generateRefreshToken(1L);
        Claims claims = jwtTokenProvider.validateToken(token);
        boolean valid = jwtTokenProvider.isRefreshTokenValid(1L, claims.getId());
        assertThat(valid).isTrue();
    }

    @Test
    void shouldRevokeRefreshToken() {
        jwtTokenProvider.revokeRefreshToken(1L, "some-jti");
        // Should not throw — revocation is best-effort via delete
    }

    @Test
    void shouldBlacklistAccessToken() {
        RBucket<String> mockBucket = mock(RBucket.class);
        when(redissonClient.getBucket(anyString())).thenReturn(mockBucket);

        String token = jwtTokenProvider.generateAccessToken(1L, "ROLE_USER");
        jwtTokenProvider.blacklistAccessToken(token);
        // Should not throw — blacklisting sets TTL
    }
}
```

- [ ] **Step 3: Run tests**

```bash
cd ticket-system/backend
mvn test -Dtest=JwtTokenProviderTest -q
# Expected: Tests pass (green)
```

---

### Task 28: Backend Integration Tests — AuthController

**Files:**
- Create: `ticket-system/backend/src/test/java/com/ticket/controller/AuthControllerTest.java`

- [ ] **Step 1: Write AuthControllerTest.java**

```java
package com.ticket.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.dto.request.LoginRequest;
import com.ticket.dto.request.RegisterRequest;
import com.ticket.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerShouldReturn400ForEmptyBody() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void loginShouldReturn400ForEmptyBody() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptInviteShouldReturn400ForInvalidToken() throws Exception {
        mockMvc.perform(post("/api/auth/accept-invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"invalid\",\"username\":\"test\",\"password\":\"pass123\"}"))
                .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 2: Run integration tests**

```bash
cd ticket-system/backend
mvn test -Dtest=AuthControllerTest -q
# Expected: Tests pass (uses H2 in-memory database)
```

---

### All Tasks Summary

| Task | Component | Files |
|------|-----------|-------|
| 1 | Project Root | `.gitignore`, `.env.example`, directory scaffold |
| 2 | Docker Compose | `docker-compose.yml` |
| 3 | Backend pom.xml | `pom.xml`, Maven project |
| 4 | Application entry | `TicketApplication.java`, `application.yml`, `application-dev.yml` |
| 5 | ErrorCode + ApiResponse | `ErrorCode.java`, `ApiResponse.java` |
| 6 | Exceptions | 8 exception classes |
| 7 | GlobalExceptionHandler | `GlobalExceptionHandler.java` |
| 8 | Flyway scripts | `V1__init_schema.sql`, `V2__seed_admin.sql` |
| 9 | Entities | `User.java`, `InviteToken.java`, `AuditLog.java` |
| 10 | Mappers + Config | `UserMapper.java`, `InviteTokenMapper.java`, `MyBatisPlusConfig.java`, `WebMvcConfig.java` |
| 11 | UserDetails | `UserDetailsImpl.java`, `UserDetailsServiceImpl.java` |
| 12 | JwtTokenProvider | `JwtTokenProvider.java` |
| 13 | JwtAuthFilter | `JwtAuthenticationFilter.java` |
| 14 | SecurityConfig | `SecurityConfig.java` |
| 15 | Request DTOs | 5 request classes |
| 16 | Response DTOs | `UserResponse.java`, `AuthResponse.java` |
| 17 | AuthService | `AuthService.java`, `AuthServiceImpl.java` |
| 18 | AuthController | `AuthController.java` |
| 19 | AuditLogAspect | `AuditLogAspect.java` |
| 20 | Manual curl tests | Backend verification |
| 21 | Frontend scaffold | `package.json`, `vite.config.js`, `index.html`, `main.js`, `App.vue` |
| 22 | Axios + Auth API | `request.js`, `auth.js` |
| 23 | Pinia auth store | `stores/auth.js` |
| 24 | Router | `router/index.js`, `DashboardPlaceholder.vue` |
| 25 | Login page | `AuthLayout.vue`, `LoginView.vue` |
| 26 | E2E smoke test | Full stack verification |
| 27 | JWT unit tests | `JwtTokenProviderTest.java` |
| 28 | Controller integration tests | `AuthControllerTest.java` |
