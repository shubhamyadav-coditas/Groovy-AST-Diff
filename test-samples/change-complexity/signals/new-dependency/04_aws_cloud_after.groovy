// Test Case 04: AWS Cloud Dependencies Added
// After: AWS S3 cloud storage

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.PutObjectRequest

class FileStorage {
    
    private String bucketName = "my-app-bucket"
    private AmazonS3 s3Client
    
    FileStorage() {
        s3Client = AmazonS3ClientBuilder.defaultClient()
    }
    
    def saveFile(String name, byte[] content) {
        def inputStream = new ByteArrayInputStream(content)
        s3Client.putObject(new PutObjectRequest(bucketName, name, inputStream, null))
        return "s3://${bucketName}/${name}"
    }
    
    def readFile(String name) {
        def s3Object = s3Client.getObject(bucketName, name)
        return s3Object.objectContent.bytes
    }
    
    def deleteFile(String name) {
        s3Client.deleteObject(bucketName, name)
        return true
    }
}

def storage = new FileStorage()
storage.saveFile("test.txt", "Hello World".bytes)
