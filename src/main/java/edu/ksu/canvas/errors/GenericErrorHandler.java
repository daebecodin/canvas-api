package edu.ksu.canvas.errors;

import com.google.gson.Gson;
import edu.ksu.canvas.exception.CanvasException;
import edu.ksu.canvas.impl.GsonResponseParser;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * The error handler that should be used when creation of a Canvas object fails.
 */
public class GenericErrorHandler implements ErrorHandler {

    private final Pattern coursePattern = Pattern.compile("/api/v1/accounts/(\\d+|\\w+:\\w+)/courses");
    private final Pattern loginPattern = Pattern.compile("/api/v1/accounts/\\d+/logins");

    @Override
    public boolean shouldHandle(HttpRequest httpRequest, ClassicHttpResponse httpResponse) throws ProtocolException {
        String contentType = httpResponse.getFirstHeader("Content-Type") != null ? 
            httpResponse.getFirstHeader("Content-Type").getValue() : null;
        try {
            String path = httpRequest.getUri().getPath();
            return (coursePattern.matcher(path).find() ||
                    loginPattern.matcher(path).find()) &&
                    httpResponse.getCode() == 400 &&
                    contentType != null && contentType.contains("application/json");
        } catch (URISyntaxException e) {
            return false;
        }
    }

    @Override
    public void handle(HttpRequest httpRequest, ClassicHttpResponse httpResponse) {
        Gson gson = GsonResponseParser.getDefaultGsonParser(false);
        try {
            String entityString = EntityUtils.toString(httpResponse.getEntity());
            GenericErrorResponse response = gson.fromJson(entityString, GenericErrorResponse.class);
            if (response.getErrors() != null) {
                try {
                    throw new CanvasException("Failed to create user.", httpRequest.getUri().toString(), response);
                } catch (URISyntaxException e) {
                    throw new CanvasException("Failed to create user due to invalid URI", null, response);
                }
            }
        } catch (ParseException | IOException e) {
            // Ignore and allow other handlers to process
        }
    }
}