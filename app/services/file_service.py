"""File service containing business logic for file operations."""

import os
from pathlib import Path
from uuid import uuid4

import aiofiles
from fastapi import HTTPException, UploadFile, status

from app.core.config import settings
from app.models.file import FileRecord
from app.repositories.file_repository import FileRepository


class FileService:
    """
    Service layer for file operations.

    Contains business logic and orchestrates between
    the API layer and repository layer.
    """

    def __init__(self, repository: FileRepository) -> None:
        """
        Initialize the file service.

        Args:
            repository: The file repository for database operations
        """
        self.repository = repository

    async def upload_file(self, file: UploadFile) -> FileRecord:
        """
        Handle file upload with validation and storage.

        Args:
            file: The uploaded file from FastAPI

        Returns:
            The created FileRecord instance

        Raises:
            HTTPException: If file validation fails
        """
        # Validate file
        await self._validate_file(file)

        # Generate unique filename
        original_filename = file.filename or "unnamed_file"
        file_extension = self._get_file_extension(original_filename)
        stored_filename = f"{uuid4()}{file_extension}"

        # Save file to disk
        file_path = await self._save_file_to_disk(file, stored_filename)

        # Get file size
        file_size = os.path.getsize(file_path)

        # Create database record
        file_data = {
            "original_filename": original_filename,
            "stored_filename": stored_filename,
            "file_path": str(file_path),
            "file_size": file_size,
            "content_type": file.content_type,
            "file_extension": file_extension,
        }

        return await self.repository.create(file_data)

    async def get_file_by_uuid(self, uuid: str) -> FileRecord:
        """
        Get a file record by UUID.

        Args:
            uuid: The unique identifier of the file

        Returns:
            The FileRecord instance

        Raises:
            HTTPException: If file not found
        """
        file_record = await self.repository.get_by_uuid(uuid)
        if not file_record:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"File with UUID '{uuid}' not found",
            )
        return file_record

    async def get_all_files(self, skip: int = 0, limit: int = 100) -> tuple[list[FileRecord], int]:
        """
        Get all file records with pagination.

        Args:
            skip: Number of records to skip
            limit: Maximum number of records to return

        Returns:
            Tuple of (list of FileRecords, total count)
        """
        files = await self.repository.get_all(skip=skip, limit=limit)
        total = await self.repository.count()
        return files, total

    async def delete_file(self, uuid: str) -> None:
        """
        Delete a file record and its associated file.

        Args:
            uuid: The unique identifier of the file

        Raises:
            HTTPException: If file not found
        """
        file_record = await self.get_file_by_uuid(uuid)

        # Delete file from disk
        file_path = Path(file_record.file_path)
        if file_path.exists():
            file_path.unlink()

        # Delete database record
        await self.repository.delete(file_record)

    async def validate_file(self, file: UploadFile, file_label: str = "file") -> None:
        """
        Validate the uploaded file.

        This method is public so it can be reused for upfront validation
        (e.g., validating multiple files before processing any).

        Args:
            file: The uploaded file
            file_label: Label for error messages (e.g., "file_a", "file_b")

        Raises:
            HTTPException: If validation fails
        """
        if not file.filename:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"{file_label}: Filename is required",
            )

        # Check file extension if restricted to Groovy files
        if not settings.ALLOW_ANY_FILE:
            extension = self._get_file_extension(file.filename)
            if extension.lower() not in settings.ALLOWED_EXTENSIONS:
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail=f"Incompatible file type for {file_label}: '{file.filename}'. "
                    f"Only Groovy files are supported. "
                    f"Allowed types: {', '.join(sorted(settings.ALLOWED_EXTENSIONS))}",
                )

        # Check file size (read content to determine size)
        content = await file.read()
        await file.seek(0)  # Reset file pointer

        if len(content) > settings.MAX_FILE_SIZE:
            raise HTTPException(
                status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
                detail=f"{file_label}: File size exceeds maximum allowed size of {settings.MAX_FILE_SIZE} bytes",
            )

    async def _validate_file(self, file: UploadFile) -> None:
        """Internal wrapper for backwards compatibility."""
        await self.validate_file(file, "file")

    async def _save_file_to_disk(self, file: UploadFile, stored_filename: str) -> Path:
        """
        Save the uploaded file to disk.

        Args:
            file: The uploaded file
            stored_filename: The filename to use when storing

        Returns:
            Path to the saved file
        """
        file_path = settings.upload_path / stored_filename

        async with aiofiles.open(file_path, "wb") as out_file:
            content = await file.read()
            await out_file.write(content)

        return file_path

    @staticmethod
    def _get_file_extension(filename: str) -> str:
        """
        Get the file extension from a filename.

        Args:
            filename: The filename to extract extension from

        Returns:
            The file extension including the dot (e.g., '.groovy')
        """
        return Path(filename).suffix.lower()