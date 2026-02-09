class DeepNestingExample {
    
    void processData(List data) {
        data.each { level1 ->
            level1.children.each { level2 ->
                level2.items.each { level3 ->
                    level3.values.each { level4 ->
                        println level4
                    }
                }
            }
        }
    }
}