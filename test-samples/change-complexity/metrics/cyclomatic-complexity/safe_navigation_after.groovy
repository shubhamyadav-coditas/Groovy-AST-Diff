// Test: Safe navigation - after (using ?. operator)
// Expected CC: 5 (base + 2 safe navigation operators + 2 spread safe navigation operators)

class UserService {
    String getUserCity(User user) {
        // Safe navigation operators
        return user?.address?.city
    }
    
    List<String> getUserAddresses(List<User> users) {
        // Spread safe navigation
        return users*?.address*?.city
    }
}
