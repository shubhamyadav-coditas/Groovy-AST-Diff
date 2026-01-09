// Test Emery multi-row operations
import com.metricstream.appstudio.dsl.engine.domain.MultiRow
import com.metricstream.appstudio.dsl.engine.domain.AbstractRow

use("EMPLOYEE_FORM")

def newRow = F.skillsMultiRow.newRow()
newRow.skillName = "Java"
newRow.experience = 5
F.skillsMultiRow.rows << newRow

def region = dataobject.addressRegion
def addressRow = region.newRow()
addressRow.street = "123 Main St"
addressRow.city = "New York"
region.addRow(addressRow)
