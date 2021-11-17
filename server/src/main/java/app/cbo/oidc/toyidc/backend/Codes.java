package app.cbo.oidc.toyidc.backend;

import app.cbo.oidc.toyidc.functions.CodeProvider;
import app.cbo.oidc.toyidc.functions.CodeRetriever;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class Codes implements CodeProvider, CodeRetriever {

    private Map<String, StoredCode> store = new HashMap<>();

    public String store(String clientId, String nonce,  String redirectUri, String userId ){
        String code = UUID.randomUUID().toString();

        this.store.put(code, new StoredCode(userId, nonce, clientId, redirectUri));

        return code;
    }

    public Optional<StoredCode> retrieve(String code) {
        return Optional.ofNullable(this.store.remove(code));
    }
}
