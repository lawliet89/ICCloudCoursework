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
    	
    	request.setCharacterEncoding("UTF-8");
    	response.setCharacterEncoding("UTF-8");
    	PrintWriter out = response.getWriter();
    	response.setContentType("text/json;charset=UTF-8");
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
			
			// Do a simple list of all the items belonging to the user
			// /json?list[&playlistId=xx]
			if (request.getParameter("list") != null){
				if (request.getParameter("playlistId") == null || request.getParameter("playlistId").equals("0"))
					json = fileManager.getItems(user);
				else
					json = fileManager.getPlaylistItemsByID(user, Integer.parseInt(request.getParameter("playlistId")));
			}
			// Retrieve detail of a single item
			else if (request.getParameter("item") != null){
				
				// /json?item&itemKey=?[&redirect]
				if (request.getParameter("itemKey") != null)
					json = fileManager.getItemByKey(request.getParameter("itemKey"), user);
					if (request.getParameter("redirect") != null)
						response.sendRedirect(json.getString("url"));
					
				// /json?item&itemId=?[&redirect]
				else if (request.getParameter("itemId") != null){
					int itemId = Integer.parseInt(request.getParameter("itemId"));
					json = fileManager.getItemById(itemId, user);
					if (request.getParameter("redirect") != null)
						response.sendRedirect(json.getString("url"));
				}
			}
			
			// Remove an item from the system or from a playlist
			else if (request.getParameter("remove") != null){
				// We consume nonce first
				if (request.getParameter("nonce") != null && MusicUtility.consumeNonce(request, request.getParameter("nonce"))){
					if (request.getParameter("playlistId") == null){			
						// /json?remove&itemId=?
						if (request.getParameter("itemId") != null){
							json = fileManager.deleteItemById(Integer.parseInt(request.getParameter("itemId")), user);
						}
						// /json?remove&itemKey=?
						else if (request.getParameter("itemKey") != null){
							json = fileManager.deleteItemByKey(request.getParameter("itemKey"), user);
						}
					}
					else{
						try{
							// Remove item from playlist
							// json?remove&itemId=?&playlistId=?
							DbManager db = DbManager.getInstance(this);
							int count = db.removeItemFromPlaylist(user.getUserId(), 
									Integer.parseInt(request.getParameter("itemId")), 
									Integer.parseInt(request.getParameter("playlistId")));
							json.element("success",true);
							json.element("count",count);
						}
						catch(Exception e){
							json.element("success", false);
							throw e;
						}
					}
				}
				else{
					json.element("error","Invalid nonce. Have you already tried this operation before?");
				}
			}
			else if (request.getParameter("renamePlaylist") != null 
					&& request.getParameter("playlistId") != null 
					&& request.getParameter("playlistName") != null){
				
				// To return
				String playlistName = null;
				String playlistId = null;
				try{
					// Consume nonce first
					if (request.getParameter("nonce") != null && MusicUtility.consumeNonce(request, request.getParameter("nonce"))){
						// Let's try to parse playlistId
						// Of the pattern playlist-xx where xx is the ID number
						// If this is invalid, an exception will be thrown.
						int id = Integer.parseInt(request.getParameter("playlistId").substring(9));
						
						if (request.getParameter("playlistName").isEmpty())
							throw new RuntimeException("Playlist name cannot be empty!");
						
						// Attempt a rename - will throw exceptions and fail if playlist does not exist. Handled by try and catch below.
						DbManager db = DbManager.getInstance(this);
						db.renamePlaylistByID(user.getUserId(), id, request.getParameter("playlistName"));
						
						// If we have reached here, then all have succeeded.
						json.element("success", true);
						playlistName = request.getParameter("playlistName");
						playlistId = Integer.toString(id);
					}
					else{
						json.element("error","Invalid nonce. Have you already tried this operation before?");
						throw new RuntimeException("Invalid nonce");
					}
				}
				finally{	// Assign. We need to assign original values in case of error for client UI to update properly
					if (playlistName == null || playlistId == null)
						json.element("success", false);
					if (playlistName == null)
						playlistName = request.getParameter("playlistName");
					if (playlistId == null)
						playlistId = request.getParameter("playlistId");
					json.element("playlistName", playlistName);
					json.element("playlistId", playlistId);
				}
			}
			else if (request.getParameter("newPlaylist") != null){
				// Consume nonce first
				if (request.getParameter("nonce") != null && MusicUtility.consumeNonce(request, request.getParameter("nonce"))){
					try{
						DbManager db = DbManager.getInstance(this);
						// Insert a new playlist
						int playlistId = db.insertPlaylist(user.getUserId(), "Untitled New Playlist");
						json.element("success",true);
						json.element("playlistId", playlistId);
						json.element("playlistName", "Untitled New Playlist");
						
					} catch(SQLException e){
						json.element("success", false);
						throw e;
					}
				}
				else{
					json.element("success", false);
					json.element("error","Invalid nonce. Have you already tried this operation before?");
					throw new RuntimeException("Invalid nonce");
				}
			}
			else if (request.getParameter("deletePlaylist") != null){
				// Consume nonce first
				if (request.getParameter("nonce") != null && MusicUtility.consumeNonce(request, request.getParameter("nonce"))){
					try{
						DbManager db = DbManager.getInstance(this);
						db.deletePlaylist(user.getUserId(), Integer.parseInt(request.getParameter("playlistId")));
						json.element("success",true);
						json.element("playlistId", Integer.parseInt(request.getParameter("playlistId")));
						
					} catch(SQLException e){
						json.element("success", false);
						throw e;
					}
				}
				else{
					json.element("success", false);
					json.element("error","Invalid nonce. Have you already tried this operation before?");
					throw new RuntimeException("Invalid nonce");
				}
			}
			else if (request.getParameter("playlists") != null){
				json = fileManager.listPlaylists(user);
			}
			else if (request.getParameter("nonce") != null){
				// Generate and return a new nonce
				// /json?nonce
				String nonce = MusicUtility.generateNonce(request, user);
				json.element("nonce",nonce);
			}
			
		} catch (LoginException e) {
			json.element("exception", ExceptionUtils.getStackTrace(e));
		} catch (SecurityException e) {
			json.element("exception", ExceptionUtils.getStackTrace(e));
		} catch (S3ServiceException e){
			json.element("exception", ExceptionUtils.getStackTrace(e));
		} catch (SQLException e) {
			json.element("exception", ExceptionUtils.getStackTrace(e));
		} catch (NumberFormatException e){
			json.element("exception", ExceptionUtils.getStackTrace(e));
			json.element("error", "ID invalid.");
		} catch (Exception e){
			json.element("exception", ExceptionUtils.getStackTrace(e));
		}
    	
    	finally{
    		out.write(json.toString(4));
    		out.close();
    	}
    }
}
