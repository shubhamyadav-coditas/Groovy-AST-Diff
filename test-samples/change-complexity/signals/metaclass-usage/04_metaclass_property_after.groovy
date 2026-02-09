class Person {
    String firstName
    String lastName
    
    String getFullName() {
        return "${firstName} ${lastName}"
    }
}

Person.metaClass.age = 0

Person.metaClass.getAgeDescription = {
    "Age: ${delegate.age}"
}

def person = new Person(firstName: "John", lastName: "Doe")
person.age = 30
println person.getFullName()
println person.getAgeDescription()
