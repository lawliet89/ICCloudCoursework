package net.acperience.cloudplayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;

import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.model.S3Object;

/**
 * Handles the streaming of music files.
 * @author Lawliet
 *
 */
public class ServletStream extends HttpServlet {
	private static final long serialVersionUID = 1969251208091469532L;
	private S3Service s3Service;
	private FileManager fileManager;
	
	@Override
	public void init() throws ServletException{
		super.init();
		// Get credentials object
		try{
			s3Service = MusicUtility.connect(getServletContext().getResourceAsStream(MusicUtility.CREDENTIALS_PATH),
					getServletContext().getResourceAsStream(MusicUtility.JETS3_PATH));	
			
			fileManager = new FileManager(this);
		}
		catch (IOException e){
			e.printStackTrace();
			throw new RuntimeException("S3 Credentials file (" + MusicUtility.CREDENTIALS_PATH + ") or Jets3 file cannot be found and loaded.");
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
    
    	// Called in the form /stream/user_id_hash/key
    	String requestPath = request.getPathInfo();
    	OutputStream out = null;
    	
    	try{
    		MusicKerberos user = MusicKerberos.createMusicKerberos(request, this);
    		user.setS3Service(s3Service);
    		if (requestPath == null)
    			throw new IllegalArgumentException("No request was sent to this servlet.");
    		
    		// Parse
    		String[] components = requestPath.split("\\/");
    		// 0 - Nothing. 1 - User Hash. 2 - Key
    		
    		// User checking -- currently only allows current user to access
    		if (!components[1].equals(user.getUserIdHash()))
    			throw new IllegalAccessException("Logged in user does not match the user that is requested.");
    		
    		// Key checking
    		File file = fileManager.getFile(components[2], user);
    		
    		// So far so good. 
    		S3Object obj = fileManager.getS3Object(components[2], user);
    		
    		// Then we shall output the necessary Meta Data and HTTP Headers
    		response.reset();
    		// Mime
    		response.setContentType(FileManager.getMime(components[2]));
    		// Last modified
    		response.setDateHeader("Last-Modified", obj.getLastModifiedDate().getTime());
    		// Content Length
    		response.setHeader("Content-Length", Long.toString(FileUtils.sizeOf(file)));
    		// ETag
    		response.setHeader("Etag", obj.getETag());
    		// Connection
    		response.setHeader("Connection", "close");
    		
    		// Write
    		out = response.getOutputStream();
    		IOUtils.copy(new FileInputStream(file), out);  
    		out.flush();
    		
    	} catch (LoginException e) {
			printError(response, e);
		} catch (SecurityException e) {
			printError(response, e);
		} catch (IllegalAccessException e) {
			printError(response, e);
		} catch (NoSuchAlgorithmException e) {
			printError(response, e);
		} catch (ServiceException e) {
			printError(response, e);
		}
 
    	finally{
    		IOUtils.closeQuietly(out);
    	}
    }
    
    /**
     * Print a text/plain error
     * @param response
     * @param e
     * @throws IOException 
     */
    protected void printError(HttpServletResponse response, Exception e) throws IOException{
    	response.setContentType("text/plain;charset=UTF-8");
    	PrintWriter out = response.getWriter();
    	out.print(ExceptionUtils.getStackTrace(e));
    	out.close();
    }

}
