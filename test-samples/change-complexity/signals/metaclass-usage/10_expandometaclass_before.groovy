class ApiClient {
    String baseUrl = "https://api.example.com"
    
    String get(String endpoint) {
        return "GET ${baseUrl}${endpoint}"
    }
    
    String post(String endpoint, Map data) {
        return "POST ${baseUrl}${endpoint} with ${data}"
    }
}

def client = new ApiClient()
println client.get("/users")
println client.post("/users", [name: "John"])
