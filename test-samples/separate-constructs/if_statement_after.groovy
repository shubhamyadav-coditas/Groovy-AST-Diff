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


/* --------------------IF–ELSEIF–ELSE BLOCK 3-------------------- */

// added
if (score == 100) {
    println "Perfect Score"
}


/* --------------------IF–ELSEIF–ELSE BLOCK 4-------------------- */

if (score > 100) {
    println "Invalid Score"
}
else if (score < 0) {
    println "Invalid Score"
}
else {
    println "Valid Score"
}


/* --------------------IF–ELSEIF–ELSE BLOCK 2-------------------- */

if (score < 40) {
    println "Fail"
}
else if (score < 60) {
    println "Pass"
}
else {
    println "Merit"
}
