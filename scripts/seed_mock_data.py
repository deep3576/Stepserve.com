"""
Seed realistic mock data for StepServe.
Safe to run multiple times — uses INSERT IGNORE / duplicate checks.
"""
import bcrypt
import pymysql
from pymysql.cursors import DictCursor

# ── Config ────────────────────────────────────────────────────
DB = dict(host="127.0.0.1", user="deep3576", password="Gmsshn!43",
          database="deep3576$StepServe", port=3306, charset="utf8mb4",
          cursorclass=DictCursor, autocommit=False)

PASS_HASH = bcrypt.hashpw(b"StepServe2026!", bcrypt.gensalt()).decode()

# ── Provider seed data ────────────────────────────────────────
PROVIDERS = [
    {
        "email": "sarah.chen@sparkle.ca",
        "profile": {"full_name": "Sarah Chen", "bio": "Professional residential and commercial cleaning since 2015. Eco-friendly products, fully bonded and insured.", "location": "Cambridge, ON", "hourly_rate": 45.00},
        "listings": [
            {"cat": "Cleaning", "title": "Residential Deep Clean", "description": "Full home deep clean — kitchen, bathrooms, bedrooms, living areas. Eco-friendly products used throughout.", "price": 120.00},
            {"cat": "Cleaning", "title": "Move-In / Move-Out Cleaning", "description": "Thorough cleaning for incoming or outgoing tenants. Includes inside appliances, cabinets, and baseboards.", "price": 180.00},
        ],
    },
    {
        "email": "mike.obrien@plumbing.ca",
        "profile": {"full_name": "Mike O'Brien", "bio": "Licensed master plumber with 20+ years experience. Drain clearing, pipe repair, water heater installs.", "location": "Cambridge, ON", "hourly_rate": 95.00},
        "listings": [
            {"cat": "Plumbing", "title": "Drain Clearing & Inspection", "description": "Camera inspection + clearing of clogged drains. Kitchen, bathroom, and main line service.", "price": 150.00},
            {"cat": "Plumbing", "title": "Water Heater Installation", "description": "Supply and install gas or electric water heaters. Old unit haul-away included.", "price": 650.00},
        ],
    },
    {
        "email": "james.wilson@greenthumb.ca",
        "profile": {"full_name": "James Wilson", "bio": "Award-winning landscape design and maintenance. Specializing in residential gardens across Waterloo Region.", "location": "Kitchener, ON", "hourly_rate": 55.00},
        "listings": [
            {"cat": "Landscaping", "title": "Spring Lawn & Garden Cleanup", "description": "Debris removal, lawn aeration, edge trimming, pruning, and mulching. Ready your yard for the season.", "price": 200.00},
            {"cat": "Landscaping", "title": "Weekly Lawn Maintenance", "description": "Mowing, edging, blowing, and waste removal. Available weekly or bi-weekly.", "price": 65.00},
        ],
    },
    {
        "email": "emily.rodriguez@profinish.ca",
        "profile": {"full_name": "Emily Rodriguez", "bio": "Interior and exterior painting. Serving Waterloo Region for 12 years. Cabinet refinishing our specialty.", "location": "Waterloo, ON", "hourly_rate": 60.00},
        "listings": [
            {"cat": "Painting", "title": "Interior Room Painting", "description": "Professional paint job — walls, trim, and ceilings. Premium paints. Free colour consultation included.", "price": 350.00},
            {"cat": "Painting", "title": "Exterior House Painting", "description": "Full exterior repaint including prep, priming, and two coats. Pressure washing included.", "price": 1200.00},
        ],
    },
    {
        "email": "david.kim@wattsup.ca",
        "profile": {"full_name": "David Kim", "bio": "Licensed electrical contractor. EV charger installs, panel upgrades, pot lights. ESA-certified.", "location": "Guelph, ON", "hourly_rate": 110.00},
        "listings": [
            {"cat": "Electrical", "title": "EV Charger Installation", "description": "Level 2 EV charger supply and install. Panel capacity assessment, permit included.", "price": 850.00},
            {"cat": "Electrical", "title": "Pot Light Installation", "description": "LED pot light supply and installation. 4-light package, dimmable. Drywall-friendly installation.", "price": 480.00},
        ],
    },
    {
        "email": "lisa.thompson@comfortzone.ca",
        "profile": {"full_name": "Lisa Thompson", "bio": "Furnace, A/C, and heat pump installs and repairs. TSSA certified. Emergency service available 24/7.", "location": "Hamilton, ON", "hourly_rate": 120.00},
        "listings": [
            {"cat": "HVAC", "title": "Furnace Tune-Up & Safety Check", "description": "Annual furnace inspection, cleaning, filter change, and carbon monoxide test. Certificate provided.", "price": 149.00},
            {"cat": "HVAC", "title": "Central A/C Installation", "description": "Supply and install central air conditioning system. Load calculation, permits, and startup included.", "price": 3800.00},
        ],
    },
    {
        "email": "tom.baker@movingpros.ca",
        "profile": {"full_name": "Tom Baker", "bio": "Local and long-distance moves. Careful handling guaranteed. 2-man team with 16ft truck.", "location": "Toronto, ON", "hourly_rate": 85.00},
        "listings": [
            {"cat": "Moving", "title": "Local Move (2-man team)", "description": "2 movers + 16ft truck for moves within 50km. Furniture wrapping and disassembly included.", "price": 350.00},
            {"cat": "Moving", "title": "Packing Service", "description": "Full-home packing by our professional team. All materials (boxes, tape, wrap) included.", "price": 280.00},
        ],
    },
    {
        "email": "anna.foster@petpaws.ca",
        "profile": {"full_name": "Anna Foster", "bio": "Certified pet sitter and dog walker. 10+ years caring for pets of all sizes. Insured and bonded.", "location": "Kitchener, ON", "hourly_rate": 30.00},
        "listings": [
            {"cat": "Pet Care", "title": "Dog Walking (1 hour)", "description": "Daily dog walks in your neighbourhood. GPS route shared after each walk. Fully insured.", "price": 25.00},
            {"cat": "Pet Care", "title": "In-Home Pet Sitting", "description": "Daily visits to your home to feed, play, and care for your pets while you're away.", "price": 40.00},
        ],
    },
    {
        "email": "carlos.mendez@carpentry.ca",
        "profile": {"full_name": "Carlos Mendez", "bio": "Custom carpentry and woodworking. Decks, fences, furniture, and cabinet installation.", "location": "Waterloo, ON", "hourly_rate": 75.00},
        "listings": [
            {"cat": "Carpentry", "title": "Deck Building & Repair", "description": "Custom deck design, build, or repair. Pressure-treated or composite lumber. Permits handled.", "price": 4500.00},
            {"cat": "Carpentry", "title": "Custom Cabinet Installation", "description": "Kitchen and bathroom cabinet installation. Handles and hardware included.", "price": 800.00},
        ],
    },
    {
        "email": "rachel.nguyen@renov8.ca",
        "profile": {"full_name": "Rachel Nguyen", "bio": "Full-service home renovation contractor. Kitchens, bathrooms, basements. Licensed and insured.", "location": "Toronto, ON", "hourly_rate": 90.00},
        "listings": [
            {"cat": "Renovation", "title": "Bathroom Renovation", "description": "Complete bathroom gut and renovation — tile, vanity, plumbing fixtures, and lighting.", "price": 8500.00},
            {"cat": "Renovation", "title": "Basement Finishing", "description": "Convert unfinished basement to livable space. Framing, drywall, flooring, electrical included.", "price": 18000.00},
        ],
    },
]

CUSTOMERS = [
    {"email": "john.homeowner@gmail.com", "role": "customer"},
    {"email": "maria.smith@outlook.com", "role": "customer"},
    {"email": "alex.jones@yahoo.ca", "role": "customer"},
]

ADMIN = {"email": "admin@stepserve.com", "role": "admin"}


def get_cat_id(cur, name):
    cur.execute("SELECT id FROM categories WHERE name=%s LIMIT 1", (name,))
    row = cur.fetchone()
    return row["id"] if row else None


def user_exists(cur, email):
    cur.execute("SELECT id FROM users WHERE email=%s LIMIT 1", (email,))
    return cur.fetchone()


def main():
    conn = pymysql.connect(**DB)
    try:
        with conn.cursor() as cur:

            # ── Admin account ──────────────────────────────────
            if not user_exists(cur, ADMIN["email"]):
                cur.execute(
                    "INSERT INTO users (email, password_hash, role, is_active) VALUES (%s,%s,%s,1)",
                    (ADMIN["email"], PASS_HASH, ADMIN["role"]),
                )
                print(f"  Created admin: {ADMIN['email']}")
            else:
                print(f"  Admin exists: {ADMIN['email']}")

            # ── Customer accounts ──────────────────────────────
            for c in CUSTOMERS:
                if not user_exists(cur, c["email"]):
                    cur.execute(
                        "INSERT INTO users (email, password_hash, role, is_active) VALUES (%s,%s,%s,1)",
                        (c["email"], PASS_HASH, c["role"]),
                    )
                    print(f"  Created customer: {c['email']}")
                else:
                    print(f"  Customer exists: {c['email']}")

            # ── Provider accounts + profiles + listings ────────
            for p in PROVIDERS:
                # User
                user_row = user_exists(cur, p["email"])
                if not user_row:
                    cur.execute(
                        "INSERT INTO users (email, password_hash, role, is_active) VALUES (%s,%s,'provider',1)",
                        (p["email"], PASS_HASH),
                    )
                    user_id = cur.lastrowid
                    print(f"  Created provider user: {p['email']}")
                else:
                    user_id = user_row["id"]
                    print(f"  Provider user exists: {p['email']}")

                # Profile
                cur.execute("SELECT id FROM provider_profiles WHERE user_id=%s LIMIT 1", (user_id,))
                prof = cur.fetchone()
                if not prof:
                    pr = p["profile"]
                    cur.execute(
                        "INSERT INTO provider_profiles (user_id, full_name, bio, location, hourly_rate) VALUES (%s,%s,%s,%s,%s)",
                        (user_id, pr["full_name"], pr["bio"], pr["location"], pr["hourly_rate"]),
                    )
                    profile_id = cur.lastrowid
                    print(f"    Profile created for {pr['full_name']}")
                else:
                    profile_id = prof["id"]
                    print(f"    Profile exists (id={profile_id})")

                # Listings
                for listing in p["listings"]:
                    cat_id = get_cat_id(cur, listing["cat"])
                    if not cat_id:
                        print(f"    WARNING: category '{listing['cat']}' not found, skipping")
                        continue

                    # Check if listing already exists
                    cur.execute(
                        "SELECT id FROM services WHERE provider_id=%s AND title=%s LIMIT 1",
                        (profile_id, listing["title"]),
                    )
                    existing = cur.fetchone()
                    if existing:
                        svc_id = existing["id"]
                        # Make sure it's active
                        cur.execute("UPDATE services SET is_active=1 WHERE id=%s", (svc_id,))
                        print(f"    Listing exists: {listing['title']}")
                    else:
                        cur.execute(
                            "INSERT INTO services (provider_id, category_id, title, description, price, is_active) VALUES (%s,%s,%s,%s,%s,1)",
                            (profile_id, cat_id, listing["title"], listing["description"], listing["price"]),
                        )
                        svc_id = cur.lastrowid
                        print(f"    Created listing: {listing['title']}")

                    # Listing payment record (paid)
                    cur.execute("SELECT id FROM listing_payments WHERE service_id=%s LIMIT 1", (svc_id,))
                    if not cur.fetchone():
                        cur.execute(
                            "INSERT INTO listing_payments (service_id, provider_id, amount, currency, status, stripe_payment_intent_id, paid_at) "
                            "VALUES (%s,%s,5.00,'CAD','paid',%s,NOW())",
                            (svc_id, profile_id, f"pi_seed_{svc_id}"),
                        )
                        print(f"      Payment record created for listing {svc_id}")

        conn.commit()
        print("\nSeed complete. All providers, listings and payment records created.")
        print(f"Login password for all seeded accounts: StepServe2026!")
        print(f"Admin login: admin@stepserve.com / StepServe2026!")

    finally:
        conn.close()


if __name__ == "__main__":
    main()
