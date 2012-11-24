package net.acperience.cloudplayer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.http.HttpServlet;

import net.sourceforge.jtpl.Jtpl;

import org.jets3t.service.*;
import org.jets3t.service.impl.rest.httpclient.*;
import org.jets3t.service.security.AWSCredentials;

/*
 * Utility classes
 */
public final class MusicUtility {
    public static final String S3_ACCESS_KEY_PROPERTY_NAME = "s3AccessKey";
    public static final String S3_SECRET_KEY_PROPERTY_NAME = "s3SecretKey";
    
    public static final String TPL_HEADER = "/WEB-INF/template/header.tpl";
    public static final String TPL_FOOTER = "/WEB-INF/template/footer.tpl";
    public static final String TPL_DIR = "/WEB-INF/template/";
    
    public static final int SECURE_PORT = 60000;
    
    private MusicUtility(){
    	// Cannot be instantiated.
    }
    
    // Create a new credentials object based on values from the property file
	public static AWSCredentials getCredentials(InputStream propertyFile) throws IOException{
		Properties properties = new Properties();
		properties.load(propertyFile);
		if(!properties.containsKey(S3_ACCESS_KEY_PROPERTY_NAME) || !properties.containsKey(S3_SECRET_KEY_PROPERTY_NAME))
    		throw new IOException("S3 Keys are not properly defined in the properties file.");
		return new AWSCredentials(properties.getProperty(S3_ACCESS_KEY_PROPERTY_NAME),properties.getProperty(S3_SECRET_KEY_PROPERTY_NAME));	
	}
	
	// Connect to S3. Returns RestS3Service object
	public static RestS3Service connect(AWSCredentials credentials) throws S3ServiceException{
		return new RestS3Service(credentials);
	}
	
	public static RestS3Service connect(InputStream propertyFile) throws IOException, S3ServiceException{
		return connect(getCredentials(propertyFile));
	}
	
	// Output page based on template
	
	// Convenience function to provide only the body path
	// In this case, the body block has to be called Body
	public static String outputPage(HttpServlet context, String title, String body) throws IOException{
		Jtpl bodyTpl = bodyTpl(context, body);
		bodyTpl.parse("Body");
		return outputPage(context, title, bodyTpl);
	}
	
	// Provide the parsed body
	public static String outputPage(HttpServlet context, String title, Jtpl body) throws IOException{
		Jtpl header = new Jtpl(context.getServletContext().getRealPath(TPL_HEADER));
		header.assign("pageTitle", title);
		header.parse("Header");
		Jtpl footer = new Jtpl(context.getServletContext().getRealPath(TPL_FOOTER));
		footer.parse("Footer");
		
		return outputPage(context, body, header,footer);
	}
	
	// Everything under control
	public static String outputPage(HttpServlet context, Jtpl body, Jtpl header, Jtpl footer) throws IOException{
		StringBuilder output = new StringBuilder();
		output.append(header.out());
		output.append(body.out());
		output.append(footer.out());
		
		return output.toString();
	}
	
	// Get Body TPL object
	public static Jtpl bodyTpl(HttpServlet context, String path) throws IOException{
		return new Jtpl(context.getServletContext().getRealPath(path));
	}
}
