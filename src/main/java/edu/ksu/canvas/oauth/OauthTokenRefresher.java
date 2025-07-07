package edu.ksu.canvas.oauth;

import com.google.gson.Gson;
import edu.ksu.canvas.impl.GsonResponseParser;
import org.apache.hc.client5.http.ContextBuilder;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class OauthTokenRefresher implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(OauthTokenRefresher.class);

    private static final Long TIMEOUT_SECONDS = 10L;
    private final String clientId;
    private final String clientSecret;
    private final String canvasUrl;

    public OauthTokenRefresher(String clientId, String clientSecret, String canvasUrl) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.canvasUrl = canvasUrl;
    }

    public TokenRefreshResponse getNewToken(String refreshToken) throws IOException, ParseException {
        LOG.debug("Getting a fresh OAuth access token");

        // request configurations
        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();

        // custom connection manager
        final PoolingHttpClientConnectionManager connManager = PoolingHttpClientConnectionManagerBuilder.create()
                .build();
        // connection configurations; validation after 10 seconds
        connManager.setDefaultConnectionConfig(ConnectionConfig.custom()
                .setConnectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build()

        );

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(connManager)
                .build();

        final HttpClientContext context = ContextBuilder.create()
                .build();

        String url = canvasUrl + "/login/oauth2/token?grant_type=refresh_token&client_id=" + clientId + "&client_secret=" + clientSecret + "&refresh_token=" + refreshToken;
        HttpPost postRequest = new HttpPost(url);

        try {
            ClassicHttpResponse httpResponse = httpClient.execute(postRequest, context, response -> {
                EntityUtils.consume(response.getEntity());
                return null;
            });
            int statusCode = httpResponse.getCode();
            if (statusCode == 401) {
                LOG.error("Unauthorized refresh token request. Wrong client_id or secret?");
                return null;
            }
            if (statusCode != 200) {
                LOG.error("Non-200 status code ( {} )returned while requesting an access token at URL {}", statusCode, url);
                HttpEntity errorEntity = httpResponse.getEntity();
                if (errorEntity != null) {
                    String errorBody = EntityUtils.toString(errorEntity);
                    LOG.error("Response from Canvas: {}", errorBody);
                }
                return null;
            }
            HttpEntity entity = httpResponse.getEntity();
            String responseBody = EntityUtils.toString(entity);
            Gson gson = GsonResponseParser.getDefaultGsonParser(false);
            return gson.fromJson(responseBody, TokenRefreshResponse.class);
        } finally {
            httpClient.close();
        }
    }
}
