"""Change complexity analysis endpoints."""

from typing import Annotated, Optional

from fastapi import APIRouter, File, Form, UploadFile, HTTPException, status
from pydantic import ValidationError

from app.core.dependencies import FileServiceDep, ChangeComplexityServiceDep
from app.schemas.file import FileInfoSimple
from app.schemas.change_complexity import (
    ChangeComplexityResponse,
    ChangeComplexityRequest,
    ComplexityWeights
)

router = APIRouter(prefix="/change-complexity", tags=["Change Complexity Analysis"])


@router.post(
    "",
    response_model=ChangeComplexityResponse,
    status_code=status.HTTP_200_OK,
    summary="Analyze change complexity between two Groovy files",
    description=(
        "Upload two Groovy files and analyze the complexity of changes between them. "
        "Calculates complexity metrics for changed blocks only and provides an overall "
        "change complexity score with risk level assessment."
    ),
)
async def analyze_change_complexity(
    file_service: FileServiceDep,
    complexity_service: ChangeComplexityServiceDep,
    file_a: Annotated[UploadFile, File(description="Original Groovy file (base version)")],
    file_b: Annotated[UploadFile, File(description="Modified Groovy file (new version)")],
    # Optional form parameters for custom weights
    weight_cyclomatic: Annotated[Optional[float], Form(description="Weight for cyclomatic complexity (0.0-1.0)")] = None,
    weight_dynamic_typing: Annotated[Optional[float], Form(description="Weight for dynamic typing (0.0-1.0)")] = None,
    weight_closures: Annotated[Optional[float], Form(description="Weight for closures (0.0-1.0)")] = None,
    weight_imports: Annotated[Optional[float], Form(description="Weight for imports (0.0-1.0)")] = None,
    weight_loc: Annotated[Optional[float], Form(description="Weight for lines of code (0.0-1.0)")] = None,
    # Analysis options
    include_unchanged_blocks: Annotated[bool, Form(description="Include unchanged blocks in analysis")] = False,
    normalize_to_100: Annotated[bool, Form(description="Normalize final score to 0-100 scale")] = True,
) -> ChangeComplexityResponse:
    """
    Analyze change complexity between two Groovy files using delta-based calculation.

    This endpoint performs comprehensive change complexity analysis:
    
    1. **File Validation**: Ensures both files are valid Groovy files
    2. **AST Diff Analysis**: Uses existing AST comparison to identify changes
    3. **Delta-Based Complexity Calculation**: Compares before vs after states:
       - Cyclomatic Complexity delta (decision points added/removed)
       - Dynamic Typing & Meta-Programming pattern changes
       - Closure complexity and nesting changes
       - Import/dependency count changes
       - Lines of code delta in changed blocks
    4. **Weighted Delta Aggregation**: Applies metric weights to deltas
    5. **Block Change Complexity**: Sums weighted deltas for each block
    6. **Overall Score**: Sums all block change complexities
    7. **Risk Assessment**: Classifies risk level (Low/Medium/High/Very High)

    ### Delta-Based Approach
    
    The new calculation method compares the complexity metrics before and after changes:
    - **Positive deltas**: Indicate increased complexity (added logic, imports, etc.)
    - **Negative deltas**: Indicate reduced complexity (removed code, simplified logic)
    - **Zero deltas**: No complexity change (pure moves, formatting changes)

    ### Custom Weights
    
    You can customize the importance of different complexity metrics:
    - **Cyclomatic Complexity** (default 35%): Most critical for logic complexity
    - **Dynamic Typing** (default 25%): Groovy-specific dynamic features
    - **Closures** (default 20%): Closure complexity and nesting
    - **Imports** (default 15%): Dependencies and coupling
    - **Lines of Code** (default 5%): Size factor
    
    Weights must sum to approximately 1.0 (±0.05 tolerance).

    ### Response
    
    Returns detailed analysis including:
    - Overall change complexity score and risk level
    - Per-block delta-based complexity breakdown with before/after metrics
    - Metric deltas and weighted contributions
    - Summary statistics and change distribution
    - File information and line counts

    Args:
        file_a: The original Groovy file (base version)
        file_b: The modified Groovy file (new version)
        weight_*: Optional custom weights for complexity metrics
        include_unchanged_blocks: Whether to analyze unchanged blocks
        normalize_to_100: Whether to normalize final score to 0-100 scale

    Returns:
        ChangeComplexityResponse with complete analysis results

    Raises:
        HTTPException 400: If files are invalid or weights are malformed
        HTTPException 422: If analysis fails due to parsing errors
    """
    # Validate both files BEFORE uploading (fail fast)
    await file_service.validate_file(file_a, "file_a")
    await file_service.validate_file(file_b, "file_b")

    # Build custom weights if any are provided
    custom_weights = None
    if any([weight_cyclomatic, weight_dynamic_typing, weight_closures, weight_imports, weight_loc]):
        try:
            # Use provided weights or defaults
            custom_weights = ComplexityWeights(
                weight_cyclomatic=weight_cyclomatic or 0.35,
                weight_dynamic_typing=weight_dynamic_typing or 0.25,
                weight_closures=weight_closures or 0.20,
                weight_imports=weight_imports or 0.15,
                weight_loc=weight_loc or 0.05
            )
        except ValidationError as e:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Invalid complexity weights: {e.errors()}"
            )

    # Validate the weights if provided
    if custom_weights:
        validation_error = complexity_service.validate_complexity_request(custom_weights)
        if validation_error:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=validation_error
            )

    # Upload and save both files
    file_record_a = await file_service.upload_file(file_a)
    file_record_b = await file_service.upload_file(file_b)
    
    # Check for large files and warn user
    size_warning = complexity_service.check_file_size_limits(
        file_record_a.file_path,
        file_record_b.file_path
    )

    # Get basic file analysis for response
    # We'll use a simple line count since we need the GroovyComparisonService for block analysis
    with open(file_record_a.file_path, 'r', encoding='utf-8') as f:
        lines_a = len(f.readlines())
    with open(file_record_b.file_path, 'r', encoding='utf-8') as f:
        lines_b = len(f.readlines())
    
    analysis_a = {"total_lines": lines_a, "total_blocks": 0}  # Will be updated after AST analysis
    analysis_b = {"total_lines": lines_b, "total_blocks": 0}

    # Build file info for response
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

    try:
        # Perform change complexity analysis
        (comparison_result, block_results, overall_score, 
         risk_level, summary) = complexity_service.analyze_change_complexity(
            file_record_a.file_path,
            file_record_b.file_path,
            custom_weights
        )

        # Create the response
        response = complexity_service.create_complexity_response(
            comparison_result,
            block_results,
            overall_score,
            risk_level,
            summary,
            file_a_info,
            file_b_info
        )
        
        # Add size warning if present
        if size_warning:
            if response.warnings:
                response.warnings += f" {size_warning}"
            else:
                response.warnings = size_warning

        return response

    except Exception as e:
        # Handle analysis errors
        error_msg = str(e)
        if "parsing" in error_msg.lower() or "ast" in error_msg.lower():
            raise HTTPException(
                status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
                detail=f"Failed to parse Groovy files: {error_msg}"
            )
        else:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail=f"Analysis failed: {error_msg}"
            )


@router.get(
    "/weights/defaults",
    summary="Get default complexity weights",
    description="Returns the default weights used for complexity metrics calculation.",
)
async def get_default_weights() -> ComplexityWeights:
    """
    Get the default complexity weights.
    
    Returns the default weights used when no custom weights are provided:
    - Cyclomatic Complexity: 35%
    - Dynamic Typing: 25%
    - Closures: 20%
    - Imports: 15%
    - Lines of Code: 5%
    
    These weights are based on the Groovy complexity framework defined
    in the file_complexity_score.md documentation.
    """
    return ComplexityWeights()


@router.post(
    "/weights/validate",
    summary="Validate custom complexity weights",
    description="Validate that custom weights are properly formatted and sum to 1.0.",
)
async def validate_weights(weights: ComplexityWeights) -> dict:
    """
    Validate custom complexity weights.
    
    Checks that:
    - All weights are between 0.0 and 1.0
    - Weights sum to approximately 1.0 (±0.05 tolerance)
    - All required weights are provided
    
    Args:
        weights: ComplexityWeights object to validate
        
    Returns:
        Validation result with status and converted weights
    """
    try:
        weights_dict = weights.to_dict()
        total = sum(weights_dict.values())
        
        return {
            "valid": True,
            "total_weight": round(total, 3),
            "weights": weights_dict,
            "message": "Weights are valid"
        }
    except ValidationError as e:
        return {
            "valid": False,
            "errors": e.errors(),
            "message": "Invalid weights provided"
        }

