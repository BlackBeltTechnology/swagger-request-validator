package hu.blackbelt.swagger.services.internal;

import com.atlassian.oai.validator.SwaggerRequestResponseValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import hu.blackbelt.swagger.services.SwaggerProvider;
import hu.blackbelt.swagger.services.ValidationError;
import hu.blackbelt.swagger.services.Validator;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component(immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Slf4j
public class SwaggerValidator implements Validator {

    @SuppressWarnings({"checkstyle:AbbreviationAsWordInName", "checkstyle:JavadocMethod"})
    @ObjectClassDefinition(name = "")
    public @interface Config {

        @AttributeDefinition(required = false, name = "Swagger URL")
        String swaggerUrl();

        @AttributeDefinition(required = false, name = "Swagger name", description = "Required SwaggerProvider component for this solution.")
        String swaggerName();
    }

    private String swaggerUrl;

    private String swaggerName;

    private SwaggerRequestResponseValidator validator;

    @Reference(policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
    private SwaggerProvider swaggerProvider;

    @Activate
    void start(final Config config) {
        swaggerUrl = config.swaggerUrl();
        swaggerName = config.swaggerName();

        String definition = null;

        if (swaggerUrl != null) {
            definition = swaggerUrl;
        } else {
            Objects.requireNonNull(swaggerProvider, "Swagger provider service not exists yet.");
            try (InputStream inputStream = swaggerProvider.getSwagger(swaggerName);
                 Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
                definition = scanner.useDelimiter("\\A").next();
            } catch (IOException ex) {
                log.warn("Failed to get swagger url by name.", ex);
            }
        }

        if (definition != null) {
            validator = SwaggerRequestResponseValidator.createFor(definition).build();
        }
    }

    @Deactivate
    void stop() {
        validator = null;
    }

    @Override
    public List<ValidationError> validate(HttpServletRequest request) {
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
        String headerName;
        while (headerNames.hasMoreElements()) {
            headerName = headerNames.nextElement();
            builder.withHeader(headerName, request.getHeader(headerName));
        }

        String queryParams = request.getQueryString();
        if (queryParams != null) {
            builder.withQueryParam(queryParams);
        }

        // TODO: can we read this stream after CXF?
        String body = null;
        try {
            body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            if (body != null) {
                builder.withBody(body);
            }
        } catch (IOException ex) {
            log.warn("Request body reading failed.", ex);
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
