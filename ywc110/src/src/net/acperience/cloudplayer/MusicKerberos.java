package net.acperience.cloudplayer;

import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;

/**
 * 
 * Kerberos Authentication implementation using a serialised version of the Subject as the persistent storage
 * Used in a servlet environment. 
 * One instance of the class is instantiated with each HTTP request and should be destroyed upon the completion
 * of that HTTP request.
 * 
 * Otherwise, you might get LEAKS on two fronts: the HttpServletRequest object and the MusicKerberos object
 * 
 * @author Lawliet
 *
 */
public class MusicKerberos extends KerberosAuth {
	
	private static final String PERSISTENT_NAME = "MusicKerberosSubject";
	private static final String FORM_USERID_NAME = "userId";
	private static final String FORM_PASSWORD_NAME = "password";
	private static final String KERBEROS_REALM = "IC.AC.UK";

	private HttpSession session;
	private HttpServletRequest request;
	
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
		Subject subject = getSubject();
		if (subject == null)
			return null;
		
		//Get a list of principals
		Set<Principal> principals = subject.getPrincipals();
		String userId = null;
		for (Principal principal : principals){
			String[] result = principal.getName().split("@");
			if (result[1] == KERBEROS_REALM){
				userId = result[0];
				break;
			}
		}
		
		return userId;
	}
	
	/**
	 * Check and sets up user's buckets and necessary files
	 * 
	 */
	public void setupUser(S3Service s3Service) throws S3ServiceException{
		
	}
}
