package in.succinct.onet.core.adaptor;


import com.venky.cache.Cache;
import com.venky.core.collections.SequenceSet;
import com.venky.core.date.DateUtils;
import com.venky.core.security.Crypt;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.CryptoKey;
import com.venky.swf.integration.api.Call;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.integration.api.InputFormat;
import com.venky.swf.routing.Config;
import in.succinct.beckn.BecknObject;
import in.succinct.beckn.BecknObjectWithId;
import in.succinct.beckn.BecknObjectsWithId;
import in.succinct.beckn.Request;
import in.succinct.beckn.Subscriber;

import in.succinct.json.JSONAwareWrapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;

public abstract class NetworkAdaptor extends BecknObjectWithId {
    protected NetworkAdaptor(){

    }
    protected NetworkAdaptor(String networkName){
        setId(networkName);
        getInner().putAll(getConfig());
    }
    public  JSONObject getConfig(){
        try {
            return  (JSONObject) JSONValue.parseWithException(new InputStreamReader(
                Objects.requireNonNull(Config.class.getResourceAsStream(String.format("/config/networks/%s.json", getId())))));
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }




    public boolean isSelfRegistrationSupported(){
        return getBoolean("self_registration_supported");
    }
    
    public void setSelfRegistrationSupported(boolean self_registration_supported){
        set("self_registration_supported",self_registration_supported);
    }

    public boolean isSubscriptionNeededPostRegistration(){
        return getBoolean("subscription_needed_post_registration", true);
    }
    public void setSubscriptionNeededPostRegistration(boolean subscription_needed_post_registration){
        set("subscription_needed_post_registration",subscription_needed_post_registration);
    }
    public Domains getDomains(){
        return get(Domains.class, "domains");
    }
    public void setDomains(Domains domains){
        set("domains",domains);
    }

    private Subscriber registry = null;
    public Subscriber getRegistry(){
        if (registry != null){
            return registry;
        }
        synchronized (this){
            if (registry == null) {
                List<Subscriber> subscribers = lookup(getRegistryId(), true);
                if (!subscribers.isEmpty()) {
                    registry = subscribers.get(0);
                }
            }
        }
        return registry;
    }

    public String getCoreVersion(){
        return get("core_version");
    }
    public void setCoreVersion(String core_version){
        set("core_version",core_version);
    }

    public String getSearchProviderId(){
        return get("search_provider_id");
    }
    public void setSearchProviderId(String search_provider_id){
        set("search_provider_id",search_provider_id);
    }

    public Subscriber getSearchProvider(){
        if (ObjectUtil.isVoid(getSearchProviderId())){
            return null;
        }
        List<Subscriber> subscribers = lookup(getSearchProviderId(),true);
        if (!subscribers.isEmpty()){
            return subscribers.get(0);
        }
        return null;
    }

    public String getRegistryId() {
        return get("registry_id");
    }

    public String getRegistryUrl(){
        return get("registry_url");
    }
    public void setRegistryUrl(String registry_url){
        set("registry_url",registry_url);
    }

    public String getUniqueKeyId(){
        return get("unique_key_id","unique_key_id");
    }
    public String getBaseUrl(){
        return get("base_url");
    }

    public void setBaseUrl(String base_url){
        set("base_url",base_url);
    }

    public int getKeyExpiryGracePeriod(){
        return getInteger("key_expiry_grace_period" ,0 );
    }
    public void setKeyExpiryGracePeriod(int key_expiry_grace_period){
        set("key_expiry_grace_period",key_expiry_grace_period);
    }
    public List<Subscriber> lookup(String subscriberId, boolean onlyIfSubscribed) {
        Subscriber subscriber = new Subscriber();
        subscriber.setSubscriberId(subscriberId);
        return lookup(subscriber,onlyIfSubscribed);
    }


    public long getKeyValidityMillis(){
        return Duration.ofDays(365*10).toMillis();
    }

    public CryptoKey getKey(String alias, String purpose){
        return CryptoKey.find(alias,purpose);
    }
    public void rotateKeys(Subscriber subscriber) {
        CryptoKey existingKey = getKey(subscriber.getAlias(),CryptoKey.PURPOSE_SIGNING);

        if (existingKey.getRawRecord().isNewRecord() || existingKey.getUpdatedAt().getTime() + getKeyValidityMillis() <= System.currentTimeMillis() ){
            KeyPair signPair = Crypt.getInstance().generateKeyPair(Request.SIGNATURE_ALGO, Request.SIGNATURE_ALGO_KEY_LENGTH);
            KeyPair encPair = Crypt.getInstance().generateKeyPair(Request.ENCRYPTION_ALGO, Request.ENCRYPTION_ALGO_KEY_LENGTH);
            /*
            String keyNumber = existingKey.getAlias().substring(existingKey.getAlias().lastIndexOf('.')+2);// .k[0-9]*
            int nextKeyNumber = Integer.parseInt(keyNumber) + 1;
            String nextKeyId = String.format("%s.k%d",
                    existingKey.getAlias().substring(0,existingKey.getAlias().lastIndexOf('.')),
                    nextKeyNumber);
            */
            String nextKeyId = existingKey.getAlias();
            subscriber.setAlias(nextKeyId);
            subscriber.setUniqueKeyId(subscriber.getAlias());

            CryptoKey signKey = getKey(subscriber.getAlias(),CryptoKey.PURPOSE_SIGNING) ;//Create new key
            signKey.setAlgorithm(Request.SIGNATURE_ALGO);
            signKey.setPrivateKey(Crypt.getInstance().getBase64Encoded(signPair.getPrivate()));
            signKey.setPublicKey(Crypt.getInstance().getBase64Encoded(signPair.getPublic()));
            signKey.save();

            CryptoKey encryptionKey = getKey(subscriber.getAlias(),CryptoKey.PURPOSE_ENCRYPTION);
            encryptionKey.setAlgorithm(Request.ENCRYPTION_ALGO);
            encryptionKey.setPrivateKey(Crypt.getInstance().getBase64Encoded(encPair.getPrivate()));
            encryptionKey.setPublicKey(Crypt.getInstance().getBase64Encoded(encPair.getPublic()));
            encryptionKey.save();
        }
    }
    public JSONObject getSubscriptionJson(Subscriber subscriber) {
        if (!ObjectUtil.isVoid(subscriber.getDomain()) && (getDomains() == null || getDomains().get(subscriber.getDomain()) == null)){
            throw new RuntimeException("Registry does not support domain " + subscriber.getDomain());
        }
        rotateKeys(subscriber);
        CryptoKey skey = getKey(subscriber.getAlias(),CryptoKey.PURPOSE_SIGNING);
        CryptoKey ekey = getKey(subscriber.getAlias(),CryptoKey.PURPOSE_ENCRYPTION);

        long validFrom = skey.getUpdatedAt().getTime();
        long validTo = (validFrom + getKeyValidityMillis());

        JSONObject object = new JSONObject();
        object.put("subscriber_id", subscriber.getSubscriberId());
        object.put("subscriber_url", subscriber.getSubscriberUrl());
        object.put("type",subscriber.getType());

        object.put("domain", subscriber.getDomain());
        object.put("signing_public_key", Request.getRawSigningKey(skey.getPublicKey()));
        object.put("encr_public_key", Request.getRawEncryptionKey(ekey.getPublicKey()));
        object.put("valid_from", BecknObject.TIMESTAMP_FORMAT.format(new Date(validFrom)));
        object.put("valid_until", BecknObject.TIMESTAMP_FORMAT.format(new Date(validTo)));
        object.put("country", subscriber.getCountry());
        if (!ObjectUtil.isVoid(subscriber.getCity())){
            object.put("city", subscriber.getCity());
        }
        object.put("created",BecknObject.TIMESTAMP_FORMAT.format(skey.getCreatedAt()));
        object.put("updated",BecknObject.TIMESTAMP_FORMAT.format(skey.getUpdatedAt()));


        object.put("unique_key_id", skey.getAlias());
        object.put("pub_key_id", skey.getAlias());
        object.put("nonce", Base64.getEncoder().encodeToString(String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8)));
        subscriber.setInner(object);

        return object;
    }

    public void register(Subscriber subscriber) {
        Request request = new Request(getSubscriptionJson(subscriber));

        Call<JSONObject> call = new Call<JSONObject>().url(getRegistryUrl(), "register").
                method(HttpMethod.POST).input(request.getInner()).inputFormat(InputFormat.JSON).
                header("Content-Type", MimeType.APPLICATION_JSON.toString()).
                header("Accept", MimeType.APPLICATION_JSON.toString());

        JSONObject response = call.getResponseAsJson();
        Config.instance().getLogger(getClass().getName()).info("register" + "-" + response.toString());
    }

    public void subscribe(Subscriber subscriber) {
        List<Subscriber> subscribers = lookup(subscriber.getSubscriberId(),false);

        if (subscriber.getDomains() == null){
            subscriber.setDomains(new Subscriber.Domains());
            subscriber.getDomains().add(subscriber.getDomain());
        }

        if (subscribers.size() < subscriber.getDomains().size()){
            if (isSelfRegistrationSupported()) {
                Subscriber.Domains domains = new Subscriber.Domains(subscriber.getDomains().getInner());
                try {
                    subscribers.forEach(s->{
                        subscriber.getDomains().remove(s.getDomain());
                    }); //Remove domains already registered!
                    register(subscriber);
                }finally {
                    subscriber.setDomains(domains);
                }
                if (isSubscriptionNeededPostRegistration()) {
                    _subscribe(subscriber);
                }
            }else {
                Config.instance().getLogger(getClass().getName()).log(Level.WARNING,"Contact Registrar to register to network !");
                return;
            }
        }else if (ObjectUtil.equals(Subscriber.SUBSCRIBER_STATUS_SUBSCRIBED,subscribers.get(0).getStatus())){
            Subscriber me = subscribers.get(0);
            long now = System.currentTimeMillis();
            if (me.getValidFrom().getTime() < now && me.getValidTo().getTime() > now){
                return;
            }
            _subscribe(subscriber);
        }
    }

    protected void _subscribe(Subscriber subscriber){
        Request request = new Request(getSubscriptionJson(subscriber));

        Call<JSONObject> call = new Call<JSONObject>().url(getRegistryUrl(), "subscribe").
                method(HttpMethod.POST).input(request.getInner()).inputFormat(InputFormat.JSON).
                header("Content-Type", MimeType.APPLICATION_JSON.toString()).
                header("Accept", MimeType.APPLICATION_JSON.toString());

        call.header("Authorization", request.generateAuthorizationHeader(subscriber.getSubscriberId(),
                Objects.requireNonNull(getKey(subscriber.getAlias(),CryptoKey.PURPOSE_SIGNING)).getAlias()));

        JSONObject response = call.getResponseAsJson();
        Config.instance().getLogger(getClass().getName()).info("subscribe" + "-" + response.toString());
    }

    private transient TimeSensitiveCache subscriberLookup = new TimeSensitiveCache(Duration.ofHours(1));

    public String getKey(Subscriber subscriber){
        TreeMap<String,String> map = new TreeMap<>();
        JSONObject inner = subscriber.getInner();
        for (Object s : inner.keySet()){
            if (s instanceof String && inner.get(s) instanceof String){
                map.put((String)s,(String)inner.get(s));
            }
        }
        return map.toString();
    }
    public List<Subscriber> lookup(Subscriber subscriber,boolean onlyIfSubscribed) {
        return subscriberLookup.get(getKey(subscriber),()->{
            List<Subscriber> subscribers = new ArrayList<>();
            Subscriber tmp = new Subscriber();
            tmp.update(subscriber);
            if (subscriber.getUniqueKeyId() != null) {
                tmp.setUniqueKeyId(null);
                tmp.set(getUniqueKeyId(), subscriber.getUniqueKeyId());
            }

            if (ObjectUtil.isVoid(tmp.getCountry())){
                tmp.setCountry("IND");
            }

            JSONArray responses = new Call<JSONObject>().method(HttpMethod.POST).url(getRegistryUrl(), "lookup").input(tmp.getInner(false)).inputFormat(InputFormat.JSON)
                    .header("content-type", MimeType.APPLICATION_JSON.toString())
                    .header("accept", MimeType.APPLICATION_JSON.toString()).getResponseAsJson();
            if (responses == null) {
                return subscribers;
            }

            Date now = new Date();
            for (Iterator<?> i = responses.iterator(); i.hasNext(); ) {
                JSONObject object1 = (JSONObject) i.next();
                Subscriber subscriber1 = new Subscriber(object1);
                if (onlyIfSubscribed && !ObjectUtil.equals(subscriber1.getStatus(), "SUBSCRIBED")) {
                    i.remove();
                } else if (subscriber1.getValidTo() != null && DateUtils.addHours(subscriber1.getValidTo(),getKeyExpiryGracePeriod()).before(now)){
                    i.remove();
                }else if (subscriber1.getValidFrom() != null && DateUtils.addHours(subscriber1.getValidFrom(),-1*getKeyExpiryGracePeriod()).after(now)){
                    i.remove();
                }else{
                    subscribers.add(subscriber1);
                }
            }
            return subscribers;
        });
    }


    public static class Domains extends BecknObjectsWithId<Domain>{

        public Domains() {
        }

        public Domains(JSONArray array) {
            super(array);
        }
    }

    public static class Domain  extends BecknObjectWithId {

        public Domain() {
        }

        public Domain(JSONObject object) {
            super(object);
        }

        public String getName(){
            return get("name");
        }
        public void setName(String name){
            set("name",name);
        }

        public String getVersion(){
            return get("version");
        }
        public void setVersion(String version){
            set("version",version);
        }

        public String getSchema(){
            return get("schema");
        }
        public URL getSchemaURL(){
            return getSchemaURL(null);
        }
        public URL getSchemaURL(String s){
            if (ObjectUtil.isVoid(s)) {
                return _getSchemaURL(getSchema());
            }else {
                return _getSchemaURL(s);
            }
        }
        private URL _getSchemaURL(String s){
            if (ObjectUtil.isVoid(s)){
                return null;
            }
            URL url = null ;

            try {
                if (s.startsWith("/")){
                    url = Config.class.getResource(s);
                    if (url == null){
                        return null;
                    }
                }else {
                    url = new URL(s);
                }
                try {
                    URLConnection connection = url.openConnection();
                    connection.connect();
                }catch (Exception ex){
                    url = null ;
                }
                return url;
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        public void setSchema(String schema){
            set("schema",schema);
        }

        public String getExtensionPackage(){
            return get("extension_package");
        }
        public void setExtensionPackage(String extension_package){
            set("extension_package",extension_package);
        }
    }

    public abstract NetworkApiAdaptor getApiAdaptor();

    public String getExtensionPackage(){
        return get("extension_package");
    }
    public void setExtensionPackage(String extension_package){
        set("extension_package",extension_package);
    }


    Set<Class<?>> classesWithNoExtension = new HashSet<>();
    @SuppressWarnings("unchecked")
    private <B> B create(Class<B> clazz , String domainId){
        if (!BecknObject.class.isAssignableFrom(clazz)){
            create(clazz);
        }
        if (classesWithNoExtension.contains(clazz)){
            create(clazz);
        }

        Set<String> packagesToCheck = new SequenceSet<>();
        packagesToCheck.add(getDomains().get(domainId).getExtensionPackage());
        packagesToCheck.add(getExtensionPackage());

        Class<?> extendedClass = clazz;
        for (String extensionPackage : packagesToCheck) {
            if (ObjectUtil.isVoid(extensionPackage)) {
                continue;
            }
            String clazzName = String.format("%s.%s", extensionPackage, clazz.getSimpleName());
            try {
                extendedClass = Class.forName(clazzName);
                break;
            } catch (ClassNotFoundException e) {
                // try next
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (extendedClass == clazz) {
            classesWithNoExtension.add(clazz);
        }
        return create(extendedClass);
    }

    @SuppressWarnings("unchecked")
    private <B> B create(Class<?> bClass){
        try {
            return (B)(bClass.getConstructor().newInstance());
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    private transient Cache<String, JSONAwareWrapperCreator> becknObjectCreatorCache = new Cache<>(0,0) {
        @Override
        protected JSONAwareWrapperCreator getValue(String domainId) {
            return new JSONAwareWrapperCreator(){
                @Override
                public <B> B create(Class<B> clazz) {
                    B b = NetworkAdaptor.this.create(clazz,domainId);
                    if (b instanceof JSONAwareWrapper){
                        ((JSONAwareWrapper)b).setObjectCreator(this);
                    }
                    return b;
                }
            };
        }
    };

    public JSONAwareWrapperCreator getObjectCreator(String domain){
        return becknObjectCreatorCache.get(domain);
    }

}