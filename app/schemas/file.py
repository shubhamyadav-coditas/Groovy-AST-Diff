"""Pydantic schemas for file operations."""

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict


class FileBase(BaseModel):
    """Base schema for file information."""

    original_filename: str
    file_size: int
    content_type: Optional[str] = None
    file_extension: str


class FileCreate(FileBase):
    """Schema for creating a new file record."""

    stored_filename: str
    file_path: str


class FileResponse(FileBase):
    """Schema for file response."""

    model_config = ConfigDict(from_attributes=True)

    uuid: str
    stored_filename: str
    file_path: str
    created_at: datetime
    updated_at: datetime


class FileUploadResponse(BaseModel):
    """Schema for file upload response."""

    message: str
    file: FileResponse


class FileListResponse(BaseModel):
    """Schema for file list response."""

    files: list[FileResponse]
    total: int
    skip: int
    limit: int


class FileInfoSimple(BaseModel):
    """Simplified file information for comparison responses."""

    file_uuid: str
    original_filename: str
    total_lines: int
    total_blocks: int
    class_count: int
    method_count: int
    field_count: int