package hu.blackbelt.swagger.services.internal;

import com.atlassian.oai.validator.SwaggerRequestResponseValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import hu.blackbelt.swagger.services.SwaggerProvider;
import hu.blackbelt.swagger.services.ValidationError;
import hu.blackbelt.swagger.services.Validator;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Slf4j
public class SwaggerValidator implements Validator {

    @SuppressWarnings({"checkstyle:AbbreviationAsWordInName", "checkstyle:JavadocMethod"})
    @ObjectClassDefinition(name = "Swagger validator settings")
    public @interface Config {

        @AttributeDefinition(required = false, name = "Swagger URL")
        String swaggerUrl();

        @AttributeDefinition(required = false, name = "Swagger name", description = "Required SwaggerProvider component for this solution.")
        String swaggerName();
    }

    private String swaggerUrl;

    private String swaggerName;

    private SwaggerRequestResponseValidator validator;

    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    private SwaggerProvider swaggerProvider;

    @Activate
    void start(final Config config) {
        swaggerUrl = config.swaggerUrl();
        swaggerName = config.swaggerName();

        if (swaggerUrl == null && swaggerName == null) {
            throw new IllegalArgumentException("Swagger URL and Swagger name are missing from config file.");
        }
        if (swaggerUrl != null) {
            validator = SwaggerRequestResponseValidator.createFor(swaggerUrl).build();
        } else {
            Objects.requireNonNull(swaggerProvider, "Swagger provider service not exists yet.");
            final String definition = swaggerProvider.getSwagger(swaggerName, null);
            if (definition != null) {
                validator = SwaggerRequestResponseValidator.createFor(definition).build();
            }
        }
    }

    @Deactivate
    void stop() {
        validator = null;
    }

    @Override
    public List<ValidationError> validate(final HttpServletRequest request, final String body) {
        final List<ValidationError> errors = new LinkedList<>();

        final SimpleRequest.Builder builder;
        final String action = request.getMethod();
        final String requestUrl = request.getPathInfo();

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
            case "DELETE":
                builder = SimpleRequest.Builder.delete(requestUrl);
                break;
            case "OPTIONS":
                builder = SimpleRequest.Builder.options(requestUrl);
                break;
            case "HEAD":
                builder = SimpleRequest.Builder.head(requestUrl);
                break;
            case "PATCH":
                builder = SimpleRequest.Builder.patch(requestUrl);
                break;
            case "TRACE":
                builder = SimpleRequest.Builder.trace(requestUrl);
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

        request.getParameterMap().forEach((parameterName, parameterValues) -> builder.withQueryParam((String)parameterName, (String[])parameterValues));

        if (body != null) {
            builder.withBody(body);
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

            if (log.isTraceEnabled()) {
                log.trace("RESTful request validation failed, errors: " + errors);
            }
        }

        return errors;
    }

    @Override
    public List<ValidationError> validate(String method, String path, Integer responseCode, Map<String, Object> headers, String body) {
        final List<ValidationError> errors = new LinkedList<>();

        final SimpleResponse.Builder builder = SimpleResponse.Builder.status(responseCode);

        headers.forEach((hn,hv) -> builder.withHeader(hn, Stream.of((List)hv).map(String::valueOf).collect(Collectors.toList())));

        if (body != null) {
            builder.withBody(body);
        }

        final ValidationReport report = validator.validateResponse(path, Request.Method.valueOf(method), builder.build());

        if (report.hasErrors()) {
            for (final ValidationReport.Message msg : report.getMessages()) {
                final ValidationError error = new ValidationError();
                error.setKey(msg.getKey());
                error.setMessage(msg.getMessage());
                error.setPath(path);
                error.setArguments(Collections.unmodifiableList(msg.getAdditionalInfo()));

                errors.add(error);
            }

            if (log.isTraceEnabled()) {
                log.trace("RESTful response validation failed, errors: " + errors);
            }
        }

        return errors;
    }
}
