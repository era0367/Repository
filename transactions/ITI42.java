package knu.myhealthhub.repository.transactions;

import knu.myhealthhub.datamodels.Metadata;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.http.HttpMethod;

import static knu.myhealthhub.common.JsonUtility.*;
import static knu.myhealthhub.enums.ERROR_CODE.XDS_REPOSITORY_ERROR;
import static knu.myhealthhub.repository.RepositoryManager.getRepositoryUniqueId;
import static knu.myhealthhub.repository.RepositoryManager.insertDocument;
import static knu.myhealthhub.settings.Configuration.*;
import static knu.myhealthhub.settings.KeyString.*;
import static knu.myhealthhub.settings.errors.ErrorUtility.setErrorMessage;
import static knu.myhealthhub.transactions.RestSender.createRest;
import static knu.myhealthhub.transactions.RestSender.getHeader;

public class ITI42 {
    public static String registerDocumentSet(String body) {
        JSONObject parameter = toJsonObject(body);
        assert parameter != null;
        JSONArray metadataList = getJsonArray(parameter, KEY_FOR_META_LIST);
        JSONArray documentList = getJsonArray(parameter, KEY_FOR_FHIR_LIST);
        JSONArray registryErrorList = new JSONArray();
        String status = SUCCESS;
        for (int i = 0; i < metadataList.size(); i++) {
            JSONObject metadataJson = getJsonObjectFromArray(metadataList, i);
            JSONObject fhirDataJson = getJsonObjectFromArray(documentList, i);
            String registerDocumentSetResult = registerDocumentEntry(metadataJson, fhirDataJson);
            if (null == registerDocumentSetResult) {
                status = FAILURE;
                String errorMessage = setErrorMessage(XDS_REPOSITORY_ERROR, null);
                JSONObject errorMessageJson = toJsonObject(errorMessage);
                registryErrorList.add(errorMessageJson);
                continue;
            }
            JSONObject registerDocumentSetResultJson = toJsonObject(registerDocumentSetResult);
            if (null == registerDocumentSetResultJson) {
                status = FAILURE;
                String errorMessage = setErrorMessage(XDS_REPOSITORY_ERROR, null);
                JSONObject errorMessageJson = toJsonObject(errorMessage);
                registryErrorList.add(errorMessageJson);
                continue;
            }
            String statusResult = getStringFromObject(registerDocumentSetResultJson, KEY_FOR_STATUS);
            if (!statusResult.equals(TRUE)) {
                status = FAILURE;
                String errorMessage = setErrorMessage(XDS_REPOSITORY_ERROR, registerDocumentSetResultJson.toJSONString());
                JSONObject errorMessageJson = toJsonObject(errorMessage);
                registryErrorList.add(errorMessageJson);
            }
        }
        JSONObject result = new JSONObject();
        result.put(KEY_FOR_STATUS, status);
        result.put(KEY_FOR_REGISTRY_ERROR_LIST, registryErrorList);
        return result.toJSONString();
    }

    private static String registerDocumentEntry(JSONObject metadataJson, JSONObject fhirDataJson) {
        String resourceType = getStringFromObject(fhirDataJson, KEY_FOR_RESOURCE_TYPE);
        String insertDataResult = insertDocument(fhirDataJson, resourceType);
        if (isJsonObject(insertDataResult)) {
            return insertDataResult;
        }
        Metadata metadata = toJavaObject(metadataJson, Metadata.class);
        Metadata updateMetadataResult = updateMetadata(metadata, insertDataResult);
        return requestRegistration(updateMetadataResult);
    }

    private static Metadata updateMetadata(Metadata metadata, String uri) {
        String repositoryUniqueId = getRepositoryUniqueId();
        metadata.setRepositoryUniqueId(repositoryUniqueId);
        metadata.setUri(uri);
        return metadata;
    }

    private static JSONObject setMetadataList(Metadata metadata) {
        JSONArray metadataList = new JSONArray();
        metadataList.add(toJsonObjectFromJavaObject(metadata));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(KEY_FOR_META_LIST, metadataList);
        return jsonObject;
    }

    private static String requestRegistration(Metadata metadata) {
        JSONObject jsonObject = setMetadataList(metadata);
        String url = URL_HEADER + BLOCKCHAIN_REGISTRY_ENDPOINT + URL_DEFAULT + METADATA;
        return createRest(url, HttpMethod.POST, getHeader(null), jsonObject);
    }
}