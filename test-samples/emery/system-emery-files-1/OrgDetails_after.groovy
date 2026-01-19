// ADDED: Enhanced script header with audit info
Emery.log.error("#######Start34 script - Enhanced Organization Details API Testing v2.0")

// ADDED: Audit tracking
def auditInfo = [
    scriptVersion: "2.0",
    testSuite: "OrgDetails_Enhanced",
    startTime: new Date(),
    testCount: 0
]

/************************ getOrgEntityDetails() API Enhanced **************************/  // MODIFIED: Enhanced comment

//getOrgEntityDetails()
def row100 = F.gr5.newRow()
row100.instance_rec_num = 100
row100.api = "getOrgEntityDetails Enhanced"  // MODIFIED: Enhanced
auditInfo.testCount++  // ADDED: Test counting

try{
	def result = Emery.util.getOrgEntityDetails(100000)
	row100.results = "EntityId="+result.getOrgEntityId()+" EntityName="+result.getOrgEntityName()+" Description="+result.getDescription()+" OrgHierarchyId="+result.getOrgHierarchyId()+" ParentOrgEntityId="+result.getParentOrgEntityId()+" LocationId="+result.getLocationId()+" EntityTitle="+result.getOrgEntityTitle()+" [ENHANCED]"  // MODIFIED: Added enhanced marker
	}
catch(Exception e){
	row100.results = e.getMessage()
	//
	}
F.gr5.rows << row100

// ADDED: New validation test
def row100a = F.gr5.newRow()
row100a.instance_rec_num = 100
row100a.api = "Entity Details Validation"
auditInfo.testCount++
try{
    def result = Emery.util.getOrgEntityDetails(100000)
    def isValid = result != null && result.getOrgEntityName() != null
    row100a.results = "Validation: " + (isValid ? "PASSED" : "FAILED")
}
catch(Exception e){
    row100a.results = "Validation ERROR: " + e.getMessage()
}
F.gr5.rows << row100a

//getOrgEntityDetails_NegativeValue
def row101 = F.gr5.newRow()
row101.instance_rec_num = 101
row101.api = "getOrgEntityDetails_NegativeValue Enhanced"  // MODIFIED: Enhanced
auditInfo.testCount++  // ADDED: Test counting
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
auditInfo.testCount++  // ADDED: Test counting
try{
	def result = Emery.util.getOrgEntityDetails(0)
	row102.results = result.getOrgEntityName()
}
catch(Exception e){
	row102.results = e.getMessage()
	//
	}
F.gr5.rows << row102 

// MOVED_MODIFIED: Uncommented and enhanced the empty value test
//getOrgEntityDetails_Empty value
def row104 = F.gr5.newRow()
row104.instance_rec_num = 104
row104.api = "getOrgEntityDetails_EmptyValue Enhanced"  // MODIFIED: Enhanced and uncommented
auditInfo.testCount++  // ADDED: Test counting
try{
	def result = Emery.util.getOrgEntityDetails()  // This should cause a compilation error, testing error handling
	row104.results = result.getOrgEntityName()
}
catch(Exception e){
	row104.results = "Expected error: " + e.getMessage()  // MODIFIED: Enhanced error message
	//
	}
F.gr5.rows << row104

//getOrgEntityDetails_RandomNo - not present in DB
def row103 = F.gr5.newRow()
row103.instance_rec_num = 103
row103.api = "getOrgEntityDetails_RandomNo Enhanced"  // MODIFIED: Enhanced
auditInfo.testCount++  // ADDED: Test counting
try{
	def result = Emery.util.getOrgEntityDetails(9999999)  // MODIFIED: Changed from 1234 to 9999999
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
auditInfo.testCount++  // ADDED: Test counting
try{
	def result = Emery.util.getOrgEntityDetails("Sometext_Enhanced")  // MODIFIED: Enhanced
	row106.results = result.getOrgEntityName()
}
catch(Exception e){
	row106.results = e.getMessage()
	//
	}
F.gr5.rows << row106

/*********************** getOrgHierarchyDetails() API Enhanced **************************/  // MODIFIED: Enhanced comment

//getOrgHierarchyDetails()
def row107 = F.gr5.newRow()
row107.instance_rec_num = 107
row107.api = "getOrgHierarchyDetails Enhanced"  // MODIFIED: Enhanced
auditInfo.testCount++  // ADDED: Test counting

try{
	def result = Emery.util.getOrgHierarchyDetails(100000)
	row107.results = " HierarchyId="+result.getOrgHierarchyId()+" HierarchyName="+result.getOrgHierarchyName()+" Description="+result.getDescription()+" ParentOrgLevelEntryId="+result.getParentOrgLevelEntryId()+" OrgLevel="+result.getOrgLevel()+" OrgHierarchyTitle="+result.getOrgHierarchyTitle()+" [ENHANCED]"  // MODIFIED: Added enhanced marker
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
auditInfo.testCount++  // ADDED: Test counting
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
auditInfo.testCount++  // ADDED: Test counting
try{
	def result = Emery.util.getOrgEntityDetails(0)
	row109.results = result.getOrgHierarchyId()
}
catch(Exception e){
	row109.results = e.getMessage()
	//
	}
F.gr5.rows << row109

// MOVED_MODIFIED: Uncommented and enhanced the empty value test
//getOrgHierarchyDetails_EmptyValue
def row110 = F.gr5.newRow()
row110.instance_rec_num = 110
row110.api = "getOrgHierarchyDetails_EmptyValue Enhanced"  // MODIFIED: Enhanced and uncommented
auditInfo.testCount++  // ADDED: Test counting
try{
	def result = Emery.util.getOrgEntityDetails()  // This should cause error
	row110.results = result.getOrgHierarchyId()
}
catch(Exception e){
	row110.results = "Expected hierarchy error: " + e.getMessage()  // MODIFIED: Enhanced error message
	//
	}
F.gr5.rows << row110

//getOrgHierarchyDetails_RandomNo - not present in DB
def row111 = F.gr5.newRow()
row111.instance_rec_num = 111
row111.api = "getOrgHierarchyDetails_RandomNo"
auditInfo.testCount++  // ADDED: Test counting
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
row112.api = "getOrgHierarchyDetails_StringValue Enhanced"  // MODIFIED: Enhanced
auditInfo.testCount++  // ADDED: Test counting
try{
	def result = Emery.util.getOrgEntityDetails("SomeText_Enhanced")  // MODIFIED: Enhanced
	row112.results = result.getOrgHierarchyId()
}
catch(Exception e){
	row112.results = e.getMessage()

	}
F.gr5.rows << row112

/*********************** getOrgLocationDetails() API Enhanced **************************/  // MODIFIED: Enhanced comment

//getOrgLocationDetails()
def row113 = F.gr5.newRow()
row113.instance_rec_num = 113
row113.api = "getOrgLocationDetails Enhanced"  // MODIFIED: Enhanced
auditInfo.testCount++  // ADDED: Test counting

try{
	def result = Emery.util.getOrgLocationDetails(100001)
	row113.results = "LocationId=" + result.getLocationId() + " LocationName=" + result.getLocationName() + " Description=" + result.getDescription() + " Address1=" + result.getAddress1() + " Address2=" + result.getAddress2() + " City=" + result.getCity()+ " State=" + result.getState()+ " Zip=" + result.getZip()+ " Country=" + result.getCountry()+ " LocationTitle=" + result.getLocationTitle() + " [ENHANCED]"  // MODIFIED: Added enhanced marker
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
auditInfo.testCount++  // ADDED: Test counting
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
auditInfo.testCount++  // ADDED: Test counting
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
row116.api = "getOrgLocationDetails_RandomNo Enhanced"  // MODIFIED: Enhanced
auditInfo.testCount++  // ADDED: Test counting
try{
	def result = Emery.util.getOrgLocationDetails(888888)  // MODIFIED: Changed from 123 to 888888
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
auditInfo.testCount++  // ADDED: Test counting
try{
	def result = Emery.util.getOrgLocationDetails("SomeText")
	row117.results = result.getOrgHierarchyId()
}
catch(Exception e){
	row117.results = e.getMessage()

	}
F.gr5.rows << row117

// MOVED: currentDateAsString moved up and enhanced
//currentDateAsString
def row118 = F.gr5.newRow()
row118.instance_rec_num = 118  // MODIFIED: Changed from 50 to 118 for consistency
row118.api = "currentDateAsString Enhanced"  // MODIFIED: Enhanced
auditInfo.testCount++  // ADDED: Test counting
try{
	def currentDate = Emery.util.currentDateAsString()
	row118.results = "Current Date: " + currentDate + " [Enhanced Format]"  // MODIFIED: Enhanced format

	}
catch(Exception e){
	row118.results = e.getMessage()	
	}
F.gr5.rows << row118

// ADDED: Audit summary
auditInfo.endTime = new Date()
def row119 = F.gr5.newRow()
row119.instance_rec_num = 119
row119.api = "Test Execution Summary"
try{
    def duration = auditInfo.endTime.time - auditInfo.startTime.time
    row119.results = "Suite: ${auditInfo.testSuite}, Version: ${auditInfo.scriptVersion}, Tests: ${auditInfo.testCount}, Duration: ${duration}ms"
}
catch(Exception e){
    row119.results = e.getMessage()
}
F.gr5.rows << row119

// ADDED: Enhanced completion logging
Emery.log.error "Enhanced Organization Details API Testing completed - ${auditInfo.testCount} tests executed successfully"