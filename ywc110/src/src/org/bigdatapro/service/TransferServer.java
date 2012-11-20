package org.bigdatapro.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;

public class TransferServer {
	// UPLOAD, wrap the library functions
	public static S3Object uploadObject(S3Object s3object, S3Bucket s3bucket, RestS3Service s3service){
		S3Object s3objectRes = null;
		try {
			s3objectRes = s3service.putObject(s3bucket, s3object);
		} catch (S3ServiceException e) {
			e.printStackTrace();
		}
		return s3objectRes;
	}
	
	public static S3Object uploadFile(File file, S3Bucket s3bucket, RestS3Service s3service) throws Exception{
		S3Object s3objectRes = null;
		try {
			s3objectRes = s3service.putObject(s3bucket, new S3Object(file));
		} catch (S3ServiceException e) {
			e.printStackTrace();
		}
		return s3objectRes;
	}
	
	// DOWNLOAD, wrap the library functions
	public static S3Object downloadObject(RestS3Service ss, String testBucknetName, String testObjectName) {
		S3Object s3objectRes = null;
		try {
			s3objectRes = ss.getObject(testBucknetName, testObjectName);
		} catch (S3ServiceException e) {
			e.printStackTrace();
		}
		return s3objectRes;
	}
	
	public static void downloadFile(RestS3Service ss, String bucketName, String objectName, File outputFile, byte[] bytes) throws IOException, ServiceException {
		// Download an object from its name
		S3Object downloadedObjectNew = TransferServer.downloadObject(ss, bucketName, objectName);
        OutputStream output = new FileOutputStream(outputFile);
        InputStream input = downloadedObjectNew.getDataInputStream();
        
        // Write into a local file
    	int read = 0;
    	while ((read = input.read(bytes)) != -1) {
    		output.write(bytes, 0, read);
    	}
        input.close();
        output.flush();
        output.close();
	}
}
