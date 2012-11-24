/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ds.coursework.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.bigdatapro.service.TransferManager;
import org.bigdatapro.service.TransferServer;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;

/**
 *
 * @author liguo
 */
public class MusicStreamServer extends HttpServlet
{

    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        response.setContentType("audio/mpeg3");
        response.reset();
        OutputStream out = response.getOutputStream();

        AWSCredentials s3Credentials = TransferManager.loadKeys();
        String bucketName = request.getParameter("bucketname");
        String musicFileName = request.getParameter("musicname");
        RestS3Service s3Service = TransferManager.connectToS3(s3Credentials);
        S3Object downloadedObject = TransferServer.downloadObject(s3Service, bucketName, musicFileName);


        // Read the data from the object's DataInputStream using a loop, and stream it out to your music player.

        int BUFF_SIZE = 1024;
        byte[] buffer = new byte[BUFF_SIZE];
        InputStream is = null;
        try
        {
            is = downloadedObject.getDataInputStream();  //plug the S3 obect data input stream to the servlet outputstream, so no local cache on the stream server side is done
            do
            {
                int byteCount = is.read(buffer);
                if (byteCount == -1)
                {
                    break;
                }
                out.write(buffer, 0, byteCount);
                out.flush();
            } while (true);
        } catch (Exception excp)
        {
            excp.printStackTrace();
        } finally
        {
            out.close();
            is.close();
        }

        downloadedObject.closeDataInputStream();
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
            throws ServletException,
            IOException
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
            throws ServletException,
            IOException
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
