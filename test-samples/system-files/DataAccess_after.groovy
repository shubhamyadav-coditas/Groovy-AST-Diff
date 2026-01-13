useDataObject("MS_ATD_RESOURCE")

// ADDED: New logging statement at the beginning
Emery.log.info "Starting Data Access APIs with enhanced logging"

Emery.log.error "Start Data Access APIs"

//Step 1 - Clear previous records in DO and then count
def row1801 = F.gr5.newRow()
row1801.instance_rec_num = 1801
row1801.api = "Step 1 - Clear previous records in DO and then count"
try{

    DeleteQuery deleteQuery = new DeleteQuery.Builder("MS_ATD_RESOURCE")   
							.build(); 
				
    int count1 = Emery.dataobject.delete(deleteQuery)

    CountQuery countQuery = new CountQuery.Builder("MS_ATD_RESOURCE")
            .build(); 

    int count2 = Emery.dataobject.count(countQuery)

    row1801.results =  count2
    //count2 == 0 to be validated using xlsx

}
catch(Exception e){
    row1801.results = e.getMessage()
}
F.gr5.rows << row1801

//Step 2 - Add 12 rows in DO - MODIFIED: Changed number of records
def row1802 = F.gr5.newRow()
row1802.instance_rec_num = 1802
row1802.api = "Step 2 - Add 15 rows in DO"
try{

    int numOfRecordsToAdd = 15
    for(int i = 0; i < numOfRecordsToAdd; i++){

        //Appending this postfix (string) to generate multiple rows
        String postfix =  "_" + i.toString()

        /* Creating new instance */
        DataObject dataobject = Emery.dataobject.newInstance("MS_ATD_RESOURCE")


        /* Populating data*/
        dataobject.resourcename = "Michael" + postfix
        dataobject.resourceband = "BMW" + postfix 

        /* Standard Columns */
        dataobject.dd_object_type = "MS_ATD_RESOURCE"
        dataobject.dd_status_flag = "Y"
        dataobject.dd_process_instance_id = 300000 + i
        dataobject.dd_process_code = "resourcewf"
        dataobject.dd_instance_id = 400000 + i
        dataobject.dd_created_on = new Date()
        dataobject.dd_created_by = "SYSTEMI"

        //region - Address (acronym : lng)
        def addRegion = dataobject.lng
        def addRow1 = addRegion.newRow()
        addRow1.languages = "ENGLISH" 
        addRow1.dd_status_flag = "N"
		
		//region - ResourceDept (acronym : rd1)
        def addRegion2 = dataobject.rd1
        def addRow2 = addRegion2.newRow()
        addRow2.rdid1 = "12111" + postfix
		addRow2.rdcat1 = "cars" 
        addRow2.dd_status_flag = "N"

        /*sub region - Project (acronym : prj)*/
        def addRegion3 = dataobject.prj
        def addRow3 = addRegion3.newRow()
        addRow3.projectid = "PRJ" + postfix
		addRow3.projectname= "R & D" 
        addRow3.dd_status_flag = "N"
		
		/*sub region - Skills (acronym : skl)*/
        def subRegion = addRow3.skl
        def strRow3 = subRegion.newRow()
        strRow3.skills = "Engineering " 
        strRow3.dd_status_flag = "N"
		
		subRegion.addRow(strRow3)
		addRegion.addRow(addRow1)
		addRegion2.addRow(addRow2)
        addRegion3.addRow(addRow3)
        dataobject.save()
        // comment added
    }
    row1802.results =  "15 records added successfully."  // MODIFIED: Changed from 12 to 15
}
catch(Exception e){
    row1802.results = e.getMessage()
}
F.gr5.rows << row1802

// MOVED: Step 4 moved before Step 3
//Step 4 - Delete 2 records from DO
def row1804 = F.gr5.newRow()
row1804.instance_rec_num = 1804
row1804.api = "Step 4 - Delete 2 records from DO"
try{
    DeleteQuery deleteQuery = new DeleteQuery.Builder("MS_ATD_RESOURCE")
                .addCondition(Condition.gte("DD_PROCESS_INSTANCE_ID","200050"))   
                .addCondition(Condition.lte("DD_PROCESS_INSTANCE_ID","300050"))     
                .addCondition(Condition.neq("DD_PROCESS_INSTANCE_ID","200050"))      
                .addCondition(Condition.inList("DD_PROCESS_INSTANCE_ID", ["300010", "300011"]))
                .build();               
    int count = Emery.dataobject.delete(deleteQuery)
    row1804.results = count
    //results == 2 to be validated using xlsx
}
catch(Exception e){
    row1804.results = e.getMessage()
}
F.gr5.rows << row1804

//Step 3 - Count number of records added
def row1803 = F.gr5.newRow()
row1803.instance_rec_num = 1803
row1803.api = "Step 3 - Count number of records"
try{
    CountQuery countQuery = new CountQuery.Builder("MS_ATD_RESOURCE")
                .build(); 
                
    int count = Emery.dataobject.count(countQuery)
    row1803.results =  count
    //results == 15 to be validated using xlsx  // MODIFIED: Changed from 12 to 15
}
catch(Exception e){
    row1803.results =  e.getMessage()
}
F.gr5.rows << row1803

//Step 5 - Run Select query 1
def row1805 = F.gr5.newRow()
row1805.instance_rec_num = 1805
row1805.api = "Step 5 - Run Select query 1"
try{
    SelectQuery selectQuery = new SelectQuery.Builder("MS_ATD_RESOURCE")
                    .addCondition(Condition.regex("resourcename","%ael_1%"))
                    .build(); 
                    
    SelectQueryResponse response = Emery.dataobject.fetch(selectQuery)
    //Emery.log.error "##### Select :: Condition eq :: {}", response
    List<DataObject> dataObjects = response.dataObjects
    String temp = ""
    String delimiter = "---"
    for(DataObject dataObject : dataObjects){
        temp += dataObject.resourcename + delimiter
        temp += dataObject.resourceband + delimiter
        temp += dataObject.dd_process_instance_id.toString() + delimiter
        
        
        dataObject.prj.allRows().each { row -> 
            temp += row.projectid + delimiter
            
            row.skl.allRows().each {AbstractRegionRow strRow ->
                temp += strRow.skills + delimiter
            }
        }
    }
    row1805.results = temp
    //results == 2 to be validated using xlsx
}
catch(Exception e){
    row1805.results = e.getMessage()
}
F.gr5.rows << row1805

//Step 5 - Run Select query 2 - MOVED_MODIFIED: Changed limit and added new condition
def row1806 = F.gr5.newRow()
row1806.instance_rec_num = 1806
row1806.api = "Step 6 - Run Select query 2 with enhanced filtering"  // MODIFIED: Enhanced description
try{
    SelectQuery selectQuery = new SelectQuery.Builder("MS_ATD_RESOURCE")
                    .addSortCondition(SortCondition.desc("DD_PROCESS_INSTANCE_ID"))
                    .addCondition(Condition.neq("resourcename", ""))  // ADDED: New condition
                    .limit(3)  // MODIFIED: Changed from 2 to 3
                    .offset(1)
                    .build(); 
                    
    SelectQueryResponse response = Emery.dataobject.fetch(selectQuery)
	
    //Emery.log.error "##### Select :: Limit/Offset with Sort :: {}", response
    List<DataObject> dataObjects = response.dataObjects
    String temp = ""
    String delimiter = "---"
    for(DataObject dataObject : dataObjects){
        temp += dataObject.resourcename + delimiter       
    }
    row1806.results = temp
    //results == 3 to be validated using xlsx  // MODIFIED: Changed from 2 to 3
}
catch(Exception e){
    row1806.results = e.getMessage()
}
F.gr5.rows << row1806

// ADDED: New step for additional validation
def row1807 = F.gr5.newRow()
row1807.instance_rec_num = 1807
row1807.api = "Step 7 - Additional validation check"
try{
    CountQuery validationQuery = new CountQuery.Builder("MS_ATD_RESOURCE")
                .addCondition(Condition.eq("dd_status_flag", "Y"))
                .build(); 
                
    int validCount = Emery.dataobject.count(validationQuery)
    row1807.results = "Valid records: " + validCount
}
catch(Exception e){
    row1807.results = e.getMessage()
}
F.gr5.rows << row1807

// DELETED: The original logging statement "End Data Access APIs" is removed

Emery.log.error "Data Access APIs completed successfully"  // MODIFIED: Changed end message