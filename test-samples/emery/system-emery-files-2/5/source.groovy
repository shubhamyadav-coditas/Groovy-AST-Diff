/**
 * Complex GRC Issue/Incident Processing Script - Create to Investigation Transition
 * Operations: CREATE_TO_INVESTIGATE transition with incident management workflow
 * i) Field updates: obj_status, obj_status_temp, owner_organizations, wfi_display, wfi_stored, dd_current_stage
 * ii) Incident classification and severity assessment
 * iii) Investigation workflow with timeline tracking
 * iv) Root cause analysis and corrective action management
 * v) Stakeholder notification and escalation handling
 * vi) SLA monitoring and compliance tracking
 **/

use("MS_GRC_ISSUE")
useDataObject("MS_GRC_REL_DEFN")
useDataObject("MS_GRC_ISSUE")
useDataObject("MS_GRC_INCIDENT_TIMELINE")
useDataObject("MS_GRC_ROOT_CAUSE")
useDataObject("MS_GRC_CORRECTIVE_ACTION")
useDataObject("MS_GRC_ESCALATION")
useDataObject("MS_GRC_SLA_CONFIG")
useDataObject("MS_GRC_STAKEHOLDER")

// Extended variable declarations for incident processing
String id, name, cur_date_string
Date current_date
String script_name = "IssueCreateToInvestigate_Advanced.groovy"
Map<String, Object> processingContext = [:]
Map<String, Object> investigationContext = [:]
Map<String, Object> slaContext = [:]
Map<String, List<String>> validationErrors = [:]
List<Map<String, Object>> auditTrailEntries = []
List<Map<String, Object>> timelineEvents = []
List<Map<String, Object>> stakeholderNotifications = []
List<Map<String, Object>> escalations = []
List<Map<String, Object>> correctiveActions = []
def rootCauseAnalysis = [:]
boolean isRollbackRequired = false
boolean requiresEscalation = false
boolean slaBreached = false
int escalationLevel = 0
final String DELIMITER = '\$_\$'
final String ISSUE_PREFIX = 'ISS_'
final String TIMELINE_PREFIX = 'TL_'
final String ACTION_PREFIX = 'CA_'
final int MAX_ESCALATION_LEVEL = 5
final int SLA_WARNING_THRESHOLD_HOURS = 24
final int SLA_BREACH_THRESHOLD_HOURS = 48

Emery.log.info("Started Pre-Hook {} at {}", script_name, Emery.util.currentDateAsString())

// Incident classification engine with severity scoring
class IncidentClassificationEngine {
    Map<String, Map<String, Object>> classificationRules = [:]
    Map<String, Integer> severityScores = [
        'CRITICAL': 100,
        'HIGH': 75,
        'MEDIUM': 50,
        'LOW': 25,
        'INFORMATIONAL': 10
    ]
    Map<String, Map<String, Object>> impactMatrix = [:]
    
    void registerClassificationRule(String ruleId, Map<String, Object> rule) {
        classificationRules[ruleId] = rule
    }
    
    void setImpactMatrix(String category, Map<String, Object> impacts) {
        impactMatrix[category] = impacts
    }
    
    Map<String, Object> classifyIncident(Map<String, Object> incidentData) {
        def classification = [
            incidentId: incidentData.incidentId,
            classifiedAt: Emery.util.currentDate(),
            category: null,
            subCategory: null,
            severity: 'MEDIUM',
            severityScore: 50,
            priority: 'P3',
            impactAssessment: [:],
            riskScore: 0,
            matchedRules: []
        ]
        
        // Apply classification rules
        classificationRules.each { ruleId, rule ->
            boolean ruleMatched = true
            
            rule.conditions?.each { condition ->
                def fieldValue = incidentData[condition.field]
                
                switch(condition.operator) {
                    case 'eq':
                        ruleMatched = ruleMatched && (fieldValue == condition.value)
                        break
                    case 'contains':
                        ruleMatched = ruleMatched && (fieldValue?.toString()?.toLowerCase()?.contains(condition.value?.toLowerCase()))
                        break
                    case 'in':
                        ruleMatched = ruleMatched && (condition.value?.contains(fieldValue))
                        break
                    case 'gt':
                        ruleMatched = ruleMatched && ((fieldValue as Double) > (condition.value as Double))
                        break
                }
            }
            
            if (ruleMatched) {
                classification.matchedRules.add(ruleId)
                if (rule.category) classification.category = rule.category
                if (rule.subCategory) classification.subCategory = rule.subCategory
                if (rule.severity) classification.severity = rule.severity
            }
        }
        
        // Calculate severity score
        classification.severityScore = severityScores[classification.severity] ?: 50
        
        // Assess impact
        if (classification.category && impactMatrix.containsKey(classification.category)) {
            classification.impactAssessment = impactMatrix[classification.category]
        }
        
        // Calculate risk score
        def likelihood = incidentData.likelihood_score ?: 50
        def impact = classification.impactAssessment.financial_impact ?: 50
        classification.riskScore = (likelihood * impact) / 100
        
        // Determine priority
        if (classification.severityScore >= 90 || classification.riskScore >= 80) {
            classification.priority = 'P1'
        } else if (classification.severityScore >= 70 || classification.riskScore >= 60) {
            classification.priority = 'P2'
        } else if (classification.severityScore >= 50 || classification.riskScore >= 40) {
            classification.priority = 'P3'
        } else {
            classification.priority = 'P4'
        }
        
        return classification
    }
    
    String recommendResponseTime(String priority) {
        switch(priority) {
            case 'P1': return '4 hours'
            case 'P2': return '8 hours'
            case 'P3': return '24 hours'
            case 'P4': return '72 hours'
            default: return '24 hours'
        }
    }
}

// Investigation workflow manager with state machine
class InvestigationWorkflowManager {
    Map<String, List<String>> stateTransitions = [
        'REPORTED': ['ACKNOWLEDGED', 'CLOSED_DUPLICATE'],
        'ACKNOWLEDGED': ['INVESTIGATING', 'CLOSED_NO_ACTION'],
        'INVESTIGATING': ['ROOT_CAUSE_IDENTIFIED', 'ESCALATED', 'PENDING_INFO'],
        'ROOT_CAUSE_IDENTIFIED': ['ACTION_PLANNING', 'ESCALATED'],
        'ACTION_PLANNING': ['ACTION_IN_PROGRESS', 'ESCALATED'],
        'ACTION_IN_PROGRESS': ['VERIFICATION', 'ESCALATED'],
        'VERIFICATION': ['CLOSED_RESOLVED', 'ACTION_IN_PROGRESS'],
        'PENDING_INFO': ['INVESTIGATING', 'CLOSED_NO_ACTION'],
        'ESCALATED': ['INVESTIGATING', 'CLOSED_RESOLVED']
    ]
    Map<String, List<Map<String, Object>>> workflowHistory = [:]
    Map<String, Map<String, Object>> activeInvestigations = [:]
    
    Map<String, Object> initializeInvestigation(String incidentId, Map<String, Object> initData) {
        def investigation = [
            investigationId: 'INV_' + System.currentTimeMillis(),
            incidentId: incidentId,
            currentState: 'REPORTED',
            previousState: null,
            assignedInvestigator: initData.investigator,
            assignedInvestigatorName: initData.investigatorName,
            team: initData.team ?: [],
            startDate: Emery.util.currentDate(),
            targetCompletionDate: initData.targetDate,
            actualCompletionDate: null,
            findings: [],
            evidence: [],
            interviews: [],
            rootCauses: [],
            status: 'ACTIVE',
            createdAt: Emery.util.currentDate()
        ]
        
        activeInvestigations[incidentId] = investigation
        
        recordTransition(incidentId, null, 'REPORTED', 'Investigation initiated', initData.initiatedBy)
        
        return investigation
    }
    
    Map<String, Object> transitionState(String incidentId, String newState, String reason, String performedBy) {
        def investigation = activeInvestigations[incidentId]
        if (!investigation) {
            return [success: false, error: 'Investigation not found']
        }
        
        def currentState = investigation.currentState
        def allowedTransitions = stateTransitions[currentState] ?: []
        
        if (!allowedTransitions.contains(newState)) {
            return [success: false, error: "Invalid transition from ${currentState} to ${newState}"]
        }
        
        investigation.previousState = currentState
        investigation.currentState = newState
        
        if (newState.startsWith('CLOSED')) {
            investigation.status = 'CLOSED'
            investigation.actualCompletionDate = Emery.util.currentDate()
        }
        
        recordTransition(incidentId, currentState, newState, reason, performedBy)
        
        return [success: true, investigation: investigation]
    }
    
    private void recordTransition(String incidentId, String fromState, String toState, String reason, String performedBy) {
        def transition = [
            transitionId: TIMELINE_PREFIX + System.currentTimeMillis(),
            incidentId: incidentId,
            fromState: fromState,
            toState: toState,
            reason: reason,
            performedBy: performedBy,
            performedAt: Emery.util.currentDate()
        ]
        
        if (!workflowHistory.containsKey(incidentId)) {
            workflowHistory[incidentId] = []
        }
        workflowHistory[incidentId].add(transition)
    }
    
    Map<String, Object> addFinding(String incidentId, Map<String, Object> finding) {
        def investigation = activeInvestigations[incidentId]
        if (!investigation) {
            return [success: false, error: 'Investigation not found']
        }
        
        def newFinding = [
            findingId: 'FND_' + System.currentTimeMillis(),
            title: finding.title,
            description: finding.description,
            category: finding.category,
            severity: finding.severity,
            evidence: finding.evidence ?: [],
            discoveredBy: finding.discoveredBy,
            discoveredDate: Emery.util.currentDate(),
            status: 'OPEN'
        ]
        
        investigation.findings.add(newFinding)
        return [success: true, finding: newFinding]
    }
    
    List<Map<String, Object>> getWorkflowHistory(String incidentId) {
        return workflowHistory[incidentId] ?: []
    }
    
    Map<String, Object> getInvestigation(String incidentId) {
        return activeInvestigations[incidentId]
    }
}

// Root cause analysis framework with Ishikawa/5-Why support
class RootCauseAnalyzer {
    List<String> ishikawaCategories = ['MAN', 'MACHINE', 'METHOD', 'MATERIAL', 'MEASUREMENT', 'ENVIRONMENT']
    Map<String, List<Map<String, Object>>> rootCausesByIncident = [:]
    
    Map<String, Object> performFiveWhyAnalysis(String incidentId, String problemStatement, List<String> whys) {
        def analysis = [
            analysisId: 'RCA_' + System.currentTimeMillis(),
            incidentId: incidentId,
            methodType: 'FIVE_WHY',
            problemStatement: problemStatement,
            whyChain: [],
            rootCause: null,
            confidence: 0.0,
            analyzedBy: null,
            analyzedAt: Emery.util.currentDate()
        ]
        
        whys.eachWithIndex { why, index ->
            analysis.whyChain.add([
                level: index + 1,
                question: "Why #${index + 1}",
                answer: why,
                isRootCause: index == whys.size() - 1
            ])
        }
        
        if (whys.size() > 0) {
            analysis.rootCause = whys.last()
            analysis.confidence = Math.min(1.0, whys.size() / 5.0)
        }
        
        if (!rootCausesByIncident.containsKey(incidentId)) {
            rootCausesByIncident[incidentId] = []
        }
        rootCausesByIncident[incidentId].add(analysis)
        
        return analysis
    }
    
    Map<String, Object> performIshikawaAnalysis(String incidentId, String effectStatement, Map<String, List<String>> causesByCategory) {
        def analysis = [
            analysisId: 'RCA_' + System.currentTimeMillis(),
            incidentId: incidentId,
            methodType: 'ISHIKAWA',
            effectStatement: effectStatement,
            categories: [:],
            primaryCauses: [],
            analyzedAt: Emery.util.currentDate()
        ]
        
        ishikawaCategories.each { category ->
            def causes = causesByCategory[category] ?: []
            analysis.categories[category] = [
                causes: causes,
                causeCount: causes.size(),
                hasCauses: causes.size() > 0
            ]
            
            if (causes.size() > 0) {
                analysis.primaryCauses.addAll(causes.collect { [category: category, cause: it] })
            }
        }
        
        // Identify most significant category
        def maxCategoryEntry = analysis.categories.max { it.value.causeCount }
        analysis.dominantCategory = maxCategoryEntry?.key
        
        if (!rootCausesByIncident.containsKey(incidentId)) {
            rootCausesByIncident[incidentId] = []
        }
        rootCausesByIncident[incidentId].add(analysis)
        
        return analysis
    }
    
    List<Map<String, Object>> getRootCauses(String incidentId) {
        return rootCausesByIncident[incidentId] ?: []
    }
}

// SLA monitoring and breach detection
class SLAMonitor {
    Map<String, Map<String, Object>> slaConfigs = [:]
    Map<String, List<Map<String, Object>>> slaTracking = [:]
    
    void registerSLAConfig(String priority, Map<String, Object> config) {
        slaConfigs[priority] = config
    }
    
    Map<String, Object> initializeSLATracking(String incidentId, String priority, Date reportedDate) {
        def config = slaConfigs[priority] ?: slaConfigs['DEFAULT']
        if (!config) {
            config = [responseHours: 24, resolutionHours: 72, escalationHours: 48]
        }
        
        def tracking = [
            trackingId: 'SLA_' + System.currentTimeMillis(),
            incidentId: incidentId,
            priority: priority,
            reportedDate: reportedDate,
            responseTargetDate: Emery.dateUtil.addHours(reportedDate, config.responseHours),
            resolutionTargetDate: Emery.dateUtil.addHours(reportedDate, config.resolutionHours),
            escalationTargetDate: Emery.dateUtil.addHours(reportedDate, config.escalationHours),
            responseActualDate: null,
            resolutionActualDate: null,
            responseSLAMet: null,
            resolutionSLAMet: null,
            currentStatus: 'TRACKING',
            breachCount: 0,
            warningCount: 0,
            createdAt: Emery.util.currentDate()
        ]
        
        if (!slaTracking.containsKey(incidentId)) {
            slaTracking[incidentId] = []
        }
        slaTracking[incidentId].add(tracking)
        
        return tracking
    }
    
    Map<String, Object> checkSLAStatus(String incidentId) {
        def tracking = slaTracking[incidentId]?.find { it.currentStatus == 'TRACKING' }
        if (!tracking) {
            return [status: 'NOT_TRACKED', incidentId: incidentId]
        }
        
        def now = Emery.util.currentDate()
        def status = [
            incidentId: incidentId,
            trackingId: tracking.trackingId,
            checkedAt: now,
            responseStatus: 'ON_TRACK',
            resolutionStatus: 'ON_TRACK',
            overallStatus: 'GREEN',
            hoursToResponseDeadline: 0,
            hoursToResolutionDeadline: 0,
            breached: false,
            warning: false
        ]
        
        // Check response SLA
        if (tracking.responseActualDate == null) {
            def hoursToResponse = Emery.dateUtil.hoursBetween(now, tracking.responseTargetDate)
            status.hoursToResponseDeadline = hoursToResponse
            
            if (hoursToResponse < 0) {
                status.responseStatus = 'BREACHED'
                status.breached = true
                tracking.breachCount++
            } else if (hoursToResponse <= SLA_WARNING_THRESHOLD_HOURS) {
                status.responseStatus = 'WARNING'
                status.warning = true
                tracking.warningCount++
            }
        } else {
            status.responseStatus = tracking.responseSLAMet ? 'MET' : 'BREACHED'
        }
        
        // Check resolution SLA
        if (tracking.resolutionActualDate == null) {
            def hoursToResolution = Emery.dateUtil.hoursBetween(now, tracking.resolutionTargetDate)
            status.hoursToResolutionDeadline = hoursToResolution
            
            if (hoursToResolution < 0) {
                status.resolutionStatus = 'BREACHED'
                status.breached = true
                tracking.breachCount++
            } else if (hoursToResolution <= SLA_WARNING_THRESHOLD_HOURS) {
                status.resolutionStatus = 'WARNING'
                status.warning = true
            }
        } else {
            status.resolutionStatus = tracking.resolutionSLAMet ? 'MET' : 'BREACHED'
        }
        
        // Determine overall status
        if (status.breached) {
            status.overallStatus = 'RED'
        } else if (status.warning) {
            status.overallStatus = 'AMBER'
        }
        
        return status
    }
    
    Map<String, Object> recordResponse(String incidentId, Date responseDate) {
        def tracking = slaTracking[incidentId]?.find { it.currentStatus == 'TRACKING' }
        if (!tracking) {
            return [success: false, error: 'Tracking not found']
        }
        
        tracking.responseActualDate = responseDate
        tracking.responseSLAMet = Emery.dateUtil.compareDates(responseDate, tracking.responseTargetDate) <= 0
        
        return [success: true, slamet: tracking.responseSLAMet]
    }
    
    Map<String, Object> recordResolution(String incidentId, Date resolutionDate) {
        def tracking = slaTracking[incidentId]?.find { it.currentStatus == 'TRACKING' }
        if (!tracking) {
            return [success: false, error: 'Tracking not found']
        }
        
        tracking.resolutionActualDate = resolutionDate
        tracking.resolutionSLAMet = Emery.dateUtil.compareDates(resolutionDate, tracking.resolutionTargetDate) <= 0
        tracking.currentStatus = 'COMPLETED'
        
        return [success: true, slaMet: tracking.resolutionSLAMet]
    }
}

// Corrective action tracker
class CorrectiveActionTracker {
    Map<String, List<Map<String, Object>>> actionsByIncident = [:]
    List<String> actionTypes = ['IMMEDIATE', 'SHORT_TERM', 'LONG_TERM', 'PREVENTIVE', 'DETECTIVE', 'CORRECTIVE']
    
    Map<String, Object> createAction(String incidentId, Map<String, Object> actionData) {
        def action = [
            actionId: ACTION_PREFIX + System.currentTimeMillis(),
            incidentId: incidentId,
            title: actionData.title,
            description: actionData.description,
            actionType: actionData.actionType ?: 'CORRECTIVE',
            rootCauseRef: actionData.rootCauseRef,
            assignedTo: actionData.assignedTo,
            assignedToName: actionData.assignedToName,
            plannedStartDate: actionData.plannedStartDate ?: Emery.util.currentDate(),
            plannedEndDate: actionData.plannedEndDate,
            actualStartDate: null,
            actualEndDate: null,
            status: 'PLANNED',
            progressPercent: 0,
            milestones: actionData.milestones ?: [],
            verificationRequired: actionData.verificationRequired ?: true,
            verificationCriteria: actionData.verificationCriteria,
            verified: false,
            verifiedBy: null,
            verifiedDate: null,
            effectiveness: null,
            createdAt: Emery.util.currentDate()
        ]
        
        if (!actionsByIncident.containsKey(incidentId)) {
            actionsByIncident[incidentId] = []
        }
        actionsByIncident[incidentId].add(action)
        
        return action
    }
    
    Map<String, Object> updateProgress(String actionId, int progressPercent, String status) {
        for (def actions : actionsByIncident.values()) {
            def action = actions.find { it.actionId == actionId }
            if (action) {
                action.progressPercent = progressPercent
                action.status = status
                
                if (status == 'IN_PROGRESS' && action.actualStartDate == null) {
                    action.actualStartDate = Emery.util.currentDate()
                }
                
                if (status == 'COMPLETED') {
                    action.actualEndDate = Emery.util.currentDate()
                    action.progressPercent = 100
                }
                
                return [success: true, action: action]
            }
        }
        return [success: false, error: 'Action not found']
    }
    
    Map<String, Object> verifyAction(String actionId, String verifiedBy, String effectiveness) {
        for (def actions : actionsByIncident.values()) {
            def action = actions.find { it.actionId == actionId }
            if (action) {
                action.verified = true
                action.verifiedBy = verifiedBy
                action.verifiedDate = Emery.util.currentDate()
                action.effectiveness = effectiveness
                action.status = 'VERIFIED'
                
                return [success: true, action: action]
            }
        }
        return [success: false, error: 'Action not found']
    }
    
    Map<String, Object> getActionSummary(String incidentId) {
        def actions = actionsByIncident[incidentId] ?: []
        
        return [
            total: actions.size(),
            byStatus: actions.groupBy { it.status }.collectEntries { k, v -> [k, v.size()] },
            byType: actions.groupBy { it.actionType }.collectEntries { k, v -> [k, v.size()] },
            completedCount: actions.count { it.status == 'COMPLETED' || it.status == 'VERIFIED' },
            overdueCount: actions.count { 
                it.status != 'COMPLETED' && it.status != 'VERIFIED' && 
                it.plannedEndDate && Emery.dateUtil.compareDates(Emery.util.currentDate(), it.plannedEndDate) > 0
            },
            averageProgress: actions.size() > 0 ? actions.sum { it.progressPercent } / actions.size() : 0
        ]
    }
    
    List<Map<String, Object>> getActions(String incidentId) {
        return actionsByIncident[incidentId] ?: []
    }
}

// Escalation manager
def escalationManager = {
    def escalationHistory = [:]
    def escalationRules = [:]
    
    // Register escalation rule
    def registerRule = { String ruleId, Map<String, Object> rule ->
        escalationRules[ruleId] = rule
    }
    
    // Check if escalation is needed
    def checkEscalation = { String incidentId, Map<String, Object> incidentData ->
        def escalationNeeded = false
        def matchedRules = []
        
        escalationRules.each { ruleId, rule ->
            boolean ruleMatched = true
            
            rule.conditions?.each { condition ->
                def fieldValue = incidentData[condition.field]
                
                switch(condition.operator) {
                    case 'eq':
                        ruleMatched = ruleMatched && (fieldValue == condition.value)
                        break
                    case 'gt':
                        ruleMatched = ruleMatched && ((fieldValue as Double) > (condition.value as Double))
                        break
                    case 'gte':
                        ruleMatched = ruleMatched && ((fieldValue as Double) >= (condition.value as Double))
                        break
                }
            }
            
            if (ruleMatched) {
                escalationNeeded = true
                matchedRules.add([ruleId: ruleId, escalateTo: rule.escalateTo, reason: rule.reason])
            }
        }
        
        return [needed: escalationNeeded, rules: matchedRules]
    }
    
    // Create escalation
    def createEscalation = { String incidentId, Map<String, Object> escalationData ->
        def escalation = [
            escalationId: 'ESC_' + System.currentTimeMillis(),
            incidentId: incidentId,
            level: escalationData.level ?: 1,
            escalatedTo: escalationData.escalatedTo,
            escalatedToName: escalationData.escalatedToName,
            reason: escalationData.reason,
            triggeredBy: escalationData.triggeredBy,
            triggeredRule: escalationData.triggeredRule,
            escalatedAt: Emery.util.currentDate(),
            acknowledgedAt: null,
            resolvedAt: null,
            status: 'PENDING'
        ]
        
        if (!escalationHistory.containsKey(incidentId)) {
            escalationHistory[incidentId] = []
        }
        escalationHistory[incidentId].add(escalation)
        
        return escalation
    }
    
    // Get escalation history
    def getHistory = { String incidentId ->
        return escalationHistory[incidentId] ?: []
    }
    
    return [
        registerRule: registerRule,
        checkEscalation: checkEscalation,
        createEscalation: createEscalation,
        getHistory: getHistory
    ]
}

// Initialize escalation manager
def escManager = escalationManager()

// Main processing block
try {
    Emery.log.info("Starting main processing for issue metric_id: {}", F.metric_id)
    
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
    
    // Initialize managers
    def classificationEngine = new IncidentClassificationEngine()
    def investigationManager = new InvestigationWorkflowManager()
    def rootCauseAnalyzer = new RootCauseAnalyzer()
    def slaMonitor = new SLAMonitor()
    def actionTracker = new CorrectiveActionTracker()
    
    // Configure classification rules
    classificationEngine.registerClassificationRule('SECURITY_BREACH', [
        category: 'SECURITY',
        subCategory: 'DATA_BREACH',
        severity: 'CRITICAL',
        conditions: [
            [field: 'incident_type', operator: 'eq', value: 'SECURITY_BREACH'],
            [field: 'data_exposed', operator: 'eq', value: 'Y']
        ]
    ])
    
    classificationEngine.registerClassificationRule('SYSTEM_OUTAGE', [
        category: 'OPERATIONAL',
        subCategory: 'SYSTEM_FAILURE',
        severity: 'HIGH',
        conditions: [
            [field: 'incident_type', operator: 'eq', value: 'SYSTEM_OUTAGE'],
            [field: 'affected_users', operator: 'gt', value: 100]
        ]
    ])
    
    // Configure SLA
    slaMonitor.registerSLAConfig('P1', [responseHours: 4, resolutionHours: 24, escalationHours: 8])
    slaMonitor.registerSLAConfig('P2', [responseHours: 8, resolutionHours: 48, escalationHours: 24])
    slaMonitor.registerSLAConfig('P3', [responseHours: 24, resolutionHours: 72, escalationHours: 48])
    slaMonitor.registerSLAConfig('P4', [responseHours: 48, resolutionHours: 168, escalationHours: 72])
    
    // Configure escalation rules
    escManager.registerRule('SLA_BREACH', [
        conditions: [[field: 'sla_breached', operator: 'eq', value: true]],
        escalateTo: 'MANAGER',
        reason: 'SLA has been breached'
    ])
    
    escManager.registerRule('HIGH_SEVERITY', [
        conditions: [[field: 'severity_score', operator: 'gte', value: 75]],
        escalateTo: 'SENIOR_MANAGEMENT',
        reason: 'High severity incident requires senior attention'
    ])
    
    /* Non-multirow field updates */
    
    // Classify incident
    def classification = classificationEngine.classifyIncident([
        incidentId: F.object_id,
        incident_type: F.incident_type,
        data_exposed: F.data_exposed,
        affected_users: F.affected_users ?: 0,
        likelihood_score: F.likelihood_score ?: 50
    ])
    
    F.incident_category = classification.category
    F.incident_subcategory = classification.subCategory
    F.severity = classification.severity
    F.severity_score = classification.severityScore
    F.priority = classification.priority
    F.risk_score = classification.riskScore
    
    investigationContext.classification = classification
    
    // Object status update
    if (F.object_action == 'START_INVESTIGATION') {
        F.obj_status = 'INVESTIGATING'
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
    F.dd_current_stage = 'INVESTIGATE'
    
    // Comment insertion
    if (F.action_comments != null && F.action_comments.toString().trim() != '') {
        String result = Emery.util.insertComments(F.process_instance_id as int, F.instance_id as int, F.metric_id as int, F.created_by as int, "ACTION_COMMENTS", F.action_comments as String)
        F.action_comments = ''
    }
    
    // Initialize investigation
    if (F.object_id && F.object_id != '' && F.object_id != 'NONE') {
        def investigation = investigationManager.initializeInvestigation(F.object_id, [
            investigator: F.assigned_investigator_id,
            investigatorName: F.assigned_investigator_name,
            team: F.investigation_team?.split(',')?.collect { it.trim() } ?: [],
            targetDate: F.target_completion_date,
            initiatedBy: F.dd_current_user_name
        ])
        
        investigationContext.investigation = investigation
        
        // Initialize SLA tracking
        def slaTracking = slaMonitor.initializeSLATracking(F.object_id, classification.priority, F.reported_date ?: Emery.util.currentDate())
        slaContext.tracking = slaTracking
        
        // Check SLA status
        def slaStatus = slaMonitor.checkSLAStatus(F.object_id)
        slaContext.status = slaStatus
        slaBreached = slaStatus.breached
        
        // Check escalation
        def escalationCheck = escManager.checkEscalation(F.object_id, [
            sla_breached: slaBreached,
            severity_score: classification.severityScore
        ])
        
        if (escalationCheck.needed) {
            requiresEscalation = true
            escalationCheck.rules.each { rule ->
                def escalation = escManager.createEscalation(F.object_id, [
                    level: escalationLevel + 1,
                    escalatedTo: rule.escalateTo,
                    reason: rule.reason,
                    triggeredBy: F.dd_current_user_name,
                    triggeredRule: rule.ruleId
                ])
                escalations.add(escalation)
            }
            escalationLevel++
        }
    }
    
    /* Apps_ID Creation */
    if (F.object_id == '' || F.object_id == 'NONE') {
        id = Emery.util.nextAppsId("MS_GRC_ISSUE", "GRC", "MS_GRC_ISSUE_IDGEN")
        F.object_id = id
    }
    
    /* Multirow processing */
    F.orb.rows.each { row ->
        if (row.rel_inst_id == '' || row.rel_inst_id == 'NONE') {
            id = Emery.util.nextAppsId("MS_GRC_ISSUE", "GRC", "MS_ORB_REL_INST_ID")
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
        id = Emery.util.nextAppsId("MS_GRC_ISSUE", "GRC", "MS_ORB_REL_SOURCE_ID")
        F.rel_source_id = id
    }
    
    if (F.rel_source_object_id == '' || F.rel_source_object_id == 'NONE') {
        F.rel_source_object_id = F.object_id
    }
    
    // App config handling
    if (F.own_app_config_temp == 'Yes' || F.own_app_config_temp == 'No') {
        F.own_app_config_temp = F.own_app_config
    }
    
    // Store contexts
    processingContext.investigation = investigationContext
    processingContext.sla = slaContext
    processingContext.escalations = escalations
    processingContext.requiresEscalation = requiresEscalation
    processingContext.slaBreached = slaBreached
    
    // Final processing context update
    processingContext.endTime = System.currentTimeMillis()
    processingContext.duration = processingContext.endTime - processingContext.startTime
    
    Emery.log.info("Processing completed in {}ms. Escalation required: {}, SLA breached: {}", 
        processingContext.duration, requiresEscalation, slaBreached)
    
} catch (Exception e) {
    Emery.log.error("Error in {}: {}", script_name, e.message)
    isRollbackRequired = true
    throw e
} finally {
    Emery.log.info("Script {} completed with rollback={}", script_name, isRollbackRequired)
}

