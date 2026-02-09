class Form {
    String title
    String description
    boolean isValid
    
    void validate() {
        isValid = title != null && !title.isEmpty()
    }
    
    Map toMap() {
        return [
            title: title,
            description: description,
            isValid: isValid
        ]
    }
}

def form = new Form()
form.title = "My Form"
form.description = "A form"
form.validate()
println form.toMap()
