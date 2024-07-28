package com.example.messagerapp.utilities;

import com.example.messagerapp.network.FirebaseAuthHelper;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class Constants {
    public static final String KEY_COLLECTION_USERS = "users";
    public static final String KEY_NAME = "name";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_PHONE = "numberPhone";
    public static final String KEY_PREFERENCE_NAME = "chatAppPreference";
    public static final String KEY_IS_SIGNED_IN = "isSignedIn";
    public static final String KEY_USER_ID = "userID";
    public static final String KEY_IMAGE = "image";
    public static final String KEY_FCM_TOKEN = "fcmToken";
    public static final String KEY_USER = "user";
    public static final String KEY_COLLECTION_CHAT = "chat";
    public static final String KEY_SENDER_ID = "senderID";
    public static final String KEY_RECEIVER_ID = "receiverID";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_COLLECTION_CONVERSATIONS = "conversations";
    public static final String KEY_SENDER_NAME = "senderName";
    public static final String KEY_RECEIVER_NAME = "receiverName";
    public static final String KEY_SENDER_IMAGE = "senderImage";
    public static final String KEY_RECEIVER_IMAGE = "receiverImage";
    public static final String KEY_LAST_MESSAGE = "lastMessage";
    public static final String KEY_AVAILABILITY = "availability";
    public static final String REMOTE_MSG_AUTHORIZATION = "Authorization";
    public static final String REMOTE_MSG_CONTENT_TYPE = "Content-Type";
    public static final String REMOTE_MSG_DATA = "data";
    public static final String REMOTE_MSG_REGISTRATION_IDS = "registration_ids";


    //key mới
    public static final String KEY_MESSAGE_TYPE = "type_message";
    public static final String KEY_FILE_NAME = "name_file";
    public static final String KEY_REACTION = "emoji";
    public static final String KEY_ADDRESS = "address";
    public static final String KEY_BIRTHDATE = "birthdate";
    public static final String KEY_COLLECTION_FRIEND_REQUESTS = "request";
    public static final String KEY_COLLECTION_FRIENDS = "connect-friend";
    public static final String KEY_USER_ID_1 = "ID-1";
    public static final String KEY_USER_ID_2 = "ID-2";
    //


    private static final String SCOPES = "https://www.googleapis.com/auth/firebase.messaging"; // Replace with appropriate scope

    private static final String jsonString = "{\n" +
            "  \"type\": \"service_account\",\n" +
            "  \"project_id\": \"messageapp-f8837\",\n" +
            "  \"private_key_id\": \"393cfc405c865d59b35fec6ccc93b6d8f9d85052\",\n" +
            "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDOsq0UBNxampuj\\nVtQ3WjMDCy45luT1nRS73GU0W8fcccmfSq0CdXTHGGbLRm3/MpgEYyfjAUznJinj\\nOWUG3yMrHHhC8w1HtDckhJ3cez+LsrxTosACY9WkgEbi0pTwzeGP1vJOEZzROjC4\\n4svHCrNMXDQpDYP7fQ0TBvlsRXduMR863Fx1YgIVB98EOSEKn02a4gFxSA/z9BMt\\nqAVXh/AhNX5EUqd2Jn2BMnx6wl/GcvR+AjGpf/PP+VVUlZi71ptthOSuTaV+dl/e\\nxbom3rQDssuf+H2tisFIVNHDU4uhR7PzmIU7o5ML1kGlL9wWiYiR+UKnwEmv6ISg\\njGpxf6+HAgMBAAECggEAUcfsJwGNVePaNJREPupXSJsB3RNQ38UY0QNwn3i46QW0\\namwXa+OhIq0K7t6c4t3Xu9SnkW+QR9yXsimhSUi8F3CaPJMB7B7nunEmLHgvOkyr\\nqsO4CLoLa5y9bz02ZNhwqIK5OB5L15SYdm51hfk727iXgq6wS668n4yg3y/VAymC\\nAnMPWYc1sNK/AD7CFCZcC2QxHJ7WglvvbYE7Nsi47noCcUXcx+7SzQrxYp4aqfhs\\nTkVgcYOqKjA4njer7hcxFR4xqwtQisFET5SPeGQr3siudpYBi1cKkFJ3GAJV8cbt\\nmWj+vBG8bf2O2Fi26zHF2Thz1bMBGc3N2SY6JIZi0QKBgQD0E62pXIhHVtFQOG+O\\nbjzg+r9n1jSpXKYb02eer30aDKdiwi0CiWCR6FZit8HhBGGEaC9vbvOOEzRFjFZF\\nLoMUymqSBM0WJzr5/+qdZW8oPfEA8W1oZYeRSaA5mSKF4YVlvGxoxqbCAbCmhDXG\\nilv3tuhaUkVykrxIKLEpygr6KwKBgQDYy42KAD38I8J+5CLMnTWes9a7o5Eb5t/Z\\nDHzGik5odhuxcVxCedvAZTVJZGL+HZjtsvX6Qe6iYI37y6QG3O3OMQ9lWU5e+8DO\\nuvhEGVOLWTDjHTtUdBkqRmksJwSilhFVQuUvGx8M3wDP2aQPd4R0kgZLN7whcpo9\\n3Fckgmh+FQKBgQDe9ssBl8H2SVzUh3mBBzsd2nHXjVp2DGjBqpFR6MXDciPGl9M+\\nKfjJ0RB5BZxazgG3TuVmSli7RNfPYK++awrbhz1rPm+K+TNrBVlxhyPQAyWR0vo7\\nD+ST2EpB054x5x5RHZt6612ShLC2rLfjMqlo1PHU8Kr4Swssb9/HaQY2GQKBgFGz\\nVGHfeXyeFVwwqZSm4mAusgwAtlzngxCPDi7cTHQ9nm4epIBA4Dn/ajeAZ3YQvWNH\\nAEARvNbgwGFV+zjC5bPA9WZiY5CUG7NM7ubrDHsFX9EXYRaQsjMmWdhT1AFZwKp7\\nDVfVRrxZBjtOb++MQRSVO135YSLRx6LseMK/ipWJAoGAScy0/LmkRqrc3xrw2C5I\\nnDJqdtusty/e3AEsNCw9SWPjQ8OiozHo+pcz9P7xT9Trzd23tU9uDtZl/JMe83PP\\n4FX8ePv9EM1LmU02V/ie2FoVdtLwLGmUzObr/+SsaastdoLnZVE8JeElOOVC2gyc\\nwQf5SiT06qbsNozrdIJLBgI=\\n-----END PRIVATE KEY-----\\n\",\n" +
            "  \"client_email\": \"firebase-adminsdk-ujqjg@messageapp-f8837.iam.gserviceaccount.com\",\n" +
            "  \"client_id\": \"117730295088697186829\",\n" +
            "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
            "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
            "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
            "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-ujqjg%40messageapp-f8837.iam.gserviceaccount.com\",\n" +
            "  \"universe_domain\": \"googleapis.com\"\n" +
            "}\n";

    private static String getAccessToken() throws IOException {
        // Chuyển chuỗi JSON thành một luồng đầu vào
        ByteArrayInputStream serviceAccountStream = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));

        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(serviceAccountStream)
                .createScoped(Arrays.asList(SCOPES)); // hoặc SCOPES
        googleCredentials.refresh();
        return googleCredentials.getAccessToken().getTokenValue();
    }

    public static HashMap<String, String> remoteMsgHeaders = null;
    public static HashMap<String, String> getRemoteMsgHeaders() {
        if (remoteMsgHeaders == null) {
            remoteMsgHeaders = new HashMap<>();
            try {
                remoteMsgHeaders.put(
                        REMOTE_MSG_AUTHORIZATION,
                        "Bearer" + getAccessToken()
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            remoteMsgHeaders.put(
                    REMOTE_MSG_CONTENT_TYPE,
                    "application/json"
            );
        }
        return remoteMsgHeaders;
    }

}
