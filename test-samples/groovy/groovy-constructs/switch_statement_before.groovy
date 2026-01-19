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


/* --------------------SWITCH BLOCK 2-------------------- */

// moved later
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


/* --------------------SWITCH BLOCK 3-------------------- */

// Moved + Modified later
switch (status) {
    case "NEW":
        println "New ticket"
        break
    default:
        println "Not new"
}


/* --------------------SWITCH BLOCK 4-------------------- */

// moved + modified later
switch (status) {
    case "ERROR":
        println "Error occurred"
        break
    case "FAILED":
        println "Failure occurred"
        break
    default:
        println "Check status"
}
