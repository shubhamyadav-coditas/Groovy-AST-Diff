/**
 * Complex GRC Function Processing Script
 * Operations: INI_TO_PUB transition with advanced function management
 * i) Field updates: obj_status, obj_status_temp, owner_organizations, wfi_display, wfi_stored, dd_current_stage
 * ii) Function relationship configuration and dependency management
 * iii) Advanced ID generation with sequence management
 * iv) Complex LOV operations and configuration parameter handling
 * v) Integration with CIF/CIS services
 **/

use("MS_GRC_FUNCTION")
useDataObject("MS_GRC_REL_DEFN")
useDataObject("MS_GRC_FUNCTION")
useDataObject("MS_GRC_FUNCTION_DEPENDENCY")
useDataObject("MS_GRC_FUNCTION_HIERARCHY")
useDataObject("MS_GRC_CONFIG_PARAMS")
useDataObject("MS_GRC_AUDIT_TRAIL")

// Extended variable declarations with complex types
String id, name, cur_date_string, valid_from_string, valid_until_string, lv_flag, lv_old_object_name
Date current_date, valid_from_date, valid_until_date, valid_until_date_temp
String script_name = "FunctionInitiatorToPublish_Advanced.groovy"
Map<String, Object> processingContext = [:]
Map<String, Object> configParams = [:]
Map<String, List<String>> validationErrors = [:]
Map<String, Object> lovCache = [:]
List<Map<String, Object>> auditTrailEntries = []
List<Map<String, Object>> dependencyGraph = []
List<DataObject> pendingUpdates = []
def functionHierarchy = []
boolean isRollbackRequired = false
boolean enableDependencyValidation = true
int recursionDepth = 0
final int MAX_RECURSION_DEPTH = 10
final int MAX_BATCH_SIZE = 500
final String DELIMITER = '\$_\$'
final String FUNCTION_PREFIX = 'FUNC_'

Emery.log.info("Started Pre-Hook {} at {}", script_name, Emery.util.currentDateAsString())

// Complex function dependency resolver with cycle detection
class FunctionDependencyResolver {
    Map<String, Set<String>> adjacencyList = [:]
    Map<String, Integer> inDegree = [:]
    Set<String> visited = new HashSet<>()
    Set<String> recursionStack = new HashSet<>()
    List<String> topologicalOrder = []
    boolean hasCycle = false
    String cycleDetails = ''
    
    void addDependency(String from, String to) {
        if (!adjacencyList.containsKey(from)) adjacencyList[from] = new HashSet<>()
        if (!adjacencyList.containsKey(to)) adjacencyList[to] = new HashSet<>()
        
        adjacencyList[from].add(to)
        
        if (!inDegree.containsKey(from)) inDegree[from] = 0
        if (!inDegree.containsKey(to)) inDegree[to] = 0
        inDegree[to] = inDegree[to] + 1
    }
    
    boolean detectCycle() {
        visited.clear()
        recursionStack.clear()
        
        for (String node : adjacencyList.keySet()) {
            if (!visited.contains(node)) {
                if (detectCycleUtil(node, [])) {
                    return true
                }
            }
        }
        return false
    }
    
    private boolean detectCycleUtil(String node, List<String> path) {
        visited.add(node)
        recursionStack.add(node)
        path.add(node)
        
        for (String neighbor : adjacencyList[node] ?: []) {
            if (!visited.contains(neighbor)) {
                if (detectCycleUtil(neighbor, path)) return true
            } else if (recursionStack.contains(neighbor)) {
                int cycleStart = path.indexOf(neighbor)
                cycleDetails = path.subList(cycleStart, path.size()).join(' -> ') + ' -> ' + neighbor
                hasCycle = true
                return true
            }
        }
        
        recursionStack.remove(node)
        path.remove(path.size() - 1)
        return false
    }
    
    List<String> getTopologicalSort() {
        if (hasCycle) return []
        
        Map<String, Integer> tempInDegree = new HashMap<>(inDegree)
        Queue<String> queue = new LinkedList<>()
        List<String> result = []
        
        tempInDegree.each { node, degree ->
            if (degree == 0) queue.offer(node)
        }
        
        while (!queue.isEmpty()) {
            String current = queue.poll()
            result.add(current)
            
            adjacencyList[current]?.each { neighbor ->
                tempInDegree[neighbor] = tempInDegree[neighbor] - 1
                if (tempInDegree[neighbor] == 0) {
                    queue.offer(neighbor)
                }
            }
        }
        
        topologicalOrder = result
        return result
    }
    
    Map<String, Object> getAnalysis() {
        return [
            nodeCount: adjacencyList.size(),
            edgeCount: adjacencyList.values().sum { it?.size() ?: 0 } ?: 0,
            hasCycle: hasCycle,
            cycleDetails: cycleDetails,
            topologicalOrder: topologicalOrder,
            rootNodes: inDegree.findAll { it.value == 0 }.keySet().toList(),
            leafNodes: adjacencyList.findAll { !it.value || it.value.isEmpty() }.keySet().toList()
        ]
    }
}

// Complex LOV operations handler
def lovOperationsHandler = {
    def cache = [:]
    
    // Get LOV values with caching
    def getLovValues = { String lovName, Map<String, Object> filters ->
        def cacheKey = "${lovName}_${filters?.hashCode()}"
        
        if (cache.containsKey(cacheKey)) {
            return cache[cacheKey]
        }
        
        try {
            def lovValues = Emery.lov.getListOfValues(lovName)
            
            if (filters && !filters.isEmpty()) {
                lovValues = lovValues.findAll { lov ->
                    filters.every { filterKey, filterValue ->
                        lov[filterKey] == filterValue
                    }
                }
            }
            
            cache[cacheKey] = lovValues
            return lovValues
        } catch (Exception e) {
            Emery.log.error("Failed to get LOV values for {}: {}", lovName, e.message)
            return []
        }
    }
    
    // Get LOV display value
    def getLovDisplayValue = { String lovName, String storedValue ->
        try {
            def lovValues = getLovValues(lovName, null)
            def match = lovValues.find { it.stored_value == storedValue }
            return match?.display_value ?: storedValue
        } catch (Exception e) {
            return storedValue
        }
    }
    
    // Validate against LOV
    def validateAgainstLov = { String lovName, String value ->
        if (value == null || value.trim() == '') return true
        
        def lovValues = getLovValues(lovName, null)
        def storedValues = lovValues.collect { it.stored_value }
        return storedValues.contains(value)
    }
    
    return [
        getLovValues: getLovValues,
        getLovDisplayValue: getLovDisplayValue,
        validateAgainstLov: validateAgainstLov,
        clearCache: { cache.clear() }
    ]
}

// Initialize LOV handler
def lovOps = lovOperationsHandler()

// Complex configuration parameter handler
def configParamHandler = {
    def paramCache = [:]
    
    // Get config parameter
    def getParam = { String paramName, String defaultValue ->
        if (paramCache.containsKey(paramName)) {
            return paramCache[paramName]
        }
        
        try {
            def paramValue = Emery.configParam.get(paramName)
            paramCache[paramName] = paramValue ?: defaultValue
            return paramCache[paramName]
        } catch (Exception e) {
            Emery.log.warn("Config param {} not found, using default: {}", paramName, defaultValue)
            return defaultValue
        }
    }
    
    // Get system parameter
    def getSysParam = { String paramName, String defaultValue ->
        try {
            def sysParamValue = Emery.sysParam.get(paramName)
            return sysParamValue ?: defaultValue
        } catch (Exception e) {
            return defaultValue
        }
    }
    
    // Get parameter as integer
    def getParamAsInt = { String paramName, int defaultValue ->
        def value = getParam(paramName, defaultValue.toString())
        try {
            return Integer.parseInt(value.toString())
        } catch (NumberFormatException e) {
            return defaultValue
        }
    }
    
    // Get parameter as boolean
    def getParamAsBool = { String paramName, boolean defaultValue ->
        def value = getParam(paramName, defaultValue.toString())
        return value?.toString()?.toLowerCase() in ['true', 'yes', '1', 'y']
    }
    
    return [
        getParam: getParam,
        getSysParam: getSysParam,
        getParamAsInt: getParamAsInt,
        getParamAsBool: getParamAsBool
    ]
}

// Initialize config handler
def configOps = configParamHandler()

// Complex function hierarchy builder with recursive traversal
def buildFunctionHierarchy
buildFunctionHierarchy = { String parentFunctionId, int currentDepth, int maxDepth, Map<String, Object> context ->
    if (currentDepth > maxDepth) {
        Emery.log.warn("Max recursion depth {} reached at function {}", maxDepth, parentFunctionId)
        return []
    }
    
    def hierarchyNodes = []
    
    try {
        SelectQuery childFunctionsQuery = new SelectQuery.Builder("MS_GRC_FUNCTION_HIERARCHY")
            .columns(["CHILD_FUNCTION_ID", "PARENT_FUNCTION_ID", "HIERARCHY_LEVEL", "SEQUENCE_ORDER", "STATUS", "EFFECTIVE_FROM", "EFFECTIVE_TO", "RELATIONSHIP_TYPE"])
            .addCondition(Condition.eq("PARENT_FUNCTION_ID", parentFunctionId))
            .addCondition(Condition.eq("STATUS", "ACTIVE"))
            .addCondition(Condition.or(Condition.isNull("EFFECTIVE_TO"), Condition.gt("EFFECTIVE_TO", Emery.util.currentDate())))
            .addSortCondition(SortCondition.asc("SEQUENCE_ORDER"))
            .loadRegions(false)
            .build()
        
        SelectQueryResponse childFunctionsResult = Emery.dataobject.fetch(childFunctionsQuery)
        List<DataObject> childFunctions = childFunctionsResult.dataObjects
        
        childFunctions.each { childFunc ->
            Map<String, Object> node = [
                functionId: childFunc.child_function_id,
                parentFunctionId: childFunc.parent_function_id,
                level: childFunc.hierarchy_level,
                sequence: childFunc.sequence_order,
                relationshipType: childFunc.relationship_type,
                depth: currentDepth,
                children: [],
                metadata: [:]
            ]
            
            // Fetch function details
            SelectQuery funcDetailsQuery = new SelectQuery.Builder("MS_GRC_FUNCTION")
                .columns(["OBJECT_ID", "OBJECT_NAME", "OBJ_STATUS", "DD_OBJECT_TYPE", "FUNCTION_TYPE", "RISK_LEVEL"])
                .addCondition(Condition.eq("OBJECT_ID", childFunc.child_function_id))
                .loadRegions(false)
                .build()
            
            SelectQueryResponse funcDetailsResult = Emery.dataobject.fetch(funcDetailsQuery)
            if (funcDetailsResult.dataObjects && funcDetailsResult.dataObjects.size() > 0) {
                def funcDetails = funcDetailsResult.dataObjects[0]
                node.metadata = [
                    objectName: funcDetails.object_name,
                    objStatus: funcDetails.obj_status,
                    objectType: funcDetails.dd_object_type,
                    functionType: funcDetails.function_type,
                    riskLevel: funcDetails.risk_level
                ]
            }
            
            // Recursively build children
            if (currentDepth < maxDepth) {
                node.children = buildFunctionHierarchy(childFunc.child_function_id.toString(), currentDepth + 1, maxDepth, context)
            }
            
            hierarchyNodes.add(node)
        }
    } catch (Exception e) {
        Emery.log.error("Error building hierarchy at depth {} for function {}: {}", currentDepth, parentFunctionId, e.message)
    }
    
    return hierarchyNodes
}

// Complex CIF/CIS integration handler
def integrationHandler = {
    // CIF data export
    def exportToCif = { Map<String, Object> exportConfig ->
        try {
            def cifPayload = [
                exportType: exportConfig.type ?: 'FUNCTION',
                objectId: exportConfig.objectId,
                objectType: exportConfig.objectType,
                includeRelations: exportConfig.includeRelations ?: false,
                includeHierarchy: exportConfig.includeHierarchy ?: false,
                format: exportConfig.format ?: 'JSON',
                timestamp: Emery.util.currentDate()
            ]
            
            // Build data for export
            if (exportConfig.includeRelations) {
                SelectQuery relQuery = new SelectQuery.Builder("MS_GRC_REL_DEFN")
                    .columns(["REL_ID", "REL_NAME", "SRC_OBJ_TYPE", "DEST_OBJ_TYPE", "CARDINALITY"])
                    .addCondition(Condition.eq("SRC_OBJ_TYPE", exportConfig.objectType))
                    .loadRegions(false)
                    .build()
                
                def relResult = Emery.dataobject.fetch(relQuery)
                cifPayload.relations = relResult.dataObjects?.collect { [
                    relId: it.rel_id,
                    relName: it.rel_name,
                    destType: it.dest_obj_type,
                    cardinality: it.cardinality
                ]} ?: []
            }
            
            return [success: true, payload: cifPayload]
        } catch (Exception e) {
            Emery.log.error("CIF export failed: {}", e.message)
            return [success: false, error: e.message]
        }
    }
    
    // CIS data import
    def importFromCis = { Map<String, Object> importConfig ->
        try {
            // Validate import configuration
            if (!importConfig.sourceId || !importConfig.targetType) {
                return [success: false, error: 'Invalid import configuration']
            }
            
            // Process import
            return [success: true, importedCount: 0, skippedCount: 0]
        } catch (Exception e) {
            return [success: false, error: e.message]
        }
    }
    
    // Hook chain execution
    def executeHookChain = { String hookChainName, Map<String, Object> params ->
        try {
            def result = Emery.hookChain.execute(hookChainName, params)
            return [success: true, result: result]
        } catch (Exception e) {
            Emery.log.error("Hook chain {} execution failed: {}", hookChainName, e.message)
            return [success: false, error: e.message]
        }
    }
    
    return [
        exportToCif: exportToCif,
        importFromCis: importFromCis,
        executeHookChain: executeHookChain
    ]
}

// Initialize integration handler
def integrationOps = integrationHandler()

// Complex batch processing for multirow data
def processFunctionMultirowBatch = { def rows, Map<String, Object> context ->
    def results = [processed: [], failed: [], skipped: []]
    def batchSize = context.batchSize ?: MAX_BATCH_SIZE
    def dependencyResolver = new FunctionDependencyResolver()
    
    // First pass: build dependency graph
    rows.each { row ->
        if (row.depends_on_function_id && row.function_id) {
            dependencyResolver.addDependency(row.depends_on_function_id, row.function_id)
        }
    }
    
    // Check for cycles
    if (enableDependencyValidation && dependencyResolver.detectCycle()) {
        Emery.log.error("Circular dependency detected: {}", dependencyResolver.cycleDetails)
        return [
            processed: [],
            failed: rows.collect { [row: it, error: 'Circular dependency detected'] },
            skipped: [],
            dependencyAnalysis: dependencyResolver.getAnalysis()
        ]
    }
    
    // Get processing order
    def processingOrder = dependencyResolver.getTopologicalSort()
    Emery.log.debug("Processing order: {}", processingOrder)
    
    // Process rows in batches
    def rowsToProcess = rows.toList()
    def processedIds = new HashSet<String>()
    
    for (int batchStart = 0; batchStart < rowsToProcess.size(); batchStart += batchSize) {
        int batchEnd = Math.min(batchStart + batchSize, rowsToProcess.size())
        def batch = rowsToProcess.subList(batchStart, batchEnd)
        
        batch.each { row ->
            try {
                // Check dependencies are processed
                if (row.depends_on_function_id && !processedIds.contains(row.depends_on_function_id)) {
                    results.skipped.add([row: row, reason: 'Dependency not yet processed'])
                    return
                }
                
                // Generate IDs if needed
                if (row.rel_inst_id == '' || row.rel_inst_id == 'NONE') {
                    row.rel_inst_id = Emery.util.nextAppsId("MS_GRC_FUNCTION", "GRC", "MS_ORB_REL_INST_ID")
                }
                
                if (row.src_obj_id == '' || row.src_obj_id == null) {
                    row.src_obj_id = context.objectId
                }
                
                // Lookup rel_config_id
                if (row.rel_config_id == '' || row.rel_config_id == null) {
                    def records = Emery.dataTable.read("MS_GRC_ORB_CONFIG_FORM_TITLE")
                    def targetRecord = records.find { r -> r.ORB_OBJ_TYPE == row.additional_column4 }
                    
                    if (targetRecord) {
                        row.additional_column3 = targetRecord.object_type
                        
                        SelectQuery relQuery = new SelectQuery.Builder("MS_GRC_REL_DEFN")
                            .columns(["REL_ID"])
                            .addCondition(Condition.eq("SRC_OBJ_TYPE", context.objectType))
                            .addCondition(Condition.eq("DEST_OBJ_TYPE", row.additional_column3))
                            .loadRegions(false)
                            .build()
                        
                        def relResult = Emery.dataobject.fetch(relQuery)
                        relResult.dataObjects?.each { dataObj ->
                            row.rel_config_id = dataObj.rel_id
                        }
                    }
                }
                
                processedIds.add(row.rel_inst_id)
                results.processed.add([row: row, timestamp: Emery.util.currentDate()])
                
            } catch (Exception e) {
                Emery.log.error("Failed to process row: {}", e.message)
                results.failed.add([row: row, error: e.message])
            }
        }
    }
    
    results.dependencyAnalysis = dependencyResolver.getAnalysis()
    return results
}

// Main processing block
try {
    Emery.log.info("Starting main processing for function metric_id: {}", F.metric_id)
    
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
    
    // Load configuration parameters
    configParams = [
        enableValidation: configOps.getParamAsBool('FUNC_ENABLE_VALIDATION', true),
        maxHierarchyDepth: configOps.getParamAsInt('FUNC_MAX_HIERARCHY_DEPTH', 5),
        enableNotifications: configOps.getParamAsBool('FUNC_ENABLE_NOTIFICATIONS', true),
        batchSize: configOps.getParamAsInt('FUNC_BATCH_SIZE', 100)
    ]
    
    /* Non-multirow field updates */
    current_date = Emery.dateUtil.convertStringToDate(Emery.dateUtil.convertDateToString(Emery.util.currentDate(), "MM/dd/yyyy"), "MM/dd/yyyy")
    
    // Date processing
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
    
    // Process multirow with batch processing
    def multirowResult = processFunctionMultirowBatch(F.orb.rows, [
        objectId: F.object_id,
        objectType: F.dd_object_type,
        batchSize: configParams.batchSize
    ])
    
    processingContext.multirowProcessing = multirowResult
    
    // Generate rel_source_id
    if (F.rel_source_id == '' || F.rel_source_id == 'NONE') {
        id = Emery.util.nextAppsId("MS_GRC_FUNCTION", "GRC", "MS_ORB_REL_SOURCE_ID")
        F.rel_source_id = id
    }
    
    if (F.rel_source_object_id == '' || F.rel_source_object_id == 'NONE') {
        F.rel_source_object_id = F.object_id
    }
    
    // Build function hierarchy
    if (configParams.maxHierarchyDepth > 0) {
        functionHierarchy = buildFunctionHierarchy(F.object_id, 0, configParams.maxHierarchyDepth, processingContext)
        processingContext.hierarchyNodeCount = functionHierarchy.size()
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
    
    // Export to CIF if enabled
    if (configParams.enableNotifications) {
        def exportResult = integrationOps.exportToCif([
            type: 'FUNCTION',
            objectId: F.object_id,
            objectType: F.dd_object_type,
            includeRelations: true,
            includeHierarchy: true
        ])
        processingContext.cifExport = exportResult
    }
    
    // Final processing context update
    processingContext.endTime = System.currentTimeMillis()
    processingContext.duration = processingContext.endTime - processingContext.startTime
    
    Emery.log.info("Processing completed in {}ms", processingContext.duration)
    
} catch (Exception e) {
    Emery.log.error("Error in {}: {}", script_name, e.message)
    isRollbackRequired = true
    throw e
} finally {
    if (isRollbackRequired) {
        Emery.log.warn("Rollback required for {}", script_name)
    }
    lovOps.clearCache()
}
