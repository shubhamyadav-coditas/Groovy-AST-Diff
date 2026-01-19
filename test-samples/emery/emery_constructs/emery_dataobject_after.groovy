// Test Emery data object operations - MODIFIED
import com.metricstream.appstudio.emery.dataaccess.domain.model.DataObject
import com.metricstream.appstudio.emery.dataaccess.query.type.SelectQuery
import com.metricstream.appstudio.emery.dataaccess.query.type.CountQuery
import com.metricstream.appstudio.emery.dataaccess.query.model.Condition
import com.metricstream.appstudio.emery.dataaccess.query.model.SortCondition

useDataObject("USER_DATA")

DataObject userObj = Emery.dataobject.newInstance("USER_DATA")
userObj.userName = "newuser"
userObj.email = "newuser@example.com"
userObj.department = "IT"
userObj.save()

SelectQuery query = new SelectQuery.Builder("USER_DATA")
    .addCondition(Condition.eq("STATUS", "ACTIVE"))
    .addCondition(Condition.like("EMAIL", "%@example.com"))
    .addSortCondition(SortCondition.asc("USER_NAME"))
    .limit(20)
    .build()

def results = Emery.dataobject.fetch(query)
def count = Emery.dataobject.count(new CountQuery.Builder("USER_DATA").build())
