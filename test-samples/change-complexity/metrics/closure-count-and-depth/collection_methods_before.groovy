class CollectionProcessor {
    
    List processNumbers(List numbers) {
        List result = []
        for (num in numbers) {
            if (num > 0) {
                result.add(num * 2)
            }
        }
        return result
    }
}