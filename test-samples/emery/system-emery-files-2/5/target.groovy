/**
 * Enhanced GRC Issue/Incident Processing Script with Upload Type and Advanced Analytics
 * Operations: CREATE_TO_INVESTIGATE transition with ML-assisted classification and predictive analytics
 * i) Upload type conditional processing with creator assignment
 * ii) ML-based incident classification and pattern recognition
 * iii) Predictive SLA breach detection
 * iv) Advanced stakeholder communication with templating
 * v) Incident correlation and trend analysis
 * vi) Automated remediation recommendation engine
 **/

use("MS_GRC_ISSUE")
useDataObject("MS_GRC_REL_DEFN")
useDataObject("MS_GRC_ISSUE")
useDataObject("MS_GRC_INCIDENT_TIMELINE")
useDataObject("MS_GRC_ROOT_CAUSE")
useDataObject("MS_GRC_CORRECTIVE_ACTION")
useDataObject("MS_GRC_INCIDENT_PATTERN")
useDataObject("MS_GRC_SIMILAR_INCIDENT")
useDataObject("MS_GRC_COMMUNICATION_LOG")

// Extended variable declarations
String id, name, cur_date_string
Date current_date
String script_name = "IssueCreateToInvestigate_MLEnhanced.groovy"
Map<String, Object> processingContext = [:]
Map<String, Object> mlContext = [:]
Map<String, Object> correlationContext = [:]
Map<String, Object> communicationContext = [:]
Map<String, List<String>> validationErrors = [:]
List<Map<String, Object>> auditTrailEntries = []
List<Map<String, Object>> similarIncidents = []
List<Map<String, Object>> patternMatches = []
List<Map<String, Object>> recommendations = []
List<Map<String, Object>> communications = []
def incidentCorrelations = []
boolean isRollbackRequired = false
boolean patternDetected = false
boolean requiresUrgentAttention = false
int similarIncidentCount = 0
double mlConfidenceScore = 0.0
final String DELIMITER = '\$_\$'
final String ISSUE_PREFIX = 'ISS_'
final String PATTERN_PREFIX = 'PAT_'
final String COMM_PREFIX = 'COM_'
final double SIMILARITY_THRESHOLD = 0.75
final int MAX_SIMILAR_INCIDENTS = 10

Emery.log.info("Started Pre-Hook {} at {}", script_name, Emery.util.currentDateAsString())

// ML-assisted incident classification with pattern matching
class MLIncidentClassifier {
    Map<String, List<Map<String, Object>>> trainingData = [:]
    Map<String, Map<String, Double>> featureWeights = [:]
    Map<String, List<String>> categoryKeywords = [:]
    double confidenceThreshold = 0.7
    
    void loadTrainingData(String category, List<Map<String, Object>> data) {
        trainingData[category] = data
    }
    
    void setFeatureWeights(String category, Map<String, Double> weights) {
        featureWeights[category] = weights
    }
    
    void setCategoryKeywords(String category, List<String> keywords) {
        categoryKeywords[category] = keywords
    }
    
    Map<String, Object> classifyWithML(Map<String, Object> incidentData) {
        def result = [
            incidentId: incidentData.incidentId,
            classifiedAt: Emery.util.currentDate(),
            predictions: [],
            topPrediction: null,
            confidence: 0.0,
            features: [:],
            explanation: []
        ]
        
        // Extract features
        def features = extractFeatures(incidentData)
        result.features = features
        
        // Calculate scores for each category
        def categoryScores = [:]
        categoryKeywords.each { category, keywords ->
            double score = calculateCategoryScore(incidentData, features, keywords, featureWeights[category] ?: [:])
            categoryScores[category] = score
            
            result.predictions.add([
                category: category,
                score: score,
                confidence: score / 100.0
            ])
        }
        
        // Sort predictions by score
        result.predictions.sort { -it.score }
        
        if (result.predictions.size() > 0) {
            result.topPrediction = result.predictions[0]
            result.confidence = result.topPrediction.confidence
            
            // Generate explanation
            if (result.confidence >= confidenceThreshold) {
                result.explanation.add("High confidence classification based on keyword matching")
                result.explanation.add("Top features: ${features.topFeatures?.take(3)?.join(', ')}")
            } else {
                result.explanation.add("Low confidence - manual review recommended")
            }
        }
        
        return result
    }
    
    private Map<String, Object> extractFeatures(Map<String, Object> incidentData) {
        def features = [
            textLength: (incidentData.description?.toString()?.length() ?: 0),
            hasAttachments: incidentData.attachments != null && incidentData.attachments.size() > 0,
            urgencyIndicators: 0,
            technicalTerms: 0,
            affectedSystems: incidentData.affected_systems?.split(',')?.size() ?: 0,
            topFeatures: []
        ]
        
        def urgentKeywords = ['urgent', 'critical', 'immediate', 'emergency', 'asap', 'down', 'outage']
        def description = incidentData.description?.toString()?.toLowerCase() ?: ''
        
        urgentKeywords.each { keyword ->
            if (description.contains(keyword)) {
                features.urgencyIndicators++
                features.topFeatures.add(keyword)
            }
        }
        
        return features
    }
    
    private double calculateCategoryScore(Map<String, Object> incidentData, Map<String, Object> features, List<String> keywords, Map<String, Double> weights) {
        double score = 0.0
        def description = incidentData.description?.toString()?.toLowerCase() ?: ''
        def title = incidentData.title?.toString()?.toLowerCase() ?: ''
        def combinedText = title + ' ' + description
        
        keywords.each { keyword ->
            if (combinedText.contains(keyword.toLowerCase())) {
                score += weights[keyword] ?: 10.0
            }
        }
        
        // Boost for urgency indicators
        score += features.urgencyIndicators * 5
        
        // Normalize score to 0-100
        return Math.min(100, score)
    }
    
    Map<String, Object> findSimilarIncidents(Map<String, Object> currentIncident, List<Map<String, Object>> historicalIncidents) {
        def similarities = []
        
        historicalIncidents.each { historical ->
            double similarity = calculateSimilarity(currentIncident, historical)
            if (similarity >= SIMILARITY_THRESHOLD) {
                similarities.add([
                    incidentId: historical.incidentId,
                    title: historical.title,
                    category: historical.category,
                    similarity: similarity,
                    resolution: historical.resolution,
                    resolvedDate: historical.resolvedDate
                ])
            }
        }
        
        similarities.sort { -it.similarity }
        
        return [
            currentIncidentId: currentIncident.incidentId,
            similarIncidents: similarities.take(MAX_SIMILAR_INCIDENTS),
            totalSimilar: similarities.size(),
            highestSimilarity: similarities.size() > 0 ? similarities[0].similarity : 0.0
        ]
    }
    
    private double calculateSimilarity(Map<String, Object> incident1, Map<String, Object> incident2) {
        // Simple Jaccard similarity on words
        def words1 = (incident1.description?.toString()?.toLowerCase()?.split(/\s+/) ?: []) as Set
        def words2 = (incident2.description?.toString()?.toLowerCase()?.split(/\s+/) ?: []) as Set
        
        if (words1.isEmpty() || words2.isEmpty()) return 0.0
        
        def intersection = words1.intersect(words2)
        def union = words1 + words2
        
        return intersection.size() / union.size()
    }
}

// Incident pattern detector for recurring issues
class IncidentPatternDetector {
    Map<String, List<Map<String, Object>>> patternLibrary = [:]
    List<Map<String, Object>> detectedPatterns = []
    int minOccurrences = 3
    int timeWindowDays = 30
    
    void registerPattern(String patternId, Map<String, Object> pattern) {
        if (!patternLibrary.containsKey(pattern.category)) {
            patternLibrary[pattern.category] = []
        }
        patternLibrary[pattern.category].add([
            patternId: patternId,
            name: pattern.name,
            description: pattern.description,
            indicators: pattern.indicators ?: [],
            severity: pattern.severity ?: 'MEDIUM',
            recommendedActions: pattern.recommendedActions ?: []
        ])
    }
    
    Map<String, Object> detectPatterns(Map<String, Object> currentIncident, List<Map<String, Object>> recentIncidents) {
        def result = [
            incidentId: currentIncident.incidentId,
            analyzedAt: Emery.util.currentDate(),
            patternsDetected: [],
            isRecurring: false,
            recurringCount: 0,
            recommendations: []
        ]
        
        def category = currentIncident.category
        def categoryPatterns = patternLibrary[category] ?: []
        
        categoryPatterns.each { pattern ->
            int matchScore = 0
            def matchedIndicators = []
            
            pattern.indicators.each { indicator ->
                if (matchesIndicator(currentIncident, indicator)) {
                    matchScore++
                    matchedIndicators.add(indicator)
                }
            }
            
            if (matchScore >= pattern.indicators.size() * 0.6) {
                result.patternsDetected.add([
                    patternId: pattern.patternId,
                    patternName: pattern.name,
                    matchScore: matchScore,
                    totalIndicators: pattern.indicators.size(),
                    matchedIndicators: matchedIndicators,
                    severity: pattern.severity
                ])
                result.recommendations.addAll(pattern.recommendedActions)
            }
        }
        
        // Check for recurring incidents
        def similarRecentIncidents = recentIncidents.findAll { incident ->
            incident.category == currentIncident.category &&
            (currentIncident.affected_system == null || incident.affected_system == currentIncident.affected_system)
        }
        
        if (similarRecentIncidents.size() >= minOccurrences) {
            result.isRecurring = true
            result.recurringCount = similarRecentIncidents.size()
            result.recommendations.add("Pattern detected: ${similarRecentIncidents.size()} similar incidents in the last ${timeWindowDays} days")
            result.recommendations.add("Consider investigating root cause for systemic fix")
        }
        
        detectedPatterns.addAll(result.patternsDetected)
        
        return result
    }
    
    private boolean matchesIndicator(Map<String, Object> incident, Map<String, Object> indicator) {
        def fieldValue = incident[indicator.field]
        
        switch(indicator.matchType) {
            case 'exact':
                return fieldValue == indicator.value
            case 'contains':
                return fieldValue?.toString()?.toLowerCase()?.contains(indicator.value?.toLowerCase())
            case 'regex':
                return fieldValue?.toString()?.matches(indicator.value)
            case 'in':
                return indicator.value?.contains(fieldValue)
            default:
                return false
        }
    }
    
    List<Map<String, Object>> getDetectedPatterns() {
        return detectedPatterns.collect()
    }
}

// Predictive SLA breach detector
class PredictiveSLAAnalyzer {
    Map<String, Map<String, Object>> historicalMetrics = [:]
    double breachPredictionThreshold = 0.7
    
    void loadHistoricalMetrics(String priority, Map<String, Object> metrics) {
        historicalMetrics[priority] = metrics
    }
    
    Map<String, Object> predictBreachProbability(Map<String, Object> incidentData) {
        def prediction = [
            incidentId: incidentData.incidentId,
            priority: incidentData.priority,
            predictedAt: Emery.util.currentDate(),
            responseBreachProbability: 0.0,
            resolutionBreachProbability: 0.0,
            riskFactors: [],
            recommendations: []
        ]
        
        def historicalData = historicalMetrics[incidentData.priority]
        if (!historicalData) {
            prediction.confidence = 0.0
            prediction.message = 'Insufficient historical data for prediction'
            return prediction
        }
        
        // Calculate breach probability based on various factors
        double baseBreachRate = historicalData.breachRate ?: 0.1
        double complexityMultiplier = calculateComplexityMultiplier(incidentData)
        double workloadMultiplier = calculateWorkloadMultiplier(incidentData)
        
        prediction.responseBreachProbability = Math.min(1.0, baseBreachRate * complexityMultiplier * workloadMultiplier)
        prediction.resolutionBreachProbability = Math.min(1.0, prediction.responseBreachProbability * 1.5)
        
        // Identify risk factors
        if (incidentData.affected_systems?.split(',')?.size() > 3) {
            prediction.riskFactors.add([factor: 'Multiple systems affected', impact: 'HIGH'])
        }
        if (incidentData.severity_score >= 75) {
            prediction.riskFactors.add([factor: 'High severity incident', impact: 'HIGH'])
        }
        if (complexityMultiplier > 1.5) {
            prediction.riskFactors.add([factor: 'Complex incident characteristics', impact: 'MEDIUM'])
        }
        
        // Generate recommendations
        if (prediction.responseBreachProbability >= breachPredictionThreshold) {
            prediction.recommendations.add('High probability of SLA breach - escalate immediately')
            prediction.recommendations.add('Consider assigning additional resources')
        } else if (prediction.responseBreachProbability >= 0.5) {
            prediction.recommendations.add('Moderate breach risk - monitor closely')
            prediction.recommendations.add('Ensure investigator availability')
        }
        
        prediction.overallRisk = prediction.responseBreachProbability >= breachPredictionThreshold ? 'HIGH' :
            prediction.responseBreachProbability >= 0.5 ? 'MEDIUM' : 'LOW'
        
        return prediction
    }
    
    private double calculateComplexityMultiplier(Map<String, Object> incidentData) {
        double multiplier = 1.0
        
        if (incidentData.affected_systems?.split(',')?.size() > 3) multiplier += 0.3
        if (incidentData.description?.toString()?.length() > 1000) multiplier += 0.2
        if (incidentData.has_dependencies == 'Y') multiplier += 0.25
        
        return multiplier
    }
    
    private double calculateWorkloadMultiplier(Map<String, Object> incidentData) {
        // In real implementation, would check investigator workload
        return 1.0 + (Math.random() * 0.3)
    }
}

// Automated remediation recommendation engine
class RemediationRecommendationEngine {
    Map<String, List<Map<String, Object>>> remediationPlaybooks = [:]
    Map<String, List<String>> categoryRemediations = [:]
    
    void registerPlaybook(String playbookId, Map<String, Object> playbook) {
        def category = playbook.category
        if (!remediationPlaybooks.containsKey(category)) {
            remediationPlaybooks[category] = []
        }
        remediationPlaybooks[category].add([
            playbookId: playbookId,
            name: playbook.name,
            description: playbook.description,
            steps: playbook.steps ?: [],
            estimatedDuration: playbook.estimatedDuration,
            requiredSkills: playbook.requiredSkills ?: [],
            automationLevel: playbook.automationLevel ?: 'MANUAL'
        ])
    }
    
    Map<String, Object> generateRecommendations(Map<String, Object> incidentData, Map<String, Object> mlClassification, List<Map<String, Object>> patterns) {
        def result = [
            incidentId: incidentData.incidentId,
            generatedAt: Emery.util.currentDate(),
            primaryRecommendations: [],
            alternativeApproaches: [],
            estimatedResolutionTime: null,
            automationOpportunities: [],
            requiredResources: []
        ]
        
        def category = mlClassification?.topPrediction?.category ?: incidentData.category
        def playbooks = remediationPlaybooks[category] ?: []
        
        // Match playbooks based on patterns and characteristics
        playbooks.each { playbook ->
            def relevanceScore = calculatePlaybookRelevance(incidentData, playbook, patterns)
            
            if (relevanceScore >= 0.6) {
                result.primaryRecommendations.add([
                    playbookId: playbook.playbookId,
                    playbookName: playbook.name,
                    relevanceScore: relevanceScore,
                    steps: playbook.steps,
                    estimatedDuration: playbook.estimatedDuration
                ])
            } else if (relevanceScore >= 0.3) {
                result.alternativeApproaches.add([
                    playbookId: playbook.playbookId,
                    playbookName: playbook.name,
                    relevanceScore: relevanceScore
                ])
            }
            
            if (playbook.automationLevel in ['FULL', 'PARTIAL']) {
                result.automationOpportunities.add([
                    playbookId: playbook.playbookId,
                    automationLevel: playbook.automationLevel
                ])
            }
            
            result.requiredResources.addAll(playbook.requiredSkills)
        }
        
        result.primaryRecommendations.sort { -it.relevanceScore }
        result.requiredResources = result.requiredResources.unique()
        
        // Estimate resolution time
        if (result.primaryRecommendations.size() > 0) {
            result.estimatedResolutionTime = result.primaryRecommendations[0].estimatedDuration
        }
        
        return result
    }
    
    private double calculatePlaybookRelevance(Map<String, Object> incidentData, Map<String, Object> playbook, List<Map<String, Object>> patterns) {
        double score = 0.5 // Base score
        
        // Boost if matching pattern
        patterns.each { pattern ->
            if (pattern.patternName?.toLowerCase()?.contains(playbook.name?.toLowerCase())) {
                score += 0.3
            }
        }
        
        // Boost based on incident characteristics
        if (incidentData.severity == 'CRITICAL' && playbook.name?.contains('Critical')) {
            score += 0.2
        }
        
        return Math.min(1.0, score)
    }
}

// Stakeholder communication manager
class StakeholderCommunicationManager {
    Map<String, Map<String, Object>> communicationTemplates = [:]
    List<Map<String, Object>> communicationLog = []
    Map<String, List<String>> stakeholderGroups = [:]
    
    void registerTemplate(String templateId, Map<String, Object> template) {
        communicationTemplates[templateId] = template
    }
    
    void registerStakeholderGroup(String groupId, List<String> stakeholders) {
        stakeholderGroups[groupId] = stakeholders
    }
    
    Map<String, Object> prepareNotification(String incidentId, String templateId, Map<String, Object> data) {
        def template = communicationTemplates[templateId]
        if (!template) {
            return [success: false, error: 'Template not found']
        }
        
        def notification = [
            notificationId: COMM_PREFIX + System.currentTimeMillis(),
            incidentId: incidentId,
            templateId: templateId,
            subject: renderTemplate(template.subject, data),
            body: renderTemplate(template.body, data),
            priority: template.priority ?: 'NORMAL',
            channels: template.channels ?: ['EMAIL'],
            recipients: [],
            status: 'PREPARED',
            preparedAt: Emery.util.currentDate()
        ]
        
        // Resolve recipients
        template.recipientGroups?.each { groupId ->
            def groupMembers = stakeholderGroups[groupId] ?: []
            notification.recipients.addAll(groupMembers)
        }
        notification.recipients = notification.recipients.unique()
        
        return [success: true, notification: notification]
    }
    
    private String renderTemplate(String template, Map<String, Object> data) {
        if (!template) return ''
        
        def rendered = template
        data.each { key, value ->
            rendered = rendered.replace("\${${key}}", value?.toString() ?: '')
            rendered = rendered.replace("{{${key}}}", value?.toString() ?: '')
        }
        
        return rendered
    }
    
    Map<String, Object> sendNotification(Map<String, Object> notification) {
        try {
            // Simulate sending
            notification.status = 'SENT'
            notification.sentAt = Emery.util.currentDate()
            
            communicationLog.add(notification)
            
            return [success: true, notification: notification]
        } catch (Exception e) {
            notification.status = 'FAILED'
            notification.error = e.message
            return [success: false, error: e.message]
        }
    }
    
    List<Map<String, Object>> getCommunicationLog(String incidentId) {
        return communicationLog.findAll { it.incidentId == incidentId }
    }
}

// Main processing block
try {
    Emery.log.info("Starting main processing for issue metric_id: {} with upload_type: {}", F.metric_id, F.upload_type)
    
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
    
    // Initialize ML and analytics components
    def mlClassifier = new MLIncidentClassifier()
    def patternDetector = new IncidentPatternDetector()
    def slaAnalyzer = new PredictiveSLAAnalyzer()
    def remediationEngine = new RemediationRecommendationEngine()
    def commManager = new StakeholderCommunicationManager()
    
    // Configure ML classifier
    mlClassifier.setCategoryKeywords('SECURITY', ['breach', 'hack', 'unauthorized', 'intrusion', 'malware', 'phishing', 'credential'])
    mlClassifier.setCategoryKeywords('OPERATIONAL', ['outage', 'down', 'unavailable', 'slow', 'error', 'failure', 'crash'])
    mlClassifier.setCategoryKeywords('DATA', ['data', 'loss', 'corruption', 'integrity', 'backup', 'recovery'])
    mlClassifier.setCategoryKeywords('COMPLIANCE', ['violation', 'audit', 'regulatory', 'policy', 'non-compliant'])
    
    mlClassifier.setFeatureWeights('SECURITY', [breach: 20.0, hack: 20.0, unauthorized: 15.0, malware: 15.0])
    mlClassifier.setFeatureWeights('OPERATIONAL', [outage: 20.0, down: 15.0, unavailable: 15.0, failure: 15.0])
    
    // Configure pattern detector
    patternDetector.registerPattern('RECURRING_AUTH_FAILURE', [
        category: 'SECURITY',
        name: 'Recurring Authentication Failures',
        description: 'Pattern of repeated authentication failures',
        severity: 'HIGH',
        indicators: [
            [field: 'incident_type', matchType: 'exact', value: 'AUTH_FAILURE'],
            [field: 'description', matchType: 'contains', value: 'failed login']
        ],
        recommendedActions: ['Review access logs', 'Check for brute force attempts', 'Consider account lockout policy']
    ])
    
    // Configure SLA analyzer
    slaAnalyzer.loadHistoricalMetrics('P1', [breachRate: 0.15, avgResolutionHours: 6])
    slaAnalyzer.loadHistoricalMetrics('P2', [breachRate: 0.10, avgResolutionHours: 24])
    slaAnalyzer.loadHistoricalMetrics('P3', [breachRate: 0.08, avgResolutionHours: 48])
    
    // Configure remediation playbooks
    remediationEngine.registerPlaybook('SECURITY_INCIDENT_PB', [
        category: 'SECURITY',
        name: 'Security Incident Response',
        description: 'Standard playbook for security incidents',
        steps: ['Contain the threat', 'Preserve evidence', 'Analyze impact', 'Remediate', 'Document and report'],
        estimatedDuration: '24 hours',
        requiredSkills: ['Security Analyst', 'Incident Responder'],
        automationLevel: 'PARTIAL'
    ])
    
    // Configure communication templates
    commManager.registerTemplate('INCIDENT_CREATED', [
        subject: 'New Incident Created: ${incident_title}',
        body: 'A new incident has been created.\n\nIncident ID: ${incident_id}\nTitle: ${incident_title}\nSeverity: ${severity}\nAssigned To: ${assigned_to}',
        priority: 'HIGH',
        channels: ['EMAIL', 'SLACK'],
        recipientGroups: ['INCIDENT_MANAGERS', 'AFFECTED_STAKEHOLDERS']
    ])
    
    commManager.registerStakeholderGroup('INCIDENT_MANAGERS', ['incident_manager@company.com'])
    commManager.registerStakeholderGroup('AFFECTED_STAKEHOLDERS', ['stakeholder1@company.com', 'stakeholder2@company.com'])
    
    /* Upload type specific handling */
    if (!F.upload_type) {
        def current_status = F.dd_current_status
        def current_stage = F.dd_current_stage
        def obj_status = F.obj_status
        
        Emery.log.debug("Script Name: {} executed for status: {}, stage: {}, obj_status: {}", script_name, current_status, current_stage, obj_status)
        
        // Creator assignment
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
        
        // ML Classification
        def incidentData = [
            incidentId: F.object_id,
            title: F.object_name,
            description: F.description,
            incident_type: F.incident_type,
            affected_systems: F.affected_systems,
            severity_score: F.severity_score ?: 50,
            priority: F.priority,
            has_dependencies: F.has_dependencies
        ]
        
        def mlResult = mlClassifier.classifyWithML(incidentData)
        mlContext.classification = mlResult
        mlConfidenceScore = mlResult.confidence
        
        if (mlResult.topPrediction) {
            F.ml_predicted_category = mlResult.topPrediction.category
            F.ml_confidence_score = mlResult.confidence * 100
        }
        
        // Find similar incidents
        def historicalIncidents = [] // In real implementation, would query from database
        def similarityResult = mlClassifier.findSimilarIncidents(incidentData, historicalIncidents)
        similarIncidents = similarityResult.similarIncidents
        similarIncidentCount = similarityResult.totalSimilar
        
        // Pattern detection
        def recentIncidents = [] // In real implementation, would query recent incidents
        def patternResult = patternDetector.detectPatterns(incidentData, recentIncidents)
        patternMatches = patternResult.patternsDetected
        patternDetected = patternMatches.size() > 0
        
        // Predictive SLA analysis
        incidentData.priority = F.priority ?: 'P3'
        def slaPrediction = slaAnalyzer.predictBreachProbability(incidentData)
        mlContext.slaPrediction = slaPrediction
        
        if (slaPrediction.overallRisk == 'HIGH') {
            requiresUrgentAttention = true
        }
        
        // Generate remediation recommendations
        def remediationResult = remediationEngine.generateRecommendations(incidentData, mlResult, patternMatches)
        recommendations = remediationResult.primaryRecommendations
        mlContext.remediation = remediationResult
        
        // Prepare stakeholder notification
        def notificationData = [
            incident_id: F.object_id,
            incident_title: F.object_name,
            severity: F.severity,
            assigned_to: F.assigned_investigator_name
        ]
        
        def notificationResult = commManager.prepareNotification(F.object_id, 'INCIDENT_CREATED', notificationData)
        if (notificationResult.success) {
            def sendResult = commManager.sendNotification(notificationResult.notification)
            communications.add(notificationResult.notification)
        }
        
        communicationContext.log = commManager.getCommunicationLog(F.object_id)
    }
    
    /* Non-multirow field updates */
    
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
    
    // Store all contexts
    processingContext.ml = mlContext
    processingContext.patterns = patternMatches
    processingContext.recommendations = recommendations
    processingContext.communications = communications
    processingContext.similarIncidents = similarIncidents
    processingContext.patternDetected = patternDetected
    processingContext.requiresUrgentAttention = requiresUrgentAttention
    
    // Final processing context update
    processingContext.endTime = System.currentTimeMillis()
    processingContext.duration = processingContext.endTime - processingContext.startTime
    
    Emery.log.info("Processing completed in {}ms. ML confidence: {}, Patterns detected: {}, Urgent: {}", 
        processingContext.duration, mlConfidenceScore, patternDetected, requiresUrgentAttention)
    
} catch (Exception e) {
    Emery.log.error("Error in {}: {}", script_name, e.message)
    isRollbackRequired = true
    throw e
} finally {
    Emery.log.info("Script {} completed with rollback={}", script_name, isRollbackRequired)
}

