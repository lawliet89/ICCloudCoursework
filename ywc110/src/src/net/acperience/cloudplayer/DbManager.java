package net.acperience.cloudplayer;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

import javax.servlet.http.HttpServlet;

/*
 *	One instance of DbManager should be shared across all the servlets
 */
public class DbManager {
	public static final String DRIVER_CLASS = "org.postgresql.Driver";
	public static final String DEFAULT_CONF = "/WEB-INF/conf/sql.conf";
	private static DbManager instance;
	private Connection connection;
	
	// List of prepared statements for use
	private PreparedStatement insertItemStatement = null;
	private static final String insertItemSQL = "INSERT INTO Cloud_Item VALUES(DEFAULT, ?, ?, ?, ?, ? ,?, ?) RETURNING ItemId;";
	private PreparedStatement listItemStatement = null;
	private static final String listItemSQL = "SELECT * FROM Cloud_Item WHERE UserId = ? ORDER BY itemartist, itemalbum,  itemtitle;";
	private PreparedStatement getItemByKeyStatement = null;
	private static final String getItemSQL = "SELECT * FROM Cloud_Item WHERE UserId = ? AND ItemKey = ?;";
	private PreparedStatement getItemByIdStatement = null;
	private static final String getItemByIdSQ = "SELECT * FROM Cloud_Item WHERE UserId = ? AND ItemId = ?;";
	private PreparedStatement deleteItemByIdStatement = null;
	private static final String deleteItemByIdSQL = "DELETE FROM Cloud_Item WHERE UserId = ? AND ItemId = ?;";
	private PreparedStatement deleteItemByKeyStatement = null;
	private static final String deleteItemByKeySQL = "DELETE FROM Cloud_Item WHERE UserId = ? AND ItemKey = ?;";
	
	// Create a connection and populate prepared statements for performance reasons
	private DbManager(String uri, String user, String pass) throws SQLException{
		connection = DriverManager.getConnection(uri,user,pass);
		// Items Related
		insertItemStatement = connection.prepareStatement(insertItemSQL);
		listItemStatement = connection.prepareStatement(listItemSQL);
		getItemByKeyStatement = connection.prepareStatement(getItemSQL);
		getItemByIdStatement = connection.prepareStatement(getItemByIdSQ);
		deleteItemByIdStatement = connection.prepareStatement(deleteItemByIdSQL);
		deleteItemByKeyStatement = connection.prepareStatement(deleteItemByKeySQL);
	}
	
	/**
	 * Insert an item into the table. 
	 * If item with the key (i.e. same artist + title + album combination) exists, will return item
	 * @param userId
	 * @param itemTitle
	 * @param itemArtist
	 * @param itemAlbum
	 * @param itemYear
	 * @param itemKey
	 * @return The ItemId of the new row created
	 * @throws SQLException 
	 */
	public int insertItem(String userId, String itemTitle, String itemArtist, String itemAlbum, int itemYear, String itemKey, int itemDuration) 
			throws SQLException {
		
		// Try to see if the user already has something like this
		ResultSet result = null;
		result = getItemByKey(itemKey, userId);
		
		if (result.next() == true)
			return result.getInt(1);
		
		insertItemStatement.setString(1, userId);
		insertItemStatement.setString(2, itemTitle);
		insertItemStatement.setString(3, itemArtist);
		insertItemStatement.setString(4, itemAlbum);
		insertItemStatement.setInt(5, itemYear);
		insertItemStatement.setString(6, itemKey);
		insertItemStatement.setInt(7, itemDuration);
		
		result = null;
		result = insertItemStatement.executeQuery();
		result.next();
		return result.getInt(1);
	}
	
	/**
	 * Gets the results for an item based on the key
	 * @return
	 * @throws SQLException
	 */
	public ResultSet getItemByKey(String itemKey, String userId) throws SQLException{
		getItemByKeyStatement.setString(1, userId);
		getItemByKeyStatement.setString(2, itemKey);
		
		return getItemByKeyStatement.executeQuery();
	}
	
	/**
	 * Returns item details via ID
	 * @param Id
	 * @param userId
	 * @return
	 * @throws SQLException
	 */
	public ResultSet getItemById(int Id, String userId) throws SQLException{
		getItemByIdStatement.setInt(2, Id);
		getItemByIdStatement.setString(1, userId);
		return getItemByIdStatement.executeQuery();
	}
	
	/**
	 * Returns a list of items belonging to user
	 * @param userId
	 * @return
	 * @throws SQLException
	 */
	public ResultSet getItems(String userId) throws SQLException{
		listItemStatement.setString(1, userId);
		return listItemStatement.executeQuery();
	}
	
	/**
	 * Delete an item
	 * @param user 
	 * @param itemId
	 * @throws SQLException
	 */
	public void deleteItemById(MusicKerberos user, int itemId) throws SQLException{
		deleteItemByIdStatement.setString(1, user.getUserId());
		deleteItemByIdStatement.setInt(2, itemId);
		deleteItemByIdStatement.executeUpdate();
	}
	/**
	 * Delete an item
	 * @param user
	 * @param itemKey
	 * @throws SQLException
	 */
	public void deleteItemByKey(MusicKerberos user, String itemKey) throws SQLException{
		deleteItemByKeyStatement.setString(1, user.getUserId());
		deleteItemByKeyStatement.setString(2, itemKey);
		deleteItemByKeyStatement.executeUpdate();
	}
	
	/**
	 * Singleton pattern to get and/or initialise DbManager
	 * @return
	 * @throws IOException 
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 */
	public static DbManager getInstance(InputStream conf) throws IOException, SQLException{
		if (instance == null){
			Properties properties = new Properties();
			properties.load(conf);
			instance = new DbManager("dbc:postgresql://db.doc.ic.ac.uk/" + properties.getProperty("db"),
					properties.getProperty("user"),
					properties.getProperty("pass"));
		}
		
		return instance;
	}
	
	/**
	 * Convenience method to load the default configuration file.
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 */
	public static DbManager getInstance(HttpServlet context) throws IOException, SQLException{
		return getInstance(context.getServletContext().getResourceAsStream(DEFAULT_CONF));
	}
	
}
