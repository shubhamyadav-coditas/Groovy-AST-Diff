"""Groovy AST comparison endpoints."""

from typing import Annotated

from fastapi import APIRouter, File, UploadFile, status

from app.core.dependencies import FileServiceDep, GroovyComparisonServiceDep
from app.schemas.file import FileInfoSimple
from app.schemas.groovy_comparison import (
    BlockDiffSchema,
    CompareFilesResponse,
    ComparisonSummary,
    StatementDiffSchema,
)
from app.types.groovy_types import ChangeType, StatementChangeType

router = APIRouter(prefix="/compare", tags=["Groovy AST Comparison"])


@router.post(
    "",
    response_model=CompareFilesResponse,
    status_code=status.HTTP_200_OK,
    summary="Compare two Groovy files",
    description=(
        "Upload two Groovy files and perform block-level comparison "
        "detecting additions, deletions, modifications, and moved code blocks."
    ),
)
async def compare_files(
    file_service: FileServiceDep,
    comparison_service: GroovyComparisonServiceDep,
    file_a: Annotated[UploadFile, File(description="First Groovy file (original/base)")],
    file_b: Annotated[UploadFile, File(description="Second Groovy file (modified/new)")],
) -> CompareFilesResponse:
    """
    Compare two Groovy files and return only the differences.

    This endpoint performs a comprehensive comparison:
    1. Validates both files are Groovy files
    2. Uploads and saves both files
    3. Performs block-level comparison to detect:
       - **ADDED**: Blocks that exist only in file B
       - **DELETED**: Blocks that exist only in file A
       - **MODIFIED**: Blocks with the same name but different content
       - **MOVED**: Blocks with same content but different position
       - **MOVED_MODIFIED**: Blocks moved AND content changed

    For modified blocks, statement-level diffs are also computed.

    Args:
        file_a: The first Groovy file (base/original version)
        file_b: The second Groovy file (modified/new version)

    Returns:
        CompareFilesResponse with:
        - Basic file info (UUID, filename, line count, block counts)
        - Comparison summary with change counts
        - Detailed list of all differences

    Raises:
        HTTPException 400: If either file is not a Groovy file
    """
    # Validate both files BEFORE uploading either (fail fast)
    await file_service.validate_file(file_a, "file_a")
    await file_service.validate_file(file_b, "file_b")

    # Upload and save both files
    file_record_a = await file_service.upload_file(file_a)
    file_record_b = await file_service.upload_file(file_b)

    # Get basic file analysis
    analysis_a = comparison_service.analyze_file(file_record_a.file_path)
    analysis_b = comparison_service.analyze_file(file_record_b.file_path)

    # Perform comparison using the comparison service
    comparison_result = comparison_service.compare_files(
        file_record_a.file_path,
        file_record_b.file_path,
    )

    # Build simplified file info
    file_a_info = FileInfoSimple(
        file_uuid=file_record_a.uuid,
        original_filename=file_record_a.original_filename,
        total_lines=analysis_a["total_lines"],
        total_blocks=analysis_a["total_blocks"]
    )
    file_b_info = FileInfoSimple(
        file_uuid=file_record_b.uuid,
        original_filename=file_record_b.original_filename,
        total_lines=analysis_b["total_lines"],
        total_blocks=analysis_b["total_blocks"]
    )

    # Build comparison summary
    summary = ComparisonSummary(
        is_identical=comparison_result.is_identical,
        structural_similarity=comparison_result.structural_similarity,
        total_blocks_a=comparison_result.total_blocks_a,
        total_blocks_b=comparison_result.total_blocks_b,
        blocks_added=comparison_result.blocks_added,
        blocks_deleted=comparison_result.blocks_deleted,
        blocks_modified=comparison_result.blocks_modified,
        blocks_moved=comparison_result.blocks_moved,
        blocks_moved_modified=comparison_result.blocks_moved_modified,
        blocks_unchanged=comparison_result.blocks_unchanged,
    )

    def convert_statement_diff(sd) -> StatementDiffSchema:
        """Recursively convert StatementDiff to StatementDiffSchema."""
        return StatementDiffSchema(
            change_type=sd.change_type.value,
            code=sd.code,
            node_type=sd.node_type,
            file_a_line=sd.file_a_line,
            file_a_index=sd.file_a_index,
            file_b_line=sd.file_b_line,
            file_b_index=sd.file_b_index,
            description=sd.description,
            old_code=sd.old_code,
            similarity_score=sd.similarity_score,
            child_diffs=[
                convert_statement_diff(child)
                for child in sd.child_diffs
                if child.change_type != StatementChangeType.UNCHANGED
            ],
            is_container=sd.is_container,
            branch_label=sd.branch_label,
        )

    # Convert diffs to schema (exclude unchanged)
    differences = [
        BlockDiffSchema(
            change_type=diff.change_type.value,
            block_type=diff.block_type.value,
            identifier=diff.identifier,
            file_a_start_line=diff.file_a_start_line,
            file_a_end_line=diff.file_a_end_line,
            file_a_code=diff.file_a_code,
            file_b_start_line=diff.file_b_start_line,
            file_b_end_line=diff.file_b_end_line,
            file_b_code=diff.file_b_code,
            similarity_score=diff.similarity_score,
            # Statement-level diffs (exclude unchanged) with recursive child_diffs
            statement_diffs=[
                convert_statement_diff(sd)
                for sd in diff.statement_diffs
                if sd.change_type != StatementChangeType.UNCHANGED
            ],
            description=diff.description,
            modifiers=diff.modifiers,
        )
        for diff in comparison_result.diffs
        if diff.change_type != ChangeType.UNCHANGED
    ]

    # Generate message based on comparison result
    if comparison_result.error:
        message = f"Comparison completed with errors: {comparison_result.error}"
    elif comparison_result.is_identical:
        message = "Files are identical - no differences found"
    else:
        changes = []
        if comparison_result.blocks_added:
            changes.append(f"{comparison_result.blocks_added} added")
        if comparison_result.blocks_deleted:
            changes.append(f"{comparison_result.blocks_deleted} deleted")
        if comparison_result.blocks_modified:
            changes.append(f"{comparison_result.blocks_modified} modified")
        if comparison_result.blocks_moved:
            changes.append(f"{comparison_result.blocks_moved} moved")
        if comparison_result.blocks_moved_modified:
            changes.append(f"{comparison_result.blocks_moved_modified} moved+modified")

        similarity_pct = comparison_result.structural_similarity * 100
        message = (
            f"Comparison complete: {', '.join(changes)}. "
            f"Structural similarity: {similarity_pct:.1f}%"
        )

    return CompareFilesResponse(
        message=message,
        file_a=file_a_info,
        file_b=file_b_info,
        summary=summary,
        differences=differences,
    )