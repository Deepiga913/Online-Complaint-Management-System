# Online Complaint Management System (OCMS)

A console-based Complaint Management System built in **Java 17**, demonstrating solid OOP design, the **MVC pattern**, custom exception handling, file-based persistence with atomic writes, in-memory caching, multi-threaded logging, and stream/lambda-driven business logic.

Customers can file complaints and track their status; Admins can triage, filter, resolve, and analyze the full complaint queue — all backed by simple CSV files, no database required.

---

## Features

- **Role-based accounts** — Register/login as a `Customer` or `Admin`, with SHA-256 password hashing and in-memory session tokens.
- **Complaint lifecycle** — File, assign, and transition complaints through `NEW → IN_PROGRESS → RESOLVED → CLOSED`, with illegal transitions (e.g. `CLOSED → NEW`) rejected by design.
- **Feedback loop** — Customers can rate and comment on resolved/closed complaints.
- **Dynamic filtering** — Admins can filter the complaint queue by category, priority, status, or date range using Java Streams & Lambdas.
- **Analytics dashboard** — Average resolution time, average satisfaction rating, and breakdowns by category/priority/status.
- **Custom exception hierarchy** — `BaseException` and typed subclasses (`UserNotFoundException`, `ComplaintNotFoundException`, `InvalidComplaintStateException`, `UnauthorizedAccessException`, `DuplicateUserException`) keep error handling explicit and readable.
- **In-memory caching** — Repositories load CSV data into `HashMap`s on startup for O(1) lookups, avoiding repeated disk reads.
- **Atomic file writes** — Every update is written to a temp file and then atomically moved into place (`java.nio.file`), so a crash mid-write can never corrupt the data files.
- **Async activity logging** — A background `ExecutorService` thread writes every significant action to `data/activity.log` without blocking the console UI.
- **Defensive console input** — All user input is validated centrally, so typing a letter into a number field never crashes the app.

---

## Tech Stack

| Concern              | Choice                                             |
|-----------------------|----------------------------------------------------|
| Language               | Java 17+ (Records, Text Blocks, Streams, `var`-free clarity) |
| Paradigm               | OOP, SOLID principles, MVC                        |
| Persistence            | Flat CSV files via `java.nio.file`                |
| Build tool              | Maven                                             |
| Version control         | Git & GitHub                                      |

---

## Project Structure

```
src/main/java/com/cms/
├── OnlineComplaintSystemApp.java     # Entry point — wiring + main menu loop
├── controller/
│   ├── AuthController.java           # Register / Login console flows
│   ├── CustomerController.java       # Customer dashboard
│   └── AdminController.java          # Admin dashboard (tables, filters, analytics)
├── model/
│   ├── User.java                     # Abstract base (encapsulated fields)
│   ├── Customer.java
│   ├── Admin.java
│   ├── Complaint.java                # Owns its own state-transition validation
│   ├── Feedback.java
│   ├── ComplaintStatus.java          # Enum + legal transition graph
│   └── Priority.java                 # Enum
├── repository/
│   ├── UserRepository.java           # HashMap-cached users.csv access
│   └── ComplaintRepository.java      # HashMap-cached complaints.csv access
├── service/
│   ├── AuthService.java              # Registration, login, password hashing, sessions
│   ├── ComplaintService.java         # Business rules, filtering, feedback, analytics
│   └── AsyncLoggerService.java       # Background ExecutorService logger
├── exception/
│   ├── BaseException.java
│   ├── UserNotFoundException.java
│   ├── DuplicateUserException.java
│   ├── ComplaintNotFoundException.java
│   ├── InvalidComplaintStateException.java
│   └── UnauthorizedAccessException.java
└── util/
    ├── FileHandler.java              # CSV read/write, atomic rewrite, escaping
    └── InputValidator.java           # Safe Scanner-based input helpers

data/                                  # Created automatically on first run
├── users.csv
├── complaints.csv
├── feedback.csv
└── activity.log
```

---

## Getting Started

### Prerequisites

- JDK 17 or higher
- Maven 3.6+ (optional — you can also compile with plain `javac`)

### Option 1 — Run with Maven

```bash
mvn compile exec:java
```

Or build a runnable JAR:

```bash
mvn package
java -jar target/ocms.jar
```

### Option 2 — Run with plain `javac`/`java` (no Maven required)

```bash
find src -name "*.java" > sources.txt
javac -d out @sources.txt
java -cp out com.cms.OnlineComplaintSystemApp
```

The app creates a `data/` folder next to wherever you run it from, containing `users.csv`, `complaints.csv`, `feedback.csv`, and `activity.log`. Delete that folder to reset the system to a clean state.

---

## Usage Walkthrough

1. **Register** as a Customer or an Admin (both roles share the same registration flow — just pick your role).
2. **Log in** to reach your dashboard.
3. **As a Customer:**
   - File a complaint (category, description, priority) — you'll get a unique tracking ID like `CMP-20260719011953-001`.
   - View all your filed complaints and their current status.
   - Once a complaint is `RESOLVED` or `CLOSED`, leave a 1–5 star rating and comment.
4. **As an Admin:**
   - View every complaint in a tabular layout.
   - Filter by category, priority, status, or a date range.
   - Update a complaint's status (illegal transitions like `CLOSED → NEW` are rejected).
   - Assign a complaint to yourself.
   - View system-wide analytics: total complaints, average resolution time, average satisfaction, and breakdowns by category/priority/status.

Every significant action (registration, login, filing, status changes, feedback) is recorded asynchronously to `data/activity.log`.

---

## Design Notes

- **MVC separation** — `controller/` only handles console I/O and delegates all business rules to `service/`; `service/` never touches `Scanner`; `repository/` never knows about console formatting.
- **State machine on the model itself** — `Complaint.updateStatus()` validates transitions using `ComplaintStatus.canTransitionTo()`, so illegal state changes are impossible to sneak in from any code path, not just the admin menu.
- **Atomicity** — `FileHandler.writeCsvAtomic()` writes a full temp file and does an atomic `Files.move`, so a rewrite (e.g. editing one complaint) can never leave `complaints.csv` half-written or corrupt neighboring rows.
- **Records** — `ComplaintService.AnalyticsSummary` is a Java `record`, a clean immutable DTO for passing the analytics snapshot from service to controller.

---

## Possible Extensions

- Swap CSV persistence for JDBC + a real database.
- Add JUnit test coverage for `ComplaintStatus` transitions and repository atomic writes.
- Add a REST API layer (Spring Boot) on top of the existing service layer.
- Add password complexity rules and account lockout after repeated failed logins.

---

## License

This project is provided as-is for educational and portfolio purposes.
