// NewForm after modification
use("MS_ATD_EMERY_NEW_FORM")
MS_ATD_EMERY_NEW_FORM formObj = Emery.form.newForm("MS_ATD_EMERY_NEW_FORM")


formObj.field2 = "Test new form2"
formObj.field1 = "SYSTEMI2"

formObj.process_instance_id = Emery.util.nextProcessInstanceId()
formObj.instance_id = Emery.util.nextInstanceId()
formObj.dd_object_type = "MS_ATD_EMERY_NEW_FORM"
formObj.dd_process_code = "Emery_wf_new"
formObj.process_flow_status = 2
formObj.dd_current_user_name = "SYSTEMI"
formObj.latest_flag = "N"


Emery.log.error "Process instance id{}", formObj.process_instance_id
CONTEXT.put("Field2",formObj.field2)


formObj.save();
formObj.submit();