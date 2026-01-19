/**
 * Complex GRC Risk Processing Script - Initiator to Level 1 Transition
 * Operations: INI_TO_L1 transition with advanced risk assessment and scoring
 * i) Field updates: obj_status, obj_status_temp, owner_organizations, wfi_display, wfi_stored, dd_current_stage
 * ii) Risk scoring calculation with weighted factors
 * iii) Control effectiveness assessment integration
 * iv) Residual risk calculation with confidence intervals
 * v) Master table update with dd_edit_flag management
 **/

use("MS_GRC_RISK")
useDataObject("MS_GRC_REL_DEFN")
useDataObject("MS_GRC_RISK")
useDataObject("MS_GRC_RISK_ASSESSMENT")
useDataObject("MS_GRC_CONTROL")
useDataObject("MS_GRC_CONTROL_EFFECTIVENESS")
useDataObject("MS_GRC_RISK_SCORE_CONFIG")
useDataObject("MS_GRC_RISK_MATRIX")

// Extended variable declarations for risk processing
String id, name
Date date1
String date2
String script_name = "RiskInitiatorToLevel1_Advanced.groovy"
Map<String, Object> processingContext = [:]
Map<String, Object> riskScoreConfig = [:]
Map<String, Double> riskWeights = [:]
Map<String, List<String>> validationErrors = [:]
List<Map<String, Object>> auditTrailEntries = []
List<Map<String, Object>> riskAssessmentHistory = []
List<Map<String, Object>> controlAssessments = []
def riskMatrixCache = [:]
boolean isRollbackRequired = false
boolean recalculateRiskScore = true
double inherentRiskScore = 0.0
double residualRiskScore = 0.0
double controlEffectiveness = 0.0
final String DELIMITER = '\$_\$'
final String RISK_PREFIX = 'RSK_'
final double DEFAULT_WEIGHT = 1.0
final int SCORE_PRECISION = 2

Emery.log.info("Started Pre-Hook {} at {}", script_name, Emery.util.currentDateAsString())

// Complex risk scoring engine with configurable weights
class RiskScoringEngine {
    Map<String, Double> factorWeights = [:]
    Map<String, Map<String, Double>> scoreMappings = [:]
    Map<String, Closure> customScoreCalculators = [:]
    double minScore = 0.0
    double maxScore = 100.0
    int precision = 2
    
    void setFactorWeight(String factor, Double weight) {
        factorWeights[factor] = weight
    }
    
    void setScoreMapping(String factor, Map<String, Double> mapping) {
        scoreMappings[factor] = mapping
    }
    
    void registerCustomCalculator(String name, Closure calculator) {
        customScoreCalculators[name] = calculator
    }
    
    double calculateScore(Map<String, Object> riskData) {
        double totalScore = 0.0
        double totalWeight = 0.0
        
        factorWeights.each { factor, weight ->
            def factorValue = riskData[factor]
            double factorScore = 0.0
            
            if (factorValue != null) {
                if (scoreMappings.containsKey(factor)) {
                    factorScore = scoreMappings[factor][factorValue.toString()] ?: 0.0
                } else if (factorValue instanceof Number) {
                    factorScore = factorValue.doubleValue()
                } else if (customScoreCalculators.containsKey(factor)) {
                    factorScore = customScoreCalculators[factor](factorValue, riskData)
                }
            }
            
            totalScore += factorScore * weight
            totalWeight += weight
        }
        
        if (totalWeight > 0) {
            totalScore = totalScore / totalWeight
        }
        
        // Normalize to range
        totalScore = Math.max(minScore, Math.min(maxScore, totalScore))
        
        // Round to precision
        return Math.round(totalScore * Math.pow(10, precision)) / Math.pow(10, precision)
    }
    
    Map<String, Object> calculateWithBreakdown(Map<String, Object> riskData) {
        def breakdown = [:]
        double totalScore = 0.0
        double totalWeight = 0.0
        
        factorWeights.each { factor, weight ->
            def factorValue = riskData[factor]
            double factorScore = 0.0
            String scoreSource = 'default'
            
            if (factorValue != null) {
                if (scoreMappings.containsKey(factor)) {
                    factorScore = scoreMappings[factor][factorValue.toString()] ?: 0.0
                    scoreSource = 'mapping'
                } else if (factorValue instanceof Number) {
                    factorScore = factorValue.doubleValue()
                    scoreSource = 'numeric'
                } else if (customScoreCalculators.containsKey(factor)) {
                    factorScore = customScoreCalculators[factor](factorValue, riskData)
                    scoreSource = 'custom'
                }
            }
            
            double weightedScore = factorScore * weight
            
            breakdown[factor] = [
                rawValue: factorValue,
                score: factorScore,
                weight: weight,
                weightedScore: weightedScore,
                scoreSource: scoreSource
            ]
            
            totalScore += weightedScore
            totalWeight += weight
        }
        
        double finalScore = totalWeight > 0 ? totalScore / totalWeight : 0.0
        finalScore = Math.max(minScore, Math.min(maxScore, finalScore))
        finalScore = Math.round(finalScore * Math.pow(10, precision)) / Math.pow(10, precision)
        
        return [
            totalScore: finalScore,
            totalWeight: totalWeight,
            breakdown: breakdown,
            calculatedAt: new Date()
        ]
    }
    
    String getRiskRating(double score) {
        if (score >= 80) return 'CRITICAL'
        if (score >= 60) return 'HIGH'
        if (score >= 40) return 'MEDIUM'
        if (score >= 20) return 'LOW'
        return 'MINIMAL'
    }
}

// Control effectiveness calculator with assessment integration
class ControlEffectivenessCalculator {
    Map<String, Double> effectivenessWeights = [
        'DESIGN_EFFECTIVENESS': 0.4,
        'OPERATING_EFFECTIVENESS': 0.4,
        'AUTOMATION_LEVEL': 0.2
    ]
    
    Map<String, Double> ratingScores = [
        'EFFECTIVE': 1.0,
        'PARTIALLY_EFFECTIVE': 0.6,
        'INEFFECTIVE': 0.2,
        'NOT_ASSESSED': 0.0
    ]
    
    Map<String, Double> automationScores = [
        'FULLY_AUTOMATED': 1.0,
        'SEMI_AUTOMATED': 0.7,
        'MANUAL': 0.4,
        'NOT_APPLICABLE': 0.5
    ]
    
    double calculateEffectiveness(Map<String, Object> controlData) {
        double designScore = ratingScores[controlData.design_effectiveness] ?: 0.0
        double operatingScore = ratingScores[controlData.operating_effectiveness] ?: 0.0
        double automationScore = automationScores[controlData.automation_level] ?: 0.5
        
        return (designScore * effectivenessWeights['DESIGN_EFFECTIVENESS']) +
               (operatingScore * effectivenessWeights['OPERATING_EFFECTIVENESS']) +
               (automationScore * effectivenessWeights['AUTOMATION_LEVEL'])
    }
    
    double calculateAggregateEffectiveness(List<Map<String, Object>> controls) {
        if (!controls || controls.isEmpty()) return 0.0
        
        double totalEffectiveness = controls.sum { calculateEffectiveness(it) }
        return totalEffectiveness / controls.size()
    }
    
    Map<String, Object> calculateWithDetails(List<Map<String, Object>> controls) {
        def details = []
        double totalEffectiveness = 0.0
        int effectiveCount = 0
        int partialCount = 0
        int ineffectiveCount = 0
        
        controls.each { control ->
            double effectiveness = calculateEffectiveness(control)
            def detail = [
                controlId: control.control_id,
                controlName: control.control_name,
                designEffectiveness: control.design_effectiveness,
                operatingEffectiveness: control.operating_effectiveness,
                automationLevel: control.automation_level,
                calculatedEffectiveness: effectiveness
            ]
            
            if (effectiveness >= 0.8) effectiveCount++
            else if (effectiveness >= 0.5) partialCount++
            else ineffectiveCount++
            
            totalEffectiveness += effectiveness
            details.add(detail)
        }
        
        return [
            aggregateEffectiveness: controls.size() > 0 ? totalEffectiveness / controls.size() : 0.0,
            controlCount: controls.size(),
            effectiveCount: effectiveCount,
            partialCount: partialCount,
            ineffectiveCount: ineffectiveCount,
            details: details
        ]
    }
}

// Residual risk calculator with control mitigation
def calculateResidualRisk = { double inherentScore, double controlEffectiveness, Map<String, Object> config ->
    def mitigationFactor = controlEffectiveness
    def minResidual = config.minResidualPercentage ?: 0.1
    
    // Apply mitigation with floor
    double residual = inherentScore * (1 - mitigationFactor)
    residual = Math.max(residual, inherentScore * minResidual)
    
    // Apply confidence adjustment if available
    if (config.confidenceLevel) {
        double confidenceAdjustment = 1.0 + ((1.0 - config.confidenceLevel) * 0.2)
        residual = residual * confidenceAdjustment
    }
    
    return Math.round(residual * 100) / 100.0
}

// Risk matrix operations with caching
def riskMatrixOperations = {
    def matrixCache = [:]
    
    // Load risk matrix
    def loadMatrix = { String matrixId ->
        if (matrixCache.containsKey(matrixId)) {
            return matrixCache[matrixId]
        }
        
        try {
            SelectQuery matrixQuery = new SelectQuery.Builder("MS_GRC_RISK_MATRIX")
                .columns(["MATRIX_ID", "MATRIX_NAME", "LIKELIHOOD_AXIS", "IMPACT_AXIS", "CELL_VALUES", "COLOR_CODING", "STATUS"])
                .addCondition(Condition.eq("MATRIX_ID", matrixId))
                .addCondition(Condition.eq("STATUS", "ACTIVE"))
                .loadRegions(false)
                .build()
            
            def matrixResult = Emery.dataobject.fetch(matrixQuery)
            
            if (matrixResult.dataObjects && matrixResult.dataObjects.size() > 0) {
                def matrix = matrixResult.dataObjects[0]
                def parsedMatrix = [
                    id: matrix.matrix_id,
                    name: matrix.matrix_name,
                    likelihoodAxis: parseJsonSafe(matrix.likelihood_axis),
                    impactAxis: parseJsonSafe(matrix.impact_axis),
                    cellValues: parseJsonSafe(matrix.cell_values),
                    colorCoding: parseJsonSafe(matrix.color_coding)
                ]
                matrixCache[matrixId] = parsedMatrix
                return parsedMatrix
            }
        } catch (Exception e) {
            Emery.log.error("Failed to load risk matrix {}: {}", matrixId, e.message)
        }
        
        return null
    }
    
    def parseJsonSafe = { String jsonStr ->
        if (!jsonStr || jsonStr.trim() == '') return [:]
        try {
            return new groovy.json.JsonSlurper().parseText(jsonStr)
        } catch (Exception e) {
            return [:]
        }
    }
    
    // Get cell value from matrix
    def getCellValue = { String matrixId, String likelihood, String impact ->
        def matrix = loadMatrix(matrixId)
        if (!matrix) return null
        
        def cellKey = "${likelihood}_${impact}"
        return matrix.cellValues[cellKey]
    }
    
    // Get color for score
    def getColorForScore = { String matrixId, double score ->
        def matrix = loadMatrix(matrixId)
        if (!matrix || !matrix.colorCoding) return '#GRAY'
        
        def colorConfig = matrix.colorCoding.find { config ->
            score >= config.minScore && score <= config.maxScore
        }
        
        return colorConfig?.color ?: '#GRAY'
    }
    
    return [
        loadMatrix: loadMatrix,
        getCellValue: getCellValue,
        getColorForScore: getColorForScore,
        clearCache: { matrixCache.clear() }
    ]
}

// Initialize risk matrix operations
def matrixOps = riskMatrixOperations()

// Complex risk assessment workflow handler
def riskAssessmentWorkflow = {
    // Create assessment record
    def createAssessment = { Map<String, Object> assessmentData ->
        def assessment = [
            assessmentId: Emery.util.nextUniqueRowId(),
            riskId: assessmentData.riskId,
            assessmentType: assessmentData.type ?: 'PERIODIC',
            assessorId: assessmentData.assessorId ?: F.created_by,
            assessorName: assessmentData.assessorName ?: F.dd_current_user_name,
            assessmentDate: Emery.util.currentDate(),
            inherentLikelihood: assessmentData.inherentLikelihood,
            inherentImpact: assessmentData.inherentImpact,
            inherentScore: assessmentData.inherentScore,
            residualLikelihood: assessmentData.residualLikelihood,
            residualImpact: assessmentData.residualImpact,
            residualScore: assessmentData.residualScore,
            controlEffectiveness: assessmentData.controlEffectiveness,
            comments: assessmentData.comments,
            status: 'PENDING_REVIEW'
        ]
        
        riskAssessmentHistory.add(assessment)
        return assessment
    }
    
    // Get assessment history
    def getAssessmentHistory = { String riskId, int limit ->
        try {
            SelectQuery historyQuery = new SelectQuery.Builder("MS_GRC_RISK_ASSESSMENT")
                .columns(["ASSESSMENT_ID", "RISK_ID", "ASSESSMENT_TYPE", "ASSESSOR_NAME", "ASSESSMENT_DATE", "INHERENT_SCORE", "RESIDUAL_SCORE", "STATUS"])
                .addCondition(Condition.eq("RISK_ID", riskId))
                .addSortCondition(SortCondition.desc("ASSESSMENT_DATE"))
                .limit(limit)
                .loadRegions(false)
                .build()
            
            def historyResult = Emery.dataobject.fetch(historyQuery)
            return historyResult.dataObjects?.collect { [
                assessmentId: it.assessment_id,
                riskId: it.risk_id,
                type: it.assessment_type,
                assessor: it.assessor_name,
                date: it.assessment_date,
                inherentScore: it.inherent_score,
                residualScore: it.residual_score,
                status: it.status
            ]} ?: []
        } catch (Exception e) {
            Emery.log.error("Failed to get assessment history for risk {}: {}", riskId, e.message)
            return []
        }
    }
    
    // Calculate trend
    def calculateTrend = { List<Map<String, Object>> history ->
        if (!history || history.size() < 2) {
            return [trend: 'STABLE', changePercent: 0.0]
        }
        
        def latestScore = history[0].residualScore ?: 0.0
        def previousScore = history[1].residualScore ?: 0.0
        
        if (previousScore == 0) return [trend: 'STABLE', changePercent: 0.0]
        
        def changePercent = ((latestScore - previousScore) / previousScore) * 100
        
        String trend
        if (changePercent > 10) trend = 'INCREASING'
        else if (changePercent < -10) trend = 'DECREASING'
        else trend = 'STABLE'
        
        return [trend: trend, changePercent: Math.round(changePercent * 100) / 100.0]
    }
    
    return [
        createAssessment: createAssessment,
        getAssessmentHistory: getAssessmentHistory,
        calculateTrend: calculateTrend
    ]
}

// Initialize risk assessment workflow
def assessmentOps = riskAssessmentWorkflow()

// Control linkage operations
def controlLinkageOperations = {
    // Get linked controls for risk
    def getLinkedControls = { String riskId ->
        try {
            SelectQuery controlQuery = new SelectQuery.Builder("MS_GRC_REL_INSTANCE")
                .columns(["REL_INST_ID", "SRC_OBJ_ID", "DEST_OBJ_ID", "DEST_OBJ_TYPE", "REL_TYPE", "STATUS"])
                .addCondition(Condition.eq("SRC_OBJ_ID", riskId))
                .addCondition(Condition.eq("DEST_OBJ_TYPE", "CONTROL"))
                .addCondition(Condition.eq("STATUS", "ACTIVE"))
                .loadRegions(false)
                .build()
            
            def controlResult = Emery.dataobject.fetch(controlQuery)
            def controlIds = controlResult.dataObjects?.collect { it.dest_obj_id } ?: []
            
            if (controlIds.isEmpty()) return []
            
            // Fetch control details
            SelectQuery detailQuery = new SelectQuery.Builder("MS_GRC_CONTROL")
                .columns(["OBJECT_ID", "OBJECT_NAME", "CONTROL_TYPE", "DESIGN_EFFECTIVENESS", "OPERATING_EFFECTIVENESS", "AUTOMATION_LEVEL", "OBJ_STATUS"])
                .addCondition(Condition.inList("OBJECT_ID", controlIds))
                .addCondition(Condition.eq("OBJ_STATUS", "ACT"))
                .loadRegions(false)
                .build()
            
            def detailResult = Emery.dataobject.fetch(detailQuery)
            return detailResult.dataObjects?.collect { [
                control_id: it.object_id,
                control_name: it.object_name,
                control_type: it.control_type,
                design_effectiveness: it.design_effectiveness,
                operating_effectiveness: it.operating_effectiveness,
                automation_level: it.automation_level
            ]} ?: []
        } catch (Exception e) {
            Emery.log.error("Failed to get linked controls for risk {}: {}", riskId, e.message)
            return []
        }
    }
    
    // Get control effectiveness summary
    def getEffectivenessSummary = { String riskId ->
        def controls = getLinkedControls(riskId)
        def calculator = new ControlEffectivenessCalculator()
        return calculator.calculateWithDetails(controls)
    }
    
    return [
        getLinkedControls: getLinkedControls,
        getEffectivenessSummary: getEffectivenessSummary
    ]
}

// Initialize control linkage operations
def controlOps = controlLinkageOperations()

// Main processing block
try {
    Emery.log.info("Starting main processing for risk metric_id: {}", F.metric_id)
    
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
    
    /* Non-multirow field updates */
    
    // Object status update based on action
    if (F.object_action == 'SND_APP') {
        F.obj_status = 'APP_PEND'
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
    
    // Clear upload_type
    if (F.upload_type != '' && F.upload_type != null) {
        F.upload_type = ''
    }
    
    // Clear approval_rqrd
    if (F.approval_rqrd != '' && F.approval_rqrd != null) {
        F.approval_rqrd = ''
    }
    
    // Build wfi_stored
    name = F.dd_current_user_name
    date1 = Emery.util.currentDate()
    date2 = Emery.dateUtil.convertDateToString(date1, "dd/MM/YYYY HH:mm:ss")
    F.wfi_stored = name.concat(DELIMITER).concat(date2)
    
    // Update stage
    F.dd_current_stage = 'L1_APPROVE'
    
    // Comment insertion
    if (F.action_comments != null && F.action_comments.toString().trim() != '') {
        String result = Emery.util.insertComments(F.process_instance_id as int, F.instance_id as int, F.metric_id as int, F.created_by as int, "ACTION_COMMENTS", F.action_comments as String)
        F.action_comments = ''
    }
    
    /* Risk scoring calculation */
    if (recalculateRiskScore && F.object_id && F.object_id != '' && F.object_id != 'NONE') {
        def scoringEngine = new RiskScoringEngine()
        
        // Configure scoring weights
        scoringEngine.setFactorWeight('likelihood', 0.5)
        scoringEngine.setFactorWeight('impact', 0.5)
        
        // Configure score mappings
        scoringEngine.setScoreMapping('likelihood', [
            'RARE': 10, 'UNLIKELY': 30, 'POSSIBLE': 50, 'LIKELY': 70, 'ALMOST_CERTAIN': 90
        ])
        scoringEngine.setScoreMapping('impact', [
            'INSIGNIFICANT': 10, 'MINOR': 30, 'MODERATE': 50, 'MAJOR': 70, 'CATASTROPHIC': 90
        ])
        
        // Calculate inherent risk score
        def inherentScoreResult = scoringEngine.calculateWithBreakdown([
            likelihood: F.inherent_likelihood,
            impact: F.inherent_impact
        ])
        
        inherentRiskScore = inherentScoreResult.totalScore
        
        // Get control effectiveness
        def effectivenessSummary = controlOps.getEffectivenessSummary(F.object_id)
        controlEffectiveness = effectivenessSummary.aggregateEffectiveness
        controlAssessments = effectivenessSummary.details
        
        // Calculate residual risk
        residualRiskScore = calculateResidualRisk(inherentRiskScore, controlEffectiveness, [
            minResidualPercentage: 0.1,
            confidenceLevel: 0.8
        ])
        
        // Get risk rating
        def inherentRating = scoringEngine.getRiskRating(inherentRiskScore)
        def residualRating = scoringEngine.getRiskRating(residualRiskScore)
        
        // Update form fields
        F.inherent_risk_score = inherentRiskScore
        F.residual_risk_score = residualRiskScore
        F.inherent_risk_rating = inherentRating
        F.residual_risk_rating = residualRating
        F.control_effectiveness_score = Math.round(controlEffectiveness * 100)
        
        // Create assessment record
        def assessment = assessmentOps.createAssessment([
            riskId: F.object_id,
            type: 'WORKFLOW_TRANSITION',
            inherentLikelihood: F.inherent_likelihood,
            inherentImpact: F.inherent_impact,
            inherentScore: inherentRiskScore,
            residualScore: residualRiskScore,
            controlEffectiveness: controlEffectiveness,
            comments: "Calculated during INI_TO_L1 transition"
        ])
        
        // Get trend analysis
        def history = assessmentOps.getAssessmentHistory(F.object_id, 5)
        def trend = assessmentOps.calculateTrend(history)
        
        processingContext.riskScoring = [
            inherentScore: inherentRiskScore,
            inherentRating: inherentRating,
            residualScore: residualRiskScore,
            residualRating: residualRating,
            controlEffectiveness: controlEffectiveness,
            controlCount: effectivenessSummary.controlCount,
            trend: trend,
            assessment: assessment
        ]
        
        Emery.log.info("Risk scoring completed: inherent={}, residual={}, trend={}", inherentRiskScore, residualRiskScore, trend.trend)
    }
    
    /* Apps_ID Creation */
    if (F.object_id == '' || F.object_id == 'NONE') {
        id = Emery.util.nextAppsId("MS_GRC_RISK", "GRC", "MS_GRC_RISK_IDGEN")
        F.object_id = id
    }
    
    /* Multirow processing */
    F.orb.rows.each { row ->
        if (row.rel_inst_id == '' || row.rel_inst_id == 'NONE') {
            id = Emery.util.nextAppsId("MS_GRC_RISK", "GRC", "MS_ORB_REL_INST_ID")
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
        id = Emery.util.nextAppsId("MS_GRC_RISK", "GRC", "MS_ORB_REL_SOURCE_ID")
        F.rel_source_id = id
    }
    
    if (F.rel_source_object_id == '' || F.rel_source_object_id == 'NONE') {
        F.rel_source_object_id = F.object_id
    }
    
    // App config handling
    if (F.own_app_config_temp == 'Yes' || F.own_app_config_temp == 'No') {
        F.own_app_config_temp = F.own_app_config
    }
    
    /* Master table update */
    UpdateQuery master_table_update = new UpdateQuery.Builder("MS_GRC_RISK")
        .setValue("DD_EDIT_FLAG", 'N')
        .addCondition(Condition.eq("OBJECT_ID", F.object_id))
        .build()
    
    int master_table_update_count = Emery.dataobject.update(master_table_update)
    Emery.log.info("Master table update count: {}", master_table_update_count)
    
    // Final processing context update
    processingContext.endTime = System.currentTimeMillis()
    processingContext.duration = processingContext.endTime - processingContext.startTime
    processingContext.masterTableUpdateCount = master_table_update_count
    
    Emery.log.info("Processing completed in {}ms", processingContext.duration)
    
} catch (Exception e) {
    Emery.log.error("Error in {}: {}", script_name, e.message)
    isRollbackRequired = true
    throw e
} finally {
    matrixOps.clearCache()
    Emery.log.info("Script {} completed with rollback={}", script_name, isRollbackRequired)
}
