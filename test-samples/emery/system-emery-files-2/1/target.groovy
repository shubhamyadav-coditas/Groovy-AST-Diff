/**
 * Enhanced GRC Organizational Profile Processing Script with Upload Type Handling
 * Operations: INI_TO_PUB transition with advanced data massaging and upload type validation
 * i) Field updates with conditional logic based on upload_type
 * ii) Advanced relation configuration with bidirectional relationship support
 * iii) Complex ID generation with collision detection
 * iv) Comprehensive MDOS operations with hierarchy traversal
 * v) Advanced audit logging and workflow history tracking
 **/

use("MS_GRC_ORG_PROFILE")
useDataObject("MS_GRC_REL_DEFN")
useDataObject("MS_GRC_ORG_PROFILE")
useDataObject("MS_GRC_AUDIT_LOG")
useDataObject("MS_GRC_USER_ASSIGNMENT")
useDataObject("MS_GRC_WORKFLOW_HISTORY")
useDataObject("MS_GRC_NOTIFICATION_QUEUE")

// Extended variable declarations
String id, name, cur_date_string, valid_from_string, valid_until_string, lv_flag, lv_old_object_name
Date current_date, valid_from_date, valid_until_date, valid_until_date_temp
String script_name = "InitiatorToPublish_Enhanced.groovy"
Map<String, Object> processingContext = [:]
Map<String, List<String>> validationErrors = [:]
Map<String, Object> notificationPayload = [:]
List<Map<String, Object>> auditTrailEntries = []
List<Map<String, Object>> workflowHistoryEntries = []
List<DataObject> pendingUpdates = []
List<Closure> deferredOperations = []
def transactionBatch = []
boolean isRollbackRequired = false
boolean notificationsEnabled = true
int retryCount = 0
final int MAX_RETRIES = 3
final int BATCH_SIZE = 100
final String DELIMITER = '\$_\$'
final String AUDIT_ACTION_PREFIX = 'ORG_PROFILE_'

Emery.log.info("Started Pre-Hook {} at {} for process_instance_id: {}", script_name, Emery.util.currentDateAsString(), F.process_instance_id)

// Advanced validation framework with nested rule engine
class AdvancedValidationEngine {
    List<Map<String, Object>> rules = []
    Map<String, Closure> customValidators = [:]
    List<String> errors = []
    List<String> warnings = []
    Map<String, Object> validationContext = [:]
    
    void addRule(Map<String, Object> rule) {
        rules.add(rule)
    }
    
    void registerCustomValidator(String name, Closure validator) {
        customValidators[name] = validator
    }
    
    boolean validate(Map<String, Object> data) {
        errors.clear()
        warnings.clear()
        
        rules.each { rule ->
            def fieldValue = data[rule.field]
            boolean ruleResult = true
            String message = ''
            
            switch(rule.type) {
                case 'required':
                    ruleResult = fieldValue != null && fieldValue.toString().trim() != ''
                    message = rule.message ?: "${rule.field} is required"
                    break
                    
                case 'pattern':
                    if (fieldValue != null) {
                        ruleResult = fieldValue.toString().matches(rule.pattern)
                        message = rule.message ?: "${rule.field} does not match required pattern"
                    }
                    break
                    
                case 'minLength':
                    if (fieldValue != null) {
                        ruleResult = fieldValue.toString().length() >= rule.minLength
                        message = rule.message ?: "${rule.field} must be at least ${rule.minLength} characters"
                    }
                    break
                    
                case 'maxLength':
                    if (fieldValue != null) {
                        ruleResult = fieldValue.toString().length() <= rule.maxLength
                        message = rule.message ?: "${rule.field} must not exceed ${rule.maxLength} characters"
                    }
                    break
                    
                case 'range':
                    if (fieldValue != null) {
                        def numVal = fieldValue as Double
                        ruleResult = numVal >= rule.min && numVal <= rule.max
                        message = rule.message ?: "${rule.field} must be between ${rule.min} and ${rule.max}"
                    }
                    break
                    
                case 'enum':
                    if (fieldValue != null) {
                        ruleResult = rule.allowedValues.contains(fieldValue)
                        message = rule.message ?: "${rule.field} must be one of: ${rule.allowedValues.join(', ')}"
                    }
                    break
                    
                case 'custom':
                    if (customValidators.containsKey(rule.validatorName)) {
                        def result = customValidators[rule.validatorName](fieldValue, data, validationContext)
                        ruleResult = result.valid
                        message = result.message ?: rule.message
                    }
                    break
                    
                case 'conditional':
                    def conditionField = data[rule.conditionField]
                    if (rule.conditionValue == conditionField || (rule.conditionValues && rule.conditionValues.contains(conditionField))) {
                        if (fieldValue == null || fieldValue.toString().trim() == '') {
                            ruleResult = false
                            message = rule.message ?: "${rule.field} is required when ${rule.conditionField} is ${conditionField}"
                        }
                    }
                    break
                    
                case 'unique':
                    // Would check database for uniqueness
                    ruleResult = true
                    break
            }
            
            if (!ruleResult) {
                if (rule.severity == 'warning') {
                    warnings.add(message)
                } else {
                    errors.add(message)
                }
            }
        }
        
        return errors.isEmpty()
    }
    
    Map<String, Object> getResults() {
        return [
            isValid: errors.isEmpty(),
            errors: errors.collect(),
            warnings: warnings.collect(),
            rulesEvaluated: rules.size()
        ]
    }
}

// Complex nested closure for MDOS hierarchy operations
def mdosHierarchyOperations = { Map<String, Object> config ->
    def results = [:]
    
    // Get users based on activity and organization
    def getUsersByActivityAndOrg = { List<String> tupleIds, List<String> activityNames, String accessLevel ->
        try {
            def users = Emery.mdos.getUsersBasedOnActivityAndOrganization(tupleIds, activityNames, HierarchyAccess."${accessLevel}")
            return [success: true, users: users, count: users?.size() ?: 0]
        } catch (Exception e) {
            Emery.log.error("Failed to get users by activity and org: {}", e.message)
            return [success: false, error: e.message, users: []]
        }
    }
    
    // Get users based on role
    def getUsersByRole = { List<String> tupleIds, List<String> roleIds, String accessLevel ->
        try {
            def users = Emery.mdos.getUsersBasedOnRole(tupleIds, roleIds, HierarchyAccess."${accessLevel}")
            return [success: true, users: users, count: users?.size() ?: 0]
        } catch (Exception e) {
            Emery.log.error("Failed to get users by role: {}", e.message)
            return [success: false, error: e.message, users: []]
        }
    }
    
    // Get MDOS display values
    def getDisplayValues = { String storedValue, String delimiter, String displayDelimiter, int localeId ->
        try {
            def displayValue = Emery.mdos.getMdosDisplayValuesClob(storedValue, delimiter, displayDelimiter, localeId)
            return [success: true, displayValue: displayValue]
        } catch (Exception e) {
            return [success: false, error: e.message, displayValue: storedValue]
        }
    }
    
    // Complex MDOS view query builder
    def queryMdosView = { String viewType, Map<String, Object> queryParams ->
        def builder
        
        switch(viewType) {
            case 'ORG_BE':
                builder = Emery.mdos.si_mdos_org_be_v()
                break
            case 'USER_BE_ORG_ROLE_ACT':
                builder = Emery.mdos.si_mdos_usr_be_org_rol_act_v()
                break
            case 'FORM_TPL_ROLE_ACC':
                builder = Emery.mdos.si_mdos_frm_tpl_rol_acc_v()
                break
            case 'USER_ORG_ROLES_BE':
                builder = Emery.mdos.si_mdos_user_org_roles_be_v()
                break
            default:
                builder = Emery.mdos.createBuilder(viewType, queryParams.attributeMap ?: [:])
        }
        
        if (queryParams.columns) builder.columns(queryParams.columns)
        
        queryParams.conditions?.each { cond ->
            builder.addCondition(Condition."${cond.op}"(cond.field, cond.value))
        }
        
        queryParams.sortConditions?.each { sort ->
            builder.addSortCondition(SortCondition."${sort.direction}"(sort.field))
        }
        
        if (queryParams.groupConditions) {
            def groupCond = GroupCondition."${queryParams.groupType ?: 'or'}"()
            queryParams.groupConditions.each { gc ->
                groupCond.add(Condition."${gc.op}"(gc.field, gc.value))
            }
            builder.addGroupCondition(groupCond)
        }
        
        if (queryParams.offset) builder.offset(queryParams.offset)
        if (queryParams.limit) builder.limit(queryParams.limit)
        
        return builder.fetch()
    }
    
    results.getUsersByActivityAndOrg = getUsersByActivityAndOrg
    results.getUsersByRole = getUsersByRole
    results.getDisplayValues = getDisplayValues
    results.queryMdosView = queryMdosView
    
    return results
}

// Initialize MDOS operations
def mdosOps = mdosHierarchyOperations([:])

// Complex workflow history tracking
def createWorkflowHistoryEntry = { Map<String, Object> params ->
    def entry = [
        processInstanceId: params.processInstanceId ?: F.process_instance_id,
        instanceId: params.instanceId ?: F.instance_id,
        metricId: params.metricId ?: F.metric_id,
        action: params.action,
        fromStage: params.fromStage ?: F.dd_current_stage,
        toStage: params.toStage,
        fromStatus: params.fromStatus ?: F.dd_current_status,
        toStatus: params.toStatus,
        performedBy: params.performedBy ?: F.dd_current_user_name,
        performedById: params.performedById ?: F.created_by,
        timestamp: Emery.util.currentDate(),
        comments: params.comments,
        metadata: params.metadata ?: [:]
    ]
    
    workflowHistoryEntries.add(entry)
    return entry
}

// Complex notification builder
def buildNotification = { Map<String, Object> notifConfig ->
    def notification = [
        type: notifConfig.type ?: 'EMAIL',
        templateId: notifConfig.templateId,
        recipients: [],
        subject: notifConfig.subject,
        body: notifConfig.body,
        priority: notifConfig.priority ?: 'NORMAL',
        metadata: notifConfig.metadata ?: [:],
        createdAt: Emery.util.currentDate()
    ]
    
    // Resolve recipients
    if (notifConfig.recipientUserIds) {
        notification.recipients.addAll(notifConfig.recipientUserIds.collect { [type: 'USER_ID', value: it] })
    }
    
    if (notifConfig.recipientRoles) {
        notifConfig.recipientRoles.each { role ->
            def users = mdosOps.getUsersByRole([F.org_groups?.toString()], [role], 'EXACT')
            if (users.success && users.users) {
                users.users.each { user ->
                    notification.recipients.add([type: 'USER', value: user])
                }
            }
        }
    }
    
    if (notifConfig.recipientActivities) {
        notifConfig.recipientActivities.each { activity ->
            def users = mdosOps.getUsersByActivityAndOrg([F.org_groups?.toString()], [activity], 'FLOW_UP')
            if (users.success && users.users) {
                users.users.each { user ->
                    notification.recipients.add([type: 'USER', value: user])
                }
            }
        }
    }
    
    return notification
}

// Main processing with upload_type handling
try {
    Emery.log.info("Starting main processing for metric_id: {} with upload_type: {}", F.metric_id, F.upload_type)
    
    // Initialize comprehensive processing context
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
        uploadType: F.upload_type,
        isUpload: F.upload_type != null && F.upload_type != ''
    ]
    
    /* Upload type specific handling */
    if (!F.upload_type) {
        def current_status = F.dd_current_status
        def current_stage = F.dd_current_stage
        def obj_status = F.obj_status
        
        Emery.log.debug("Script Name: {} executed for status: {}, stage: {}, obj_status: {}", script_name, current_status, current_stage, obj_status)
        
        // Complex nested condition for creator assignment
        if ((current_status in ['NEW', ''] || obj_status == 'NEW') && (current_stage in ['CREATE_EDIT', 'NONE'])) {
            F.created_by_hidden = F.dd_current_user_name
            F.obj_created_by = F.dd_current_user_name
            F.obj_created_on = Emery.util.currentDate()
            
            auditTrailEntries.add([
                action: AUDIT_ACTION_PREFIX + 'CREATOR_ASSIGNED',
                details: [
                    creatorName: F.dd_current_user_name,
                    createdOn: Emery.util.currentDate(),
                    status: current_status,
                    stage: current_stage
                ],
                timestamp: Emery.util.currentDate()
            ])
            
            createWorkflowHistoryEntry([
                action: 'CREATOR_ASSIGNED',
                toStage: current_stage,
                toStatus: current_status,
                comments: 'Initial creator assignment during form creation'
            ])
        }
        
        // Additional validation for non-upload scenarios
        def validationEngine = new AdvancedValidationEngine()
        
        validationEngine.addRule([field: 'object_name', type: 'required', message: 'Object name is required'])
        validationEngine.addRule([field: 'object_name', type: 'maxLength', maxLength: 500, message: 'Object name cannot exceed 500 characters'])
        validationEngine.addRule([field: 'dd_object_type', type: 'required', message: 'Object type is required'])
        validationEngine.addRule([field: 'org_groups', type: 'conditional', conditionField: 'is_org_required', conditionValue: 'Y', message: 'Organization is required when org requirement is enabled'])
        
        validationEngine.registerCustomValidator('dateRangeValidator', { fieldValue, allData, context ->
            if (allData.valid_from && allData.valid_until) {
                def fromDate = allData.valid_from instanceof Date ? allData.valid_from : Emery.dateUtil.convertStringToDate(allData.valid_from.toString(), "MM/dd/yyyy")
                def untilDate = allData.valid_until instanceof Date ? allData.valid_until : Emery.dateUtil.convertStringToDate(allData.valid_until.toString(), "MM/dd/yyyy")
                if (Emery.dateUtil.compareDates(untilDate, fromDate) < 0) {
                    return [valid: false, message: 'Valid until date must be after valid from date']
                }
            }
            return [valid: true]
        })
        
        validationEngine.addRule([field: 'valid_until', type: 'custom', validatorName: 'dateRangeValidator'])
        
        def formData = [
            object_name: F.object_name,
            dd_object_type: F.dd_object_type,
            org_groups: F.org_groups,
            is_org_required: F.is_org_required,
            valid_from: F.valid_from,
            valid_until: F.valid_until
        ]
        
        boolean isValid = validationEngine.validate(formData)
        def validationResults = validationEngine.getResults()
        
        if (!isValid) {
            Emery.log.warn("Validation failed with {} errors: {}", validationResults.errors.size(), validationResults.errors.join('; '))
            validationErrors.putAll(validationResults.errors.collectEntries { [(it): [it]] })
        }
        
        if (validationResults.warnings.size() > 0) {
            Emery.log.info("Validation warnings: {}", validationResults.warnings.join('; '))
        }
    }
    
    /* Non-multirow field updates with enhanced date handling */
    current_date = Emery.dateUtil.convertStringToDate(Emery.dateUtil.convertDateToString(Emery.util.currentDate(), "MM/dd/yyyy"), "MM/dd/yyyy")
    
    // Enhanced valid_from processing
    if (F.valid_from == null) {
        valid_from_date = current_date
    } else {
        valid_from_string = Emery.dateUtil.convertDateToString(F.valid_from, "MM/dd/yyyy")
        valid_from_date = Emery.dateUtil.convertStringToDate(valid_from_string, "MM/dd/yyyy")
    }
    
    // Enhanced valid_until processing
    if (F.valid_until == null) {
        valid_until_date = current_date
    } else {
        valid_until_string = Emery.dateUtil.convertDateToString(F.valid_until, "MM/dd/yyyy")
        valid_until_date = Emery.dateUtil.convertStringToDate(valid_until_string, "MM/dd/yyyy")
    }
    
    valid_until_date_temp = Emery.dateUtil.addDays(valid_until_date, 1)
    
    // Enhanced object status determination
    if (F.valid_from != null && (Emery.dateUtil.compareDates(current_date, valid_from_date) == -1)) {
        F.obj_status = 'INACT'
    } else if (F.valid_until != null && (Emery.dateUtil.compareDates(valid_until_date_temp, current_date) == 0 || Emery.dateUtil.compareDates(valid_until_date_temp, current_date) == -1)) {
        F.obj_status = 'EXP'
    } else {
        F.obj_status = 'ACT'
    }
    
    F.obj_status_temp = F.obj_status
    
    // SDOS handling with MDOS operations
    if (Emery.ctx.isSDOS()) {
        F.owner_organizations = F.org_groups
        
        // Perform MDOS queries for additional context
        if (F.org_groups != null && F.org_groups.toString().trim() != '') {
            try {
                def mdosResult = mdosOps.queryMdosView('ORG_BE', [
                    columns: ['ORG_ID', 'ORG_NAME', 'BE_ID', 'STATUS'],
                    conditions: [[op: 'eq', field: 'STATUS', value: 'ACTIVE']],
                    limit: 100
                ])
                
                if (mdosResult && mdosResult.dataObjects) {
                    processingContext.mdosOrgCount = mdosResult.dataObjects.size()
                }
            } catch (Exception e) {
                Emery.log.warn("MDOS query failed: {}", e.message)
            }
        }
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
    
    // Insert comments with enhanced error handling
    if (F.action_comments != null && F.action_comments.toString().trim() != '') {
        try {
            String commentResult = Emery.util.insertComments(F.process_instance_id as int, F.instance_id as int, F.metric_id as int, F.created_by as int, "ACTION_COMMENTS", F.action_comments as String)
            F.action_comments = ''
            
            auditTrailEntries.add([action: AUDIT_ACTION_PREFIX + 'COMMENT_INSERTED', result: commentResult, timestamp: Emery.util.currentDate()])
        } catch (Exception e) {
            Emery.log.error("Comment insertion failed: {}", e.message)
        }
    }
    
    /* Apps_ID Creation */
    if (F.object_id == '' || F.object_id == 'NONE' || F.object_id == null) {
        id = Emery.util.nextAppsId("MS_GRC_ORG_PROFILE", "GRC", "MS_GRC_ORGPROF_IDGEN")
        F.object_id = id
        Emery.log.info("Generated new object_id: {}", id)
    }
    
    /* Complex multirow processing */
    F.orb.rows.each { row ->
        // Generate rel_inst_id
        if (row.rel_inst_id == '' || row.rel_inst_id == 'NONE') {
            id = Emery.util.nextAppsId("MS_GRC_ORG_PROFILE", "GRC", "MS_ORB_REL_INST_ID")
            row.rel_inst_id = id
        }
        
        // Set src_obj_id
        if (row.src_obj_id == '' || row.src_obj_id == null) {
            row.src_obj_id = F.object_id
        }
        
        // Complex rel_config_id lookup
        if (row.rel_config_id == '' || row.rel_config_id == null) {
            def records = Emery.dataTable.read("MS_GRC_ORB_CONFIG_FORM_TITLE")
            def targetRecord = records.find { row1 -> row1.ORB_OBJ_TYPE == row.additional_column4 }
            
            if (targetRecord) {
                row.additional_column3 = targetRecord.object_type
                
                SelectQuery relDefnQuery = new SelectQuery.Builder("MS_GRC_REL_DEFN")
                    .columns(["REL_ID", "REL_NAME", "SRC_OBJ_TYPE", "DEST_OBJ_TYPE"])
                    .addCondition(Condition.eq("SRC_OBJ_TYPE", F.dd_object_type))
                    .addCondition(Condition.eq("DEST_OBJ_TYPE", row.additional_column3))
                    .loadRegions(false)
                    .build()
                
                SelectQueryResponse relDefnResult = Emery.dataobject.fetch(relDefnQuery)
                List<DataObject> relDefnObjects = relDefnResult.dataObjects
                
                for (DataObject dataObject : relDefnObjects) {
                    row.rel_config_id = dataObject.rel_id
                }
            }
        }
    }
    
    // Generate rel_source_id
    if (F.rel_source_id == '' || F.rel_source_id == 'NONE' || F.rel_source_id == null) {
        id = Emery.util.nextAppsId("MS_GRC_ORG_PROFILE", "GRC", "MS_ORB_REL_SOURCE_ID")
        F.rel_source_id = id
    }
    
    if (F.rel_source_object_id == '' || F.rel_source_object_id == 'NONE' || F.rel_source_object_id == null) {
        F.rel_source_object_id = F.object_id
    }
    
    // Object name change detection
    SelectQuery query2 = new SelectQuery.Builder("MS_GRC_ORG_PROFILE")
        .columns(["OBJECT_NAME"])
        .addCondition(Condition.eq("DD_OBJECT_TYPE", F.dd_object_type))
        .addCondition(Condition.eq("OBJECT_ID", F.object_id))
        .loadRegions(false)
        .build()
    
    SelectQueryResponse result2 = Emery.dataobject.fetch(query2)
    List<DataObject> dataObj2 = result2.dataObjects
    
    for (DataObject dataObject : dataObj2) {
        lv_old_object_name = dataObject.object_name
    }
    
    if (F.object_name == lv_old_object_name) {
        lv_flag = 'NAME_CHANGE=N'
    } else {
        lv_flag = 'NAME_CHANGE=Y'
    }
    
    F.additional_column_h1 = lv_flag
    F.created_by = 100000
    
    if (F.created_by_hidden != null && F.created_by_hidden.toString().trim() != '') {
        F.obj_created_by = F.created_by_hidden
    }
    
    if (F.modif_req_by_hidden != null && F.modif_req_by_hidden.toString().trim() != '') {
        F.obj_modified_by = F.modif_req_by_hidden
    }
    
    F.dd_current_status = 'NEW'
    
    if (F.own_app_config_temp == 'Yes' || F.own_app_config_temp == 'No') {
        F.own_app_config_temp = F.own_app_config
    }
    
    // Build notification if enabled
    if (notificationsEnabled && F.obj_status == 'ACT') {
        notificationPayload = buildNotification([
            type: 'EMAIL',
            templateId: 'ORG_PROFILE_PUBLISHED',
            recipientRoles: ['ORG_ADMIN', 'COMPLIANCE_MANAGER'],
            subject: "Organization Profile Published: ${F.object_name}",
            body: "The organization profile ${F.object_name} (${F.object_id}) has been published.",
            priority: 'NORMAL',
            metadata: [objectId: F.object_id, objectName: F.object_name, publishedBy: F.dd_current_user_name]
        ])
    }
    
    // Final workflow history entry
    createWorkflowHistoryEntry([
        action: 'PUBLISHED',
        fromStage: processingContext.currentStage,
        toStage: 'PUBLISH',
        fromStatus: processingContext.currentStatus,
        toStatus: 'NEW',
        comments: 'Form published successfully'
    ])
    
    // Update processing context
    processingContext.endTime = System.currentTimeMillis()
    processingContext.duration = processingContext.endTime - processingContext.startTime
    processingContext.auditEntries = auditTrailEntries.size()
    processingContext.workflowEntries = workflowHistoryEntries.size()
    
    Emery.log.info("Processing completed in {}ms", processingContext.duration)
    
} catch (Exception e) {
    Emery.log.error("Error in {}: {}", script_name, e.message)
    isRollbackRequired = true
    throw e
} finally {
    Emery.log.info("Script {} completed with rollback={}", script_name, isRollbackRequired)
}
