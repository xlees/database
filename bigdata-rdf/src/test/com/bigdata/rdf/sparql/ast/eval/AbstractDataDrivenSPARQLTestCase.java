/**

Copyright (C) SYSTAP, LLC 2006-2011.  All rights reserved.

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
/*
Portions of this code are:

Copyright Aduna (http://www.aduna-software.com/) � 2001-2007

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the copyright holder nor the names of its contributors
      may be used to endorse or promote products derived from this software
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/
/*
 * Created on Sep 5, 2011
 */

package com.bigdata.rdf.sparql.ast.eval;

import info.aduna.iteration.Iterations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.algebra.evaluation.QueryBindingSet;
import org.openrdf.query.dawg.DAWGTestResultSetUtil;
import org.openrdf.query.impl.TupleQueryResultBuilder;
import org.openrdf.query.resultio.BooleanQueryResultFormat;
import org.openrdf.query.resultio.BooleanQueryResultParserRegistry;
import org.openrdf.query.resultio.QueryResultIO;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultParser;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParser.DatatypeHandling;
import org.openrdf.rio.RDFParserFactory;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.helpers.StatementCollector;

import com.bigdata.bop.engine.AbstractQueryEngineTestCase;
import com.bigdata.rdf.model.StatementEnum;
import com.bigdata.rdf.rio.StatementBuffer;
import com.bigdata.rdf.sail.sparql.Bigdata2ASTSPARQLParser;
import com.bigdata.rdf.sparql.ast.ASTContainer;
import com.bigdata.rdf.sparql.ast.AbstractASTEvaluationTestCase;
import com.bigdata.rdf.sparql.ast.QueryRoot;
import com.bigdata.rdf.store.AbstractTripleStore;

/**
 * Abstract base class for data driven test suites.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: AbstractDataDrivenSPARQLTestCase.java 5130 2011-09-05 16:54:20Z
 *          thompsonbry $
 * 
 *          TODO Add tests for triples-only and sids modes.
 * 
 *          TODO Support manifest driven test suite.
 */
public class AbstractDataDrivenSPARQLTestCase extends
        AbstractASTEvaluationTestCase {

    private static final Logger log = Logger
            .getLogger(AbstractDataDrivenSPARQLTestCase.class);
    
    /**
     * 
     */
    public AbstractDataDrivenSPARQLTestCase() {
    }

    /**
     * @param name
     */
    public AbstractDataDrivenSPARQLTestCase(String name) {
        super(name);
    }

    /**
     * Data-driven unit tests for SPARQL queries.
     * <p>
     * Note: This class was derived from the openrdf SPARQLQueryTest file (Aduna
     * BSD style license).
     */
    public class TestHelper {

        private final String testURI;
        
        private final String queryFileURL;
        private final String[] dataFileURLs;
        private final String resultFileURL;

        private final boolean laxCardinality;
        private final boolean checkOrder;
        
        private final String queryStr;

        private final ASTContainer astContainer;
//        private final AST2BOpContext context;
       
//        private final PipelineOp queryPlan;

        public ASTContainer getASTContainer() {
            
            return astContainer;
            
        }
        
        public AbstractTripleStore getTripleStore() {
            
            return store;
            
        }
        
        /**
         * 
         * @param testURI
         * @throws Exception
         * 
         */
        public TestHelper(final String testURI) throws Exception {
            
            this(testURI, testURI + ".rq", testURI + ".trig", testURI + ".srx");
            
        }

        /**
         * 
         * @param testURI
         * @param queryFileURL
         * @param dataFileURL
         * @param resultFileURL
         * @throws Exception
         */
        public TestHelper(final String testURI, final String queryFileURL,
                final String dataFileURL, final String resultFileURL)
                throws Exception {
            
            this(testURI, queryFileURL, dataFileURL, resultFileURL,
                    false/* laxCardinality */, false/* checkOrder */);
            
        }

        public TestHelper(final String testURI, final String queryFileURL,
                final String[] dataFileURLs, final String resultFileURL)
                throws Exception {
            
            this(testURI, queryFileURL, dataFileURLs, resultFileURL,
                    false/* laxCardinality */, false/* checkOrder */);
            
        }

        public TestHelper(final String testURI, final String queryFileURL,
                final String dataFileURL, final String resultFileURL,
                final boolean laxCardinality, final boolean checkOrder)
                throws Exception {
            
            this(testURI, queryFileURL, new String[] { dataFileURL },
                    resultFileURL, laxCardinality, checkOrder);

        }
        
        /**
         * Read the query and load the data file(s) but do not run the query.
         * 
         * @param testURI
         * @param queryFileURL
         * @param dataFileURLs
         * @param resultFileURL
         * @param laxCardinality
         * @param checkOrder
         * @throws Exception
         */
        public TestHelper(final String testURI, final String queryFileURL,
                final String[] dataFileURLs, final String resultFileURL,
                final boolean laxCardinality, final boolean checkOrder)
                throws Exception {

            if (log.isInfoEnabled())
                log.info("\ntestURI:\n" + testURI);

            this.testURI = testURI;
            this.queryFileURL = queryFileURL;
            this.dataFileURLs = dataFileURLs;
            this.resultFileURL = resultFileURL;
            this.laxCardinality = laxCardinality;
            this.checkOrder = checkOrder;

            this.queryStr = getResourceAsString(queryFileURL);

            if (log.isInfoEnabled())
                log.info("\nquery:\n" + queryStr);

            if (dataFileURLs != null) {

                for (String dataFileURL : dataFileURLs) {

                    final long nparsed = loadData(dataFileURL);

                    if (log.isInfoEnabled())
                        log.info("\nLoaded " + nparsed + " statements from "
                                + dataFileURL);
                }
                
            }

            /**
             * Note: This should be the URL specified in the manifest as having
             * the appropriate scope for the value of the qt:query attribute,
             * which is normally specified as something like:
             * 
             * <pre>
             * <dataset-01.rq>
             * </pre>
             * 
             * and hence interpreted as relative to the baseURI.
             */
            final String baseURI = "http://www.w3.org/2001/sw/DataAccess/tests/data-r2/dataset/manifest#"
                    + queryFileURL;

            /*
             * FIXME This winds up using the UNISOLATED view to issue the
             * queries since the [store] reference is the one that we used to
             * load the data. The test harness lacks a means to distinguish a
             * read-only query from an update. That could be captured by an
             * extension of the manifest. We could also capture the concept of
             * the triple/sids/quads mode distinction and even other
             * configuration properties for the KB instance.
             */
            astContainer = new Bigdata2ASTSPARQLParser(store).parseQuery2(
                    queryStr, baseURI);

//            queryPlan = AST2BOpUtility.convert(context = new AST2BOpContext(
//                    astContainer, store));
//
//            if (log.isInfoEnabled())
//                log.info(astContainer);
//
//            if (log.isInfoEnabled())
//                log.info("\n"+BOpUtility.toString(queryPlan));

        }

        /**
         * Run a data-driven SPARQL test.
         * 
         * @param resource
         *            The base name of the resource. It will locate the query
         *            (.rq), the data (.trig), and the expected resources (.srx)
         *            using this base name.
         * 
         * @throws Exception
         *             if the resources can not be found, if the query does not
         *             produce the expected results, etc.
         */
        public ASTContainer runTest() throws Exception {

            final QueryRoot queryRoot = astContainer.getOriginalAST();
            
            switch (queryRoot.getQueryType()) {
            case SELECT: {

                final TupleQueryResult expectedResult = readExpectedTupleQueryResult();

//                final IVariable<?>[] projected = queryRoot.getProjection()
//                        .getProjectionVars();

                final TupleQueryResult queryResult = ASTEvalHelper
                        .evaluateTupleQuery(store, astContainer,
                                new QueryBindingSet());

//                final TupleQueryResult queryResult = ASTEvalHelper
//                        .evaluateTupleQuery(store, queryPlan,
//                                new QueryBindingSet(), context.queryEngine,
//                                projected);

                compareTupleQueryResults(queryResult, expectedResult);

                break;
            }
            case DESCRIBE:
            case CONSTRUCT: {
                
                final Set<Statement> expectedResult = readExpectedGraphQueryResult();

                final GraphQueryResult gqr = ASTEvalHelper.evaluateGraphQuery(
                        store, astContainer, new QueryBindingSet());
                
//                final GraphQueryResult gqr = ASTEvalHelper.evaluateGraphQuery(
//                        store, //
//                        queryPlan, //
//                        new QueryBindingSet(),//
//                        context.queryEngine, //
//                        queryRoot.getProjection().getProjectionVars(),
//                        queryRoot.getPrefixDecls(), //
//                        queryRoot.getConstruct()//
//                        );

                final Set<Statement> queryResult = Iterations.asSet(gqr);

                compareGraphs(queryResult, expectedResult);
                
                break;
                
            }
            case ASK: {

                final boolean queryResult = ASTEvalHelper.evaluateBooleanQuery(
                        store, astContainer, new QueryBindingSet());

//                final boolean queryResult = ASTEvalHelper.evaluateBooleanQuery(
//                        store, queryPlan, new QueryBindingSet(),
//                        context.queryEngine);
                
                final boolean expectedResult = readExpectedBooleanQueryResult();
                
                assertEquals(expectedResult, queryResult);
                
                break;
                
            }
            default:
                throw new RuntimeException("Unknown query type: "
                        + queryRoot.getQueryType());
            }

            return astContainer;
            
        }

        public TupleQueryResult readExpectedTupleQueryResult() throws Exception {

            final TupleQueryResultFormat tqrFormat = QueryResultIO
                    .getParserFormatForFileName(resultFileURL);

            if (tqrFormat != null) {
                
                final InputStream in = getResourceAsStream(resultFileURL);
                
                try {
                
                    final TupleQueryResultParser parser = QueryResultIO
                            .createParser(tqrFormat);
                    
                    parser.setValueFactory(store.getValueFactory());

                    final TupleQueryResultBuilder qrBuilder = new TupleQueryResultBuilder();
                    
                    parser.setTupleQueryResultHandler(qrBuilder);

                    parser.parse(in);
                    
                    return qrBuilder.getQueryResult();
                    
                } finally {
                    
                    in.close();
                    
                }

            } else {

                final Set<Statement> resultGraph = readExpectedGraphQueryResult();

                return DAWGTestResultSetUtil.toTupleQueryResult(resultGraph);

            }

        }

        public boolean readExpectedBooleanQueryResult() throws Exception {
            
            final BooleanQueryResultFormat bqrFormat = BooleanQueryResultParserRegistry
                    .getInstance().getFileFormatForFileName(resultFileURL);

            if (bqrFormat != null) {

                final InputStream in = getResourceAsStream(resultFileURL);
                
                try {
                
                    return QueryResultIO.parse(in, bqrFormat);
                    
                } finally {
                    
                    in.close();
                    
                }

            } else {

                final Set<Statement> resultGraph = readExpectedGraphQueryResult();
                
                return DAWGTestResultSetUtil.toBooleanQueryResult(resultGraph);
                
            }

        }

        /**
         * Set lazily by {@link #readExpectedGraphQueryResult()}.
         */
        public Set<Statement> expectedGraphQueryResult = null;

        public Set<Statement> readExpectedGraphQueryResult() throws Exception {

            if (expectedGraphQueryResult != null)
                return expectedGraphQueryResult;
            
            final RDFFormat rdfFormat = Rio
                    .getParserFormatForFileName(resultFileURL);

            if (rdfFormat != null) {

                final RDFParser parser = Rio.createParser(rdfFormat);
                parser.setDatatypeHandling(DatatypeHandling.IGNORE);
                parser.setPreserveBNodeIDs(true);
                parser.setValueFactory(store.getValueFactory());

                final Set<Statement> result = new LinkedHashSet<Statement>();
                
                parser.setRDFHandler(new StatementCollector(result));

                final InputStream in = getResourceAsStream(resultFileURL);
                
                try {
                
                    parser.parse(in, resultFileURL);
                    
                } finally {
                    
                    in.close();
                    
                }
                
                expectedGraphQueryResult = result;

                return result;
                
            } else {

                throw new RuntimeException(
                        "Unable to determine file type of results file: "
                                + resultFileURL);

            }
            
        }

		private void compareTupleQueryResults(
				final TupleQueryResult queryResult,
				final TupleQueryResult expectedResult)
				throws QueryEvaluationException {

			AbstractQueryEngineTestCase.compareTupleQueryResults(getName(),
					testURI, store, astContainer, queryResult, expectedResult,
					laxCardinality, checkOrder);

		}

		public void compareGraphs(final Set<Statement> queryResult,
				final Set<Statement> expectedResult) {

			AbstractQueryEngineTestCase.compareGraphs(getName(), queryResult,
					expectedResult);
		}

		protected InputStream getResourceAsStream(final String resource) {

            // try the classpath
            InputStream is = getClass().getResourceAsStream(resource);

            if (is == null) {

                // Searching for the resource from the root of the class
                // returned
                // by getClass() (relative to the class' package) failed.
                // Next try searching for the desired resource from the root
                // of the jar; that is, search the jar file for an exact match
                // of the input string.
                is = getClass().getClassLoader().getResourceAsStream(resource);

            }

            if (is == null) {

                final File file = new File(resource);

                if (file.exists()) {

                    try {

                        is = new FileInputStream(resource);

                    } catch (FileNotFoundException e) {

                        throw new RuntimeException(e);

                    }

                }

            }

            if (is == null) {

                try {

                    is = new URL(resource).openStream();

                } catch (MalformedURLException e) {

                    /*
                     * Ignore. we will handle the problem below if this was not
                     * a URL.
                     */

                } catch (IOException e) {

                    throw new RuntimeException(e);

                }

            }

            if (is == null)
                throw new RuntimeException("Not found: " + resource);

            return is;

        }

        /**
         * Return the contents of the resource.
         * 
         * @param resource
         *            The resource.
         * 
         * @return It's contents.
         */
        protected String getResourceAsString(final String resource) {

            final StringBuilder sb = new StringBuilder();

            final InputStream is = getResourceAsStream(resource);

            if (is == null)
                throw new RuntimeException("Not found: " + resource);

            try {

                final LineNumberReader r = new LineNumberReader(
                        new InputStreamReader(is));

                String s;
                while ((s = r.readLine()) != null) {

                    sb.append(s);

                    sb.append("\n");

                }

                return sb.toString();

            } catch (IOException e) {

                throw new RuntimeException(e);

            } finally {

                try {

                    if (is != null)
                        is.close();

                } catch (IOException e) {

                    throw new RuntimeException(e);

                }

            }

        }

        /**
         * Load some RDF data.
         * 
         * @param resource
         *            The resource whose data will be loaded.
         * 
         * @return The #of statements parsed from the source. If there are
         *         duplicate told statements, then there may be fewer statements
         *         written onto the KB.
         */
        protected long loadData(final String resource) {

            final RDFFormat rdfFormat = RDFFormat.forFileName(resource);

            final RDFParserFactory rdfParserFactory = RDFParserRegistry
                    .getInstance().get(rdfFormat);

            final RDFParser rdfParser = rdfParserFactory.getParser();

            rdfParser.setValueFactory(store.getValueFactory());

            rdfParser.setVerifyData(true);

            rdfParser.setStopAtFirstError(true);

            rdfParser.setDatatypeHandling(RDFParser.DatatypeHandling.IGNORE);

            final AddStatementHandler handler = new AddStatementHandler();

            handler.setContext(new URIImpl(new File(resource).toURI().toString()));
            
            rdfParser.setRDFHandler(handler);
                        
            /*
             * Run the parser, which will cause statements to be inserted.
             */

            final InputStream is = getResourceAsStream(resource);

            try {

                rdfParser.parse(is, baseURI);

                return handler.close();

            } catch (Exception e) {

                throw new RuntimeException(e);

            } finally {

                try {

                    is.close();

                } catch (IOException e) {

                    throw new RuntimeException(e);

                }

            }

        }

        /**
         * Helper class adds statements to the sail as they are visited by a
         * parser.
         */
        private class AddStatementHandler extends RDFHandlerBase {

            private final StatementBuffer<Statement> buffer;

            private Resource context = null;
            
            private long n = 0L;

            public AddStatementHandler() {

                buffer = new StatementBuffer<Statement>(store, 100/* capacity */);

            }

            public void setContext(final Resource context) {

                this.context = context;
                
            }
            
            public void handleStatement(final Statement stmt)
                    throws RDFHandlerException {

                final Resource s = stmt.getSubject();
                final URI p = stmt.getPredicate();
                final Value o = stmt.getObject();
                final Resource c = stmt.getContext() == null ? this.context
                        : stmt.getContext();

                if (log.isDebugEnabled())
                    log.debug("<" + s + "," + p + "," + o + "," + c + ">");

                buffer.add(s, p, o, c, StatementEnum.Explicit);

                n++;

            }

            /**
             * 
             * @return The #of statements visited by the parser.
             */
            public long close() {

                buffer.flush();

                return n;

            }

        }

    }

}
