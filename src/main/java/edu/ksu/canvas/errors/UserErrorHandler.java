package edu.ksu.canvas.errors;

import com.google.gson.Gson;
import edu.ksu.canvas.exception.CanvasException;
import edu.ksu.canvas.impl.GsonResponseParser;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * The error handler that should be used when creation of a user fails.
 */
public class UserErrorHandler implements ErrorHandler {

    private final Pattern pattern = Pattern.compile("/api/v1/accounts/\\d+/users");

    @Override
    public boolean shouldHandle(HttpRequest httpRequest, ClassicHttpResponse httpResponse) {
        try {
            String contentType = httpResponse.getFirstHeader("Content-Type") != null ? 
                httpResponse.getFirstHeader("Content-Type").getValue() : null;
            String method = httpRequest.getMethod();
            return
                    pattern.matcher(httpRequest.getUri().toString()).find()
                            && (method.equals("POST") || method.equals("GET"))
                            && httpResponse.getCode() == 400
                            && contentType != null && contentType.contains("application/json");
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void handle(HttpRequest httpRequest, ClassicHttpResponse httpResponse) {
        Gson gson = GsonResponseParser.getDefaultGsonParser(false);
        try {
            UserErrorResponse response = gson.fromJson(EntityUtils.toString(httpResponse.getEntity()), UserErrorResponse.class);
            if (response.getErrors() != null) {
                throw new CanvasException("Failed to create user.", httpRequest.getUri().toString(), response);
            }
        } catch (IOException | URISyntaxException | ParseException e) {
            // Ignore.
        }

    }


    public static String get(String url) {
        String resultContent = null;
        HttpGet httpGet = new HttpGet(url);
        try (CloseableHttpClient client = HttpClients.createDefault()){
            try (CloseableHttpResponse response = client.execute(httpGet)) {
                String version = response.getVersion().toString();
                int code = response.getCode();
                String phrase = response.getReasonPhrase();

                HttpEntity entity = response.getEntity();

                resultContent = EntityUtils.toString(entity);

            }
        } catch (IOException |ParseException e) {
            e.printStackTrace();
        }
        return resultContent;
    }
}
