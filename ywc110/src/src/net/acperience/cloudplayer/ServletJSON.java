package net.acperience.cloudplayer;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;

public class ServletJSON extends HttpServlet {
	private static final long serialVersionUID = 4450837983686054190L;

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
    	
    	PrintWriter out = response.getWriter();
    	response.setContentType("text/html;charset=UTF-8");
    	JSONObject json = new JSONObject();
    	try {
    		//First up, check that user is logged in
			MusicKerberos user = MusicKerberos.createMusicKerberos(request, this);
			if (!user.isAuthenticated()){
				response.sendRedirect("/auth");
				return;
			}
			user.setS3Service(s3Service);
			user.setResponse(response);		// Debugging purposes
			user.setupUser();
			
			/*
			 * Time to handle
			 */
			if (request.getParameter("list") != null)
				json = fileManager.getItems(request);
			
		} catch (LoginException e) {
			json.element("exception", ExceptionUtils.getStackTrace(e));
		} catch (SecurityException e) {
			json.element("exception", ExceptionUtils.getStackTrace(e));
		} catch (S3ServiceException e){
			json.element("exception", ExceptionUtils.getStackTrace(e));
		} catch (SQLException e) {
			json.element("exception", ExceptionUtils.getStackTrace(e));
		}
    	finally{
    		out.write(json.toString(4));
    		out.close();
    	}
    }
}
