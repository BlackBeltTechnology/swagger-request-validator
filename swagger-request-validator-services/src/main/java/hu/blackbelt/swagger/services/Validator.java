package hu.blackbelt.swagger.services;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * RESTful service validator service interface.
 */
public interface Validator {

    /**
     * Validate an HTTP request. Swagger definition is loaded at initialization of the component.
     *
     * @param request HTTP request
     * @return list of validation errors
     */
    List<ValidationError> validate(HttpServletRequest request, Object body);
}
