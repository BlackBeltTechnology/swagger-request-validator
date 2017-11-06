package hu.blackbelt.swagger.services.internal;

import com.atlassian.oai.validator.SwaggerRequestResponseValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import hu.blackbelt.swagger.services.ValidationError;
import hu.blackbelt.swagger.services.Validator;
import lombok.extern.slf4j.Slf4j;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.util.Collections;
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
    public List<ValidationError> validate(Object request) {
        final List<ValidationError> errors = new LinkedList<>();

        // TODO: validate request

        final SimpleRequest.Builder builder;
        final String action = "action-from-request";
        if ("GET".equals(action)) {
            builder = SimpleRequest.Builder.get("path-from-request");
        } else {
            throw new IllegalArgumentException("Invalid HTTP action: " + action);
        }

        //TODO: for builder.withHeader()
        //TODO: for builder.withQueryParam()
        //TODO: set builder.withBody()

        final ValidationReport report = validator.validateRequest(builder.build());

        if (report.hasErrors()) {
            for (final ValidationReport.Message msg : report.getMessages()) {
                final ValidationError error = new ValidationError();
                error.setKey(msg.getKey());
                error.setMessage(msg.getMessage());
                //error.setPath(TODO);
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
