import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import com.company.model.User
import com.company.repository.UserRepository
import com.company.service.EmailService
import com.company.service.NotificationService
import com.company.dto.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Service
@Transactional
class UserManager {
    
    Logger log = LoggerFactory.getLogger(UserManager)
    
    @Autowired
    UserRepository userRepository
    
    @Autowired
    EmailService emailService
    
    @Autowired
    NotificationService notificationService
    
    User createUser(String name) {
        log.info("Creating user: $name")
        def user = new User(name: name)
        userRepository.save(user)
        emailService.sendWelcome(user)
        notificationService.notify(user, "Welcome!")
        return user
    }
    
    User findUser(Long id) {
        return userRepository.findById(id)
    }
    
    void deleteUser(Long id) {
        userRepository.deleteById(id)
        log.info("Deleted user: $id")
    }
}
