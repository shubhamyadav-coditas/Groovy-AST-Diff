# New Infrastructure / Plugin Dependency Signal

## Overview

The **New Infrastructure / Plugin Dependency** signal detects when new external infrastructure dependencies, plugins, or frameworks are introduced in a code change. These additions significantly increase the complexity and risk of a change because they introduce new external coupling, deployment requirements, and potential failure points.

## What This Signal Represents

This signal identifies imports that match known infrastructure patterns including:

- **CI/CD Tools**: Jenkins, Hudson, Gradle, Maven
- **Container/Orchestration**: Kubernetes, Docker
- **Cloud Providers**: AWS, Azure, GCP, Google Cloud
- **Infrastructure as Code**: Terraform, Ansible, Puppet, Chef
- **Frameworks**: Spring, Hibernate, Grails
- **Messaging Systems**: Kafka, RabbitMQ, ActiveMQ
- **Databases/Caching**: MongoDB, Redis, Elasticsearch

## Why This Signal Matters

### 1. Increased External Coupling

New infrastructure dependencies create tight coupling to external systems. If the external system changes its API, requires updates, or becomes unavailable, your application is directly impacted.

### 2. Deployment Complexity

Each new infrastructure dependency adds:
- Configuration requirements
- Environment variables
- Connection strings/credentials
- Network access requirements
- Version compatibility concerns

### 3. Testing Challenges

Infrastructure dependencies often require:
- Integration test environments
- Mocking/stubbing for unit tests
- Container setups for local development
- CI/CD pipeline modifications

### 4. Operational Overhead

New dependencies mean:
- More systems to monitor
- Additional failure points
- Security surface expansion
- License/compliance considerations

### 5. Learning Curve

Team members need to understand:
- How to configure the dependency
- Best practices for usage
- Troubleshooting procedures
- Security implications

## Detection Logic

### Algorithm

```
1. Parse both source and target files
2. Extract all import statements from each file
3. Identify NEW imports (present in target but not in source)
4. For each new import:
   a. Convert to lowercase for pattern matching
   b. Check against known infrastructure patterns
   c. If matched, increment count
5. Return total count of new infrastructure dependencies
```

### AST Node Types

The detector looks for these import node types:

| Node Type | Description |
|-----------|-------------|
| `groovy_import` | Standard Groovy import statement |
| `import_declaration` | Java-style import declaration |
| `import_statement` | Generic import statement |

### Pattern Matching

The detector uses substring matching against known infrastructure patterns:

```groovy
INFRA_IMPORT_PATTERNS = [
    // CI/CD tools
    'jenkins', 'hudson', 'gradle', 'maven',
    
    // Container/orchestration
    'kubernetes', 'docker',
    
    // Cloud providers
    'aws', 'azure', 'gcp', 'google.cloud',
    
    // Infrastructure as code
    'terraform', 'ansible', 'puppet', 'chef',
    
    // Frameworks
    'spring', 'hibernate', 'grails',
    
    // Messaging
    'kafka', 'rabbitmq', 'activemq',
    
    // Databases
    'mongodb', 'redis', 'elasticsearch',
]
```

## Dependency Categories

### 1. CI/CD Tools

**Examples:**
```groovy
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.gradle.api.Plugin
import org.apache.maven.plugin.AbstractMojo
```

**Risk**: Ties application code to specific build/deployment infrastructure.

### 2. Container & Orchestration

**Examples:**
```groovy
import io.kubernetes.client.openapi.ApiClient
import com.github.dockerjava.api.DockerClient
```

**Risk**: Requires container runtime environment and orchestration knowledge.

### 3. Cloud Provider SDKs

**Examples:**
```groovy
import com.amazonaws.services.s3.AmazonS3
import com.microsoft.azure.storage.CloudStorageAccount
import com.google.cloud.storage.Storage
```

**Risk**: Cloud vendor lock-in, credential management, network access.

### 4. Frameworks

**Examples:**
```groovy
import org.springframework.stereotype.Service
import org.hibernate.Session
import grails.gorm.transactions.Transactional
```

**Risk**: Framework version dependencies, configuration complexity.

### 5. Messaging Systems

**Examples:**
```groovy
import org.apache.kafka.clients.producer.KafkaProducer
import com.rabbitmq.client.Connection
```

**Risk**: Message broker availability, serialization compatibility.

### 6. Database Clients

**Examples:**
```groovy
import com.mongodb.client.MongoClient
import redis.clients.jedis.Jedis
import org.elasticsearch.client.RestHighLevelClient
```

**Risk**: Data persistence, connection pooling, schema management.

## Code Examples

### Example 1: Adding AWS S3 Dependency

**Before:**
```groovy
class FileStorage {
    def saveFile(String name, byte[] content) {
        def file = new File("/tmp/${name}")
        file.bytes = content
    }
}
```

**After:**
```groovy
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder

class FileStorage {
    private AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient()
    
    def saveFile(String name, byte[] content) {
        s3Client.putObject("bucket", name, new ByteArrayInputStream(content), null)
    }
}
```

**Signal Count**: +2 (two AWS imports)

### Example 2: Adding Spring Framework

**Before:**
```groovy
class UserService {
    def getUser(Long id) { /* ... */ }
}
```

**After:**
```groovy
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired

@Service
class UserService {
    @Autowired
    UserRepository repository
    
    def getUser(Long id) {
        return repository.findById(id)
    }
}
```

**Signal Count**: +2 (two Spring imports)

### Example 3: No Infrastructure Dependency (Control)

**Before:**
```groovy
import groovy.json.JsonSlurper
```

**After:**
```groovy
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
```

**Signal Count**: 0 (Groovy standard library is not infrastructure)

## What is NOT Detected

### 1. Standard Library Imports

```groovy
import groovy.json.JsonSlurper      // Groovy standard
import java.util.List               // Java standard
import java.io.File                 // Java standard
```

### 2. Internal Package Imports

```groovy
import com.mycompany.utils.Helper   // Internal code
import app.services.UserService     // Application code
```

### 3. Existing Imports

Imports that already exist in the source file are not counted:

```groovy
// Source file:
import org.springframework.stereotype.Service

// Target file:
import org.springframework.stereotype.Service  // Not new, not counted
import org.springframework.web.bind.annotation.RestController  // NEW, counted
```

## Impact on Change Complexity Score

### Scoring

| Metric | Value |
|--------|-------|
| Base Score per Occurrence | **+10 points** |
| Risk Category | **High** |

### Why +10 Points?

The score of +10 reflects the significant impact of infrastructure dependencies:

1. **High Blast Radius**: Infrastructure changes can affect entire systems
2. **External Coupling**: Dependency on third-party services/libraries
3. **Deployment Changes**: Often requires infrastructure updates
4. **Security Surface**: New attack vectors and vulnerabilities
5. **Operational Complexity**: More systems to manage and monitor

### Score Calculation

```
Signal Score = Count of New Infrastructure Imports × 10
```

### Example Scoring

| Scenario | New Infra Imports | Signal Score |
|----------|------------------|--------------|
| No new dependencies | 0 | 0 |
| Single Jenkins import | 1 | 10 |
| Three Spring imports | 3 | 30 |
| Four different DB clients | 4 | 40 |

## Best Practices

### 1. Evaluate Necessity

Before adding a new dependency, ask:
- Is this functionality essential?
- Can it be achieved with existing dependencies?
- What's the maintenance burden?

### 2. Version Pinning

Always pin dependency versions:
```groovy
// Good
implementation 'org.springframework:spring-core:5.3.21'

// Risky
implementation 'org.springframework:spring-core:+'
```

### 3. Abstraction Layers

Create abstraction layers around infrastructure:
```groovy
interface StorageService {
    def save(String key, byte[] data)
    def load(String key)
}

// Implementation can be swapped
class S3StorageService implements StorageService { ... }
class LocalStorageService implements StorageService { ... }
```

### 4. Feature Flags

Use feature flags for gradual rollout:
```groovy
if (featureEnabled('use-new-cache')) {
    return redisCache.get(key)
} else {
    return localCache.get(key)
}
```

### 5. Documentation

Document why each dependency was added:
- Purpose and use case
- Configuration requirements
- Fallback behavior
- Owner/maintainer

## Limitations

1. **Pattern-Based Detection**: Only detects imports matching known patterns
2. **No Semantic Analysis**: Cannot determine if import is actually used
3. **False Negatives**: May miss new/custom infrastructure not in patterns
4. **Transitive Dependencies**: Only detects direct imports, not transitive

## Related Signals

- **Pipeline Step**: CI/CD pipeline changes often accompany new dependencies
- **MetaClass Usage**: Dynamic behavior may interact with framework dependencies
- **Closure Mutable Capture**: Framework callbacks may capture mutable state

## References

- [Groovy Import Documentation](https://groovy-lang.org/structure.html#_imports)
- [Dependency Management Best Practices](https://docs.gradle.org/current/userguide/dependency_management.html)
- [Spring Framework Reference](https://spring.io/projects/spring-framework)
- [AWS SDK for Java](https://aws.amazon.com/sdk-for-java/)
