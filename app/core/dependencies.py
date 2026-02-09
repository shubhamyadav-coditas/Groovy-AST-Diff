"""Dependency injection container for the application."""

from typing import Annotated, AsyncGenerator

from fastapi import Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.database import get_db
from app.repositories.file_repository import FileRepository
from app.services.file_service import FileService
from app.services.groovy_comparison_service import GroovyComparisonService
from app.services.change_complexity_service import ChangeComplexityService


async def get_file_repository(
    db: Annotated[AsyncSession, Depends(get_db)],
) -> AsyncGenerator[FileRepository, None]:
    """Dependency for getting FileRepository instance."""
    yield FileRepository(db)


async def get_file_service(
    repository: Annotated[FileRepository, Depends(get_file_repository)],
) -> AsyncGenerator[FileService, None]:
    """Dependency for getting FileService instance with injected repository."""
    yield FileService(repository)


def get_groovy_comparison_service() -> GroovyComparisonService:
    """Dependency for getting GroovyComparisonService instance."""
    return GroovyComparisonService()


def get_change_complexity_service() -> ChangeComplexityService:
    """Dependency for getting ChangeComplexityService instance."""
    return ChangeComplexityService()


# Type aliases for cleaner dependency injection in endpoints
FileRepositoryDep = Annotated[FileRepository, Depends(get_file_repository)]
FileServiceDep = Annotated[FileService, Depends(get_file_service)]
GroovyComparisonServiceDep = Annotated[GroovyComparisonService, Depends(get_groovy_comparison_service)]
ChangeComplexityServiceDep = Annotated[ChangeComplexityService, Depends(get_change_complexity_service)]