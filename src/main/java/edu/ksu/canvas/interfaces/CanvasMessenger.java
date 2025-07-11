package edu.ksu.canvas.interfaces;

import com.google.gson.JsonObject;

import edu.ksu.canvas.exception.InvalidOauthTokenException;
import edu.ksu.canvas.net.Response;
import edu.ksu.canvas.oauth.OauthToken;
import org.apache.hc.core5.http.ProtocolException;

import java.io.InputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface CanvasMessenger {
    List<Response> getFromCanvas(OauthToken oauthToken, String url) throws InvalidOauthTokenException, IOException;
    List<Response> getFromCanvas(OauthToken oauthToken, String url, Consumer<Response> consumer) throws InvalidOauthTokenException, IOException;
    //TODO: Should probably make this parameter list more sane
    Response sendToCanvas(OauthToken oauthToken, String url, Map<String, List<String>> parameters) throws InvalidOauthTokenException, IOException, ProtocolException, URISyntaxException;
    Response sendFileToCanvas(OauthToken oauthToken, String url, Map<String, List<String>> parameters, String fileParameter, String filePath, InputStream is) throws InvalidOauthTokenException, IOException;
    Response sendJsonPostToCanvas(OauthToken oauthToken, String url, JsonObject requestBody) throws InvalidOauthTokenException, IOException;
    Response sendJsonPutToCanvas(OauthToken oauthToken, String url, JsonObject requestBody) throws InvalidOauthTokenException, IOException;
    Response deleteFromCanvas(OauthToken oauthToken, String url, Map<String, List<String>> parameters) throws InvalidOauthTokenException, IOException, ProtocolException, URISyntaxException;
    Response getSingleResponseFromCanvas(OauthToken oauthToken, String url) throws InvalidOauthTokenException, IOException;
    Response putToCanvas(OauthToken oauthToken, String url, Map<String, List<String>> parameters) throws InvalidOauthTokenException, IOException, ProtocolException, URISyntaxException;

    String sendUpload(String uploadUrl, Map<String, List<String>> params, InputStream in, String filename) throws IOException, ProtocolException, URISyntaxException;
}
