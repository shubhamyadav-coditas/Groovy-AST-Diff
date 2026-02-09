# Pipeline Step Signal

## Overview

The **Pipeline Step** signal detects the addition or modification of CI/CD pipeline steps in Groovy code. These are commands and blocks commonly used in Jenkins pipelines, Gradle builds, and other automation systems. Changes to pipeline steps can have a wide blast radius, affecting build processes, deployments, and overall system reliability.

## Why It Matters for Change Complexity

Pipeline steps are critical for several reasons:

1. **Wide Blast Radius**: A single pipeline change can affect all builds, deployments, and releases across an organization.

2. **Infrastructure Impact**: Pipeline steps often interact with external systems (servers, cloud services, databases), making errors potentially costly.

3. **Security Sensitivity**: Steps like `withCredentials`, `sh`, and deployment commands handle sensitive operations that require careful review.

4. **Execution Environment**: Changes to `node`, `agent`, or execution context can cause unexpected failures in different environments.

5. **Flow Control**: Steps like `input`, `milestone`, and `lock` change the execution flow and can introduce deadlocks or approval bottlenecks.

6. **Parallel Execution**: Adding `parallel` blocks changes the execution model and can introduce race conditions.

## Signal Score

- **Default Score**: +12 points per occurrence
- This is an additive score applied to the block's overall change complexity

## Detection Patterns

The detector identifies the following pipeline-related method names and patterns:

### Shell Execution Steps

| Step | Description |
|------|-------------|
| `sh` | Execute shell command (Unix/Linux) |
| `bat` | Execute batch command (Windows) |
| `powershell` | Execute PowerShell command |

### Jenkins Pipeline Core

| Step | Description |
|------|-------------|
| `pipeline` | Declarative pipeline block |
| `node` | Allocate an executor (scripted pipeline) |
| `stage` | Define a pipeline stage |
| `steps` | Container for step definitions |
| `parallel` | Execute stages/steps in parallel |

### Jenkins Steps

| Step | Description |
|------|-------------|
| `script` | Execute scripted pipeline code |
| `checkout` | Check out source code |
| `git` | Clone a Git repository |

### Jenkins Wrappers

| Step | Description |
|------|-------------|
| `withCredentials` | Inject credentials securely |
| `withEnv` | Set environment variables |
| `timeout` | Set execution timeout |

### Jenkins Publishers

| Step | Description |
|------|-------------|
| `archiveArtifacts` | Archive build artifacts |
| `publishHTML` | Publish HTML reports |
| `junit` | Publish JUnit test results |

### Jenkins Flow Control

| Step | Description |
|------|-------------|
| `input` | Wait for user input/approval |
| `milestone` | Define a milestone for build ordering |
| `lock` | Acquire a lock for exclusive access |

### Status Steps

| Step | Description |
|------|-------------|
| `echo` | Print a message |
| `error` | Fail the build with an error |
| `unstable` | Mark build as unstable |

### Common CI/CD Steps

| Step | Description |
|------|-------------|
| `build` | Trigger another job |
| `deploy` | Deployment step |
| `test` | Test execution step |

## AST Detection Logic

The detector uses two patterns to identify pipeline steps:

### Pattern 1: Method/Function Calls

```groovy
// AST node types: method_call, function_call, juxt_function_call
sh 'mvn clean install'
checkout scm
withCredentials([...]) { ... }
```

### Pattern 2: Identifier + Block (Groovy DSL Pattern)

```groovy
// AST pattern: identifier followed by closure/block
pipeline {
    // ...
}

node('linux') {
    // ...
}

parallel {
    // ...
}
```

## Implementation

```python
class PipelineStepSignalDetector(BaseSignalDetector):
    PIPELINE_METHODS = {
        # Shell execution
        'sh', 'bat', 'powershell',
        # Jenkins pipeline core
        'parallel', 'node', 'stage', 'pipeline',
        # Jenkins steps
        'steps', 'script', 'checkout', 'git',
        # Jenkins wrappers
        'withCredentials', 'withEnv', 'timeout',
        # Jenkins publishers
        'archiveArtifacts', 'publishHTML', 'junit',
        # Jenkins flow control
        'input', 'milestone', 'lock',
        # Jenkins status
        'echo', 'error', 'unstable',
        # Common CI/CD steps
        'build', 'deploy', 'test',
    }
    
    def detect(self, root_node, source_bytes, **kwargs):
        count = 0
        
        def traverse(node):
            nonlocal count
            
            # Check for method/function calls
            if node.type in ['method_call', 'function_call', 'juxt_function_call']:
                method_name = self._extract_method_name(node, source_bytes)
                if method_name and method_name.lower() in [p.lower() for p in self.PIPELINE_METHODS]:
                    count += 1
            
            # Check for identifier followed by block (Groovy DSL pattern)
            if node.type == 'identifier':
                text = self._get_node_text(node, source_bytes)
                if text.lower() in [p.lower() for p in self.PIPELINE_METHODS]:
                    if node.next_sibling and node.next_sibling.type in ['closure', 'block', 'argument_list']:
                        count += 1
            
            for child in node.children:
                traverse(child)
        
        traverse(root_node)
        return count
```

## Code Examples

### Example 1: Single sh Step Addition

```groovy
// BEFORE: Only echo
steps {
    echo 'Building...'
}

// AFTER: Added sh step (Signal Count: +1)
steps {
    echo 'Building...'
    sh 'mvn clean install'
}
```

### Example 2: Parallel Execution Block

```groovy
// BEFORE: Sequential tests
stage('Test') {
    steps {
        echo 'Testing...'
    }
}

// AFTER: Parallel tests (Signal Count: +3 - parallel, 2x sh)
stage('Test') {
    parallel {
        stage('Unit Tests') {
            steps {
                sh 'npm run test:unit'
            }
        }
        stage('Integration Tests') {
            steps {
                sh 'npm run test:integration'
            }
        }
    }
}
```

### Example 3: withCredentials Wrapper

```groovy
// BEFORE: Direct deployment
sh 'scp -r dist/ user@server:/var/www/'

// AFTER: Secure deployment (Signal Count: +1 for withCredentials)
withCredentials([usernamePassword(
    credentialsId: 'deploy-creds',
    usernameVariable: 'USER',
    passwordVariable: 'PASS'
)]) {
    sh 'scp -r dist/ ${USER}@server:/var/www/'
}
```

### Example 4: Node Block (Scripted Pipeline)

```groovy
// BEFORE: Simple script
println "Starting build..."

// AFTER: Scripted pipeline (Signal Count: +4 - node, 2x stage, sh)
node('linux') {
    stage('Build') {
        sh 'make build'
    }
    stage('Test') {
        sh 'make test'
    }
}
```

## Impact on Change Complexity

When pipeline steps are introduced or modified in a changed block:

1. **Added Blocks**: The signal count from the target code is added to the block's signal score
2. **Modified Blocks**: The signal count from the target code is analyzed
3. **Deleted Blocks**: The signal count from the source code is analyzed
4. **Moved Blocks**: Signal count from target is analyzed

### Score Calculation

```
Signal Score = Count × Default Score
             = Count × 12.0
```

For example, if a block adds 3 pipeline steps:
- Signal Count: 3
- Signal Score: 3 × 12.0 = 36.0 points added to complexity

## Best Practices

1. **Review Pipeline Changes Carefully**: Changes to pipeline steps should be reviewed with extra scrutiny.

2. **Test in Non-Production First**: Always test pipeline changes in staging or development environments.

3. **Use Timeout Wrappers**: Wrap long-running steps with `timeout` to prevent hung builds.

4. **Secure Credentials**: Always use `withCredentials` instead of hardcoding sensitive values.

5. **Document Pipeline Changes**: Add comments explaining why pipeline steps were added or modified.

6. **Consider Rollback Plans**: Have a plan to revert pipeline changes if they cause issues.

## Related Signals

- **New Dependency**: Pipeline changes often introduce new plugin dependencies
- **MetaClass Usage**: Dynamic code in pipelines adds complexity
- **Closure Mutable Capture**: Closures in pipelines may capture external state

## See Also

- [Jenkins Pipeline Syntax](https://www.jenkins.io/doc/book/pipeline/syntax/)
- [Jenkins Pipeline Steps Reference](https://www.jenkins.io/doc/pipeline/steps/)
- [Scripted vs Declarative Pipelines](https://www.jenkins.io/doc/book/pipeline/)
