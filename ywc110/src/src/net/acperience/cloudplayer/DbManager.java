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
	private PreparedStatement insertItem = null;
	private static final String insertItemSQL = "INSERT INTO CloudItems VALUES(DEFAULT, ?, ?, ?, ?, ? ,?) RETURNING ItemId;";
	private PreparedStatement listItem = null;
	private static final String listItemSQL = "SELECT * FROM CloudItems WHERE UserId = ?;";
	private PreparedStatement deleteItem = null;
	private static final String deleteItemSQL = "DELETE FROM CloudItems WHERE ItemId = ?;";
	
	// Create a connection and populate prepared statements for performance reasons
	private DbManager(String uri, String user, String pass) throws SQLException{
		connection = DriverManager.getConnection(uri,user,pass);
		insertItem = connection.prepareStatement(insertItemSQL);
		listItem = connection.prepareStatement(listItemSQL);
		deleteItem = connection.prepareStatement(deleteItemSQL);
	}
	
	/**
	 * Insert an item into the table
	 * @param userId
	 * @param itemTitle
	 * @param itemArtist
	 * @param itemAlbum
	 * @param itemYear
	 * @param itemKey
	 * @return The ItemId of the new row created
	 * @throws SQLException 
	 */
	public int insertItem(String userId, String itemTitle, String itemArtist, String itemAlbum, int itemYear, String itemKey) 
			throws SQLException {
		insertItem.setString(1, userId);
		insertItem.setString(2, itemTitle);
		insertItem.setString(3, itemArtist);
		insertItem.setString(4, itemAlbum);
		insertItem.setInt(4, itemYear);
		insertItem.setString(5, itemKey);
		
		ResultSet result = insertItem.executeQuery();
		return result.getInt(0);
	}
	
	/**
	 * Delete an item
	 * @param itemId
	 * @throws SQLException
	 */
	public void deleteItem(int itemId) throws SQLException{
		deleteItem.setInt(1, itemId);
		deleteItem.executeUpdate();
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
