package com.xulai.elementalcraft.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xulai.elementalcraft.ElementalCraft;
import net.minecraftforge.fml.ModList;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {
    public static final String MODRINTH_URL = "https://modrinth.com/mod/elementalcraft-reactions/versions";
    public static final String CURSEFORGE_URL = "https://www.curseforge.com/minecraft/mc-mods/elementalcraft-reactions/files/all?page=1&pageSize=20&version=1.20.1&showAlphaFiles=hide";

    private static volatile boolean hasUpdate = false;
    private static volatile String latestVersion = "";

    public static void checkForUpdate() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.modrinth.com/v2/project/elementalcraft-reactions/version"))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "elementalcraft-reactions/1.0")
                .build();

        CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) return null;
                return response.body();
            } catch (Exception e) {
                ElementalCraft.LOGGER.warn("[ElementalCraft] Update check failed: {}", e.getMessage());
                return null;
            }
        }).thenAccept(body -> {
            if (body == null) return;
            try {
                String currentVersion = ModList.get().getModContainerById(ElementalCraft.MODID)
                        .map(container -> container.getModInfo().getVersion().toString())
                        .orElse(null);
                if (currentVersion == null) return;

                JsonArray versions = JsonParser.parseString(body).getAsJsonArray();
                String newest = null;

                for (JsonElement el : versions) {
                    JsonObject versionObj = el.getAsJsonObject();
                    JsonArray loaders = versionObj.getAsJsonArray("loaders");
                    JsonArray gameVersions = versionObj.getAsJsonArray("game_versions");

                    boolean hasForge = false;
                    boolean hasMC1201 = false;
                    for (JsonElement l : loaders) {
                        if (l.getAsString().equals("forge")) { hasForge = true; break; }
                    }
                    for (JsonElement g : gameVersions) {
                        if (g.getAsString().equals("1.20.1")) { hasMC1201 = true; break; }
                    }
                    if (!hasForge || !hasMC1201) continue;

                    String ver = versionObj.get("version_number").getAsString();
                    if (newest == null || compareVersions(ver, newest) > 0) {
                        newest = ver;
                    }
                }

                if (newest != null && compareVersions(newest, currentVersion) > 0) {
                    hasUpdate = true;
                    latestVersion = newest;
                    ElementalCraft.LOGGER.info("[ElementalCraft] Update available: {} -> {}", currentVersion, newest);
                }
            } catch (Exception e) {
                ElementalCraft.LOGGER.warn("[ElementalCraft] Update check parse failed: {}", e.getMessage());
            }
        });
    }

    public static boolean hasUpdate() {
        return hasUpdate;
    }

    public static String getLatestVersion() {
        return latestVersion;
    }

    private static int compareVersions(String a, String b) {
        String[] partsA = a.split("\\.");
        String[] partsB = b.split("\\.");
        int len = Math.max(partsA.length, partsB.length);
        for (int i = 0; i < len; i++) {
            int numA = i < partsA.length ? parseIntSafe(partsA[i]) : 0;
            int numB = i < partsB.length ? parseIntSafe(partsB[i]) : 0;
            if (numA != numB) return Integer.compare(numA, numB);
        }
        return 0;
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}