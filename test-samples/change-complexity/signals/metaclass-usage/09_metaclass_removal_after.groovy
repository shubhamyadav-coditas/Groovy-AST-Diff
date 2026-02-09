class Formatter {
    String format(String text) {
        return text.trim()
    }
    
    String formatUpper(String text) {
        return text?.toUpperCase()?.trim()
    }
    
    String formatLower(String text) {
        return text?.toLowerCase()?.trim()
    }
}

def formatter = new Formatter()
println formatter.format("  Hello World  ")
println formatter.formatUpper("  Hello World  ")
println formatter.formatLower("  Hello World  ")
