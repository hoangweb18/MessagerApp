package com.example.messagerapp.network;

import android.util.Log;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.Lists;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class FirebaseAuthHelper {

    private static final String SCOPE = "https://developers.google.com/identity/protocols/oauth2/scopes";

    public String getAccessToken() {
        try {
            String jsonString = "{\n" +
                    "  \"type\": \"service_account\",\n" +
                    "  \"project_id\": \"messageapp-f8837\",\n" +
                    "  \"private_key_id\": \"489ac05fa08663cd9799ec16aa1557275ea6c598\",\n" +
                    "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCorfC4aQ2roRuA\\nn86UnqGGHzQMt/R/Ukg8I0bYwzuwTRTyt4AKw9ObRRnWHIIkOQddUhNMf8ljsIbT\\nM6K3bu5CFldbDWtWKCcMNW1mgbSYduuHMC5yNJN6891AQu/uxx8Ql5EYCg2KBKzx\\nyvNQEOAaN6qgDYZbY1G8ToU5uZi3E3pKLEF3rufxi+RY4Hu8F7R2xiQuJtGMblSl\\nDwwQ+v/1fIf93Yzr5IHf0Fj06ojUtH4P9prfnlfOAwqtCjq9l62QlD/dSbMSpXhc\\ni2tiBgvSAaN+X6kKJlGZHqe4wndMSymM4b4uBC2X/8W4zpwuGc5BHt2Ju12l6iPz\\nlLQMUkzVAgMBAAECggEAI+FQDaIHDuWHuJ4FrtZpbGKxyyzMHNMvW19zoAMzbKaQ\\n2KpQ2blbBzG1M7a/th3NY7vkMAcUmxh/LWjT6Wn202b2MP+TP/HtkGrC+SWI+U9+\\nfTSyBIgLgQFjpFo4uFVx1bEv42msBJpHAW3WwMa+LMV/jWkjFch0J/aZ3zQL+JEa\\njG66TI5TpSTplK9rr17H83AjtNFTstctif0HRzFyu6l7FX7pUwPkORyMCi0GK9tF\\n8aUPFtf4cX2LOj4enoAWAcziFuUX/6qSHutXmINcaT4DwXxAuZKjTOyKHa/oJTi0\\nheGKUXiPTjA1xrfyt4qe7WdbqWabLa2OWwx0tDqlBwKBgQDqUoomypPkmZCUgMbP\\nlyRzAHonvEiyMFeg3dAU0DCsk+54+F4TGN1nD1J3bvrJc0CBSFtnkRi9nh6hsW8A\\ndLPnImEF6EQUpdhrf3jyXnoKgIg+c+pi6Y8oPlcxU98xQj5sXkk48tiWIYr7foPr\\nGlgwpVRiOK1kKXYWS5TGKwQeCwKBgQC4SMcQ8ocEDRyQhP68NRCmcxc3Fxqq4m5Y\\nc8gL4XW8Aa7XuC1rtDWbSYgPlaebvSKaV5+9vArRIdXC0Ca9XPBv0ePJazw8EF2f\\nc9SDWvWNYHOCVxleXuRdeEY3tIpJL7BkcAP46dbklgE+7kL9QmmUkrkizG01Irbq\\nSSUqkaVsnwKBgBRw0NZjc5VMhB1AQpeXUrmpf/55YUoSB4Lj9qPqKWTUnsf0upgl\\n4iJ8ZrA+gSgTorvkaPzW3nakFX37cXhztcHi8N3IDjBS61ctToquDS1fWwM/r2Gj\\niDZ1gbuu2lh8s99WNkL+hEBMeshHmBBpQuozcfIDibSkHUYCn97Nn9VTAoGAPUEz\\nqhZzeo1BKWiLo9OfrTQkS1OKsbKCKzSREiHnGI8Wpu25a0uDZhKS/1snU0US82qc\\n1lxi3BD7FL5LojX6VbDWnmTeqLhn5lCCGwdpB40/19tLu2qfTdJkxK9mJ92KLFhv\\neZWAsIqL73cTHUut/IyorUB2LGtVkZqul+/BpNECgYAz7AcbJLFoXacZp5JeYnb6\\niFOzzTXz1D/hiKc2RdmJQTioFgjHkliUf152hLIadS55CYfcyiStQHkv1OTj0Ccs\\niI0W4yGdbYzzB/8syGg2WdftUhWgjffxDn4oMkiF3fZnp9hb1OEqTnxJpRXRAsF1\\niCKQty0A/NTpgxpMUUa1Dg==\\n-----END PRIVATE KEY-----\\n\",\n" +
                    "  \"client_email\": \"firebase-adminsdk-ujqjg@messageapp-f8837.iam.gserviceaccount.com\",\n" +
                    "  \"client_id\": \"117730295088697186829\",\n" +
                    "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
                    "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
                    "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
                    "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-ujqjg%40messageapp-f8837.iam.gserviceaccount.com\",\n" +
                    "  \"universe_domain\": \"googleapis.com\"\n" +
                    "}\n";

            InputStream stream = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));
            GoogleCredentials googleCredentials = GoogleCredentials.fromStream(stream).createScoped(Lists.newArrayList(SCOPE));
            googleCredentials.refresh();
            return googleCredentials.getAccessToken().getTokenValue();

        } catch (IOException e) {
            Log.e("error", "" + e.getMessage());
            return null;
        }
    }

}
