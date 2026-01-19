// Test Emery advanced query operations - MODIFIED
import com.metricstream.appstudio.emery.dataaccess.query.type.CountQuery
import com.metricstream.appstudio.emery.dataaccess.query.type.DeleteQuery
import com.metricstream.appstudio.emery.dataaccess.query.type.UpdateQuery
import com.metricstream.appstudio.emery.dataaccess.query.model.Condition
import com.metricstream.appstudio.emery.dataaccess.query.model.SortCondition
import com.metricstream.appstudio.emery.dataaccess.query.model.GroupCondition

// Count query with grouping
CountQuery countQuery = new CountQuery.Builder("USER_DATA")
    .addCondition(Condition.eq("STATUS", "INACTIVE"))
    .addCondition(Condition.isNotNull("EMAIL"))
    .addGroupCondition(GroupCondition.by("DEPARTMENT"))
    .build()

def inactiveCount = Emery.dataobject.count(countQuery)

// Delete query with additional conditions
DeleteQuery deleteQuery = new DeleteQuery.Builder("USER_DATA")
    .addCondition(Condition.eq("STATUS", "DELETED"))
    .addCondition(Condition.lt("LAST_LOGIN", "2022-01-01"))
    .addCondition(Condition.isNull("RECOVERY_EMAIL"))
    .build()

def deletedRows = Emery.dataobject.delete(deleteQuery)

// Update query with multiple fields
UpdateQuery updateQuery = new UpdateQuery.Builder("USER_DATA")
    .addCondition(Condition.eq("STATUS", "PENDING"))
    .addCondition(Condition.gt("CREATED_DATE", "2024-01-01"))
    .setValue("STATUS", "VERIFIED")
    .setValue("UPDATED_DATE", new Date())
    .setValue("VERIFIED_BY", "SYSTEM")
    .build()

def updatedRows = Emery.dataobject.update(updateQuery)

// Bulk insert operation
def userList = []
for (int i = 1; i <= 5; i++) {
    def user = Emery.dataobject.newInstance("USER_DATA")
    user.userName = "bulk_user_${i}"
    user.email = "bulk${i}@example.com"
    user.status = "ACTIVE"
    userList.add(user)
}

def insertResult = Emery.dataobject.insertAll(userList)
