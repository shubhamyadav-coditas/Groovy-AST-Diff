# Groovy AST Diff API - FastAPI Application

A sophisticated FastAPI application for comparing Groovy source files using Abstract Syntax Tree (AST) analysis, implementing repository pattern and dependency injection.

## Features

- **FastAPI** - Modern, fast web framework for building APIs
- **Poetry** - Dependency management and packaging
- **SQLAlchemy** - Async ORM with SQLite (easily switchable to PostgreSQL)
- **Repository Pattern** - Clean separation of data access logic
- **Dependency Injection** - Loosely coupled components
- **Pydantic v2** - Data validation and serialization
- **File Upload** - Accept Groovy files for comparison
- **Advanced AST Comparison** - Block-level and statement-level diff analysis
- **Tree-sitter Parser** - Robust Groovy syntax parsing

## Project Structure

```
app/
├── __init__.py
├── main.py                 # Application entry point
├── api/                    # API layer
│   ├── __init__.py
│   └── v1/
│       ├── __init__.py
│       ├── router.py       # API router
│       └── endpoints/
│           ├── __init__.py
│           ├── file_upload.py      # File upload endpoints
│           └── groovy_comparison.py # Groovy comparison endpoints
├── core/                   # Core configuration
│   ├── __init__.py
│   ├── config.py          # Application settings
│   └── dependencies.py    # Dependency injection
├── db/                    # Database layer
│   ├── __init__.py
│   ├── base.py           # SQLAlchemy base
│   └── database.py       # Database connection
├── domain/               # Domain logic (relocated from src/)
│   ├── __init__.py
│   ├── groovy_ast_diff.py      # Main AST comparison logic
│   ├── groovy_domain.py        # Domain models and data classes
│   └── groovy_recursive_parser.py # Recursive AST parsing
├── models/               # SQLAlchemy models
│   ├── __init__.py
│   └── file.py          # File model
├── repositories/         # Repository layer
│   ├── __init__.py
│   ├── base.py          # Base repository
│   └── file_repository.py  # File repository
├── schemas/              # Pydantic schemas
│   ├── __init__.py
│   ├── file.py          # File schemas
│   └── groovy_comparison.py # Groovy comparison schemas
├── services/             # Service layer
│   ├── __init__.py
│   ├── file_service.py         # File service
│   └── groovy_comparison_service.py # Groovy comparison service
└── types/                # Type definitions
    ├── __init__.py
    └── groovy_types.py   # Groovy AST types and enums
```

## Installation

### Prerequisites

- Python 3.11+
- Poetry

### Setup

1. **Install Poetry** (if not already installed):
   ```bash
   curl -sSL https://install.python-poetry.org | python3 -
   ```

2. **Install dependencies**:
   ```bash
   poetry install
   ```

3. **Activate virtual environment**:
   ```bash
   poetry env activate
   source <path_from above command>
   ```

4. **Build the Tree-sitter Groovy parser**:
   ```bash
   python setup_parser.py
   ```

5. **Create environment file** (optional):
   ```bash
   cp .env.example .env
   # Edit .env with your settings
   ```

## Database Setup

### Using Alembic Migrations (Recommended for Production)

1. **Create a new migration** (after modifying models):
   ```bash
   poetry run alembic revision --autogenerate -m "Description of changes"
   ```

2. **Apply migrations**:
   ```bash
   poetry run alembic upgrade head
   ```

3. **Rollback migration**:
   ```bash
   poetry run alembic downgrade -1
   ```

4. **View migration history**:
   ```bash
   poetry run alembic history
   ```

### Development Mode

For quick development, the app auto-creates tables on startup using `Base.metadata.create_all()`. 
This is handled in `app/db/database.py:init_db()`.

## Running the Application

### Development Server

```bash
poetry run uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Or with Poetry shell activated:

```bash
uvicorn app.main:app --reload
```

### Access the API

- **API Documentation (Swagger)**: http://localhost:8000/docs
- **Alternative Docs (ReDoc)**: http://localhost:8000/redoc
- **Health Check**: http://localhost:8000/health

## API Endpoints

### File Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/files/upload` | Upload a Groovy file |
| `GET` | `/api/v1/files/` | List all files |
| `GET` | `/api/v1/files/{uuid}` | Get file by UUID |
| `DELETE` | `/api/v1/files/{uuid}` | Delete file |

### Groovy AST Comparison

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/compare` | Compare two Groovy files |

### Example: Upload a File

```bash
curl -X POST "http://localhost:8000/api/v1/files/upload" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/your/file.groovy"
```

### Example: Compare Two Files

```bash
curl -X POST "http://localhost:8000/api/v1/compare" \
  -H "Content-Type: multipart/form-data" \
  -F "file_a=@/path/to/original.groovy" \
  -F "file_b=@/path/to/modified.groovy"
```

## AST Comparison Features

### Change Types Detected

- **ADDED** - New blocks in target file
- **DELETED** - Removed blocks from source file
- **MODIFIED** - Same identifier, different content
- **MOVED** - Same content, different position
- **MOVED_MODIFIED** - Different position AND content changed
- **UNCHANGED** - Identical blocks

### Block Types Analyzed

- **Classes** - class, interface, trait, enum, annotation
- **Methods** - method, constructor, static method, abstract method
- **Fields** - field, property, static field
- **Closures** - Groovy closures
- **Script Elements** - top-level methods and variables
- **Imports/Packages** - import and package declarations
- **Expressions** - function calls, assignments, binary operations
- **Statements** - control flow, loops, comments

### Comparison Process

1. **File Validation** - Ensures files are valid Groovy syntax
2. **AST Parsing** - Uses Tree-sitter for robust parsing
3. **Block Extraction** - Identifies top-level code blocks
4. **Multi-Phase Matching** - 4-phase strategy for accurate matching
5. **Similarity Analysis** - Sørensen-Dice coefficient scoring
6. **Statement-Level Diffs** - Detailed line-by-line comparison for modified blocks

## Development Tools

### Code Quality

The project includes several linting and formatting tools:

```bash
# Format code with Black
poetry run black app tests

# Sort imports with isort
poetry run isort app tests

# Lint with Ruff
poetry run ruff check app tests

# Type checking with mypy
poetry run mypy app

# Remove unused imports with autoflake
poetry run autoflake --in-place --recursive app tests
```

### Run All Linters

```bash
# Format and lint
poetry run black app tests && \
poetry run isort app tests && \
poetry run ruff check --fix app tests && \
poetry run mypy app
```

### Testing

```bash
# Run tests
poetry run pytest

# Run with coverage
poetry run pytest --cov=app
```

## Configuration

Environment variables can be set in `.env` file:

| Variable | Default | Description |
|----------|---------|-------------|
| `APP_NAME` | Groovy AST Diff API | Application name |
| `DEBUG` | True | Enable debug mode |
| `DATABASE_URL` | sqlite+aiosqlite:///./groovy_ast_diff.db | Database connection URL |
| `UPLOAD_DIR` | uploads | Directory for uploaded files |
| `MAX_FILE_SIZE` | 10485760 | Maximum file size (10MB) |
| `ALLOW_ANY_FILE` | True | Accept any file type |

## Architecture

### Repository Pattern

The repository pattern abstracts data access, making the code more testable and maintainable:

```python
# Base repository provides common CRUD operations
class BaseRepository(Generic[ModelType]):
    async def create(self, obj_in: dict) -> ModelType
    async def get_by_id(self, id: int) -> ModelType | None
    async def get_all(self, skip: int, limit: int) -> list[ModelType]
    async def update(self, db_obj: ModelType, obj_in: dict) -> ModelType
    async def delete(self, db_obj: ModelType) -> None

# Specific repositories extend the base
class FileRepository(BaseRepository[FileRecord]):
    async def get_by_uuid(self, uuid: str) -> FileRecord | None
    async def get_groovy_files(self, ...) -> list[FileRecord]
```

### Dependency Injection

Dependencies are injected using FastAPI's `Depends`:

```python
# In dependencies.py
async def get_file_service(
    repository: Annotated[FileRepository, Depends(get_file_repository)],
) -> FileService:
    return FileService(repository)

# In endpoints
@router.post("/compare")
async def compare_files(
    file_service: FileServiceDep,  # Injected automatically
    comparison_service: GroovyComparisonServiceDep,
    file_a: UploadFile,
    file_b: UploadFile,
) -> CompareFilesResponse:
    ...
```

### Service Layer

Business logic is encapsulated in service classes:

```python
class GroovyComparisonService:
    def compare_files(self, file_a_path: str, file_b_path: str) -> ComparisonResult:
        """Compare two Groovy files using AST analysis."""
        return self.groovy_differ.compare_files(file_a_path, file_b_path)
```

## Example Response

```json
{
  "message": "Comparison complete: 2 added, 1 modified. Structural similarity: 75.0%",
  "file_a": {
    "file_uuid": "123e4567-e89b-12d3-a456-426614174000",
    "original_filename": "Calculator.groovy",
    "total_lines": 25,
    "total_blocks": 3
  },
  "file_b": {
    "file_uuid": "987fcdeb-51a2-43d7-b456-426614174001",
    "original_filename": "CalculatorModified.groovy",
    "total_lines": 35,
    "total_blocks": 5
  },
  "summary": {
    "is_identical": false,
    "structural_similarity": 0.75,
    "total_blocks_a": 3,
    "total_blocks_b": 5,
    "blocks_added": 2,
    "blocks_deleted": 0,
    "blocks_modified": 1,
    "blocks_moved": 0,
    "blocks_moved_modified": 0,
    "blocks_unchanged": 2
  },
  "differences": [
    {
      "change_type": "added",
      "block_type": "method",
      "identifier": "Calculator.subtract",
      "file_b_start_line": 8,
      "file_b_end_line": 10,
      "file_b_code": "def subtract(a, b) {\n    return a - b\n}",
      "similarity_score": 0.0,
      "statement_diffs": [],
      "description": "Method 'Calculator.subtract' was added",
      "modifiers": []
    }
  ]
}
```

## Parser Limitations

The Tree-sitter Groovy parser has some known limitations with certain Groovy syntax constructs:

### Unsupported Syntax
- **Type Casting with Arrays**: `as String[]` syntax is not supported
  ```groovy
  // ❌ This will cause parsing errors:
  def array = ["A", "B", "C"] as String[]
  
  // ✅ Use this instead:
  def array = ["A", "B", "C"]
  ```

### Workarounds
When encountering parsing errors:
1. Check for type casting syntax and remove unnecessary casts
2. Simplify complex generic expressions if possible
3. Use alternative Groovy syntax that achieves the same result

## Testing with Sample Files

The project includes test samples in the `test_samples/` directory:

```bash
# Test different change types using the CLI tool
python groovy_ast_diff.py test_samples/added_before.groovy test_samples/added_after.groovy
python groovy_ast_diff.py test_samples/modified_before.groovy test_samples/modified_after.groovy
```

## Contributing

This implementation follows clean architecture principles and can be extended with:
- Additional Groovy constructs support
- Enhanced statement-level comparison
- Cross-file dependency analysis
- IDE integration capabilities
- Performance optimizations

## License

MIT License

---

**🎯 Architecture Achievement**: This Groovy AST Diff API successfully replicates the service-repository pattern and folder structure from the AST_DEMO_fastapi project while maintaining the sophisticated Groovy AST comparison capabilities from the original implementation.