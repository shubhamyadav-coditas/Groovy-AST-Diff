import groovy.lang.ExpandoMetaClass

class ApiClient {
    String baseUrl = "https://api.example.com"
    
    String get(String endpoint) {
        return "GET ${baseUrl}${endpoint}"
    }
    
    String post(String endpoint, Map data) {
        return "POST ${baseUrl}${endpoint} with ${data}"
    }
}

def emc = new ExpandoMetaClass(ApiClient)
emc.put = { String endpoint, Map data ->
    "PUT ${delegate.baseUrl}${endpoint} with ${data}"
}
emc.delete = { String endpoint ->
    "DELETE ${delegate.baseUrl}${endpoint}"
}
emc.initialize()

ApiClient.metaClass = emc

def client = new ApiClient()
println client.get("/users")
println client.post("/users", [name: "John"])
println client.put("/users/1", [name: "Jane"])
println client.delete("/users/1")
