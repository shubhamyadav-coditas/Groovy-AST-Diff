"""File repository for database operations on file records."""

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.file import FileRecord
from app.repositories.base import BaseRepository


class FileRepository(BaseRepository[FileRecord]):
    """
    Repository for FileRecord model.

    Extends BaseRepository with file-specific query methods.
    """

    def __init__(self, db: AsyncSession) -> None:
        """
        Initialize the file repository.

        Args:
            db: The async database session
        """
        super().__init__(FileRecord, db)

    async def get_by_uuid(self, uuid: str) -> FileRecord | None:
        """
        Get a file record by its UUID.

        Args:
            uuid: The unique identifier of the file

        Returns:
            The FileRecord if found, None otherwise
        """
        result = await self.db.execute(
            select(FileRecord).where(FileRecord.uuid == uuid)
        )
        return result.scalar_one_or_none()

    async def get_by_extension(
        self, extension: str, skip: int = 0, limit: int = 100
    ) -> list[FileRecord]:
        """
        Get all files with a specific extension.

        Args:
            extension: The file extension to filter by (e.g., '.groovy')
            skip: Number of records to skip
            limit: Maximum number of records to return

        Returns:
            List of FileRecord instances with the specified extension
        """
        result = await self.db.execute(
            select(FileRecord)
            .where(FileRecord.file_extension == extension)
            .offset(skip)
            .limit(limit)
        )
        return list(result.scalars().all())

    async def get_groovy_files(self, skip: int = 0, limit: int = 100) -> list[FileRecord]:
        """
        Get all Groovy files (.groovy, .gradle).

        Args:
            skip: Number of records to skip
            limit: Maximum number of records to return

        Returns:
            List of Groovy FileRecord instances
        """
        groovy_extensions = [".groovy", ".gradle"]
        result = await self.db.execute(
            select(FileRecord)
            .where(FileRecord.file_extension.in_(groovy_extensions))
            .offset(skip)
            .limit(limit)
        )
        return list(result.scalars().all())

    async def delete_by_uuid(self, uuid: str) -> bool:
        """
        Delete a file record by its UUID.

        Args:
            uuid: The unique identifier of the file

        Returns:
            True if deleted, False if not found
        """
        file_record = await self.get_by_uuid(uuid)
        if file_record:
            await self.delete(file_record)
            return True
        return False