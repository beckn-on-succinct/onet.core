package in.succinct.onet.core.api;

import in.succinct.beckn.BecknAware;
import in.succinct.beckn.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessageLoggerFactory implements MessageLogger{
    private static volatile MessageLoggerFactory sSoleInstance;

    //private constructor.
    private MessageLoggerFactory() {
        //Prevent form the reflection api.
        if (sSoleInstance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static MessageLoggerFactory getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            synchronized (MessageLoggerFactory.class) {
                if (sSoleInstance == null) sSoleInstance = new MessageLoggerFactory();
            }
        }

        return sSoleInstance;
    }

    //Make singleton from serialize and deserialize operation.
    protected MessageLoggerFactory readResolve() {
        return getInstance();
    }

    private List<MessageLogger> loggers = new ArrayList<>();
    public void registerMessageLogger(MessageLogger logger){
        loggers.add(logger);
    }

    @Override
    public void log(String direction, Request request, Map<String, String> headers, BecknAware response) {
        for (MessageLogger l : loggers){
            l.log(direction,request,headers,response);
        }
    }
}
