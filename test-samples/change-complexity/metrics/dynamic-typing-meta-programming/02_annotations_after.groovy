// Test: Annotations - after
// Multiple annotations on class and methods

@Service
@Transactional
class UserService {
    
    @Autowired
    String repository
    
    @Override
    @Cacheable("users")
    String getName(String id) {
        return repository.findById(id)?.name ?: "Unknown"
    }
    
    @Transactional(rollbackFor = Exception.class)
    @Deprecated
    void updateUser(String id, String name) {
        // Update logic
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    void deleteUser(String id) {
        // Delete logic
    }
}
