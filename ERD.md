       ┌────────────────────────┐
       │         Users          │
       ├──────┬──────────┬──────┤
       │ int  │ id       │  PK  │
       │ text │ username │  UK  │
       └────────────────────────┘
                   │
                   │ owns (1-to-many)
                   ├───┐
                   │   ▼
       ┌───────────┴────────────┐
       │      listed_items      │
       ├──────┬──────────┬──────┤
       │ int  │ item_id  │  PK  │
       │ int  │ owner_id │  FK  │
       │ text │ item_name│      │
       └────────────────────────┘
                   │
                   │ tracks (1-to-many)
                   ├───┐
                   │   ▼
       ┌───────────┴────────────┐
       │        rentals         │
       ├──────┬──────────┬──────┤
       │ int  │ rental_id│  PK  │
       │ int  │ item_id  │  FK  │
       │ int  │ renter_id│  FK  │
       └────────────────────────┘


# 🗃️ Database Schema Definitions

### 👥 Users Table
Stores the primary identity details for individuals participating in the rental ecosystem.

| Data Type | Column Name | Key | Description |
| :--- | :--- | :---: | :--- |
| `int` | **id** | **PK** | System-generated unique identifier for each user profile. |
| `text` | **username** | **UK** | Unique handle chosen by the user; used for terminal authentication. |

---

### 📦 Listed Items Table
Contains properties for asset equipment placed into market circulation by asset owners.

| Data Type | Column Name | Key | Description |
| :--- | :--- | :---: | :--- |
| `int` | **item_id** | **PK** | Unique sequential item catalog tracking number. |
| `int` | **owner_id** | **FK** | Maps back to `Users.id`. Defines which user owns and lists this item. |
| `text` | **item_name** | | Short title/display name of the equipment asset. |
| `text` | **description** | | Helpful context details regarding conditions or accessories included. |
| `real` | **cost_per_day**| | Daily fixed financial pricing rate calculation value. |
| `text` | **status** | | **State Constraint**: Must strictly evaluate as `available`, `rented`, or `unlisted`. |

---

### ⏳ Rentals Table
Tracks the transaction history, current custody timelines, and structural lifecycle metrics of item orders.

| Data Type | Column Name | Key | Description |
| :--- | :--- | :---: | :--- |
| `int` | **rental_id** | **PK** | Auto-incremented registration sequence identification token. |
| `int` | **item_id** | **FK** | Maps to `listed_items.item_id`. Identifies the equipment asset being leased. |
| `int` | **renter_id** | **FK** | Maps to `Users.id`. Tracks the client account signing out the asset. |
| `text` | **start_time** | | Chronological date stamp when the active possession begins. |
| `text` | **end_time** | | Expected expiration deadline date stamp for the contract window. |
| `text` | **returned_at**| | Nullable entry field updated only when physical delivery is marked safe. |
| `text` | **status** | | **State Constraint**: Tracks the current order progress as either `active` or `closed`. |
