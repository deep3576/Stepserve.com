from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    app_name: str = "Stepserve API"
    app_env: str = "dev"
    secret_key: str = "change-me"
    access_token_expire_minutes: int = 60 * 24
    auto_create_tables: bool = False

    mysql_user: str = "stepserve"
    mysql_password: str = "stepserve"
    mysql_host: str = "127.0.0.1"
    mysql_port: int = 3306
    mysql_db: str = "stepserve"

    stripe_secret_key: str = "sk_test_change_me"
    stripe_webhook_secret: str = "whsec_change_me"

    database_url_override: str | None = None

    @property
    def database_url(self) -> str:
        if self.database_url_override:
            return self.database_url_override
        return (
            f"mysql+pymysql://{self.mysql_user}:{self.mysql_password}"
            f"@{self.mysql_host}:{self.mysql_port}/{self.mysql_db}"
        )


settings = Settings()
