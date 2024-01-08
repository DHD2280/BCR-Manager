package it.dhd.bcrmanager.json;

import android.net.Uri;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class UriJsonAdapter implements JsonSerializer<Uri>, JsonDeserializer<Uri> {
    @Override
    public JsonElement serialize(Uri src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toString());
    }

    @Override
    public Uri deserialize(JsonElement src, Type srcType, JsonDeserializationContext context) {
        try {
            String url = src.getAsString();
            if (url == null || url.isEmpty()) {
                return Uri.EMPTY;
            } else {
                return Uri.parse(url);
            }
        } catch (UnsupportedOperationException e) {
            return Uri.EMPTY;
        }
    }
}
