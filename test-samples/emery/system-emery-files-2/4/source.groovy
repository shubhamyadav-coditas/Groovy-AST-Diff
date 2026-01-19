/**
 * Complex GRC Control Processing Script - Draft to Review Transition
 * Operations: DRAFT_TO_REVIEW transition with control testing and effectiveness assessment
 * i) Field updates: obj_status, obj_status_temp, owner_organizations, wfi_display, wfi_stored, dd_current_stage
 * ii) Control test execution tracking and evidence management
 * iii) Control design and operating effectiveness evaluation
 * iv) Deficiency identification and remediation tracking
 * v) SOX compliance validation and certification support
 **/

use("MS_GRC_CONTROL")
useDataObject("MS_GRC_REL_DEFN")
useDataObject("MS_GRC_CONTROL")
useDataObject("MS_GRC_CONTROL_TEST")
useDataObject("MS_GRC_CONTROL_EVIDENCE")
useDataObject("MS_GRC_CONTROL_DEFICIENCY")
useDataObject("MS_GRC_SOX_CERTIFICATION")
useDataObject("MS_GRC_REMEDIATION_PLAN")

// Extended variable declarations for control processing
String id, name, cur_date_string
Date current_date
String script_name = "ControlDraftToReview_Advanced.groovy"
Map<String, Object> processingContext = [:]
Map<String, Object> testExecutionContext = [:]
Map<String, Object> evidenceRepository = [:]
Map<String, List<String>> validationErrors = [:]
List<Map<String, Object>> auditTrailEntries = []
List<Map<String, Object>> testResults = []
List<Map<String, Object>> deficiencies = []
List<Map<String, Object>> remediationPlans = []
def controlHierarchy = []
boolean isRollbackRequired = false
boolean requiresRemediation = false
int testSampleSize = 25
double effectivenessThreshold = 0.8
final String DELIMITER = '\$_\$'
final String CONTROL_PREFIX = 'CTL_'
final String TEST_PREFIX = 'TST_'
final String EVIDENCE_PREFIX = 'EVD_'

Emery.log.info("Started Pre-Hook {} at {}", script_name, Emery.util.currentDateAsString())

// Control testing framework with sample-based evaluation
class ControlTestingFramework {
    Map<String, Map<String, Object>> testTemplates = [:]
    Map<String, List<Map<String, Object>>> testExecutions = [:]
    List<String> testTypes = ['DESIGN', 'OPERATING', 'WALKTHROUGH', 'INQUIRY', 'OBSERVATION', 'INSPECTION', 'REPERFORMANCE']
    Map<String, Double> testTypeWeights = [
        'DESIGN': 0.3,
        'OPERATING': 0.4,
        'WALKTHROUGH': 0.1,
        'INQUIRY': 0.05,
        'OBSERVATION': 0.05,
        'INSPECTION': 0.05,
        'REPERFORMANCE': 0.05
    ]
    
    void registerTestTemplate(String templateId, Map<String, Object> template) {
        testTemplates[templateId] = template
    }
    
    Map<String, Object> createTestExecution(String controlId, String testType, Map<String, Object> params) {
        def execution = [
            executionId: TEST_PREFIX + System.currentTimeMillis() + '_' + controlId,
            controlId: controlId,
            testType: testType,
            testerId: params.testerId,
            testerName: params.testerName,
            plannedDate: params.plannedDate ?: Emery.util.currentDate(),
            actualDate: null,
            status: 'PLANNED',
            sampleSize: params.sampleSize ?: 25,
            samplesSelected: [],
            samplesTestedCount: 0,
            exceptionsFound: 0,
            exceptionDetails: [],
            conclusion: null,
            effectivenessRating: null,
            evidenceRefs: [],
            workpaperRef: params.workpaperRef,
            comments: params.comments,
            createdAt: Emery.util.currentDate()
        ]
        
        if (!testExecutions.containsKey(controlId)) {
            testExecutions[controlId] = []
        }
        testExecutions[controlId].add(execution)
        
        return execution
    }
    
    Map<String, Object> executeTest(String executionId, Map<String, Object> testData) {
        def execution = findExecution(executionId)
        if (!execution) {
            return [success: false, error: 'Execution not found']
        }
        
        execution.actualDate = Emery.util.currentDate()
        execution.status = 'IN_PROGRESS'
        execution.samplesTestedCount = testData.samplesTestedCount ?: 0
        execution.exceptionsFound = testData.exceptionsFound ?: 0
        
        if (testData.exceptionDetails) {
            execution.exceptionDetails.addAll(testData.exceptionDetails)
        }
        
        // Calculate effectiveness
        if (execution.samplesTestedCount > 0) {
            double exceptionRate = execution.exceptionsFound / execution.samplesTestedCount
            double effectiveness = 1.0 - exceptionRate
            
            if (effectiveness >= 0.95) {
                execution.effectivenessRating = 'EFFECTIVE'
            } else if (effectiveness >= 0.80) {
                execution.effectivenessRating = 'PARTIALLY_EFFECTIVE'
            } else {
                execution.effectivenessRating = 'INEFFECTIVE'
            }
            
            execution.calculatedEffectiveness = effectiveness
        }
        
        execution.conclusion = testData.conclusion
        execution.status = 'COMPLETED'
        execution.completedAt = Emery.util.currentDate()
        
        return [success: true, execution: execution]
    }
    
    private Map<String, Object> findExecution(String executionId) {
        for (def entries : testExecutions.values()) {
            def found = entries.find { it.executionId == executionId }
            if (found) return found
        }
        return null
    }
    
    Map<String, Object> calculateOverallEffectiveness(String controlId) {
        def executions = testExecutions[controlId] ?: []
        if (executions.isEmpty()) {
            return [effectiveness: 0.0, rating: 'NOT_TESTED', testCount: 0]
        }
        
        double weightedSum = 0.0
        double totalWeight = 0.0
        int completedTests = 0
        
        executions.findAll { it.status == 'COMPLETED' }.each { exec ->
            def weight = testTypeWeights[exec.testType] ?: 0.1
            def effectiveness = exec.calculatedEffectiveness ?: 0.0
            
            weightedSum += effectiveness * weight
            totalWeight += weight
            completedTests++
        }
        
        double overallEffectiveness = totalWeight > 0 ? weightedSum / totalWeight : 0.0
        
        String rating
        if (overallEffectiveness >= 0.95) rating = 'EFFECTIVE'
        else if (overallEffectiveness >= 0.80) rating = 'PARTIALLY_EFFECTIVE'
        else if (overallEffectiveness >= 0.50) rating = 'INEFFECTIVE'
        else rating = 'NOT_EFFECTIVE'
        
        return [
            effectiveness: Math.round(overallEffectiveness * 1000) / 1000.0,
            rating: rating,
            testCount: completedTests,
            totalTests: executions.size()
        ]
    }
}

// Evidence management system with document classification
class EvidenceManagementSystem {
    Map<String, List<Map<String, Object>>> evidenceByControl = [:]
    Map<String, Map<String, Object>> evidenceIndex = [:]
    List<String> evidenceTypes = ['DOCUMENT', 'SCREENSHOT', 'SYSTEM_REPORT', 'EMAIL', 'APPROVAL', 'LOG_FILE', 'CONFIGURATION', 'OTHER']
    Map<String, List<String>> requiredEvidenceByControlType = [:]
    
    void setRequiredEvidence(String controlType, List<String> requiredTypes) {
        requiredEvidenceByControlType[controlType] = requiredTypes
    }
    
    Map<String, Object> addEvidence(String controlId, Map<String, Object> evidenceData) {
        def evidence = [
            evidenceId: EVIDENCE_PREFIX + System.currentTimeMillis(),
            controlId: controlId,
            evidenceType: evidenceData.type ?: 'DOCUMENT',
            title: evidenceData.title,
            description: evidenceData.description,
            documentRef: evidenceData.documentRef,
            sourceSystem: evidenceData.sourceSystem,
            collectedBy: evidenceData.collectedBy,
            collectedDate: evidenceData.collectedDate ?: Emery.util.currentDate(),
            periodStart: evidenceData.periodStart,
            periodEnd: evidenceData.periodEnd,
            isComplete: evidenceData.isComplete ?: false,
            qualityScore: evidenceData.qualityScore,
            verifiedBy: null,
            verifiedDate: null,
            status: 'PENDING_REVIEW',
            metadata: evidenceData.metadata ?: [:],
            createdAt: Emery.util.currentDate()
        ]
        
        if (!evidenceByControl.containsKey(controlId)) {
            evidenceByControl[controlId] = []
        }
        evidenceByControl[controlId].add(evidence)
        evidenceIndex[evidence.evidenceId] = evidence
        
        return evidence
    }
    
    Map<String, Object> verifyEvidence(String evidenceId, String verifierId, String verifierName) {
        def evidence = evidenceIndex[evidenceId]
        if (!evidence) {
            return [success: false, error: 'Evidence not found']
        }
        
        evidence.verifiedBy = verifierName
        evidence.verifiedById = verifierId
        evidence.verifiedDate = Emery.util.currentDate()
        evidence.status = 'VERIFIED'
        
        return [success: true, evidence: evidence]
    }
    
    Map<String, Object> assessEvidenceCompleteness(String controlId, String controlType) {
        def evidences = evidenceByControl[controlId] ?: []
        def requiredTypes = requiredEvidenceByControlType[controlType] ?: []
        
        def providedTypes = evidences.collect { it.evidenceType }.unique()
        def missingTypes = requiredTypes.findAll { !providedTypes.contains(it) }
        
        double completeness = requiredTypes.size() > 0 ? 
            (requiredTypes.size() - missingTypes.size()) / requiredTypes.size() : 1.0
        
        return [
            controlId: controlId,
            totalEvidence: evidences.size(),
            verifiedEvidence: evidences.count { it.status == 'VERIFIED' },
            pendingEvidence: evidences.count { it.status == 'PENDING_REVIEW' },
            completeness: Math.round(completeness * 100) / 100.0,
            missingTypes: missingTypes,
            isComplete: missingTypes.isEmpty()
        ]
    }
    
    List<Map<String, Object>> getEvidenceForControl(String controlId) {
        return evidenceByControl[controlId] ?: []
    }
}

// Deficiency tracking and remediation management
class DeficiencyManager {
    List<Map<String, Object>> deficiencies = []
    Map<String, List<Map<String, Object>>> remediationPlans = [:]
    Map<String, String> severityClassification = [
        'MATERIAL_WEAKNESS': 'CRITICAL',
        'SIGNIFICANT_DEFICIENCY': 'HIGH',
        'CONTROL_DEFICIENCY': 'MEDIUM',
        'OBSERVATION': 'LOW'
    ]
    
    Map<String, Object> createDeficiency(Map<String, Object> deficiencyData) {
        def deficiency = [
            deficiencyId: 'DEF_' + System.currentTimeMillis(),
            controlId: deficiencyData.controlId,
            testExecutionId: deficiencyData.testExecutionId,
            title: deficiencyData.title,
            description: deficiencyData.description,
            classification: deficiencyData.classification ?: 'CONTROL_DEFICIENCY',
            severity: severityClassification[deficiencyData.classification] ?: 'MEDIUM',
            rootCause: deficiencyData.rootCause,
            impact: deficiencyData.impact,
            likelihood: deficiencyData.likelihood,
            affectedProcesses: deficiencyData.affectedProcesses ?: [],
            affectedAccounts: deficiencyData.affectedAccounts ?: [],
            identifiedBy: deficiencyData.identifiedBy,
            identifiedDate: deficiencyData.identifiedDate ?: Emery.util.currentDate(),
            status: 'OPEN',
            targetRemediationDate: deficiencyData.targetRemediationDate,
            actualRemediationDate: null,
            compensatingControls: deficiencyData.compensatingControls ?: [],
            metadata: deficiencyData.metadata ?: [:],
            createdAt: Emery.util.currentDate()
        ]
        
        deficiencies.add(deficiency)
        return deficiency
    }
    
    Map<String, Object> createRemediationPlan(String deficiencyId, Map<String, Object> planData) {
        def plan = [
            planId: 'REM_' + System.currentTimeMillis(),
            deficiencyId: deficiencyId,
            title: planData.title,
            description: planData.description,
            actionItems: planData.actionItems ?: [],
            assignedTo: planData.assignedTo,
            assignedToName: planData.assignedToName,
            plannedStartDate: planData.plannedStartDate,
            plannedEndDate: planData.plannedEndDate,
            actualStartDate: null,
            actualEndDate: null,
            milestones: planData.milestones ?: [],
            status: 'DRAFT',
            progressPercent: 0,
            approvedBy: null,
            approvedDate: null,
            createdAt: Emery.util.currentDate()
        ]
        
        if (!remediationPlans.containsKey(deficiencyId)) {
            remediationPlans[deficiencyId] = []
        }
        remediationPlans[deficiencyId].add(plan)
        
        return plan
    }
    
    Map<String, Object> updateRemediationProgress(String planId, int progressPercent, String status) {
        for (def plans : remediationPlans.values()) {
            def plan = plans.find { it.planId == planId }
            if (plan) {
                plan.progressPercent = progressPercent
                plan.status = status
                plan.lastUpdated = Emery.util.currentDate()
                
                if (status == 'COMPLETED' && progressPercent >= 100) {
                    plan.actualEndDate = Emery.util.currentDate()
                    
                    // Update deficiency status
                    def deficiency = deficiencies.find { it.deficiencyId == plan.deficiencyId }
                    if (deficiency) {
                        deficiency.status = 'REMEDIATED'
                        deficiency.actualRemediationDate = Emery.util.currentDate()
                    }
                }
                
                return [success: true, plan: plan]
            }
        }
        return [success: false, error: 'Plan not found']
    }
    
    List<Map<String, Object>> getOpenDeficiencies(String controlId) {
        return deficiencies.findAll { it.controlId == controlId && it.status == 'OPEN' }
    }
    
    Map<String, Object> getDeficiencySummary(String controlId) {
        def controlDeficiencies = deficiencies.findAll { it.controlId == controlId }
        
        return [
            total: controlDeficiencies.size(),
            open: controlDeficiencies.count { it.status == 'OPEN' },
            remediated: controlDeficiencies.count { it.status == 'REMEDIATED' },
            bySeverity: controlDeficiencies.groupBy { it.severity }.collectEntries { k, v -> [k, v.size()] },
            byClassification: controlDeficiencies.groupBy { it.classification }.collectEntries { k, v -> [k, v.size()] }
        ]
    }
}

// SOX compliance validator
class SOXComplianceValidator {
    Map<String, List<String>> soxRequirements = [:]
    List<Map<String, Object>> certifications = []
    
    void loadSOXRequirements(String controlType) {
        // Define SOX requirements by control type
        soxRequirements['FINANCIAL_REPORTING'] = [
            'SEGREGATION_OF_DUTIES',
            'AUTHORIZATION_CONTROLS',
            'RECONCILIATION',
            'DOCUMENTATION',
            'AUDIT_TRAIL'
        ]
        soxRequirements['IT_GENERAL'] = [
            'ACCESS_CONTROLS',
            'CHANGE_MANAGEMENT',
            'BACKUP_RECOVERY',
            'SECURITY_MONITORING'
        ]
        soxRequirements['ENTITY_LEVEL'] = [
            'CONTROL_ENVIRONMENT',
            'RISK_ASSESSMENT',
            'MONITORING_ACTIVITIES',
            'INFORMATION_COMMUNICATION'
        ]
    }
    
    Map<String, Object> validateSOXCompliance(String controlId, String controlType, Map<String, Object> controlData) {
        loadSOXRequirements(controlType)
        def requirements = soxRequirements[controlType] ?: []
        
        def validationResults = []
        def passedCount = 0
        def failedCount = 0
        
        requirements.each { requirement ->
            def result = [
                requirement: requirement,
                status: 'PENDING',
                details: []
            ]
            
            // Simulate requirement validation
            switch(requirement) {
                case 'SEGREGATION_OF_DUTIES':
                    result.status = controlData.has_sod_controls ? 'PASSED' : 'FAILED'
                    break
                case 'AUTHORIZATION_CONTROLS':
                    result.status = controlData.has_authorization ? 'PASSED' : 'FAILED'
                    break
                case 'DOCUMENTATION':
                    result.status = controlData.documentation_complete ? 'PASSED' : 'FAILED'
                    break
                case 'AUDIT_TRAIL':
                    result.status = controlData.audit_trail_enabled ? 'PASSED' : 'FAILED'
                    break
                default:
                    result.status = 'NOT_APPLICABLE'
            }
            
            if (result.status == 'PASSED') passedCount++
            else if (result.status == 'FAILED') failedCount++
            
            validationResults.add(result)
        }
        
        def complianceScore = requirements.size() > 0 ? passedCount / requirements.size() : 0.0
        
        return [
            controlId: controlId,
            controlType: controlType,
            complianceScore: Math.round(complianceScore * 100) / 100.0,
            isCompliant: failedCount == 0,
            passedRequirements: passedCount,
            failedRequirements: failedCount,
            totalRequirements: requirements.size(),
            validationResults: validationResults,
            validatedAt: Emery.util.currentDate()
        ]
    }
    
    Map<String, Object> createCertification(String controlId, String certifierId, String certifierName, boolean isCompliant) {
        def certification = [
            certificationId: 'CERT_' + System.currentTimeMillis(),
            controlId: controlId,
            certifierId: certifierId,
            certifierName: certifierName,
            certificationDate: Emery.util.currentDate(),
            certificationPeriodStart: Emery.dateUtil.addDays(Emery.util.currentDate(), -365),
            certificationPeriodEnd: Emery.util.currentDate(),
            isCompliant: isCompliant,
            certificationStatement: isCompliant ? 
                "I certify that control ${controlId} is operating effectively as of the certification date." :
                "Control ${controlId} has identified deficiencies that require remediation.",
            status: 'CERTIFIED',
            createdAt: Emery.util.currentDate()
        ]
        
        certifications.add(certification)
        return certification
    }
}

// Control hierarchy and dependency analyzer
def analyzeControlHierarchy = { String controlId ->
    def hierarchy = [
        controlId: controlId,
        parentControls: [],
        childControls: [],
        relatedRisks: [],
        relatedProcesses: [],
        dependencies: []
    ]
    
    try {
        // Get parent controls
        SelectQuery parentQuery = new SelectQuery.Builder("MS_GRC_CONTROL_HIERARCHY")
            .columns(["PARENT_CONTROL_ID", "CHILD_CONTROL_ID", "HIERARCHY_TYPE", "SEQUENCE"])
            .addCondition(Condition.eq("CHILD_CONTROL_ID", controlId))
            .addCondition(Condition.eq("STATUS", "ACTIVE"))
            .loadRegions(false)
            .build()
        
        def parentResult = Emery.dataobject.fetch(parentQuery)
        parentResult.dataObjects?.each { parent ->
            hierarchy.parentControls.add([
                controlId: parent.parent_control_id,
                hierarchyType: parent.hierarchy_type,
                sequence: parent.sequence
            ])
        }
        
        // Get child controls
        SelectQuery childQuery = new SelectQuery.Builder("MS_GRC_CONTROL_HIERARCHY")
            .columns(["PARENT_CONTROL_ID", "CHILD_CONTROL_ID", "HIERARCHY_TYPE", "SEQUENCE"])
            .addCondition(Condition.eq("PARENT_CONTROL_ID", controlId))
            .addCondition(Condition.eq("STATUS", "ACTIVE"))
            .loadRegions(false)
            .build()
        
        def childResult = Emery.dataobject.fetch(childQuery)
        childResult.dataObjects?.each { child ->
            hierarchy.childControls.add([
                controlId: child.child_control_id,
                hierarchyType: child.hierarchy_type,
                sequence: child.sequence
            ])
        }
        
        // Get related risks
        SelectQuery riskQuery = new SelectQuery.Builder("MS_GRC_REL_INSTANCE")
            .columns(["SRC_OBJ_ID", "DEST_OBJ_ID", "DEST_OBJ_TYPE", "REL_TYPE"])
            .addCondition(Condition.eq("DEST_OBJ_ID", controlId))
            .addCondition(Condition.eq("SRC_OBJ_TYPE", "RISK"))
            .addCondition(Condition.eq("STATUS", "ACTIVE"))
            .loadRegions(false)
            .build()
        
        def riskResult = Emery.dataobject.fetch(riskQuery)
        riskResult.dataObjects?.each { risk ->
            hierarchy.relatedRisks.add([
                riskId: risk.src_obj_id,
                relationType: risk.rel_type
            ])
        }
        
    } catch (Exception e) {
        Emery.log.error("Failed to analyze control hierarchy for {}: {}", controlId, e.message)
    }
    
    return hierarchy
}

// Main processing block
try {
    Emery.log.info("Starting main processing for control metric_id: {}", F.metric_id)
    
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
        currentStage: F.dd_current_stage
    ]
    
    // Initialize testing framework and managers
    def testingFramework = new ControlTestingFramework()
    def evidenceManager = new EvidenceManagementSystem()
    def deficiencyManager = new DeficiencyManager()
    def soxValidator = new SOXComplianceValidator()
    
    // Configure evidence requirements
    evidenceManager.setRequiredEvidence('PREVENTIVE', ['DOCUMENT', 'SYSTEM_REPORT', 'APPROVAL'])
    evidenceManager.setRequiredEvidence('DETECTIVE', ['SYSTEM_REPORT', 'LOG_FILE', 'SCREENSHOT'])
    evidenceManager.setRequiredEvidence('CORRECTIVE', ['DOCUMENT', 'EMAIL', 'APPROVAL'])
    
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
    
    /* Control testing and effectiveness assessment */
    if (F.object_id && F.object_id != '' && F.object_id != 'NONE') {
        // Create test executions
        def designTest = testingFramework.createTestExecution(F.object_id, 'DESIGN', [
            testerId: F.created_by,
            testerName: F.dd_current_user_name,
            sampleSize: testSampleSize
        ])
        
        def operatingTest = testingFramework.createTestExecution(F.object_id, 'OPERATING', [
            testerId: F.created_by,
            testerName: F.dd_current_user_name,
            sampleSize: testSampleSize
        ])
        
        // Simulate test execution (in real scenario, would be populated from form data)
        testingFramework.executeTest(designTest.executionId, [
            samplesTestedCount: testSampleSize,
            exceptionsFound: F.design_exceptions ?: 0,
            conclusion: F.design_conclusion ?: 'Test completed'
        ])
        
        testingFramework.executeTest(operatingTest.executionId, [
            samplesTestedCount: testSampleSize,
            exceptionsFound: F.operating_exceptions ?: 0,
            conclusion: F.operating_conclusion ?: 'Test completed'
        ])
        
        // Calculate overall effectiveness
        def effectivenessResult = testingFramework.calculateOverallEffectiveness(F.object_id)
        testExecutionContext.effectiveness = effectivenessResult
        
        // Update form fields
        F.design_effectiveness = effectivenessResult.rating
        F.overall_effectiveness_score = effectivenessResult.effectiveness * 100
        
        // Check if remediation is required
        if (effectivenessResult.effectiveness < effectivenessThreshold) {
            requiresRemediation = true
            
            // Create deficiency
            def deficiency = deficiencyManager.createDeficiency([
                controlId: F.object_id,
                title: "Control effectiveness below threshold",
                description: "Control ${F.object_name} has effectiveness of ${effectivenessResult.effectiveness * 100}% which is below the threshold of ${effectivenessThreshold * 100}%",
                classification: effectivenessResult.effectiveness < 0.5 ? 'SIGNIFICANT_DEFICIENCY' : 'CONTROL_DEFICIENCY',
                rootCause: F.deficiency_root_cause,
                identifiedBy: F.dd_current_user_name
            ])
            
            deficiencies.add(deficiency)
            
            // Create remediation plan
            def remPlan = deficiencyManager.createRemediationPlan(deficiency.deficiencyId, [
                title: "Remediation for ${F.object_name}",
                description: "Address control deficiency identified during testing",
                assignedTo: F.control_owner_id,
                assignedToName: F.control_owner_name,
                plannedEndDate: Emery.dateUtil.addDays(Emery.util.currentDate(), 90)
            ])
            
            remediationPlans.add(remPlan)
        }
        
        // SOX compliance validation
        def soxResult = soxValidator.validateSOXCompliance(F.object_id, F.control_type ?: 'FINANCIAL_REPORTING', [
            has_sod_controls: F.sod_controls_present == 'Y',
            has_authorization: F.authorization_required == 'Y',
            documentation_complete: F.documentation_status == 'COMPLETE',
            audit_trail_enabled: F.audit_trail_enabled == 'Y'
        ])
        
        testExecutionContext.soxCompliance = soxResult
        F.sox_compliant = soxResult.isCompliant ? 'Y' : 'N'
        F.sox_compliance_score = soxResult.complianceScore * 100
        
        // Analyze control hierarchy
        controlHierarchy = analyzeControlHierarchy(F.object_id)
        testExecutionContext.hierarchy = controlHierarchy
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
    
    // Store test results in processing context
    processingContext.testExecution = testExecutionContext
    processingContext.deficiencies = deficiencies
    processingContext.remediationPlans = remediationPlans
    processingContext.requiresRemediation = requiresRemediation
    
    // Final processing context update
    processingContext.endTime = System.currentTimeMillis()
    processingContext.duration = processingContext.endTime - processingContext.startTime
    
    Emery.log.info("Processing completed in {}ms. Requires remediation: {}", processingContext.duration, requiresRemediation)
    
} catch (Exception e) {
    Emery.log.error("Error in {}: {}", script_name, e.message)
    isRollbackRequired = true
    throw e
} finally {
    Emery.log.info("Script {} completed with rollback={}", script_name, isRollbackRequired)
}

