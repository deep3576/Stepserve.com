# Stepserve.com backend (PythonAnywhere + MySQL 8.0)

This repo contains a direct-deploy backend for **PythonAnywhere servers** using **FastAPI + MySQL 8.0** (no Docker required).

## What is implemented

- JWT auth with roles: `customer`, `provider`, `admin`
- Provider profile management
- Category management (admin)
- Service creation + listing
- Booking creation (duration-based pricing)
- Payment recording flow (CAD default, Stripe-ready placeholder)
- Review creation after completed bookings

## Stack

- Python 3.11+
- FastAPI
- PyMySQL direct SQL queries (no ORM)
- MySQL 8.0 (`pymysql` driver)

## Direct setup (local or PythonAnywhere)

1. Create and activate a virtualenv:

```bash
python3.11 -m venv .venv
source .venv/bin/activate
```

2. Install dependencies:

```bash
pip install -r requirements.txt
```

3. Configure `config.ini` (primary configuration source).

   - Update `[flask]` and `[mysql]` values for your environment.
   - Optional `[api]` section controls frontend API default (`base_url`).

4. (Optional) Create `.env` for overrides:

```bash
cp .env.example .env
```

5. Initialize DB tables:

```bash
python scripts/init_db.py
```

6. Run locally for development:

```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

## UI (React, modern design)

A separate React frontend is provided in `ui/`.

```bash
cd ui
npm install
npm run dev
```

The UI is built with Vite + React and now follows a Karrot-style Stepserve classifieds experience: local-first feed, neighborhood search, category chips, featured/latest cards, account-based posting for handymen/providers, and an admin-only panel on admin login. The default API base URL is loaded from `config.ini` (`[api].base_url`) and can be overridden with `VITE_API_BASE_URL`.

## PythonAnywhere deployment

1. Upload/clone this project to your PythonAnywhere home.
2. Create virtualenv and install requirements (`pip install -r requirements.txt`).
3. Configure environment variables in your WSGI file or via `os.environ`.
4. In PythonAnywhere Web tab, point WSGI config to `passenger_wsgi.py` in this repo.
5. Reload the web app.

`passenger_wsgi.py` wraps FastAPI (ASGI) into WSGI using `a2wsgi`, which works with standard PythonAnywhere web app configuration.

## API

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me` (authenticated user profile + role)
- `GET /api/v1/categories` (public category listing with counts)
- `POST /api/v1/categories` (admin)
- `POST /api/v1/providers/profile` (provider)
- `POST /api/v1/services` (provider)
- `GET /api/v1/services`
- `POST /api/v1/bookings` (customer)
- `POST /api/v1/payments` (customer)
- `POST /api/v1/reviews` (customer, completed booking)
- `GET /api/v1/stepserve/home` (public Stepserve home feed: categories, featured, latest, top locations)
- `GET /api/v1/market/home` (legacy alias of Stepserve home feed)
- `GET /api/v1/search/services` (public search options)
- `GET /api/v1/customer/bookings` (customer view)
- `GET/POST /api/v1/providers/uploads` (handyman upload panel)
- `GET /api/v1/provider/dashboard` (handyman dashboard)
- `GET /api/v1/admin/overview` (admin panel)
- `GET /api/v1/admin/users` (admin panel)
- `PATCH /api/v1/admin/users/{user_id}/status` (admin moderation)
- `GET /api/v1/admin/bookings` (admin panel)

## Notes

- The project intentionally uses **direct SQL** and avoids ORM usage.
- Current payment endpoint uses a demo payment intent id (`pi_demo_*`); replace with Stripe PaymentIntent + webhook verification in production.
