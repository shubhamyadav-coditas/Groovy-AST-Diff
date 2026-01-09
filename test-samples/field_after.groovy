class Config {
    // MOVED: static field moved to top
    static final String VERSION = "2.0"  // MODIFIED: version changed
    
    // MODIFIED: field value changed
    private String database = "postgresql"
    
    // UNCHANGED: property stays same
    String environment = "dev"
    
    // ADDED: new field
    boolean debug = false
}
