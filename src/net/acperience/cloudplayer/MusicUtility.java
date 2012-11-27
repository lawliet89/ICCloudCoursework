package net.acperience.cloudplayer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.sourceforge.jtpl.Jtpl;

import org.apache.commons.codec.digest.DigestUtils;
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
    
    public static final int SECURE_PORT = 48953;
	public static final String CREDENTIALS_PATH = "/WEB-INF/properties/s3credential.properties";
	public static final String JETS3_PATH = "/WEB-INF/properties/jets3t.properties";
    
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
	public static RestS3Service connect(AWSCredentials credentials, InputStream s3Properties) 
			throws S3ServiceException, IOException {
		Jets3tProperties property = new Jets3tProperties();
		property.loadAndReplaceProperties(s3Properties,null);
		
		return new RestS3Service(credentials, null, null, property);
	}
	
	public static RestS3Service connect(InputStream propertyFile, InputStream s3Properties) throws IOException, S3ServiceException{
		return connect(getCredentials(propertyFile), s3Properties);
	}
	
	// Output page based on template
	
	// Convenience function to provide only the body path
	// In this case, the body block has to be called Body
	public static String outputPage(HttpServlet context, String title, String body) throws IOException{
		Jtpl bodyTpl = createBodyTpl(context, body);
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
	public static Jtpl createBodyTpl(HttpServlet context, String path) throws IOException{
		return new Jtpl(context.getServletContext().getRealPath(path));
	}
	
	/**
	 * Return sh1 hash of string
	 * @param subject The string
	 * @return
	 */
	public static String sha1(String subject){
		return DigestUtils.shaHex(subject);
	}
	
	/**
	 * Generate a new nonce hash and store it in the user's session
	 * @param request
	 * @param user
	 * @return the Nonce generated
	 */
	public static String generateNonce(HttpServletRequest request, MusicKerberos user){
		HttpSession session = request.getSession();
		StringBuilder str = new StringBuilder(user.getUserIdHash());
		str.append(new Date().getTime());
		String nonce = sha1(str.toString());
		
		session.setAttribute("nonce", nonce);
		
		return nonce;
	}
	
	/**
	 * Consume the nonce and return whether it was valid or not.
	 * @param request
	 * @return Whether the nonce was valid or not
	 */
	
	public static boolean consumeNonce(HttpServletRequest request, String nonce){
		HttpSession session = request.getSession();
		String stored = (String) session.getAttribute("nonce");
		if (stored == null)
			return false;
		
		if (!stored.equals(nonce))
			return false;
		
		session.removeAttribute("nonce");
		return true;
	}
}
