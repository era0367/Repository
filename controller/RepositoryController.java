package knu.myhealthhub.repository.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static knu.myhealthhub.repository.transactions.ITI42.registerDocumentSet;
import static knu.myhealthhub.repository.transactions.ITI43.retrieveDocumentSet;
import static knu.myhealthhub.repository.validator.ParamValidator.validateParametersForITI41;
import static knu.myhealthhub.repository.validator.ParamValidator.validateParametersForITI43;
import static knu.myhealthhub.settings.Configuration.*;

@RestController
public class RepositoryController {
    @PostMapping(METADATA)
    public String xdsProvideAndRegisterDocumentSet(@RequestBody @Validated String body) {
        String validateParamResult = validateParametersForITI41(body);
        if (!validateParamResult.equals(TRUE)) {
            return validateParamResult;
        }
        return registerDocumentSet(body);
    }

    @PostMapping(DOCUMENT)
    public String xdsRetrieveDocumentSet(@RequestBody @Validated String body) {
        String validateParamResult = validateParametersForITI43(body);
        if (!validateParamResult.equals(TRUE)) {
            return validateParamResult;
        }
        return retrieveDocumentSet(body);
    }
}