# EdgeStore & Local Persistence Architecture

**Scope:** Edge-Sync-Mobile (Local Store only)  
**Audience:** Mobile Engineers, Platform Engineers  
**Last Updated:** Jan 2, 2026

## 1. Purpose
This document records the architectural design for how local data persistence is handled in Edge-Sync-Mobile.

The goal is to:

- Enforce a single data access boundary
- Prevent accidental coupling to ObjectBox
- Preserve debuggability and developer velocity
- Enable future sync/P2P without rework

## 2. High-Level Principle (Non-Negotiable)
Application developers must never call ObjectBox APIs directly. All CRUD operations must go through EdgeStore.

This rule is enforced by architecture, not convention.

## 3. System Architecture Overview
```
App Code
   ↓
EdgeStoreInitializer (creates & caches EdgeStore per local store)
   ↓
EdgeStore  (CRUD + policy)
   ↓
EdgeBox    (ObjectBox wrapper)
   ↓
ObjectBox  (Local storage engine)
```
**Key Intent**

- EdgeStore owns behavior and policy
- ObjectBox is an internal storage implementation
- App code never depends on ObjectBox

## 4. Responsibilities by Layer
### 4.1 App (Consumer)
The app:

- Defines all data models
- Owns ObjectBox entity annotations
- Initializes local stores exclusively through `EdgeStoreInitializer`
- Calls `EdgeStore` for all CRUD operations

The app must never:

- Call BoxStore
- Call Box
- Call ObjectBox query APIs
- Manage persistence rules
- Instantiate EdgeStore directly or bypass `EdgeStoreInitializer`

### 4.2 EdgeStoreInitializer
The initializer lives in the SDK and owns ObjectBox bootstrapping so the app never imports
ObjectBox types.

**EdgeStoreInitializer responsibilities:**

- Accept an Android `Context` and use `applicationContext`
- Create one `EdgeStore` per `storeName` and reuse it on subsequent lookups
- Initialize the ObjectBox `BoxStore` in `<app files dir>/objectbox/<storeName>`
- Provide lifecycle hooks to close a single store or all stores
- Remain thread-safe while serving concurrent callers

### 4.3 Models
Models reside in the app, not in EdgeStore.

Models are ObjectBox entities and must include:

- `@Id long id` → ObjectBox local ID
- `String _id` → Backend / master DB identity

**Model Rules**

- `_id` is the only business identity
- ObjectBox `id` is local only
- No ObjectBox relations (`ToOne`, `ToMany`) are used
- Foreign keys are stored explicitly using `_id` strings

### 4.4 EdgeStore (Library API)
EdgeStore is the only CRUD interface exposed to app developers.

**EdgeStore responsibilities:**

- Create / Read / Update / Delete
- Enforce presence and validity of `_id`
- Centralize all writes
- Track mutations (dirty state)
- Act as the future sync integration point
- Provide a single debugging choke point

**EdgeStore must:**

- Be model-agnostic
- Not define application entities
- Not expose ObjectBox types
- Not leak storage details

### 4.5 EdgeBox (ObjectBox Adapter)
EdgeBox is a thin internal wrapper over ObjectBox.

EdgeBox:

- Wraps `BoxStore`
- Performs low-level persistence
- Is not visible to app code
- Contains no business logic

ObjectBox is treated strictly as an implementation detail.

## 5. ObjectBox Usage Policy
**Allowed**

- ObjectBox entities defined in the app
- ObjectBox annotation processing
- ObjectBox local indexing
- ObjectBox storage optimizations

**Forbidden**

- Direct ObjectBox access from app code
- ObjectBox relations
- ObjectBox APIs outside EdgeStore
- Using ObjectBox IDs as business identifiers

## 6. Identity Rules
| Field | Meaning | Scope |
| --- | --- | --- |
| `@Id long id` | Local storage identifier | ObjectBox only |
| `String _id` | Master DB / backend identifier | System-wide |

**Rules:**

- `_id` must always be present
- Sync and P2P use `_id` only
- ObjectBox `id` never leaves EdgeStore / EdgeBox

## 7. Querying & Joins
- No ORM relations are used
- All joins are manual and explicit
- Joins are performed inside EdgeStore
- Foreign keys use `_id` strings
- Indexed lookups are mandatory

This approach is chosen to maximize:

- Debuggability
- Predictability
- Sync safety
- Future flexibility

## 8. Debuggability Principles
This architecture intentionally favors debuggability over abstraction purity.

**Key decisions:**

- Explicit foreign keys instead of relations
- Centralized mutation tracking
- Clear ownership of responsibilities
- No hidden ORM behavior

A system that is easy to debug is more valuable than one that is theoretically perfect.

## 9. Future-Facing Considerations
This agreement intentionally does not implement:

- Sync logic
- P2P logic
- CRDTs
- Event sourcing
- ObjectBox Sync

However:

- EdgeStore is the single integration point for all future sync work
- No app-level changes should be required when sync is introduced

## 10. Enforcement Rules
The architecture is considered broken if any of the following occur:

- App code imports ObjectBox classes
- App code calls `BoxStore` or `Box`
- CRUD bypasses EdgeStore
- ObjectBox relations are introduced
- ObjectBox IDs are used as business identifiers

## 11. Final Agreement Statement
EdgeStore is the only way to access local data. ObjectBox is an internal detail. Models belong to the app. Control belongs to EdgeStore.
