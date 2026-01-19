/**
 * Enhanced GRC Function Processing Script with Upload Type and Blueprint Support
 * Operations: INI_TO_PUB transition with advanced function management and blueprint integration
 * i) Upload type conditional processing with validation
 * ii) Blueprint template instantiation and customization
 * iii) Advanced dependency management with impact analysis
 * iv) Complex email notification with template rendering
 * v) Data table operations with caching and optimization
 **/

use("MS_GRC_FUNCTION")
useDataObject("MS_GRC_REL_DEFN")
useDataObject("MS_GRC_FUNCTION")
useDataObject("MS_GRC_FUNCTION_DEPENDENCY")
useDataObject("MS_GRC_BLUEPRINT")
useDataObject("MS_GRC_TEMPLATE_INSTANCE")
useDataObject("MS_GRC_EMAIL_QUEUE")

// Extended variable declarations
String id, name, cur_date_string, valid_from_string, valid_until_string, lv_flag, lv_old_object_name
Date current_date, valid_from_date, valid_until_date, valid_until_date_temp
String script_name = "FunctionInitiatorToPublish_Blueprint.groovy"
Map<String, Object> processingContext = [:]
Map<String, Object> blueprintContext = [:]
Map<String, List<String>> validationErrors = [:]
Map<String, Object> emailTemplateCache = [:]
List<Map<String, Object>> auditTrailEntries = []
List<Map<String, Object>> impactAnalysisResults = []
List<DataObject> templateInstances = []
def deferredEmailQueue = []
boolean isRollbackRequired = false
boolean blueprintMode = false
int templateInstanceCount = 0
final int MAX_TEMPLATE_INSTANCES = 100
final int EMAIL_BATCH_SIZE = 50
final String DELIMITER = '\$_\$'
final String BLUEPRINT_PREFIX = 'BP_'

Emery.log.info("Started Pre-Hook {} at {}", script_name, Emery.util.currentDateAsString())

// Complex blueprint template handler
class BlueprintTemplateHandler {
    Map<String, Object> templateCache = [:]
    Map<String, List<Map<String, Object>>> fieldMappings = [:]
    List<String> requiredFields = []
    List<String> optionalFields = []
    Map<String, Closure> transformers = [:]
    
    void loadTemplate(String blueprintId) {
        if (templateCache.containsKey(blueprintId)) return
        
        try {
            SelectQuery bpQuery = new SelectQuery.Builder("MS_GRC_BLUEPRINT")
                .columns(["BLUEPRINT_ID", "BLUEPRINT_NAME", "TEMPLATE_TYPE", "FIELD_MAPPINGS", "REQUIRED_FIELDS", "OPTIONAL_FIELDS", "TRANSFORMATIONS", "STATUS"])
                .addCondition(Condition.eq("BLUEPRINT_ID", blueprintId))
                .addCondition(Condition.eq("STATUS", "ACTIVE"))
                .loadRegions(false)
                .build()
            
            SelectQueryResponse bpResult = Emery.dataobject.fetch(bpQuery)
            
            if (bpResult.dataObjects && bpResult.dataObjects.size() > 0) {
                def bp = bpResult.dataObjects[0]
                templateCache[blueprintId] = [
                    id: bp.blueprint_id,
                    name: bp.blueprint_name,
                    type: bp.template_type,
                    fieldMappings: parseJsonField(bp.field_mappings),
                    requiredFields: parseListField(bp.required_fields),
                    optionalFields: parseListField(bp.optional_fields),
                    transformations: parseJsonField(bp.transformations)
                ]
            }
        } catch (Exception e) {
            Emery.log.error("Failed to load blueprint {}: {}", blueprintId, e.message)
        }
    }
    
    private Map parseJsonField(String jsonStr) {
        if (!jsonStr || jsonStr.trim() == '') return [:]
        try {
            return new groovy.json.JsonSlurper().parseText(jsonStr)
        } catch (Exception e) {
            return [:]
        }
    }
    
    private List parseListField(String listStr) {
        if (!listStr || listStr.trim() == '') return []
        return listStr.split(',').collect { it.trim() }
    }
    
    Map<String, Object> instantiateTemplate(String blueprintId, Map<String, Object> sourceData) {
        loadTemplate(blueprintId)
        
        def template = templateCache[blueprintId]
        if (!template) {
            return [success: false, error: "Blueprint not found: ${blueprintId}"]
        }
        
        def instance = [:]
        def errors = []
        
        // Validate required fields
        template.requiredFields.each { field ->
            if (!sourceData.containsKey(field) || sourceData[field] == null || sourceData[field].toString().trim() == '') {
                errors.add("Required field missing: ${field}")
            }
        }
        
        if (errors.size() > 0) {
            return [success: false, errors: errors]
        }
        
        // Apply field mappings
        template.fieldMappings.each { targetField, mapping ->
            if (mapping instanceof String) {
                instance[targetField] = sourceData[mapping]
            } else if (mapping instanceof Map) {
                def sourceField = mapping.source
                def defaultValue = mapping.default
                def transformer = mapping.transformer
                
                def value = sourceData[sourceField] ?: defaultValue
                
                if (transformer && transformers.containsKey(transformer)) {
                    value = transformers[transformer](value, sourceData)
                }
                
                instance[targetField] = value
            }
        }
        
        return [success: true, instance: instance, templateId: blueprintId]
    }
    
    void registerTransformer(String name, Closure transformer) {
        transformers[name] = transformer
    }
}

// Complex email service handler
def emailServiceHandler = {
    def templateCache = [:]
    def sendQueue = []
    
    // Load email template
    def loadTemplate = { String templateId ->
        if (templateCache.containsKey(templateId)) {
            return templateCache[templateId]
        }
        
        try {
            def template = Emery.email.getTemplate(templateId)
            templateCache[templateId] = template
            return template
        } catch (Exception e) {
            Emery.log.error("Failed to load email template {}: {}", templateId, e.message)
            return null
        }
    }
    
    // Render template with data
    def renderTemplate = { String templateId, Map<String, Object> data ->
        def template = loadTemplate(templateId)
        if (!template) return null
        
        def rendered = [
            subject: template.subject,
            body: template.body
        ]
        
        // Simple placeholder replacement
        data.each { key, value ->
            def placeholder = "\${${key}}"
            rendered.subject = rendered.subject?.replace(placeholder, value?.toString() ?: '')
            rendered.body = rendered.body?.replace(placeholder, value?.toString() ?: '')
        }
        
        return rendered
    }
    
    // Queue email for sending
    def queueEmail = { Map<String, Object> emailConfig ->
        def email = [
            to: emailConfig.to,
            cc: emailConfig.cc ?: [],
            bcc: emailConfig.bcc ?: [],
            subject: emailConfig.subject,
            body: emailConfig.body,
            bodyType: emailConfig.bodyType ?: 'HTML',
            attachments: emailConfig.attachments ?: [],
            priority: emailConfig.priority ?: 'NORMAL',
            scheduledTime: emailConfig.scheduledTime,
            metadata: emailConfig.metadata ?: [:],
            queuedAt: Emery.util.currentDate()
        ]
        
        sendQueue.add(email)
        return email
    }
    
    // Send queued emails
    def flushQueue = { int batchSize ->
        def results = [sent: 0, failed: 0, errors: []]
        
        def toSend = sendQueue.take(batchSize)
        sendQueue = sendQueue.drop(batchSize)
        
        toSend.each { email ->
            try {
                Emery.email.send(email.to, email.cc, email.bcc, email.subject, email.body, email.bodyType, email.attachments)
                results.sent++
            } catch (Exception e) {
                results.failed++
                results.errors.add([email: email.to, error: e.message])
            }
        }
        
        return results
    }
    
    // Send notification to users
    def sendNotificationToUsers = { List<String> userIds, String templateId, Map<String, Object> data ->
        def results = [queued: 0, skipped: 0]
        
        userIds.each { userId ->
            try {
                // Get user email
                SelectQuery userQuery = new SelectQuery.Builder("MS_USERS")
                    .columns(["USER_ID", "EMAIL", "FIRST_NAME", "LAST_NAME", "STATUS"])
                    .addCondition(Condition.eq("USER_ID", userId))
                    .addCondition(Condition.eq("STATUS", "ACTIVE"))
                    .loadRegions(false)
                    .build()
                
                def userResult = Emery.dataobject.fetch(userQuery)
                
                if (userResult.dataObjects && userResult.dataObjects.size() > 0) {
                    def user = userResult.dataObjects[0]
                    def userData = data + [
                        userName: "${user.first_name} ${user.last_name}",
                        userEmail: user.email
                    ]
                    
                    def rendered = renderTemplate(templateId, userData)
                    
                    if (rendered) {
                        queueEmail([
                            to: [user.email],
                            subject: rendered.subject,
                            body: rendered.body,
                            metadata: [userId: userId, templateId: templateId]
                        ])
                        results.queued++
                    }
                } else {
                    results.skipped++
                }
            } catch (Exception e) {
                Emery.log.warn("Failed to queue notification for user {}: {}", userId, e.message)
                results.skipped++
            }
        }
        
        return results
    }
    
    return [
        loadTemplate: loadTemplate,
        renderTemplate: renderTemplate,
        queueEmail: queueEmail,
        flushQueue: flushQueue,
        sendNotificationToUsers: sendNotificationToUsers,
        getQueueSize: { sendQueue.size() }
    ]
}

// Initialize email service
def emailOps = emailServiceHandler()

// Complex impact analysis for function changes
def performImpactAnalysis = { String functionId, String changeType, Map<String, Object> changeDetails ->
    def analysis = [
        functionId: functionId,
        changeType: changeType,
        analyzedAt: Emery.util.currentDate(),
        impactedObjects: [],
        impactedUsers: [],
        riskLevel: 'LOW',
        recommendations: []
    ]
    
    try {
        // Find dependent functions
        SelectQuery dependentQuery = new SelectQuery.Builder("MS_GRC_FUNCTION_DEPENDENCY")
            .columns(["DEPENDENT_FUNCTION_ID", "DEPENDENCY_TYPE", "IS_CRITICAL"])
            .addCondition(Condition.eq("FUNCTION_ID", functionId))
            .addCondition(Condition.eq("STATUS", "ACTIVE"))
            .loadRegions(false)
            .build()
        
        def dependentResult = Emery.dataobject.fetch(dependentQuery)
        
        dependentResult.dataObjects?.each { dep ->
            analysis.impactedObjects.add([
                objectId: dep.dependent_function_id,
                objectType: 'FUNCTION',
                dependencyType: dep.dependency_type,
                isCritical: dep.is_critical == 'Y'
            ])
            
            if (dep.is_critical == 'Y') {
                analysis.riskLevel = 'HIGH'
            } else if (analysis.riskLevel == 'LOW') {
                analysis.riskLevel = 'MEDIUM'
            }
        }
        
        // Find related controls
        SelectQuery controlsQuery = new SelectQuery.Builder("MS_GRC_REL_INSTANCE")
            .columns(["DEST_OBJ_ID", "DEST_OBJ_TYPE", "REL_TYPE"])
            .addCondition(Condition.eq("SRC_OBJ_ID", functionId))
            .addCondition(Condition.eq("DEST_OBJ_TYPE", "CONTROL"))
            .addCondition(Condition.eq("STATUS", "ACTIVE"))
            .loadRegions(false)
            .build()
        
        def controlsResult = Emery.dataobject.fetch(controlsQuery)
        
        controlsResult.dataObjects?.each { ctrl ->
            analysis.impactedObjects.add([
                objectId: ctrl.dest_obj_id,
                objectType: ctrl.dest_obj_type,
                relationType: ctrl.rel_type
            ])
        }
        
        // Find owner users
        if (changeDetails.ownerOrganizations) {
            def orgIds = changeDetails.ownerOrganizations.toString().split(',').toList()
            def users = Emery.mdos.getUsersBasedOnActivityAndOrganization(orgIds, ['FUNCTION_OWNER'], HierarchyAccess.EXACT)
            
            users?.each { user ->
                analysis.impactedUsers.add([
                    userId: user.userId,
                    userName: user.userName,
                    role: 'FUNCTION_OWNER'
                ])
            }
        }
        
        // Generate recommendations
        if (analysis.riskLevel == 'HIGH') {
            analysis.recommendations.add('Review all critical dependencies before proceeding')
            analysis.recommendations.add('Notify all impacted function owners')
            analysis.recommendations.add('Consider scheduling change during maintenance window')
        } else if (analysis.riskLevel == 'MEDIUM') {
            analysis.recommendations.add('Review impacted objects for potential issues')
        }
        
        if (analysis.impactedObjects.size() > 10) {
            analysis.recommendations.add('Large number of impacted objects - consider phased rollout')
        }
        
    } catch (Exception e) {
        Emery.log.error("Impact analysis failed for function {}: {}", functionId, e.message)
        analysis.error = e.message
    }
    
    impactAnalysisResults.add(analysis)
    return analysis
}

// Data table operations with caching
def dataTableOperations = {
    def tableCache = [:]
    def queryCache = [:]
    
    // Read with caching
    def readWithCache = { String tableName, boolean forceRefresh ->
        if (!forceRefresh && tableCache.containsKey(tableName)) {
            return tableCache[tableName]
        }
        
        try {
            def records = Emery.dataTable.read(tableName)
            tableCache[tableName] = records
            return records
        } catch (Exception e) {
            Emery.log.error("Failed to read data table {}: {}", tableName, e.message)
            return []
        }
    }
    
    // Find in table
    def findInTable = { String tableName, Map<String, Object> criteria ->
        def records = readWithCache(tableName, false)
        return records.findAll { record ->
            criteria.every { key, value ->
                record[key] == value
            }
        }
    }
    
    // Find first in table
    def findFirstInTable = { String tableName, Map<String, Object> criteria ->
        def records = findInTable(tableName, criteria)
        return records.size() > 0 ? records[0] : null
    }
    
    // Clear cache
    def clearCache = { String tableName ->
        if (tableName) {
            tableCache.remove(tableName)
        } else {
            tableCache.clear()
        }
    }
    
    return [
        readWithCache: readWithCache,
        findInTable: findInTable,
        findFirstInTable: findFirstInTable,
        clearCache: clearCache
    ]
}

// Initialize data table operations
def dataTableOps = dataTableOperations()

// Main processing block
try {
    Emery.log.info("Starting main processing for function metric_id: {} with upload_type: {}", F.metric_id, F.upload_type)
    
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
    
    /* Upload type specific handling */
    if (!F.upload_type) {
        def current_status = F.dd_current_status
        def current_stage = F.dd_current_stage
        def obj_status = F.obj_status
        
        Emery.log.debug("Script Name: {} executed for status: {}, stage: {}, obj_status: {}", script_name, current_status, current_stage, obj_status)
        
        // Complex creator assignment logic
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
        
        // Check if blueprint mode is enabled
        if (F.blueprint_id && F.blueprint_id != '') {
            blueprintMode = true
            blueprintContext.blueprintId = F.blueprint_id
            
            def bpHandler = new BlueprintTemplateHandler()
            bpHandler.registerTransformer('uppercase', { value, data -> value?.toString()?.toUpperCase() })
            bpHandler.registerTransformer('lowercase', { value, data -> value?.toString()?.toLowerCase() })
            bpHandler.registerTransformer('trim', { value, data -> value?.toString()?.trim() })
            
            def instantiationResult = bpHandler.instantiateTemplate(F.blueprint_id, [
                object_name: F.object_name,
                object_type: F.dd_object_type,
                owner_orgs: F.org_groups
            ])
            
            if (instantiationResult.success) {
                blueprintContext.instance = instantiationResult.instance
                Emery.log.info("Blueprint {} instantiated successfully", F.blueprint_id)
            } else {
                Emery.log.warn("Blueprint instantiation failed: {}", instantiationResult.errors?.join(', '))
            }
        }
        
        // Perform impact analysis for significant changes
        if (F.object_id && F.object_id != '' && F.object_id != 'NONE') {
            def impactAnalysis = performImpactAnalysis(F.object_id, 'PUBLISH', [
                ownerOrganizations: F.org_groups,
                objectName: F.object_name
            ])
            
            processingContext.impactAnalysis = impactAnalysis
            
            if (impactAnalysis.riskLevel == 'HIGH') {
                Emery.log.warn("High risk change detected for function {}. Impacted objects: {}", F.object_id, impactAnalysis.impactedObjects.size())
            }
        }
    }
    
    /* Non-multirow field updates */
    current_date = Emery.dateUtil.convertStringToDate(Emery.dateUtil.convertDateToString(Emery.util.currentDate(), "MM/dd/yyyy"), "MM/dd/yyyy")
    
    if (F.valid_from == null) {
        valid_from_date = current_date
    } else {
        valid_from_string = Emery.dateUtil.convertDateToString(F.valid_from, "MM/dd/yyyy")
        valid_from_date = Emery.dateUtil.convertStringToDate(valid_from_string, "MM/dd/yyyy")
    }
    
    if (F.valid_until == null) {
        valid_until_date = current_date
    } else {
        valid_until_string = Emery.dateUtil.convertDateToString(F.valid_until, "MM/dd/yyyy")
        valid_until_date = Emery.dateUtil.convertStringToDate(valid_until_string, "MM/dd/yyyy")
    }
    
    valid_until_date_temp = Emery.dateUtil.addDays(valid_until_date, 1)
    
    // Object status determination
    if (F.valid_from != null && (Emery.dateUtil.compareDates(current_date, valid_from_date) == -1)) {
        F.obj_status = 'INACT'
    } else if (F.valid_until != null && (Emery.dateUtil.compareDates(valid_until_date_temp, current_date) == 0 || Emery.dateUtil.compareDates(valid_until_date_temp, current_date) == -1)) {
        F.obj_status = 'EXP'
    } else {
        F.obj_status = 'ACT'
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
    F.dd_current_stage = 'PUBLISH'
    
    // Comment insertion
    if (F.action_comments != null && F.action_comments.toString().trim() != '') {
        String result = Emery.util.insertComments(F.process_instance_id as int, F.instance_id as int, F.metric_id as int, F.created_by as int, "ACTION_COMMENTS", F.action_comments as String)
        F.action_comments = ''
    }
    
    /* Apps_ID Creation */
    if (F.object_id == '' || F.object_id == 'NONE') {
        id = Emery.util.nextAppsId("MS_GRC_FUNCTION", "GRC", "MS_GRC_FUNCTION_IDGEN")
        F.object_id = id
    }
    
    /* Multirow processing with data table operations */
    F.orb.rows.each { row ->
        if (row.rel_inst_id == '' || row.rel_inst_id == 'NONE') {
            id = Emery.util.nextAppsId("MS_GRC_FUNCTION", "GRC", "MS_ORB_REL_INST_ID")
            row.rel_inst_id = id
        }
        
        if (row.src_obj_id == '' || row.src_obj_id == null) {
            row.src_obj_id = F.object_id
        }
        
        if (row.rel_config_id == '' || row.rel_config_id == null) {
            def targetRecord = dataTableOps.findFirstInTable("MS_GRC_ORB_CONFIG_FORM_TITLE", [ORB_OBJ_TYPE: row.additional_column4])
            
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
        id = Emery.util.nextAppsId("MS_GRC_FUNCTION", "GRC", "MS_ORB_REL_SOURCE_ID")
        F.rel_source_id = id
    }
    
    if (F.rel_source_object_id == '' || F.rel_source_object_id == 'NONE') {
        F.rel_source_object_id = F.object_id
    }
    
    // Object name change detection
    SelectQuery nameQuery = new SelectQuery.Builder("MS_GRC_FUNCTION")
        .columns(["OBJECT_NAME"])
        .addCondition(Condition.eq("DD_OBJECT_TYPE", F.dd_object_type))
        .addCondition(Condition.eq("OBJECT_ID", F.object_id))
        .loadRegions(false)
        .build()
    
    SelectQueryResponse nameResult = Emery.dataobject.fetch(nameQuery)
    List<DataObject> nameObjects = nameResult.dataObjects
    
    for (DataObject dataObject : nameObjects) {
        lv_old_object_name = dataObject.object_name
    }
    
    lv_flag = (F.object_name == lv_old_object_name) ? 'NAME_CHANGE=N' : 'NAME_CHANGE=Y'
    
    F.additional_column_h1 = lv_flag
    F.created_by = 100000
    
    if (F.created_by_hidden != '') F.obj_created_by = F.created_by_hidden
    if (F.modif_req_by_hidden != '') F.obj_modified_by = F.modif_req_by_hidden
    
    F.dd_current_status = 'NEW'
    
    if (F.own_app_config_temp == 'Yes' || F.own_app_config_temp == 'No') {
        F.own_app_config_temp = F.own_app_config
    }
    
    // Send notifications for impacted users
    if (impactAnalysisResults.size() > 0 && impactAnalysisResults[0].impactedUsers.size() > 0) {
        def userIds = impactAnalysisResults[0].impactedUsers.collect { it.userId }
        def notifResult = emailOps.sendNotificationToUsers(userIds, 'FUNCTION_CHANGE_NOTIFICATION', [
            functionName: F.object_name,
            functionId: F.object_id,
            changeType: 'PUBLISHED',
            changedBy: F.dd_current_user_name
        ])
        
        processingContext.notificationsSent = notifResult.queued
        
        // Flush email queue
        if (emailOps.getQueueSize() > 0) {
            def flushResult = emailOps.flushQueue(EMAIL_BATCH_SIZE)
            processingContext.emailsSent = flushResult.sent
            processingContext.emailsFailed = flushResult.failed
        }
    }
    
    // Final processing context update
    processingContext.endTime = System.currentTimeMillis()
    processingContext.duration = processingContext.endTime - processingContext.startTime
    processingContext.blueprintMode = blueprintMode
    processingContext.auditEntries = auditTrailEntries.size()
    
    Emery.log.info("Processing completed in {}ms", processingContext.duration)
    
} catch (Exception e) {
    Emery.log.error("Error in {}: {}", script_name, e.message)
    isRollbackRequired = true
    throw e
} finally {
    dataTableOps.clearCache(null)
    Emery.log.info("Script {} completed with rollback={}", script_name, isRollbackRequired)
}
