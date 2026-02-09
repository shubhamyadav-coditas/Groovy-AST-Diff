class Person {
    String firstName
    String lastName
    
    String getFullName() {
        return "${firstName} ${lastName}"
    }
}

def person = new Person(firstName: "John", lastName: "Doe")
println person.getFullName()
