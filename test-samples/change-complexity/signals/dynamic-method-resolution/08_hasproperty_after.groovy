class Form {
    String title
    String description
    boolean isValid
    
    void validate() {
        isValid = title != null && !title.isEmpty()
    }
    
    Map toMap() {
        def result = [:]
        ['title', 'description', 'isValid', 'createdAt', 'updatedAt'].each { propName ->
            if (this.hasProperty(propName)) {
                result[propName] = this."${propName}"
            }
        }
        return result
    }
    
    void copyFrom(Object source, List<String> properties) {
        properties.each { prop ->
            if (source.hasProperty(prop) && this.hasProperty(prop)) {
                this."${prop}" = source."${prop}"
            }
        }
    }
}

def form = new Form()
form.title = "My Form"
form.description = "A form"
form.validate()
println form.toMap()
