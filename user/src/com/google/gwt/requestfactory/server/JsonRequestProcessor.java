/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.requestfactory.server;

import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.ProxyFor;
import com.google.gwt.requestfactory.shared.ServerFailure;
import com.google.gwt.requestfactory.shared.WriteOperation;
import com.google.gwt.requestfactory.shared.impl.CollectionProperty;
import com.google.gwt.requestfactory.shared.impl.Property;
import com.google.gwt.user.server.Base64Utils;

import static com.google.gwt.requestfactory.shared.impl.RequestData.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

/**
 * An implementation of RequestProcessor for JSON encoded payloads.
 */
public class JsonRequestProcessor implements RequestProcessor<String> {

  // TODO should we consume String, InputStream, or JSONObject?
  private static class DvsData {
    private final JSONObject jsonObject;
    private final WriteOperation writeOperation;

    DvsData(JSONObject jsonObject, WriteOperation writeOperation) {
      this.jsonObject = jsonObject;
      this.writeOperation = writeOperation;
    }
  }

  private static class EntityData {
    private final Object entityInstance;
    // TODO: violations should have more structure than JSONObject
    private final JSONObject violations;

    EntityData(Object entityInstance, JSONObject violations) {
      this.entityInstance = entityInstance;
      this.violations = violations;
    }
  }

  private class EntityKey {
    final boolean isFuture;
    final String encodedId;
    final Class<? extends EntityProxy> proxyType;

    EntityKey(String id, boolean isFuture,
        Class<? extends EntityProxy> proxyType) {
      this.encodedId = id;
      this.isFuture = isFuture;
      assert proxyType != null;
      this.proxyType = proxyType;
    }

    public Object decodedId(Class<?> entityIdType) throws SecurityException,
        JSONException, IllegalAccessException, InvocationTargetException,
        NoSuchMethodException, InstantiationException {
      if (isFuture) {
        return encodedId;
      }

      if (String.class.isAssignableFrom(entityIdType)) {
        return base64Decode(encodedId);
      }

      return decodeParameterValue(entityIdType, encodedId);
    }

    @Override
    public boolean equals(Object ob) {
      if (!(ob instanceof EntityKey)) {
        return false;
      }
      EntityKey other = (EntityKey) ob;
      return (encodedId.equals(other.encodedId))
          && (isFuture == other.isFuture)
          && (proxyType.equals(other.proxyType));
    }

    @Override
    public int hashCode() {
      return 31 * this.proxyType.hashCode()
          + (31 * this.encodedId.hashCode() + (isFuture ? 1 : 0));
    }

    @Override
    public String toString() {
      return encodedId + "@" + (isFuture ? "IS" : "NO") + "@"
          + proxyType.getName();
    }
  }

  private static class SerializedEntity {
    // the field value of the entityInstance might change from under us.
    private final Object entityInstance;

    private final JSONObject serializedEntity;

    SerializedEntity(Object entityInstance, JSONObject serializedEntity) {
      this.entityInstance = entityInstance;
      this.serializedEntity = serializedEntity;
    }
  }

  public static final String RELATED = "related";

  private static final Logger log = Logger.getLogger(JsonRequestProcessor.class.getName());

  // Decodes a string encoded as web-safe base64
  public static String base64Decode(String encoded) {
    byte[] decodedBytes;
    try {
      decodedBytes = Base64Utils.fromBase64(encoded);
    } catch (Exception e) {
      throw new IllegalArgumentException("EntityKeyId was not Base64 encoded: "
          + encoded);
    }
    return new String(decodedBytes);
  }

  // Encodes a string with web-safe base64
  public static String base64Encode(String decoded) {
    byte[] decodedBytes = decoded.getBytes();
    return Base64Utils.toBase64(decodedBytes);
  }

  @SuppressWarnings("unchecked")
  public static Class<EntityProxy> getRecordFromClassToken(String recordToken) {
    try {
      // TODO(rjrjr) Should be getting class loader from servlet environment?
      Class<?> clazz = Class.forName(recordToken, false,
          JsonRequestProcessor.class.getClassLoader());
      if (EntityProxy.class.isAssignableFrom(clazz)) {
        return (Class<EntityProxy>) clazz;
      }
      throw new SecurityException("Attempt to access non-record class "
          + recordToken);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Non-existent record class "
          + recordToken);
    }
  }

  private RequestProperty propertyRefs;

  private final Map<String, JSONObject> relatedObjects = new HashMap<String, JSONObject>();

  private OperationRegistry operationRegistry;

  private ExceptionHandler exceptionHandler;
  /*
   * <li>Request comes in. Construct the involvedKeys, dvsDataMap and
   * beforeDataMap, using DVS and parameters.
   *
   * <li>Apply the DVS and construct the afterDvsDataMqp.
   *
   * <li>Invoke the method noted in the operation.
   *
   * <li>Find the changes that need to be sent back.
   */
  private final Map<EntityKey, Object> cachedEntityLookup = new HashMap<EntityKey, Object>();
  private final Set<EntityKey> involvedKeys = new HashSet<EntityKey>();
  private final Map<EntityKey, DvsData> dvsDataMap = new HashMap<EntityKey, DvsData>();
  private final Map<EntityKey, SerializedEntity> beforeDataMap = new HashMap<EntityKey, SerializedEntity>();

  private Map<EntityKey, EntityData> afterDvsDataMap = new HashMap<EntityKey, EntityData>();

  @SuppressWarnings(value = {"unchecked", "rawtypes"})
  public Collection<Property<?>> allProperties(
      Class<? extends EntityProxy> clazz) throws IllegalArgumentException {
    return getPropertiesFromRecordProxyType(clazz).values();
  }

  public String decodeAndInvokeRequest(String encodedRequest)
      throws RequestProcessingException {
    try {
      Logger.getLogger(this.getClass().getName()).finest(
          "Incoming request " + encodedRequest);
      String response = processJsonRequest(encodedRequest).toString();
      Logger.getLogger(this.getClass().getName()).finest(
          "Outgoing response " + response);
      return response;
    } catch (InvocationTargetException e) {
      JSONObject exceptionResponse = buildExceptionResponse(e.getCause());
      throw new RequestProcessingException("Unexpected exception", e,
          exceptionResponse.toString());
    } catch (Exception e) {
      JSONObject exceptionResponse = buildExceptionResponse(e);
      throw new RequestProcessingException("Unexpected exception", e,
          exceptionResponse.toString());
    }
  }

  /**
   * Decodes parameter value.
   */
  public Object decodeParameterValue(Type genericParameterType,
      String parameterValue) throws SecurityException, JSONException,
      IllegalAccessException, InvocationTargetException, NoSuchMethodException,
      InstantiationException {
    if (parameterValue == null) {
      return null;
    }
    Class<?> parameterType = null;
    if (genericParameterType instanceof Class<?>) {
      parameterType = (Class<?>) genericParameterType;
    }
    if (genericParameterType instanceof ParameterizedType) {
      ParameterizedType pType = (ParameterizedType) genericParameterType;
      if (pType.getRawType() instanceof Class<?>) {
        Class<?> rType = (Class<?>) pType.getRawType();
        // Ensure parameterType is initialized
        parameterType = rType;
        if (Collection.class.isAssignableFrom(rType)) {
          Collection<Object> collection = createCollection(rType);
          if (collection != null) {
            JSONArray array = new JSONArray(parameterValue);
            for (int i = 0; i < array.length(); i++) {
              String value = array.isNull(i) ? null : array.getString(i);
              collection.add(decodeParameterValue(
                  pType.getActualTypeArguments()[0], value));
            }
            return collection;
          }
        }
      }
    }
    if (String.class == parameterType) {
      return parameterValue;
    }
    if (Boolean.class == parameterType || boolean.class == parameterType) {
      return Boolean.valueOf(parameterValue);
    }
    if (Integer.class == parameterType || int.class == parameterType) {
      return new Integer(parameterValue);
    }
    if (Byte.class == parameterType || byte.class == parameterType) {
      return new Byte(parameterValue);
    }
    if (Short.class == parameterType || short.class == parameterType) {
      return new Short(parameterValue);
    }
    if (Float.class == parameterType || float.class == parameterType) {
      return new Float(parameterValue);
    }
    if (Double.class == parameterType || double.class == parameterType) {
      return new Double(parameterValue);
    }
    if (Long.class == parameterType || long.class == parameterType) {
      return new Long(parameterValue);
    }
    if (Character.class == parameterType || char.class == parameterType) {
      return parameterValue.charAt(0);
    }
    if (BigInteger.class == parameterType) {
      return new BigInteger(parameterValue);
    }
    if (BigDecimal.class == parameterType) {
      return new BigDecimal(parameterValue);
    }
    if (parameterType.isEnum()) {
      int ordinal = Integer.parseInt(parameterValue);
      Method valuesMethod = parameterType.getDeclaredMethod("values",
          new Class[0]);

      if (valuesMethod != null) {
        valuesMethod.setAccessible(true);
        Enum<?>[] values = (Enum<?>[]) valuesMethod.invoke(null);
        // we use ordinal serialization instead of name since future compiler
        // opts may remove names
        for (Enum<?> e : values) {
          if (ordinal == e.ordinal()) {
            return e;
          }
        }
      }
      throw new IllegalArgumentException("Can't decode enum " + parameterType
          + " no matching ordinal " + ordinal);
    }
    if (Date.class == parameterType) {
      return new Date(Long.parseLong(parameterValue));
    }
    if (EntityProxy.class.isAssignableFrom(parameterType)) {
      /*
       * TODO: 1. Don't resolve in this step, just get EntityKey. May need to
       * use DVS.
       *
       * 2. Merge the following and the object resolution code in getEntityKey.
       * 3. Update the involvedKeys set.
       */
      ProxyFor service = parameterType.getAnnotation(ProxyFor.class);
      if (service != null) {
        EntityKey entityKey = getEntityKey(parameterValue.toString());

        DvsData dvsData = dvsDataMap.get(entityKey);
        if (dvsData != null) {
          EntityData entityData = getEntityDataForRecordWithSettersApplied(
              entityKey, dvsData.jsonObject, dvsData.writeOperation);
          return entityData.entityInstance;
        } else {
          // TODO(rjrjr): This results in a ConcurrentModificationException.
          // involvedKeys loops in constructAfterDvsDataMapAfterCallingSetters.
          involvedKeys.add(entityKey);
          return getEntityInstance(entityKey);
        }
      }
    }
    if (EntityProxyId.class.isAssignableFrom(parameterType)) {
      EntityKey entityKey = getEntityKey(parameterValue.toString());
      ProxyFor service = entityKey.proxyType.getAnnotation(ProxyFor.class);
      if (service == null) {
        throw new IllegalArgumentException("Unknown service, unable to decode "
            + parameterValue);
      }
      // TODO(rjrjr): This results in a ConcurrentModificationException.
      // involvedKeys loops in constructAfterDvsDataMapAfterCallingSetters.
      involvedKeys.add(entityKey);
      return getEntityInstance(entityKey);
    }
    throw new IllegalArgumentException("Unknown parameter type: "
        + parameterType);
  }

  /*
   * Encode a property value to be sent across the wire.
   */
  public Object encodePropertyValue(Object value) {
    if (value == null) {
      return JSONObject.NULL;
    }
    Class<?> type = value.getClass();
    if (Boolean.class == type) {
      return value;
    }
    if (Date.class.isAssignableFrom(type)) {
      return String.valueOf(((Date) value).getTime());
    }
    if (Enum.class.isAssignableFrom(type)) {
      return Double.valueOf(((Enum<?>) value).ordinal());
    }
    if (BigDecimal.class == type || BigInteger.class == type
        || Long.class == type) {
      return String.valueOf(value);
    }
    if (Number.class.isAssignableFrom(type)) {
      return ((Number) value).doubleValue();
    }
    return String.valueOf(value);
  }

  /**
   * Returns the propertyValue in the right type, from the DataStore. The value
   * is sent into the response.
   */
  public Object encodePropertyValueFromDataStore(Object entityElement,
      Property<?> property, String propertyName, RequestProperty propertyContext)
      throws SecurityException, NoSuchMethodException, IllegalAccessException,
      InvocationTargetException, JSONException {

    Object returnValue = getRawPropertyValueFromDatastore(entityElement,
        propertyName);
    Class<?> proxyPropertyType = property.getType();
    Class<?> elementType = property instanceof CollectionProperty<?,?>
        ? ((CollectionProperty<?,?>) property).getLeafType() : proxyPropertyType;
    String encodedEntityId = isEntityReference(returnValue, proxyPropertyType);

    if (returnValue == null) {
      return JSONObject.NULL;
    } else if (encodedEntityId != null) {
      String keyRef = encodeRelated(proxyPropertyType, propertyName,
          propertyContext, returnValue);
      return keyRef;
    } else if (property instanceof CollectionProperty<?,?>) {
      Class<?> colType = ((CollectionProperty<?,?>) property).getType();
      Collection<Object> col = createCollection(colType);
      if (col != null) {
        for (Object o : ((Collection<?>) returnValue)) {
          String encodedValId = isEntityReference(o,
              ((CollectionProperty<?,?>) property).getLeafType());
          if (encodedValId != null) {
            col.add(encodeRelated(elementType, propertyName, propertyContext, o));
          } else {
            col.add(encodePropertyValue(o));
          }
        }
        return col;
      }
      return null;
    } else {
      return encodePropertyValue(returnValue);
    }
  }

  /**
   * Generate an ID for a new record. The default behavior is to return null and
   * let the data store generate the ID automatically.
   *
   * @param key the key of the record field
   * @return the ID of the new record, or null to auto generate
   */
  public String generateIdForCreate(@SuppressWarnings("unused") String key) {
    // TODO(rjrjr) is there any point to this method if a service layer
    // is coming?
    return null;
  }

  /**
   * Find the entity in the server data store, apply its setters, capture any
   * violations, and return an {@link EntityData} encapsulating the results.
   * <p>
   * If a <i>set</i> method has side-effects, we will not notice.
   */
  public EntityData getEntityDataForRecordWithSettersApplied(
      EntityKey entityKey, JSONObject recordObject,
      WriteOperation writeOperation) throws JSONException, SecurityException,
      IllegalAccessException, InvocationTargetException, NoSuchMethodException,
      InstantiationException {

    Class<?> entityType = getEntityTypeForProxyType(entityKey.proxyType);

    Map<String, Property<?>> propertiesInProxy = getPropertiesFromRecordProxyType(entityKey.proxyType);
    Map<String, Class<?>> propertiesInDomain = updatePropertyTypes(
        propertiesInProxy, entityType);
    validateKeys(recordObject, propertiesInDomain.keySet());

    // get entityInstance
    Object entityInstance = getEntityInstance(
        writeOperation,
        entityType,
        entityKey.decodedId(propertiesInProxy.get(ENTITY_ID_PROPERTY).getType()),
        propertiesInProxy.get(ENTITY_ID_PROPERTY).getType());

    cachedEntityLookup.put(entityKey, entityInstance);

    Iterator<?> keys = recordObject.keys();
    while (keys.hasNext()) {
      String key = (String) keys.next();
      Class<?> propertyType = propertiesInDomain.get(key);
      Property<?> dtoProperty = propertiesInProxy.get(key);
      if (writeOperation == WriteOperation.PERSIST
          && (ENTITY_ID_PROPERTY.equals(key))) {
        String id = generateIdForCreate(key);
        if (id != null) {
          // TODO(rjrjr) generateIdForCreate returns null. Has this ever
          // execute
          entityType.getMethod(getMethodNameFromPropertyName(key, "set"),
              propertyType).invoke(entityInstance, id);
        }
      } else {
        Object propertyValue = null;
        if (recordObject.isNull(key)) {
          // null
        } else if (dtoProperty instanceof CollectionProperty<?,?>) {
          Class<?> cType = dtoProperty.getType();
          Class<?> leafType = ((CollectionProperty<?,?>) dtoProperty).getLeafType();
          Collection<Object> col = createCollection(cType);
          if (col != null) {
            JSONArray array = recordObject.getJSONArray(key);
            for (int i = 0; i < array.length(); i++) {
              if (EntityProxy.class.isAssignableFrom(leafType)) {
                propertyValue = getPropertyValueFromRequestCached(array,
                    i, dtoProperty);
              } else {
                propertyValue = decodeParameterValue(leafType,
                    array.getString(i));
              }
              col.add(propertyValue);
            }
            propertyValue = col;
          }
        } else {
          propertyValue = getPropertyValueFromRequestCached(recordObject,
              propertiesInProxy, key, dtoProperty);
        }
        entityType.getMethod(getMethodNameFromPropertyName(key, "set"),
            propertiesInDomain.get(key)).invoke(entityInstance, propertyValue);
      }
    }

    Set<ConstraintViolation<Object>> violations = Collections.emptySet();
    // validations check..
    Validator validator = null;
    try {
      ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
      validator = validatorFactory.getValidator();
    } catch (Exception e) {
      /*
       * This is JBoss's clumsy way of telling us that the system has not been
       * configured.
       */
      log.info(String.format(
          "Ignoring exception caught initializing bean validation framework. "
              + "It is probably unconfigured or misconfigured. [%s] %s ",
          e.getClass().getName(), e.getLocalizedMessage()));
    }

    if (validator != null) {
      violations = validator.validate(entityInstance);
    }
    return new EntityData(entityInstance, (violations.isEmpty() ? null
        : getViolationsAsJson(violations)));
  }

  public Object getEntityInstance(EntityKey entityKey)
      throws NoSuchMethodException, IllegalAccessException,
      InvocationTargetException, JSONException, InstantiationException {
    Class<?> entityClass = getEntityTypeForProxyType(entityKey.proxyType);
    Class<?> idType = getIdMethodForEntity(entityClass).getReturnType();
    Object entityInstance = entityClass.getMethod(
        "find" + entityClass.getSimpleName(), idType).invoke(null,
        entityKey.decodedId(idType));
    return entityInstance;
  }

  public Object getEntityInstance(WriteOperation writeOperation,
      Class<?> entityType, Object idValue, Class<?> idType)
      throws SecurityException, InstantiationException, IllegalAccessException,
      InvocationTargetException, NoSuchMethodException,
      IllegalArgumentException, JSONException {

    if (writeOperation == WriteOperation.PERSIST) {
      return entityType.getConstructor().newInstance();
    }
    // TODO: check "version" validity.
    return entityType.getMethod("find" + entityType.getSimpleName(), idType).invoke(
        null, decodeParameterValue(idType, idValue.toString()));
  }

  @SuppressWarnings("unchecked")
  public Class<Object> getEntityTypeForProxyType(
      Class<? extends EntityProxy> record) {
    ProxyFor dtoAnn = record.getAnnotation(ProxyFor.class);
    if (dtoAnn != null) {
      return (Class<Object>) dtoAnn.value();
    }
    throw new IllegalArgumentException("Proxy class " + record.getName()
        + " missing ProxyFor annotation");
  }

  /**
   * Converts the returnValue of a 'get' method to a JSONArray.
   *
   * @param resultList object returned by a 'get' method, must be of type
   *          List<?>
   * @param entityKeyClass the class type of the contained value
   * @return the JSONArray
   */
  public JSONArray getJsonArray(Collection<?> resultList,
      Class<? extends EntityProxy> entityKeyClass)
      throws IllegalArgumentException, SecurityException,
      IllegalAccessException, JSONException, NoSuchMethodException,
      InvocationTargetException {
    JSONArray jsonArray = new JSONArray();
    if (resultList.size() == 0) {
      return jsonArray;
    }

    for (Object entityElement : resultList) {
      if (entityElement instanceof Number || entityElement instanceof String
          || entityElement instanceof Character
          || entityElement instanceof Date || entityElement instanceof Boolean
          || entityElement instanceof Enum<?>) {
        jsonArray.put(encodePropertyValue(entityElement));
      } else if (entityElement instanceof List<?> || entityElement instanceof Set<?>) {
        // TODO: unwrap nested type params?
        jsonArray.put(getJsonArray((Collection<?>) entityElement,
            entityKeyClass));
      } else {
        jsonArray.put(getJsonObject(entityElement, entityKeyClass, propertyRefs));
      }
    }
    return jsonArray;
  }

  public Object getJsonObject(Object entityElement,
      Class<? extends EntityProxy> entityKeyClass,
      RequestProperty propertyContext) throws JSONException,
      NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    JSONObject jsonObject = new JSONObject();
    if (entityElement == null
        || !EntityProxy.class.isAssignableFrom(entityKeyClass)) {
      // JSONObject.NULL isn't a JSONObject
      return JSONObject.NULL;
    }

    jsonObject.put(ENCODED_ID_PROPERTY, isEntityReference(entityElement,
        entityKeyClass));
    jsonObject.put(ENCODED_VERSION_PROPERTY, encodePropertyValueFromDataStore(
        entityElement, ENTITY_VERSION_PROPERTY,
        ENTITY_VERSION_PROPERTY.getName(), propertyContext));
    for (Property<?> p : allProperties(entityKeyClass)) {
      if (requestedProperty(p, propertyContext)) {
        String propertyName = p.getName();
        jsonObject.put(propertyName, encodePropertyValueFromDataStore(
            entityElement, p, propertyName, propertyContext));
      }
    }
    return jsonObject;
  }

  /**
   * Returns methodName corresponding to the propertyName that can be invoked on
   * an entity.
   *
   * Example: "userName" returns prefix + "UserName". "version" returns prefix +
   * "Version"
   */
  public String getMethodNameFromPropertyName(String propertyName, String prefix) {
    if (propertyName == null) {
      throw new NullPointerException("propertyName must not be null");
    }

    StringBuffer methodName = new StringBuffer(prefix);
    methodName.append(propertyName.substring(0, 1).toUpperCase());
    methodName.append(propertyName.substring(1));
    return methodName.toString();
  }

  /**
   * Returns Object[0][0] as the entityKey corresponding to the object instance
   * or null if it is a static method. Returns Object[1] as the params array.
   */
  public Object[][] getObjectsFromParameterMap(boolean isInstanceMethod,
      Map<String, String> parameterMap, Type parameterClasses[])
      throws SecurityException, JSONException, IllegalAccessException,
      InvocationTargetException, NoSuchMethodException, InstantiationException {
    // TODO: create an EntityMethodCall (instance, args) instead.
    assert parameterClasses != null;
    Object args[][] = new Object[2][];
    args[0] = new Object[1];
    if (isInstanceMethod) {
      EntityKey entityKey = getEntityKey(parameterMap.get(PARAM_TOKEN + "0"));
      involvedKeys.add(entityKey);
      args[0][0] = entityKey;
    } else {
      args[0][0] = null;
    }

    // TODO: update the involvedKeys for other params
    int offset = (isInstanceMethod ? 1 : 0);
    args[1] = new Object[parameterClasses.length - offset];
    for (int i = 0; i < parameterClasses.length - offset; i++) {
      args[1][i] = decodeParameterValue(parameterClasses[i + offset],
          parameterMap.get(PARAM_TOKEN + (i + offset)));
    }
    return args;
  }

  public RequestDefinition getOperation(String operationName) {
    RequestDefinition operation;
    operation = operationRegistry.getOperation(operationName);
    if (null == operation) {
      throw new IllegalArgumentException("Unknown operation " + operationName);
    }
    return operation;
  }

  public Map<String, String> getParameterMap(JSONObject jsonObject)
      throws JSONException {
    Map<String, String> parameterMap = new HashMap<String, String>();
    Iterator<?> keys = jsonObject.keys();
    while (keys.hasNext()) {
      String key = keys.next().toString();
      if (key.startsWith(PARAM_TOKEN)) {
        String value = jsonObject.isNull(key) ? null : jsonObject.getString(key);
        parameterMap.put(key, value);
      }
    }
    return parameterMap;
  }

  /**
   * Returns the property fields (name => type) for a record.
   */
  @SuppressWarnings("unchecked")
  public Map<String, Property<?>> getPropertiesFromRecordProxyType(
      Class<? extends EntityProxy> record) throws SecurityException {
    if (!EntityProxy.class.isAssignableFrom(record)) {
      return Collections.emptyMap();
    }

    Map<String, Property<?>> properties = new LinkedHashMap<String, Property<?>>();
    Method[] methods = record.getMethods();
    for (Method method : methods) {
      String methodName = method.getName();
      String propertyName = null;
      Property newProperty = null;
      if (methodName.startsWith("get")) {
        propertyName = Introspector.decapitalize(methodName.substring(3));
        if (propertyName.length() == 0) {
          continue;
        }
        newProperty = getPropertyFromGenericType(propertyName,
            method.getGenericReturnType());
      } else if (methodName.startsWith("set")) {
        propertyName = Introspector.decapitalize(methodName.substring(3));
        if (propertyName.length() > 0) {
          Type[] parameterTypes = method.getGenericParameterTypes();
          if (parameterTypes.length > 0) {
            newProperty = getPropertyFromGenericType(propertyName,
                parameterTypes[parameterTypes.length - 1]);
          }
        }
      }
      if (newProperty == null) {
        continue;
      }
      Property existing = properties.put(propertyName, newProperty);
      if (existing != null && !existing.equals(newProperty)) {
        throw new IllegalStateException(String.format(
            "In %s, mismatched getter and setter types for property %s, "
                + "found %s and %s", record.getName(), propertyName,
            existing.getName(), newProperty.getName()));
      }
    }
    return properties;
  }

  @SuppressWarnings("unchecked")
  public Property getPropertyFromGenericType(String propertyName, Type type) {
    if (type instanceof ParameterizedType) {
      ParameterizedType pType = (ParameterizedType) type;
      Class<?> rawType = (Class<Object>) pType.getRawType();
      Type[] typeArgs = pType.getActualTypeArguments();
      if (Collection.class.isAssignableFrom(rawType)) {
        if (typeArgs.length == 1) {
          Type leafType = typeArgs[0];
          if (leafType instanceof Class) {
            return new CollectionProperty(propertyName, rawType,
                (Class) leafType);
          }
        }
      }
    } else {
      return new Property<Object>(propertyName, (Class<Object>) type);
    }
    return null;
  }

  /**
   * Returns the property value, in the specified type, from the request object.
   * The value is put in the DataStore.
   *
   * @throws InstantiationException
   * @throws NoSuchMethodException
   * @throws InvocationTargetException
   * @throws IllegalAccessException
   * @throws SecurityException
   */
  public Object getPropertyValueFromRequest(JSONArray recordArray, int index,
      Class<?> propertyType) throws JSONException, SecurityException,
      IllegalAccessException, InvocationTargetException, NoSuchMethodException,
      InstantiationException {
    return decodeParameterValue(propertyType, recordArray.get(index).toString());
  }

  public Object getPropertyValueFromRequest(JSONObject recordObject,
      String key, Class<?> propertyType) throws JSONException,
      SecurityException, IllegalAccessException, InvocationTargetException,
      NoSuchMethodException, InstantiationException {
    return decodeParameterValue(propertyType, recordObject.isNull(key) ? null
        : recordObject.get(key).toString());
  }

  public JSONObject getViolationsAsJson(
      Set<ConstraintViolation<Object>> violations) throws JSONException {
    JSONObject violationsAsJson = new JSONObject();
    for (ConstraintViolation<Object> violation : violations) {
      violationsAsJson.put(violation.getPropertyPath().toString(),
          violation.getMessage());
    }
    return violationsAsJson;
  }

  public Object invokeDomainMethod(Object domainObject, Method domainMethod,
      Object args[]) throws IllegalAccessException, InvocationTargetException {
    return domainMethod.invoke(domainObject, args);
  }

  @SuppressWarnings("unchecked")
  public JSONObject processJsonRequest(String jsonRequestString)
      throws JSONException, NoSuchMethodException, IllegalAccessException,
      InvocationTargetException, ClassNotFoundException, SecurityException,
      InstantiationException {
    RequestDefinition operation;
    JSONObject topLevelJsonObject = new JSONObject(jsonRequestString);

    String operationName = topLevelJsonObject.getString(OPERATION_TOKEN);
    String propertyRefsString = topLevelJsonObject.has(PROPERTY_REF_TOKEN)
        ? topLevelJsonObject.getString(PROPERTY_REF_TOKEN) : "";
    propertyRefs = RequestProperty.parse(propertyRefsString);

    operation = getOperation(operationName);
    Class<?> domainClass = Class.forName(operation.getDomainClassName());
    Method domainMethod = domainClass.getMethod(
        operation.getDomainMethod().getName(), operation.getParameterTypes());
    if (Modifier.isStatic(domainMethod.getModifiers()) == operation.isInstance()) {
      throw new IllegalArgumentException("the " + domainMethod.getName()
          + " should " + (operation.isInstance() ? "not " : "") + "be static");
    }

    if (topLevelJsonObject.has(CONTENT_TOKEN)) {
      // updates involvedKeys and dvsDataMap.
      decodeDVS(topLevelJsonObject.getString(CONTENT_TOKEN));
    }
    // get the domain object (for instance methods) and args.
    Object args[][] = getObjectsFromParameterMap(operation.isInstance(),
        getParameterMap(topLevelJsonObject),
        operation.getRequestParameterTypes());
    // Construct beforeDataMap
    constructBeforeDataMap();
    // Construct afterDvsDataMap.
    constructAfterDvsDataMapAfterCallingSetters();

    // violations are the only sideEffects at this point.
    JSONArray violationsAsJson = getViolations();
    if (violationsAsJson.length() > 0) {
      JSONObject envelop = new JSONObject();
      envelop.put(VIOLATIONS_TOKEN, violationsAsJson);
      return envelop;
    }

    // resolve parameters that are so far just EntityKeys.
    // TODO: resolve parameters other than the domainInstance
    EntityKey domainEntityKey = null;
    if (args[0][0] != null) {
      // Instance method, replace the key with the actual receiver
      domainEntityKey = (EntityKey) args[0][0];
      EntityData domainEntityData = afterDvsDataMap.get(domainEntityKey);
      if (domainEntityData != null) {
        args[0][0] = domainEntityData.entityInstance;
        assert args[0][0] != null;
      }
    }
    Object result = invokeDomainMethod(args[0][0], domainMethod, args[1]);

    JSONObject sideEffects = getSideEffects();

    if (result != null
        && (result instanceof List<?>) != List.class.isAssignableFrom(operation.getDomainMethod().getReturnType())) {
      throw new IllegalArgumentException(String.format(
          "Type mismatch, expected %s%s, but %s returns %s",
          List.class.isAssignableFrom(operation.getReturnType()) ? "list of "
              : "", operation.getReturnType(), domainMethod,
          domainMethod.getReturnType()));
    }

    JSONObject envelop = new JSONObject();
    if (result instanceof List<?> || result instanceof Set<?>) {
      envelop.put(RESULT_TOKEN, toJsonArray(operation, result));
    } else if (result instanceof Number || result instanceof Enum<?>
        || result instanceof String || result instanceof Date
        || result instanceof Character || result instanceof Boolean) {
      envelop.put(RESULT_TOKEN, encodePropertyValue(result));
    } else {
      Class<? extends EntityProxy> returnType = null;
      if (operation.getDomainClassName().equals(FindService.class.getName())) {
        // HACK.
        if (involvedKeys.size() == 1) {
          returnType = involvedKeys.iterator().next().proxyType;
        } else {
          System.out.println("null find");
        }
      } else {
        returnType = (Class<? extends EntityProxy>) operation.getReturnType();
      }
      Object jsonObject = getJsonObject(result, returnType, propertyRefs);
      envelop.put(RESULT_TOKEN, jsonObject);
    }
    envelop.put(SIDE_EFFECTS_TOKEN, sideEffects);
    envelop.put(RELATED_TOKEN, encodeRelatedObjectsToJson());
    return envelop;
  }

  public void setExceptionHandler(ExceptionHandler exceptionHandler) {
    this.exceptionHandler = exceptionHandler;
  }

  public void setOperationRegistry(OperationRegistry operationRegistry) {
    this.operationRegistry = operationRegistry;
  }

  public void validateKeys(JSONObject recordObject,
      Set<String> declaredProperties) {
    /*
     * We don't need it by the time we're here (it's in the EntityKey), and it
     * gums up the works.
     */
    recordObject.remove(ENCODED_ID_PROPERTY);
    recordObject.remove(ENCODED_VERSION_PROPERTY);

    Iterator<?> keys = recordObject.keys();
    while (keys.hasNext()) {
      String key = (String) keys.next();
      if (!declaredProperties.contains(key)) {
        throw new IllegalArgumentException("key " + key
            + " is not permitted to be set");
      }
    }
  }

  /**
   * Returns true iff the after and before JSONArray are different.
   */
  boolean hasChanged(JSONArray beforeArray, JSONArray afterArray)
      throws JSONException {
    if (beforeArray.length() != afterArray.length()) {
      return true;
    } else {
      for (int i = 0; i < beforeArray.length(); i++) {
        Object bVal = beforeArray.get(i);
        Object aVal = afterArray.get(i);
        if (aVal != null && bVal != null) {
          if (aVal == bVal || aVal.equals(bVal)) {
            continue;
          }
          if (aVal.getClass() != bVal.getClass()) {
            return true;
          }
          if (aVal instanceof JSONObject) {
            if (hasChanged((JSONObject) bVal, (JSONObject) aVal)) {
              return true;
            }
          }
          if (aVal instanceof JSONArray) {
            if (hasChanged((JSONArray) bVal, (JSONArray) aVal)) {
              return true;
            }
          }
        }
        if (aVal != bVal) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns true iff the after and before JSONObjects are different.
   */
  boolean hasChanged(JSONObject before, JSONObject after) throws JSONException {
    if (before == null) {
      return after != null;
    }
    // before != null
    if (after == null) {
      return true;
    }
    // before != null && after != null
    Iterator<?> keyIterator = before.keys();
    while (keyIterator.hasNext()) {
      String key = keyIterator.next().toString();
      Object beforeValue = before.isNull(key) ? null : before.get(key);
      Object afterValue = after.isNull(key) ? null : after.get(key);
      if (beforeValue == null) {
        if (afterValue == null) {
          continue;
        }
        return true;
      }
      if (afterValue == null) {
        return true;
      }
      // equals method on JSONArray doesn't consider contents
      if (!beforeValue.equals(afterValue)) {
        if (beforeValue instanceof JSONArray && afterValue instanceof JSONArray) {
          JSONArray beforeArray = (JSONArray) beforeValue;
          JSONArray afterArray = (JSONArray) afterValue;
          if (hasChanged(beforeArray, afterArray)) {
            return true;
          }
        } else {
          return true;
        }
      }
    }
    return false;
  }

  private void addRelatedObject(String keyRef, Object returnValue,
      Class<? extends EntityProxy> propertyType, RequestProperty propertyContext)
      throws JSONException, IllegalAccessException, NoSuchMethodException,
      InvocationTargetException {
    if (!relatedObjects.containsKey(keyRef)) {
      // put temporary value to prevent infinite recursion
      relatedObjects.put(keyRef, null);
      Object jsonObject = getJsonObject(returnValue, propertyType,
          propertyContext);
      if (jsonObject != JSONObject.NULL) {
        // put real value
        relatedObjects.put(keyRef, (JSONObject) jsonObject);
      }
    }
  }

  private JSONObject buildExceptionResponse(Throwable throwable) {
    JSONObject exceptionResponse = new JSONObject();
    ServerFailure failure = exceptionHandler.createServerFailure(throwable);
    try {
      JSONObject exceptionMessage = new JSONObject();

      String message = failure.getMessage();
      String exceptionType = failure.getExceptionType();
      String stackTraceString = failure.getStackTraceString();

      if (message != null && message.length() != 0) {
        exceptionMessage.put("message", message);
      }
      if (exceptionType != null && exceptionType.length() != 0) {
        exceptionMessage.put("type", exceptionType);
      }
      if (stackTraceString != null && stackTraceString.length() != 0) {
        exceptionMessage.put("trace", stackTraceString);
      }
      exceptionResponse.put("exception", exceptionMessage);
    } catch (JSONException jsonException) {
      throw new IllegalStateException(jsonException);
    }
    return exceptionResponse;
  }

  @SuppressWarnings("unchecked")
  private Class<? extends EntityProxy> castToRecordClass(Class<?> propertyType) {
    return (Class<? extends EntityProxy>) propertyType;
  }

  private void constructAfterDvsDataMapAfterCallingSetters()
      throws SecurityException, JSONException, IllegalAccessException,
      InvocationTargetException, NoSuchMethodException, InstantiationException {
    afterDvsDataMap = new HashMap<EntityKey, EntityData>();
    for (EntityKey entityKey : involvedKeys) {
      // use the beforeDataMap and dvsDataMap
      DvsData dvsData = dvsDataMap.get(entityKey);
      if (dvsData != null) {
        EntityData entityData = getEntityDataForRecordWithSettersApplied(
            entityKey, dvsData.jsonObject, dvsData.writeOperation);
        if (entityKey.isFuture) {
          // TODO: assert that the id is null for entityData.entityInstance
        }
        afterDvsDataMap.put(entityKey, entityData);
      } else if (entityKey.isFuture) {
        // The client-side DVS failed to include a CREATE operation.
        throw new RuntimeException("Future object with no dvsData");
      } else {
        /*
         * Involved, but not in the deltaValueStore -- param ref to an unedited,
         * existing object.
         */
        SerializedEntity serializedEntity = beforeDataMap.get(entityKey);
        if (serializedEntity.entityInstance != null) {
          afterDvsDataMap.put(entityKey, new EntityData(
              serializedEntity.entityInstance, null));
        }
      }
    }
  }

  /**
   * Constructs the beforeDataMap.
   *
   * <p>
   * Algorithm: go through the involvedKeys, and find the entityData
   * corresponding to each.
   */
  private void constructBeforeDataMap() throws IllegalArgumentException,
      SecurityException, IllegalAccessException, InvocationTargetException,
      NoSuchMethodException, JSONException, InstantiationException {
    for (EntityKey entityKey : involvedKeys) {
      if (entityKey.isFuture) {
        // the "before" is empty.
        continue;
      }
      beforeDataMap.put(entityKey, getSerializedEntity(entityKey));
    }
  }

  private Collection<Object> createCollection(Class<?> colType) {
    return colType == List.class ? new ArrayList<Object>()
        : colType == Set.class ? new HashSet<Object>() : null;
  }

  /**
   * Decode deltaValueStore to populate involvedKeys and dvsDataMap.
   */
  private void decodeDVS(String content) throws SecurityException,
      JSONException {
    JSONObject jsonObject = new JSONObject(content);
    for (WriteOperation writeOperation : WriteOperation.values()) {
      if (!jsonObject.has(writeOperation.name())) {
        continue;
      }
      JSONArray reportArray = new JSONArray(
          jsonObject.getString(writeOperation.name()));
      int length = reportArray.length();
      if (length == 0) {
        throw new IllegalArgumentException("No json array for "
            + writeOperation.name() + " should have been sent");
      }
      for (int i = 0; i < length; i++) {
        JSONObject recordWithSchema = reportArray.getJSONObject(i);
        Iterator<?> iterator = recordWithSchema.keys();
        String recordToken = (String) iterator.next();
        if (iterator.hasNext()) {
          throw new IllegalArgumentException(
              "There cannot be more than one record token");
        }
        JSONObject recordObject = recordWithSchema.getJSONObject(recordToken);
        Class<? extends EntityProxy> record = getRecordFromClassToken(recordToken);
        EntityKey entityKey = new EntityKey(
            recordObject.getString(ENCODED_ID_PROPERTY),
            (writeOperation == WriteOperation.PERSIST), record);
        involvedKeys.add(entityKey);
        dvsDataMap.put(entityKey, new DvsData(recordObject, writeOperation));
      }
    }
  }

  private WriteOperation detectDeleteOrUpdate(EntityKey entityKey,
      EntityData entityData) throws IllegalArgumentException,
      SecurityException, IllegalAccessException, InvocationTargetException,
      NoSuchMethodException, JSONException, InstantiationException {
    if (entityData == null || entityData.entityInstance == null) {
      return null;
    }

    Object entityInstance = getEntityInstance(entityKey);
    if (entityInstance == null) {
      return WriteOperation.DELETE;
    }
    if (hasChanged(beforeDataMap.get(entityKey).serializedEntity,
        serializeEntity(entityInstance, entityKey))) {
      return WriteOperation.UPDATE;
    }
    return null;
  }

  private String encodeId(Object id) {
    if (id instanceof String) {
      return base64Encode((String) id);
    }
    return encodePropertyValue(id).toString();
  }

  @SuppressWarnings("unchecked")
  private String encodeRelated(Class<?> propertyType, String propertyName,
      RequestProperty propertyContext, Object returnValue)
      throws NoSuchMethodException, IllegalAccessException,
      InvocationTargetException, JSONException {
    String encodedId = isEntityReference(returnValue, propertyType);

    String keyRef = new EntityKey(encodedId, false,
        (Class<? extends EntityProxy>) propertyType).toString();

    addRelatedObject(keyRef, returnValue, castToRecordClass(propertyType),
        propertyContext.getProperty(propertyName));
    return keyRef;
  }

  private JSONObject encodeRelatedObjectsToJson() throws JSONException {
    JSONObject array = new JSONObject();
    for (Map.Entry<String, JSONObject> entry : relatedObjects.entrySet()) {
      array.put(entry.getKey(), entry.getValue());
    }
    return array;
  }

  private JSONObject getCreateReturnRecord(EntityKey originalEntityKey,
      EntityData entityData) throws SecurityException, JSONException,
      IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    // id/futureId, the identifying field is sent back from the incoming record.
    assert originalEntityKey.isFuture;
    Object entityInstance = entityData.entityInstance;
    assert entityInstance != null;
    JSONObject returnObject = new JSONObject();
    returnObject.put(ENCODED_FUTUREID_PROPERTY, originalEntityKey.encodedId
        + "");
    // violations have already been taken care of.
    Object newId = getRawPropertyValueFromDatastore(entityInstance,
        ENTITY_ID_PROPERTY);
    if (newId == null) {
      log.warning("Record with futureId " + originalEntityKey.encodedId
          + " not persisted");
      return null; // no changeRecord for this CREATE.
    }

    newId = encodeId(newId);
    returnObject.put(ENCODED_ID_PROPERTY, getSchemaAndId(
        originalEntityKey.proxyType, newId));
    returnObject.put(ENCODED_VERSION_PROPERTY,
        encodePropertyValueFromDataStore(entityInstance,
            ENTITY_VERSION_PROPERTY, ENTITY_VERSION_PROPERTY.getName(),
            propertyRefs));
    return returnObject;
  }

  /**
   * Given param0, return the EntityKey. String is of the form
   * "239@NO@com....EmployeeRecord" or "239@IS@com...EmployeeRecord".
   */
  private EntityKey getEntityKey(String string) {
    String parts[] = string.split("@");
    assert parts.length == 3;

    String encodedId = parts[0];
    return new EntityKey(encodedId, "IS".equals(parts[1]),
        getRecordFromClassToken(parts[2]));
  }

  private Method getIdMethodForEntity(Class<?> entityType)
      throws NoSuchMethodException {
    Method idMethod = entityType.getMethod(getMethodNameFromPropertyName(
        ENTITY_ID_PROPERTY, "get"));
    return idMethod;
  }

  private Object getPropertyValueFromRequestCached(JSONObject recordObject,
      Map<String, Property<?>> propertiesInProxy, String key,
      Property<?> dtoProperty) throws JSONException, IllegalAccessException,
      InvocationTargetException, NoSuchMethodException, InstantiationException {
    Object propertyValue;
    if (!recordObject.isNull(key)
        && EntityProxy.class.isAssignableFrom(dtoProperty.getType())) {
      // if the property type is a Proxy, we expect an encoded Key string
      EntityKey propKey = getEntityKey(recordObject.getString(key));
      // check to see if we've already decoded this object from JSON
      Object cacheValue = cachedEntityLookup.get(propKey);
      // containsKey is used here because an entity lookup can return null
      if (cachedEntityLookup.containsKey(propKey)) {
        propertyValue = cacheValue;
      } else {
        propertyValue = getPropertyValueFromRequest(recordObject, key,
            propertiesInProxy.get(key).getType());
      }
    } else {
      propertyValue = getPropertyValueFromRequest(recordObject, key,
          propertiesInProxy.get(key).getType());
    }
    return propertyValue;
  }

  private Object getPropertyValueFromRequestCached(JSONArray recordArray,
      int index, Property<?> dtoProperty) throws JSONException, IllegalAccessException,
      InvocationTargetException, NoSuchMethodException, InstantiationException {
    Object propertyValue;
    Class<?> leafType = dtoProperty instanceof CollectionProperty<?,?>
        ? ((CollectionProperty<?,?>) dtoProperty).getLeafType()
        : dtoProperty.getType();

    // if the property type is a Proxy, we expect an encoded Key string
    if (EntityProxy.class.isAssignableFrom(leafType)) {
      // check to see if we've already decoded this object from JSON
      EntityKey propKey = getEntityKey(recordArray.getString(index));
      // containsKey is used here because an entity lookup can return null
      Object cacheValue = cachedEntityLookup.get(propKey);
      if (cachedEntityLookup.containsKey(propKey)) {
        propertyValue = cacheValue;
      } else {
        propertyValue = getPropertyValueFromRequest(recordArray, index,
            leafType);
      }
    } else {
      propertyValue = getPropertyValueFromRequest(recordArray, index, leafType);
    }
    return propertyValue;
  }

  private Object getRawPropertyValueFromDatastore(Object entityElement,
      String propertyName)
      throws SecurityException, NoSuchMethodException, IllegalAccessException,
      InvocationTargetException {
    String methodName = getMethodNameFromPropertyName(propertyName, "get");
    Method method = entityElement.getClass().getMethod(methodName);
    return method.invoke(entityElement);
  }

  private String getSchemaAndId(Class<? extends EntityProxy> proxyType,
      Object newId) {
    return proxyType.getName() + "@" + newId;
  }

  private SerializedEntity getSerializedEntity(EntityKey entityKey)
      throws IllegalArgumentException, SecurityException,
      IllegalAccessException, InvocationTargetException, NoSuchMethodException,
      JSONException, InstantiationException {

    Object entityInstance = getEntityInstance(entityKey);
    JSONObject serializedEntity = serializeEntity(entityInstance, entityKey);
    return new SerializedEntity(entityInstance, serializedEntity);
  }

  /**
   * Returns a JSONObject with at most three keys: CREATE, UPDATE, DELETE. Each
   * value is a JSONArray of JSONObjects.
   */
  private JSONObject getSideEffects() throws SecurityException, JSONException,
      IllegalAccessException, InvocationTargetException, NoSuchMethodException,
      IllegalArgumentException, InstantiationException {
    JSONObject sideEffects = new JSONObject();
    JSONArray createArray = new JSONArray();
    JSONArray deleteArray = new JSONArray();
    JSONArray updateArray = new JSONArray();
    for (EntityKey entityKey : involvedKeys) {
      EntityData entityData = afterDvsDataMap.get(entityKey);
      if (entityData == null) {
        continue;
      }
      if (entityKey.isFuture) {
        JSONObject createRecord = getCreateReturnRecord(entityKey, entityData);
        if (createRecord != null) {
          createArray.put(createRecord);
        }
        continue;
      }
      WriteOperation writeOperation = detectDeleteOrUpdate(entityKey,
          entityData);
      if (writeOperation == WriteOperation.DELETE) {
        JSONObject deleteRecord = new JSONObject();
        deleteRecord.put(ENCODED_ID_PROPERTY, getSchemaAndId(
            entityKey.proxyType, entityKey.encodedId));
        deleteArray.put(deleteRecord);
      }
      if (writeOperation == WriteOperation.UPDATE) {
        JSONObject updateRecord = new JSONObject();
        updateRecord.put(ENCODED_ID_PROPERTY, getSchemaAndId(
            entityKey.proxyType, entityKey.encodedId));
        updateArray.put(updateRecord);
      }
    }
    if (createArray.length() > 0) {
      sideEffects.put(WriteOperation.PERSIST.name(), createArray);
    }
    if (deleteArray.length() > 0) {
      sideEffects.put(WriteOperation.DELETE.name(), deleteArray);
    }
    if (updateArray.length() > 0) {
      sideEffects.put(WriteOperation.UPDATE.name(), updateArray);
    }
    return sideEffects;
  }

  private JSONArray getViolations() throws JSONException {
    JSONArray violations = new JSONArray();
    for (EntityKey entityKey : involvedKeys) {
      EntityData entityData = afterDvsDataMap.get(entityKey);
      if (entityData == null || entityData.violations == null
          || entityData.violations.length() == 0) {
        continue;
      }
      DvsData dvsData = dvsDataMap.get(entityKey);
      if (dvsData != null) {
        JSONObject returnObject = new JSONObject();
        returnObject.put(VIOLATIONS_TOKEN, entityData.violations);
        if (entityKey.isFuture) {
          returnObject.put(ENCODED_FUTUREID_PROPERTY, entityKey.encodedId);
          returnObject.put(ENCODED_ID_PROPERTY, getSchemaAndId(
              entityKey.proxyType, null));
        } else {
          returnObject.put(ENCODED_ID_PROPERTY, getSchemaAndId(
              entityKey.proxyType, entityKey.encodedId));
        }
        violations.put(returnObject);
      }
    }
    return violations;
  }

  private String isEntityReference(Object entity, Class<?> proxyPropertyType)
      throws SecurityException, NoSuchMethodException,
      IllegalArgumentException, IllegalAccessException,
      InvocationTargetException {
    if (entity != null && EntityProxy.class.isAssignableFrom(proxyPropertyType)) {
      Method idMethod = getIdMethodForEntity(entity.getClass());
      return encodeId(idMethod.invoke(entity));
    }
    return null;
  }

  /**
   * returns true if the property has been requested. TODO: use the properties
   * that should be coming with the request.
   *
   * @param p the field of entity ref
   * @param propertyContext the root of the current dotted property reference
   * @return has the property value been requested
   */
  private boolean requestedProperty(Property<?> p,
      RequestProperty propertyContext) {
    if (propertyContext == null) {
      return false;
    }
    Class<?> leafType = p.getType();
    if (p instanceof CollectionProperty<?,?>) {
      leafType = ((CollectionProperty<?,?>) p).getLeafType();
    }
    if (EntityProxy.class.isAssignableFrom(leafType)) {
      return propertyContext.hasProperty(p.getName());
    }

    return true;
  }

  /**
   * Return the client-visible properties of an entityInstance as a JSONObject.
   * <p>
   * TODO: clean up the copy-paste from getJSONObject.
   */
  private JSONObject serializeEntity(Object entityInstance, EntityKey entityKey)
      throws SecurityException, NoSuchMethodException,
      IllegalArgumentException, IllegalAccessException,
      InvocationTargetException, JSONException {
    if (entityInstance == null) {
      return null;
    }
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(ENCODED_ID_PROPERTY, entityKey.encodedId);
    for (Property<?> p : allProperties(entityKey.proxyType)) {
      String propertyName = p.getName();
      String methodName = getMethodNameFromPropertyName(propertyName, "get");
      Method method = entityInstance.getClass().getMethod(methodName);
      Object returnValue = method.invoke(entityInstance);

      Object propertyValue;
      String encodedEntityId = isEntityReference(returnValue, p.getType());
      if (returnValue == null) {
        propertyValue = JSONObject.NULL;
      } else if (encodedEntityId != null) {
        propertyValue = encodedEntityId
            + "@NO@"
            + operationRegistry.getSecurityProvider().encodeClassType(
                p.getType());
      } else if (p instanceof CollectionProperty<?,?>) {
        JSONArray array = new JSONArray();
        for (Object val : ((Collection<?>) returnValue)) {
          String encodedIdVal = isEntityReference(val, p.getType());
          if (encodedIdVal != null) {
            propertyValue = encodedIdVal
                + "@NO@"
                + operationRegistry.getSecurityProvider().encodeClassType(
                    p.getType());
          } else {
            propertyValue = encodePropertyValue(val);
          }
          array.put(propertyValue);
        }
        propertyValue = array;
      } else {
        propertyValue = encodePropertyValue(returnValue);
      }
      jsonObject.put(propertyName, propertyValue);
    }
    return jsonObject;
  }

  @SuppressWarnings("unchecked")
  private Object toJsonArray(RequestDefinition operation, Object result)
      throws IllegalAccessException, JSONException, NoSuchMethodException,
      InvocationTargetException {
    JSONArray jsonArray = getJsonArray((Collection<?>) result,
        (Class<? extends EntityProxy>) operation.getReturnType());
    return jsonArray;
  }

  /**
   * Update propertiesInRecord based on the types of entity type.
   */
  private Map<String, Class<?>> updatePropertyTypes(
      Map<String, Property<?>> propertiesInProxy, Class<?> entity) {
    Map<String, Class<?>> toReturn = new HashMap<String, Class<?>>();

    /*
     * TODO: this logic fails if the field and getter/setter methods are
     * differently named.
     */
    for (Field field : entity.getDeclaredFields()) {
      Property<?> property = propertiesInProxy.get(field.getName());
      if (property == null) {
        continue;
      }
      Class<?> fieldType = property.getType();
      if (property instanceof CollectionProperty<?,?>) {
        toReturn.put(field.getName(), fieldType);
      } else if (fieldType != null) {
        if (EntityProxy.class.isAssignableFrom(fieldType)) {
          ProxyFor pFor = fieldType.getAnnotation(ProxyFor.class);
          if (pFor != null) {
            fieldType = pFor.value();
          }
          // TODO: remove override declared method return type with field type
          if (!fieldType.equals(field.getType())) {
            fieldType = field.getType();
          }
        }
        toReturn.put(field.getName(), fieldType);
      }
    }
    return toReturn;
  }
}
