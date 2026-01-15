String script_name = "storedToDisplayLOV"

Emery.log.error "Start {} script", script_name

String lovName = "test_lov"
Integer locale_id = 1009
int error_count = 0;

//getLovDisplayValue
def row1901 = F.gr5.newRow()
row1901.instance_rec_num = 1901
row1901.api = "getLovDisplayValue"
try{
    String out1 = Emery.lov.getLovDisplayValue(lovName, locale_id, "7");
	row1901.results = out1
    //results = "seven!@#`,:;'(=>)"%$#@%^&"
	}
catch(Exception e){
	row1901.results = e.getMessage()
	}
F.gr5.rows << row1901


//getLovDisplayValue_invalid_lov_name
def row1902 = F.gr5.newRow()
row1902.instance_rec_num = 1902
row1902.api = "getLovDisplayValue_invalid_lov_name"
try{
    String out1 = Emery.lov.getLovDisplayValue("invalid_lov_name", locale_id, "7")
	row1902.results = out1
	}
catch(Exception e){
	row1902.results = e.getMessage()
    //results = "LOV with name invalid_lov_name is not present"
	}
F.gr5.rows << row1902


//getLovDisplayValue_empty_string_as_SV
def row1903 = F.gr5.newRow()
row1903.instance_rec_num = 1903
row1903.api = "getLovDisplayValue_empty_string_as_SV"
try{
    String out1 = Emery.lov.getLovDisplayValue(lovName, locale_id, "")
	row1903.results = out1
	}
catch(Exception e){
	row1903.results = e.getMessage()
    //results = "Specify valid stored value to get display value. Value cannot be empty or null."
	}
F.gr5.rows << row1903


//getLovDisplayValue_null_as_SV
def row1904 = F.gr5.newRow()
row1904.instance_rec_num = 1904
row1904.api = "getLovDisplayValue_null_as_SV"
try{
    String out1 = Emery.lov.getLovDisplayValue(lovName, locale_id, null)
	row1904.results = out1
	}
catch(Exception e){
	row1904.results = e.getMessage()
    //results = "Specify valid stored value to get display value. Value cannot be empty or null."
	}
F.gr5.rows << row1904


//getLovDisplayValue_empty_string_as_localeID
def row1905 = F.gr5.newRow()
row1905.instance_rec_num = 1905
row1905.api = "getLovDisplayValue_empty_string_as_localeID"
try{
    String out1 = Emery.lov.getLovDisplayValue(lovName, "", "7")
	row1905.results = out1
	}
catch(Exception e){
	row1905.results = e.getMessage()
    //results = "Valid locale needs to be specified to fetch LOV values. Locale cannot be empty or null."
	}
F.gr5.rows << row1905


//getLovDisplayValue_invalid_SV
def row1906 = F.gr5.newRow()
row1906.instance_rec_num = 1906
row1906.api = "getLovDisplayValue_invalid_SV"
try{
    String out1 = Emery.lov.getLovDisplayValue(lovName, locale_id, "210i4")
	if(out1 == null){
		row1906.results = "null"
		//results = "null"
	}
	else{
		row1906.results = out1
	}
	}
catch(Exception e){
	row1906.results = e.getMessage()
	}
F.gr5.rows << row1906


//getLovDisplayValues
def row1907 = F.gr5.newRow()
row1907.instance_rec_num = 1907
row1907.api = "getLovDisplayValues"
try{
    Map<String, String> out1 = Emery.lov.getLovDisplayValues(lovName, locale_id, ["5", "7"])
	row1907.results = out1
	//results = {5=five, 7=seven!@#`,:;'(=>)"%$#@%^&}
	}
catch(Exception e){
	row1907.results = e.getMessage()
	}
F.gr5.rows << row1907


//getLovDisplayValues_invalid_lov_name
def row1908 = F.gr5.newRow()
row1908.instance_rec_num = 1908
row1908.api = "getLovDisplayValues_invalid_lov_name"
try{
     Map<String, String> out1 = Emery.lov.getLovDisplayValues("invalid_lov_name", locale_id, ["5", "7"])
	row1908.results = out1
	}
catch(Exception e){
	row1908.results = e.getMessage()
    //results = "LOV with name invalid_lov_name is not present"
	}
F.gr5.rows << row1908


//getLovDisplayValues_empty_list_as_SVs
def row1909 = F.gr5.newRow()
row1909.instance_rec_num = 1909
row1909.api = "getLovDisplayValues_empty_list_as_SVs"
try{
    Map<String, String> out1 = Emery.lov.getLovDisplayValues(lovName, locale_id, [])
	row1909.results = out1
	}
catch(Exception e){
	row1909.results = e.getMessage()
    //results = "Specify valid stored values to get display values. Stored Values cannot be empty."
	}
F.gr5.rows << row1909


//getLovDisplayValues_nulls_as_SVs
def row1910 = F.gr5.newRow()
row1910.instance_rec_num = 1910
row1910.api = "getLovDisplayValues_nulls_as_SVs"
try{
    Map<String, String> out1 = Emery.lov.getLovDisplayValues(lovName, locale_id, [null, null])
	row1910.results = out1
	}
catch(Exception e){
	row1910.results = e.getMessage()
    //results = {null=null}
	}
F.gr5.rows << row1910


//getLovDisplayValues_empty_string_as_localeID
def row1911 = F.gr5.newRow()
row1911.instance_rec_num = 1911
row1911.api = "getLovDisplayValues_empty_string_as_localeID"
try{
    Map<String, String> out1 = Emery.lov.getLovDisplayValues(lovName, "", ["5", "7"])
	row1911.results = out1
	}
catch(Exception e){
	row1911.results = e.getMessage()
    //results = "Valid locale needs to be specified to fetch LOV values. Locale cannot be empty or null."
	}
F.gr5.rows << row1911


//getMultiLocaleLovDisplayValue
def row1912 = F.gr5.newRow()
row1912.instance_rec_num = 1912
row1912.api = "getMultiLocaleLovDisplayValue"
try{
    Map<Integer, String> out1 = Emery.lov.getMultiLocaleLovDisplayValue(lovName, [1009], "7")
	row1912.results = out1
	//results = {1009=seven!@#`,:;'(=>)"%$#@%^&}
	}
catch(Exception e){
	row1912.results = e.getMessage()
	}
F.gr5.rows << row1912


//getMultiLocaleLovDisplayValue_invalid_locale
def row1913 = F.gr5.newRow()
row1913.instance_rec_num = 1913
row1913.api = "getMultiLocaleLovDisplayValue_invalid_locale"
try{
    Map<Integer, String> out1 = Emery.lov.getMultiLocaleLovDisplayValue(lovName, [1009, 1008, 12344224], "7")
	row1913.results = out1
	}
catch(Exception e){
	row1913.results = e.getMessage()
    //results = "Locale specified for getting lov display values is not enabled."
	}
F.gr5.rows << row1913


//getMultiLocaleLovDisplayValues_invalid_locale
def row1914 = F.gr5.newRow()
row1914.instance_rec_num = 1914
row1914.api = "getMultiLocaleLovDisplayValues_invalid_locale"
try{
    Map<Integer, Map<String, String>> out1 = Emery.lov.getMultiLocaleLovDisplayValues(lovName, [1008, 1009, 12344224], ["5", "7"])
	row1914.results = out1
	}
catch(Exception e){
	row1914.results = e.getMessage()
    //results = "Locale specified for getting lov display values is not enabled."
	}
F.gr5.rows << row1914


//getMultiLocaleLovDisplayValues
def row1915 = F.gr5.newRow()
row1915.instance_rec_num = 1915
row1915.api = "getMultiLocaleLovDisplayValues"
try{
    String out1 = Emery.lov.getMultiLocaleLovDisplayValues(lovName, [1009], ["5", "7"])
	row1915.results = out1
	}
catch(Exception e){
	row1915.results = e.getMessage()
    //results = {1009={5=five, 7=seven!@#`,:;'(=>)%$#@%^&}}
	}
F.gr5.rows << row1915


//********** Get Store Values ******

//getLovStoredValue
def row1916 = F.gr5.newRow()
row1916.instance_rec_num = 1916
row1916.api = "getLovStoredValue"
try
{
	String singleStoredValue_valid = Emery.lov.getLovStoredValue(lovName,locale_id, "five")
	row1916.results = singleStoredValue_valid
	}
catch(Exception e){
	row1916.results = e.getMessage()
	}
F.gr5.rows << row1916

//getLovStoredValues
def row1917 = F.gr5.newRow()
row1917.instance_rec_num = 1917
row1917.api = "getLovStoredValues"
try
{
	String multipleStoredValue_valid = Emery.lov.getLovStoredValues(lovName,locale_id, ["five","six"])
	row1917.results = multipleStoredValue_valid
	}
catch(Exception e){
	row1917.results = e.getMessage()
	}
F.gr5.rows << row1917

//getLovStoredValuesSpecialChar
def row1918 = F.gr5.newRow()
row1918.instance_rec_num = 1918
row1918.api = "getLovStoredValues Special Char"
try
{
	String specialCharStoredValue_valid = Emery.lov.getLovStoredValue(lovName,locale_id, "seven!@#,:;=*%\$#@%^&")
	row1918.results = specialCharStoredValue_valid
	}
catch(Exception e){
	row1918.results = e.getMessage()
	}
F.gr5.rows << row1918


//getLovStoredValuesInvalid
def row1919 = F.gr5.newRow()
row1919.instance_rec_num = 1919
row1919.api = "getLovStoredValues Invalid"
try
{
	String invalid_name = Emery.lov.getLovStoredValue("ABCDEF",locale_id, "Turn OFF logging")
	//String invalidStoredValue_valid = Emery.lov.getLovStoredValue(lovName,locale_id, "xyzzz")
	//row1919.results = invalidStoredValue_valid
	}
catch (Exception e){
	Emery.log.error("TESTLOG: Exception while lov name LOV NAME : ABCDEF" + e.getMessage() +"  "+ e.getClass())
	row1919.results = e.getMessage()
	}
F.gr5.rows << row1919


Emery.log.error "End {} script", script_name
