# Rental Tracker

A SQLite-backed CLI for listing equipment, tracking rentals, delisting inventory, and confirming returns. The schema and full column constraints are documented in [ERD.md](ERD.md).

## Run

```sh
mvn clean package
mvn exec:java -Dexec.mainClass="tech.kood.rental.Main"
```

The default database is `app.db`. Create Profile signs in immediately; selecting the same username on a later run loads the existing account. Seed at least six listed items in `app.db` before the review demonstration.

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

1. Launch the CLI and create or load a profile.
2. List an item, view public listings, then open inventory and delist an item.
3. Rent an available item by entering a renter username, start date, and number of days.
4. Confirm the return; a normally rented item becomes available, while an item delisted during rental remains unlisted.
5. Run `mvn clean verify` to show the automated tests and coverage report.
