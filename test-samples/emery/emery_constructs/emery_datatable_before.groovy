// Test Emery data table operations
def countryData = Emery.dataTable.read("COUNTRY_DATA_TABLE")
def statusData = Emery.dataTable.read("STATUS_LOOKUP_TABLE")

for (row in countryData) {
    println("Country: ${row.countryName}, Code: ${row.countryCode}")
}

def activeStatuses = statusData.findAll { it.isActive == true }
