// Test Emery multi-row operations - MODIFIED
import com.metricstream.appstudio.dsl.engine.domain.MultiRow
import com.metricstream.appstudio.dsl.engine.domain.AbstractRow

use("EMPLOYEE_FORM")

def newRow1 = F.skillsMultiRow.newRow()
newRow1.skillName = "Python"
newRow1.experience = 3
newRow1.level = "Intermediate"
F.skillsMultiRow.rows << newRow1

def newRow2 = F.skillsMultiRow.newRow()
newRow2.skillName = "JavaScript"
newRow2.experience = 7
newRow2.level = "Expert"
F.skillsMultiRow.rows << newRow2

def region = dataobject.addressRegion
def addressRow = region.newRow()
addressRow.street = "456 Oak Ave"
addressRow.city = "San Francisco"
addressRow.state = "CA"
addressRow.zipCode = "94102"
region.addRow(addressRow)
