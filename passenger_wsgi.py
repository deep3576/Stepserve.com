"""PythonAnywhere entrypoint for Stepserve FastAPI app.

Set your web app to point to this file. It wraps ASGI (FastAPI) as WSGI using a2wsgi
for compatibility with standard PythonAnywhere WSGI configuration.
"""

import os
import sys
from pathlib import Path

from a2wsgi import ASGIMiddleware

BASE_DIR = Path(__file__).resolve().parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

os.environ.setdefault("APP_ENV", "production")

from app.main import app as asgi_app  # noqa: E402

application = ASGIMiddleware(asgi_app)
