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
import java.util.Set;
import java.util.logging.Logger;

/**
 * This class represents a file system-based client registry.
 * It implements the ClientRegistry interface and provides methods for client authentication and management.
 * The clients are stored in a file system, with the client ID as the key and the client secret as the value.
 */
@Injectable
public class FsClientRegistry implements ClientRegistry {

    private final static Logger LOGGER = Logger.getLogger(FsClientRegistry.class.getCanonicalName());

    private final FileStorage fsUserStorage;
    private Map<String, String> configured;

    /**
     * This constructor initializes the file storage and reads the clients from the file system.
     *
     * @param fsUserStorage The file storage to be used.
     */
    @BuildWith
    public FsClientRegistry(FileStorage fsUserStorage) {
        this.fsUserStorage = fsUserStorage;
        this.configured = readFromFs();
    }

    /**
     * This method reads the clients from the file system.
     *
     * @return Returns a map with the client ID as the key and the client secret as the value.
     */
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

    /**
     * This method is used to authenticate a client.
     * It checks if the provided client ID and client secret match the ones stored in the file system.
     *
     * @param clientId     The ID of the client to be authenticated.
     * @param clientSecret The secret of the client to be authenticated.
     * @return             Returns true if the client ID and client secret match the ones stored in the file system, false otherwise.
     */
    @Override
    public boolean authenticate(String clientId, String clientSecret) {


        boolean result = false;
        if(this.configured.containsKey(clientId)){
            LOGGER.info(STR."Client '\{clientId}' is defined in the registry");
            result =  this.configured.get(clientId).equals(clientSecret);
        }else{
            LOGGER.info(STR."Client '\{clientId}' is NOT defined in the registry ; checking if clientId and secret are equles");
            result = !Utils.isEmpty(clientId) && clientId.equals(clientSecret);
        }
        LOGGER.info(STR."Client authentication result : \{result ? "OK" : "KO"} for client '\{clientId}'");
        return result;

    }

    /**
     * This method is used to get the IDs of all registered clients.
     *
     * @return Returns a set containing the IDs of all registered clients.
     */
    @Override
    public Set<String> getRegisteredClients() {
        return this.configured.keySet();
    }

    /**
     * This method is used to register a new client or update an existing one.
     * It adds the provided client ID and client secret to the file system.
     *
     * @param clientId     The ID of the client to be registered or updated.
     * @param clientSecret The secret of the client to be registered or updated.
     */
    @Override
    public void setClient(String clientId, String clientSecret) {

        //create new HashMap, we need to be sure this instance is mutable
        var diskContents = new HashMap<>(this.readFromFs());


        var isAReplacement = (diskContents.put(clientId, clientSecret)) != null;


        try {
            this.fsUserStorage.writeMap(FileSpecifications.full("clients.txt", "clients"), diskContents);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.configured = readFromFs();
        if(isAReplacement) {
            LOGGER.info(STR."Client '\{clientId}' has been updated with secret '\{clientSecret}'");
        } else {
            LOGGER.info(STR."Client '\{clientId}' has been registered with secret '\{clientSecret}'");
        }
    }
}