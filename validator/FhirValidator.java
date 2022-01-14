package knu.myhealthhub.repository.validator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.http.HttpMethod;

import java.util.HashMap;

import static knu.myhealthhub.common.JsonUtility.*;
import static knu.myhealthhub.enums.ERROR_CODE.XDS_MISSING_DOCUMENT_METADATA;
import static knu.myhealthhub.enums.ERROR_CODE.XDS_REPOSITORY_ERROR;
import static knu.myhealthhub.settings.Configuration.*;
import static knu.myhealthhub.settings.KeyString.*;
import static knu.myhealthhub.settings.errors.ErrorUtility.setErrorMessage;
import static knu.myhealthhub.transactions.RestSender.createRest;

public class FhirValidator {
    public static String isValidFhirData(String profile, String fhirData) {
        JSONObject fhirDataJsonObject = toJsonObject(fhirData);
        String response = requestProfileValidation(profile, fhirDataJsonObject);
        if (null == response) {
            return setErrorMessage(XDS_REPOSITORY_ERROR, null);
        }
        return parseProfileValidationResult(response);
    }

    private static String parseProfileValidationResult(String response) {
        JSONObject responseJson = toJsonObject(response);
        if (null == responseJson) {
            String reason = String.format("Fail to parse string to JSON - %s", response);
            return setErrorMessage(XDS_REPOSITORY_ERROR, reason);
        }
        JSONArray issueList = getJsonArray(responseJson, KEY_FOR_ISSUE);
        if (null == issueList) {
            String reason = String.format("Fail to find key[issue] from %s", responseJson.toJSONString());
            return setErrorMessage(XDS_REPOSITORY_ERROR, reason);
        }
        JSONObject issue = getJsonObjectFromArray(issueList, 0);
        String severity = getStringFromObject(issue, KEY_FOR_SEVERITY);
        if (!severity.equals(INFORMATION_SEVERITY)) {
            return setErrorMessage(XDS_MISSING_DOCUMENT_METADATA, responseJson.toJSONString());
        }
        String diagnosis = getStringFromObject(issue, KEY_FOR_DIAGNOSTICS);
        if (!diagnosis.equals(SUCCESS_DIAGNOSTICS)) {
            return setErrorMessage(XDS_MISSING_DOCUMENT_METADATA, responseJson.toJSONString());
        }
        return TRUE;
    }

    private static String requestProfileValidation(String profile, JSONObject fhirDataJsonObject) {
        String url = URL_HEADER + FHIR_SERVER_ENDPOINT + FHIR_SERVER_VALIDATOR + profile;
        HashMap<String, String> header = new HashMap<>();
        header.put(HEADER_KEY_CONTENT_TYPE, HEADER_VALUE_CONTENT_TYPE_APPLICATION_JSON);
        return createRest(url, HttpMethod.POST, header, fhirDataJsonObject);
    }
}