package in.succinct.onet.core.adaptor;

import com.venky.swf.routing.Config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class NetworkAdaptorFactory {
    private static volatile NetworkAdaptorFactory sSoleInstance;

    //private constructor.
    private NetworkAdaptorFactory() {
        //Prevent form the reflection api.
        if (sSoleInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static NetworkAdaptorFactory getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            synchronized (NetworkAdaptorFactory.class) {
                if (sSoleInstance == null) sSoleInstance = new NetworkAdaptorFactory();
            }
        }

        return sSoleInstance;
    }

    //Make singleton from serialize and deserialize operation.
    protected NetworkAdaptorFactory readResolve() {
        return getInstance();
    }

    private final Map<String,NetworkAdaptor> cache = new HashMap<>();
    public void registerAdaptor(NetworkAdaptor adaptor){
        synchronized (cache) {
            NetworkAdaptor existing = cache.get(adaptor.getId());
            if (existing == null ) {
                cache.put(adaptor.getId(), adaptor);
            }else if (existing != adaptor) {
                Config.instance().getLogger(getClass().getName()).log(Level.WARNING,"Another adaptor already registered for "+ adaptor.getId());
            }
        }
    }


    public NetworkAdaptor getAdaptor(String networkName){
        NetworkAdaptor adaptor =  cache.get(networkName) ;
        if (adaptor == null) {
            synchronized (cache) {
                adaptor = cache.get(networkName);
            }
        }
        if (adaptor == null){
            throw new RuntimeException("No adaptor is registered for onet: " +networkName);
        }
        return adaptor;
    }
}
