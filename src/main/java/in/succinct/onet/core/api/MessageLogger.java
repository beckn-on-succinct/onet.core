package in.succinct.onet.core.api;

import in.succinct.beckn.BecknAware;
import in.succinct.beckn.Request;

import java.util.Map;

public interface MessageLogger {
    String FROM_NETWORK= "FromNetwork" ;
    String TO_APP = "ToApplication" ;
    String FROM_APP ="FromApplication" ;
    String TO_NET = "ToNetwork";

    public void log(String direction,
                    Request request, Map<String, String> headers, BecknAware response);


}
