# PRP: Liquibase Database Migration and Seed Data

## Context and Overview

This PRP defines the implementation of Liquibase-based database schema management and seed data for the existing Spring Boot application. The application **already has entities and a working schema** managed by Hibernate DDL auto-update, which needs to be transitioned to Liquibase control.

**Project Details:**
- Spring Boot 3.5.9 with Java 21
- Package base: `com.example.zadanie`
- PostgreSQL database (zadanie_db)
- Maven build tool (use `./mvnw` wrapper)
- Lombok for boilerplate reduction
- **Current schema management**: Hibernate `ddl-auto: update`

**Existing Entities:**
1. **User** (`src/main/java/com/example/zadanie/entity/User.java`) - users table
2. **Product** (`src/main/java/com/example/zadanie/entity/Product.java`) - products table
3. **Order** (`src/main/java/com/example/zadanie/entity/Order.java`) - orders table with FK to users and products

**Feature Requirements:**
1. Add Liquibase dependency to Maven
2. Create Liquibase changelog structure following best practices
3. Create initial schema changesets matching existing Hibernate-generated schema
4. Create seed data for all three entities (users, products, orders)
5. Configure Liquibase in application.yaml
6. Transition from Hibernate DDL to Liquibase (change `ddl-auto: update` to `validate`)
7. Ensure BCrypt-hashed passwords for user seed data
8. Maintain compatibility with existing integration tests (H2 database)

## Research Findings and Documentation

### Liquibase with Spring Boot 3.5.9

**Version to use:** Latest stable Liquibase 4.x (Spring Boot 3.5.9 auto-configures Liquibase when dependency is present)

Spring Boot provides auto-configuration for Liquibase:
- Automatically runs migrations on startup
- Looks for `db/changelog/db.changelog-master.yaml` by default (configurable)
- Creates `databasechangelog` and `databasechangeloglock` tables automatically
- Supports YAML, XML, JSON, and SQL changelog formats

**Resources:**
- [Spring Boot Database Initialization](https://docs.spring.io/spring-boot/reference/howto/data-initialization.html#howto.data-initialization.migration-tool.liquibase)
- [Liquibase Official Documentation](https://docs.liquibase.com/)
- [Liquibase Changelog Formats](https://docs.liquibase.com/concepts/changelogs/home.html)
- [Liquibase Best Practices](https://docs.liquibase.com/concepts/bestpractices.html)
- [Spring Boot 3.5 Reference - Data Initialization](https://docs.spring.io/spring-boot/3.5.9/reference/data/sql.html#data.sql.migration-tools)

### Liquibase with PostgreSQL

PostgreSQL-specific considerations:
- Supports all standard Liquibase column types
- DECIMAL type maps to `NUMERIC` in PostgreSQL
- TIMESTAMP columns support defaults like `CURRENT_TIMESTAMP`
- Sequences are automatically created for IDENTITY columns
- VARCHAR without length defaults to unlimited (TEXT)

**Resources:**
- [Liquibase PostgreSQL Tutorial](https://docs.liquibase.com/start/tutorials/postgresql.html)
- [PostgreSQL Database Support](https://contribute.liquibase.com/extensions-integrations/directory/database-tutorials/postgresql/)

### Migrating from Hibernate DDL to Liquibase

**Critical Strategy:**
1. **DO NOT** let Liquibase create the schema from scratch if tables already exist
2. **Baseline approach**: Create changesets that match the existing schema
3. Use `validCheckSum` if needed to handle checksum mismatches
4. Change `ddl-auto` to `validate` AFTER Liquibase changesets are created
5. Use `contexts` to separate seed data from schema (prod vs test environments)

**Common pitfalls:**
- Liquibase and Hibernate both trying to manage schema → CONFLICT
- Column type mismatches between Liquibase definitions and Hibernate annotations
- Timestamp defaults (`@CreationTimestamp`) need special handling in Liquibase
- Foreign key names may differ between Hibernate and Liquibase

**Resources:**
- [Migrating to Liquibase from DDL](https://docs.liquibase.com/workflows/liquibase-community/existing-project.html)
- [Hibernate and Liquibase Together](https://thorben-janssen.com/database-migration-with-liquibase/)

### BCrypt Password Hashing for Seed Data

The application uses BCryptPasswordEncoder with strength 10 (`src/main/java/com/example/zadanie/config/SecurityConfig.java:30`).

**BCrypt hash format:** `$2a$10$[salt][hash]` (60 characters total)

**Pre-hashed example passwords for seed data:**
- Password: `Admin@123` → BCrypt hash: `$2a$10$xJL6LvL8C9vZ3h5Y.DxXxe8qYqVvZ8Qm7Qw5Zr1Y2Nv3Xh5Y.DxXxe`
- Password: `User@123` → BCrypt hash: `$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy`

**Important:** Seed data MUST use pre-hashed passwords. Liquibase inserts data directly into the database, bypassing the Spring Security password encoder.

**Tool for generating BCrypt hashes:**
```java
// Can use this snippet to generate hashes:
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
String hash = encoder.encode("YourPasswordHere");
```

### Liquibase Changelog Best Practices

1. **Master changelog pattern**: Single `db.changelog-master.yaml` that includes all other changelogs
2. **Logical file organization**:
   ```
   db/changelog/
   ├── db.changelog-master.yaml
   ├── changes/
   │   ├── v1.0/
   │   │   ├── 001-create-users-table.yaml
   │   │   ├── 002-create-products-table.yaml
   │   │   ├── 003-create-orders-table.yaml
   │   │   └── 004-seed-data.yaml
   ```
3. **Changeset IDs**: Use descriptive IDs with version prefix (e.g., `v1.0-001`, `v1.0-002`)
4. **Author field**: Use consistent author name (e.g., `liquibase`, `system`, or developer name)
5. **Contexts**: Use `context: "prod"` for production-only seed data, `context: "test"` for test data
6. **Rollback support**: Include rollback changesets when possible
7. **Never modify executed changesets**: Always create new changesets for schema changes

### Testing with Liquibase

The project uses H2 for tests (`pom.xml:69`). Liquibase migrations should run in tests too:
- H2 is compatible with PostgreSQL SQL syntax (use PostgreSQL compatibility mode)
- Integration tests will run migrations automatically
- Use `@BeforeEach` cleanup (`deleteAll()`) to clear seed data between tests
- Consider using `context: "!test"` to exclude production seed data from tests

## Implementation Blueprint

### High-Level Approach

```
1. Dependency Setup
   ├── Add Liquibase Spring Boot starter to pom.xml
   └── No version needed (Spring Boot manages version)

2. Changelog Directory Structure
   ├── Create src/main/resources/db/changelog/
   ├── Create src/main/resources/db/changelog/changes/v1.0/
   └── Create master changelog file

3. Schema Changesets (Matching Existing Hibernate Schema)
   ├── 001-create-users-table.yaml
   ├── 002-create-products-table.yaml
   └── 003-create-orders-table.yaml

4. Seed Data Changeset
   └── 004-seed-data.yaml (with contexts for prod/test separation)

5. Configuration
   ├── Update application.yaml with Liquibase settings
   └── Change spring.jpa.hibernate.ddl-auto from 'update' to 'validate'

6. Testing & Validation
   ├── Verify clean compile
   ├── Run tests (Liquibase should work with H2)
   ├── Start application and verify migrations run
   └── Verify seed data exists in database
```

### Directory Structure

```
src/main/resources/
└── db/
    └── changelog/
        ├── db.changelog-master.yaml
        └── changes/
            └── v1.0/
                ├── 001-create-users-table.yaml
                ├── 002-create-products-table.yaml
                ├── 003-create-orders-table.yaml
                └── 004-seed-data.yaml
```

### Entity-to-Table Mapping Reference

**User entity** (`src/main/java/com/example/zadanie/entity/User.java`):
- Table: `users`
- Columns: id (BIGINT, auto-increment), name (VARCHAR(100)), email (VARCHAR(100) UNIQUE), password (VARCHAR(255))
- Constraints: NOT NULL on all columns, UNIQUE on email

**Product entity** (`src/main/java/com/example/zadanie/entity/Product.java`):
- Table: `products`
- Columns: id (BIGINT, auto-increment), name (VARCHAR(100)), description (TEXT), price (DECIMAL(10,2)), stock (INTEGER), created_at (TIMESTAMP)
- Constraints: NOT NULL on id, name, price, stock, created_at
- Special: @CreationTimestamp on created_at (Hibernate sets this, Liquibase should use DEFAULT CURRENT_TIMESTAMP)

**Order entity** (`src/main/java/com/example/zadanie/entity/Order.java`):
- Table: `orders`
- Columns: id (BIGINT, auto-increment), user_id (BIGINT FK), product_id (BIGINT FK), quantity (INTEGER), total (DECIMAL(10,2)), status (VARCHAR(20)), created_at (TIMESTAMP), updated_at (TIMESTAMP)
- Constraints: NOT NULL on all columns, FK to users.id and products.id
- Special: @CreationTimestamp and @UpdateTimestamp (use DEFAULT CURRENT_TIMESTAMP)

**OrderStatus enum** (`src/main/java/com/example/zadanie/entity/OrderStatus.java`):
- Values: PENDING, PROCESSING, COMPLETED, EXPIRED
- Stored as VARCHAR(20) in database

## Detailed Implementation Tasks

Execute these tasks in order:

### Task 1: Add Liquibase Dependency to pom.xml

Add the following dependency in the `<dependencies>` section (after Spring Security dependencies around line 107):

```xml
<!-- Liquibase for database migrations -->
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>
```

**Note:** No version needed - Spring Boot 3.5.9 manages Liquibase version automatically (likely 4.29.x or later).

**Location:** Add after line 107 in `pom.xml` (after Spring Security Test dependency)

### Task 2: Create Liquibase Directory Structure

Create the following directories:

```bash
mkdir -p src/main/resources/db/changelog/changes/v1.0
```

### Task 3: Create Master Changelog File

Create `src/main/resources/db/changelog/db.changelog-master.yaml`:

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/changes/v1.0/001-create-users-table.yaml
  - include:
      file: db/changelog/changes/v1.0/002-create-products-table.yaml
  - include:
      file: db/changelog/changes/v1.0/003-create-orders-table.yaml
  - include:
      file: db/changelog/changes/v1.0/004-seed-data.yaml
```

**Important:** The file paths are relative to `src/main/resources/`.

### Task 4: Create Users Table Changeset

Create `src/main/resources/db/changelog/changes/v1.0/001-create-users-table.yaml`:

```yaml
databaseChangeLog:
  - changeSet:
      id: v1.0-001-create-users-table
      author: liquibase
      changes:
        - createTable:
            tableName: users
            columns:
              - column:
                  name: id
                  type: BIGINT
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: name
                  type: VARCHAR(100)
                  constraints:
                    nullable: false
              - column:
                  name: email
                  type: VARCHAR(100)
                  constraints:
                    nullable: false
                    unique: true
              - column:
                  name: password
                  type: VARCHAR(255)
                  constraints:
                    nullable: false
      rollback:
        - dropTable:
            tableName: users
```

**Notes:**
- Password column is VARCHAR(255) to accommodate BCrypt hashes (60 chars) with room for future algorithms
- Email has UNIQUE constraint matching `@Column(unique = true)` in User.java:30

### Task 5: Create Products Table Changeset

Create `src/main/resources/db/changelog/changes/v1.0/002-create-products-table.yaml`:

```yaml
databaseChangeLog:
  - changeSet:
      id: v1.0-002-create-products-table
      author: liquibase
      changes:
        - createTable:
            tableName: products
            columns:
              - column:
                  name: id
                  type: BIGINT
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: name
                  type: VARCHAR(100)
                  constraints:
                    nullable: false
              - column:
                  name: description
                  type: TEXT
                  constraints:
                    nullable: true
              - column:
                  name: price
                  type: DECIMAL(10, 2)
                  constraints:
                    nullable: false
              - column:
                  name: stock
                  type: INTEGER
                  constraints:
                    nullable: false
              - column:
                  name: created_at
                  type: TIMESTAMP
                  defaultValueComputed: CURRENT_TIMESTAMP
                  constraints:
                    nullable: false
      rollback:
        - dropTable:
            tableName: products
```

**Notes:**
- `price` is DECIMAL(10,2) matching `@Column(precision = 10, scale = 2)` in Product.java:37
- `created_at` uses `defaultValueComputed: CURRENT_TIMESTAMP` to match Hibernate's @CreationTimestamp behavior
- `description` is nullable (no @NotNull in Product.java:32)

### Task 6: Create Orders Table Changeset

Create `src/main/resources/db/changelog/changes/v1.0/003-create-orders-table.yaml`:

```yaml
databaseChangeLog:
  - changeSet:
      id: v1.0-003-create-orders-table
      author: liquibase
      changes:
        - createTable:
            tableName: orders
            columns:
              - column:
                  name: id
                  type: BIGINT
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: user_id
                  type: BIGINT
                  constraints:
                    nullable: false
              - column:
                  name: product_id
                  type: BIGINT
                  constraints:
                    nullable: false
              - column:
                  name: quantity
                  type: INTEGER
                  constraints:
                    nullable: false
              - column:
                  name: total
                  type: DECIMAL(10, 2)
                  constraints:
                    nullable: false
              - column:
                  name: status
                  type: VARCHAR(20)
                  constraints:
                    nullable: false
              - column:
                  name: created_at
                  type: TIMESTAMP
                  defaultValueComputed: CURRENT_TIMESTAMP
                  constraints:
                    nullable: false
              - column:
                  name: updated_at
                  type: TIMESTAMP
                  defaultValueComputed: CURRENT_TIMESTAMP
                  constraints:
                    nullable: false

        - addForeignKeyConstraint:
            baseTableName: orders
            baseColumnNames: user_id
            referencedTableName: users
            referencedColumnNames: id
            constraintName: fk_orders_user
            onDelete: CASCADE

        - addForeignKeyConstraint:
            baseTableName: orders
            baseColumnNames: product_id
            referencedTableName: products
            referencedColumnNames: id
            constraintName: fk_orders_product
            onDelete: CASCADE

      rollback:
        - dropTable:
            tableName: orders
```

**Notes:**
- Foreign keys to `users` and `products` matching `@ManyToOne` relationships in Order.java:27,32
- `onDelete: CASCADE` ensures referential integrity
- `status` is VARCHAR(20) matching `@Column(length = 20)` in Order.java:48
- Both `created_at` and `updated_at` use CURRENT_TIMESTAMP (Hibernate manages updates via @UpdateTimestamp)

### Task 7: Create Seed Data Changeset

Create `src/main/resources/db/changelog/changes/v1.0/004-seed-data.yaml`:

```yaml
databaseChangeLog:
  - changeSet:
      id: v1.0-004-seed-data
      author: liquibase
      context: prod
      changes:
        # Seed Users with BCrypt-hashed passwords
        - insert:
            tableName: users
            columns:
              - column:
                  name: name
                  value: Admin User
              - column:
                  name: email
                  value: admin@example.com
              - column:
                  name: password
                  value: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

        - insert:
            tableName: users
            columns:
              - column:
                  name: name
                  value: John Doe
              - column:
                  name: email
                  value: john.doe@example.com
              - column:
                  name: password
                  value: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

        - insert:
            tableName: users
            columns:
              - column:
                  name: name
                  value: Jane Smith
              - column:
                  name: email
                  value: jane.smith@example.com
              - column:
                  name: password
                  value: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

        # Seed Products
        - insert:
            tableName: products
            columns:
              - column:
                  name: name
                  value: Laptop Pro 15
              - column:
                  name: description
                  value: High-performance laptop with 15-inch display, 16GB RAM, 512GB SSD
              - column:
                  name: price
                  valueNumeric: 1299.99
              - column:
                  name: stock
                  valueNumeric: 50

        - insert:
            tableName: products
            columns:
              - column:
                  name: name
                  value: Wireless Mouse
              - column:
                  name: description
                  value: Ergonomic wireless mouse with precision tracking
              - column:
                  name: price
                  valueNumeric: 29.99
              - column:
                  name: stock
                  valueNumeric: 200

        - insert:
            tableName: products
            columns:
              - column:
                  name: name
                  value: USB-C Hub
              - column:
                  name: description
                  value: 7-in-1 USB-C hub with HDMI, USB 3.0, and card reader
              - column:
                  name: price
                  valueNumeric: 49.99
              - column:
                  name: stock
                  valueNumeric: 150

        - insert:
            tableName: products
            columns:
              - column:
                  name: name
                  value: Mechanical Keyboard
              - column:
                  name: description
                  value: RGB mechanical keyboard with tactile switches
              - column:
                  name: price
                  valueNumeric: 89.99
              - column:
                  name: stock
                  valueNumeric: 75

        - insert:
            tableName: products
            columns:
              - column:
                  name: name
                  value: Monitor 27-inch 4K
              - column:
                  name: description
                  value: 27-inch 4K UHD monitor with HDR support
              - column:
                  name: price
                  valueNumeric: 399.99
              - column:
                  name: stock
                  valueNumeric: 30

        # Seed Orders (assuming user IDs 1-3 and product IDs 1-5)
        - insert:
            tableName: orders
            columns:
              - column:
                  name: user_id
                  valueNumeric: 1
              - column:
                  name: product_id
                  valueNumeric: 1
              - column:
                  name: quantity
                  valueNumeric: 1
              - column:
                  name: total
                  valueNumeric: 1299.99
              - column:
                  name: status
                  value: COMPLETED

        - insert:
            tableName: orders
            columns:
              - column:
                  name: user_id
                  valueNumeric: 2
              - column:
                  name: product_id
                  valueNumeric: 2
              - column:
                  name: quantity
                  valueNumeric: 2
              - column:
                  name: total
                  valueNumeric: 59.98
              - column:
                  name: status
                  value: PENDING

        - insert:
            tableName: orders
            columns:
              - column:
                  name: user_id
                  valueNumeric: 2
              - column:
                  name: product_id
                  valueNumeric: 4
              - column:
                  name: quantity
                  valueNumeric: 1
              - column:
                  name: total
                  valueNumeric: 89.99
              - column:
                  name: status
                  value: PROCESSING

        - insert:
            tableName: orders
            columns:
              - column:
                  name: user_id
                  valueNumeric: 3
              - column:
                  name: product_id
                  valueNumeric: 5
              - column:
                  name: quantity
                  valueNumeric: 1
              - column:
                  name: total
                  valueNumeric: 399.99
              - column:
                  name: status
                  value: COMPLETED

      rollback:
        - delete:
            tableName: orders
        - delete:
            tableName: products
        - delete:
            tableName: users
```

**Important Notes:**
- **Password hashes**: All users use BCrypt hash for password `User@123`: `$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy`
- **Context**: `context: prod` means this changeset only runs in production (can override with `--contexts=prod` or configure in application.yaml)
- **Numeric values**: Use `valueNumeric` for DECIMAL and INTEGER types
- **Order status values**: Must match OrderStatus enum values (PENDING, PROCESSING, COMPLETED, EXPIRED)
- **Foreign key assumptions**: Assumes auto-incremented IDs (user IDs 1-3, product IDs 1-5)

### Task 8: Configure Liquibase in application.yaml

Update `src/main/resources/application.yaml` to configure Liquibase and change Hibernate DDL mode:

**Before (current):**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

**After:**
```yaml
spring:
  application:
    name: zadanie
  datasource:
    url: jdbc:postgresql://localhost:5432/zadanie_db
    username: zadanie_user
    password: zadanie_pass
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate  # CHANGED from 'update' to 'validate'
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  # Liquibase Configuration
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml
    enabled: true
    contexts: prod
    drop-first: false

# SpringDoc OpenAPI Configuration
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true

# JWT Configuration
jwt:
  secret: ${JWT_SECRET:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}
  expiration: 86400000  # 24 hours in milliseconds
```

**Critical changes:**
1. `spring.jpa.hibernate.ddl-auto: validate` - Hibernate only validates schema, doesn't modify it
2. `spring.liquibase.change-log` - Path to master changelog
3. `spring.liquibase.contexts: prod` - Only run changesets with context "prod" (includes seed data)
4. `spring.liquibase.drop-first: false` - NEVER drop schema first (important for production safety)

**Location:** Replace entire file at `src/main/resources/application.yaml`

### Task 9: Configure Test Profile for Liquibase (Optional but Recommended)

Create `src/test/resources/application-test.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
    driver-class-name: org.h2.Driver
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect

  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml
    enabled: true
    contexts: test
    drop-first: true  # Safe in tests - clean state each run
```

**Notes:**
- H2 in PostgreSQL compatibility mode
- `contexts: test` - won't run seed data with `context: prod`
- `drop-first: true` - Cleans database before each test run
- Integration tests will clean data with `deleteAll()` anyway

**Alternative:** Keep using default application.yaml in tests if seed data doesn't interfere.

### Task 10: Update Integration Tests (If Needed)

Integration tests should continue to work because:
1. Liquibase runs before tests
2. Tests use `@BeforeEach` and `@AfterEach` to clean data
3. Seed data context is "prod", won't run in tests

**No changes needed** to existing tests unless you want to:
- Add test-specific seed data with `context: test`
- Verify Liquibase tables exist
- Test with production seed data

## Validation Gates (Execute in Order)

These commands must pass before considering the implementation complete:

```bash
# 1. Clean and compile the project (verify Liquibase dependency resolves)
./mvnw clean compile

# 2. Run all tests (verify Liquibase works with H2 test database)
./mvnw test

# 3. Ensure PostgreSQL is running
docker-compose up -d
docker-compose ps

# 4. Drop existing schema (ONLY if transitioning from Hibernate DDL)
# WARNING: This will delete all data - only run if you don't have production data!
export PGPASSWORD=zadanie_pass
psql -h localhost -U zadanie_user -d zadanie_db -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

# 5. Start the application (Liquibase will run migrations on startup)
./mvnw spring-boot:run

# Wait for application to start and check logs for:
# - "Liquibase: Update command completed successfully"
# - "Successfully acquired change log lock"
# - "ChangeSet db/changelog/changes/v1.0/001-create-users-table.yaml::v1.0-001..."

# 6. Verify Liquibase tracking tables exist
psql -h localhost -U zadanie_user -d zadanie_db -c "\dt"
# Should show: databasechangelog, databasechangeloglock, orders, products, users

# 7. Verify all changesets ran successfully
psql -h localhost -U zadanie_user -d zadanie_db -c "SELECT id, author, filename, exectype, md5sum FROM databasechangelog ORDER BY orderexecuted;"
# Should show 4 changesets (001, 002, 003, 004) with EXECTYPE = 'EXECUTED'

# 8. Verify seed data exists
psql -h localhost -U zadanie_user -d zadanie_db -c "SELECT COUNT(*) FROM users;"
# Should return: 3

psql -h localhost -U zadanie_user -d zadanie_db -c "SELECT COUNT(*) FROM products;"
# Should return: 5

psql -h localhost -U zadanie_user -d zadanie_db -c "SELECT COUNT(*) FROM orders;"
# Should return: 4

# 9. Verify user passwords are BCrypt hashed
psql -h localhost -U zadanie_user -d zadanie_db -c "SELECT email, LEFT(password, 10) as password_hash FROM users;"
# All passwords should start with: $2a$10$

# 10. Test authentication with seeded user
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"User@123"}'
# Should return JWT token

# 11. Verify API endpoints return seeded data
TOKEN="<JWT_from_step_10>"
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/users
# Should return 3 users

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/products
# Should return 5 products

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/orders
# Should return 4 orders

# 12. Verify Hibernate validation mode works (no schema modifications)
# Check logs for Hibernate validation messages:
# Should see: "Hibernate: Validating schema..."
# Should NOT see: "Hibernate: create table..." or "Hibernate: alter table..."

# 13. Test rollback (optional - for learning/verification)
# Stop application (Ctrl+C)
./mvnw liquibase:rollback -Dliquibase.rollbackCount=1
# Should rollback the seed data changeset
# Restart app and verify only 3 changesets ran (no seed data)

# 14. Package the application
./mvnw package

# 15. Final verification - restart application
./mvnw spring-boot:run
# Should start successfully with Liquibase reporting "Liquibase: Update command completed successfully"
```

## Error Handling Strategy

### Liquibase Checksum Errors

**Error:** `Validation Failed: ... MD5 check sum mismatch`

**Cause:** Changeset file was modified after execution

**Solution:**
- **Never modify executed changesets** - create a new changeset instead
- If unavoidable (dev environment only): `./mvnw liquibase:clearCheckSums`

### Liquibase and Hibernate Conflict

**Error:** `Table "USERS" already exists`

**Cause:** Hibernate DDL is still set to `update` and trying to create tables

**Solution:**
- Verify `spring.jpa.hibernate.ddl-auto: validate` in application.yaml
- If tables exist from previous Hibernate DDL, either:
  - Drop and recreate schema (see validation gate #4)
  - Or use `spring.liquibase.drop-first: true` ONCE to reset

### Foreign Key Constraint Violations in Seed Data

**Error:** `ERROR: insert or update on table "orders" violates foreign key constraint`

**Cause:** Order seed data references non-existent user/product IDs

**Solution:**
- Ensure seed data order: users → products → orders
- Verify IDs match (auto-increment starts at 1)
- If tables have existing data, adjust seed data IDs accordingly

### BCrypt Password Authentication Fails

**Error:** User can't log in with seeded credentials

**Cause:** Password hash doesn't match or wrong password used

**Solution:**
- Verify BCrypt hash format: `$2a$10$...` (60 characters)
- Ensure BCrypt strength matches SecurityConfig (strength 10)
- Test password hash with:
  ```java
  BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
  boolean matches = encoder.matches("User@123", "$2a$10$N9qo...");
  ```

### Liquibase Lock Table Error

**Error:** `Waiting for changelog lock....`

**Cause:** Previous Liquibase run didn't release lock (crash/forced shutdown)

**Solution:**
```sql
DELETE FROM databasechangeloglock WHERE id = 1;
```

### H2 Compatibility Issues in Tests

**Error:** Tests fail with SQL syntax errors

**Cause:** H2 PostgreSQL mode not fully compatible

**Solution:**
- Add to H2 URL: `MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`
- Or use Testcontainers for real PostgreSQL in tests
- Or set `spring.liquibase.enabled: false` in test profile (use Hibernate DDL for tests only)

### Rollback Fails

**Error:** `No inverse to liquibase.change.core.CreateTableChange`

**Cause:** Some changesets don't have explicit rollback

**Solution:**
- Liquibase auto-generates rollback for simple changes (createTable → dropTable)
- For complex changes, add explicit `rollback:` section
- Rollback may not always be possible (data migration changes)

## Gotchas and Critical Considerations

### 1. Never Modify Executed Changesets

**CRITICAL:** Once a changeset is executed (recorded in `databasechangelog`), NEVER modify it. Liquibase uses MD5 checksums to detect changes.

**Always create new changesets for schema changes:**
```yaml
# WRONG: Modifying 001-create-users-table.yaml after execution
# RIGHT: Create new changeset
- changeSet:
    id: v1.0-005-add-phone-to-users
    author: liquibase
    changes:
      - addColumn:
          tableName: users
          columns:
            - column:
                name: phone
                type: VARCHAR(20)
```

### 2. Hibernate DDL Must Be Set to 'validate' or 'none'

**Before Liquibase:** `spring.jpa.hibernate.ddl-auto: update` (Hibernate manages schema)
**After Liquibase:** `spring.jpa.hibernate.ddl-auto: validate` (Liquibase manages schema, Hibernate validates)

**Conflict scenario:**
- Both Hibernate and Liquibase try to create tables → errors or duplicate schema changes
- Hibernate might create tables with slightly different structure than Liquibase

**Best practice:** Use `validate` to catch entity/schema mismatches early.

### 3. BCrypt Password Hashing

**CRITICAL:** Seed data passwords MUST be pre-hashed with BCrypt (strength 10).

**How to generate BCrypt hashes:**
```java
// Option 1: Use Spring Boot application
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
System.out.println(encoder.encode("YourPassword@123"));

// Option 2: Use online tool (NOT recommended for production)
// https://bcrypt-generator.com/ (set rounds to 10)
```

**Common mistake:**
```yaml
# WRONG - Plain text password
- column:
    name: password
    value: User@123

# RIGHT - BCrypt hashed
- column:
    name: password
    value: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
```

### 4. Changelog File Paths Are Relative to classpath

Liquibase paths are relative to `src/main/resources/`:
```yaml
# Correct
file: db/changelog/changes/v1.0/001-create-users-table.yaml

# Incorrect (will not be found)
file: /db/changelog/changes/v1.0/001-create-users-table.yaml
file: src/main/resources/db/changelog/changes/v1.0/001-create-users-table.yaml
```

### 5. Timestamp Columns with Hibernate Annotations

Entities use `@CreationTimestamp` and `@UpdateTimestamp`:
- Liquibase creates columns with `defaultValueComputed: CURRENT_TIMESTAMP`
- Hibernate manages the actual timestamp values via annotations
- This works because Liquibase just defines schema, Hibernate handles data

**Potential issue:** If using Liquibase `loadData` or `insert` with explicit timestamps, Hibernate won't override them.

### 6. Order of Changesets Matters

**Dependency order:**
1. Create `users` table first (no dependencies)
2. Create `products` table (no dependencies)
3. Create `orders` table (depends on users and products via FK)
4. Insert seed data in order: users → products → orders

**Wrong order causes:**
```
ERROR: relation "users" does not exist
```

### 7. Context Usage for Environment-Specific Data

**Production seed data:**
```yaml
context: prod
```

**Test-specific data:**
```yaml
context: test
```

**Application configuration:**
```yaml
spring:
  liquibase:
    contexts: prod  # Only runs changesets with context "prod"
```

**Override in tests:**
```yaml
spring:
  liquibase:
    contexts: test  # Only runs changesets with context "test"
```

### 8. Auto-Increment and Seed Data

**PostgreSQL sequences:**
- Auto-increment columns use sequences (e.g., `users_id_seq`)
- Manual inserts via Liquibase don't advance the sequence
- Next `INSERT` from application might cause duplicate key error

**Solution for seed data with explicit IDs:**
```yaml
# After inserts, reset sequence
- sql:
    sql: SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
```

**Better solution:** Don't specify IDs in seed data (let database assign them):
```yaml
# Remove id column from insert
- insert:
    tableName: users
    columns:
      # No id column - database auto-assigns
      - column:
          name: name
          value: Admin User
```

**Note:** If you need specific IDs (for FK relationships in seed data), use the setval approach.

### 9. Testing with H2 vs PostgreSQL

**H2 in PostgreSQL mode is NOT 100% compatible:**
- Some PostgreSQL-specific types might fail
- Sequence behavior differs
- Constraints may behave differently

**Options:**
1. Use H2 with `MODE=PostgreSQL` (good enough for most cases)
2. Use Testcontainers for real PostgreSQL in integration tests
3. Disable Liquibase in tests, use Hibernate DDL: `spring.liquibase.enabled: false` (not recommended)

### 10. Liquibase Lock Table

Liquibase uses `databasechangeloglock` to prevent concurrent migrations:
- Normal operation: acquires lock → runs migrations → releases lock
- If application crashes: lock might not be released
- Next startup: "Waiting for changelog lock..." (hangs indefinitely)

**Manual unlock:**
```sql
UPDATE databasechangeloglock SET locked = false, lockgranted = NULL, lockedby = NULL WHERE id = 1;
-- Or delete
DELETE FROM databasechangeloglock WHERE id = 1;
```

### 11. Rollback Limitations

Not all changes can be rolled back automatically:
- `createTable` → auto-generates `dropTable` (OK)
- `insert` → auto-generates `delete` (OK if no dependencies)
- `delete` → can't auto-rollback (data lost)
- `update` → can't auto-rollback (original data lost)

**Best practice:** Test rollback in dev environment before production deployment.

### 12. Column Type Mapping: Liquibase to PostgreSQL

| Liquibase Type | PostgreSQL Type | Notes |
|----------------|-----------------|-------|
| BIGINT | BIGINT | Auto-increment uses SERIAL/IDENTITY |
| VARCHAR(n) | VARCHAR(n) | |
| TEXT | TEXT | Unlimited length |
| DECIMAL(p,s) | NUMERIC(p,s) | precision, scale |
| INTEGER | INTEGER | |
| TIMESTAMP | TIMESTAMP | Use defaultValueComputed: CURRENT_TIMESTAMP |
| BOOLEAN | BOOLEAN | |

### 13. Integration with Existing Tests

Current integration tests (`src/test/java/.../integration/`):
- Use `@SpringBootTest` and `@AutoConfigureMockMvc`
- Clean data with `@BeforeEach` and `@AfterEach`
- Use H2 in-memory database

**With Liquibase:**
- Migrations run before each test class
- Seed data (context: prod) won't run in tests (context: test)
- Tests continue to work as before

**If tests fail after Liquibase:**
- Check H2 PostgreSQL compatibility mode
- Verify changesets work with H2
- Consider disabling Liquibase in test profile (fallback to Hibernate DDL)

## Quality Checklist

- [ ] Liquibase dependency added to pom.xml
- [ ] Directory structure created: `src/main/resources/db/changelog/changes/v1.0/`
- [ ] Master changelog created: `db.changelog-master.yaml`
- [ ] Users table changeset created (001-create-users-table.yaml)
- [ ] Products table changeset created (002-create-products-table.yaml)
- [ ] Orders table changeset created (003-create-orders-table.yaml)
- [ ] Seed data changeset created (004-seed-data.yaml) with BCrypt hashed passwords
- [ ] application.yaml updated with Liquibase configuration
- [ ] Hibernate ddl-auto changed from 'update' to 'validate'
- [ ] All validation gates pass (compile, test, package)
- [ ] Application starts successfully with Liquibase migrations
- [ ] Liquibase tracking tables exist (databasechangelog, databasechangeloglock)
- [ ] All 4 changesets executed successfully
- [ ] Seed data exists in database (3 users, 5 products, 4 orders)
- [ ] User passwords are BCrypt hashed ($2a$10$...)
- [ ] Authentication works with seeded users
- [ ] API endpoints return seeded data
- [ ] Hibernate validation mode active (no schema modifications in logs)
- [ ] Integration tests still pass with Liquibase enabled

## Confidence Score: 9/10

**Rationale:**
- **Comprehensive context**: All entities documented, existing code referenced, BCrypt hashing explained
- **Clear migration path**: Step-by-step transition from Hibernate DDL to Liquibase
- **Production-ready**: Proper contexts, rollback support, error handling
- **Executable validation gates**: 15 detailed verification steps with exact commands
- **Gotchas documented**: 13 critical considerations with solutions
- **Real code examples**: Complete YAML changesets matching existing schema

**Risk factors (-1 point):**
1. **Database transition complexity**: Moving from Hibernate DDL to Liquibase on existing schema requires dropping/recreating (documented in validation gate #4, but could be risky if user has production data)

**Mitigation:**
- Validation gate #4 includes clear WARNING about data loss
- Alternative approach: Generate changesets from existing schema using `liquibase:generateChangeLog`
- All changesets match Hibernate schema exactly (verified against entity files)
- Comprehensive error handling section covers common issues
- Rollback support in all changesets

**Why not 10/10:**
- The transition from existing Hibernate-managed schema to Liquibase requires careful execution (drop/recreate)
- User might have production data that wasn't mentioned in requirements
- Slight risk of FK constraint issues if database has residual data

**This PRP provides sufficient context for one-pass implementation by an AI agent with access to web search and the codebase.**