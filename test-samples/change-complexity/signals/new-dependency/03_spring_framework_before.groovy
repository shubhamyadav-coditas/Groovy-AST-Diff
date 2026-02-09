// Test Case 03: Spring Framework Dependencies Added
// Before: Plain Groovy service class

class UserService {
    
    private List<Map> users = []
    
    def addUser(String name, String email) {
        users.add([name: name, email: email])
    }
    
    def getUser(String name) {
        return users.find { it.name == name }
    }
    
    def getAllUsers() {
        return users
    }
}

def service = new UserService()
service.addUser("John", "john@example.com")
println(service.getAllUsers())
