// Test: Logical operators - before
// Expected CC: 2 (base + 1 if)

class AccessChecker {
    boolean hasAccess(User user) {
        if (user != null) {
            return true
        }
        return false
    }
}
