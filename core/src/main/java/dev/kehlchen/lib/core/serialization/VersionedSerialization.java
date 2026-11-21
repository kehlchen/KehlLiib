package dev.kehlchen.lib.core.serialization;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class VersionedSerialization {
    public static String envelope(int version, String payload) {
        JsonObject object = new JsonObject();
        object.addProperty("version", version);
        object.addProperty("data", payload);
        return object.toString();
    }
    public static int version(String envelope) {
        return JsonParser.parseString(envelope).getAsJsonObject().get("version").getAsInt();
    }
    public static String payload(String envelope) {
        return JsonParser.parseString(envelope).getAsJsonObject().get("data").getAsString();
    }
}
