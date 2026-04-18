from __future__ import annotations

import configparser
from pathlib import Path
from typing import Any

from pydantic_settings import BaseSettings, SettingsConfigDict

ROOT_DIR = Path(__file__).resolve().parents[2]
CONFIG_INI_PATH = ROOT_DIR / "config.ini"


def _to_bool(value: str, default: bool) -> bool:
    normalized = value.strip().lower()
    if normalized in {"1", "true", "yes", "on"}:
        return True
    if normalized in {"0", "false", "no", "off"}:
        return False
    return default


def _load_config_ini_defaults() -> dict[str, Any]:
    if not CONFIG_INI_PATH.exists():
        return {}

    parser = configparser.ConfigParser()
    parser.read(CONFIG_INI_PATH, encoding="utf-8")

    defaults: dict[str, Any] = {}

    if parser.has_section("flask"):
        defaults["secret_key"] = parser.get("flask", "secret_key", fallback="change-me")
        defaults["app_env"] = parser.get("flask", "env", fallback="dev")
        defaults["debug"] = _to_bool(parser.get("flask", "debug", fallback="false"), False)

    if parser.has_section("mysql"):
        defaults["mysql_host"] = parser.get("mysql", "host", fallback="127.0.0.1")
        defaults["mysql_port"] = parser.getint("mysql", "port", fallback=3306)
        defaults["mysql_user"] = parser.get("mysql", "user", fallback="stepserve")
        defaults["mysql_password"] = parser.get("mysql", "password", fallback="stepserve")
        defaults["mysql_db"] = parser.get("mysql", "database", fallback="stepserve")
        defaults["mysql_charset"] = parser.get("mysql", "charset", fallback="utf8mb4")

    if parser.has_section("api"):
        defaults["api_base_url"] = parser.get("api", "base_url", fallback="http://127.0.0.1:8000/api/v1")

    if parser.has_section("stripe"):
        defaults["stripe_secret_key"] = parser.get("stripe", "secret_key", fallback="sk_test_change_me")
        defaults["stripe_publishable_key"] = parser.get("stripe", "publishable_key", fallback="pk_test_change_me")
        defaults["stripe_webhook_secret"] = parser.get("stripe", "webhook_secret", fallback="whsec_change_me")
        defaults["stripe_listing_price_id"] = parser.get("stripe", "listing_price_id", fallback="price_change_me")

    if parser.has_section("security"):
        defaults["secret_key"] = parser.get("security", "secret_key",
                                             fallback=defaults.get("secret_key", "change-me"))
        defaults["access_token_expire_minutes"] = parser.getint(
            "security", "access_token_expire_minutes", fallback=1440)

    if parser.has_section("rate_limits"):
        defaults["rl_public_per_minute"] = parser.getint("rate_limits", "public_per_minute", fallback=60)
        defaults["rl_register_per_hour"] = parser.getint("rate_limits", "register_per_hour", fallback=20)
        defaults["rl_login_per_minute"] = parser.getint("rate_limits", "login_per_minute", fallback=10)
        defaults["rl_payment_per_minute"] = parser.getint("rate_limits", "payment_per_minute", fallback=10)

    return defaults


INI_DEFAULTS = _load_config_ini_defaults()


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    app_name: str = "Stepserve API"
    app_env: str = INI_DEFAULTS.get("app_env", "dev")
    debug: bool = INI_DEFAULTS.get("debug", False)
    secret_key: str = INI_DEFAULTS.get("secret_key", "change-me")
    access_token_expire_minutes: int = INI_DEFAULTS.get("access_token_expire_minutes", 60 * 24)
    auto_create_tables: bool = False

    mysql_user: str = INI_DEFAULTS.get("mysql_user", "stepserve")
    mysql_password: str = INI_DEFAULTS.get("mysql_password", "stepserve")
    mysql_host: str = INI_DEFAULTS.get("mysql_host", "127.0.0.1")
    mysql_port: int = INI_DEFAULTS.get("mysql_port", 3306)
    mysql_db: str = INI_DEFAULTS.get("mysql_db", "stepserve")
    mysql_charset: str = INI_DEFAULTS.get("mysql_charset", "utf8mb4")

    stripe_secret_key: str = INI_DEFAULTS.get("stripe_secret_key", "sk_test_change_me")
    stripe_publishable_key: str = INI_DEFAULTS.get("stripe_publishable_key", "pk_test_change_me")
    stripe_webhook_secret: str = INI_DEFAULTS.get("stripe_webhook_secret", "whsec_change_me")
    stripe_listing_price_id: str = INI_DEFAULTS.get("stripe_listing_price_id", "price_change_me")

    # Rate limit settings (driven from config.ini [rate_limits])
    rl_public_per_minute: int = INI_DEFAULTS.get("rl_public_per_minute", 60)
    rl_register_per_hour: int = INI_DEFAULTS.get("rl_register_per_hour", 20)
    rl_login_per_minute: int = INI_DEFAULTS.get("rl_login_per_minute", 10)
    rl_payment_per_minute: int = INI_DEFAULTS.get("rl_payment_per_minute", 10)

    # Comma-separated list of allowed CORS origins. Override via CORS_ORIGINS env var or config.ini.
    cors_origins: str = (
        "http://localhost:5170,http://localhost:5171,http://localhost:5172,"
        "http://localhost:5173,http://localhost:5174,http://localhost:5175,"
        "http://127.0.0.1:5170,http://127.0.0.1:5171,http://127.0.0.1:5172,"
        "http://127.0.0.1:5173,http://127.0.0.1:5174,http://127.0.0.1:5175"
    )

    @property
    def cors_origins_list(self) -> list[str]:
        return [o.strip() for o in self.cors_origins.split(",") if o.strip()]

    database_url_override: str | None = None
    api_base_url: str = INI_DEFAULTS.get("api_base_url", "http://127.0.0.1:8000/api/v1")

    @property
    def database_url(self) -> str:
        if self.database_url_override:
            return self.database_url_override
        return (
            f"mysql+pymysql://{self.mysql_user}:{self.mysql_password}"
            f"@{self.mysql_host}:{self.mysql_port}/{self.mysql_db}"
        )


settings = Settings()
