# Rental Tracker ERD

```mermaid
erDiagram
       USERS ||--o{ LISTED_ITEMS : owns
       USERS ||--o{ RENTALS : rents
       LISTED_ITEMS ||--o{ RENTALS : has_history

       USERS {
              INTEGER id PK "AUTOINCREMENT"
              TEXT username UK "NOT NULL"
              DATETIME created_at "DEFAULT CURRENT_TIMESTAMP"
       }

       LISTED_ITEMS {
              INTEGER item_id PK "AUTOINCREMENT"
              INTEGER owner_id FK "NOT NULL REFERENCES users(id)"
              TEXT item_name "NOT NULL"
              TEXT description "NOT NULL"
              REAL cost_per_day "NOT NULL; positive value validated by service"
              TEXT status "NOT NULL; CHECK available, rented, unlisted"
              DATETIME created_at "DEFAULT CURRENT_TIMESTAMP"
       }

       RENTALS {
              INTEGER rental_id PK "AUTOINCREMENT"
              INTEGER item_id FK "NOT NULL REFERENCES listed_items(item_id)"
              INTEGER renter_id FK "NOT NULL REFERENCES users(id)"
              TEXT start_time "NOT NULL"
              TEXT end_time "NOT NULL"
              TEXT returned_at "NULL DEFAULT NULL"
              TEXT status "NOT NULL; CHECK active, closed"
       }
```

The SQLite schema is defined in `src/main/resources/schema.sql`. Foreign keys are enabled on every JDBC connection. Rental and item rows are retained as history; delisting is represented by the `unlisted` item status rather than deletion.
