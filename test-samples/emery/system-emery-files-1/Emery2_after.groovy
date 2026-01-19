// ADDED: Enhanced script header
Emery.log.error "Start script - Enhanced Date Utilities Version"

// ADDED: New performance monitoring
def performanceStart = System.currentTimeMillis()

// ************ General API's Enhanced ***********  // MODIFIED: Enhanced comment

//isMDOS
def row30 = F.gr5.newRow()
row30.instance_rec_num = 30
row30.api = "isMDOS Enhanced"  // MODIFIED: Enhanced
try{
	row30.results = Emery.ctx.isMDOS()
	}
catch(Exception e){
	row30.results = e.getMessage()
	//
	}
F.gr5.rows << row30

//isSDOS
def row31 = F.gr5.newRow()
row31.instance_rec_num = 31
row31.api = "isSDOS Enhanced"  // MODIFIED: Enhanced
try{
	row31.results = Emery.ctx.isSDOS()
	}
catch(Exception e){
	row31.results = e.getMessage()
	//
	}
F.gr5.rows << row31

// ADDED: New system check
def row31a = F.gr5.newRow()
row31a.instance_rec_num = 31
row31a.api = "System Environment Check"
try{
	row31a.results = "Environment: " + (Emery.ctx.isMDOS() ? "MDOS" : "SDOS")
}
catch(Exception e){
	row31a.results = e.getMessage()
}
F.gr5.rows << row31a

//*************** DATE UTILITIES ENHANCED ***************  // MODIFIED: Enhanced comment

//addDays
def row32 = F.gr5.newRow()
row32.instance_rec_num = 32
row32.api = "addDays"
try{
	Date date1 = new Date(2020,7,22)
	int day = 3  // MODIFIED: Changed from 2 to 3
	row32.results = Emery.dateUtil.addDays(date1, day)
	}
catch(Exception e){
	row32.results = e.getMessage()
	//new Date(2020,3,10,9,30,30)
	}
F.gr5.rows << row32

//addHours
def row33 = F.gr5.newRow()
row33.instance_rec_num = 33
row33.api = "addHours"
try{
	Date date2 = new Date(2020,7,22)
	int day2 = 5
	row33.results = Emery.dateUtil.addHours(date2, day2)
	}
catch(Exception e){
	row33.results = e.getMessage()
	//Sun Aug 22 05:00:00 IST 3920
	}
F.gr5.rows << row33

//addMilliseconds
def row34 = F.gr5.newRow()
row34.instance_rec_num = 34
row34.api = "addMilliseconds"
try{
	Date date2 = new Date(2020,7,22)
	int day2 = 1000
	row34.results = Emery.dateUtil.addMilliseconds(date2, day2)
	}
catch(Exception e){
	row34.results = e.getMessage()
	//Sun Aug 22 00:00:01 IST 3920
	}
F.gr5.rows << row34

//addMinutes
def row35 = F.gr5.newRow()
row35.instance_rec_num = 35
row35.api = "addMinutes Enhanced"  // MODIFIED: Enhanced
try{
	Date date2 = new Date(2020,7,22)
	int day2 = 45  // MODIFIED: Changed from 30 to 45
	row35.results = Emery.dateUtil.addMinutes(date2, day2)
	}
catch(Exception e){
	row35.results = e.getMessage()
	//Sun Aug 22 00:45:00 IST 3920  // MODIFIED: Updated comment
	}
F.gr5.rows << row35

//addSeconds
def row36 = F.gr5.newRow()
row36.instance_rec_num = 36
row36.api = "addSeconds"
try{
	Date date2 = new Date(2020,7,22)
	int day2 = 30
	row36.results = Emery.dateUtil.addSeconds(date2, day2)
	}
catch(Exception e){
	row36.results = e.getMessage()
	//Sun Aug 22 00:00:30 IST 3920
	}
F.gr5.rows << row36

//addWeeks
def row37 = F.gr5.newRow()
row37.instance_rec_num = 37
row37.api = "addWeeks"
try{
	Date date2 = new Date(2020,7,22)
	int day2 = 2
	row37.results = Emery.dateUtil.addWeeks(date2, day2)
	}
catch(Exception e){
	row37.results = e.getMessage()
	//Sun Sep 05 00:00:00 IST 3920
	}
F.gr5.rows << row37

//addYears
def row38 = F.gr5.newRow()
row38.instance_rec_num = 38
row38.api = "addYears"
try{
	Date date2 = new Date(2020,7,22)
	int day2 = 2
	row38.results = Emery.dateUtil.addYears(date2, day2)
	}
catch(Exception e){
	row38.results = e.getMessage()
	//Tue Aug 22 00:00:00 IST 3922
	}
F.gr5.rows << row38

// MOVED: isSameDateTime moved before isSameDate
//isSameDateTime - Valid
def row41 = F.gr5.newRow()
row41.instance_rec_num = 41
row41.api = "isSameDateTime-VALID Enhanced"  // MODIFIED: Enhanced
try{
	Date date1 = new Date(2020,3,10,9,30,30)
	Date date2 = new Date(2020,3,10,9,30,30)
	row41.results = Emery.dateUtil.isSameDateTime(date1, date2)
	}
catch(Exception e){
	row41.results = e.getMessage()
	//true
	}
F.gr5.rows << row41

//isSameDateTime - null
def row42 = F.gr5.newRow()
row42.instance_rec_num = 42
row42.api = "isSameDateTime-NULL"
try{
	Date date1 = null
	Date date2 = new Date(2020,3,10,9,30,30)
	row42.results = Emery.dateUtil.isSameDateTime(date1, date2)
	}
catch(Exception e){
	row42.results = e.getMessage()
	//Invalid date1 value provided, date1 should not be empty or null
	}
F.gr5.rows << row42

//isSameDate - Valid
def row39 = F.gr5.newRow()
row39.instance_rec_num = 39
row39.api = "isSameDate-VALID"
try{
	Date date1 = Emery.util.currentDate()
	Date date2 = Emery.util.currentDate()
	row39.results = Emery.dateUtil.isSameDate(date1, date2)
	}
catch(Exception e){
	row39.results= e.getMessage()
	//true
	}
F.gr5.rows << row39

//isSameDate - null
def row40 = F.gr5.newRow()
row40.instance_rec_num = 40
row40.api = "isSameDate-NULL"
try{
	Date date1 = null
	Date date2 = Emery.util.currentDate()
	row40.results = Emery.dateUtil.isSameDate(date1, date2)
	}
catch(Exception e){
	row40.results= e.getMessage()
	//Invalid date1 value provided, date1 should not be empty or null
	}
F.gr5.rows << row40

//isTodayBetween Date Format
def row43 = F.gr5.newRow()
row43.instance_rec_num = 43
row43.api = "isTodayBetween-DATE"
try{
	Date fromDate = new Date()-2
	Date untilDate = new Date()+2
	row43.results = Emery.dateUtil.isTodayBetween(fromDate,untilDate)
	}
catch(Exception e){
	row43.results = e.getMessage()
	//true
	}
F.gr5.rows << row43

//isTodayBetween String Format
def row44 = F.gr5.newRow()
row44.instance_rec_num = 44
row44.api = "isTodayBetween-STRING Enhanced"  // MODIFIED: Enhanced
try{
	String fromDateString = "05/15/2020 03:30:01"
	String untilDateString = "05/15/2025 03:30:01"  // MODIFIED: Changed year to 2025
	row44.results = Emery.dateUtil.isTodayBetween(fromDateString,untilDateString)
	}
catch(Exception e){
	row44.results = e.getMessage()
	//false
	}
F.gr5.rows << row44

//convertDateToString Valid
def row45 = F.gr5.newRow()
row45.instance_rec_num = 45
row45.api = "convertDateToString-VALID"
try{
	Calendar cal = Calendar.getInstance()
	cal.set(2020, 7, 15, 9, 30, 30)
	Date baseDate = cal.getTime()
	String format2 = "dd-MMM-yyyy HH:mm:ss"
	row45.results = Emery.dateUtil.convertDateToString(baseDate, format2)
	}
catch(Exception e){
	row45.results = e.getMessage()
	//15-Aug-2020 09:30:30
	}
F.gr5.rows << row45

//convertDateToString InValid
def row46 = F.gr5.newRow()
row46.instance_rec_num = 46
row46.api = "convertDateToString-INVALID"
try{
	Date date1 = null
	String format2 = "dd-MMM-yyyy HH:mm:ss"
	row46.results = Emery.dateUtil.convertDateToString(date1, format2)
	}
catch(Exception e){
	row46.results = e.getMessage()
	//Invalid date value provided, date should not be empty or null
	}
F.gr5.rows << row46

//convertStringToDate Valid
def row47 = F.gr5.newRow()
row47.instance_rec_num = 47
row47.api = "convertStringToDate-VALID"
try{
	String sDate = "15-Aug-2020 09:30:30"
	String format = "dd-MMM-yyyy HH:mm:ss"
	row47.results = Emery.dateUtil.convertStringToDate(sDate, format)
	}
catch(Exception e){
	row47.results = e.getMessage()
	//Thu Dec 31 00:00:00 UTC 1998
	}
F.gr5.rows << row47

//convertStringToDate InValid
def row48 = F.gr5.newRow()
row48.instance_rec_num = 48
row48.api = "convertStringToDate-INVALID"
try{
	String sDate = "null"
	String format = "E, MMM dd yyyy"
	row48.results = Emery.dateUtil.convertStringToDate(sDate, format)
	}
catch(Exception e){
	row48.results = e.getMessage()
	//Invalid string value provided, date should not be empty or null
	}
F.gr5.rows << row48

//isLeapYear Valid
def row49 = F.gr5.newRow()
row49.instance_rec_num = 49
row49.api = "isLeapYear-2024"  // MODIFIED: Changed from 2021 to 2024 (leap year)
try{
	
def LeapYear1 = 2024  // MODIFIED: Changed from 2021 to 2024
row49.results = Emery.dateUtil.isLeapYear(LeapYear1)

Emery.log.info "java.lang.Integer year : {}", Emery.dateUtil.isLeapYear(LeapYear1)
	}
catch(Exception e){
	row49.results = e.getMessage()	
	}
F.gr5.rows << row49

//isLeapYear passing null
def row50 = F.gr5.newRow()
row50.instance_rec_num = 50
row50.api = "isLeapYear-Null"
try{
	
def LeapYear2 = null
row50.results = Emery.dateUtil.isLeapYear(LeapYear2)

	}
catch(Exception e){
	row50.results = e.getMessage()	
	}
F.gr5.rows << row50

/**************************************** Row no starting from 61 Enhanced ********************************************/  // MODIFIED: Enhanced comment

//getMdosDisplayValuesClob - Passing single stored value
def row61 = F.gr5.newRow()
row61.instance_rec_num = 61
row61.api = "getMdosDisplayValuesClob_SingleStoredValue"
try{
	row61.results = Emery.mdos.getMdosDisplayValuesClob('100000',',','',1009)
	}
catch(Exception e){
	row61.results = e.getMessage()
	//Output: MetricStream → Compliance → Global → Assurance Solutions Agency Inc.
	}
F.gr5.rows << row61

//getMdosDisplayValuesClob - Passing Multiple stored Values
def row62 = F.gr5.newRow()
row62.instance_rec_num = 62
row62.api = "getMdosDisplayValuesClob_MultipleStoredValue"
try{
	row62.results = Emery.mdos.getMdosDisplayValuesClob('100000,100001',',','',1009)
	}
catch(Exception e){
	row62.results = e.getMessage()
	//Output:MetricStream → Compliance → Global → Assurance Solutions Agency Inc.,MetricStream → Marketing → Global → Chartered Financial Holdings Limited
	}
F.gr5.rows << row62

//getMdosDisplayValuesClob - Passing specialchar @ for StringDelimiter
def row63 = F.gr5.newRow()
row63.instance_rec_num = 63
row63.api = "getMdosDisplayValuesClob_StringDelimiter Enhanced"  // MODIFIED: Enhanced
try{
	row63.results = Emery.mdos.getMdosDisplayValuesClob('100000@100001','@','',1009)
	}
catch(Exception e){
	row63.results = e.getMessage()
	//Output:MetricStream → Compliance → Global → Assurance Solutions Agency Inc.@MetricStream → Marketing → Global → Chartered Financial Holdings Limited
	}
F.gr5.rows << row63

//getMdosDisplayValuesClob - Passing specialchar * for StringDelimiter and ### for DisplayValueDelimiter
def row64 = F.gr5.newRow()
row64.instance_rec_num = 64
row64.api = "getMdosDisplayValuesClob_DisplayValueDelimiter Enhanced"  // MODIFIED: Enhanced
try{
	row64.results = Emery.mdos.getMdosDisplayValuesClob('100000*100001','*','###',1009)
		}
catch(Exception e){
	row64.results = e.getMessage()
	//Output: MetricStream ### Compliance ### Global ### Assurance Solutions Agency Inc.*MetricStream ### Marketing ### Global ### Chartered Financial Holdings Limited}
    }
F.gr5.rows << row64

//getMdosDisplayValuesClob - Passing Empty for StringDelimiter
def row65 = F.gr5.newRow()
row65.instance_rec_num = 65
row65.api = "getMdosDisplayValuesClob_Empty"
try{
	row65.results = Emery.mdos.getMdosDisplayValuesClob('100000,100001','','###',1009)
	}
catch(Exception e){
	row65.results = e.getMessage()
	//Output: Please provide correct delimiter, delimiter should not be empty or null
	}
F.gr5.rows << row65

// ADDED: New performance summary
def performanceEnd = System.currentTimeMillis()
def row66 = F.gr5.newRow()
row66.instance_rec_num = 66
row66.api = "Performance Summary"
try{
	row66.results = "Total execution time: " + (performanceEnd - performanceStart) + "ms"
}
catch(Exception e){
	row66.results = e.getMessage()
}
F.gr5.rows << row66

// DELETED: The original "End of the script" message is removed

Emery.log.error "Enhanced Date Utilities Script Completed Successfully"  // MODIFIED: Enhanced end message