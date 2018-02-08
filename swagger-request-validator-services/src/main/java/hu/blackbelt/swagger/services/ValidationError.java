package hu.blackbelt.swagger.services;

import java.util.List;

@lombok.Getter
@lombok.Setter
@lombok.ToString
public class ValidationError {

    private String key;
    private String path;
    private String message;
    private List<String> arguments;
}
