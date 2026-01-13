def score = 75

/* --------------------IF–ELSEIF–ELSE BLOCK 1-------------------- */

// unchanged
if (score >= 90) {
    println "Excellent"
}
else if (score >= 75) {
    println "Good"
}
else {
    println "Needs Improvement"
}


/* --------------------IF–ELSEIF–ELSE BLOCK 2-------------------- */

// moved later
if (score < 40) {
    println "Fail"
}
else if (score < 60) {
    println "Pass"
}
else {
    println "Merit"
}


/* --------------------IF–ELSEIF–ELSE BLOCK 3-------------------- */

// deleted later
if (score == 0) {
    println "No Score"
}
else {
    println "Has Score"
}


/* --------------------IF–ELSEIF–ELSE BLOCK 4-------------------- */

// moved + modified later
if (score > 100) {
    println "Invalid"
}
else if (score < 0) {
    println "Invalid"
}
else {
    println "Valid"
}
