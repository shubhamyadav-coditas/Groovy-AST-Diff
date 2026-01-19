def status = "OPEN"

/* --------------------SWITCH BLOCK 1-------------------- */

// unchanged
switch (status) {
    case "OPEN":
        println "Ticket is open"
        break
    case "CLOSED":
        println "Ticket is closed"
        break
    default:
        println "Unknown status"
}


/* --------------------SWITCH BLOCK 3-------------------- */

// Moved + Modified later
switch (status) {
    case "RESOLVED":
        println "Ticket resolved"
        break
    default:
        println "Resolution unknown"
}


/* --------------------SWITCH BLOCK 4-------------------- */

switch (status) {
    case "ERROR":
        println "Error detected"
        break
    case "FAILED":
        println "Failure detected"
        break
    default:
        println "Investigate status"
}


/* --------------------SWITCH BLOCK 2-------------------- */

switch (status) {
    case "IN_PROGRESS":
        println "Work in progress"
        break
    case "ON_HOLD":
        println "On hold"
        break
    default:
        println "Other"
}
