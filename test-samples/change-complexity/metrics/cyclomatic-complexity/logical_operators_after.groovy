// Test: Logical operators - after (multiple && and ||)
// Expected CC: 6 (base + 1 if + 2 && + 2 ||)

class AccessChecker {
    boolean hasAccess(User user, String resource) {
        if (user != null && user.isActive() && (user.isAdmin() || user.hasPermission(resource) || user.isOwner(resource))) {
            return true
        }
        return false
    }
}
