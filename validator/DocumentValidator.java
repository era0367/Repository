package knu.myhealthhub.repository.validator;

import knu.myhealthhub.datamodels.Consent;
import knu.myhealthhub.datamodels.DocumentRequest;
import knu.myhealthhub.enums.CONSENT_STATUS;
import org.json.simple.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;

import static knu.myhealthhub.common.JsonUtility.*;
import static knu.myhealthhub.enums.CONSENT_STATUS.ACCEPTED;
import static knu.myhealthhub.enums.ERROR_CODE.XDS_REPOSITORY_ERROR;
import static knu.myhealthhub.enums.ERROR_CODE.XDS_UNKNOWN_REPOSITORY_ID;
import static knu.myhealthhub.repository.RepositoryManager.getRepositoryUniqueId;
import static knu.myhealthhub.repository.RepositoryManager.getTotalCount;
import static knu.myhealthhub.settings.Configuration.*;
import static knu.myhealthhub.settings.KeyString.KEY_FOR_CONSENT;
import static knu.myhealthhub.settings.KeyString.KEY_FOR_CONSENT_ID;
import static knu.myhealthhub.settings.errors.ErrorUtility.setErrorMessage;
import static knu.myhealthhub.transactions.RestSender.createRest;
import static knu.myhealthhub.transactions.RestSender.getHeader;

public class DocumentValidator {
    public static String isValidDocumentRequest(DocumentRequest documentRequest) {
        String validateRepositoryUniqueIdResult = isValidRepositoryUniqueId(documentRequest.getRepositoryUniqueId());
        if (!validateRepositoryUniqueIdResult.equals(TRUE)) {
            return validateRepositoryUniqueIdResult;
        }
        String validateDocumentResult = isValidDocument(documentRequest.getDocumentUniqueId(), documentRequest.getClassCode());
        if (!validateDocumentResult.equals(TRUE)) {
            return validateDocumentResult;
        }
        return isValidConsent(documentRequest.getConsentId(), documentRequest.getDataConsumerId(), documentRequest.getToken());
    }

    private static String isValidRepositoryUniqueId(String documentRequestRepositoryId) {
        String repositoryUniqueId = getRepositoryUniqueId();
        if (!repositoryUniqueId.equals(documentRequestRepositoryId)) {
            String reason = String.format("Wrong repositoryUniqueId[%s]", documentRequestRepositoryId);
            return setErrorMessage(XDS_UNKNOWN_REPOSITORY_ID, reason);
        }
        return TRUE;
    }

    private static String isValidDocument(String documentId, String classCode) {
        String totalString = getTotalCount(documentId, classCode);
        int total = Integer.parseInt(totalString);
        if (1 != total) {
            String reason = String.format("Fail to find document, total count is [%s]", totalString);
            return setErrorMessage(XDS_REPOSITORY_ERROR, reason);
        }
        return TRUE;
    }

    private static String isValidConsent(String consentId, String dataConsumerId, String token) {
        String response = requestConsent(consentId);
        if (null == response) {
            return setErrorMessage(XDS_REPOSITORY_ERROR, null);
        }
        JSONObject responseJson = toJsonObject(response);
        if (null == responseJson) {
            String reason = String.format("Fail to parse string to JSON - %s", response);
            return setErrorMessage(XDS_REPOSITORY_ERROR, reason);
        }
        return parseRequestConsentResult(responseJson, dataConsumerId, token);
    }

    private static String requestConsent(String consentId) {
        String url = CONSENT_ENDPOINT + URL_DEFAULT + CONSENT + QUERY;
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url).queryParam(KEY_FOR_CONSENT_ID, consentId);
        return createRest(builder.toUriString(), HttpMethod.GET, getHeader(null), new JSONObject());
    }

    private static String parseRequestConsentResult(JSONObject jsonObject, String dataConsumerId, String token) {
        JSONObject consentJson = getJsonObject(jsonObject, KEY_FOR_CONSENT);
        if (null == consentJson) {
            String reason = String.format("Fail to find key[%s] from %s", KEY_FOR_CONSENT, jsonObject.toJSONString());
            return setErrorMessage(XDS_REPOSITORY_ERROR, reason);
        }
        Consent consent = toJavaObject(consentJson, Consent.class);
        if (null == consent) {
            String reason = String.format("Fail to parse JSON to JavaObject[Consent] %s", consentJson.toJSONString());
            return setErrorMessage(XDS_REPOSITORY_ERROR, reason);
        }
        CONSENT_STATUS consentStatus = consent.getStatus();
        if (!consentStatus.equals(ACCEPTED)) {
            String reason = String.format("consent state[%s] is not available", consentStatus);
            return setErrorMessage(XDS_REPOSITORY_ERROR, reason);
        }
        String dataConsumer = consent.getDataConsumerId();
        if (!dataConsumer.equals(dataConsumerId)) {
            String reason = String.format("dataConsumerId[%s] is wrong", dataConsumerId);
            return setErrorMessage(XDS_REPOSITORY_ERROR, reason);
        }
        String validToken = consent.getToken();
        if (!validToken.equals(token)) {
            String reason = String.format("token[%s] is invalid", token);
            return setErrorMessage(XDS_REPOSITORY_ERROR, reason);
        }
        return TRUE;
    }
}