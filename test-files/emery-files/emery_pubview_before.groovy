// Test Emery public view operations
import com.metricstream.appstudio.emery.dataaccess.query.type.SelectQuery
import com.metricstream.appstudio.emery.dataaccess.query.type.CountQuery
import com.metricstream.appstudio.emery.dataaccess.query.model.Condition
import com.metricstream.appstudio.emery.dataaccess.query.model.SortCondition

SelectQuery pubviewQuery = new SelectQuery.Builder("USER_PUBLIC_VIEW")
    .addCondition(Condition.eq("STATUS", "ACTIVE"))
    .addSortCondition(SortCondition.asc("CREATED_DATE"))
    .limit(50)
    .build()

def pubviewResults = Emery.pubview.fetch(pubviewQuery)

CountQuery countQuery = new CountQuery.Builder("USER_PUBLIC_VIEW")
    .addCondition(Condition.eq("STATUS", "ACTIVE"))
    .build()

def activeUserCount = Emery.pubview.count(countQuery)
