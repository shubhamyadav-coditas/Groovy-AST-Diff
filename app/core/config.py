"""Application configuration settings."""

from pathlib import Path
from typing import ClassVar

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
    )

    # Application (with defaults)
    APP_NAME: str = "Groovy AST Diff API"
    APP_VERSION: str = "0.1.0"
    DEBUG: bool = True

    # API (with defaults)
    API_V1_PREFIX: str = "/api/v1"

    # PostgreSQL Database Configuration (with defaults for SQLite fallback)
    POSTGRES_USER: str = "groovy_user"
    POSTGRES_PASSWORD: str = "groovy_password"
    POSTGRES_HOST: str = "localhost"
    POSTGRES_PORT: int = 5432
    POSTGRES_DB: str = "groovy_ast_diff"

    # JWT Configuration (with defaults)
    JWT_SECRET_KEY: str = "your-super-secret-jwt-key-change-this-in-production"
    JWT_ALGORITHM: str = "HS256"
    JWT_ACCESS_TOKEN_EXPIRE_MINUTES: int = 30

    # File Upload (with defaults)
    UPLOAD_DIR: str = "uploads"
    MAX_FILE_SIZE: int = 10485760  # 10MB
    ALLOW_ANY_FILE: bool = True

    # AST Parser Configuration (with defaults)
    AST_LANGUAGE_NAME: str = "groovy"

    # Allowed extensions (not from .env - constant)
    ALLOWED_EXTENSIONS: ClassVar[set[str]] = {".groovy", ".gradle"}

    @property
    def DATABASE_URL(self) -> str:
        """Construct SQLite async database URL for demo purposes."""
        return "sqlite+aiosqlite:///./groovy_ast_diff.db"

    @property
    def DATABASE_URL_SYNC(self) -> str:
        """Construct SQLite sync database URL (for Alembic)."""
        return "sqlite:///./groovy_ast_diff.db"

    @property
    def upload_path(self) -> Path:
        """Get the upload directory path."""
        path = Path(self.UPLOAD_DIR)
        path.mkdir(parents=True, exist_ok=True)
        return path


# Global settings instance
settings = Settings()