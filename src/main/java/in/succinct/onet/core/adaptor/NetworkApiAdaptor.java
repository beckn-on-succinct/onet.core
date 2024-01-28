package in.succinct.onet.core.adaptor;

import com.venky.swf.routing.Config;
import in.succinct.beckn.BecknAware;
import in.succinct.beckn.Request;
import in.succinct.onet.core.api.MessageLoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public abstract class NetworkApiAdaptor {
    private final NetworkAdaptor networkAdaptor ;
    public NetworkApiAdaptor(NetworkAdaptor networkAdaptor) {
        this.networkAdaptor = networkAdaptor;
    }

    public NetworkAdaptor getNetworkAdaptor() {
        return networkAdaptor;
    }

    public void log(String direction,
                    Request request, Map<String, String> headers, BecknAware response,
                    String url) {
        Map<String,String> maskedHeaders = new HashMap<>();
        headers.forEach((k,v)->{
            maskedHeaders.put(k, Config.instance().isDevelopmentEnvironment()? v : "***");
        });
        Config.instance().getLogger(NetworkApiAdaptor.class.getName()).log(Level.INFO,String.format("%s|%s|%s|%s|%s",direction,request,headers,response,url));
        MessageLoggerFactory.getInstance().log(direction,request,headers,response);

    }
}
