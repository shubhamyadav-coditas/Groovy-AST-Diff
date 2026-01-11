"""File upload endpoints."""

from typing import Annotated

from fastapi import APIRouter, File, Query, UploadFile, status

from app.core.dependencies import FileServiceDep
from app.schemas.file import FileListResponse, FileResponse, FileUploadResponse

router = APIRouter(prefix="/files", tags=["File Management"])


@router.post(
    "/upload",
    response_model=FileUploadResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Upload a Groovy file",
    description="Upload a Groovy file to the server for later comparison.",
)
async def upload_file(
    file_service: FileServiceDep,
    file: Annotated[UploadFile, File(description="Groovy file to upload")],
) -> FileUploadResponse:
    """
    Upload a Groovy file to the server.

    Args:
        file: The Groovy file to upload

    Returns:
        FileUploadResponse with upload confirmation and file details

    Raises:
        HTTPException 400: If file validation fails
        HTTPException 413: If file size exceeds limit
    """
    file_record = await file_service.upload_file(file)

    return FileUploadResponse(
        message=f"File '{file_record.original_filename}' uploaded successfully",
        file=FileResponse.model_validate(file_record),
    )


@router.get(
    "/",
    response_model=FileListResponse,
    summary="List all uploaded files",
    description="Get a paginated list of all uploaded files.",
)
async def list_files(
    file_service: FileServiceDep,
    skip: Annotated[int, Query(ge=0, description="Number of files to skip")] = 0,
    limit: Annotated[int, Query(ge=1, le=100, description="Maximum number of files to return")] = 50,
) -> FileListResponse:
    """
    Get a list of all uploaded files with pagination.

    Args:
        skip: Number of files to skip (for pagination)
        limit: Maximum number of files to return (1-100)

    Returns:
        FileListResponse with list of files and pagination info
    """
    files, total = await file_service.get_all_files(skip=skip, limit=limit)

    return FileListResponse(
        files=[FileResponse.model_validate(file) for file in files],
        total=total,
        skip=skip,
        limit=limit,
    )


@router.get(
    "/{file_uuid}",
    response_model=FileResponse,
    summary="Get file by UUID",
    description="Get detailed information about a specific file by its UUID.",
)
async def get_file(
    file_service: FileServiceDep,
    file_uuid: str,
) -> FileResponse:
    """
    Get detailed information about a specific file.

    Args:
        file_uuid: The unique identifier of the file

    Returns:
        FileResponse with file details

    Raises:
        HTTPException 404: If file not found
    """
    file_record = await file_service.get_file_by_uuid(file_uuid)
    return FileResponse.model_validate(file_record)


@router.delete(
    "/{file_uuid}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Delete file",
    description="Delete a file and its associated record from the server.",
)
async def delete_file(
    file_service: FileServiceDep,
    file_uuid: str,
) -> None:
    """
    Delete a file and its associated record.

    Args:
        file_uuid: The unique identifier of the file to delete

    Raises:
        HTTPException 404: If file not found
    """
    await file_service.delete_file(file_uuid)