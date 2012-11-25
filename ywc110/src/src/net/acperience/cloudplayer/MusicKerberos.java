package net.acperience.cloudplayer;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.model.S3Bucket;

/**
 * 
 * Kerberos Authentication implementation using a serialised version of the Subject as the persistent storage
 * Used in a servlet environment. 
 * One instance of the class is instantiated with each HTTP request and should be destroyed upon the completion
 * of that HTTP request.
 * 
 * Otherwise, you might get LEAKS on two fronts: the HttpServletRequest object and the MusicKerberos object
 * 
 * This also encapsulates the user related operations in this application
 * 
 * @author Lawliet
 *
 */
public class MusicKerberos extends KerberosAuth {
	
	private static final String PERSISTENT_NAME = "MusicKerberosSubject";
	private static final String FORM_USERID_NAME = "userId";
	private static final String FORM_PASSWORD_NAME = "password";
	private static final String KERBEROS_REALM = "IC.AC.UK";
	
	private static final String AUTH_BASE = "/WEB-INF/conf/";
	private static final String LOGIN_CONF = AUTH_BASE + "jaas.conf";
	private static final String KRB5_CONF = AUTH_BASE + "krb5.conf";
	private static final String AUTH_ATTRIBUTE = "CloudMusicAuth";
	
	
	private HttpSession session;
	private HttpServletRequest request;
	private HttpServletResponse response;	// Debugging purposes
	private S3Bucket bucket;
	private S3Service s3Service;
	
	/**
	 * Set S3Service
	 * @param s3Service the s3Service to set
	 */
	public void setS3Service(S3Service s3Service) {
		this.s3Service = s3Service;
	}

	public MusicKerberos(String name, String authLoginConfig, String krb5Config, HttpServletRequest request)
			throws LoginException, SecurityException {
		// Call super to initialise
		super(name, authLoginConfig, krb5Config);
		this.request = request;
		this.session = request.getSession();
	}

	/* (non-Javadoc)
	 * @see net.acperience.cloudplayer.KerberosAuth#getPersistentSubject()
	 */
	@Override
	public Subject getPersistentSubject() {
		// See if we can get a subject from the HttpSession
		Object persistent = session.getAttribute(PERSISTENT_NAME);
		if (persistent == null)
			return null;
		if (persistent instanceof Subject)
			return (Subject) persistent;
		
		return null;
	}

	/* (non-Javadoc)
	 * @see net.acperience.cloudplayer.KerberosAuth#storePersistentSubject()
	 */
	@Override
	protected void storePersistentSubject(Subject subject) {
		session.setAttribute(PERSISTENT_NAME, subject);
	}

	/* (non-Javadoc)
	 * @see net.acperience.cloudplayer.KerberosAuth#destroyPersistentSubject()
	 */
	@Override
	protected void destroyPersistentSubject() {
		session.removeAttribute(PERSISTENT_NAME);
	}

	/* (non-Javadoc)
	 * @see net.acperience.cloudplayer.KerberosAuth#getUsername()
	 */
	@Override
	public String getUsername() {
		
		return request.getParameter(FORM_USERID_NAME);
	}

	/* (non-Javadoc)
	 * @see net.acperience.cloudplayer.KerberosAuth#getPassword()
	 */
	@Override
	public String getPassword() {
		return request.getParameter(FORM_PASSWORD_NAME);
	}
	
	/**
	 * Returns the user ID of the user that has been authenticated
	 * @return The College User ID of the user that has been authenticated. If the user has not been authenticated, returns null
	 */
	public String getUserId(){
		final String attributeName = "UserID";
		Subject subject = getSubject();
		if (subject == null)
			return null;
		
		// Check to see if we have stored the User ID already
		
		Object cache = session.getAttribute(attributeName);
		if (cache instanceof String){
			return (String) cache;
		}
		
		//Get a list of principals
		Set<Principal> principals = subject.getPrincipals();
		String userId = null;
		for (Principal principal : principals){
			String[] result = principal.getName().split("@");
			if (result[1].equals(KERBEROS_REALM)){
				userId = result[0];
				break;
			}
		}
		userId = userId.toLowerCase();
		session.setAttribute(attributeName, userId);
		return userId;
	}
	
	/**
	 * Returns the hash of the User ID or null if user is not logged in
	 * @return Returns the hash of the User ID or null if user is not logged in
	 */
	public String getUserIdHash(){
		String id = getUserId();
		if (id == null)
			return null;
		return MusicUtility.sha1(id);
	}
	
	/**
	 * Returns the name of the bucket for the logged in user, or null if not logged in.
	 * @return Returns the name of the bucket for the logged in user, or null if not logged in.
	 */
	public String getUserBucketName(){
		String id = getUserIdHash();
		if (id == null)
			return null;
		
		return "ywc110-cloud-" + id;
	}
	
	/**
	 * Returns the user's bucket. If it does not exist, attempts to create.
	 * @return
	 */
	public S3Bucket getUserBucket() throws S3ServiceException{
		if (bucket != null)
			return bucket;
		String name = getUserBucketName();
		if (name == null)
			return null;
		bucket = s3Service.getOrCreateBucket(name);
		return bucket;
	}
	
	/**
	 * Put a boolean value into the session
	 * 
	 * @param name
	 * @param value
	 */
	public void putSessionBoolean(String name, boolean value){
		session.setAttribute(name, value);
	}

	
	/**
	 * Get a Boolean object from session. Returns NULL if it doesn't exist
	 * @param name
	 * @return Boolean object stored in session, or NULL if non existent
	 */
	
	public Boolean getSessionBoolean(String name){
		Object result = session.getAttribute(name);
		if (result instanceof Boolean)
			return (Boolean) result;
		
		return null;
	}
	
	/**
	 * Check and sets up user's buckets and necessary files
	 * 
	 */
	public void setupUser() throws S3ServiceException{
		//if (getSessionBoolean("s3Setup") != null)
		//		return;
		// Get User Bucket - Create if necessary
		// S3Bucket bucket = getUserBucket();
		
	}

	/**
	 * Sets response object
	 * @param response
	 */
	public void setResponse(HttpServletResponse response) {
		this.response = response;
	}
	/**
	 * Write a message to output. Debugging purposes
	 * @param message
	 */
	public void writeResponse(String message){
		try {
			PrintWriter out = response.getWriter();
			out.println(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * Creates a MusicKeberos object to get authenticated user data based on the HttpRequest
	 * 
	 * @param request Request to contain the necessary user context information
	 * @param context The HttpServlet calling the method
	 * @return The object
	 * @throws LoginException
	 * @throws SecurityException
	 */
	public static MusicKerberos createMusicKerberos(HttpServletRequest request, HttpServlet context)
		throws LoginException, SecurityException {
		// We will store a cache of the object for each HTTP Request
		Object cache = request.getAttribute(AUTH_ATTRIBUTE);
		if (cache != null){
			if (cache instanceof MusicKerberos)
				return (MusicKerberos) cache;
		}
		MusicKerberos obj = new MusicKerberos(AUTH_ATTRIBUTE,
				context.getServletContext().getRealPath(LOGIN_CONF),
				context.getServletContext().getRealPath(KRB5_CONF),
				request);
		// Attempt to authenticate and populate
		obj.authenticate(false);
		
		request.setAttribute(AUTH_ATTRIBUTE, obj);
		return obj;
	}
}
