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
	// Items management
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
	
	// Playlist Management
	private PreparedStatement listPlaylistStatement = null;
	private static final String listPlaylistSQL = "SELECT * FROM Cloud_Playlist WHERE UserId = ? ORDER BY PlaylistName;";
	private PreparedStatement insertPlaylistStatement = null;
	private static final String insertPlaylistSQL = "INSERT INTO Cloud_Playlist VALUES(DEFAULT, ?, ?) RETURNING PlaylistId;";
	private PreparedStatement getPlaylistByIdStatement = null;
	private static final String getPlaylistByIdSQL = "SELECT * FROM Cloud_Playlist WHERE UserId = ? AND PlaylistId = ?;";
	private PreparedStatement renamePlaylistByIDStatement = null;
	private static final String renamePlaylistByIDSQL = "UPDATE Cloud_Playlist SET PlaylistName = ? WHERE UserId = ? AND PlaylistId = ?";
	
	// Playlist Items
	private PreparedStatement getPlaylistItemsByIDStatement = null;
	private static final String getPlaylistItemsByIDSQL = "SELECT * FROM cloud_item NATURAL JOIN cloud_playlistitem "
			+ "NATURAL JOIN cloud_playlist WHERE cloud_item.userid = ? AND cloud_playlist.playlistid= ?;";
	private PreparedStatement getPlaylistItemsByNameStatement = null;
	private static final String getPlaylistItemsNameSQL = "SELECT * FROM cloud_item NATURAL JOIN cloud_playlistitem "
			+ "NATURAL JOIN cloud_playlist WHERE cloud_item.userid = ? AND cloud_playlist.playlistname= ?;";
	
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
		
		// Playlist Management
		listPlaylistStatement = connection.prepareStatement(listPlaylistSQL);
		insertPlaylistStatement = connection.prepareStatement(insertPlaylistSQL);
		getPlaylistByIdStatement = connection.prepareStatement(getPlaylistByIdSQL);
		renamePlaylistByIDStatement = connection.prepareStatement(renamePlaylistByIDSQL);
		
		// Playlist Items
		getPlaylistItemsByIDStatement = connection.prepareStatement(getPlaylistItemsByIDSQL);
		getPlaylistItemsByNameStatement = connection.prepareStatement(getPlaylistItemsNameSQL);
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
	 * Delete an item. Throws an SQLException if item does not exist
	 * @param user 
	 * @param itemId
	 * @throws SQLException
	 */
	public void deleteItemById(String userID, int itemId) throws SQLException{
		ResultSet test = getItemById(itemId, userID);
		if (!test.next())
			throw new SQLException("Item with ID " + Integer.toString(itemId) + " does not exist.");
		deleteItemByIdStatement.setString(1, userID);
		deleteItemByIdStatement.setInt(2, itemId);
		deleteItemByIdStatement.executeUpdate();
	}
	/**
	 * Delete an item. Throws an SQLException if item does not exist.
	 * @param user
	 * @param itemKey
	 * @throws SQLException
	 */
	public void deleteItemByKey(String userID, String itemKey) throws SQLException{
		ResultSet test = getItemByKey(itemKey, userID);
		if (!test.next())
			throw new SQLException("Item with key " + itemKey + " does not exist.");
		
		deleteItemByKeyStatement.setString(1, userID);
		deleteItemByKeyStatement.setString(2, itemKey);
		deleteItemByKeyStatement.executeUpdate();
	}
	
	/**
	 * Insert a new playlist. Returns the ID of the inserted playlist
	 * @param userId
	 * @param playlistName
	 * @return ID of the newly inserted playlist
	 * @throws SQLException
	 */
	public int insertPlaylist(String userId, String playlistName) throws SQLException{
		// Try to see if the user already has something like this
		ResultSet result;
		
		insertPlaylistStatement.setString(1, playlistName);
		insertPlaylistStatement.setString(2, userId);
		
		result = null;
		result = insertPlaylistStatement.executeQuery();
		result.next();
		return result.getInt(1);
	}
	
	/**
	 * Get Playlist by ID
	 * @param userID
	 * @param playlistId
	 * @return
	 * @throws SQLException
	 */
	public ResultSet getPlaylistById(String userID, int playlistId) throws SQLException{
		getPlaylistByIdStatement.setString(1, userID);
		getPlaylistByIdStatement.setInt(2, playlistId);
		return getPlaylistByIdStatement.executeQuery();
	}

	
	/**
	 * Rename playlist based on ID.
	 * Throws an SQLException if no such row exists.
	 * @param userID
	 * @param playlistID
	 * @param playlistName
	 * @throws SQLException Throws an SQLException if no such playlist exists.
	 */
	public void renamePlaylistByID(String userID, int playlistID, String playlistName) throws SQLException{
		ResultSet test = getPlaylistById(userID, playlistID);
		
		if (!test.next())
			throw new SQLException("Playlist with ID " + playlistID + "does not exist.");
		
		renamePlaylist(userID, playlistID, playlistName);
	}
	
	/**
	 * Internal method to rename playlist
	 * @param userID
	 * @param playlistID
	 * @param playlistName
	 * @throws SQLException
	 */
	private void renamePlaylist(String userID, int playlistID, String playlistName) throws SQLException{
		renamePlaylistByIDStatement.setString(1, playlistName);
		renamePlaylistByIDStatement.setString(2, userID);
		renamePlaylistByIDStatement.setInt(3, playlistID);
		renamePlaylistByIDStatement.execute();
	}
	
	/**
	 * Gets a list of playlists for a user
	 * @param userID
	 * @return
	 * @throws SQLException 
	 */
	public ResultSet getPlaylistsByUser(String userID) throws SQLException{
		listPlaylistStatement.setString(1, userID);
		return listPlaylistStatement.executeQuery();
	}
	
	/**
	 * Gets the list of items on a playlist
	 * @param userID
	 * @param playlistID
	 * @return
	 * @throws SQLException
	 */
	public ResultSet getPlaylistItemsByID(String userID, int playlistID) throws SQLException{
		getPlaylistItemsByIDStatement.setString(1, userID);
		getPlaylistItemsByIDStatement.setInt(2, playlistID);
		return getPlaylistItemsByIDStatement.executeQuery();
	}
	
	/**
	 * Gets the list of items of a playlist
	 * @param userID
	 * @param playlistName
	 * @return
	 * @throws SQLException
	 */
	public ResultSet getPlaylistItemsByName(String userID, String playlistName) throws SQLException{
		getPlaylistItemsByNameStatement.setString(1, userID);
		getPlaylistItemsByNameStatement.setString(2, playlistName);
		return getPlaylistItemsByNameStatement.executeQuery();
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
