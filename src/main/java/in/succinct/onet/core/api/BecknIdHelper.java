package in.succinct.onet.core.api;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.routing.Config;
import in.succinct.beckn.Subscriber;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BecknIdHelper {
    public static String getIdPrefix(){
        return Config.instance().getProperty("onet.core.id.prefix", "./");
    }

    public enum Entity {
        fulfillment,
        category,
        // provider, not needed as it is subscriber_id
        provider_category,
        provider_location,
        item,
        catalog,
        cancellation_reason,
        return_reason,
        order,
        payment
    }


    public static String getLocalUniqueId(String beckId, Entity becknEntity) {
        String pattern = "^(.*/)(.*)@(.*)\\." + becknEntity + "$";
        Matcher matcher = Pattern.compile(pattern).matcher(beckId);
        if (matcher.find()){
            return matcher.group(2);
        }
        return "-1";
    }
    public static String getBecknId(String localUniqueId, Subscriber subscriber , Entity becknEntity){
        StringBuilder builder = new StringBuilder();
        builder.append(getIdPrefix());
        if (!ObjectUtil.isVoid(localUniqueId)){
            builder.append(localUniqueId);
        }else {
            builder.append(0);
        }
        builder.append("@");
        builder.append(subscriber.getSubscriberId());
        if (becknEntity != null){
            builder.append(".").append(becknEntity);
        }
        return builder.toString();
    }


}
