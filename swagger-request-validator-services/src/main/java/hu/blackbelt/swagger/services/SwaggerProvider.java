package hu.blackbelt.swagger.services;

import javax.servlet.http.HttpServletRequest;

public interface SwaggerProvider {

    /**
     * Return Swagger file by name.
     *
     * @param filename Swagger filename
     * @return content of Swagger file
     */
    String getSwagger(final String filename, final HttpServletRequest request);
}
