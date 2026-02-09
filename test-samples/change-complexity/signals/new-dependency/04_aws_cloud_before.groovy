// Test Case 04: AWS Cloud Dependencies Added
// Before: Local file storage

class FileStorage {
    
    private String baseDir = "/tmp/storage"
    
    def saveFile(String name, byte[] content) {
        def file = new File("${baseDir}/${name}")
        file.bytes = content
        return file.absolutePath
    }
    
    def readFile(String name) {
        def file = new File("${baseDir}/${name}")
        return file.bytes
    }
    
    def deleteFile(String name) {
        def file = new File("${baseDir}/${name}")
        return file.delete()
    }
}

def storage = new FileStorage()
storage.saveFile("test.txt", "Hello World".bytes)
