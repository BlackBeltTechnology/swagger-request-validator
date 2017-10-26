package hu.blackbelt.swagger.services;

import javax.annotation.Nonnull;
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
    @Nonnull List<ValidationError> validate(Object request); // TODO: change type to HTTP
}
