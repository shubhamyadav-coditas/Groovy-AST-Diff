// Test Emery data table operations - MODIFIED
def countryData = Emery.dataTable.read("COUNTRY_DATA_TABLE")
def statusData = Emery.dataTable.read("STATUS_LOOKUP_TABLE")
def regionData = Emery.dataTable.read("REGION_DATA_TABLE")

for (def row : countryData) {
    println("Country: ${row.countryName}, Code: ${row.countryCode}, Region: ${row.regionCode}")
}

def activeStatuses = statusData.findAll { it.isActive == true && it.priority > 0 }
