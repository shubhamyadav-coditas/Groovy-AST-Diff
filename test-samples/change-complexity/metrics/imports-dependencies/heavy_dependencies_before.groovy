import com.company.model.User

class UserManager {
    
    User createUser(String name) {
        return new User(name: name)
    }
}