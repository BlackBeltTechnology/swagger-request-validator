package hu.blackbelt.swagger.services.internal;

import com.atlassian.oai.validator.SwaggerRequestResponseValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.blackbelt.swagger.services.ValidationError;
import hu.blackbelt.swagger.services.Validator;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

@Component(immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Slf4j
public class SwaggerValidator implements Validator {

    @SuppressWarnings({"checkstyle:AbbreviationAsWordInName", "checkstyle:JavadocMethod"})
    @ObjectClassDefinition(name = "Local settings")
    public @interface Config {

        @AttributeDefinition(name = "AGS server time zone (format: continent/city)")
        String swaggerURI();
    }

    private String swaggerURI;

    private SwaggerRequestResponseValidator validator;

    @Activate
    void start(final Config config) {
        swaggerURI = config.swaggerURI();

        // TODO: load Swagger definition
        final String definition = null;

        validator = SwaggerRequestResponseValidator.createFor(definition).build();
    }

    @Deactivate
    void stop() {
        validator = null;
    }

    @Override
    public List<ValidationError> validate(HttpServletRequest request, Object body) {
        final List<ValidationError> errors = new LinkedList<>();

        final SimpleRequest.Builder builder;
        final String action = request.getMethod();
        final String requestUrl = request.getRequestURL().toString();

        switch (action) {
            case "GET":
                builder = SimpleRequest.Builder.get(requestUrl);
                break;
            case "POST":
                builder = SimpleRequest.Builder.post(requestUrl);
                break;
            case "PUT":
                builder = SimpleRequest.Builder.put(requestUrl);
                break;
            default:
                throw new IllegalArgumentException("Invalid HTTP action: " + action);
        }

        final Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            builder.withHeader(request.getHeader(headerNames.nextElement()));
        }

        String queryParams = request.getQueryString();
        if (queryParams != null) {
            builder.withQueryParam(queryParams);
        }

        if (body != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                String bodyString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(body);

                builder.withBody(bodyString);
            } catch (JsonProcessingException ex) {
                log.warn("Request body parsing failed.", ex);
            }
        }

        final ValidationReport report = validator.validateRequest(builder.build());

        if (report.hasErrors()) {
            for (final ValidationReport.Message msg : report.getMessages()) {
                final ValidationError error = new ValidationError();
                error.setKey(msg.getKey());
                error.setMessage(msg.getMessage());
                error.setPath(requestUrl);
                error.setArguments(Collections.unmodifiableList(msg.getAdditionalInfo()));

                errors.add(error);
            }

            if (log.isDebugEnabled()) {
                log.debug("RESTful request validation failed, errors: " + errors);
            }
        }

        return errors;
    }
}
