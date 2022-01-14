package knu.myhealthhub.repository;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.UUID;

import static knu.myhealthhub.common.JsonUtility.*;
import static knu.myhealthhub.enums.ERROR_CODE.XDS_DUPLICATE_UNIQUE_ID_IN_REGISTRY;
import static knu.myhealthhub.enums.ERROR_CODE.XDS_REPOSITORY_ERROR;
import static knu.myhealthhub.settings.Configuration.*;
import static knu.myhealthhub.settings.KeyString.*;
import static knu.myhealthhub.settings.errors.ErrorUtility.setErrorMessage;
import static knu.myhealthhub.transactions.RestSender.createRest;
import static knu.myhealthhub.transactions.RestSender.getFhirHeader;

public class RepositoryManager {
    public static String getRepositoryUniqueId() {
        return UUID.nameUUIDFromBytes(BASE_STRING.getBytes()).toString();
    }

    public static String insertDocument(JSONObject fhirDataJsonObject, String base) {
        JSONArray identifierJsonArray = getJsonArray(fhirDataJsonObject, KEY_FOR_IDENTIFIER);
        JSONObject identifierJsonObject = getJsonObjectFromArray(identifierJsonArray, 0);
        String identifier = getStringFromObject(identifierJsonObject, KEY_FOR_VALUE);
        String checkDuplicateResult = checkDuplicate(identifier, base);
        if (!checkDuplicateResult.equals(TRUE)) {
            return checkDuplicateResult;
        }
        String requestInsertResult = requestInsert(fhirDataJsonObject, base);
        JSONObject requestInsertResultJson = toJsonObject(requestInsertResult);
        if (null == requestInsertResultJson) {
            return setErrorMessage(XDS_REPOSITORY_ERROR, null);
        }
        String resourceId = getStringFromObject(requestInsertResultJson, KEY_FOR_RESOURCE_ID);
        if (null == resourceId) {
            String reason = String.format("Fail to find key[%s] from %s", KEY_FOR_RESOURCE_ID, requestInsertResultJson.toJSONString());
            return setErrorMessage(XDS_REPOSITORY_ERROR, reason);
        }
        String requestQueryResult = requestQuery(resourceId, base);
        // @Todo 예외처리
        JSONObject requestQueryResultJson = toJsonObject(requestQueryResult);
        return getUri(requestQueryResultJson, base);
    }

    public static String requestQuery(String id, String base) {
        String url = URL_HEADER + FHIR_SERVER_ENDPOINT + base + "?" + KEY_FOR_ID + "=" + id;
        return createRest(url, HttpMethod.GET, getFhirHeader(), new JSONObject());
    }

    public static String getTotalCount(String id, String base) {
        String requestQueryResult = requestQuery(id, base);
        JSONObject requestQueryResultJson = toJsonObject(requestQueryResult);
        String totalString = getStringFromObject(requestQueryResultJson, KEY_FOR_TOTAL);
        if (null == totalString) {
            String reason = String.format("Fail to find key[total] from %s", requestQueryResultJson.toJSONString());
            return setErrorMessage(XDS_REPOSITORY_ERROR, reason);
        }
        return totalString;
    }

    private static String checkDuplicate(String id, String base) {
        String totalString = getTotalCount(id, base);
        int total = Integer.parseInt(totalString);
        if (0 < total) {
            return setErrorMessage(XDS_DUPLICATE_UNIQUE_ID_IN_REGISTRY, null);
        }
        return TRUE;
    }

    private static String requestInsert(JSONObject fhirDataJsonObject, String base) {
        String url = URL_HEADER + FHIR_SERVER_ENDPOINT + base;
        HashMap<String, String> header = getFhirHeader();
        return createRest(url, HttpMethod.POST, header, fhirDataJsonObject);
    }

    private static String getUri(JSONObject responseJson, String base) {
        JSONArray entryList = getJsonArray(responseJson, KEY_FOR_RESOURCE_ENTRY);
        if (null == entryList) {
            String reason = String.format("Fail to find key[%s] from %s", KEY_FOR_RESOURCE_ENTRY, responseJson.toJSONString());
            return setErrorMessage(XDS_REPOSITORY_ERROR, reason);
        }
        JSONObject entry = getJsonObjectFromArray(entryList, 0);
        String fullUrl = getStringFromObject(entry, KEY_FOR_RESOURCE_ENTRY_FULL_URL);
        if (null == fullUrl) {
            String reason = String.format("Fail to find key[%s] from %s", KEY_FOR_RESOURCE_ENTRY_FULL_URL, entry.toJSONString());
            return setErrorMessage(XDS_REPOSITORY_ERROR, reason);
        }
        String toRemove = URL_HEADER + FHIR_SERVER_ENDPOINT + base + "/";
        String uri = fullUrl.replace(toRemove, "");
        return uri.replace(FHIR_SERVER_ENDPOINT_LOCALHOST, "");
    }
}