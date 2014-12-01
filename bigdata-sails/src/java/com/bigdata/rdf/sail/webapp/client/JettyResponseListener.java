/**
Copyright (C) SYSTAP, LLC 2014.  All rights reserved.

Contact:
     SYSTAP, LLC
     4501 Tower Road
     Greensboro, NC 27410
     licenses@bigdata.com

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package com.bigdata.rdf.sail.webapp.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;

public class JettyResponseListener extends InputStreamResponseListener {
	
    private static final transient Logger log = Logger
            .getLogger(JettyResponseListener.class);
    
	Response m_response;
	
	private void ensureResponse() {
		if (m_response == null) {
			try {
				final boolean traceEnabled = log.isTraceEnabled();
				
				final long start = traceEnabled ? System.currentTimeMillis() : 0;
				
				m_response = get(300, TimeUnit.SECONDS); // wait up to 5 minutes!
				
				if (traceEnabled)
					log.trace("Response in " + (System.currentTimeMillis()-start) + "ms");
			} catch (InterruptedException | TimeoutException
					| ExecutionException e) {
				throw new RuntimeException(e);
			}
		}		
	}
	
	public String getContentType() {
		ensureResponse();
		
		final HttpFields headers = m_response.getHeaders();
		
		final String[] contentSpec = headers.get(HttpHeader.CONTENT_TYPE).split(";");
		
		return contentSpec[0];		
	}
	
	public String getContentEncoding() {
		ensureResponse();
				
		final HttpFields headers = m_response.getHeaders();
		
		final String[] contentSpec = headers.get(HttpHeader.CONTENT_TYPE).split(";");
		
		// charset=
		return contentSpec[contentSpec.length-1];		
	}

	public int getStatus() {
		ensureResponse();
		
		return m_response.getStatus();
	}

	public String getReason() {
		ensureResponse();
		
		return null;
	}

	public HttpFields getHeaders() {
		ensureResponse();
		
		return m_response.getHeaders();
	}

	public String getResponseBody() throws IOException {
        final Reader r = new InputStreamReader(getInputStream());

        try {

            final StringWriter w = new StringWriter();

            int ch;
            while ((ch = r.read()) != -1) {
                w.append((char) ch);
            }

            return w.toString();

        } finally {

            r.close();

        }
	}

    public void consume()
            throws IOException {
    	
        final InputStream r = getInputStream();

        try {

            final byte[] data = new byte[4096];

            int ch;
            while ((ch = r.read(data)) != -1) {
            	if (log.isTraceEnabled())
            		log.trace("Read " + ch + " bytes");
            }


        } finally {

            r.close();

        }

    }

}
