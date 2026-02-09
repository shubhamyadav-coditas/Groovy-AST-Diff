// Test Case 10: Hibernate/Database Dependencies Added
// After: Hibernate ORM for database persistence

import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.cfg.Configuration
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.GeneratedValue

@Entity
class User {
    @Id
    @GeneratedValue
    Long id
    String name
    String email
}

class UserRepository {
    
    private SessionFactory sessionFactory
    
    UserRepository() {
        sessionFactory = new Configuration()
            .configure("hibernate.cfg.xml")
            .addAnnotatedClass(User)
            .buildSessionFactory()
    }
    
    def save(User user) {
        Session session = sessionFactory.openSession()
        session.beginTransaction()
        session.save(user)
        session.getTransaction().commit()
        session.close()
        return user
    }
    
    def findById(Long id) {
        Session session = sessionFactory.openSession()
        def user = session.get(User, id)
        session.close()
        return user
    }
    
    def findAll() {
        Session session = sessionFactory.openSession()
        def users = session.createQuery("from User").list()
        session.close()
        return users
    }
    
    def delete(Long id) {
        Session session = sessionFactory.openSession()
        session.beginTransaction()
        def user = session.get(User, id)
        if (user) session.delete(user)
        session.getTransaction().commit()
        session.close()
        return user != null
    }
}
