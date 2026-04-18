# StepServe — Local Service Marketplace

StepServe connects homeowners with verified service providers in Canada.  
Direct-deploy FastAPI + MySQL backend, React 18 + Vite frontend.

## Stack

| Layer | Technology |
|---|---|
| Backend | FastAPI (Python 3.12) + PyMySQL (no ORM) |
| Database | MySQL 8 |
| Auth | JWT HS256, 24hr expiry, stored in `localStorage` |
| Frontend | React 18 + Vite 5 (SPA, inline styles) |
| Config | `config.ini` + `pydantic-settings` |
| Deploy | PythonAnywhere (wsgi via `a2wsgi`) |

---

## iOS app

A native iOS app (SwiftUI) is located in the `ios/` directory.
It mirrors the Android app exactly — same screens, same API calls, same brand colours.

### Prerequisites

| Tool | Version |
|---|---|
| Xcode | 15 or newer |
| iOS Deployment Target | iOS 16+ |
| macOS | Ventura 13+ |

### Running on the iOS Simulator

```bash
# 1. Open the project in Xcode:
open ios/StepServe.xcodeproj

# 2. Select an iPhone simulator (e.g. iPhone 15) in the device picker
# 3. Press Cmd+R (or the Run button)
```

The debug build points to `http://localhost:8000/api/v1/`.  
Start the FastAPI backend first: `uvicorn app.main:app --reload --port 8000`

### Running on a physical iPhone

1. Connect your iPhone via USB
2. Open `ios/StepServe.xcodeproj` in Xcode
3. In the Signing & Capabilities tab, set your Apple Developer Team
4. Select your device in the device picker
5. Press Cmd+R

For a physical device you need to update the API base URL in [`ios/StepServe/Data/NetworkClient.swift`](ios/StepServe/Data/NetworkClient.swift) to your machine's local IP:
```swift
let API_BASE_URL = "http://192.168.x.x:8000/api/v1"
```

### Changing the API base URL

Edit the constant at the top of `NetworkClient.swift`:
```swift
let API_BASE_URL = "http://localhost:8000/api/v1"          // Simulator
let API_BASE_URL = "http://192.168.x.x:8000/api/v1"        // Physical device (same Wi-Fi)
let API_BASE_URL = "https://youruser.pythonanywhere.com/api/v1"  // Production
```

---

## Running locally

```bash
# 1. Install Python dependencies
pip install -r requirements.txt

# 2. Configure database
# Edit config.ini → [mysql] section with your host/user/password/database

# 3. Create tables + seed categories
PYTHONPATH=. python scripts/init_db.py

# 4. Start backend (port 8000)
uvicorn app.main:app --reload --port 8000

# 5. Start frontend (port 5174)
cd ui && npm install && npm run dev -- --port 5174
```

Frontend talks to `VITE_API_BASE_URL` (default `http://127.0.0.1:8000/api/v1`).

---

## Android app

A native Android app (Kotlin + Jetpack Compose) is located in the `android/` directory.
It covers every API endpoint and mirrors the web UI.

### Prerequisites

| Tool | Version |
|---|---|
| Android Studio | Hedgehog (2023.1.1) or newer |
| JDK | 17 or 21 |
| Android SDK | API 35 (install via SDK Manager) |
| Emulator | Pixel 6 API 33+ recommended |

### First-time setup

```bash
# 1. Open the android/ folder as a project in Android Studio
#    (File → Open → select the android/ directory)

# 2. Let Gradle sync finish (it downloads all dependencies automatically)

# 3. Create android/local.properties if it doesn't exist:
echo "sdk.dir=$HOME/Library/Android/sdk" > android/local.properties
# On Linux: echo "sdk.dir=$HOME/Android/Sdk" > android/local.properties
```

### Running on an emulator

```bash
# Build & install debug APK via command line:
cd android
./gradlew assembleDebug
# APK is at: app/build/outputs/apk/debug/app-debug.apk

# Or simply press the green Run button in Android Studio
# (make sure an AVD emulator is selected)
```

The debug build points to `http://10.0.2.2:8000/api/v1/` which is the Android emulator's alias for `localhost:8000`. Start the FastAPI backend first (`uvicorn app.main:app --reload --port 8000`) before launching the app.

### Running on a physical device

1. Enable **Developer Options** → **USB Debugging** on your phone
2. Connect via USB
3. Select your device in Android Studio's device picker
4. Press Run

### Changing the API base URL

The URL is baked in at compile time via `BuildConfig.API_BASE_URL`.

- **Debug** (`10.0.2.2:8000`) — emulator localhost alias, set in `android/app/build.gradle.kts`
- **Release** (`youruser.pythonanywhere.com`) — update the `release` `buildConfigField` in `build.gradle.kts` before building a production APK

### Building a release APK

```bash
cd android
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## Business model

- **Account signup** — free for providers and customers
- **Listing fee** — $5 CAD per listing to publish (one-time, non-recurring)
- Listings start as inactive drafts; paying the fee activates them in search

---

## Base URL

```
http://127.0.0.1:8000/api/v1
```

---

## Authentication

All protected endpoints require a Bearer token:

```
Authorization: Bearer <access_token>
```

Tokens are obtained from `POST /auth/register` or `POST /auth/login`.

### Roles

| Role | Access |
|---|---|
| `customer` | Browse, book, review |
| `provider` | Create/manage listings, upload documents, view dashboard |
| `admin` | Full platform access |

---

## API Reference

### Health

#### `GET /health`

```json
{ "status": "ok" }
```

---

### Auth

#### `POST /auth/register`
Create a new account. Admin self-registration is blocked.

**Body**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "role": "provider"
}
```

**Response 200**
```json
{ "access_token": "eyJ...", "token_type": "bearer" }
```

**Errors** · `409` email exists · `403` admin role blocked

---

#### `POST /auth/login`

**Body**
```json
{ "email": "user@example.com", "password": "password123" }
```

**Response 200**
```json
{ "access_token": "eyJ...", "token_type": "bearer" }
```

**Errors** · `401` invalid credentials

---

#### `GET /auth/me`
*Auth: any role*

**Response 200**
```json
{ "id": 1, "email": "user@example.com", "role": "provider", "is_active": 1 }
```

---

### Provider Profile

Must be created before posting listings or uploading documents.

#### `POST /providers/profile`
*Auth: provider* — create or update profile

**Body**
```json
{
  "full_name": "John Smith",
  "bio": "Licensed plumber with 10 years experience.",
  "location": "Cambridge, ON",
  "hourly_rate": 75.00
}
```

**Response 200**
```json
{ "id": 1, "user_id": 1, "full_name": "John Smith", "location": "Cambridge, ON", "hourly_rate": 75.0 }
```

---

### Listings (Services)

Listings are service offerings. Each costs **$5 CAD** to publish.  
New listings start as `is_active=0` drafts until the fee is paid.

#### `POST /services`
*Auth: provider* — create a listing draft

**Body**
```json
{
  "category_id": 2,
  "title": "Deep House Cleaning",
  "description": "Full residential deep clean.",
  "price": 80.00
}
```

**Response 200**
```json
{
  "id": 4,
  "provider_id": 1,
  "category_id": 2,
  "title": "Deep House Cleaning",
  "price": 80.0,
  "is_active": false,
  "payment_status": "pending",
  "listing_fee": 5.0
}
```

**Errors** · `400` provider profile not set up

---

#### `POST /listings/{service_id}/pay`
*Auth: provider* — pay the $5 listing fee to publish the draft  
*(Demo mode — no real Stripe charge)*

**Response 200**
```json
{
  "id": 1,
  "service_id": 4,
  "amount": 5.0,
  "currency": "CAD",
  "status": "paid",
  "paid_at": "2026-04-16T20:00:00"
}
```

**Errors** · `400` already paid · `404` listing not found or not yours

---

#### `GET /provider/listings`
*Auth: provider* — all listings with payment status

**Response 200**
```json
[
  {
    "id": 4,
    "title": "Deep House Cleaning",
    "description": "...",
    "price": 80.0,
    "is_active": 1,
    "created_at": "2026-04-16T19:00:00",
    "category_name": "Cleaning",
    "payment_status": "paid",
    "listing_fee": 5.0,
    "paid_at": "2026-04-16T19:05:00"
  }
]
```

---

#### `PATCH /services/{service_id}`
*Auth: provider* — update a listing (all fields optional)

**Body**
```json
{
  "title": "Updated title",
  "description": "New description",
  "price": 95.00,
  "category_id": 3
}
```

**Response 200** — updated service row

**Errors** · `400` no fields provided · `404` not found or not yours

---

#### `DELETE /services/{service_id}`
*Auth: provider* — deactivate a listing (`is_active=0`, not deleted)

**Response 200**
```json
{ "id": 4, "is_active": false }
```

**Errors** · `404` not found or not yours

---

#### `GET /services`
*Public* — list all active services

**Query params**

| Param | Type | Default | Description |
|---|---|---|---|
| `active_only` | bool | `true` | Include inactive listings |

---

### Search

#### `GET /search/services`
*Public* — search and filter active listings

**Query params**

| Param | Type | Description |
|---|---|---|
| `query` | string | Full-text search in title and description |
| `category_id` | int | Filter by category ID |
| `location` | string | Provider location partial match |
| `min_price` | float | Minimum price |
| `max_price` | float | Maximum price |

**Example**
```
GET /api/v1/search/services?query=cleaning&location=Cambridge&min_price=50
```

**Response 200**
```json
[
  {
    "id": 4,
    "title": "Deep House Cleaning",
    "price": 80.0,
    "category_id": 2,
    "provider_name": "John Smith",
    "location": "Cambridge, ON"
  }
]
```

---

### Home / Marketplace

#### `GET /stepserve/home`
*Public* — all data for the home page in one call

**Response 200**
```json
{
  "categories": [{ "id": 2, "name": "Cleaning", "slug": "cleaning", "services_count": 3 }],
  "featured": [...],
  "latest": [...],
  "top_locations": [{ "location": "Cambridge, ON", "listings_count": 5 }]
}
```

> `top_locations` drives the city dropdown on the home page.  
> It is empty until providers have profiles with `location` set and at least one paid listing.

---

### Categories

#### `GET /categories`
*Public* — all categories with active service counts

**Response 200**
```json
[
  { "id": 2, "name": "Cleaning", "slug": "cleaning", "services_count": 3 }
]
```

---

#### `POST /categories`
*Auth: admin* — create a category

**Body**
```json
{ "name": "Roofing", "slug": "roofing" }
```

**Response 200**
```json
{ "id": 13, "name": "Roofing", "slug": "roofing" }
```

---

### Provider Dashboard

#### `GET /provider/dashboard`
*Auth: provider* — summary of services, bookings and uploads

**Response 200**
```json
{
  "services": [...],
  "bookings": [...],
  "uploads": [...]
}
```

---

### Documents

#### `POST /providers/uploads`
*Auth: provider* · `multipart/form-data`  
Upload a certification or insurance document. Requires provider profile.

**Form field**: `file` (PDF, JPG, PNG)

**Response 200**
```json
{ "id": 1, "file_name": "insurance_coi.pdf", "stored_path": "uploads/provider_1_insurance_coi.pdf" }
```

**Errors** · `400` provider profile not set up

---

#### `GET /providers/uploads`
*Auth: provider* — list uploaded documents

**Response 200**
```json
[
  {
    "id": 1,
    "file_name": "insurance_coi.pdf",
    "content_type": "application/pdf",
    "file_size": 204800,
    "created_at": "2026-04-16T19:00:00"
  }
]
```

---

### Bookings

#### `POST /bookings`
*Auth: customer* — book a service

**Body**
```json
{
  "service_id": 4,
  "start_time": "2026-05-01T09:00:00",
  "end_time": "2026-05-01T11:00:00"
}
```

**Response 200**
```json
{ "id": 1, "status": "pending", "total_price": 160.00 }
```

**Errors** · `400` invalid time window · `404` service not found/inactive

---

#### `GET /customer/bookings`
*Auth: customer* — list all bookings including `service_title`

---

### Payments (Booking)

#### `POST /payments`
*Auth: customer* — pay for a booking *(Demo mode)*

**Body**
```json
{ "booking_id": 1 }
```

**Response 200**
```json
{
  "id": 1,
  "status": "paid",
  "amount": 160.00,
  "stripe_payment_intent_id": "pi_demo_1"
}
```

---

### Reviews

#### `POST /reviews`
*Auth: customer* — review a **completed** booking

**Body**
```json
{ "booking_id": 1, "rating": 5, "comment": "Excellent service!" }
```

**Response 200**
```json
{ "id": 1, "booking_id": 1, "rating": 5, "comment": "Excellent service!" }
```

**Errors** · `400` booking not completed or not yours

---

### Admin

All admin endpoints require `admin` role.

#### `GET /admin/overview`

**Response 200**
```json
{
  "users_count": 42,
  "services_count": 18,
  "bookings_count": 7,
  "paid_total": 35.00
}
```

---

#### `GET /admin/users`
List all users (max 500) — `{ id, email, role, is_active, created_at }`

---

#### `PATCH /admin/users/{user_id}/status`
Activate or deactivate a user.

**Query param**: `active` (bool)  
**Example**: `PATCH /admin/users/5/status?active=false`

**Response 200**
```json
{ "user_id": 5, "is_active": false }
```

---

#### `GET /admin/bookings`
List all bookings across the platform (max 500), including `customer_email`.

---

## Database schema

```
users                   accounts (customer / provider / admin)
provider_profiles       bio, location, hourly rate
categories              12 pre-seeded service categories
services                listings (is_active=0 until listing fee paid)
listing_payments        $5 per-listing fee record (pending → paid)
bookings                customer service bookings
payments                booking payments (Stripe demo)
reviews                 post-booking customer reviews
provider_uploads        certification & insurance documents
```

### listing_payments

| Column | Type | Notes |
|---|---|---|
| id | INT PK | |
| service_id | INT | FK → services.id (unique) |
| provider_id | INT | FK → provider_profiles.id |
| amount | DECIMAL(10,2) | 5.00 |
| currency | CHAR(3) | CAD |
| status | ENUM | `pending` or `paid` |
| stripe_payment_intent_id | VARCHAR | populated on payment |
| paid_at | TIMESTAMP | populated on payment |

---

## Troubleshooting

**`500` on register/login**  
→ `bcrypt` version issue. Requires `bcrypt>=4.0.0`. Run `pip install -r requirements.txt`.

**`400 Provider profile required`**  
→ Call `POST /providers/profile` before creating listings or uploading files.

**Listings not showing in search**  
→ Listing must be paid (`POST /listings/{id}/pay`) to become `is_active=1`. Only active listings appear in search and on the home page.

**`401 Invalid token`**  
→ Token expired (24hr TTL). Call `POST /auth/login` to get a new one.

**City dropdown empty on home page**  
→ Cities come from `top_locations` in `GET /stepserve/home`. Populated only when providers have a `location` in their profile AND have at least one paid listing.

**Categories missing**  
→ Run `PYTHONPATH=. python scripts/init_db.py` to seed the 12 default categories.
