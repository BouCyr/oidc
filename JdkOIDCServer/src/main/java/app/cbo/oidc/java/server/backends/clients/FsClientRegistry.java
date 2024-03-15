package app.cbo.oidc.java.server.backends.clients;

import app.cbo.oidc.java.server.backends.filesystem.FileSpecifications;
import app.cbo.oidc.java.server.backends.filesystem.FileStorage;
import app.cbo.oidc.java.server.scan.BuildWith;
import app.cbo.oidc.java.server.scan.Injectable;
import app.cbo.oidc.java.server.utils.Utils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

@Injectable
public class FsClientRegistry implements ClientRegistry {

    private final static Logger LOGGER = Logger.getLogger(FsClientRegistry.class.getCanonicalName());

    private final FileStorage fsUserStorage;
    private Map<String, String> configured;

    @BuildWith
    public FsClientRegistry(FileStorage fsUserStorage) {
        this.fsUserStorage = fsUserStorage;
        this.configured = readFromFs();
    }

    private Map<String, String> readFromFs() {
        final Map<String, String> diskContents;
        try {
            diskContents = this.fsUserStorage.readMap(FileSpecifications.full("clients.txt","clients"))
                    .orElse(Collections.emptyMap());


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return diskContents;
    }


    @Override
    public boolean authenticate(String clientId, String clientSecret) {


        boolean result = false;
        if(this.configured.containsKey(clientId)){
            LOGGER.info(STR."Client '\{clientId}' is defined in the registry");
            result =  this.configured.get(clientId).equals(clientSecret);
        }else{
            LOGGER.info(STR."Client '\{clientId}' is NOT defined in the registry ; checking if clientId and secret are equles");
            result = !Utils.isEmpty(clientId) && clientId.equals(clientSecret);//TODO [14/04/2023] client registry ?
        }
        LOGGER.info(STR."Client authentication result : \{result ? "OK" : "KO"} for client '\{clientId}'");
        return result;

    }

    @Override
    public Set<String> getRegisteredClients() {
        return this.configured.keySet();
    }

    @Override
    public void setClient(String clientId, String clientSecret) {

        //create new HashMap, we need to be sure this instance is mutable
        var diskContents = new HashMap<>(this.readFromFs());


        var result = Optional.ofNullable(diskContents.put(clientId, clientSecret));

        try {
            this.fsUserStorage.writeMap(FileSpecifications.full("clients.txt", "clients"), diskContents);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.configured = readFromFs();
        LOGGER.info(STR."Client '\{clientId}' has been registered with secret '\{clientSecret}'");
    }
}
