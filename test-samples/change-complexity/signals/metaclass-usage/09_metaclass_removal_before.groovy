class Formatter {
    String format(String text) {
        return text.trim()
    }
}

Formatter.metaClass.formatUpper = { String text ->
    text?.toUpperCase()?.trim()
}

Formatter.metaClass.formatLower = { String text ->
    text?.toLowerCase()?.trim()
}

def formatter = new Formatter()
println formatter.format("  Hello World  ")
println formatter.formatUpper("  Hello World  ")
println formatter.formatLower("  Hello World  ")
