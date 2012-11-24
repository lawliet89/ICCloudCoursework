/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ds.coursework.service;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.bigdatapro.service.TransferManager;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;

/**
 *
 * @author liguo
 */
public class MusicManagementService extends HttpServlet
{

    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    private final String CREATEPLAYLIST = "createplaylist";
    private final String MUSICLISTS = "fetchmusiclists";
    private final String DELETEPLAYLIST = "deleteplaylist";
//    private final String UPLOADMUSIC = "uploadmusic";
    private final String DELETEMUSIC = "deletemusic";
    private final String PLAYLISTS = "fetchplaylists";
    private final String DOWNLOADMUSIC = "downloadmusic";
    private AWSCredentials s3Credentials;
    private RestS3Service s3Service;

    public MusicManagementService()
    {
        try
        {
            s3Credentials = TransferManager.loadKeys();

            // To communicate with S3 use the RestS3Service.
            s3Service = TransferManager.connectToS3(s3Credentials);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try
        {
            String methodType = request.getParameter("action"); //get action value from the URL

            if (methodType.equalsIgnoreCase(PLAYLISTS))
            {
                // A good test to see if your S3Service can connect to S3 is to list all the buckets you own.
                // Fetching all the buckets that you own and return them as a JSON array for client to process

                S3Bucket[] myBuckets = TransferManager.listAllBuckets(s3Service); //get all buckets
                JSONArray jarray = new JSONArray();
                for (S3Bucket bucket : myBuckets)
                {
                    jarray.add(bucket.getName());  //put bucket list into JSON array
                }
                out.println(jarray.toString());
            } else if (methodType.equalsIgnoreCase(CREATEPLAYLIST))
            {
                String bucketName = request.getParameter("bucketname");
                S3Bucket myBucket = TransferManager.createBucket(s3Service, bucketName); //get all buckets
                if (myBucket == null)
                {
                    JSONObject jObj = new JSONObject();
                    jObj.put("result", "error");
                    out.println(jObj.toString());
                } else
                {
                    JSONObject jObj = new JSONObject();
                    jObj.put("result", "playlistcreated");
                    out.println(jObj.toString());
                }
            } else if (methodType.equalsIgnoreCase(DELETEPLAYLIST))
            {
                //Leave to the students to complete
            } else if (methodType.equalsIgnoreCase(DELETEMUSIC))
            {
                //Leave to the students to complete
            } else if (methodType.equalsIgnoreCase(MUSICLISTS))
            {
                String playListName = request.getParameter("playlistname");
                S3Object[] s3Objects = TransferManager.listAllObjects(s3Service, playListName);
                JSONArray jArray = new JSONArray();
                for (S3Object s3Object : s3Objects)
                {
                    JSONObject musicJson = new JSONObject();
                    musicJson.put("musicname", s3Object.getName());

                    //Think about what more information you will need on your music player side for each individual music file?
                    jArray.add(musicJson);
                }
                out.println(jArray.toString());
            }

        } finally
        {
            out.close();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        processRequest(request, response);
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
            throws ServletException, IOException
    {
        processRequest(request, response);
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo()
    {
        return "Short description";
    }// </editor-fold>
}
