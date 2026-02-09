// Test: Try-catch - after (multiple catch clauses)
// Expected CC: 4 (base + 3 catch clauses)

class FileReader {
    String readFile(String path) {
        try {
            def file = new File(path)
            return file.text
        } catch (FileNotFoundException e) {
            return "File not found: ${path}"
        } catch (IOException e) {
            return "IO error: ${e.message}"
        } catch (Exception e) {
            return "Unknown error: ${e.message}"
        }
    }
}
