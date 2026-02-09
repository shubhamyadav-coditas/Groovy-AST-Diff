"""API v1 router aggregating all endpoints."""

from fastapi import APIRouter

from app.api.v1.endpoints import file_upload, groovy_comparison, change_complexity

api_router = APIRouter()

# Include all endpoint routers
api_router.include_router(file_upload.router)
api_router.include_router(groovy_comparison.router)
api_router.include_router(change_complexity.router)