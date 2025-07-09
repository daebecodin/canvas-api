package edu.ksu.canvas;

import edu.ksu.canvas.net.SimpleRestClientUTest;
import edu.ksu.canvas.util.JsonTestUtil;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.testing.classic.ClassicTestServer;
import org.junit.After;
import org.junit.Before;

import java.util.Map;

/**
 *
 * Base test class for classes that need a LocalTestServer to simulate
 * network traffic
 */
public class LocalServerTestBase {
    protected String baseUrl;
    protected URIScheme scheme;
    protected HttpHost httpHost;
    protected ClassicTestServer server;

    public LocalServerTestBase(final URIScheme scheme) {
        this.scheme = scheme;
    }

    @Before
    public void setUp() throws Exception {
        // Don't start server here - it will be started in registerUrlResponse
        // This allows each test to have a fresh server instance
    }

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.shutdown(CloseMode.GRACEFUL);
        }
    }

    protected void registerUrlResponse(String url, String sampleJsonFileName, Integer statusCode, Map<String, String> headers) throws Exception {
        String jsonContent = JsonTestUtil.loadJson(sampleJsonFileName, SimpleRestClientUTest.class);
        
        // Stop server if it exists to register new handlers
        if (server != null) {
            try {
                server.shutdown(CloseMode.GRACEFUL);
            } catch (Exception e) {
                // Ignore shutdown errors
            }
        }
        
        // Create new server instance for each test
        server = new ClassicTestServer();
        server.register(url, (request, response, context) -> {
            response.setCode(statusCode);
            for (String key : headers.keySet()) {
                String value = headers.get(key);
                response.addHeader(key, value);
            }
            response.setEntity(new StringEntity(jsonContent));
        });
        
        // Start server and update base URL
        server.start();
        httpHost = new HttpHost(scheme.id, "localhost", server.getPort());
        baseUrl = scheme.id + "://localhost:" + server.getPort();
    }

}
