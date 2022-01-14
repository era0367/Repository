package knu.myhealthhub.repository.validator;

import knu.myhealthhub.datamodels.Metadata;

import static knu.myhealthhub.common.Common.getHashSha256;
import static knu.myhealthhub.common.Common.getSize;
import static knu.myhealthhub.enums.ERROR_CODE.*;
import static knu.myhealthhub.settings.Configuration.TRUE;
import static knu.myhealthhub.settings.errors.ErrorUtility.setErrorMessage;

public class MetaValidator {
    public static String isValidMetadata(Metadata metadata, String fhirData) {
        String validateHashResult = isValidHash(metadata.getHash(), fhirData);
        if (!validateHashResult.equals(TRUE)) {
            return validateHashResult;
        }
        String validateSizeResult = isValidSize(metadata.getSize(), fhirData);
        if (!validateSizeResult.equals(TRUE)) {
            return validateSizeResult;
        }
        return TRUE;
    }

    private static String isValidSize(int size, String fhirData) {
        int calculatedSize = getSize(fhirData);
        if (calculatedSize != size) {
            String reason = String.format("Size is wrong - %d", size);
            return setErrorMessage(XDS_NON_IDENTICAL_SIZE, reason);
        }
        return TRUE;
    }

    private static String isValidHash(String hash, String fhirData) {
        fhirData = fhirData.replaceAll("\\\\", "");
        String calculatedHash = getHashSha256(fhirData);
        if (null == calculatedHash) {
            return setErrorMessage(XDS_REPOSITORY_ERROR, null);
        }
        if (!calculatedHash.equals(hash)) {
            String reason = String.format("Hash value is wrong - %s Calculated Hash value is %s", hash, calculatedHash);
            return setErrorMessage(XDS_NON_IDENTICAL_HASH, reason);
        }
        return TRUE;
    }
}