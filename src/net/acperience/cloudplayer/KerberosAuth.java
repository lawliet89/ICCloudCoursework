package net.acperience.cloudplayer;


import java.io.IOException;

import javax.security.auth.*;
import javax.security.auth.login.*;
import javax.security.auth.callback.*;

/**
 *	The default kerberos configuration is found on linux machines in DoC at /etc/krb5.conf <br />
 *	The realm to use is IC.AC.UK. The KDCs can be found by running dig -t SRV _kerberos._tcp.ic.ac.uk<br /><br />
 *	
 *	Everything else is pretty much: just follow the tutorial!<br /><br />
 *	
 *	References: http://www.bsg.co.za/web/guest/software_solutions_technical_solution_showcase_java_kerberos<br /><br />
 *	
 *	Implement the abstract methods described<ul>
 *	<li>getPersistentSubject: Used to see if a subject has been authenticated</li>
 *	<li>storePersistentSubject: Used to store a persistent subject upon successful login</li>
 *	<li>destroyPersistentSubject: Destroy a persistent subject upon logout</li>
 *	<li>getUsername: Used to get a username for authentication</li>
 *	<li>getPassword: Used to get a password for authentication</li></ul>
 *	 	
 *	 After constructing the object, to populate the necessary fields, call authenticate()
 * @author Lawliet
 *
 */
public abstract class KerberosAuth implements CallbackHandler{
		
	protected String name;		// The name of the authentication "rule"/module to use
	private LoginContext lc;		// Login Context
	private Subject subject;		// Logged in subject

	// Default constructor to do no configuration changes i.e. loading of stuff
	public KerberosAuth(String name)
		throws LoginException, SecurityException
	{
		this.name = name;
	}
	
	// Constructor to set the AuthLoginConfig file along with the 
	public KerberosAuth(String name, String authLoginConfig)
			throws LoginException, SecurityException
	{
		this(name);
		
		System.setProperty("java.security.auth.login.config", authLoginConfig);
	}
	
	// Constructor to control everything and load a krb5.conf file
	public KerberosAuth(String name, String authLoginConfig, String krb5Config)
			throws LoginException, SecurityException
	{
		this(name, authLoginConfig);
		
		System.setProperty("java.security.krb5.conf", krb5Config);
	}
	
	// Constructor to set AuthConfig, realm and kdc
	public KerberosAuth(String name, String authLoginConfig, String krbRealm, String krbKdc)
			throws LoginException, SecurityException
	{
		this(name, authLoginConfig);
		
		System.setProperty("java.security.krb5.realm", krbRealm);
		System.setProperty("java.security.krb5.kdc", krbKdc);
	}

	/**
	 * @return the authenticated subject
	 */
	public Subject getSubject() {
		return subject;
	}
	
	/**
	 * Handle login callbacks - implemented as required by CallbackHandler
	 */
	@Override
	public void handle(Callback[] callbacks)
            throws IOException, UnsupportedCallbackException{
		for (Callback current : callbacks){
			if (current instanceof NameCallback){
				NameCallback nc = (NameCallback) current;
				nc.setName(getUsername());
			}
			else if (current instanceof PasswordCallback){
				PasswordCallback pc = (PasswordCallback) current;
				pc.setPassword(getPassword().toCharArray());
			}
			else{
				throw new UnsupportedCallbackException(current, "Unsupported callback");
			}
		}
	}
	
	/**
	 * Creates a LoginContext based on settings
	 */
	protected void createLoginContext()
			throws LoginException, SecurityException
	{
		lc = new LoginContext(name,this);
	}
	
	protected LoginContext getLoginContext()
			throws LoginException, SecurityException
	{
		if (lc == null) createLoginContext();
		return lc;
	}
	
	/**
	 * Check if we have a subject authenticated. 
	 * You should call authenticate() first to populate the fields
	 * 
	 * @return whether a user has been authenticated
	 */
	public boolean isAuthenticated(){
		return subject != null;
	}

	/**
	 * Authenticates and populate the subject field of the class
	 * 
	 * Method will first attempt to load a persistent subject. 
	 * Failing that, will attempt to load a subject from the LoginContext
	 * If that fails, depending on whether doLogin is set to true or not, will perform a login
	 * 
	 * @param doLogin Set to true to perform a login
	 * @throws LoginException
	 */
	public void authenticate(boolean doLogin)
		throws LoginException
	{
		// Get persistent subject
		subject = getPersistentSubject();
		if (subject != null)
			return;
		
		// Get subject from LC
		subject = getLoginContext().getSubject();
		
		if (subject != null)
			return;
		
		if (doLogin)
			login();
	}
	
	/**
	 * Convenience method to do a login by default
	 */
	public void authenticate()
			throws LoginException, SecurityException
	{
		authenticate(true);
	}
	
	/**
	 * Performs a login
	 */
	public void login()
		throws LoginException
	{
		getLoginContext().login();
		storePersistentSubject(lc.getSubject());
		
		// Let's repopulate
		authenticate(false);
	}
	
	/**
	 * Performs a logout
	 */
	public void logout()
		throws LoginException
	{
		// Check if LoginContext has a subject
		if (getLoginContext().getSubject() != null)
			getLoginContext().logout();
		destroyPersistentSubject();
		
		// Let's repopulate
		authenticate(false);
	}
	
	/**
	 * Override this method to allow the class to retrieve any stored authenticated subjects
	 * If no one has been authenticated, return NULL and the class will attempt to authenticate
	 * If not using persistent subjects, just override an empty method.
	 * 
	 * @return The subject stored in some persistent storage or NULL if none
	 */
	public abstract Subject getPersistentSubject();
	
	/**
	 * Store a persistent subject. Called after a successful login attempt.
	 * If not storing, just override an empty method
	 */
	protected abstract void storePersistentSubject(Subject subject);
	
	/**
	 * Destroys a persistent subject upon successful logout attempt.
	 * If not using persistent subject, override an empty method
	 */
	protected abstract void destroyPersistentSubject();
	
	/**
	 * Override this method to allow the class to get a username for authentication purposes if necessary
	 * 
	 * @return Username for authentication
	 */
	public abstract String getUsername();
	
	/**
	 * Override this method to allow the class to get a password for authentication purposes if necessary
	 * 
	 * @return Password for authentication
	 */
	public abstract String getPassword();
}
