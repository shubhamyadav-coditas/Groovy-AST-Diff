class CollectionProcessor {
    
    List processNumbers(List numbers) {
        return numbers
            .findAll { it > 0 }
            .collect { it * 2 }
    }
    
    def findActive(List items) {
        return items.find { it.active }
    }
    
    boolean hasPositive(List numbers) {
        return numbers.any { it > 0 }
    }
    
    boolean allPositive(List numbers) {
        return numbers.every { it > 0 }
    }
    
    Map groupItems(List items) {
        return items.groupBy { it.category }
    }
}