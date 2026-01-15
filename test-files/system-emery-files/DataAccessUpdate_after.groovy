useDataObject("MS_ATD_RESOURCE")

//Update Primary key should throw an exception
/*try
{
SelectQuery sQuery = new SelectQuery.Builder("MS_ATD_RESOURCE")
					.addCondition(Condition.eq("resourcename", 'Michael_2'))
                    .build();
					
SelectQueryResponse response = Emery.dataobject.fetch(sQuery)
for(DataObject obj : response.dataObjects){
    Emery.log.error "UPDATE DO TEST: will Update ${obj.resourcename} fields"
	obj.resourcename = "Martin garrix POR"
	obj.update()
	Emery.log.error "UPDATE DO TEST: Updated ${obj.resourcename} fields"
	}
}
catch(Exception e){
	Emery.log.error("TESTLOG: Exception while invalid_locale_id : " + e.getMessage() +"  "+ e.getClass())
	Emery.log.error "UPDATE DO TEST: Updated failed for primary key"
	e.getMessage()
}*/
	
//Select query after updating resourceband to Michael Beaven
def row1851 = F.gr5.newRow()
row1851.instance_rec_num = 1852

try
{
SelectQuery simpleUpdate = new SelectQuery.Builder("MS_ATD_RESOURCE")
					.addCondition(Condition.eq("resourcename", "Michael_5"))
                    .build();				
SelectQueryResponse response = Emery.dataobject.fetch(simpleUpdate)
for(DataObject obj : response.dataObjects){
    Emery.log.error "UPDATE DO TEST: will Update ${obj.resourceband} fields"
	obj.resourceband = "Michael Beavens BMW 3"
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
row1851.api = "Step 7 - Run Select query after updating resourceband to Michael Beaven"

//Select query after updating resourceband to Michael Special Character
def row1852 = F.gr5.newRow()
row1852.api = "Step 8 - Run Select query after updating resourceband to Michael Special Characters"
row1852.instance_rec_num = 1852
try
{
SelectQuery specialCharUpdate = new SelectQuery.Builder("MS_ATD_RESOURCE")
					.addCondition(Condition.eq("resourcename", "Michael_6"))
                    .build();					
SelectQueryResponse responseSpl = Emery.dataobject.fetch(specialCharUpdate)
for(DataObject obj : responseSpl.dataObjects){
    Emery.log.error "UPDATE DO TEST: will Update ${obj.resourceband} fields"
	obj.resourceband = "Michael ()}{]!@#%^&*_+<?/."
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
F.gr6.rows << row1851
//Records of the region after deleting one row
def row1853 = F.gr5.newRow()
row1853.instance_rec_num = 1853
row1853.api = "Step 9 - Records of the region after deleting one row"
try{
	SelectQuery sQuery = new SelectQuery.Builder("MS_ATD_RESOURCE")
						.build();
	SelectQueryResponse response = Emery.dataobject.fetch(sQuery)
	List<DataObject> dataObjects = response.dataObjects


	for(DataObject obj : dataObjects){
		Emery.log.error "UPDATE DO TEST: will Update ${obj.resourceband} fields"
		def resourceDept = obj.rd1
		resourceDept.removeIf({row -> row.rdid1 == '12111_5'})
		obj.update()
		obj.rd1.allRows().each {row -> 	temp += row.rdid1 }
	}
	row1853.results =  temp
}
catch(Exception e){
    row1853.results =  e.getMessage()
}
F.gr5.rows << row1853

