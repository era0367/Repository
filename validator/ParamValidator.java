package knu.myhealthhub.repository.validator;

import knu.myhealthhub.datamodels.DocumentRequest;
import knu.myhealthhub.datamodels.Metadata;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import static knu.myhealthhub.common.JsonUtility.*;
import static knu.myhealthhub.enums.ERROR_CODE.*;
import static knu.myhealthhub.repository.validator.DocumentValidator.isValidDocumentRequest;
import static knu.myhealthhub.repository.validator.FhirValidator.isValidFhirData;
import static knu.myhealthhub.repository.validator.MetaValidator.isValidMetadata;
import static knu.myhealthhub.settings.Configuration.TRUE;
import static knu.myhealthhub.settings.KeyString.*;
import static knu.myhealthhub.settings.errors.ErrorUtility.setErrorMessage;

public class ParamValidator {
    public static String validateParametersForITI41(String body) {
        String getJsonArrayFromParametersResult = getJsonArrayFromParameters(body, KEY_FOR_FHIR_LIST);
        if (!getJsonArrayFromParametersResult.equals(TRUE)) {
            return getJsonArrayFromParametersResult;
        }
        JSONObject jsonObject = toJsonObject(body);
        JSONArray documentList = getJsonArray(jsonObject, KEY_FOR_FHIR_LIST);
        JSONArray metadataList = getJsonArray(jsonObject, KEY_FOR_META_LIST);
        if (null == metadataList) {
            String reason = String.format("Fail to find key[%s] from %s", KEY_FOR_META_LIST, jsonObject.toJSONString());
            return setErrorMessage(XDS_MISSING_DOCUMENT_METADATA, reason);
        }
        if (documentList.size() != metadataList.size()) {
            return setErrorMessage(XDS_NON_IDENTICAL_SIZE, null);
        }
        return validateDocumentSet(metadataList, documentList);
    }

    private static String validateDocumentSet(JSONArray metadataList, JSONArray documentList) {
        for (int i = 0; i < metadataList.size(); i++) {
            JSONObject metadataJsonObject = getJsonObjectFromArray(metadataList, i);
            String fhirData = getStringFromArray(documentList, i).replaceAll("\\\\", "");
            String validateDocumentEntryResult = validateDocumentEntry(metadataJsonObject, fhirData);
            if (!validateDocumentEntryResult.equals(TRUE)) {
                return validateDocumentEntryResult;
            }
        }
        return TRUE;
    }

    private static String validateDocumentEntry(JSONObject metadataJsonObject, String fhirData) {
        Metadata metadata = toJavaObject(metadataJsonObject, Metadata.class);
        if (null == metadata) {
            String reason = String.format("Fail to parse JSON to JavaObject[Metadata] %s", metadataJsonObject);
            return setErrorMessage(XDS_REPOSITORY_METADATA_ERROR, reason);
        }
        String validateMetadataResult = isValidMetadata(metadata, fhirData);
        if (!validateMetadataResult.equals(TRUE)) {
            return validateMetadataResult;
        }
        return isValidFhirData(metadata.getSchemaId(), fhirData);
    }

    public static String validateParametersForITI43(String body) {
        String getJsonArrayFromParametersResult = getJsonArrayFromParameters(body, KEY_FOR_DOCUMENT_REQUEST_LIST);
        if (!getJsonArrayFromParametersResult.equals(TRUE)) {
            return getJsonArrayFromParametersResult;
        }
        JSONObject jsonObject = toJsonObject(body);
        JSONArray documentRequestList = getJsonArray(jsonObject, KEY_FOR_DOCUMENT_REQUEST_LIST);
        return validateDocumentRequestSet(documentRequestList);
    }

    private static String getJsonArrayFromParameters(String body, String key) {
        JSONObject jsonObject = toJsonObject(body);
        if (null == jsonObject) {
            String reason = String.format("Fail to parse string to JSON - %s", body);
            return setErrorMessage(XDS_MISSING_DOCUMENT, reason);
        }
        JSONArray documentRequestList = getJsonArray(jsonObject, key);
        if (null == documentRequestList) {
            String reason = String.format("Fail to find key[%s] from %s", key, jsonObject.toJSONString());
            return setErrorMessage(XDS_MISSING_DOCUMENT, reason);
        }
        return TRUE;
    }

    private static String validateDocumentRequestSet(JSONArray documentRequestList) {
        for (int i = 0; i < documentRequestList.size(); i++) {
            JSONObject documentRequestJsonObject = getJsonObjectFromArray(documentRequestList, i);
            String validateDocumentRequestEntryResult = validateDocumentRequestEntry(documentRequestJsonObject);
            if (!validateDocumentRequestEntryResult.equals(TRUE)) {
                return validateDocumentRequestEntryResult;
            }
        }
        return TRUE;
    }

    private static String validateDocumentRequestEntry(JSONObject documentRequestJsonObject) {
        DocumentRequest documentRequest = toJavaObject(documentRequestJsonObject, DocumentRequest.class);
        if (null == documentRequest) {
            String reason = String.format("Fail to parse JSON to JavaObject[DocumentRequest] %s", documentRequestJsonObject.toJSONString());
            return setErrorMessage(XDS_REPOSITORY_ERROR, reason);
        }
        return isValidDocumentRequest(documentRequest);
    }
}