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

/**
 * Utility class providing convenience methods.
 * @author Lawliet
 *
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
    
    /**
     * Creates an AWSCredentials object based on the contents of an Input Property file. The fields should be named according to 
     * {@linkplain #S3_ACCESS_KEY_PROPERTY_NAME} and {@linkplain #S3_SECRET_KEY_PROPERTY_NAME}.
     * @param propertyFile File containing the properties necessary for creating an AWSCredentials object.
     * @return AWSCredentials object based on provided details.
     * @throws IOException
     */
	public static AWSCredentials getCredentials(InputStream propertyFile) throws IOException{
		Properties properties = new Properties();
		properties.load(propertyFile);
		if(!properties.containsKey(S3_ACCESS_KEY_PROPERTY_NAME) || !properties.containsKey(S3_SECRET_KEY_PROPERTY_NAME))
    		throw new IOException("S3 Keys are not properly defined in the properties file.");
		return new AWSCredentials(properties.getProperty(S3_ACCESS_KEY_PROPERTY_NAME),properties.getProperty(S3_SECRET_KEY_PROPERTY_NAME));	
	}
	
	/**
	 * Create a connection to the S3 service based on provided credentials. You can change the S3 endpoint settings in an InputStream file.
	 * 
	 * @param credentials Use {@linkplain #getCredentials(InputStream)} to create the object.
	 * @param s3Properties The settings to connect to S3.
	 * @return
	 * @throws S3ServiceException
	 * @throws IOException
	 */
	public static RestS3Service connect(AWSCredentials credentials, InputStream s3Properties) 
			throws S3ServiceException, IOException {
		Jets3tProperties property = new Jets3tProperties();
		property.loadAndReplaceProperties(s3Properties,null);
		
		return new RestS3Service(credentials, null, null, property);
	}
	
	/**
	 * Convenience method to provide an InputStream to an input file to create a <code>AWSCredentials</code> object automatically.<br /><br/>
	 * @param propertyFile
	 * @param s3Properties
	 * @return
	 * @throws IOException
	 * @throws S3ServiceException
	 * @see {@linkplain #connect(AWSCredentials, InputStream)}
	 */
	public static RestS3Service connect(InputStream propertyFile, InputStream s3Properties) throws IOException, S3ServiceException{
		return connect(getCredentials(propertyFile), s3Properties);
	}
	
	/** Output page based on template. Convenience function to provide only the body path. In this case, the body block has to be called Body
	 * 
	 * @param context
	 * @param title
	 * @param body
	 * @return
	 * @throws IOException
	 */
	public static String outputPage(HttpServlet context, String title, String body) throws IOException{
		Jtpl bodyTpl = createBodyTpl(context, body);
		bodyTpl.parse("Body");
		return outputPage(context, title, bodyTpl);
	}
	
	/**
	 * Output page based on template. Provide a parsed Body object.
	 * @param context
	 * @param title
	 * @param body
	 * @return
	 * @throws IOException
	 */
	public static String outputPage(HttpServlet context, String title, Jtpl body) throws IOException{
		Jtpl header = new Jtpl(context.getServletContext().getRealPath(TPL_HEADER));
		header.assign("pageTitle", title);
		header.parse("Header");
		Jtpl footer = new Jtpl(context.getServletContext().getRealPath(TPL_FOOTER));
		footer.parse("Footer");
		
		return outputPage(context, body, header,footer);
	}
	
	/**
	 * Output page based on template. Remember to parse all the objects.
	 * @param context
	 * @param body
	 * @param header
	 * @param footer
	 * @return
	 * @throws IOException
	 */
	public static String outputPage(HttpServlet context, Jtpl body, Jtpl header, Jtpl footer) throws IOException{
		StringBuilder output = new StringBuilder();
		output.append(header.out());
		output.append(body.out());
		output.append(footer.out());
		
		return output.toString();
	}
	
	/**
	 * Convenience method to create a JTpl object based on a relative path.
	 * @param context
	 * @param path Relative path
	 * @return
	 * @throws IOException
	 */
	public static Jtpl createBodyTpl(HttpServlet context, String path) throws IOException{
		return new Jtpl(context.getServletContext().getRealPath(path));
	}
	
	/**
	 * Return sh1 hash of string
	 * @param subject The string
	 * @return
	 */
	public static String sha1(String subject){
		return DigestUtils.sha1Hex(subject);
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
	
	/**
	 * Generate a sha1 hash from a combination of a provided salt plus the current time
	 * @param salt
	 * @return
	 */
	public static String generateRandomStringHash(String salt){
		StringBuilder str = new StringBuilder(salt);
		str.append(new Date().getTime());
		return sha1(str.toString());
	}
}
