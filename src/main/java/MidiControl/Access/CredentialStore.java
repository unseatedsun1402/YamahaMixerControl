package MidiControl.Access;

import java.util.HashMap;
import java.util.Optional;

public class CredentialStore {
    private final HashMap<String,Authenticate.Role> directory = new HashMap();

    public void storeBearerToken(String token, Authenticate.Role role){
        directory.put(token,role);
        System.out.println(String.format("Stored %s for role %s",token,role));
    }

    public boolean checkTokenExists(String token){
        return directory.get(token) == null ? false : true;
    }

    public Optional<Authenticate.Role> getTokenRole(String token){
        if (! checkTokenExists(token)) { return Optional.empty(); }
        return Optional.of(directory.get(token));
    }
}
