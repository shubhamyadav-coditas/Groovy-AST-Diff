// Test: Switch statement - after (switch with cases)
// Expected CC: 5 (base + 4 cases)

class StatusHandler {
    String getStatus(int code) {
        switch (code) {
            case 200:
                return "OK"
            case 404:
                return "Not Found"
            case 500:
                return "Server Error"
            default:
                return "Unknown"
        }
    }
}
