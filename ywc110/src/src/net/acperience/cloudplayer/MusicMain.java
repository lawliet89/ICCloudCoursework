package net.acperience.cloudplayer;

import java.io.*;
import java.util.Properties;

import javax.security.auth.login.LoginException;
import javax.servlet.*;
import javax.servlet.http.*;

import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;

public class MusicMain extends HttpServlet {
	private static final long serialVersionUID = -1418564285836413358L;
	public static final String PROPERTIES_PATH = "/WEB-INF/properties/s3credential.properties";
	public static final String TAG = "MusicMain";
	
	private S3Service s3Service;
	
	public MusicMain(){
		// ...
	}

	@Override
	public void init() throws ServletException{
		super.init();
		// Get credentials object
		try{
			s3Service = MusicUtility.connect(getServletContext().getResourceAsStream(PROPERTIES_PATH));	
		}
		catch (IOException e){
			e.printStackTrace();
			throw new RuntimeException("S3 Credentials file (" + PROPERTIES_PATH + ") cannot be found and loaded.");
		}
		catch (S3ServiceException e){
			e.printStackTrace();
			throw new RuntimeException("Error connecting to S3.");
		}
	}

	
    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
    	
    	doRequest(request, response); 
    }

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
    	doRequest(request, response);
    }
    
    /**
     * Handles HTTP GET or POST Request
     */
    
    protected void doRequest(HttpServletRequest request, HttpServletResponse response)
    		throws ServletException, IOException{
    	
    	PrintWriter out = response.getWriter();
    	
    	try {
    		//First up, check that user is logged in
			MusicKerberos user = MusicAuth.createMusicKerberos(request, this);
			if (!user.isAuthenticated()){
				response.sendRedirect("/auth");
				return;
			}
			// Make sure user is up and running
			user.setupUser(s3Service);
			
			
		} catch (LoginException e) {
			e.printStackTrace(out);
		} catch (SecurityException e) {
			e.printStackTrace(out);
		} catch (S3ServiceException e){
			// TODO
		}
    	finally{
    		out.close();
    	}
    	
    }
}
