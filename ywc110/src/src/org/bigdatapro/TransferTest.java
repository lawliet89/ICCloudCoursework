package org.bigdatapro;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.UUID;

import org.bigdatapro.service.TransferManager;
import org.bigdatapro.service.TransferServer;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;

public class TransferTest {
	private static final String TEST_BUCKNET_NAME = "test-bucket-" + UUID.randomUUID();	// bucket name has to be unique
	private static final String TEST_OBJECT_NAME = "test-object";
	
	public static void main(String[] args) throws Exception{
        // Your S3 credentials are required to manage S3 accounts.
        // These credentials are stored in an AWSCredentials object:
        AWSCredentials s3Credentials = TransferManager.loadKeys();
        
        // To communicate with S3 use the RestS3Service.
        RestS3Service s3Service = TransferManager.connectToS3(s3Credentials);
        
        // A good test to see if your S3Service can connect to S3 is to list all the buckets you own.
        // If a bucket listing produces no exceptions, all is well.
        S3Bucket[] myBuckets = TransferManager.listAllBuckets(s3Service);
        System.out.println("1. How many buckets do I have in S3? " + myBuckets.length);
        // System.out.println("The name of my first bucket? " + myBuckets[0].getName());
        // System.out.println("The name of my second bucket? " + myBuckets[1].getName());
        
        // To store data in S3 you must first create a bucket, a container for objects.
        S3Bucket testBucket = TransferManager.createBucket(s3Service, TEST_BUCKNET_NAME);
        System.out.println("2. Created test bucket: " + testBucket.getName());
        
        // Create an empty object with a key/name, and print the object's details.
        S3Object object = TransferManager.createObject(TEST_OBJECT_NAME);
        System.out.println("3. Created empty object, the object before upload: " + object);
        
        // Upload the object to our test bucket in S3.
        // Print the details about the uploaded object, which contains more information.
        object = TransferServer.uploadObject(object, testBucket, s3Service);
        System.out.println("4. Uploaded the object, the object after upload: " + object);
        
        // Create an S3Object based on a file, with Content-Length set automatically and
        // Content-Type set based on the file's extension (using the Mimetypes utility class)
        File file = new File("src/file/sample.txt");
        S3Object fileObject = TransferServer.uploadFile(file, testBucket, s3Service);
        System.out.println("5.1 Uploaded the text file: " + fileObject.getName());
        
        file = new File("src/file/sample.mp3");
        S3Object soundObject = TransferServer.uploadFile(file, testBucket, s3Service);
        System.out.println("5.2 Uploaded the mp3 file " + soundObject.getName());
        
        /*
         * Create an S3Object that is attached with an input file
         * 
		soundObject = TransferManager.createObject("new-name.mp3");
		soundObject.setDataInputFile(file);
		soundObject.setContentLength(file.length());
		TransferServer.uploadObject(soundObject, s3bucket, s3service);
		*/
        
        // List all objects in the test bucket
        S3Object[] s3Objects = TransferManager.listAllObjects(s3Service, TEST_BUCKNET_NAME);
        for(int i = 0; i < s3Objects.length; i++){
        	int j = i + 1;
        	System.out.println("6." + j + " Listed objects in test bucket: " + s3Objects[i].getName());
        }
        
        // To download data from S3 you retrieve an S3Object through the S3Service.
        // You may retrieve an object in one of two ways, with the data contents or without.
        S3Object downloadedObject = TransferServer.downloadObject(s3Service, TEST_BUCKNET_NAME, fileObject.getName());
        System.out.println("7.1 Downloaded the text object from bucket: " + downloadedObject.getName());
        
        // Read the data from the object's DataInputStream using a loop, and print it out.
        BufferedReader reader = new BufferedReader(new InputStreamReader(downloadedObject.getDataInputStream()));
        String data = null;
        System.out.print("7.2 Downloaded content is: ");
        while ((data = reader.readLine()) != null) {
            System.out.println(data);
        }
        
        // Download the object's data to a local file
        String fileName = soundObject.getName();
        File downloadedFile = new File("src/" + fileName);	
        byte[] bytes = new byte[1024];		// buffer size
        TransferServer.downloadFile(s3Service, TEST_BUCKNET_NAME, fileName, downloadedFile, bytes);
        System.out.println("7.3 Downloaded the mp3 file to " + downloadedFile.getPath());
        
        // Delete all the objects in the bucket
        TransferManager.deleteObject(s3Service, testBucket, object.getKey());
        TransferManager.deleteObject(s3Service, testBucket, fileObject.getKey());
        TransferManager.deleteObject(s3Service, testBucket, soundObject.getKey());
        System.out.println("8. Deleted all objects in test bucket");
        
        // Now that the bucket is empty, you can delete it.
        TransferManager.deleteBucket(s3Service, testBucket);
        System.out.println("9. Deleted the bucket: " + testBucket.getName());
	}
}
