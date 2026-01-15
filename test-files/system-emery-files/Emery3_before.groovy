Emery.log.error "Start of the script"

//USING ROWS FROM 1000-2000. DON'T USE THIS RANGE IN OTHER LINKED EMERY SCRIPTS.

//*************** STRING UTILS START ***************

//mergeString
/*def row1001 = F.gr5.newRow()
row1001.instance_rec_num = 1001
row1001.api = "mergeString"
try{
    String string_1= "string one"
    String string_2= "string two"
    String string_3= "string three"
	row1001.results = Emery.stringUtil.mergeString(string_1, string_2, string_3)
    //results = "string one string two string three"
	}
catch(Exception e){
	row1001.results = e.getMessage()
	}
F.gr5.rows << row1001*/


//join
def row1002 = F.gr5.newRow()
row1002.instance_rec_num = 1002
row1002.api = "join"
try{
    String[] array = ["apple","mango", "  ", "(&%^)", "banana", "grapes"];
	row1002.results = Emery.stringUtil.join(array)
    //results = "apple,mango,  ,(&%^),banana,grapes"
	}
catch(Exception e){
	row1002.results = e.getMessage()
	}
F.gr5.rows << row1002


//join_with_delimiter
def row1003 = F.gr5.newRow()
row1003.instance_rec_num = 1003
row1003.api = "join_with_delimiter"
try{
    String[] array = ["apple","mango", "  ", "(&%^)", "banana", "grapes"];
	row1003.results = Emery.stringUtil.join(array, "-")
    //results = "apple-mango-  -(&%^)-banana-grapes"
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
    String str = "Hellooooo Wooooorld ooo ooo check"
	row1004.results = Emery.stringUtil.removeText(str, "oo")
    //results = "Hello World o o check"
	}
catch(Exception e){
	row1004.results = e.getMessage()
	}
F.gr5.rows << row1004

//reverse
def row1505= F.gr5.newRow()
row1505.instance_rec_num = 1505
row1505.api = "reverse"
try{
    String str = "  Hello World"
	row1505.results = Emery.stringUtil.reverse(str)
    //results = "dlroW olleH  "
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
row1008.api = "isEmpty"
try{
    String str1 = ""
    String str2 = "*&&dsgkbasf0"
	row1008.results = Emery.stringUtil.isEmpty(str1).toString() + "-" + Emery.stringUtil.isEmpty(str2).toString()
    //results = "true-false"
	}
catch(Exception e){
	row1008.results = e.getMessage()
	}
F.gr5.rows << row1008

//*************** STRING UTILS END ***************


//workFlowCode
/*def row1101 = F.gr5.newRow()
row1101.instance_rec_num = 1101
row1101.api = "workFlowCode"
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
row1102.api = "getBlueprintCode"
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
row1103.api = "getUserId"
try{
	row1103.results = Emery.util.getUserId("pfadmin")
    //results = "100001"
	}
catch(Exception e){
	row1103.results = e.getMessage()
	}
F.gr5.rows << row1103*/


//getUserId_INVALID
def row1104 = F.gr5.newRow()
row1104.instance_rec_num = 1104
row1104.api = "getUserId_INVALID"
try{
	row1104.results = Emery.util.getUserId("invalid_user")
	}
catch(Exception e){
	row1104.results = e.getMessage()
    // results = "The user id is not active or present in the application "
	}
F.gr5.rows << row1104



// //Temporary
// Emery.log.error("########Started script") 
// List<String> tupleId = new ArrayList<>()
// tupleId.add("100007")
// List<String> roleId = new ArrayList<>()
// roleId.add("100026")
// HierarchyAccess accessLevel=HierarchyAccess.EXACT

// List<User> result = Emery.mdos.getUsersBasedOnRole(tupleId,roleId,accessLevel)
// for(user in result){
//     Emery.log.error("User : {}",user)
//     Emery.log.error("User_ID : {}",user.getUserId())
//     Emery.log.error("User_UserName : {}",user.getUserName())
//     Emery.log.error("User_UserFullName : {}",user.getUserFullName())
// }

//getUserProfileDetails
def row1151 = F.gr5.newRow()
row1151.instance_rec_num = 1151
row1151.api = "getUserProfileDetails"
try{
	//valid user name
	String user_name = "sp_char_user"
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
	user_name = "invalid_user"
	userpd = Emery.util.getUserProfileDetails(user_name)
	if(userpd == null){
		temp += " : " + "null"
	}

	row1151.results = temp
	//results = 101405 : sp_char_user : F*&^@#$';->"F : M!`~(=)M : last : F*&^@#$';->"F last : (+91) 98121-34445 : efgehf'fsgk-5'gdn' : 5 : en_US : temp@email.com : null

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
row1162.api = "nextInstanceId"
try{
	def res1 = Emery.util.nextInstanceId()
	def res2 = Emery.util.nextInstanceId()
	row1162.results = res2 - res1
	}
catch(Exception e){
	row1162.results = e.getMessage()
    // results = "1"
	}
F.gr5.rows << row1162



Emery.log.error "End of the script"
