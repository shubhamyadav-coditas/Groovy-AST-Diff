// Test: Safe navigation - before
// Expected CC: 4 (base + 3 if statements for null checks)

class UserService {
    String getUserCity(User user) {
        if (user == null) {
            return null
        }
        if (user.address == null) {
            return null
        }
        if (user.address.city == null) {
            return null
        }
        return user.address.city
    }
}
