package MidiControl.Access;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.google.gson.JsonObject;

public class CredentialStore {
    private record Credential(Authenticate.Role role, JsonObject metadata) {}

    private final Map<String, Credential> directory = new HashMap<>();

    public void storeBearerToken(String token, Authenticate.Role role) {
        storeBearerToken(token, role, new JsonObject());
    }

    public void storeBearerToken(String token, Authenticate.Role role, JsonObject metadata) {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(metadata, "metadata");
        directory.put(token, new Credential(role, metadata.deepCopy()));
    }

    public boolean checkTokenExists(String token) {
        return directory.containsKey(token);
    }

    public Optional<Authenticate.Role> getTokenRole(String token) {
        return Optional.ofNullable(directory.get(token)).map(Credential::role);
    }

    public Optional<JsonObject> getTokenMetadata(String token) {
        return Optional.ofNullable(directory.get(token))
                .map(Credential::metadata)
                .map(JsonObject::deepCopy);
    }
}
