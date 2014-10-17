/*
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teiid.authoring.backend.server.servlets;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.teiid.authoring.backend.server.services.TeiidService;
import org.teiid.authoring.share.Constants;
import org.teiid.authoring.share.exceptions.DataVirtUiException;

/**
 * A standard servlet that makes it easy to download VDB content.
 *
 * @author mdrillin@redhat.com
 */
public class DataVirtDownloadServlet extends HttpServlet {

	private static final long serialVersionUID = DataVirtDownloadServlet.class.hashCode();

    @Inject
    protected TeiidService teiidService;

    /**
	 * Constructor.
	 */
	public DataVirtDownloadServlet() {
	}

	/**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
			IOException {
        HttpServletResponse httpResponse = resp;
		try {
	        String vdbName = req.getParameter("vdbname"); //$NON-NLS-1$
	        String jarName = req.getParameter("jarname"); //$NON-NLS-1$
			
	        if(vdbName!=null && !vdbName.isEmpty()) {
	    		String vdbXml = teiidService.getVdbXml(vdbName);

				doDownloadVdb(httpResponse, vdbName + Constants.DYNAMIC_VDB_SUFFIX, new ByteArrayInputStream(vdbXml.getBytes("UTF-8")));
	        } else if(jarName!=null && !jarName.isEmpty()) {
				doDownloadJar(httpResponse);
	        }
			
		} catch (Exception e) {
			// TODO throw sensible errors (http responses - 404, 500, etc)
			throw new ServletException(e);
		}
	}
	
    protected void doDownloadVdb(HttpServletResponse httpResponse, String fileName, InputStream inputStream) throws Exception {
        try {
            // Set the content-disposition
            String disposition = String.format("attachment; filename=\"%1$s\"", fileName); //$NON-NLS-1$
            httpResponse.setHeader("Content-Disposition", disposition); //$NON-NLS-1$

            // Set the content-type
            httpResponse.setHeader("Content-Type", "application/xml"); //$NON-NLS-1$

            // Make sure the browser doesn't cache it
            Date now = new Date();
            httpResponse.setDateHeader("Date", now.getTime()); //$NON-NLS-1$
            httpResponse.setDateHeader("Expires", now.getTime() - 86400000L); //$NON-NLS-1$
            httpResponse.setHeader("Pragma", "no-cache"); //$NON-NLS-1$ //$NON-NLS-2$
            httpResponse.setHeader("Cache-control", "no-cache, no-store, must-revalidate"); //$NON-NLS-1$ //$NON-NLS-2$

            IOUtils.copy(inputStream, httpResponse.getOutputStream());
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }
    
    protected void doDownloadJar(HttpServletResponse httpResponse) throws Exception {
    	FileInputStream fileInputStream = null;  
    	
    	// Access the teiid jdbc jar in the jboss directory structure
    	String homeDir = System.getProperty("jboss.home.dir");
    	
        try {
        	File jarFile = new File(homeDir+"/dataVirtualization/jdbc/teiid-8.4.1-redhat-7-jdbc.jar");

        	try {
        		fileInputStream = new FileInputStream(jarFile);    	
        	} catch (Exception e) {
        		throw new DataVirtUiException(e.getMessage());
        	}
            
            // Set the content-disposition
            String disposition = String.format("attachment; filename=\"%1$s\"", "teiid-8.4.1-jdbc.jar"); //$NON-NLS-1$
            httpResponse.setHeader("Content-Disposition", disposition); //$NON-NLS-1$

            // Set the content-type
            httpResponse.setHeader("Content-Type", "application/java-archive"); //$NON-NLS-1$

            // Make sure the browser doesn't cache it
            Date now = new Date();
            httpResponse.setDateHeader("Date", now.getTime()); //$NON-NLS-1$
            httpResponse.setDateHeader("Expires", now.getTime() - 86400000L); //$NON-NLS-1$
            httpResponse.setHeader("Pragma", "no-cache"); //$NON-NLS-1$ //$NON-NLS-2$
            httpResponse.setHeader("Cache-control", "no-cache, no-store, must-revalidate"); //$NON-NLS-1$ //$NON-NLS-2$

            IOUtils.copy(fileInputStream, httpResponse.getOutputStream());
        } finally {
            IOUtils.closeQuietly(fileInputStream);
        }
    }

}
