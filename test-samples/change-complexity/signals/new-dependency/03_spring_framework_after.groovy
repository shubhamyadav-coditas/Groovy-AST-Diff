// Test Case 03: Spring Framework Dependencies Added
// After: Added Spring framework imports for DI and web

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@Service
class UserService {
    
    @Autowired
    private UserRepository repository
    
    def addUser(String name, String email) {
        repository.save([name: name, email: email])
    }
    
    def getUser(String name) {
        return repository.findByName(name)
    }
    
    def getAllUsers() {
        return repository.findAll()
    }
}

@RestController
class UserController {
    
    @Autowired
    UserService userService
    
    def listUsers() {
        return userService.getAllUsers()
    }
}
