package net.acperience.cloudplayer;

import java.io.IOException;
import java.io.PrintWriter;

import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.apache.commons.fileupload.FileUploadException;
import org.jets3t.service.S3ServiceException;

public class ServletUpload extends HttpServlet {

	private static final long serialVersionUID = -7552635585810140780L;
	
	private FileManager fileManager;
	
	@Override
	public void init() throws ServletException {
		super.init();
		try {
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
	
	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
		response.sendRedirect("/");
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		
		PrintWriter out = response.getWriter();
    	response.setContentType("text/html;charset=UTF-8");
    	
		try {
			//First up, check that user is logged in
			MusicKerberos user = MusicKerberos.createMusicKerberos(request, this);
			if (!user.isAuthenticated()){
				response.sendRedirect("/auth");
				return;
			}
			JSONObject json = fileManager.handleUpload(request);
			out.write(json.toString(4));
			
		} catch (FileUploadException e) {
			e.printStackTrace(out);
		} catch (LoginException e) {
			e.printStackTrace(out);
		} catch (SecurityException e) {
			e.printStackTrace(out);
		} finally{
			out.close();
		}
	}
}
