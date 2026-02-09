// Test Case 10: Hibernate/Database Dependencies Added
// Before: Simple in-memory data store

class UserRepository {
    
    private Map<Long, Map> users = [:]
    private Long nextId = 1L
    
    def save(Map user) {
        user.id = nextId++
        users[user.id] = user
        return user
    }
    
    def findById(Long id) {
        return users[id]
    }
    
    def findAll() {
        return users.values().toList()
    }
    
    def delete(Long id) {
        return users.remove(id) != null
    }
}

def repo = new UserRepository()
repo.save([name: "John", email: "john@test.com"])
println(repo.findAll())
