// Test Emery advanced query operations
import com.metricstream.appstudio.emery.dataaccess.query.type.CountQuery
import com.metricstream.appstudio.emery.dataaccess.query.type.DeleteQuery
import com.metricstream.appstudio.emery.dataaccess.query.type.UpdateQuery
import com.metricstream.appstudio.emery.dataaccess.query.model.Condition
import com.metricstream.appstudio.emery.dataaccess.query.model.SortCondition
import com.metricstream.appstudio.emery.dataaccess.query.model.GroupCondition

// Count query
CountQuery countQuery = new CountQuery.Builder("USER_DATA")
    .addCondition(Condition.eq("STATUS", "INACTIVE"))
    .build()

def inactiveCount = Emery.dataobject.count(countQuery)

// Delete query
DeleteQuery deleteQuery = new DeleteQuery.Builder("USER_DATA")
    .addCondition(Condition.eq("STATUS", "DELETED"))
    .addCondition(Condition.lt("LAST_LOGIN", "2023-01-01"))
    .build()

def deletedRows = Emery.dataobject.delete(deleteQuery)

// Update query
UpdateQuery updateQuery = new UpdateQuery.Builder("USER_DATA")
    .addCondition(Condition.eq("STATUS", "PENDING"))
    .setValue("STATUS", "ACTIVE")
    .setValue("UPDATED_DATE", new Date())
    .build()

def updatedRows = Emery.dataobject.update(updateQuery)
