package net.acperience.cloudplayer;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.acl.GroupGrantee;
import org.jets3t.service.acl.Permission;
import org.jets3t.service.model.S3Object;

/**
 * File manager class to take care of uploading to the current user's bucket
 * 
 * Each instance of the class is tied to a Servlet
 * 
 * @author Lawliet
 *
 */

public class FileManager {
	private HttpServlet context;
	public static final String CACHE_BASE = "/WEB-INF/cache/";
	public static final String TEMP_BASE = CACHE_BASE + "temp/";
	private S3Service s3Service = null;
	
	/**
	 * Enumeration to map file extensions to JPlayer Attribute Names
	 * @author Lawliet
	 *
	 */
	public static enum JPlayerMapping{
		mp3("mp3"),
		mp4("m4a"),
		m4a("m4a"),
		aac("m4a"),
		ogg("oga"),
		oga("oga"),
		wav("wav");
		
		private final String extension;
		JPlayerMapping(String extension){
			this.extension = extension;
		}
		
		String getExtension(){
			return this.extension;
		}
	}
	
	public FileManager(HttpServlet context) throws S3ServiceException, IOException{
		// .. Private constructor. Use getInstance() to get the object
		this.context = context;
		
		// Connect to S3
		s3Service = MusicUtility.connect(context.getServletContext().getResourceAsStream(MusicUtility.CREDENTIALS_PATH),
				context.getServletContext().getResourceAsStream(MusicUtility.JETS3_PATH));	
	}
	
	/**
	 * Based on relative path, generate the absolute path in the cache
	 * @param path
	 * @return
	 */
	public String getFilePath(String path){
		return context.getServletContext().getRealPath(CACHE_BASE + path);
	}
	
	/**
	 * Handles the upload in a HTTP POST Requwst
	 * 
	 * 
	 * @param request
	 * @return a JSONObject with the upload result of each file
	 * @throws FileUploadException
	 * @throws SecurityException 
	 * @throws LoginException 
	 * @throws IOException 
	 */
	public JSONObject handleUpload(HttpServletRequest request) throws FileUploadException, LoginException, SecurityException, IOException{
		JSONArray json = new JSONArray();
		MusicKerberos user = MusicKerberos.createMusicKerberos(request, context);
		user.setS3Service(s3Service);
		
		// See http://www.servletworld.com/servlet-tutorials/servlet-file-upload-example.html
		DiskFileItemFactory  fileItemFactory = new DiskFileItemFactory ();
		fileItemFactory.setSizeThreshold(5*1024*1024);	// Any size above thant his will be stored on disk
		// Temp storage
		fileItemFactory.setRepository(new File(context.getServletContext().getRealPath(TEMP_BASE)));
		
		ServletFileUpload uploadHandler = new ServletFileUpload(fileItemFactory);
		
			/*
			 * Parse the request
			 */
			@SuppressWarnings("unchecked")
			List<FileItem> items = uploadHandler.parseRequest(request);
			Iterator<FileItem> itr = items.iterator();
			while(itr.hasNext()) {
				FileItem item = (FileItem) itr.next();
				// If form field, ignore and carry on
				if(item.isFormField()) {
					continue;
				} else {
					JSONObject jsonCurrent = new JSONObject();
					
					jsonCurrent.element("name", item.getName());
					jsonCurrent.element("size", item.getSize());
					
					// Write file to cache storage first
					File file = new File(
							context.getServletContext().getRealPath(CACHE_BASE)+"/"+user.getUserIdHash() 
							+ "." + FilenameUtils.getExtension(item.getName()));
					
					int itemId = 0;
					DbManager db = null;
					try{
						item.write(file);
						
						AudioFile meta = null;
						// Read metadata
						meta = AudioFileIO.read(file);
						
						jsonCurrent.element("format", meta.getAudioHeader().getFormat());
						//TODO Restrict to mp3, oga, m4a, wav, aac
						
						//jsonCurrent.element("encoding",meta.getAudioHeader().getEncodingType());
						jsonCurrent.element("duration", meta.getAudioHeader().getTrackLength());
						
						// See http://www.jthink.net/jaudiotagger/examples_read.jsp
						Tag tag = meta.getTag();
						jsonCurrent.element("artist", tag.getFirst(FieldKey.ARTIST));
						jsonCurrent.element("album", tag.getFirst(FieldKey.ALBUM));
						jsonCurrent.element("title", tag.getFirst(FieldKey.TITLE));
						//jsonCurrent.element("year", Integer.parseInt(tag.getFirst(FieldKey.YEAR)));
						//jsonCurrent.element("track", tag.getFirst(FieldKey.TRACK));
						
						jsonCurrent.element("success", true);
						
						// Now let's do S3 Processing
						// Let's build a new key
						String key = getKey(tag.getFirst(FieldKey.ARTIST), 
								tag.getFirst(FieldKey.ALBUM), 
								tag.getFirst(FieldKey.TITLE), 
								meta.getAudioHeader().getEncodingType());
						

						// Now we are going to insert a new entry into the database first
						
						db = DbManager.getInstance(context);
						int year;
						try{
							year = Integer.parseInt(tag.getFirst(FieldKey.YEAR));
						} catch (NumberFormatException e){
							year = 0;
						}
						itemId = db.insertItem(user.getUserId(), 
								tag.getFirst(FieldKey.TITLE), 
								tag.getFirst(FieldKey.ARTIST),
								tag.getFirst(FieldKey.ALBUM), 
								year, key,
								meta.getAudioHeader().getTrackLength());
						jsonCurrent.element("itemId", itemId);						
						
						S3Object s3Object = null;
						s3Object = new S3Object(file);
						s3Object.setKey(key);
						
						// Now let's put the file to S3
						s3Object = s3Service.putObject(user.getUserBucket(), s3Object);		
						
						// Now let's give it public access
						AccessControlList acl = s3Service.getObjectAcl(user.getUserBucket(), key);
						acl.grantPermission(GroupGrantee.ALL_USERS, Permission.PERMISSION_READ);
						s3Object.setAcl(acl);
						s3Service.putObjectAcl(user.getUserBucket(), s3Object);
						
						jsonCurrent.element("key",key);
						jsonCurrent.element("success", true);
						
					} catch (CannotReadException e) {		// Reading meta data from uploaded file
						jsonCurrent.element("success", false);
						jsonCurrent.element("exception", ExceptionUtils.getStackTrace(e));
						jsonCurrent.element("errorFriendly", "Unable to parse file for meta data");
						
					} catch (TagException e) { // Reading meta data from uploaded file
						jsonCurrent.element("success", false);
						jsonCurrent.element("exception", ExceptionUtils.getStackTrace(e));
						jsonCurrent.element("errorFriendly", "Unable to parse file metadata -- is the file corrupted?");
						
					} catch (ReadOnlyFileException e) { // Reading meta data from uploaded file
						jsonCurrent.element("success", false);
						jsonCurrent.element("exception", ExceptionUtils.getStackTrace(e));
						jsonCurrent.element("errorFriendly", "Cache file permission error.");
						
					} catch (InvalidAudioFrameException e) { // Reading meta data from uploaded file
						jsonCurrent.element("success", false);
						jsonCurrent.element("exception", ExceptionUtils.getStackTrace(e));
						jsonCurrent.element("errorFriendly", "Unable to parse file metadata -- is the file corrupted?");
						
					} catch (SQLException e) {	// Any database operations
						jsonCurrent.element("success", false);
						jsonCurrent.element("exception", ExceptionUtils.getStackTrace(e));
						jsonCurrent.element("errorFriendly", "Error performing database operations.");
						continue;
					} catch (NoSuchAlgorithmException e) {
						// .. Gonna ignore this, for now. Thrown when the JVM doens't support MD5. Unlikely.
					} catch (S3ServiceException e) {
						jsonCurrent.element("success", false);
						jsonCurrent.element("exception", ExceptionUtils.getStackTrace(e));
						jsonCurrent.element("errorFriendly", "Unable to store file in cloud");
						
						try {
							if (db != null && itemId != 0)
								db.deleteItemById(user.getUserId(), itemId);
						} catch (SQLException e1) {
							// Seriously? so screwed up?
							jsonCurrent.element("exception", ExceptionUtils.getStackTrace(e1));
						} 
					} catch (Exception e) {		// From writing upload file to cache
						jsonCurrent.element("success", false); 
						jsonCurrent.element("exception", ExceptionUtils.getStackTrace(e));
						jsonCurrent.element("errorFriendly", "Failed writing file to cache for processing.");
					} finally{
						json.add(jsonCurrent);
						file.delete();
					}
				}
			}
			
		return new JSONObject().element("files", json);
	}
	
	/**
	 * Returns a JSONObject containing a list of files belonging to a user 
	 * @param user
	 * @return
	 * @throws LoginException
	 * @throws SecurityException
	 * @throws SQLException 
	 * @throws IOException 
	 */
	public JSONObject getItems(MusicKerberos user) throws LoginException, SecurityException, IOException, SQLException{
		JSONObject json = new JSONObject();
		DbManager db = DbManager.getInstance(context);
		
		ResultSet results = db.getItems(user.getUserId());
		JSONArray jsonArray = new JSONArray();
		while(results.next()){
			JSONObject jsonCurrent = new JSONObject();
			try {
				String url = getUrl(results.getString("itemkey"), user.getUserBucketName());
				jsonCurrent.element("id", results.getInt("itemid"));
				jsonCurrent.element("title", results.getString("itemtitle"));
				jsonCurrent.element("artist", results.getString("itemartist"));
				jsonCurrent.element("album", results.getString("itemalbum"));
				jsonCurrent.element("key", results.getString("itemkey"));
				jsonCurrent.element("year",results.getInt("itemyear"));
				jsonCurrent.element("duration", results.getInt("itemDuration"));
				jsonCurrent.element("url", url);
				//JPlayer specific
				jsonCurrent.element(getJPlayerAttributeName(url), url);
				jsonCurrent.element("free", true);		// Allow access to the URL
				jsonArray.add(jsonCurrent);
			} catch (SQLException e) {
				json.element("exception", ExceptionUtils.getStackTrace(e));
			}
		}
		json.element("items", jsonArray);
		json.element("playlistId", 0);
		return json;
	}
	
	/**
	 * Get a list of items for a playlist
	 * @param user
	 * @param playlistID
	 * @return
	 * @throws SQLException 
	 * @throws IOException 
	 */
	public JSONObject getPlaylistItemsByID(MusicKerberos user, int playlistID) throws IOException, SQLException{
		JSONObject json = new JSONObject();
		DbManager db = DbManager.getInstance(context);
		
		ResultSet results = db.getPlaylistItemsByID(user.getUserId(), playlistID);
		JSONArray jsonArray = new JSONArray();
		while(results.next()){
			if (!json.has("playlistName"))
				json.element("playlistName", results.getString("playlistName"));
			JSONObject jsonCurrent = new JSONObject();
			try {
				String url = getUrl(results.getString("itemkey"), user.getUserBucketName());
				jsonCurrent.element("id", results.getInt("itemid"));
				jsonCurrent.element("title", results.getString("itemtitle"));
				jsonCurrent.element("artist", results.getString("itemartist"));
				jsonCurrent.element("album", results.getString("itemalbum"));
				jsonCurrent.element("key", results.getString("itemkey"));
				jsonCurrent.element("year",results.getInt("itemyear"));
				jsonCurrent.element("duration", results.getInt("itemDuration"));
				jsonCurrent.element("url", url);
				//JPlayer specific
				jsonCurrent.element(getJPlayerAttributeName(url), url);
				jsonCurrent.element("free", true);		// Allow access to the URL
				jsonArray.add(jsonCurrent);
			} catch (SQLException e) {
				json.element("exception", ExceptionUtils.getStackTrace(e));
			}
		}
		
		// Oh, empty playlist
		if (!json.has("playlistName")){
			results = db.getPlaylistById(user.getUserId(), playlistID);
			results.next();
			json.element("playlistName", results.getString("playlistName"));
		}
		json.element("items", jsonArray);
		json.element("playlistId", playlistID);
		return json;
	}
	
	/**
	 * Internal method to handle item requests;
	 * @param user
	 * @param results
	 * @return
	 * @throws LoginException
	 * @throws SecurityException
	 * @throws IOException
	 * @throws SQLException
	 */
	private JSONObject getItem(MusicKerberos user, ResultSet results) 
			throws IOException, SQLException{
		JSONObject json = new JSONObject();

		if (results != null && results.next()){
			String url = getUrl(results.getString("itemkey"), user.getUserBucketName());
			json.element("id", results.getInt("itemid"));
			json.element("title", results.getString("itemtitle"));
			json.element("artist", results.getString("itemartist"));
			json.element("album", results.getString("itemalbum"));
			json.element("key", results.getString("itemkey"));
			json.element("year",results.getInt("itemyear"));
			json.element("duration", results.getInt("itemDuration"));
			json.element("url", url);
			json.element(getJPlayerAttributeName(url), url);
		}
		if (json.isEmpty())
			json.element("error", "Incorrect request parameter, unauthorised access, or item was not found.");
		return json;
	}
	
	/**
	 * Get items by Key
	 * @param itemKey
	 * @param user
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 */
	public JSONObject getItemByKey(String itemKey, MusicKerberos user) throws IOException, SQLException{
		DbManager db = DbManager.getInstance(context);
		return getItem(user,db.getItemByKey(itemKey, user.getUserId()));
	}
	/**
	 * Get items by ID
	 * @param itemId
	 * @param user
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 */
	public JSONObject getItemById(int itemId, MusicKerberos user) throws IOException, SQLException{
		DbManager db = DbManager.getInstance(context);
		return getItem(user,db.getItemById(itemId, user.getUserId()));
	}
	
	/**
	 * Delete item from the entire system
	 * @param itemId
	 * @param user
	 * @return
	 * @throws SQLException 
	 * @throws IOException 
	 */
	public JSONObject deleteItemById(int itemId, MusicKerberos user) throws IOException, SQLException{
		JSONObject json = new JSONObject();
		DbManager db = DbManager.getInstance(context);
		
		// Let's fetch the Object Key first
		JSONObject result = getItemById(itemId, user);
		if (!result.has("key")){
			json.element("error", "No such item found.");
			return json;
		}
		String itemKey = result.getString("key");
		
		// Delete it now
		db.deleteItemById(user.getUserId(), itemId);
		
		// Remove from S3
		try{
			// Delete object from S3		
			s3Service.deleteObject(user.getUserBucketName(), itemKey);
			json.element("success", true);
		}
		catch (ServiceException e){
			// Doesn't really matter
			json.element("exception", ExceptionUtils.getStackTrace(e));
		} 
		return json;
	}

	/**
	 * Delete item from the entire system
	 * @param itemId
	 * @param user
	 * @return
	 * @throws SQLException 
	 * @throws IOException 
	 */
	public JSONObject deleteItemByKey(String itemKey, MusicKerberos user) throws IOException, SQLException{
		JSONObject json = new JSONObject();
		DbManager db = DbManager.getInstance(context);
		
		
		// Delete it now
		db.deleteItemByKey(user.getUserId(), itemKey);
		
		// Remove from S3
		try{
			// Delete object from S3		
			s3Service.deleteObject(user.getUserBucketName(), itemKey);
			json.element("success", true);
		}
		catch (ServiceException e){
			// Doesn't really matter
			json.element("exception", ExceptionUtils.getStackTrace(e));
		} 
		return json;
	}
	
	/**
	 * Do a listing of the user's playlist. Does not include the "ALL" playlist
	 * @param user
	 * @return
	 * @throws SQLException 
	 * @throws IOException 
	 */
	public JSONObject listPlaylists(MusicKerberos user) throws IOException, SQLException{
		JSONObject json = new JSONObject();
		DbManager db = DbManager.getInstance(context);
		JSONArray jsonArray = new JSONArray();
		
		ResultSet results = db.getPlaylistsByUser(user.getUserId());
		while (results.next()){
			JSONObject jsonCurrent = new JSONObject();
			jsonCurrent.element("playlistName", results.getString("playlistName"));
			jsonCurrent.element("playlistId", results.getInt("playlistId"));
			
			jsonArray.add(jsonCurrent);
		}
		json.element("playlists", jsonArray);
		return json;
	}
	
	/**
	 * Based on inputs, get the key
	 * @param artist
	 * @param album
	 * @param title
	 * @param extension
	 * @return
	 */
	public static String getKey(String artist, String album, String title, String extension){
		StringBuilder key = new StringBuilder();
		key.append(artist).append("_");
		key.append(album).append("_");
		key.append(title);
		
		return key.toString().replaceAll("\\W+", "_") + "." + extension;
		
	}
	
	/**
	 * Return the URL to the item based on key and bucket
	 * @param key
	 * @param bucket
	 * @return
	 */
	public static String getUrl(String key, String bucket){
		return new StringBuilder().append("http://")
				.append(bucket).append(".s3.bigdatapro.org/")
				.append(key).toString();
	}
	
	/**
	 * Convenience method to get URL
	 * @param artist
	 * @param album
	 * @param title
	 * @param extension
	 * @param bucket
	 * @return
	 */
	public static String getUrl(String artist, String album, String title, String extension, String bucket){
		return getUrl(getKey(artist, album, title, extension), bucket);
	}
	
	/**
	 * Based on key, generate the name of the JPlayer attribute for use with JPlayer
	 * @param key
	 * @return
	 */
	public static String getJPlayerAttributeName(String key){
		// Get extension of key
		String extension = FilenameUtils.getExtension(key).toLowerCase();
		try{
			return JPlayerMapping.valueOf(extension).getExtension();
		}
		catch(IllegalArgumentException e){
			// Just do a fake MP3 for now
			return "mp3";
		}
	}
}
