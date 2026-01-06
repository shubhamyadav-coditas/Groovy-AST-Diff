int value = 0

void add(int number) {
    value += number
}

// NEW FUNCTION - will be detected as ADDED
void subtract(int number) {
    value -= number
}

// NEW FUNCTION - will be detected as ADDED  
int getValue() {
    return value
}
