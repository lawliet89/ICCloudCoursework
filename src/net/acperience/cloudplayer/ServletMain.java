package net.acperience.cloudplayer;

import java.io.*;
import javax.security.auth.login.LoginException;
import javax.servlet.*;
import javax.servlet.http.*;

import net.sourceforge.jtpl.Jtpl;

import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;

public class ServletMain extends HttpServlet {
	private static final long serialVersionUID = -1418564285836413358L;
	public static final String TAG = "ServletMain";
	
	private S3Service s3Service;
//	private FileManager fileManager;
	
	public ServletMain(){
		// ...
	}

	@Override
	public void init() throws ServletException{
		super.init();
		// Get credentials object
		try{
			s3Service = MusicUtility.connect(getServletContext().getResourceAsStream(MusicUtility.CREDENTIALS_PATH),
					getServletContext().getResourceAsStream(MusicUtility.JETS3_PATH));	
			
//			fileManager = new FileManager(this);
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
    	
    	response.setCharacterEncoding("UTF-8");
    	PrintWriter out = response.getWriter();
    	response.setContentType("text/html;charset=UTF-8");
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
			
			// Print the page
			Jtpl body = MusicUtility.createBodyTpl(this, MusicUtility.TPL_DIR + "main.tpl");
			body.assign("userId", user.getUserId());
			body.parse("Body");
			out.print(MusicUtility.outputPage(this, "Cloud Music Player", body));
			
			
		} catch (LoginException e) {
			e.printStackTrace(out);
		} catch (SecurityException e) {
			e.printStackTrace(out);
		} catch (S3ServiceException e){
			e.printStackTrace(out);
		}
    	finally{
    		out.close();
    	}
    	
    }
}
