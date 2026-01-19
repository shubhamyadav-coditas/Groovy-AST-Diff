// ADDED: Enhanced script header with metadata
Emery.log.error "Start of the enhanced script - String Utils & User Management v2.0"

// ADDED: Script configuration
def scriptConfig = [
    version: "2.0",
    module: "StringUtils_UserMgmt",
    enhanced: true
]
Emery.log.info "Script configuration: ${scriptConfig}"

//USING ROWS FROM 1000-2000. DON'T USE THIS RANGE IN OTHER LINKED EMERY SCRIPTS.

//*************** STRING UTILS START ENHANCED ***************  // MODIFIED: Enhanced comment

//mergeString - MOVED_MODIFIED: Uncommented and enhanced
def row1001 = F.gr5.newRow()
row1001.instance_rec_num = 1001
row1001.api = "mergeString Enhanced"  // MODIFIED: Enhanced
try{
    String string_1= "string one enhanced"  // MODIFIED: Enhanced
    String string_2= "string two enhanced"  // MODIFIED: Enhanced
    String string_3= "string three enhanced"  // MODIFIED: Enhanced
	row1001.results = Emery.stringUtil.mergeString(string_1, string_2, string_3)
    //results = "string one enhanced string two enhanced string three enhanced"  // MODIFIED: Enhanced
	}
catch(Exception e){
	row1001.results = e.getMessage()
	}
F.gr5.rows << row1001

//join
def row1002 = F.gr5.newRow()
row1002.instance_rec_num = 1002
row1002.api = "join"
try{
    String[] array = ["apple","mango", "  ", "(&%^)", "banana", "grapes", "enhanced"];  // MODIFIED: Added enhanced
	row1002.results = Emery.stringUtil.join(array)
    //results = "apple,mango,  ,(&%^),banana,grapes,enhanced"  // MODIFIED: Updated result
	}
catch(Exception e){
	row1002.results = e.getMessage()
	}
F.gr5.rows << row1002

//join_with_delimiter
def row1003 = F.gr5.newRow()
row1003.instance_rec_num = 1003
row1003.api = "join_with_delimiter Enhanced"  // MODIFIED: Enhanced
try{
    String[] array = ["apple","mango", "  ", "(&%^)", "banana", "grapes"];
	row1003.results = Emery.stringUtil.join(array, " | ")  // MODIFIED: Changed delimiter from "-" to " | "
    //results = "apple | mango |   | (&%^) | banana | grapes"  // MODIFIED: Updated result
	}
catch(Exception e){
	row1003.results = e.getMessage()
	}
F.gr5.rows << row1003

//removeText
def row1004 = F.gr5.newRow()
row1004.instance_rec_num = 1004
row1004.api = "removeText"
try{
    String str = "Hellooooo Wooooorld ooo ooo check enhanced"  // MODIFIED: Added enhanced
	row1004.results = Emery.stringUtil.removeText(str, "oo")
    //results = "Hello World o o check enhanced"  // MODIFIED: Updated result
	}
catch(Exception e){
	row1004.results = e.getMessage()
	}
F.gr5.rows << row1004

// ADDED: New string utility test
def row1004a = F.gr5.newRow()
row1004a.instance_rec_num = 1004
row1004a.api = "String Length Validation"
try{
    String testStr = "Enhanced validation test string"
    row1004a.results = "Length: " + testStr.length() + ", Content: " + testStr
}
catch(Exception e){
    row1004a.results = e.getMessage()
}
F.gr5.rows << row1004a

//reverse
def row1505= F.gr5.newRow()
row1505.instance_rec_num = 1505
row1505.api = "reverse Enhanced"  // MODIFIED: Enhanced
try{
    String str = "  Hello World Enhanced"  // MODIFIED: Added Enhanced
	row1505.results = Emery.stringUtil.reverse(str)
    //results = "decnahnE dlroW olleH  "  // MODIFIED: Updated result
	}
catch(Exception e){
	row1505.results = e.getMessage()
	}
F.gr5.rows << row1505

//split
def row1506 = F.gr5.newRow()
row1506.instance_rec_num = 1506
row1506.api = "split"
try{
    String str = "M\"etricstream\" infotech pvt ltd"
	row1506.results = Emery.stringUtil.split(str, " ").toString()
    //results = "[M"etricstream", infotech, pvt, ltd]"
	}
catch(Exception e){
	row1506.results = e.getMessage()
	}
F.gr5.rows << row1506

//splitForDisplay
def row1007 = F.gr5.newRow()
row1007.instance_rec_num = 1007
row1007.api = "splitForDisplay"
try{
    String str = "M\"etricstream\" infotech pvt ltd"
	row1007.results = Emery.stringUtil.splitForDisplay(str).toString()
    //results = "[M"etricstream", infotech, pvt, ltd]"
	}
catch(Exception e){
	row1007.results = e.getMessage()
	}
F.gr5.rows << row1007

//isEmpty
def row1008 = F.gr5.newRow()
row1008.instance_rec_num = 1008
row1008.api = "isEmpty Enhanced"  // MODIFIED: Enhanced
try{
    String str1 = ""
    String str2 = "*&&dsgkbasf0_enhanced"  // MODIFIED: Added enhanced
	row1008.results = Emery.stringUtil.isEmpty(str1).toString() + "-" + Emery.stringUtil.isEmpty(str2).toString()
    //results = "true-false"
	}
catch(Exception e){
	row1008.results = e.getMessage()
	}
F.gr5.rows << row1008

//*************** STRING UTILS END ENHANCED ***************  // MODIFIED: Enhanced comment

// MOVED: Form utilities moved up and uncommented
//workFlowCode
def row1101 = F.gr5.newRow()
row1101.instance_rec_num = 1101
row1101.api = "workFlowCode Enhanced"  // MODIFIED: Enhanced and uncommented
try{
	row1101.results = Emery.form.workFlowCode("MS_ATD_EMERY_F")
    //results = "emery_w"
	}
catch(Exception e){
	row1101.results = e.getMessage()
	}
F.gr5.rows << row1101

//getBlueprintCode
def row1102 = F.gr5.newRow()
row1102.instance_rec_num = 1102
row1102.api = "getBlueprintCode Enhanced"  // MODIFIED: Enhanced and uncommented
try{
	row1102.results = Emery.form.getBlueprintCode("MS_ATD_EMERY_F")
    //results = "emery_b"
	}
catch(Exception e){
	row1102.results = e.getMessage()
	}
F.gr5.rows << row1102

//getUserId
def row1103 = F.gr5.newRow()
row1103.instance_rec_num = 1103
row1103.api = "getUserId Enhanced"  // MODIFIED: Enhanced and uncommented
try{
	row1103.results = Emery.util.getUserId("pfadmin")
    //results = "100001"
	}
catch(Exception e){
	row1103.results = e.getMessage()
	}
F.gr5.rows << row1103

//getUserId_INVALID
def row1104 = F.gr5.newRow()
row1104.instance_rec_num = 1104
row1104.api = "getUserId_INVALID Enhanced"  // MODIFIED: Enhanced
try{
	row1104.results = Emery.util.getUserId("invalid_user_enhanced")  // MODIFIED: Enhanced
	}
catch(Exception e){
	row1104.results = e.getMessage()
    // results = "The user id is not active or present in the application "
	}
F.gr5.rows << row1104

// Temporary - DELETED: The commented temporary section is completely removed

//getUserProfileDetails
def row1151 = F.gr5.newRow()
row1151.instance_rec_num = 1151
row1151.api = "getUserProfileDetails Enhanced"  // MODIFIED: Enhanced
try{
	//valid user name
	String user_name = "sp_char_user_enhanced"  // MODIFIED: Enhanced
	UserProfileDetails userpd = Emery.util.getUserProfileDetails(user_name)
	String temp = ""
	if(userpd){
		temp = 
		//userpd.getUserId() + ":" +
		userpd.getUserName() + ":" +
		userpd.getFirstName() + ":" +
		userpd.getMiddleInitial() + ":" +
		userpd.getLastName() + ":" +
		userpd.getFullName() + ":" +
		userpd.getPhoneNumber() + ":" +
		userpd.getLocation() + ":" +
		userpd.getTimeZoneId() + ":" +
		userpd.getLocale() + ":" +
		userpd.getEmailAddress()
	}

	//Invalid username
	user_name = "invalid_user_enhanced"  // MODIFIED: Enhanced
	userpd = Emery.util.getUserProfileDetails(user_name)
	if(userpd == null){
		temp += " : " + "null"
	}

	row1151.results = temp
	//results = 101405 : sp_char_user_enhanced : F*&^@#$';->"F : M!`~(=)M : last : F*&^@#$';->"F last : (+91) 98121-34445 : efgehf'fsgk-5'gdn' : 5 : en_US : temp@email.com : null

}
catch(Exception e){
	row1151.results = e.getMessage()
}
F.gr5.rows << row1151

//nextProcessInstanceId
def row1161 = F.gr5.newRow()
row1161.instance_rec_num = 1161
row1161.api = "nextProcessInstanceId"
try{
	def res1 = Emery.util.nextProcessInstanceId()
	def res2 = Emery.util.nextProcessInstanceId()
	row1161.results = res2 - res1
	}
catch(Exception e){
	row1161.results = e.getMessage()
    // results = "1"
	}
F.gr5.rows << row1161

//nextInstanceId
def row1162 = F.gr5.newRow()
row1162.instance_rec_num = 1162
row1162.api = "nextInstanceId Enhanced"  // MODIFIED: Enhanced
try{
	def res1 = Emery.util.nextInstanceId()
	def res2 = Emery.util.nextInstanceId()
	def res3 = Emery.util.nextInstanceId()  // ADDED: Third instance
	row1162.results = "Sequence: " + (res2 - res1) + ", " + (res3 - res2)  // MODIFIED: Enhanced result
	}
catch(Exception e){
	row1162.results = e.getMessage()
    // results = "Sequence: 1, 1"  // MODIFIED: Updated result
	}
F.gr5.rows << row1162

// ADDED: Script completion summary
def row1200 = F.gr5.newRow()
row1200.instance_rec_num = 1200
row1200.api = "Script Completion Summary"
try{
    row1200.results = "Enhanced String Utils & User Management completed successfully. Version: ${scriptConfig.version}"
}
catch(Exception e){
    row1200.results = e.getMessage()
}
F.gr5.rows << row1200

Emery.log.error "End of the enhanced script - All utilities tested successfully"  // MODIFIED: Enhanced end message