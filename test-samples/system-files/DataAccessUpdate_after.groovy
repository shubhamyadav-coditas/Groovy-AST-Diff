useDataObject("MS_ATD_RESOURCE")

// ADDED: New configuration check
def configCheck = Emery.ctx.configurationParameter("EMERY CONFIGURATIONS", "DATA_UPDATE_ENABLED")
Emery.log.info "Data update configuration: ${configCheck}"

Emery.log.error "Start Data Access APIs"

//Update Primary key should throw an exception - MODIFIED: Uncommented and enhanced
try
{
SelectQuery sQuery = new SelectQuery.Builder("MS_ATD_RESOURCE")
					.addCondition(Condition.eq("resourcename", 'Michael_2'))
                    .build();
					
SelectQueryResponse response = Emery.dataobject.fetch(sQuery)
for(DataObject obj : response.dataObjects){
    Emery.log.error "UPDATE DO TEST: will Update ${obj.resourcename} fields"
	obj.resourcename = "Martin garrix ENHANCED"  // MODIFIED: Changed name
	obj.update()
	Emery.log.error "UPDATE DO TEST: Updated ${obj.resourcename} fields"
	}
}
catch(Exception e){
	Emery.log.error("TESTLOG: Exception while updating primary key : " + e.getMessage() +"  "+ e.getClass())
	Emery.log.error "UPDATE DO TEST: Update failed for primary key"
	e.getMessage()
}
	
//Select query after updating resourceband to Michael Beaven
def row1851 = F.gr5.newRow()
row1851.instance_rec_num = 1851
row1851.api = "Step 7 - Run Select query after updating resourceband to Michael Beaven"
try
{
SelectQuery simpleUpdate = new SelectQuery.Builder("MS_ATD_RESOURCE")
					.addCondition(Condition.eq("resourcename", "Michael_5"))
                    .build();				
SelectQueryResponse response = Emery.dataobject.fetch(simpleUpdate)
for(DataObject obj : response.dataObjects){
    Emery.log.error "UPDATE DO TEST: will Update ${obj.resourceband} fields"
	obj.resourceband = "Michael Beavens BMW 3 Series"  // MODIFIED: Enhanced description
	obj.update()
	SelectQuery afterUpdate = new SelectQuery.Builder("MS_ATD_RESOURCE")
					.addCondition(Condition.eq("resourcename" ,"Michael_5"))
                    .build();
	List<DataObject> dataObjects = response.dataObjects
	for(DataObject dataObject : dataObjects){
    Emery.log.info "Select :: resourceband :: {}", dataObject.resourceband
	row1851.results = dataObject.resourceband
}				
	Emery.log.error "UPDATE DO TEST: Updated ${obj.resourceband} fields"
}
}catch(Exception e) {
e.getMessage()
}
F.gr5.rows << row1851

// MOVED: Special character update moved before region deletion
//Select query after updating resourceband to Michael Special Character
def row1852 = F.gr5.newRow()
row1852.instance_rec_num = 1852
row1852.api = "Step 8 - Run Select query after updating resourceband to Michael Special Character"
try
{
SelectQuery specialCharUpdate = new SelectQuery.Builder("MS_ATD_RESOURCE")
					.addCondition(Condition.eq("resourcename", "Michael_6"))
                    .build();					
SelectQueryResponse responseSpl = Emery.dataobject.fetch(specialCharUpdate)
for(DataObject obj : responseSpl.dataObjects){
    Emery.log.error "UPDATE DO TEST: will Update ${obj.resourceband} fields"
	obj.resourceband = "Michael ()}{]!@#%^&*_+<?/. ENHANCED"  // MODIFIED: Added ENHANCED
	obj.update()
	SelectQuery afterUpdate = new SelectQuery.Builder("MS_ATD_RESOURCE")
					.addCondition(Condition.eq("resourcename", "Michael_6"))
                    .build();
	List<DataObject> dataObjects = responseSpl.dataObjects
	for(DataObject dataObject : dataObjects){
    Emery.log.info "Select :: resourceband :: {}", dataObject.resourceband
	row1852.results = dataObject.resourceband
}
	Emery.log.error "UPDATE DO TEST: Updated ${obj.resourceband} fields"
}
}catch(Exception e) {
e.getMessage()
}
F.gr5.rows << row1852

//Records of the region after deleting one row - MOVED_MODIFIED
def row1853 = F.gr5.newRow()
row1853.instance_rec_num = 1853
row1853.api = "Step 9 - Records of the region after deleting multiple rows"  // MODIFIED: Changed to multiple
try{
SelectQuery sQuery = new SelectQuery.Builder("MS_ATD_RESOURCE")
                    .addCondition(Condition.neq("resourcename", ""))  // ADDED: New condition
                    .build();
SelectQueryResponse response = Emery.dataobject.fetch(sQuery)
List<DataObject> dataObjects = response.dataObjects
def temp = ""
for(DataObject obj : dataObjects){
	Emery.log.error "UPDATE DO TEST: will Update ${obj.resourceband} fields"
	def resourceDept = obj.rd1
	// MODIFIED: Remove multiple rows instead of just one
	resourceDept.removeIf({row -> row.rdid1.startsWith('12111_')})
    obj.update()
	obj.rd1.allRows().each {row -> 	temp += row.rdid1 + ";" }  // MODIFIED: Added separator
}
row1853.results =  temp
}
catch(Exception e){
    row1853.results =  e.getMessage()
}
F.gr5.rows << row1853

// ADDED: New validation step
def row1854 = F.gr5.newRow()
row1854.instance_rec_num = 1854
row1854.api = "Step 10 - Final validation of updates"
try{
    CountQuery finalCount = new SelectQuery.Builder("MS_ATD_RESOURCE")
                    .addCondition(Condition.like("resourceband", "%ENHANCED%"))
                    .build();
    SelectQueryResponse finalResponse = Emery.dataobject.fetch(finalCount)
    row1854.results = "Enhanced records count: " + finalResponse.dataObjects.size()
}
catch(Exception e){
    row1854.results = e.getMessage()
}
F.gr5.rows << row1854

// ADDED: Cleanup logging
Emery.log.error "Data Access Update APIs completed with enhancements"