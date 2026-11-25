
package com.warcontry.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

public class WarContryAuthAPI {

    private static final java.net.http.HttpClient CLIENT =
            java.net.http.HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

    private WarContryAuthAPI() {
    }

    public static String createLinkCode(UUID minecraftUuid, String minecraftUsername)
            throws IOException, InterruptedException {

        String secret = WarContryAuthConfig.MINECRAFT_LINK_SECRET.get();

        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "Le secret Minecraft du Worker n'est pas configuré."
            );
        }

        JsonObject body = new JsonObject();
        body.addProperty("minecraftUuid", minecraftUuid.toString());
        body.addProperty("minecraftUsername", minecraftUsername);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        WarContryAuthConfig.WORKER_URL.get()
                                + "/minecraft-link-create"
                ))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("X-Minecraft-Link-Secret", secret)
                .POST(HttpRequest.BodyPublishers.ofString(
                        body.toString(), StandardCharsets.UTF_8
                ))
                .build();

        HttpResponse<String> response = CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();

        if (response.statusCode() != 200 || !result.has("ok")
                || !result.get("ok").getAsBoolean()) {
            throw new IOException("Erreur Worker : " + response.body());
        }

        if (!result.has("code")) {
            throw new IOException("Le Worker n'a pas retourné de code.");
        }

        return result.get("code").getAsString();
    }


    public static JsonObject checkMinecraftLink(UUID minecraftUuid)
            throws IOException, InterruptedException {

        String secret = WarContryAuthConfig.MINECRAFT_LINK_SECRET.get();

        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "Le secret Minecraft du Worker n'est pas configuré."
            );
        }

        JsonObject body = new JsonObject();
        body.addProperty("minecraftUuid", minecraftUuid.toString());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        WarContryAuthConfig.WORKER_URL.get()
                                + "/minecraft-link-check"
                ))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("X-Minecraft-Link-Secret", secret)
                .POST(HttpRequest.BodyPublishers.ofString(
                        body.toString(), StandardCharsets.UTF_8
                ))
                .build();

        HttpResponse<String> response = CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        JsonObject result = JsonParser.parseString(response.body()).getAsJsonObject();

        if (response.statusCode() != 200 || !result.has("ok")
                || !result.get("ok").getAsBoolean()) {
            throw new IOException("Erreur Worker : " + response.body());
        }

        return result;
    }

    /**
     * Authentifie un joueur avec son compte War Contry.
     */
    public static JsonObject authenticate(String username, String password)
            throws IOException, InterruptedException {

        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("password", password);

        return postMinecraftRequest("/minecraft-authenticate", body);
    }

    /**
     * Vérifie le code de double authentification envoyé par e-mail.
     */
    public static JsonObject authenticate2FA(String challengeId, String code)
            throws IOException, InterruptedException {

        JsonObject body = new JsonObject();
        body.addProperty("challengeId", challengeId);
        body.addProperty("code", code);

        return postMinecraftRequest("/minecraft-authenticate-2fa", body);
    }

    /**
     * Envoie une requête sécurisée au Worker Cloudflare.
     */
    private static JsonObject postMinecraftRequest(String route, JsonObject body)
            throws IOException, InterruptedException {

        String secret = WarContryAuthConfig.MINECRAFT_LINK_SECRET.get();

        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "Le secret Minecraft du Worker n'est pas configuré."
            );
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        WarContryAuthConfig.WORKER_URL.get() + route
                ))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .header("X-Minecraft-Link-Secret", secret)
                .POST(HttpRequest.BodyPublishers.ofString(
                        body.toString(), StandardCharsets.UTF_8
                ))
                .build();

        HttpResponse<String> response = CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        try {
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException(
                    "Réponse invalide du Worker (HTTP "
                            + response.statusCode() + ")."
            );
        }
    }


}
