package knu.myhealthhub.repository.transactions;

import knu.myhealthhub.datamodels.DocumentRequest;
import knu.myhealthhub.datamodels.DocumentResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import static knu.myhealthhub.common.JsonUtility.*;
import static knu.myhealthhub.repository.RepositoryManager.requestQuery;
import static knu.myhealthhub.settings.Configuration.FAILURE;
import static knu.myhealthhub.settings.Configuration.SUCCESS;
import static knu.myhealthhub.settings.KeyString.*;

public class ITI43 {
    public static String retrieveDocumentSet(String body) {
        JSONObject jsonObject = toJsonObject(body);
        assert jsonObject != null;
        JSONArray documentRequestList = getJsonArray(jsonObject, KEY_FOR_DOCUMENT_REQUEST_LIST);
        JSONArray documentResponseList = new JSONArray();
        JSONArray registryErrorList = new JSONArray();
        String status = SUCCESS;
        for (int i = 0; i < documentRequestList.size(); i++) {
            JSONObject documentRequestJson = getJsonObjectFromArray(documentRequestList, i);
            String retrieveDocumentEntryResult = retrieveDocumentEntry(documentRequestJson);
            JSONObject retrieveDocumentEntryResultJson = toJsonObject(retrieveDocumentEntryResult);
            if (null == toJavaObject(retrieveDocumentEntryResultJson, DocumentResponse.class)) {
                status = FAILURE;
                registryErrorList.add(retrieveDocumentEntryResultJson);
                continue;
            }
            documentResponseList.add(retrieveDocumentEntryResultJson);
        }
        JSONObject result = new JSONObject();
        result.put(KEY_FOR_STATUS, status);
        result.put(KEY_FOR_DOCUMENT_RESPONSE_LIST, documentResponseList);
        result.put(KEY_FOR_REGISTRY_ERROR_LIST, registryErrorList);
        return result.toJSONString();
    }

    private static String retrieveDocumentEntry(JSONObject documentRequestJson) {
        DocumentRequest documentRequest = toJavaObject(documentRequestJson, DocumentRequest.class);
        String requestQueryResult = requestQuery(documentRequest.getDocumentUniqueId(), documentRequest.getClassCode());
        JSONObject requestQueryResultJson = toJsonObject(requestQueryResult);
        JSONArray entryList = getJsonArray(requestQueryResultJson, KEY_FOR_RESOURCE_ENTRY);
        JSONObject entryJson = getJsonObjectFromArray(entryList, 0);
        String resource = getStringFromObject(entryJson, KEY_FOR_RESOURCE);
        return setDocumentResponse(resource, documentRequest);
    }

    private static String setDocumentResponse(String resource, DocumentRequest documentRequest) {
        DocumentResponse documentResponse = new DocumentResponse();
        documentResponse.setRepositoryUniqueId(documentRequest.getRepositoryUniqueId());
        documentResponse.setDocumentUniqueId(documentRequest.getDocumentUniqueId());
        documentResponse.setType(documentRequest.getClassCode());
        documentResponse.setDocument(resource);
        JSONObject documentResponseJson = toJsonObject(documentResponse.toString());
        return documentResponseJson.toJSONString();
    }
}
