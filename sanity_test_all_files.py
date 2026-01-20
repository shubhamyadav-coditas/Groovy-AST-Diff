#!/usr/bin/env python3
"""
Simple test script that uses curl to test all file pairs.
No external dependencies required.
"""

import os
import sys
import subprocess
import json
from pathlib import Path
from typing import List, Tuple

def find_test_file_pairs(test_samples_dir: str = "test-samples") -> List[Tuple[str, str]]:
    """Find all test file pairs in the test-samples directory."""
    pairs = []
    test_samples_path = Path(test_samples_dir)
    
    if not test_samples_path.exists():
        print(f"❌ Test samples directory not found: {test_samples_dir}")
        return pairs
    
    # Pattern 1: *_before.groovy and *_after.groovy
    for before_file in test_samples_path.rglob("*_before.groovy"):
        after_file = before_file.parent / before_file.name.replace("_before.groovy", "_after.groovy")
        if after_file.exists():
            pairs.append((str(before_file), str(after_file)))
    
    # Pattern 2: source.groovy and target.groovy in same directory
    for source_file in test_samples_path.rglob("source.groovy"):
        target_file = source_file.parent / "target.groovy"
        if target_file.exists():
            pairs.append((str(source_file), str(target_file)))
    
    return sorted(pairs)

def test_api_health() -> bool:
    """Check if the API is running."""
    try:
        result = subprocess.run([
            'curl', '-s', '-o', '/dev/null', '-w', '%{http_code}', 
            'http://localhost:8000/docs'
        ], capture_output=True, text=True, timeout=5)
        return result.returncode == 0 and result.stdout.strip() == '200'
    except Exception:
        return False

def test_file_pair(file_a: str, file_b: str) -> dict:
    """Test a single file pair using curl."""
    try:
        # Use curl to test the API
        cmd = [
            'curl', '-s', '--location', 'http://localhost:8000/api/v1/compare',
            '--form', f'file_a=@"{file_a}"',
            '--form', f'file_b=@"{file_b}"'
        ]
        
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
        
        if result.returncode != 0:
            return {
                'success': False, 
                'error': f'Curl failed: {result.stderr}',
                'file_pair': (file_a, file_b)
            }
        
        # Try to parse JSON response
        try:
            response_data = json.loads(result.stdout)
            
            # Check for parsing errors
            if 'error' in response_data:
                return {
                    'success': False,
                    'error': f'API error: {response_data["error"]}',
                    'file_pair': (file_a, file_b)
                }
            
            # Check message for parsing-related errors
            message = response_data.get("message", "").lower()
            error_indicators = ["parsing error", "syntax error", "failed to parse", "invalid syntax"]
            
            if any(indicator in message for indicator in error_indicators):
                return {
                    'success': False,
                    'error': f'Parsing error detected: {response_data.get("message", "")}',
                    'file_pair': (file_a, file_b)
                }
            
            return {
                'success': True,
                'error': None,
                'file_pair': (file_a, file_b),
                'response': response_data
            }
            
        except json.JSONDecodeError as e:
            return {
                'success': False,
                'error': f'Invalid JSON response: {e}',
                'file_pair': (file_a, file_b)
            }
            
    except Exception as e:
        return {
            'success': False,
            'error': f'Exception: {str(e)}',
            'file_pair': (file_a, file_b)
        }

def main():
    print("🚀 Starting simple test suite...")
    
    # Check API health
    if not test_api_health():
        print("❌ API is not running or not healthy. Please start the server first.")
        print("   Run: uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload")
        return False
    
    print("✅ API is healthy")
    
    # Find all test file pairs
    file_pairs = find_test_file_pairs()
    print(f"📁 Found {len(file_pairs)} test file pairs")
    
    if not file_pairs:
        print("⚠️  No test file pairs found")
        return True
    
    # Run tests
    results = []
    passed = 0
    failed = 0
    
    print("\\n🧪 Running tests...")
    for i, (file_a, file_b) in enumerate(file_pairs, 1):
        print(f"  [{i:3d}/{len(file_pairs)}] Testing {Path(file_a).name} vs {Path(file_b).name}...", end=" ")
        
        result = test_file_pair(file_a, file_b)
        results.append(result)
        
        if result['success']:
            print("✅")
            passed += 1
        else:
            print("❌")
            failed += 1
            print(f"      Error: {result['error']}")
    
    # Summary
    print(f"\\n📊 Test Results Summary:")
    print(f"   Total: {len(file_pairs)}")
    print(f"   Passed: {passed} ✅")
    print(f"   Failed: {failed} ❌")
    print(f"   Success Rate: {(passed/len(file_pairs)*100):.1f}%")
    
    if failed > 0:
        print(f"\\n❌ Failed Tests:")
        for result in results:
            if not result['success']:
                file_a, file_b = result['file_pair']
                print(f"   • {Path(file_a).name} vs {Path(file_b).name}: {result['error']}")
    
    return failed == 0

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)