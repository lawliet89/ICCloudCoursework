package org.bigdatapro.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;

public class TransferManager {
	
    public static final String SAMPLES_PROPERTIES_NAME = "s3credential.properties";
    public static final String S3_ACCESS_KEY_PROPERTY_NAME = "s3AccessKey";
    public static final String S3_SECRET_KEY_PROPERTY_NAME = "s3SecretKey";
    
    // Load S3 access and secret keys from the property file
    public static AWSCredentials loadKeys() throws IOException{
    	InputStream propertiesIS = ClassLoader.getSystemResourceAsStream(SAMPLES_PROPERTIES_NAME);
    	if(propertiesIS == null) {
    		throw new RuntimeException("property file is not existed");
    	}
    	
    	Properties properties = new Properties();
    	properties.load(propertiesIS);
    	
    	if(!properties.containsKey(S3_ACCESS_KEY_PROPERTY_NAME) || !properties.containsKey(S3_SECRET_KEY_PROPERTY_NAME)){
    		throw new RuntimeException("s3 keys are not in the file");
    		
    	}
    	
    	AWSCredentials s3credential = new AWSCredentials(properties.getProperty(S3_ACCESS_KEY_PROPERTY_NAME),properties.getProperty(S3_SECRET_KEY_PROPERTY_NAME));
		return s3credential;
    }
    
    // Make a connection to the S3 service
    public static RestS3Service connectToS3(AWSCredentials a){
    	RestS3Service s3Service = null;
        try {
			s3Service = new RestS3Service(a);
		} catch (S3ServiceException e) {
			e.printStackTrace();
		}
        return s3Service;
    }
    
    // BUCKETS, wrap the library functions
    public static S3Bucket[] listAllBuckets(RestS3Service s){
    	S3Bucket[] s3buckets= null;
    	try {
    		s3buckets = s.listAllBuckets();
    	} catch (S3ServiceException e) {
    		e.printStackTrace();
    	}
    	return s3buckets;
    }
    
    public static S3Bucket createBucket(RestS3Service s, String n){
    	S3Bucket s3bucket= null;
    	try {
    		s3bucket = s.createBucket(n);
    	} catch (S3ServiceException e) {
    		e.printStackTrace();
    	}
    	return s3bucket;
    }
    
	public static void deleteBucket(RestS3Service ss, S3Bucket sb) {
		try {
			ss.deleteBucket(sb.getName());
		} catch (ServiceException e) {
			e.printStackTrace();
		}
	}
	
	// OBJECTS, wrap the library functions
	public static S3Object[] listAllObjects(RestS3Service s3, String bucketName) {
		S3Object[] s3Re = null;
		try {
			s3Re = s3.listObjects(bucketName);
		} catch (S3ServiceException e) {
			e.printStackTrace();
		}
		return s3Re;
	}
    
    public static S3Object createObject(String key){
    	return new S3Object(key);
    }

	public static void deleteObject(RestS3Service ss, S3Bucket sb, String key) {
		try {
			ss.deleteObject(sb, key);
		} catch (S3ServiceException e) {
			e.printStackTrace();
		}
	}
}
