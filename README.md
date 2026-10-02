# Rental Tracker

A SQLite-backed CLI for listing equipment, tracking rentals, delisting inventory, and confirming returns. The schema and full column constraints are documented in [ERD.md](ERD.md).

## Run

```sh
mvn clean package
mvn exec:java -Dexec.mainClass="tech.kood.rental.Main"
```

The default database is `app.db`. On the first run, enter a username to create the owner account. Later runs load the first saved account directly into the main menu. Seed at least six listed items in `app.db` before the review demonstration.

The main menu supports listing an item, paging through inventory, recording rentals from the available-item list, confirming returns from the due-date-ordered active-rental list, and exiting. Inventory detail screens show listing information and allow items to be delisted or relisted; an item with an active rental cannot be relisted.

## Test And Coverage

Tests use a fresh temporary SQLite file per test and execute the production `schema.sql`; no database mocks are used. Run the tests and create the JaCoCo report with:

```sh
mvn clean verify
```

The HTML report is generated at `coverage-report/index.html`.

## Architecture

- `Main` initializes the schema and wires the concrete dependencies.
- `transport.CliHandler` handles input/output and calls only `RentalService`.
- `service.RentalService` enforces rental rules, state transitions, and date calculation; it does not issue SQL.
- `repository` classes own SQL, row mapping, and translation of SQLite constraint errors into rule-specific exceptions.
- `infrastructure.DatabaseConnection` creates SQLite connections and enables foreign-key enforcement on each one.

The boundaries are enforced through the dependency direction and constructor injection: the CLI depends on the service API, the service depends on repository APIs, and repositories depend on the database connection. SQLite constraints provide a final integrity boundary beneath service validation.

## Demo Lifecycle

1. Launch the CLI; a first run asks for the owner username, later runs load the saved account.
2. List an item, open inventory, and delist or relist it from its detail view.
3. Choose an available item to rent, then enter the renter username and duration in days. The app records the current date as the start date.
4. Open active rentals, choose a rental, and confirm its return. The item becomes available unless it was delisted while rented.
5. Run `mvn clean verify` to show the automated tests and refreshed coverage report.

## Review Walkthrough

Run this isolated CLI walkthrough from the project root. It uses a new database file so it does not change `app.db`. If `rental-lifecycle-test.db` already exists, choose a different database filename.

```sh
mvn exec:java -Dexec.mainClass=tech.kood.rental.Main -Dexec.args=rental-lifecycle-test.db
```

At the first-run `Username:` prompt, enter `owner-test`, then enter these choices and values as each prompt appears:

```text
Main menu:       1                 (List an item)
Item name:       Rental test item
Description:     Lifecycle test item
Cost per day:    5
Main menu:       3                 (Record a rental)
Available list:  1                 (Select the test item)
Item detail:     1                 (Rent)
Renter username: renter-test
Number of days:  2
Empty rental list: 1               (Back to menu)
Main menu:       4                 (Confirm a return)
Return list:     1                 (Select the active rental)
Rental detail:   1                 (Confirm return)
Empty return list: 1               (Back to menu)
Main menu:       5                 (Exit)
```

The CLI sets the rental start date to the current date; it does not ask for a start date. Check that the rental was retained as closed, has a return timestamp, and that the item is available again:

```sh
sqlite3 rental-lifecycle-test.db "SELECT item_name, status FROM listed_items;"
sqlite3 rental-lifecycle-test.db "SELECT status, returned_at FROM rentals;"
```

Expected results: `Rental test item | available`, then `closed | <non-NULL timestamp>`. To check the account-reload path, run the app again with the same database path; it should show the main menu directly without asking for a username.

The committed demo database should also have at least six listed items:

```sh
sqlite3 app.db "SELECT COUNT(*) FROM listed_items;"
```

Automated tests use separate temporary SQLite files. Run `mvn clean verify` to execute the suite and regenerate `coverage-report/index.html`.

To present the tests, run `mvn clean verify` and open [coverage-report/index.html](coverage-report/index.html). The test suite uses temporary SQLite files, runs repository operations against the production schema, and checks stored rows directly. It covers repository CRUD and constraints, service state transitions and business rules, plus scripted CLI and startup flows.

To explain constraint handling: `RentalService` checks business rules before changing state. Repositories execute SQL and catch `SQLException`; SQLite constraint messages are translated into specific exceptions for unique, foreign-key, not-null, and check violations. The CLI catches these application exceptions at the interaction boundary and displays the message, while the underlying cause remains attached for diagnosis.
