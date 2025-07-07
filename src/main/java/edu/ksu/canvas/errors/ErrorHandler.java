package edu.ksu.canvas.errors;

import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.impl.*;

/**
 * This allows additional specific behaviour for handling errors.
 */
public interface ErrorHandler {

    boolean shouldHandle(HttpRequest httpRequest, ClassicHttpResponse httpResponse) throws ProtocolException;

    void handle(HttpRequest httpRequest, ClassicHttpResponse httpResponse);
}
