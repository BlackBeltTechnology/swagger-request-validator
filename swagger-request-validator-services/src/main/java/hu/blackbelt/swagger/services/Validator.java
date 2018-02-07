package hu.blackbelt.swagger.services;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * RESTful service validator service interface.
 */
public interface Validator {

    /**
     * Validate an HTTP request. Swagger definition is loaded at initialization of the component.
     *
     * @param request HTTP request
     * @param body HTTP request body
     * @return list of validation errors
     */
    List<ValidationError> validate(HttpServletRequest request, String body);

    /**
     * Validate an HTTP response. Swagger definition is loaded at initialization of the component.
     *
     * @param method HTTP method
     * @param path HTTP uri
     * @param responseCode HTTP response code
     * @param headers HTTP response headers
     * @param body HTTP response body
     * @return list of validation errors
     */
    List<ValidationError> validate(String method, String path, Integer responseCode, Map<String, Object> headers, String body);
}
