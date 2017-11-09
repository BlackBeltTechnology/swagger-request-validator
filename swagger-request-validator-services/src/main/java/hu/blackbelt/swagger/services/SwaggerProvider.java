package hu.blackbelt.swagger.services;

import java.io.IOException;
import java.io.InputStream;

public interface SwaggerProvider {

    /**
     * Return swagger url by name.
     *
     * @param name OSGi bundle context
     * @return Swagger url in input stream.
     * @throws IOException
     */
    InputStream getSwagger(String name) throws IOException;
}
