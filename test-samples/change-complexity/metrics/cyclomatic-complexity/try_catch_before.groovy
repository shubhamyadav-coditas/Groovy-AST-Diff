// Test: Try-catch - before
// Expected CC: 1 (base only)

class FileReader {
    String readFile(String path) {
        def file = new File(path)
        return file.text
    }
}
