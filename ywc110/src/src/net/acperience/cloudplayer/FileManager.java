package net.acperience.cloudplayer;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

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
import org.jaudiotagger.tag.TagField;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
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
	private static final String UPLOAD_FIELD = "uploadFile";
	
	private S3Service s3Service = null;
	
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
		JSONObject json = new JSONObject();
		MusicKerberos user = MusicKerberos.createMusicKerberos(request, context);
		
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
					try {
						item.write(file);
					} catch (Exception e) {
						jsonCurrent.element("sucess", false);
						jsonCurrent.element("exception", ExceptionUtils.getStackTrace(e));
						jsonCurrent.element("errorFriendly", "Failed writing file to cache for processing.");
						continue;
					}
					
					AudioFile meta = null;
					// Read metadata
					try {
						meta = AudioFileIO.read(file);
					} catch (CannotReadException e) {
						jsonCurrent.element("sucess", false);
						jsonCurrent.element("exception", ExceptionUtils.getStackTrace(e));
						jsonCurrent.element("errorFriendly", "Unable to parse file for meta data");
						continue;
					} catch (TagException e) {
						jsonCurrent.element("sucess", false);
						jsonCurrent.element("exception", ExceptionUtils.getStackTrace(e));
						jsonCurrent.element("errorFriendly", "Unable to parse file metadata -- is the file corrupted?");
						continue;
					} catch (ReadOnlyFileException e) {
						jsonCurrent.element("sucess", false);
						jsonCurrent.element("exception", ExceptionUtils.getStackTrace(e));
						jsonCurrent.element("errorFriendly", "Cache file permission error.");
						continue;
					} catch (InvalidAudioFrameException e) {
						jsonCurrent.element("sucess", false);
						jsonCurrent.element("exception", ExceptionUtils.getStackTrace(e));
						jsonCurrent.element("errorFriendly", "Unable to parse file metadata -- is the file corrupted?");
						continue;
					}
					jsonCurrent.element("format", meta.getAudioHeader().getFormat());
					jsonCurrent.element("encoding",meta.getAudioHeader().getEncodingType());
					
					// See http://www.jthink.net/jaudiotagger/examples_read.jsp
					Tag tag = meta.getTag();
					jsonCurrent.element("artist", tag.getFirst(FieldKey.ARTIST));
					jsonCurrent.element("album", tag.getFirst(FieldKey.ALBUM));
					jsonCurrent.element("title", tag.getFirst(FieldKey.TITLE));
					jsonCurrent.element("year", Integer.parseInt(tag.getFirst(FieldKey.YEAR)));
					jsonCurrent.element("track", tag.getFirst(FieldKey.TRACK));
					
					jsonCurrent.element("sucess", true);
					
					// Now let's do S3 Processing
					// Let's build a new key
					String key = new StringBuilder().append(tag.getFirst(FieldKey.ARTIST))
							.append("_").append(tag.getFirst(FieldKey.ALBUM))
							.append("_").append(tag.getFirst(FieldKey.TITLE))
							.append(".").append(meta.getAudioHeader().getEncodingType())
							.toString().replaceAll("\\W+", "_");
					
					DbManager db = null;
					int itemId;
					// Now we are going to insert a new entry into the database first
					try {
						db = DbManager.getInstance(context);
						itemId = db.insertItem(user.getUserId(), 
								tag.getFirst(FieldKey.TITLE), 
								tag.getFirst(FieldKey.ARTIST),
								tag.getFirst(FieldKey.ALBUM), 
								Integer.parseInt(tag.getFirst(FieldKey.YEAR)), 
								key);
						jsonCurrent.element("itemId", itemId);
						
					} catch (SQLException e) {
						jsonCurrent.element("sucess", false);
						jsonCurrent.element("exception", ExceptionUtils.getStackTrace(e));
						jsonCurrent.element("errorFriendly", "Error performing database operations.");
						continue;
					}
					
					
					S3Object s3Object = null;
					try {
						s3Object = new S3Object(file);
					} catch (NoSuchAlgorithmException e) {
						// .. Gonna ignore this, for now. Thrown when the JVM doens't support MD5. Unlikely.
						continue;
					}
					s3Object.setKey(key);
					
					// Now let's put the file to S3
					try {
						s3Object = s3Service.putObject(user.getUserBucket(), s3Object);
					} catch (S3ServiceException e) {
						jsonCurrent.element("sucess", false);
						jsonCurrent.element("exception", ExceptionUtils.getStackTrace(e));
						jsonCurrent.element("errorFriendly", "Unable to store file in cloud");
						
						try {
							db.deleteItem(itemId);
						} catch (SQLException e1) {
							// Seriously? so Fucked up?
							jsonCurrent.element("exception", ExceptionUtils.getStackTrace(e1));
							continue;
						} 
						continue;
					}
					// .. all good.
					json.accumulate("files", jsonCurrent);
					file.delete();
				}
			}
		return json;
	}
}
