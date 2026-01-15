String text = ""

void setText(String newText) {
    // MODIFIED: Added validation
    if (newText != null && !newText.isEmpty()) {
        text = newText.trim()
    }
}

String processText() {
    // MODIFIED: Changed logic completely
    return text.toUpperCase().toLowerCase()
}
