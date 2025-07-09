package edu.ksu.canvas.net;

import com.google.gson.Gson;
import edu.ksu.canvas.errors.ErrorHandler;
import edu.ksu.canvas.errors.GenericErrorHandler;
import edu.ksu.canvas.errors.UserErrorHandler;
import edu.ksu.canvas.exception.*;
import edu.ksu.canvas.impl.GsonResponseParser;
import edu.ksu.canvas.model.status.CanvasErrorResponse;
import edu.ksu.canvas.model.status.CanvasErrorResponse.ErrorMessage;
import edu.ksu.canvas.oauth.OauthToken;

import org.apache.commons.lang3.StringUtils;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.entity.mime.ContentBody;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.InputStreamBody;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;


import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SimpleRestClient implements RestClient {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleRestClient.class);

    private final List<ErrorHandler> errorHandlers;

    public SimpleRestClient() {
        errorHandlers = new ArrayList<>();
        errorHandlers.add(new UserErrorHandler());
        errorHandlers.add(new GenericErrorHandler());
    }

    @Override
    public Response sendApiGet(@NotNull OauthToken token, @NotNull String url,
                               int connectTimeout, int readTimeout) throws IOException {

        LOG.debug("Sending GET request to URL: {}", url);
        Long beginTime = System.currentTimeMillis();
        Response response = new Response();
        try (CloseableHttpClient httpClient = createHttpClient(connectTimeout, readTimeout)) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Authorization", "Bearer" + " " + token.getAccessToken());

            String[] entityContent = new String[1]; // Array to capture content from lambda
            try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet, httpResp -> {
                // Read the entity content here before it gets closed
                try {
                    entityContent[0] = new BasicHttpClientResponseHandler().handleResponse(httpResp);
                } catch (IOException e) {
                    entityContent[0] = null;
                }
                return (CloseableHttpResponse) httpResp;
            })) {

                //deal with the actual content
                response.setContent(handleResponse(httpResponse, httpGet, entityContent[0]));
                response.setResponseCode(httpResponse.getCode());
                Long endTime = System.currentTimeMillis();
                LOG.debug("GET call took: {}ms", endTime - beginTime);

                //deal with pagination
                Header linkHeader = httpResponse.getFirstHeader("Link");
                String linkHeaderValue = linkHeader == null ? null : httpResponse.getFirstHeader("Link").getValue();
                if (linkHeaderValue == null) {
                    return response;
                }
                String[] links = linkHeaderValue.split(",");
                for (String link : links) {
                    if (link.contains("rel=\"next\"")) {
                        LOG.debug("response has more pages");
                        String nextLink = link.substring(1, link.indexOf(';') - 1); //format is <http://.....>; rel="next"
                        response.setNextLink(nextLink);
                    }
                }
            } catch (ProtocolException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        return response;
    }

    @Override
    public Response sendJsonPut(OauthToken token, String url, String json, int connectTimeout, int readTimeout) throws IOException {
        return sendJsonPostOrPut(token, url, json, connectTimeout, readTimeout, "PUT");
    }

    @Override
    public Response sendJsonPost(OauthToken token, String url, String json, int connectTimeout, int readTimeout) throws IOException {
        return sendJsonPostOrPut(token, url, json, connectTimeout, readTimeout, "POST");
    }

    // PUT and POST are identical calls except for the header specifying the method
    private Response sendJsonPostOrPut(OauthToken token, String url, String json,
                                       int connectTimeout, int readTimeout, String method) throws IOException {
        LOG.debug("Sending JSON {} to URL: {}", method, url);
        Response response = new Response();

        try (CloseableHttpClient httpClient = createHttpClient(connectTimeout, readTimeout)) {
            ClassicHttpRequest action;
            if ("POST".equals(method)) {
                action = new HttpPost(url);
            } else if ("PUT".equals(method)) {
                action = new HttpPut(url);
            } else {
                throw new IllegalArgumentException("Method must be either POST or PUT");
            }
            Long beginTime = System.currentTimeMillis();
            action.setHeader("Authorization", "Bearer" + " " + token.getAccessToken());

            StringEntity requestBody = new StringEntity(json, ContentType.APPLICATION_JSON);
            action.setEntity(requestBody);
            try {
                String[] entityContent = new String[1]; // Array to capture content from lambda
                ClassicHttpResponse httpResponse = httpClient.execute(action, httpResp -> {
                    // Read the entity content here before it gets closed
                    try {
                        entityContent[0] = new BasicHttpClientResponseHandler().handleResponse(httpResp);
                    } catch (IOException e) {
                        entityContent[0] = null;
                    }
                    return httpResp;
                });

                String content = handleResponse(httpResponse, action, entityContent[0]);

                response.setContent(content);
                response.setResponseCode(httpResponse.getCode());
                Long endTime = System.currentTimeMillis();
                LOG.debug("POST call took: {}ms", endTime - beginTime);
                // Entity already consumed in response handler
            } catch (ProtocolException | URISyntaxException e) {
                throw new RuntimeException(e);
            }

            return response;
        }
    }

    /**
     *
     * @param token
     * @param url
     * @param postParameters
     * @param connectTimeout
     * @param readTimeout
     * @return
     * @throws InvalidOauthTokenException
     * @throws IOException
     * @throws ProtocolException
     * @throws URISyntaxException
     */
    @Override
    public Response sendApiPost(OauthToken token, String url, Map<String, List<String>> postParameters,
                                int connectTimeout, int readTimeout) throws InvalidOauthTokenException, IOException, ProtocolException, URISyntaxException {
        LOG.debug("Sending API POST request to URL: " + url);
        Response response = new Response();
        try (CloseableHttpClient httpClient = createHttpClient(connectTimeout, readTimeout)) {
            Long beginTime = System.currentTimeMillis();
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", "Bearer" + " " + token.getAccessToken());
            List<NameValuePair> params = convertParameters(postParameters);

            httpPost.setEntity(new UrlEncodedFormEntity(params));
            // execute() with request only is deprecated; it must be executed with response handler now.
            String[] entityContent = new String[1]; // Array to capture content from lambda
            ClassicHttpResponse httpResponse = httpClient.execute(httpPost, httpResp -> {
                // Read the entity content here before it gets closed
                try {
                    entityContent[0] = new BasicHttpClientResponseHandler().handleResponse(httpResp);
                } catch (IOException e) {
                    entityContent[0] = null;
                }
                return httpResp;
            });
            String content = handleResponse(httpResponse, httpPost, entityContent[0]);

            response.setContent(content);
            response.setResponseCode(httpResponse.getCode());
            Long endTime = System.currentTimeMillis();
            LOG.debug("POST call took: {}ms", endTime - beginTime);
            return response;
        }
    }


    /**
     *
     * @param token
     * @param url
     * @param postParameters
     * @param fileParameter
     * @param filePath
     * @param is
     * @param connectTimeout
     * @param readTimeout
     * @return
     * @throws InvalidOauthTokenException
     * @throws IOException
     */
    @Override
    public Response sendApiPostFile(OauthToken token, String url, Map<String, List<String>> postParameters, String fileParameter, String filePath, InputStream is,
                                int connectTimeout, int readTimeout) throws InvalidOauthTokenException, IOException {
        String result = null;
        LOG.debug("Sending API POST file request to URL: {}", url);
        Response response = new Response();
        try (CloseableHttpClient httpClient = createHttpClient(connectTimeout, readTimeout)) {
            Long beginTime = System.currentTimeMillis();
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Authorization", "Bearer" + " " + token.getAccessToken());
            List<NameValuePair> params = convertParameters(postParameters);

            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
            entityBuilder.setMode(HttpMultipartMode.STRICT);
            if (is == null) {
                FileBody fileBody = new FileBody(new File(filePath));
                entityBuilder.addPart(fileParameter, fileBody);
            } else {
                entityBuilder.addPart(fileParameter, new InputStreamBody(is, filePath));
            }
            for (NameValuePair param : params) {
                entityBuilder.addTextBody(param.getName(), param.getValue());
            }

            httpPost.setEntity(entityBuilder.build());
            String[] entityContent = new String[1]; // Array to capture content from lambda
            try (ClassicHttpResponse httpResponse = httpClient.execute(httpPost, httpResp -> {
                // Read the entity content here before it gets closed
                try {
                    entityContent[0] = new BasicHttpClientResponseHandler().handleResponse(httpResp);
                } catch (IOException e) {
                    entityContent[0] = null;
                }
                return (CloseableHttpResponse) httpResp;
            })) {
                String content = handleResponse(httpResponse, httpPost, entityContent[0]);
                response.setContent(content);
                response.setResponseCode(httpResponse.getCode());

                result = entityContent[0]; // Use the already read content
            }



            Long endTime = System.currentTimeMillis();
            LOG.debug("POST file call took: {}ms", endTime - beginTime);
            return response;
        } catch (ProtocolException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @param token
     * @param url
     * @param putParameters
     * @param connectTimeout
     * @param readTimeout
     * @return
     * @throws InvalidOauthTokenException
     * @throws IOException
     * @throws ProtocolException
     * @throws URISyntaxException
     */
    @Override
    public Response sendApiPut(OauthToken token, String url, Map<String, List<String>> putParameters,
                               int connectTimeout, int readTimeout) throws InvalidOauthTokenException, IOException, ProtocolException, URISyntaxException {
        LOG.debug("Sending API PUT request to URL: {}", url);
        Response response = new Response();
        try (CloseableHttpClient httpClient = createHttpClient(connectTimeout, readTimeout)) {
            Long beginTime = System.currentTimeMillis();
            HttpPut httpPut = new HttpPut(url);
            httpPut.setHeader("Authorization", "Bearer" + " " + token.getAccessToken());
            List<NameValuePair> params = convertParameters(putParameters);

            httpPut.setEntity(new UrlEncodedFormEntity(params));
            String[] entityContent = new String[1]; // Array to capture content from lambda
            ClassicHttpResponse httpResponse = httpClient.execute(httpPut, httpResp -> {
                // Read the entity content here before it gets closed
                try {
                    entityContent[0] = new BasicHttpClientResponseHandler().handleResponse(httpResp);
                } catch (IOException e) {
                    entityContent[0] = null;
                }
                return httpResp;
            });
            String content = handleResponse(httpResponse, httpPut, entityContent[0]);

            response.setContent(content);
            response.setResponseCode(httpResponse.getCode());
            Long endTime = System.currentTimeMillis();
            LOG.debug("PUT call took: {}ms", endTime - beginTime);
            return response;
        }
    }

    /**
     *
     * @param token
     * @param url
     * @param deleteParameters
     * @param connectTimeout
     * @param readTimeout
     * @return
     * @throws InvalidOauthTokenException
     * @throws IOException
     * @throws ProtocolException
     * @throws URISyntaxException
     */
    @Override
    public Response sendApiDelete(OauthToken token, String url, Map<String, List<String>> deleteParameters,
                                  int connectTimeout, int readTimeout) throws InvalidOauthTokenException, IOException, ProtocolException, URISyntaxException {
        LOG.debug("Sending API DELETE request to URL: {}", url);
        Response response = new Response();

        Long beginTime = System.currentTimeMillis();
        try (CloseableHttpClient httpClient = createHttpClient(connectTimeout, readTimeout)) {

            //This class is defined here because we need to be able to add form body elements to a delete request for a few api calls.
            class HttpDeleteWithBody extends HttpPost {
                public HttpDeleteWithBody(String uri) {
                    super(uri);
                }

                @Override
                public String getMethod() {
                    return "DELETE";
                }
            }

            HttpDeleteWithBody httpDelete = new HttpDeleteWithBody(url);

            httpDelete.setUri(URI.create(url));
            httpDelete.setHeader("Authorization", "Bearer" + " " + token.getAccessToken());
            List<NameValuePair> params = convertParameters(deleteParameters);

            httpDelete.setEntity(new UrlEncodedFormEntity(params));
            String[] entityContent = new String[1]; // Array to capture content from lambda
            ClassicHttpResponse httpResponse = httpClient.execute(httpDelete, httpResp -> {
                // Read the entity content here before it gets closed
                try {
                    entityContent[0] = new BasicHttpClientResponseHandler().handleResponse(httpResp);
                } catch (IOException e) {
                    entityContent[0] = null;
                }
                return httpResp;
            });

            String content = handleResponse(httpResponse, httpDelete, entityContent[0]);
            response.setContent(content);
            response.setResponseCode(httpResponse.getCode());
            Long endTime = System.currentTimeMillis();
            LOG.debug("DELETE call took: " + (endTime - beginTime) + "ms");

            return response;
        }
    }

    /**
     *
     * @param uploadUrl
     * @param params
     * @param in
     * @param filename
     * @param connectTimeout
     * @param readTimeout
     * @return
     * @throws IOException
     * @throws ProtocolException
     * @throws URISyntaxException
     */
    @Override
    public String sendUpload(String uploadUrl, Map<String, List<String>> params, InputStream in, String filename, int connectTimeout, int readTimeout) throws IOException, ProtocolException, URISyntaxException {

        try (CloseableHttpClient client = createHttpClient(connectTimeout, readTimeout)) {

            HttpPost httpPost = new HttpPost(uploadUrl);
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
            for (Map.Entry<String, List<String>> entry : params.entrySet()) {
                for (String value : entry.getValue()) {
                    entityBuilder.addTextBody(entry.getKey(), value);
                }
            }
            ContentBody fileBody = new InputStreamBody(in, filename);
            entityBuilder.addPart("file", fileBody);
            httpPost.setEntity(entityBuilder.build());

            String[] entityContent = new String[1]; // Array to capture content from lambda
            ClassicHttpResponse httpResponse = client.execute(httpPost, httpResp -> {
                // Read the entity content here before it gets closed
                try {
                    entityContent[0] = new BasicHttpClientResponseHandler().handleResponse(httpResp);
                } catch (IOException e) {
                    entityContent[0] = null;
                }
                return httpResp;
            });
            checkHeaders(httpResponse, httpPost, true, entityContent[0]);
            int httpStatus = httpResponse.getCode();
            if (httpStatus == 201 || (300 <= httpStatus && httpStatus <= 399)) {
                Header location = httpResponse.getFirstHeader("Location");
                if (location != null) {
                    return location.getValue();
                } else {
                    throw new CanvasException("No location to redirect to when uploading file: " + httpStatus, uploadUrl);
                }
            } else {
                throw new CanvasException("Bad status when uploading file: " + httpStatus, uploadUrl);
            }

        }
    }

    /**
     *
     * @param httpResponse The Response containing the headers
     * @param request The requesting containing the headers
     * @param allowRedirect
     * @throws ProtocolException If things we have a messed up message body, status line, ect
     * @throws URISyntaxException If the Url cannot be parsed
     */
    private void checkHeaders(ClassicHttpResponse httpResponse, ClassicHttpRequest request, boolean allowRedirect, String responseContent) throws ProtocolException, URISyntaxException {
        int statusCode = httpResponse.getCode();
        double rateLimitThreshold = 0.1;
        double xRateCost = 0;
        double xRateLimitRemaining = 0;

        try {
            xRateCost = Double.parseDouble(httpResponse.getFirstHeader("x-request-cost").getValue());
            xRateLimitRemaining = Double.parseDouble(httpResponse.getFirstHeader("x-rate-limit-remaining").getValue());

            //Throws a 403 with a "Rate Limit Exceeded" error message if the API throttle limit is hit.
            //See https://canvas.instructure.com/doc/api/file.throttling.html.
            if(xRateLimitRemaining < rateLimitThreshold) {
                LOG.error("Canvas API rate limit exceeded. Bucket quota: {} Cost: {} Threshold: {} HTTP status: {} Requested URL: {}", xRateLimitRemaining, xRateCost, rateLimitThreshold, statusCode, request.getUri());
                throw new RateLimitException(extractErrorMessageFromResponse(httpResponse, responseContent), String.valueOf(request.getUri()));
            }
        } catch (NullPointerException e) {
            LOG.debug("Rate not being limited: {}", String.valueOf(e));
        }
        if (statusCode == 401) {
            //If the WWW-Authenticate header is set, it is a token problem.
            //If the header is not present, it is a user permission error.
            //See https://canvas.instructure.com/doc/api/file.oauth.html#storing-access-tokens
            if(httpResponse.containsHeader(HttpHeaders.WWW_AUTHENTICATE)) {
                LOG.debug("User's token is invalid. It might need refreshing");
                throw new InvalidOauthTokenException();
            }
            LOG.error("User is not authorized to perform this action");
            throw new UnauthorizedException();
        }
        if(statusCode == 403) {
            LOG.error("Canvas has throttled this request. Requested URL: {}", request.getUri());
            throw new ThrottlingException(extractErrorMessageFromResponse(httpResponse, responseContent), String.valueOf(request.getUri()));
        }
        if(statusCode == 404) {
            LOG.error("Object not found in Canvas. Requested URL: {}", request.getUri());
            throw new ObjectNotFoundException(extractErrorMessageFromResponse(httpResponse, responseContent), String.valueOf(request.getUri()));
        }
        if(statusCode == 504) {
            LOG.error("504 Gateway Time-out while requesting: {}", request.getUri());
            throw new RetriableException("status code: 504, reason phrase: Gateway Time-out", String.valueOf(request.getUri()));
        }
        // If we receive a 5xx exception, we should not wrap it in an unchecked exception for upstream clients to deal with.
        if(statusCode < 200 || (statusCode > (allowRedirect?399:299) && statusCode <= 499)) {
            LOG.error("HTTP status {} returned from {}", statusCode, request.getUri());
            handleError(request, httpResponse, responseContent);
        }
        //TODO Handling of 422 when the entity is malformed.
    }

    /**
     *
     * @param httpRequest Base Class representing out Http request
     * @param httpResponse The Http message response eclosing our entity
     * @throws URISyntaxException If something goes wrong parsing the URI
     * @throws ProtocolException If something goes wrong during the request/response process
     */
    private void handleError(ClassicHttpRequest httpRequest, ClassicHttpResponse httpResponse, String responseContent) throws URISyntaxException, ProtocolException {
        for (ErrorHandler handler : errorHandlers) {
            if (handler.shouldHandle(httpRequest, httpResponse)) {
                handler.handle(httpRequest, httpResponse);
            }
        }
        String canvasErrorString = extractErrorMessageFromResponse(httpResponse, responseContent);
        throw new CanvasException(canvasErrorString, String.valueOf(httpRequest.getUri()));
    }

    /**
     * Attempts to extract a useful Canvas error message from a response object.
     * Sometimes Canvas API errors come back with a JSON body containing something like
     * <pre>{"errors":[{"message":"Human readable message here."}],"error_report_id":123456}</pre>.
     * This method will attempt to extract the message. If parsing fails, it will return
     * the raw JSON string without trying to parse it. Returns null if all attempts fail.
     * @param response HttpResponse object representing the error response from Canvas
     * @return The Canvas human-readable error string or null if unable to extract it
     */
    private String extractErrorMessageFromResponse(ClassicHttpResponse response, String responseContent) {
        String contentType = response.getEntity().getContentType();
        String message = null;
        if(contentType.contains("application/json")) {
            Gson gson = GsonResponseParser.getDefaultGsonParser(false);
            String responseBody = responseContent; // Use the pre-read content
            try {
                if (responseBody != null) {
                    LOG.error("Body of error response from Canvas: {}", responseBody);
                    CanvasErrorResponse errorResponse = gson.fromJson(responseBody, CanvasErrorResponse.class);
                    List<ErrorMessage> errors = errorResponse.getErrors();
                    if(errors != null) {
                        //I have only ever seen a single error message but it is an array so presumably there could be more.
                        message = errors.stream().map(ErrorMessage::getMessage).collect(Collectors.joining(", "));
                    }
                    else{
                        message = responseBody;
                    }
                }
            } catch (Exception e) {
                //Returned JSON was not in expected format. Fall back to returning the whole response body, if any
                if(StringUtils.isNotBlank(responseBody)) {
                    message = responseBody;
                }
            }
        }
        return message;
    }

    /**
     *
     * @param httpResponse HttpResponse to encapsulate an HttpEntity
     * @param request base for Http request
     * @return response entity body of there was one
     * @throws IOException Problems giving or receiving data
     */
    private String handleResponse(ClassicHttpResponse httpResponse, ClassicHttpRequest request, String entityContent) throws IOException, ProtocolException, URISyntaxException {
        checkHeaders(httpResponse, request, false, entityContent);
        return entityContent;
    }

    /**
     *
     * @param connectTimeout maximum allowed time for data exchange
     * @param readTimeout maximum allowed time for the client will wait for sent data
     * @return Configured Closable Http Client
     */
    private CloseableHttpClient createHttpClient(int connectTimeout, int readTimeout) {
        return HttpClients.custom()
                .setConnectionManager(buildHttpClient(connectTimeout, readTimeout))
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(StandardCookieSpec.STRICT)
                        .build())
                .build();
    }

    /**
     *
     * @param connectTimeout maximum allowed time for data exchange
     * @param readTimeout maximum allowed time for the client for data to be sent
     * @return a managed pool of client connections
     */
    private PoolingHttpClientConnectionManager buildHttpClient(int connectTimeout, int readTimeout) {
        ConnectionConfig config = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMinutes(connectTimeout))
                .setSocketTimeout(Timeout.ofMinutes(readTimeout))
                .build();
        return PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(config)
                .build();
    }



    private static List<NameValuePair> convertParameters(final Map<String, List<String>> parameterMap) {
        final List<NameValuePair> params = new ArrayList<>();

        if (parameterMap == null) {
            return params;
        }

        for (final Map.Entry<String, List<String>> param : parameterMap.entrySet()) {
            final String key = param.getKey();
            if(param.getValue() == null || param.getValue().isEmpty()) {
                params.add(new BasicNameValuePair(key, null));
                LOG.debug("key: {}\tempty value", key);
            }
            for (final String value : param.getValue()) {
                params.add(new BasicNameValuePair(key, value));
                LOG.debug("key: {}\tvalue: {}", key, value);
            }
        }
        return params;
    }

}
