/**
 * Complex GRC Organizational Profile Processing Script
 * Operations: INI_TO_PUB transition with advanced data massaging
 * i) Field updates: obj_status, obj_status_temp, owner_organizations, wfi_display, wfi_stored, dd_current_stage, action_comments
 * ii) Relation config: rel_config_id, additional_column3, rel_source_object_id, additional_column_h1, obj_created_by, obj_modified_by, dd_current_status
 * iii) ID Generation: object_id, multirow_id, rel_source_id
 * iv) Advanced MDOS operations and complex nested validations
 **/

use("MS_GRC_ORG_PROFILE")
useDataObject("MS_GRC_REL_DEFN")
useDataObject("MS_GRC_ORG_PROFILE")
useDataObject("MS_GRC_AUDIT_LOG")
useDataObject("MS_GRC_USER_ASSIGNMENT")
useDataObject("MS_GRC_WORKFLOW_HISTORY")

// Variable declarations with complex types
String id, name, cur_date_string, valid_from_string, valid_until_string, lv_flag, lv_old_object_name
Date current_date, valid_from_date, valid_until_date, valid_until_date_temp
String script_name = "InitiatorToPublish_Advanced.groovy"
Map<String, Object> processingContext = [:]
Map<String, List<String>> validationErrors = [:]
List<Map<String, Object>> auditTrailEntries = []
List<DataObject> pendingUpdates = []
def transactionBatch = []
boolean isRollbackRequired = false
int retryCount = 0
final int MAX_RETRIES = 3
final String DELIMITER = '\$_\$'

Emery.log.info("Started Pre-Hook {} at {}", script_name, Emery.util.currentDateAsString())

// Complex nested class for validation results
class ValidationResult {
    boolean isValid = true
    List<String> errors = []
    List<String> warnings = []
    Map<String, Object> metadata = [:]
    
    void addError(String field, String message) {
        isValid = false
        errors.add("${field}: ${message}")
    }
    
    void addWarning(String field, String message) {
        warnings.add("${field}: ${message}")
    }
    
    String toString() {
        return "ValidationResult[valid=${isValid}, errors=${errors.size()}, warnings=${warnings.size()}]"
    }
}

// Complex nested closure for deep validation
def deepFieldValidator = { Map<String, Object> fieldConfig, Object fieldValue, ValidationResult result ->
    def validators = fieldConfig.validators ?: []
    validators.each { validator ->
        switch(validator.type) {
            case 'required':
                if (fieldValue == null || fieldValue.toString().trim().isEmpty()) {
                    result.addError(fieldConfig.name, validator.message ?: "Field is required")
                }
                break
            case 'pattern':
                if (fieldValue != null && !fieldValue.toString().matches(validator.pattern)) {
                    result.addError(fieldConfig.name, validator.message ?: "Field does not match pattern")
                }
                break
            case 'range':
                if (fieldValue != null) {
                    def numValue = fieldValue as Double
                    if (numValue < validator.min || numValue > validator.max) {
                        result.addError(fieldConfig.name, "Value must be between ${validator.min} and ${validator.max}")
                    }
                }
                break
            case 'custom':
                if (validator.closure && !validator.closure(fieldValue)) {
                    result.addError(fieldConfig.name, validator.message ?: "Custom validation failed")
                }
                break
            case 'dateRange':
                if (fieldValue != null) {
                    Date dateVal = fieldValue instanceof Date ? fieldValue : Emery.dateUtil.convertStringToDate(fieldValue.toString(), "MM/dd/yyyy")
                    if (validator.minDate && Emery.dateUtil.compareDates(dateVal, validator.minDate) < 0) {
                        result.addError(fieldConfig.name, "Date must be after ${Emery.dateUtil.convertDateToString(validator.minDate, 'MM/dd/yyyy')}")
                    }
                    if (validator.maxDate && Emery.dateUtil.compareDates(dateVal, validator.maxDate) > 0) {
                        result.addError(fieldConfig.name, "Date must be before ${Emery.dateUtil.convertDateToString(validator.maxDate, 'MM/dd/yyyy')}")
                    }
                }
                break
            case 'dependency':
                def dependentField = validator.dependsOn
                def dependentValue = F."${dependentField}"
                if (validator.condition(dependentValue) && (fieldValue == null || fieldValue.toString().isEmpty())) {
                    result.addError(fieldConfig.name, "Field is required when ${dependentField} is ${dependentValue}")
                }
                break
        }
    }
}

// Advanced MDOS query builder with complex nested conditions
def buildComplexMdosQuery = { String viewName, Map<String, Object> queryParams ->
    def builder = Emery.mdos.createBuilder(viewName, [
        'ORG_ID': 'Number',
        'ORG_NAME': 'Normal Text',
        'BE_ID': 'Number',
        'BE_NAME': 'Normal Text',
        'TUPLE_ID': 'Number',
        'STATUS': 'Normal Text',
        'CREATED_DATE': 'Date',
        'MODIFIED_DATE': 'Date'
    ])
    
    builder.columns(queryParams.columns ?: ['*'])
    
    if (queryParams.conditions) {
        queryParams.conditions.each { condition ->
            switch(condition.operator) {
                case 'eq':
                    builder.addCondition(Condition.eq(condition.field, condition.value))
                    break
                case 'ne':
                    builder.addCondition(Condition.ne(condition.field, condition.value))
                    break
                case 'gt':
                    builder.addCondition(Condition.gt(condition.field, condition.value))
                    break
                case 'lt':
                    builder.addCondition(Condition.lt(condition.field, condition.value))
                    break
                case 'like':
                    builder.addCondition(Condition.like(condition.field, condition.value))
                    break
                case 'in':
                    builder.addCondition(Condition.inList(condition.field, condition.value))
                    break
                case 'between':
                    builder.addCondition(Condition.between(condition.field, condition.value[0], condition.value[1]))
                    break
            }
        }
    }
    
    if (queryParams.groupConditions) {
        def groupCond = GroupCondition.or()
        queryParams.groupConditions.each { gc ->
            groupCond.add(Condition.eq(gc.field, gc.value))
        }
        builder.addGroupCondition(groupCond)
    }
    
    if (queryParams.sortConditions) {
        queryParams.sortConditions.each { sc ->
            builder.addSortCondition(SortCondition."${sc.direction}"(sc.field))
        }
    }
    
    if (queryParams.offset) builder.offset(queryParams.offset)
    if (queryParams.limit) builder.limit(queryParams.limit)
    
    return builder.fetch()
}

// Complex recursive function for hierarchy traversal
def traverseOrgHierarchy
traverseOrgHierarchy = { String tupleId, int depth, int maxDepth, List<Map> accumulator ->
    if (depth > maxDepth) return accumulator
    
    try {
        SelectQuery hierarchyQuery = new SelectQuery.Builder("MS_GRC_ORG_HIERARCHY")
            .columns(["PARENT_TUPLE_ID", "CHILD_TUPLE_ID", "HIERARCHY_LEVEL", "PATH", "ORG_NAME", "ORG_TYPE", "STATUS", "EFFECTIVE_FROM", "EFFECTIVE_TO"])
            .addCondition(Condition.eq("PARENT_TUPLE_ID", tupleId))
            .addCondition(Condition.eq("STATUS", "ACTIVE"))
            .addCondition(Condition.or(Condition.isNull("EFFECTIVE_TO"), Condition.gt("EFFECTIVE_TO", Emery.util.currentDate())))
            .loadRegions(false)
            .build()
        
        SelectQueryResponse hierarchyResult = Emery.dataobject.fetch(hierarchyQuery)
        List<DataObject> childOrgs = hierarchyResult.dataObjects
        
        childOrgs.each { childOrg ->
            Map<String, Object> orgNode = [
                tupleId: childOrg.child_tuple_id,
                parentTupleId: childOrg.parent_tuple_id,
                level: childOrg.hierarchy_level,
                path: childOrg.path,
                name: childOrg.org_name,
                type: childOrg.org_type,
                depth: depth,
                children: []
            ]
            
            if (depth < maxDepth) {
                orgNode.children = traverseOrgHierarchy(childOrg.child_tuple_id.toString(), depth + 1, maxDepth, [])
            }
            
            accumulator.add(orgNode)
        }
    } catch (Exception e) {
        Emery.log.error("Error traversing hierarchy at depth {} for tuple {}: {}", depth, tupleId, e.message)
    }
    
    return accumulator
}

// Complex batch processing with retry logic
def processBatchWithRetry = { List<Map<String, Object>> batchItems, Closure processor, int maxRetries ->
    def results = [success: [], failed: [], skipped: []]
    
    batchItems.eachWithIndex { item, index ->
        int attempts = 0
        boolean processed = false
        Exception lastException = null
        
        while (attempts < maxRetries && !processed) {
            try {
                attempts++
                Emery.log.debug("Processing batch item {} (attempt {}/{})", index, attempts, maxRetries)
                
                def result = processor(item)
                
                if (result.success) {
                    results.success.add([index: index, item: item, result: result])
                    processed = true
                } else if (result.skip) {
                    results.skipped.add([index: index, item: item, reason: result.reason])
                    processed = true
                } else {
                    lastException = new RuntimeException(result.error ?: "Unknown error")
                    Thread.sleep(100 * attempts) // Exponential backoff
                }
            } catch (Exception e) {
                lastException = e
                Emery.log.warn("Attempt {} failed for batch item {}: {}", attempts, index, e.message)
                if (attempts < maxRetries) Thread.sleep(100 * attempts)
            }
        }
        
        if (!processed) {
            results.failed.add([index: index, item: item, error: lastException?.message, attempts: attempts])
        }
    }
    
    return results
}

// Complex multirow processing with nested operations
def processMultirowRegion = { def region, Map<String, Object> context ->
    def processedRows = []
    def regionValidation = new ValidationResult()
    
    region.rows.each { row ->
        def rowContext = [
            rowId: row.unique_row_id ?: Emery.util.nextUniqueRowId(),
            parentObjectId: context.objectId,
            processingDate: Emery.util.currentDate(),
            status: 'PENDING'
        ]
        
        // Deep nested validation for each row
        if (row.rel_inst_id == '' || row.rel_inst_id == 'NONE') {
            row.rel_inst_id = Emery.util.nextAppsId("MS_GRC_ORG_PROFILE", "GRC", "MS_ORB_REL_INST_ID")
            rowContext.newRelInstId = true
        }
        
        if (row.src_obj_id == '' || row.src_obj_id == null) {
            row.src_obj_id = context.objectId
            rowContext.srcObjIdSet = true
        }
        
        // Complex rel_config_id lookup with caching
        if (row.rel_config_id == '' || row.rel_config_id == null) {
            def cacheKey = "${F.dd_object_type}_${row.additional_column4}"
            
            if (!context.relConfigCache) context.relConfigCache = [:]
            
            if (context.relConfigCache.containsKey(cacheKey)) {
                row.rel_config_id = context.relConfigCache[cacheKey].relConfigId
                row.additional_column3 = context.relConfigCache[cacheKey].objectType
            } else {
                def records = Emery.dataTable.read("MS_GRC_ORB_CONFIG_FORM_TITLE")
                def targetRecord = records.find { row1 -> row1.ORB_OBJ_TYPE == row.additional_column4 }
                
                if (targetRecord) {
                    row.additional_column3 = targetRecord.object_type
                    
                    SelectQuery relDefnQuery = new SelectQuery.Builder("MS_GRC_REL_DEFN")
                        .columns(["REL_ID", "REL_NAME", "REL_TYPE", "SRC_OBJ_TYPE", "DEST_OBJ_TYPE", "CARDINALITY", "IS_BIDIRECTIONAL"])
                        .addCondition(Condition.eq("SRC_OBJ_TYPE", F.dd_object_type))
                        .addCondition(Condition.eq("DEST_OBJ_TYPE", row.additional_column3))
                        .addCondition(Condition.eq("STATUS", "ACTIVE"))
                        .loadRegions(false)
                        .build()
                    
                    SelectQueryResponse relDefnResult = Emery.dataobject.fetch(relDefnQuery)
                    List<DataObject> relDefnObjects = relDefnResult.dataObjects
                    
                    if (relDefnObjects && relDefnObjects.size() > 0) {
                        def relDefn = relDefnObjects[0]
                        row.rel_config_id = relDefn.rel_id
                        
                        context.relConfigCache[cacheKey] = [
                            relConfigId: relDefn.rel_id,
                            objectType: row.additional_column3,
                            relName: relDefn.rel_name,
                            cardinality: relDefn.cardinality
                        ]
                        
                        // Additional nested processing for bidirectional relationships
                        if (relDefn.is_bidirectional == 'Y') {
                            SelectQuery reverseRelQuery = new SelectQuery.Builder("MS_GRC_REL_DEFN")
                                .columns(["REL_ID"])
                                .addCondition(Condition.eq("SRC_OBJ_TYPE", row.additional_column3))
                                .addCondition(Condition.eq("DEST_OBJ_TYPE", F.dd_object_type))
                                .loadRegions(false)
                                .build()
                            
                            SelectQueryResponse reverseResult = Emery.dataobject.fetch(reverseRelQuery)
                            if (reverseResult.dataObjects && reverseResult.dataObjects.size() > 0) {
                                row.reverse_rel_config_id = reverseResult.dataObjects[0].rel_id
                            }
                        }
                    }
                }
            }
        }
        
        // Advanced validation for row data
        def rowValidators = [
            [name: 'rel_inst_id', validators: [[type: 'required', message: 'Relation instance ID is required']]],
            [name: 'src_obj_id', validators: [[type: 'required', message: 'Source object ID is required']]],
            [name: 'additional_column3', validators: [[type: 'required', message: 'Object type is required']]]
        ]
        
        rowValidators.each { fieldConfig ->
            deepFieldValidator(fieldConfig, row."${fieldConfig.name}", regionValidation)
        }
        
        rowContext.status = regionValidation.isValid ? 'VALID' : 'INVALID'
        rowContext.validationResult = regionValidation
        processedRows.add(rowContext)
    }
    
    return [rows: processedRows, validation: regionValidation]
}

// Main processing block with deeply nested logic
try {
    Emery.log.info("Starting main processing for metric_id: {}", F.metric_id)
    
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
    
    /* Non-multirow field updates with complex date handling */
    current_date = Emery.dateUtil.convertStringToDate(Emery.dateUtil.convertDateToString(Emery.util.currentDate(), "MM/dd/yyyy"), "MM/dd/yyyy")
    
    // Complex valid_from date processing
    if (F.valid_from == null) {
        valid_from_date = current_date
        Emery.log.debug("valid_from was null, set to current date: {}", Emery.dateUtil.convertDateToString(valid_from_date, "MM/dd/yyyy"))
    } else {
        valid_from_string = Emery.dateUtil.convertDateToString(F.valid_from, "MM/dd/yyyy")
        valid_from_date = Emery.dateUtil.convertStringToDate(valid_from_string, "MM/dd/yyyy")
        
        // Additional date validation
        if (Emery.dateUtil.compareDates(valid_from_date, Emery.dateUtil.addDays(current_date, -365)) < 0) {
            Emery.log.warn("valid_from date {} is more than 1 year in the past", valid_from_string)
        }
    }
    
    // Complex valid_until date processing with business rule validation
    if (F.valid_until == null) {
        valid_until_date = current_date
        Emery.log.debug("valid_until was null, set to current date")
    } else {
        valid_until_string = Emery.dateUtil.convertDateToString(F.valid_until, "MM/dd/yyyy")
        valid_until_date = Emery.dateUtil.convertStringToDate(valid_until_string, "MM/dd/yyyy")
        
        // Business rule: valid_until must be after valid_from
        if (Emery.dateUtil.compareDates(valid_until_date, valid_from_date) < 0) {
            Emery.log.error("Business rule violation: valid_until {} is before valid_from {}", valid_until_string, valid_from_string)
            validationErrors.put("valid_until", ["Valid until date must be after valid from date"])
        }
    }
    
    valid_until_date_temp = Emery.dateUtil.addDays(valid_until_date, 1)
    
    // Complex object status determination with multiple nested conditions
    def determineObjectStatus = {
        String status = 'ACT'
        String statusReason = ''
        
        if (F.valid_from != null) {
            int fromComparison = Emery.dateUtil.compareDates(current_date, valid_from_date)
            if (fromComparison == -1) {
                status = 'INACT'
                statusReason = 'Current date is before valid_from date'
            }
        }
        
        if (status == 'ACT' && F.valid_until != null) {
            int untilComparison = Emery.dateUtil.compareDates(valid_until_date_temp, current_date)
            if (untilComparison == 0 || untilComparison == -1) {
                status = 'EXP'
                statusReason = 'Object has expired based on valid_until date'
            }
        }
        
        // Additional status checks based on workflow state
        if (status == 'ACT') {
            if (F.dd_current_status == 'SUSPENDED') {
                status = 'SUSP'
                statusReason = 'Object is suspended'
            } else if (F.dd_current_status == 'PENDING_REVIEW') {
                status = 'PEND'
                statusReason = 'Object is pending review'
            }
        }
        
        return [status: status, reason: statusReason]
    }
    
    def statusResult = determineObjectStatus()
    F.obj_status = statusResult.status
    F.obj_status_temp = F.obj_status
    processingContext.statusDetermination = statusResult
    
    // SDOS validation with complex organization handling
    if (Emery.ctx.isSDOS()) {
        F.owner_organizations = F.org_groups
        
        // Additional MDOS operations for SDOS context
        if (F.org_groups != null && F.org_groups.toString().trim() != '') {
            def orgIds = F.org_groups.toString().split(',').collect { it.trim() }
            
            orgIds.each { orgId ->
                try {
                    def mdosResponse = buildComplexMdosQuery("SI_MDOS_ORG_BE_V", [
                        columns: ['ORG_ID', 'ORG_NAME', 'BE_ID', 'STATUS'],
                        conditions: [
                            [field: 'ORG_ID', operator: 'eq', value: orgId],
                            [field: 'STATUS', operator: 'eq', value: 'ACTIVE']
                        ],
                        limit: 1
                    ])
                    
                    if (mdosResponse.dataObjects && mdosResponse.dataObjects.size() > 0) {
                        def orgData = mdosResponse.dataObjects[0]
                        auditTrailEntries.add([
                            action: 'ORG_ASSIGNMENT',
                            orgId: orgId,
                            orgName: orgData.org_name,
                            timestamp: Emery.util.currentDate()
                        ])
                    }
                } catch (Exception e) {
                    Emery.log.warn("Failed to fetch MDOS data for org {}: {}", orgId, e.message)
                }
            }
        }
    }
    
    // Reset wfi_display
    if (F.wfi_display != null) {
        F.wfi_display = null
    }
    
    // Complex wfi_stored generation with user tracking
    name = F.dd_current_user_name
    current_date = Emery.util.currentDate()
    cur_date_string = Emery.dateUtil.convertDateToString(current_date, "dd/MM/yyyy HH:mm:ss")
    F.wfi_stored = name.concat(DELIMITER).concat(cur_date_string)
    
    // Update workflow stage
    F.dd_current_stage = 'PUBLISH'
    
    // Complex comment insertion with audit trail
    if (F.action_comments != null && F.action_comments.toString().trim() != '') {
        try {
            String commentResult = Emery.util.insertComments(
                F.process_instance_id as int,
                F.instance_id as int,
                F.metric_id as int,
                F.created_by as int,
                "ACTION_COMMENTS",
                F.action_comments as String
            )
            
            auditTrailEntries.add([
                action: 'COMMENT_ADDED',
                commentType: 'ACTION_COMMENTS',
                result: commentResult,
                timestamp: Emery.util.currentDate()
            ])
            
            F.action_comments = ''
        } catch (Exception e) {
            Emery.log.error("Failed to insert comments: {}", e.message)
        }
    }
    
    /* Apps_ID Creation with complex validation */
    if (F.object_id == '' || F.object_id == 'NONE' || F.object_id == null) {
        id = Emery.util.nextAppsId("MS_GRC_ORG_PROFILE", "GRC", "MS_GRC_ORGPROF_IDGEN")
        F.object_id = id
        processingContext.newObjectIdGenerated = true
        Emery.log.info("Generated new object_id: {}", F.object_id)
    }
    
    // Process multirow regions with complex nested operations
    def multirowResult = processMultirowRegion(F.orb, [objectId: F.object_id, relConfigCache: [:]])
    processingContext.multirowProcessing = multirowResult
    
    // Generate relation source ID
    if (F.rel_source_id == '' || F.rel_source_id == 'NONE' || F.rel_source_id == null) {
        id = Emery.util.nextAppsId("MS_GRC_ORG_PROFILE", "GRC", "MS_ORB_REL_SOURCE_ID")
        F.rel_source_id = id
    }
    
    if (F.rel_source_object_id == '' || F.rel_source_object_id == 'NONE' || F.rel_source_object_id == null) {
        F.rel_source_object_id = F.object_id
    }
    
    // Complex object name change detection with history tracking
    SelectQuery objectNameQuery = new SelectQuery.Builder("MS_GRC_ORG_PROFILE")
        .columns(["OBJECT_NAME", "OBJECT_ID", "DD_OBJECT_TYPE", "OBJ_STATUS", "CREATED_ON", "MODIFIED_ON"])
        .addCondition(Condition.eq("DD_OBJECT_TYPE", F.dd_object_type))
        .addCondition(Condition.eq("OBJECT_ID", F.object_id))
        .loadRegions(false)
        .build()
    
    SelectQueryResponse objectNameResult = Emery.dataobject.fetch(objectNameQuery)
    List<DataObject> objectNameObjects = objectNameResult.dataObjects
    
    if (objectNameObjects && objectNameObjects.size() > 0) {
        lv_old_object_name = objectNameObjects[0].object_name
        
        if (F.object_name == lv_old_object_name) {
            lv_flag = 'NAME_CHANGE=N'
        } else {
            lv_flag = 'NAME_CHANGE=Y'
            
            // Track name change in audit
            auditTrailEntries.add([
                action: 'NAME_CHANGE',
                oldName: lv_old_object_name,
                newName: F.object_name,
                timestamp: Emery.util.currentDate()
            ])
        }
    } else {
        lv_flag = 'NAME_CHANGE=N'
    }
    
    F.additional_column_h1 = lv_flag
    F.created_by = 100000
    
    // User tracking fields
    if (F.created_by_hidden != null && F.created_by_hidden.toString().trim() != '') {
        F.obj_created_by = F.created_by_hidden
    }
    
    if (F.modif_req_by_hidden != null && F.modif_req_by_hidden.toString().trim() != '') {
        F.obj_modified_by = F.modif_req_by_hidden
    }
    
    F.dd_current_status = 'NEW'
    
    // App config handling
    if (F.own_app_config_temp == 'Yes' || F.own_app_config_temp == 'No') {
        F.own_app_config_temp = F.own_app_config
    }
    
    // Final processing context update
    processingContext.endTime = System.currentTimeMillis()
    processingContext.processingDuration = processingContext.endTime - processingContext.startTime
    processingContext.auditTrail = auditTrailEntries
    processingContext.validationErrors = validationErrors
    
    Emery.log.info("Completed processing in {}ms with {} audit entries", processingContext.processingDuration, auditTrailEntries.size())
    
} catch (Exception e) {
    Emery.log.error("Critical error in {}: {}", script_name, e.message)
    Emery.log.error("Stack trace: {}", e.stackTrace.collect { it.toString() }.join('\n'))
    isRollbackRequired = true
    throw e
} finally {
    if (isRollbackRequired) {
        Emery.log.warn("Rollback flag is set, cleanup may be required")
    }
    Emery.log.info("Script {} execution completed", script_name)
}
