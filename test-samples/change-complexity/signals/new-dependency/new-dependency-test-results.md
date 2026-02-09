# New Infrastructure / Plugin Dependency Signal - Test Results

## Test Summary

| Test | Description | Before | After | Delta | Status |
|------|-------------|--------|-------|-------|--------|
| 01 | Control - No infrastructure dependencies | 0 | 0 | 0 | ✅ Pass |
| 02 | Single Jenkins Dependency | 0 | 1 | +1 | ✅ Pass |
| 03 | Spring Framework Dependencies | 0 | 3 | +3 | ✅ Pass |
| 04 | AWS Cloud Dependencies | 0 | 3 | +3 | ✅ Pass |
| 05 | Kubernetes Dependencies | 0 | 3 | +3 | ✅ Pass |
| 06 | Docker Dependencies | 0 | 3 | +3 | ✅ Pass |
| 07 | Kafka Messaging Dependencies | 0 | 3 | +3 | ✅ Pass |
| 08 | Multiple Infrastructure Dependencies | 0 | 4 | +4 | ✅ Pass |
| 09 | Gradle Plugin Dependencies | 0 | 3 | +3 | ✅ Pass |
| 10 | Hibernate Database Dependencies | 0 | 3 | +3 | ✅ Pass |

**Signal Score:** +10 points per new infrastructure dependency

**Note:** The count represents new imports that match infrastructure patterns. Standard library imports (groovy.*, java.*, javax.*) that don't match infrastructure patterns are not counted.

---

## Test Case Details

### Test 01: Control - No Infrastructure Dependencies

**Purpose:** Verify no false positives for standard Groovy library imports.

**Files:**
- `01_no_dependency_before.groovy`
- `01_no_dependency_after.groovy`

**Before Code:**

```groovy
import groovy.json.JsonSlurper
import groovy.xml.XmlParser
```

**After Code:**

```groovy
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.xml.XmlParser
import groovy.xml.MarkupBuilder
```

**Changes Made:**
- Added `groovy.json.JsonOutput`
- Added `groovy.xml.MarkupBuilder`

**Expected Behavior:**
- 2 new imports, but 0 infrastructure dependencies
- Groovy standard library doesn't match infra patterns

| Metric | Value |
|--------|-------|
| New Imports (Total) | 2 |
| New Infra Dependencies | 0 |
| Signal Score | 0.0 |

---

### Test 02: Single Jenkins Dependency

**Purpose:** Detect a single CI/CD tool dependency.

**Files:**
- `02_single_jenkins_before.groovy`
- `02_single_jenkins_after.groovy`

**Before Code:**

```groovy
// No imports
class BuildScript { ... }
```

**After Code:**

```groovy
import org.jenkinsci.plugins.workflow.cps.CpsScript

class BuildScript { ... }
```

**Changes Made:**
- Added Jenkins pipeline plugin import

**Expected Behavior:**
- Pattern matched: `jenkins` in `org.jenkinsci.plugins...`

| Metric | Value |
|--------|-------|
| New Imports (Total) | 1 |
| New Infra Dependencies | 1 |
| Matched Pattern | `jenkins` |
| Signal Score | 10.0 |

---

### Test 03: Spring Framework Dependencies

**Purpose:** Detect Spring framework dependency injection imports.

**Files:**
- `03_spring_framework_before.groovy`
- `03_spring_framework_after.groovy`

**Before Code:**

```groovy
// No imports
class UserService { ... }
```

**After Code:**

```groovy
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@Service
class UserService { ... }
```

**Changes Made:**
- Added Spring `@Service` annotation import
- Added Spring `@Autowired` annotation import
- Added Spring `@RestController` annotation import

**Expected Behavior:**
- Pattern matched: `spring` in all three imports

| Metric | Value |
|--------|-------|
| New Imports (Total) | 3 |
| New Infra Dependencies | 3 |
| Matched Pattern | `spring` |
| Signal Score | 30.0 |

---

### Test 04: AWS Cloud Dependencies

**Purpose:** Detect AWS cloud provider SDK imports.

**Files:**
- `04_aws_cloud_before.groovy`
- `04_aws_cloud_after.groovy`

**Before Code:**

```groovy
// No imports - local file storage
class FileStorage { ... }
```

**After Code:**

```groovy
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.PutObjectRequest

class FileStorage { ... }
```

**Changes Made:**
- Added AWS S3 client imports

**Expected Behavior:**
- Pattern matched: `aws` in `com.amazonaws...`

| Metric | Value |
|--------|-------|
| New Imports (Total) | 3 |
| New Infra Dependencies | 3 |
| Matched Pattern | `aws` |
| Signal Score | 30.0 |

---

### Test 05: Kubernetes Dependencies

**Purpose:** Detect Kubernetes client SDK imports.

**Files:**
- `05_kubernetes_before.groovy`
- `05_kubernetes_after.groovy`

**Before Code:**

```groovy
// No imports
class DeploymentManager { ... }
```

**After Code:**

```groovy
import io.kubernetes.client.openapi.ApiClient
import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.models.V1Deployment

class DeploymentManager { ... }
```

**Changes Made:**
- Added Kubernetes Java client imports

**Expected Behavior:**
- Pattern matched: `kubernetes` in `io.kubernetes...`

| Metric | Value |
|--------|-------|
| New Imports (Total) | 3 |
| New Infra Dependencies | 3 |
| Matched Pattern | `kubernetes` |
| Signal Score | 30.0 |

---

### Test 06: Docker Dependencies

**Purpose:** Detect Docker client library imports.

**Files:**
- `06_docker_before.groovy`
- `06_docker_after.groovy`

**Before Code:**

```groovy
// No imports
class ContainerRunner { ... }
```

**After Code:**

```groovy
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.api.command.CreateContainerResponse

class ContainerRunner { ... }
```

**Changes Made:**
- Added Docker Java client imports

**Expected Behavior:**
- Pattern matched: `docker` in `com.github.dockerjava...`

| Metric | Value |
|--------|-------|
| New Imports (Total) | 3 |
| New Infra Dependencies | 3 |
| Matched Pattern | `docker` |
| Signal Score | 30.0 |

---

### Test 07: Kafka Messaging Dependencies

**Purpose:** Detect Apache Kafka messaging library imports.

**Files:**
- `07_kafka_messaging_before.groovy`
- `07_kafka_messaging_after.groovy`

**Before Code:**

```groovy
// No imports - in-memory queue
class MessageQueue { ... }
```

**After Code:**

```groovy
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer

class MessageQueue { ... }
```

**Changes Made:**
- Added Kafka producer and consumer imports

**Expected Behavior:**
- Pattern matched: `kafka` in `org.apache.kafka...`

| Metric | Value |
|--------|-------|
| New Imports (Total) | 3 |
| New Infra Dependencies | 3 |
| Matched Pattern | `kafka` |
| Signal Score | 30.0 |

---

### Test 08: Multiple Infrastructure Dependencies

**Purpose:** Detect multiple different infrastructure dependencies added together.

**Files:**
- `08_multiple_deps_before.groovy`
- `08_multiple_deps_after.groovy`

**Before Code:**

```groovy
// No imports
class Application { ... }
```

**After Code:**

```groovy
import redis.clients.jedis.Jedis
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import org.elasticsearch.client.RestHighLevelClient

class Application { ... }
```

**Changes Made:**
- Added Redis client import
- Added MongoDB client imports (2)
- Added Elasticsearch client import

**Expected Behavior:**
- Patterns matched: `redis`, `mongodb` (×2), `elasticsearch`

| Metric | Value |
|--------|-------|
| New Imports (Total) | 4 |
| New Infra Dependencies | 4 |
| Matched Patterns | `redis`, `mongodb`, `elasticsearch` |
| Signal Score | 40.0 |

---

### Test 09: Gradle Plugin Dependencies

**Purpose:** Detect Gradle plugin API imports.

**Files:**
- `09_gradle_plugin_before.groovy`
- `09_gradle_plugin_after.groovy`

**Before Code:**

```groovy
// No imports
class BuildHelper { ... }
```

**After Code:**

```groovy
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class BuildHelper implements Plugin<Project> { ... }
```

**Changes Made:**
- Added Gradle Plugin API imports

**Expected Behavior:**
- Pattern matched: `gradle` in `org.gradle.api...`

| Metric | Value |
|--------|-------|
| New Imports (Total) | 3 |
| New Infra Dependencies | 3 |
| Matched Pattern | `gradle` |
| Signal Score | 30.0 |

---

### Test 10: Hibernate Database Dependencies

**Purpose:** Detect Hibernate ORM and JPA imports.

**Files:**
- `10_hibernate_before.groovy`
- `10_hibernate_after.groovy`

**Before Code:**

```groovy
// No imports - in-memory storage
class UserRepository { ... }
```

**After Code:**

```groovy
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.GeneratedValue

class UserRepository { ... }
```

**Changes Made:**
- Added Hibernate Session/SessionFactory imports
- Added JPA annotation imports

**Expected Behavior:**
- Pattern matched: `hibernate` in `org.hibernate...`
- Note: `javax.persistence` imports are not matched (no pattern)

| Metric | Value |
|--------|-------|
| New Imports (Total) | 6 |
| New Infra Dependencies | 3 |
| Matched Pattern | `hibernate` |
| Non-matched Imports | `javax.persistence.*` (3) |
| Signal Score | 30.0 |

---

## Actual Test Results

All tests have been executed. Results are verified and match expected values.

| Test | Before | After | Delta | Signal Score | Status |
|------|--------|-------|-------|--------------|--------|
| 01 | 0 | 0 | 0 | 0.0 | ✅ Pass |
| 02 | 0 | 1 | +1 | 10.0 | ✅ Pass |
| 03 | 0 | 3 | +3 | 30.0 | ✅ Pass |
| 04 | 0 | 3 | +3 | 30.0 | ✅ Pass |
| 05 | 0 | 3 | +3 | 30.0 | ✅ Pass |
| 06 | 0 | 3 | +3 | 30.0 | ✅ Pass |
| 07 | 0 | 3 | +3 | 30.0 | ✅ Pass |
| 08 | 0 | 4 | +4 | 40.0 | ✅ Pass |
| 09 | 0 | 3 | +3 | 30.0 | ✅ Pass |
| 10 | 0 | 3 | +3 | 30.0 | ✅ Pass |

---

## Infrastructure Patterns Detected

| Test | Patterns Matched | Import Examples |
|------|------------------|-----------------|
| 01 | (none) | `groovy.json.*` - standard library |
| 02 | `jenkins` | `org.jenkinsci.plugins.workflow.cps.CpsScript` |
| 03 | `spring` | `org.springframework.stereotype.Service` |
| 04 | `aws` | `com.amazonaws.services.s3.AmazonS3` |
| 05 | `kubernetes` | `io.kubernetes.client.openapi.ApiClient` |
| 06 | `docker` | `com.github.dockerjava.api.DockerClient` |
| 07 | `kafka` | `org.apache.kafka.clients.producer.KafkaProducer` |
| 08 | `redis`, `mongodb`, `elasticsearch` | Multiple clients |
| 09 | `gradle` | `org.gradle.api.Plugin` |
| 10 | `hibernate` | `org.hibernate.Session` |

---

## Summary

All 10 test cases passed successfully. The New Dependency signal detector correctly identifies:

1. **Control cases** (01): No false positives for standard library imports
2. **CI/CD tools** (02, 09): Jenkins and Gradle plugin imports
3. **Frameworks** (03, 10): Spring and Hibernate imports
4. **Cloud providers** (04): AWS SDK imports
5. **Container/orchestration** (05, 06): Kubernetes and Docker imports
6. **Messaging** (07): Kafka imports
7. **Multiple dependencies** (08): Redis, MongoDB, Elasticsearch together

### Coverage Summary

| Category | Covered |
|----------|---------|
| CI/CD Tools (Jenkins, Gradle) | ✅ |
| Frameworks (Spring, Hibernate) | ✅ |
| Cloud Providers (AWS) | ✅ |
| Container (Docker) | ✅ |
| Orchestration (Kubernetes) | ✅ |
| Messaging (Kafka) | ✅ |
| Databases (MongoDB, Redis, Elasticsearch) | ✅ |
| Control (no infra) | ✅ |
| Multiple dependencies | ✅ |

### Verification Checklist

- [x] All test files created and valid Groovy syntax
- [x] Before/After pairs properly structured
- [x] Expected counts verified against detection logic
- [x] Control case produces no false positives
- [x] Multiple dependency types detected correctly
- [x] Pattern matching is case-insensitive

*Tests executed on: 2026-01-21*
