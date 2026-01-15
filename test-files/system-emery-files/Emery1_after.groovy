Emery.log.error "Start script"

def row1 = F.gr5.newRow()
row1.api = "formId"
row1.instance_rec_num = 2
row1.results = Emery.form.formId("MS_ATD_EMERYAUTOMATION2")
F.gr5.rows << row1
def row2 = F.gr5.newRow()
row2.api = "Workflow Code 2"
row2.instance_rec_num = 2
row2.results = Emery.form.workFlowCode("MS_ATD_EMERYAUTOMATION2")
F.gr5.rows << row2
def row3 = F.gr5.newRow()
row3.instance_rec_num = 30
row3.api = "Blueprint Code row3"
row3.results = Emery.form.getBlueprintCode("MS_ATD_EMERYAUTOMATION2")
F.gr5.rows << row3
def row4 = F.gr5.newRow()
HashMap<String,String> name = Emery.util.userFullName(100001)	
row4.instance_rec_num = 4
row4.api = "User full name for ID 100001"

F.gr5.rows << row4
def row5 = F.gr5.newRow()	
row5.instance_rec_num = 5

row5.results = Emery.util.getUserId("pfadmin")
F.gr5.rows << row5
def formId = Emery.form.formId("MS_ATD_EMERYAUTOMATION2")
def row6 = F.gr5.newRow()	
row6.instance_rec_num = 6
row6.api = "Form Name"
row6.results = Emery.form.formName(formId)
F.gr5.rows << row6

/*def row7 = F.gr5.newRow()	
row7.instance_rec_num = 7
row7.api = "Config Parameter Lov"
row7.results = Emery.ctx.configurationParameter("EMERY CONFIGURATIONS", "Emery Batch Processing of MDOS tuples")
F.gr5.rows << row7*/

def row8 = F.gr5.newRow()	
row8.instance_rec_num = 8
row8.api = "Config Parameter"
row8.results = Emery.ctx.configurationParameter("EMERY CONFIGURATIONS", "BULK_API_LIMIT")
F.gr5.rows << row8
def row9 = F.gr5.newRow()	
row9.instance_rec_num = 9
row9.api = "System Config Parameter"
row9.results = Emery.ctx.systemConfigurationParameter("DEFAULT_SERVANT")
F.gr5.rows << row9
def row10 = F.gr5.newRow()	
row10.instance_rec_num = 10
row10.api = "New Form Field2 Value"
row10.results = CONTEXT.get("Field2")
F.gr5.rows << row10
//F.description = "Running automation for EMERY API's using Autometric" // Enter this text from autometric sheet while filling the assignee field and read the value form the API and validate.
def row11 = F.gr5.newRow()	
row11.instance_rec_num = 10
row11.api = "Description"
row11.results = "Running automation for EMERY API's using Autometric"
F.gr5.rows << row11

def row12 = F.gr5.newRow()
row12.instance_rec_num = 12
row12.api = "Empty userName"
try{
	row12.results = Emery.util.getUserId("empty")
	}
catch(Exception e){
    // results = "Provided userName is empty, Please provide a valid userName "
	row12.results = e.getMessage()
	}
F.gr5.rows << row12

def row13 = F.gr5.newRow()
row13.instance_rec_num = 13
row13.api = "User name asNULL"
try{
	row13.results = Emery.util.getUserId(null)
	}
catch(Exception e){
	row13.results = e.getMessage()
    //results = "Provided userName is empty, Please provide a valid userName"
	}
F.gr5.rows << row13

// Workflow / Formflow
MultiRow gr5 = (MultiRow) F.getField("gr5");
AbstractRow newRow = (AbstractRow) gr5.newRow(); 
newRow.updateField("api","Current Stage");
newRow.updateField("results",CURRENT_STAGE);
newRow.updateField("unique_row_id",Emery.util.nextUniqueRowId())
gr5.addRow(newRow);
AbstractRow newRow1 = (AbstractRow) gr5.newRow(); 
newRow1.updateField("api","Destination Stage");
newRow1.updateField("results",TARGET_STAGE);
newRow1.updateField("unique_row_id",Emery.util.nextUniqueRowId())
gr5.addRow(newRow1);
AbstractRow newRow2 = (AbstractRow) gr5.newRow(); 
newRow2.updateField("api","Process Code");
newRow2.updateField("results",PROCESS_CODE);
newRow2.updateField("unique_row_id",Emery.util.nextUniqueRowId())
gr5.addRow(newRow2);
AbstractRow newRow3 = (AbstractRow) gr5.newRow(); 
newRow3.updateField("api","Transition Code");
newRow3.updateField("results",TRANSITION_CODE);
newRow3.updateField("unique_row_id",Emery.util.nextUniqueRowId())
gr5.addRow(newRow3);

// DataTable
def records = Emery.dataTable.read("MS_GRC_STATUS")
def targetRecord1 = records.find{row -> row.STATUS_CODE == 'MOD' && row.STATUS_NAME == "Update"} 
AbstractRow newRow4 = (AbstractRow) gr5.newRow(); 
newRow4.updateField("api","Status Name of the GRC DataTable");
newRow4.updateField("results",targetRecord1.STATUS_NAME);
newRow4.updateField("unique_row_id",Emery.util.nextUniqueRowId())
gr5.addRow(newRow4);

//String Utils
String encode1 = "M\"etricstream\", infotech, pvt, ltd"
AbstractRow newRow5 = (AbstractRow) gr5.newRow(); 
newRow5.updateField("api","String Encoded");
newRow5.updateField("results",Emery.stringUtil.displayEncode(encode1));
newRow5.updateField("unique_row_id",Emery.util.nextUniqueRowId())
gr5.addRow(newRow5);
String decode = "M\"\"etricstream\"\", infotech, pvt, ltd"
AbstractRow newRow6 = (AbstractRow) gr5.newRow(); 
newRow6.updateField("api","String Decoded");
newRow6.updateField("results",Emery.stringUtil.displayDecode (decode));
newRow6.updateField("unique_row_id",Emery.util.nextUniqueRowId())
gr5.addRow(newRow6);

/********Locales Brazos 2021 *******/

BiMap<Long, String> locales = Emery.util.fetchLocales(LocaleType.ALL)
String locale =  locales.get(Long.valueOf(1001))
Emery.log.error "All Locales {}",locale
AbstractRow newRow7 = (AbstractRow) gr5.newRow(); 
newRow7.updateField("api","Locale of the instance");
newRow7.updateField("results",locale);
newRow7.updateField("unique_row_id",Emery.util.nextUniqueRowId())
gr5.addRow(newRow7);
AbstractRow newRow8 = (AbstractRow) gr5.newRow(); 
BiMap localesEnabled = Emery.util.fetchLocales(LocaleType.ENABLED)
newRow8.updateField("api","Enabled locales size");
newRow8.updateField("results",localesEnabled.size());
newRow8.updateField("unique_row_id",Emery.util.nextUniqueRowId())
gr5.addRow(newRow8);

newRow9.updateField("api","New Row 9");
newRow9.updateField("results","New Row 9");
newRow9.updateField("unique_row_id",Emery.util.nextUniqueRowId())
gr5.addRow(newRow9);

F.save();

Emery.log.error "Autometric Framework"