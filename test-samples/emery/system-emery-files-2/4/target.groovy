/**
 * Enhanced GRC Control Processing Script with Upload Type and Continuous Monitoring
 * Operations: DRAFT_TO_REVIEW transition with automated testing and real-time monitoring
 * i) Upload type conditional processing with creator assignment
 * ii) Automated control testing with AI-assisted sample selection
 * iii) Continuous monitoring integration with alert management
 * iv) Control self-assessment (CSA) workflow support
 * v) Issue linkage and root cause analysis
 * vi) Regulatory mapping and compliance tracking
 **/

use("MS_GRC_CONTROL")
useDataObject("MS_GRC_REL_DEFN")
useDataObject("MS_GRC_CONTROL")
useDataObject("MS_GRC_CONTROL_TEST")
useDataObject("MS_GRC_CONTROL_EVIDENCE")
useDataObject("MS_GRC_CONTINUOUS_MONITORING")
useDataObject("MS_GRC_CSA_RESPONSE")
useDataObject("MS_GRC_REGULATORY_MAPPING")
useDataObject("MS_GRC_ISSUE")

// Extended variable declarations
String id, name, cur_date_string
Date current_date
String script_name = "ControlDraftToReview_ContinuousMonitoring.groovy"
Map<String, Object> processingContext = [:]
Map<String, Object> monitoringContext = [:]
Map<String, Object> csaContext = [:]
Map<String, Object> regulatoryContext = [:]
Map<String, List<String>> validationErrors = [:]
List<Map<String, Object>> auditTrailEntries = []
List<Map<String, Object>> monitoringAlerts = []
List<Map<String, Object>> csaResponses = []
List<Map<String, Object>> linkedIssues = []
List<Map<String, Object>> regulatoryMappings = []
def automatedTestResults = []
boolean isRollbackRequired = false
boolean enableContinuousMonitoring = true
boolean enableCSA = true
int alertThresholdCount = 5
double monitoringThreshold = 0.9
final String DELIMITER = '\$_\$'
final String CONTROL_PREFIX = 'CTL_'
final String MONITOR_PREFIX = 'MON_'
final String CSA_PREFIX = 'CSA_'

Emery.log.info("Started Pre-Hook {} at {}", script_name, Emery.util.currentDateAsString())

// Continuous monitoring engine with real-time alert management
class ContinuousMonitoringEngine {
    Map<String, Map<String, Object>> monitoringRules = [:]
    Map<String, List<Map<String, Object>>> alertHistory = [:]
    Map<String, Object> thresholds = [:]
    List<Map<String, Object>> activeAlerts = []
    boolean isEnabled = true
    
    void registerMonitoringRule(String ruleId, Map<String, Object> rule) {
        monitoringRules[ruleId] = rule
    }
    
    void setThreshold(String metricName, Map<String, Object> threshold) {
        thresholds[metricName] = threshold
    }
    
    Map<String, Object> evaluateRule(String ruleId, Map<String, Object> currentData) {
        def rule = monitoringRules[ruleId]
        if (!rule || !isEnabled) {
            return [evaluated: false, reason: 'Rule not found or monitoring disabled']
        }
        
        def result = [
            ruleId: ruleId,
            ruleName: rule.name,
            evaluatedAt: Emery.util.currentDate(),
            status: 'PASSED',
            findings: [],
            score: 100.0
        ]
        
        // Evaluate conditions
        rule.conditions?.each { condition ->
            def fieldValue = currentData[condition.field]
            boolean conditionMet = false
            
            switch(condition.operator) {
                case 'eq':
                    conditionMet = fieldValue == condition.value
                    break
                case 'ne':
                    conditionMet = fieldValue != condition.value
                    break
                case 'gt':
                    conditionMet = (fieldValue as Double) > (condition.value as Double)
                    break
                case 'lt':
                    conditionMet = (fieldValue as Double) < (condition.value as Double)
                    break
                case 'gte':
                    conditionMet = (fieldValue as Double) >= (condition.value as Double)
                    break
                case 'lte':
                    conditionMet = (fieldValue as Double) <= (condition.value as Double)
                    break
                case 'contains':
                    conditionMet = fieldValue?.toString()?.contains(condition.value)
                    break
                case 'in':
                    conditionMet = condition.value?.contains(fieldValue)
                    break
            }
            
            if (!conditionMet && condition.required) {
                result.status = 'FAILED'
                result.findings.add([
                    field: condition.field,
                    expected: condition.value,
                    actual: fieldValue,
                    message: condition.message ?: "Condition not met for ${condition.field}"
                ])
            }
        }
        
        // Calculate score based on findings
        if (rule.conditions && rule.conditions.size() > 0) {
            def passedConditions = rule.conditions.size() - result.findings.size()
            result.score = (passedConditions / rule.conditions.size()) * 100
        }
        
        return result
    }
    
    Map<String, Object> createAlert(String controlId, Map<String, Object> alertData) {
        def alert = [
            alertId: MONITOR_PREFIX + System.currentTimeMillis(),
            controlId: controlId,
            alertType: alertData.type ?: 'THRESHOLD_BREACH',
            severity: alertData.severity ?: 'MEDIUM',
            title: alertData.title,
            description: alertData.description,
            triggerRule: alertData.triggerRule,
            triggerValue: alertData.triggerValue,
            thresholdValue: alertData.thresholdValue,
            status: 'OPEN',
            assignedTo: alertData.assignedTo,
            createdAt: Emery.util.currentDate(),
            acknowledgedAt: null,
            resolvedAt: null,
            resolution: null
        ]
        
        activeAlerts.add(alert)
        
        if (!alertHistory.containsKey(controlId)) {
            alertHistory[controlId] = []
        }
        alertHistory[controlId].add(alert)
        
        return alert
    }
    
    Map<String, Object> acknowledgeAlert(String alertId, String acknowledgedBy) {
        def alert = activeAlerts.find { it.alertId == alertId }
        if (alert) {
            alert.status = 'ACKNOWLEDGED'
            alert.acknowledgedAt = Emery.util.currentDate()
            alert.acknowledgedBy = acknowledgedBy
            return [success: true, alert: alert]
        }
        return [success: false, error: 'Alert not found']
    }
    
    Map<String, Object> resolveAlert(String alertId, String resolution, String resolvedBy) {
        def alert = activeAlerts.find { it.alertId == alertId }
        if (alert) {
            alert.status = 'RESOLVED'
            alert.resolvedAt = Emery.util.currentDate()
            alert.resolution = resolution
            alert.resolvedBy = resolvedBy
            activeAlerts.remove(alert)
            return [success: true, alert: alert]
        }
        return [success: false, error: 'Alert not found']
    }
    
    List<Map<String, Object>> getActiveAlerts(String controlId) {
        return activeAlerts.findAll { it.controlId == controlId }
    }
    
    Map<String, Object> getAlertSummary(String controlId) {
        def controlAlerts = alertHistory[controlId] ?: []
        return [
            total: controlAlerts.size(),
            open: controlAlerts.count { it.status == 'OPEN' },
            acknowledged: controlAlerts.count { it.status == 'ACKNOWLEDGED' },
            resolved: controlAlerts.count { it.status == 'RESOLVED' },
            bySeverity: controlAlerts.groupBy { it.severity }.collectEntries { k, v -> [k, v.size()] }
        ]
    }
}

// Control Self-Assessment (CSA) workflow handler
class CSAWorkflowHandler {
    Map<String, Map<String, Object>> csaTemplates = [:]
    Map<String, List<Map<String, Object>>> csaResponses = [:]
    List<String> assessmentPeriods = ['QUARTERLY', 'SEMI_ANNUAL', 'ANNUAL']
    
    void registerTemplate(String templateId, Map<String, Object> template) {
        csaTemplates[templateId] = template
    }
    
    Map<String, Object> createCSAInstance(String controlId, String templateId, Map<String, Object> params) {
        def template = csaTemplates[templateId]
        if (!template) {
            return [success: false, error: 'Template not found']
        }
        
        def instance = [
            instanceId: CSA_PREFIX + System.currentTimeMillis(),
            controlId: controlId,
            templateId: templateId,
            templateName: template.name,
            assessmentPeriod: params.assessmentPeriod ?: 'QUARTERLY',
            periodStartDate: params.periodStartDate,
            periodEndDate: params.periodEndDate,
            assignedTo: params.assignedTo,
            assignedToName: params.assignedToName,
            dueDate: params.dueDate,
            questions: template.questions?.collect { q ->
                [
                    questionId: q.id,
                    questionText: q.text,
                    questionType: q.type,
                    required: q.required,
                    options: q.options,
                    response: null,
                    responseDate: null,
                    evidence: []
                ]
            } ?: [],
            status: 'PENDING',
            overallAssessment: null,
            completedDate: null,
            reviewedBy: null,
            reviewedDate: null,
            createdAt: Emery.util.currentDate()
        ]
        
        if (!csaResponses.containsKey(controlId)) {
            csaResponses[controlId] = []
        }
        csaResponses[controlId].add(instance)
        
        return [success: true, instance: instance]
    }
    
    Map<String, Object> submitResponse(String instanceId, String questionId, Map<String, Object> response) {
        for (def instances : csaResponses.values()) {
            def instance = instances.find { it.instanceId == instanceId }
            if (instance) {
                def question = instance.questions.find { it.questionId == questionId }
                if (question) {
                    question.response = response.value
                    question.responseDate = Emery.util.currentDate()
                    question.respondedBy = response.respondedBy
                    
                    if (response.evidence) {
                        question.evidence.addAll(response.evidence)
                    }
                    
                    // Check if all required questions are answered
                    def requiredQuestions = instance.questions.findAll { it.required }
                    def answeredRequired = requiredQuestions.findAll { it.response != null }
                    
                    if (answeredRequired.size() == requiredQuestions.size()) {
                        instance.status = 'READY_FOR_REVIEW'
                    } else {
                        instance.status = 'IN_PROGRESS'
                    }
                    
                    return [success: true, question: question]
                }
            }
        }
        return [success: false, error: 'Instance or question not found']
    }
    
    Map<String, Object> completeAssessment(String instanceId, String overallAssessment, String completedBy) {
        for (def instances : csaResponses.values()) {
            def instance = instances.find { it.instanceId == instanceId }
            if (instance) {
                instance.overallAssessment = overallAssessment
                instance.status = 'COMPLETED'
                instance.completedDate = Emery.util.currentDate()
                instance.completedBy = completedBy
                
                // Calculate effectiveness based on responses
                def positiveResponses = instance.questions.count { 
                    it.response in ['YES', 'EFFECTIVE', 'COMPLIANT', 'SATISFIED']
                }
                instance.effectivenessScore = instance.questions.size() > 0 ? 
                    (positiveResponses / instance.questions.size()) * 100 : 0
                
                return [success: true, instance: instance]
            }
        }
        return [success: false, error: 'Instance not found']
    }
    
    List<Map<String, Object>> getCSAHistory(String controlId) {
        return csaResponses[controlId] ?: []
    }
}

// Regulatory mapping and compliance tracker
class RegulatoryComplianceTracker {
    Map<String, List<Map<String, Object>>> regulatoryMappings = [:]
    Map<String, Map<String, Object>> frameworks = [:]
    List<String> supportedFrameworks = ['SOX', 'GDPR', 'HIPAA', 'PCI_DSS', 'ISO27001', 'NIST', 'COBIT']
    
    void registerFramework(String frameworkId, Map<String, Object> framework) {
        frameworks[frameworkId] = framework
    }
    
    Map<String, Object> addMapping(String controlId, Map<String, Object> mappingData) {
        def mapping = [
            mappingId: 'REG_' + System.currentTimeMillis(),
            controlId: controlId,
            frameworkId: mappingData.frameworkId,
            frameworkName: frameworks[mappingData.frameworkId]?.name ?: mappingData.frameworkId,
            requirementId: mappingData.requirementId,
            requirementText: mappingData.requirementText,
            mappingType: mappingData.mappingType ?: 'DIRECT',
            coverage: mappingData.coverage ?: 'FULL',
            notes: mappingData.notes,
            validFrom: mappingData.validFrom ?: Emery.util.currentDate(),
            validTo: mappingData.validTo,
            status: 'ACTIVE',
            createdAt: Emery.util.currentDate()
        ]
        
        if (!regulatoryMappings.containsKey(controlId)) {
            regulatoryMappings[controlId] = []
        }
        regulatoryMappings[controlId].add(mapping)
        
        return mapping
    }
    
    Map<String, Object> getComplianceSummary(String controlId) {
        def mappings = regulatoryMappings[controlId] ?: []
        def activeMappings = mappings.findAll { it.status == 'ACTIVE' }
        
        return [
            totalMappings: activeMappings.size(),
            byFramework: activeMappings.groupBy { it.frameworkId }.collectEntries { k, v -> [k, v.size()] },
            byCoverage: activeMappings.groupBy { it.coverage }.collectEntries { k, v -> [k, v.size()] },
            frameworks: activeMappings.collect { it.frameworkId }.unique()
        ]
    }
    
    List<Map<String, Object>> getMappingsForFramework(String controlId, String frameworkId) {
        def mappings = regulatoryMappings[controlId] ?: []
        return mappings.findAll { it.frameworkId == frameworkId && it.status == 'ACTIVE' }
    }
}

// Issue linkage and root cause analyzer
class IssueLinkageManager {
    Map<String, List<Map<String, Object>>> linkedIssues = [:]
    List<String> rootCauseCategories = [
        'PEOPLE', 'PROCESS', 'TECHNOLOGY', 'GOVERNANCE', 'EXTERNAL', 'OTHER'
    ]
    
    Map<String, Object> linkIssue(String controlId, Map<String, Object> issueData) {
        def link = [
            linkId: 'LNK_' + System.currentTimeMillis(),
            controlId: controlId,
            issueId: issueData.issueId,
            issueTitle: issueData.issueTitle,
            linkageType: issueData.linkageType ?: 'IDENTIFIED_DURING_TESTING',
            rootCauseCategory: issueData.rootCauseCategory,
            rootCauseDescription: issueData.rootCauseDescription,
            impactAssessment: issueData.impactAssessment,
            linkedBy: issueData.linkedBy,
            linkedDate: Emery.util.currentDate(),
            status: 'ACTIVE'
        ]
        
        if (!linkedIssues.containsKey(controlId)) {
            linkedIssues[controlId] = []
        }
        linkedIssues[controlId].add(link)
        
        return link
    }
    
    Map<String, Object> analyzeRootCauses(String controlId) {
        def issues = linkedIssues[controlId] ?: []
        
        def analysis = [
            totalIssues: issues.size(),
            byCategory: [:],
            byLinkageType: [:],
            recommendations: []
        ]
        
        rootCauseCategories.each { category ->
            analysis.byCategory[category] = issues.count { it.rootCauseCategory == category }
        }
        
        issues.groupBy { it.linkageType }.each { type, typeIssues ->
            analysis.byLinkageType[type] = typeIssues.size()
        }
        
        // Generate recommendations
        def dominantCategory = analysis.byCategory.max { it.value }?.key
        if (dominantCategory && analysis.byCategory[dominantCategory] > 2) {
            switch(dominantCategory) {
                case 'PEOPLE':
                    analysis.recommendations.add('Consider additional training or resource allocation')
                    break
                case 'PROCESS':
                    analysis.recommendations.add('Review and update process documentation')
                    break
                case 'TECHNOLOGY':
                    analysis.recommendations.add('Evaluate technology controls and automation opportunities')
                    break
                case 'GOVERNANCE':
                    analysis.recommendations.add('Strengthen governance framework and oversight')
                    break
            }
        }
        
        return analysis
    }
    
    List<Map<String, Object>> getLinkedIssues(String controlId) {
        return linkedIssues[controlId] ?: []
    }
}

// Automated testing with sample selection
def automatedTestEngine = {
    def testHistory = []
    
    // Select samples using stratified sampling
    def selectSamples = { List<Map<String, Object>> population, int sampleSize, String stratifyBy ->
        if (population.isEmpty()) return []
        
        def samples = []
        
        if (stratifyBy && population[0].containsKey(stratifyBy)) {
            def strata = population.groupBy { it[stratifyBy] }
            def samplesPerStratum = Math.max(1, sampleSize / strata.size())
            
            strata.each { stratum, items ->
                def stratumSamples = items.size() <= samplesPerStratum ? 
                    items : items.shuffled().take(samplesPerStratum as int)
                samples.addAll(stratumSamples)
            }
        } else {
            samples = population.size() <= sampleSize ? 
                population : population.shuffled().take(sampleSize)
        }
        
        return samples
    }
    
    // Execute automated test
    def executeAutomatedTest = { String controlId, String testType, Map<String, Object> testConfig ->
        def result = [
            testId: 'AUTO_' + System.currentTimeMillis(),
            controlId: controlId,
            testType: testType,
            executedAt: Emery.util.currentDate(),
            config: testConfig,
            status: 'RUNNING',
            findings: [],
            metrics: [:]
        ]
        
        try {
            // Simulate test execution based on type
            switch(testType) {
                case 'ACCESS_REVIEW':
                    result.metrics.totalAccess = testConfig.accessCount ?: 100
                    result.metrics.appropriateAccess = testConfig.appropriateCount ?: 95
                    result.metrics.inappropriateAccess = result.metrics.totalAccess - result.metrics.appropriateAccess
                    result.metrics.complianceRate = result.metrics.appropriateAccess / result.metrics.totalAccess
                    break
                    
                case 'SEGREGATION_OF_DUTIES':
                    result.metrics.totalUsers = testConfig.userCount ?: 50
                    result.metrics.sodViolations = testConfig.violations ?: 2
                    result.metrics.complianceRate = (result.metrics.totalUsers - result.metrics.sodViolations) / result.metrics.totalUsers
                    break
                    
                case 'CONFIGURATION_BASELINE':
                    result.metrics.totalSettings = testConfig.settingsCount ?: 200
                    result.metrics.compliantSettings = testConfig.compliantCount ?: 190
                    result.metrics.nonCompliantSettings = result.metrics.totalSettings - result.metrics.compliantSettings
                    result.metrics.complianceRate = result.metrics.compliantSettings / result.metrics.totalSettings
                    break
                    
                default:
                    result.metrics.complianceRate = 0.95
            }
            
            result.status = 'COMPLETED'
            result.overallResult = result.metrics.complianceRate >= 0.95 ? 'PASSED' : 'FAILED'
            
        } catch (Exception e) {
            result.status = 'FAILED'
            result.error = e.message
        }
        
        testHistory.add(result)
        return result
    }
    
    return [
        selectSamples: selectSamples,
        executeAutomatedTest: executeAutomatedTest,
        getTestHistory: { testHistory.collect() }
    ]
}

// Initialize automated test engine
def autoTestEngine = automatedTestEngine()

// Main processing block
try {
    Emery.log.info("Starting main processing for control metric_id: {} with upload_type: {}", F.metric_id, F.upload_type)
    
    // Initialize processing context
    processingContext = [
        scriptName: script_name,
        startTime: System.currentTimeMillis(),
        formMetricId: F.metric_id,
        processInstanceId: F.process_instance_id,
        instanceId: F.instance_id,
        currentUser: F.dd_current_user_name,
        currentUserId: F.created_by,
        isSDOS: Emery.ctx.isSDOS(),
        objectType: F.dd_object_type,
        currentStatus: F.dd_current_status,
        currentStage: F.dd_current_stage,
        uploadType: F.upload_type
    ]
    
    // Initialize managers
    def monitoringEngine = new ContinuousMonitoringEngine()
    def csaHandler = new CSAWorkflowHandler()
    def regulatoryTracker = new RegulatoryComplianceTracker()
    def issueLinkageManager = new IssueLinkageManager()
    
    // Configure monitoring rules
    monitoringEngine.registerMonitoringRule('EFFECTIVENESS_THRESHOLD', [
        name: 'Control Effectiveness Threshold',
        conditions: [
            [field: 'effectiveness_score', operator: 'gte', value: 80, required: true, message: 'Effectiveness below threshold']
        ]
    ])
    
    monitoringEngine.registerMonitoringRule('TEST_COMPLETION', [
        name: 'Test Completion Check',
        conditions: [
            [field: 'test_status', operator: 'eq', value: 'COMPLETED', required: true, message: 'Testing not completed']
        ]
    ])
    
    // Register CSA template
    csaHandler.registerTemplate('STANDARD_CSA', [
        name: 'Standard Control Self-Assessment',
        questions: [
            [id: 'Q1', text: 'Is the control operating as designed?', type: 'YES_NO', required: true],
            [id: 'Q2', text: 'Have there been any control failures?', type: 'YES_NO', required: true],
            [id: 'Q3', text: 'Is documentation up to date?', type: 'YES_NO', required: true],
            [id: 'Q4', text: 'Rate the overall effectiveness', type: 'RATING', required: true, options: ['EFFECTIVE', 'PARTIALLY_EFFECTIVE', 'INEFFECTIVE']],
            [id: 'Q5', text: 'Additional comments', type: 'TEXT', required: false]
        ]
    ])
    
    // Register regulatory frameworks
    regulatoryTracker.registerFramework('SOX', [name: 'Sarbanes-Oxley Act', version: '2002'])
    regulatoryTracker.registerFramework('GDPR', [name: 'General Data Protection Regulation', version: '2018'])
    regulatoryTracker.registerFramework('ISO27001', [name: 'ISO 27001 Information Security', version: '2013'])
    
    /* Upload type specific handling */
    if (!F.upload_type) {
        def current_status = F.dd_current_status
        def current_stage = F.dd_current_stage
        def obj_status = F.obj_status
        
        Emery.log.debug("Script Name: {} executed for status: {}, stage: {}, obj_status: {}", script_name, current_status, current_stage, obj_status)
        
        // Creator assignment for new controls
        if ((current_status in ['NEW', ''] || obj_status == 'NEW') && (current_stage in ['CREATE_EDIT', 'NONE'])) {
            F.created_by_hidden = F.dd_current_user_name
            F.obj_created_by = F.dd_current_user_name
            F.obj_created_on = Emery.util.currentDate()
            
            auditTrailEntries.add([
                action: 'CREATOR_ASSIGNED',
                details: [creator: F.dd_current_user_name, timestamp: Emery.util.currentDate()],
                timestamp: Emery.util.currentDate()
            ])
        }
        
        // Continuous monitoring evaluation
        if (enableContinuousMonitoring && F.object_id && F.object_id != '' && F.object_id != 'NONE') {
            def effectivenessData = [
                effectiveness_score: F.overall_effectiveness_score ?: 0,
                test_status: F.test_status ?: 'PENDING'
            ]
            
            def effectivenessEval = monitoringEngine.evaluateRule('EFFECTIVENESS_THRESHOLD', effectivenessData)
            def testEval = monitoringEngine.evaluateRule('TEST_COMPLETION', effectivenessData)
            
            monitoringContext.evaluations = [effectivenessEval, testEval]
            
            // Create alerts for failed evaluations
            if (effectivenessEval.status == 'FAILED') {
                def alert = monitoringEngine.createAlert(F.object_id, [
                    type: 'EFFECTIVENESS_BREACH',
                    severity: 'HIGH',
                    title: 'Control effectiveness below threshold',
                    description: "Control ${F.object_name} has effectiveness of ${F.overall_effectiveness_score}%",
                    triggerValue: F.overall_effectiveness_score,
                    thresholdValue: 80,
                    assignedTo: F.control_owner_id
                ])
                monitoringAlerts.add(alert)
            }
            
            monitoringContext.alertSummary = monitoringEngine.getAlertSummary(F.object_id)
        }
        
        // CSA workflow initiation
        if (enableCSA && F.csa_required == 'Y') {
            def csaResult = csaHandler.createCSAInstance(F.object_id, 'STANDARD_CSA', [
                assessmentPeriod: F.assessment_period ?: 'QUARTERLY',
                periodStartDate: F.assessment_period_start,
                periodEndDate: F.assessment_period_end,
                assignedTo: F.control_owner_id,
                assignedToName: F.control_owner_name,
                dueDate: Emery.dateUtil.addDays(Emery.util.currentDate(), 30)
            ])
            
            if (csaResult.success) {
                csaResponses.add(csaResult.instance)
                csaContext.currentInstance = csaResult.instance
            }
        }
        
        // Regulatory mapping
        if (F.regulatory_frameworks && F.regulatory_frameworks != '') {
            def frameworks = F.regulatory_frameworks.toString().split(',').collect { it.trim() }
            
            frameworks.each { frameworkId ->
                def mapping = regulatoryTracker.addMapping(F.object_id, [
                    frameworkId: frameworkId,
                    requirementId: F."${frameworkId.toLowerCase()}_requirement_id",
                    requirementText: F."${frameworkId.toLowerCase()}_requirement_text",
                    coverage: F."${frameworkId.toLowerCase()}_coverage" ?: 'FULL'
                ])
                regulatoryMappings.add(mapping)
            }
            
            regulatoryContext.summary = regulatoryTracker.getComplianceSummary(F.object_id)
        }
        
        // Automated testing
        if (F.automated_testing_enabled == 'Y') {
            def testTypes = ['ACCESS_REVIEW', 'SEGREGATION_OF_DUTIES', 'CONFIGURATION_BASELINE']
            
            testTypes.each { testType ->
                def testResult = autoTestEngine.executeAutomatedTest(F.object_id, testType, [
                    accessCount: 100,
                    appropriateCount: 95,
                    userCount: 50,
                    violations: 2,
                    settingsCount: 200,
                    compliantCount: 190
                ])
                automatedTestResults.add(testResult)
            }
            
            processingContext.automatedTestResults = automatedTestResults
        }
        
        // Issue linkage analysis
        if (F.has_linked_issues == 'Y') {
            def rootCauseAnalysis = issueLinkageManager.analyzeRootCauses(F.object_id)
            processingContext.rootCauseAnalysis = rootCauseAnalysis
        }
    }
    
    /* Non-multirow field updates */
    
    // Object status update
    if (F.object_action == 'SUBMIT_REVIEW') {
        F.obj_status = 'REVIEW_PENDING'
    }
    F.obj_status_temp = F.obj_status
    
    // SDOS handling
    if (Emery.ctx.isSDOS()) {
        F.owner_organizations = F.org_groups
    }
    
    // Clear wfi_display
    if (F.wfi_display != null) {
        F.wfi_display = null
    }
    
    // Build wfi_stored
    name = F.dd_current_user_name
    current_date = Emery.util.currentDate()
    cur_date_string = Emery.dateUtil.convertDateToString(current_date, "dd/MM/yyyy HH:mm:ss")
    F.wfi_stored = name.concat(DELIMITER).concat(cur_date_string)
    
    // Update stage
    F.dd_current_stage = 'REVIEW'
    
    // Comment insertion
    if (F.action_comments != null && F.action_comments.toString().trim() != '') {
        String result = Emery.util.insertComments(F.process_instance_id as int, F.instance_id as int, F.metric_id as int, F.created_by as int, "ACTION_COMMENTS", F.action_comments as String)
        F.action_comments = ''
    }
    
    /* Apps_ID Creation */
    if (F.object_id == '' || F.object_id == 'NONE') {
        id = Emery.util.nextAppsId("MS_GRC_CONTROL", "GRC", "MS_GRC_CONTROL_IDGEN")
        F.object_id = id
    }
    
    /* Multirow processing */
    F.orb.rows.each { row ->
        if (row.rel_inst_id == '' || row.rel_inst_id == 'NONE') {
            id = Emery.util.nextAppsId("MS_GRC_CONTROL", "GRC", "MS_ORB_REL_INST_ID")
            row.rel_inst_id = id
        }
        
        if (row.src_obj_id == '' || row.src_obj_id == null) {
            row.src_obj_id = F.object_id
        }
        
        if (row.rel_config_id == '' || row.rel_config_id == null) {
            def records = Emery.dataTable.read("MS_GRC_ORB_CONFIG_FORM_TITLE")
            def targetRecord = records.find { r -> r.ORB_OBJ_TYPE == row.additional_column4 }
            
            if (targetRecord) {
                row.additional_column3 = targetRecord.object_type
                
                SelectQuery relQuery = new SelectQuery.Builder("MS_GRC_REL_DEFN")
                    .columns(["REL_ID"])
                    .addCondition(Condition.eq("SRC_OBJ_TYPE", F.dd_object_type))
                    .addCondition(Condition.eq("DEST_OBJ_TYPE", row.additional_column3))
                    .loadRegions(false)
                    .build()
                
                def relResult = Emery.dataobject.fetch(relQuery)
                relResult.dataObjects?.each { dataObj ->
                    row.rel_config_id = dataObj.rel_id
                }
            }
        }
    }
    
    // Generate rel_source_id
    if (F.rel_source_id == '' || F.rel_source_id == 'NONE') {
        id = Emery.util.nextAppsId("MS_GRC_CONTROL", "GRC", "MS_ORB_REL_SOURCE_ID")
        F.rel_source_id = id
    }
    
    if (F.rel_source_object_id == '' || F.rel_source_object_id == 'NONE') {
        F.rel_source_object_id = F.object_id
    }
    
    // App config handling
    if (F.own_app_config_temp == 'Yes' || F.own_app_config_temp == 'No') {
        F.own_app_config_temp = F.own_app_config
    }
    
    // Store contexts in processing context
    processingContext.monitoring = monitoringContext
    processingContext.csa = csaContext
    processingContext.regulatory = regulatoryContext
    processingContext.alerts = monitoringAlerts
    
    // Final processing context update
    processingContext.endTime = System.currentTimeMillis()
    processingContext.duration = processingContext.endTime - processingContext.startTime
    
    Emery.log.info("Processing completed in {}ms with {} alerts", processingContext.duration, monitoringAlerts.size())
    
} catch (Exception e) {
    Emery.log.error("Error in {}: {}", script_name, e.message)
    isRollbackRequired = true
    throw e
} finally {
    Emery.log.info("Script {} completed with rollback={}", script_name, isRollbackRequired)
}

