"""Base repository implementing common CRUD operations."""

from typing import Any, Generic, TypeVar

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.base import Base

ModelType = TypeVar("ModelType", bound=Base)


class BaseRepository(Generic[ModelType]):
    """
    Base repository class implementing the repository pattern.

    Provides common CRUD operations for SQLAlchemy models.
    All repositories should inherit from this class.
    """

    def __init__(self, model: type[ModelType], db: AsyncSession) -> None:
        """
        Initialize the repository.

        Args:
            model: The SQLAlchemy model class
            db: The async database session
        """
        self.model = model
        self.db = db

    async def create(self, obj_in: dict[str, Any]) -> ModelType:
        """
        Create a new record in the database.

        Args:
            obj_in: Dictionary of attributes for the new record

        Returns:
            The created model instance
        """
        db_obj = self.model(**obj_in)
        self.db.add(db_obj)
        await self.db.flush()
        await self.db.refresh(db_obj)
        return db_obj

    async def get_by_id(self, id: int) -> ModelType | None:
        """
        Get a record by its primary key ID.

        Args:
            id: The primary key ID

        Returns:
            The model instance if found, None otherwise
        """
        result = await self.db.execute(select(self.model).where(self.model.id == id))
        return result.scalar_one_or_none()

    async def get_all(self, skip: int = 0, limit: int = 100) -> list[ModelType]:
        """
        Get all records with pagination.

        Args:
            skip: Number of records to skip
            limit: Maximum number of records to return

        Returns:
            List of model instances
        """
        result = await self.db.execute(select(self.model).offset(skip).limit(limit))
        return list(result.scalars().all())

    async def update(self, db_obj: ModelType, obj_in: dict[str, Any]) -> ModelType:
        """
        Update an existing record.

        Args:
            db_obj: The existing model instance
            obj_in: Dictionary of attributes to update

        Returns:
            The updated model instance
        """
        for field, value in obj_in.items():
            if hasattr(db_obj, field):
                setattr(db_obj, field, value)
        await self.db.flush()
        await self.db.refresh(db_obj)
        return db_obj

    async def delete(self, db_obj: ModelType) -> None:
        """
        Delete a record from the database.

        Args:
            db_obj: The model instance to delete
        """
        await self.db.delete(db_obj)
        await self.db.flush()

    async def count(self) -> int:
        """
        Count total number of records.

        Returns:
            Total count of records
        """
        from sqlalchemy import func

        result = await self.db.execute(select(func.count()).select_from(self.model))
        return result.scalar() or 0