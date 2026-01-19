// Test Emery data object operations
import com.metricstream.appstudio.emery.dataaccess.domain.model.DataObject
import com.metricstream.appstudio.emery.dataaccess.query.type.SelectQuery
import com.metricstream.appstudio.emery.dataaccess.query.model.Condition

useDataObject("USER_DATA")

DataObject userObj = Emery.dataobject.newInstance("USER_DATA")
userObj.userName = "testuser"
userObj.email = "test@example.com"
userObj.save()

SelectQuery query = new SelectQuery.Builder("USER_DATA")
    .addCondition(Condition.eq("STATUS", "ACTIVE"))
    .limit(10)
    .build()

def results = Emery.dataobject.fetch(query)
