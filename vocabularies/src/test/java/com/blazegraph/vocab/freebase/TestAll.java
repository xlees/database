/**

Copyright (C) SYSTAP, LLC DBA Blazegraph 2006-2016.  All rights reserved.

Contact:
     SYSTAP, LLC DBA Blazegraph
     2501 Calvert ST NW #106
     Washington, DC 20008
     licenses@blazegraph.com

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
/*
 * Created on Oct 19, 2007
 */

package com.blazegraph.vocab.freebase;

import junit.extensions.proxy.ProxyTestSuite;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.bigdata.rdf.store.TestLocalTripleStore;

/**
 * 
 * @author beebs
 */
public class TestAll extends TestCase {

    /**
     * 
     */
    public TestAll() {
    }

    /**
     * @param arg0
     */
    public TestAll(String arg0) {
        super(arg0);
    }

    /**
     * Returns a test that will run each of the implementation specific test
     * suites in turn.
     */
    public static Test suite()
    {

        final TestSuite suite = new TestSuite("FreebaseVocabulary");

        final ProxyTestSuite proxySuite = new ProxyTestSuite(new TestLocalTripleStore(),
                "Local Triple Store With Provenance Test Suite"); 
        
        proxySuite.addTest(TestFreebaseVocabInlineUris.suite());
        
        //Test handlers for Freebase MID and GUID inlining
        suite.addTestSuite(TestInlineFreebaseGUIDMIDURIHandler.class);
        
        
        suite.addTest( proxySuite);

        return suite;
        
    }
    
}
