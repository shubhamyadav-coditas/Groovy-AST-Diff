Emery.log.error("tart34 script")


//getOrgEntityDetails()
def row100 = F.gr5.newRow()
row100.api = "getOrgEntityDetails "
row100.instance_rec_num = 1000
row100.results = "100"
try{
	def result = Emery.util.getOrgEntityDetails(100000)
	row100.results = "EntityId="+result.getOrgEntityId()+" EntityName="+result.getOrgEntityName()+" Description="+result.getDescription()+" OrgHierarchyId="+result.getOrgHierarchyId()+" ParentOrgEntityId="+result.getParentOrgEntityId()+" LocationId="+result.getLocationId()+" EntityTitle="+result.getOrgEntityTitle()
	}
catch(Exception e){
	row100.results = e.getMessage()
	//
	}



//getOrgEntityDetails_NegativeValue
def row101 = F.gr5.newRow()
row101.instance_rec_num = 101
row101.api = "getOrgEntityDetails_NegativeValue "
try{
	def result = Emery.util.getOrgEntityDetails(-100000)
	row101.results = result.getOrgEntityName()
}
catch(Exception e){
	row101.results = e.getMessage()
	//
	}
F.gr5.rows << row101


//getOrgEntityDetails_ZERO
def row102 = F.gr5.newRow()
row102.instance_rec_num = 102
row102.api = "getOrgEntityDetails_ZERO "
try{
	def result = Emery.util.getOrgEntityDetails(0)
	row102.results = result.getOrgEntityName()
}
catch(Exception e){
	row102.results = e.getMessage()
	//
	}
F.gr5.rows << row102 

/*
//getOrgEntityDetails_Empty value
def row104 = F.gr5.newRow()
row104.instance_rec_num = 104
row104.api = "getOrgEntityDetails_EmptyValue "
try{
	def result = Emery.util.getOrgEntityDetails()
	row104.results = result.getOrgEntityName()
}
catch(Exception e){
	row104.results = e.getMessage()
	//
	}
F.gr5.rows << row104  */

//getOrgEntityDetails_RandomNo - not present in DB
def row103 = F.gr5.newRow()
row103.instance_rec_num = 103
row103.api = "getOrgEntityDetails_RandomNo "
try{
	def result = Emery.util.getOrgEntityDetails(1234)
	row103.results = result.getOrgEntityName()
}
catch(Exception e){
	row103.results = e.getMessage()
	//
	}
F.gr5.rows << row103


//getOrgEntityDetails_StringValue
def row106 = F.gr5.newRow()
row106.instance_rec_num = 106
row106.api = "getOrgEntityDetails_StringValue"
try{
	def result = Emery.util.getOrgEntityDetails("Sometext")
	row106.results = result.getOrgEntityName()
}
catch(Exception e){
	row106.results = e.getMessage()
	//
	}
F.gr5.rows << row106



//getOrgHierarchyDetails()
def row107 = F.gr5.newRow()
row107.instance_rec_num = 107
row107.api = "getOrgHierarchyDetails "

try{
	def result = Emery.util.getOrgHierarchyDetails(100000)
	row107.results = " HierarchyId="+result.getOrgHierarchyId()+" HierarchyName="+result.getOrgHierarchyName()+" Description="+result.getDescription()+" ParentOrgLevelEntryId="+result.getParentOrgLevelEntryId()+" OrgLevel="+result.getOrgLevel()+" OrgHierarchyTitle="+result.getOrgHierarchyTitle()
	}
catch(Exception e){
	row107.results = e.getMessage()
	//
	}
F.gr5.rows << row107

//getOrgHierarchyDetails_NegativeValue
def row108 = F.gr5.newRow()
row108.instance_rec_num = 108
row108.api = "getOrgHierarchyDetails_NegativeValue"
try{
	def result = Emery.util.getOrgEntityDetails(-100000)
	row108.results = result.getOrgHierarchyId()
}
catch(Exception e){
	row108.results = e.getMessage()
	//
	}
F.gr5.rows << row108


//getOrgHierarchyDetails_ZERO
def row109 = F.gr5.newRow()
row109.instance_rec_num = 109
row109.api = "getOrgHierarchyDetails_ZERO"
try{
	def result = Emery.util.getOrgEntityDetails(0)
	row109.results = result.getOrgHierarchyId()
}
catch(Exception e){
	row109.results = e.getMessage()
	//
	}
F.gr5.rows << row109

/*
//getOrgHierarchyDetails_EmptyValue
def row110 = F.gr5.newRow()
row110.instance_rec_num = 110
row110.api = "getOrgHierarchyDetails_EmptyValue"
try{
	def result = Emery.util.getOrgEntityDetails()
	row110.results = result.getOrgHierarchyId()
}
catch(Exception e){
	row110.results = e.getMessage()
	//
	}
F.gr5.rows << row110  */

//getOrgHierarchyDetails_RandomNo - not present in DB
def row111 = F.gr5.newRow()
row111.instance_rec_num = 111
row111.api = "getOrgHierarchyDetails_RandomNo"
try{
	def result = Emery.util.getOrgEntityDetails(123)
	row111.results = result.getOrgHierarchyId()
}
catch(Exception e){
	row111.results = e.getMessage()
	//
	}
F.gr5.rows << row111


//getOrgHierarchyDetails_StringValue
def row112 = F.gr5.newRow()
row112.instance_rec_num = 112
row112.api = "getOrgHierarchyDetails_StringValue"
try{
	def result = Emery.util.getOrgEntityDetails("SomeText")
	row112.results = result.getOrgHierarchyId()
}
catch(Exception e){
	row112.results = e.getMessage()

	}
F.gr5.rows << row112




//getOrgLocationDetails()
def row113 = F.gr5.newRow()
row113.instance_rec_num = 113
row113.api = "getOrgLocationDetails "

try{
	def result = Emery.util.getOrgLocationDetails(100001)
	row113.results = "LocationId=" + result.getLocationId() + " LocationName=" + result.getLocationName() + " Description=" + result.getDescription() + " Address1=" + result.getAddress1() + " Address2=" + result.getAddress2() + " City=" + result.getCity()+ " State=" + result.getState()+ " Zip=" + result.getZip()+ " Country=" + result.getCountry()+ " LocationTitle=" + result.getLocationTitle()
	}
catch(Exception e){
	row113.results = e.getMessage()
	//
	}
F.gr5.rows << row113 


//getOrgLocationDetails_NegativeValue
def row114 = F.gr5.newRow()
row114.instance_rec_num = 114
row114.api = "getOrgLocationDetails_NegativeValue"
try{
	def result = Emery.util.getOrgLocationDetails(-100001)
	row114.results = result.getOrgHierarchyId()
}
catch(Exception e){
	row114.results = e.getMessage()

	}
F.gr5.rows << row114

//getOrgLocationDetails_ZERO
def row115 = F.gr5.newRow()
row115.instance_rec_num = 115
row115.api = "getOrgLocationDetails_ZERO"
try{
	def result = Emery.util.getOrgLocationDetails(0)
	row115.results = result.getOrgHierarchyId()
}
catch(Exception e){
	row115.results = e.getMessage()

	}
F.gr5.rows << row115


//getOrgLocationDetails_RandomNo - Not present in DB
def row116 = F.gr5.newRow()
row116.instance_rec_num = 116
row116.api = "getOrgLocationDetails_RandomNo"
try{
	def result = Emery.util.getOrgLocationDetails(123)
	row116.results = result.getOrgHierarchyId()
}
catch(Exception e){
	row116.results = e.getMessage()

	}
F.gr5.rows << row116



//getOrgLocationDetails_StringValue
def row117 = F.gr5.newRow()
row117.instance_rec_num = 117
row117.api = "getOrgLocationDetails_StringValue"
try{
	row117.results = result.getOrgHierarchyId()
	def result = Emery.util.getOrgLocationDetails("SomeText")
}
catch(Exception e){
	row117.results = e.getMessage()

	}
F.gr5.rows << row117


//currentDateAsString
def row118 = F.gr5.newRow()
row118.instance_rec_num = 50
row118.api = "currentDateAsString"
F.gr5.rows << row118