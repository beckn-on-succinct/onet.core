package in.succinct.onet.core.adaptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

    private final Map<String,NetworkAdaptor> cache = Collections.synchronizedMap(new HashMap<>());
    public void registerAdaptor(NetworkAdaptor adaptor){
        cache.put(adaptor.getId(),adaptor);
    }


    public NetworkAdaptor getAdaptor(String networkName){
        NetworkAdaptor adaptor =  cache.get(networkName);
        if (adaptor == null){
            throw new RuntimeException("No adaptor is registered for onet: " +networkName);
        }
        return adaptor;
    }
}
