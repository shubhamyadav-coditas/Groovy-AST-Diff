// Test Emery public view operations - MODIFIED
import com.metricstream.appstudio.emery.dataaccess.query.type.SelectQuery
import com.metricstream.appstudio.emery.dataaccess.query.type.CountQuery
import com.metricstream.appstudio.emery.dataaccess.query.model.Condition
import com.metricstream.appstudio.emery.dataaccess.query.model.SortCondition
import com.metricstream.appstudio.emery.dataaccess.query.model.GroupCondition

SelectQuery pubviewQuery = new SelectQuery.Builder("USER_PUBLIC_VIEW")
    .addCondition(Condition.eq("STATUS", "ACTIVE"))
    .addCondition(Condition.gt("LAST_LOGIN", "2024-01-01"))
    .addSortCondition(SortCondition.desc("LAST_LOGIN"))
    .addGroupCondition(GroupCondition.by("DEPARTMENT"))
    .limit(100)
    .build()

def pubviewResults = Emery.pubview.fetch(pubviewQuery)

CountQuery countQuery = new CountQuery.Builder("USER_PUBLIC_VIEW")
    .addCondition(Condition.eq("STATUS", "ACTIVE"))
    .addCondition(Condition.isNotNull("EMAIL"))
    .build()

def activeUserCount = Emery.pubview.count(countQuery)

// Additional query for departments
SelectQuery deptQuery = new SelectQuery.Builder("DEPARTMENT_PUBLIC_VIEW")
    .addSortCondition(SortCondition.asc("DEPT_NAME"))
    .build()

def departments = Emery.pubview.fetch(deptQuery)
