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

import com.google.gwt.requestfactory.shared.ProxyFor;
import com.google.gwt.requestfactory.shared.Property;
import com.google.gwt.requestfactory.shared.PropertyReference;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.RequestData;
import com.google.gwt.requestfactory.shared.WriteOperation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

  private static class EntityKey {
    private final boolean isFuture;
    // TODO: update for non-long id?
    private final long id;
    private final Class<? extends EntityProxy> record;

    EntityKey(long id, boolean isFuture, Class<? extends EntityProxy> record) {
      this.id = id;
      this.isFuture = isFuture;
      assert record != null;
      this.record = record;
    }

    @Override
    public boolean equals(Object ob) {
      if (!(ob instanceof EntityKey)) {
        return false;
      }
      EntityKey other = (EntityKey) ob;
      return (id == other.id) && (isFuture == other.isFuture)
          && (record.equals(other.record));
    }

    @Override
    public int hashCode() {
      return (int) (31 * this.record.hashCode() + (31 * this.id + (isFuture ? 1
          : 0)));
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

  private RequestProperty propertyRefs;

  private Map<String, JSONObject> relatedObjects = new HashMap<String, JSONObject>();

  private OperationRegistry operationRegistry;

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
  private Map<EntityKey, Object> cachedEntityLookup = new HashMap<EntityKey, Object>();
  private Set<EntityKey> involvedKeys = new HashSet<EntityKey>();
  private Map<EntityKey, DvsData> dvsDataMap = new HashMap<EntityKey, DvsData>();
  private Map<EntityKey, SerializedEntity> beforeDataMap = new HashMap<EntityKey, SerializedEntity>();
  private Map<EntityKey, EntityData> afterDvsDataMap = new HashMap<EntityKey, EntityData>();

  public Collection<Property<?>> allProperties(Class<? extends EntityProxy> clazz) {
    Set<Property<?>> rtn = new HashSet<Property<?>>();
    for (Field f : clazz.getFields()) {
      if (Modifier.isStatic(f.getModifiers())
          && Property.class.isAssignableFrom(f.getType())) {
        try {
          rtn.add((Property<?>) f.get(null));
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return rtn;
  }

  public String decodeAndInvokeRequest(String encodedRequest) throws RequestProcessingException {
    try {
      Logger.getLogger(this.getClass().getName()).finest("Incoming request "
          + encodedRequest);
      String response = processJsonRequest(encodedRequest).toString();
      Logger.getLogger(this.getClass().getName()).finest("Outgoing response "
          + response);
      return response;
    } catch (Exception e) {
      JSONObject exceptionResponse = new JSONObject();
      try {
        exceptionResponse.put("exception", "Server error");
      } catch (JSONException jsonException) {
        throw new IllegalStateException(jsonException);
      }
      throw new RequestProcessingException("Unexpected exception", e, 
          exceptionResponse.toString());
    } 
  }

  /**
   * Encodes parameter value.
   */
  public Object decodeParameterValue(Type genericParameterType,
      String parameterValue) {
    Class<?>parameterType = null;
    if (genericParameterType instanceof Class<?>) {
      parameterType = (Class<?>) genericParameterType;
    }
    if (genericParameterType instanceof ParameterizedType) {
      ParameterizedType pType = (ParameterizedType) genericParameterType;
      if (PropertyReference.class == pType.getRawType()) {
        parameterType = (Class<?>) pType.getActualTypeArguments()[0];
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
      try {
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
        throw new IllegalArgumentException(
            "Can't decode enum " + parameterType + " no matching ordinal "
                + ordinal);
      } catch (NoSuchMethodException e) {
        throw new IllegalArgumentException(
            "Can't decode enum " + parameterType);
      } catch (InvocationTargetException e) {
        throw new IllegalArgumentException(
            "Can't decode enum " + parameterType);
      } catch (IllegalAccessException e) {
        throw new IllegalArgumentException(
            "Can't decode enum " + parameterType);
      }
    }
    if (Date.class == parameterType) {
      return new Date(Long.parseLong(parameterValue));
    }
    if (EntityProxy.class.isAssignableFrom(parameterType)) {
      /* TODO: 1. Don't resolve in this step, just get EntityKey. May need to
       * use DVS.
       *
       * 2. Merge the following and the object resolution code in getEntityKey.
       * 3. Update the involvedKeys set.
       */
      ProxyFor service = parameterType.getAnnotation(ProxyFor.class);
      if (service != null) {
        Class<?> sClass = service.value();
        EntityKey entityKey = getEntityKey(parameterValue.toString());

        DvsData dvsData = dvsDataMap.get(entityKey);
        try {
          if (dvsData != null) {
            EntityData entityData = getEntityDataForRecord(entityKey,
                dvsData.jsonObject, dvsData.writeOperation);
            return entityData.entityInstance;
          } else {
            Method findMeth = sClass.getMethod(
                getMethodNameFromPropertyName(sClass.getSimpleName(), "find"),
                Long.class);
            return findMeth.invoke(null, entityKey.id);
          }
        } catch (NoSuchMethodException e) {
          throw new IllegalArgumentException(
              "No such method " + getMethodNameFromPropertyName(
                  sClass.getSimpleName(), "find"), e);
        } catch (InvocationTargetException e) {
          throw new IllegalArgumentException("Can't invoke method", e);
        } catch (IllegalAccessException e) {
          throw new IllegalArgumentException("Can't invoke method", e);
        }
      }
    }
    throw new IllegalArgumentException(
        "Unknown parameter type: " + parameterType);
  }

  public Object encodePropertyValue(Object value) {
    if (value == null) {
      return value;
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
      Class<?> propertyType, String propertyName,
      RequestProperty propertyContext)
      throws SecurityException, NoSuchMethodException, IllegalAccessException,
      InvocationTargetException, JSONException {
    String methodName = getMethodNameFromPropertyName(propertyName, "get");
    Method method = entityElement.getClass().getMethod(methodName);
    Object returnValue = method.invoke(entityElement);
    if (returnValue != null && EntityProxy.class.isAssignableFrom(propertyType)) {
      Method idMethod = returnValue.getClass().getMethod("getId");
      Long id = (Long) idMethod.invoke(returnValue);

      String keyRef = operationRegistry.getSecurityProvider().encodeClassType(
          propertyType)
          + "-" + id;
      addRelatedObject(keyRef, returnValue,
          castToRecordClass(propertyType),
          propertyContext.getProperty(propertyName));
      // replace value with id reference
      return keyRef;
    }
    return encodePropertyValue(returnValue);
  }

  /**
   * Generate an ID for a new record. The default behavior is to return null and
   * let the data store generate the ID automatically.
   *
   * @param key the key of the record field
   * @return the ID of the new record, or null to auto generate
   */
  public Long generateIdForCreate(@SuppressWarnings("unused") String key) {
    // ignored. id is assigned by default.
    return null;
  }

  /**
   * Returns the entityData for a record in the DeltaValueStore.
   * <p>
   * A <i>set</i> might have side-effects, but we don't handle that.
   */
  public EntityData getEntityDataForRecord(EntityKey entityKey,
      JSONObject recordObject, WriteOperation writeOperation) {

    try {
      Class<?> entity = getEntityFromRecordAnnotation(entityKey.record);

      Map<String, Class<?>> propertiesInRecord = getPropertiesFromRecord(entityKey.record);
      Map<String, Class<?>> propertiesToDTO = new HashMap<String, Class<?>>(propertiesInRecord);
      validateKeys(recordObject, propertiesInRecord.keySet());
      updatePropertyTypes(propertiesInRecord, entity);

      // get entityInstance
      Object entityInstance = getEntityInstance(writeOperation, entity,
          recordObject.get("id"), propertiesInRecord.get("id"));

      cachedEntityLookup.put(entityKey, entityInstance);

      Iterator<?> keys = recordObject.keys();
      while (keys.hasNext()) {
        String key = (String) keys.next();
        Class<?> propertyType = propertiesInRecord.get(key);
        Class<?> dtoType = propertiesToDTO.get(key);
        if (writeOperation == WriteOperation.CREATE && ("id".equals(key))) {
          Long id = generateIdForCreate(key);
          if (id != null) {
            entity.getMethod(getMethodNameFromPropertyName(key, "set"),
                propertyType).invoke(entityInstance, id);
          }
        } else {
          Object propertyValue = null;
          if (EntityProxy.class.isAssignableFrom(dtoType)) {
            EntityKey propKey = getEntityKey(recordObject.getString(key));
            Object cacheValue = cachedEntityLookup.get(propKey);
            if (cachedEntityLookup.containsKey(propKey)) {
              propertyValue = cacheValue;
            } else {
              propertyValue = getPropertyValueFromRequest(recordObject, key,
               propertiesToDTO.get(key));
            }
          } else {
             propertyValue = getPropertyValueFromRequest(recordObject, key,
               propertiesToDTO.get(key));
          }
          entity.getMethod(getMethodNameFromPropertyName(key, "set"),
              propertyType).invoke(entityInstance, propertyValue);
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
    } catch (Exception ex) {
      log.severe(String.format("Caught exception [%s] %s",
          ex.getClass().getName(), ex.getLocalizedMessage()));
      return getEntityDataForException(ex);
    }
  }

  @SuppressWarnings("unchecked")
  public Class<Object> getEntityFromRecordAnnotation(
      Class<? extends EntityProxy> record) {
    ProxyFor dtoAnn = record.getAnnotation(ProxyFor.class);
    if (dtoAnn != null) {
      return (Class<Object>) dtoAnn.value();
    }
    throw new IllegalArgumentException("Record class " + record.getName()
        + " missing DataTransferObject annotation");
  }

  public Object getEntityInstance(WriteOperation writeOperation,
      Class<?> entity, Object idValue, Class<?> idType)
      throws SecurityException, InstantiationException, IllegalAccessException,
      InvocationTargetException, NoSuchMethodException {

    if (writeOperation == WriteOperation.CREATE) {
      return entity.getConstructor().newInstance();
    }
    // TODO: check "version" validity.
    return entity.getMethod("find" + entity.getSimpleName(), 
        idType).invoke(null, decodeParameterValue(idType, idValue.toString()));
  }

  /**
   * Converts the returnValue of a 'get' method to a JSONArray.
   *
   * @param resultList     object returned by a 'get' method, must be of type
   *                       List<?>
   * @param entityKeyClass the class type of the contained value
   * @return the JSONArray
   */
  public JSONArray getJsonArray(List<?> resultList,
      Class<? extends EntityProxy> entityKeyClass)
      throws IllegalArgumentException, SecurityException,
      IllegalAccessException, JSONException, NoSuchMethodException,
      InvocationTargetException {
    JSONArray jsonArray = new JSONArray();
    if (resultList.size() == 0) {
      return jsonArray;
    }

    for (Object entityElement : resultList) {
      jsonArray.put(getJsonObject(entityElement, entityKeyClass, propertyRefs));
    }
    return jsonArray;
  }

  public JSONObject getJsonObject(Object entityElement,
      Class<? extends EntityProxy> entityKeyClass, RequestProperty propertyContext)
      throws JSONException, NoSuchMethodException, IllegalAccessException,
      InvocationTargetException {
    JSONObject jsonObject = new JSONObject();
    for (Property<?> p : allProperties(entityKeyClass)) {

      if (requestedProperty(p, propertyContext)) {
        String propertyName = p.getName();
        jsonObject.put(propertyName, encodePropertyValueFromDataStore(
            entityElement, p.getType(), propertyName, propertyContext));
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
  public String getMethodNameFromPropertyName(String propertyName,
      String prefix) {
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
      Map<String, String> parameterMap, Type parameterClasses[]) {
    // TODO: create an EntityMethodCall (instance, args) instead.
    assert parameterClasses != null;
    Object args[][] = new Object[2][];
    args[0] = new Object[1];
    if (isInstanceMethod) {
      EntityKey entityKey = getEntityKey(parameterMap.get(RequestData.PARAM_TOKEN + "0"));
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
          parameterMap.get(RequestData.PARAM_TOKEN + (i + offset)));
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

  /**
   * @param jsonObject
   * @return
   * @throws org.json.JSONException
   */
  public Map<String, String> getParameterMap(JSONObject jsonObject)
      throws JSONException {
    Map<String, String> parameterMap = new HashMap<String, String>();
    Iterator<?> keys = jsonObject.keys();
    while (keys.hasNext()) {
      String key = keys.next().toString();
      if (key.startsWith(RequestData.PARAM_TOKEN)) {
        parameterMap.put(key, jsonObject.getString(key));
      }
    }
    return parameterMap;
  }

  /**
   * Returns the property fields (name => type) for a record.
   */
  public Map<String, Class<?>> getPropertiesFromRecord(
      Class<? extends EntityProxy> record) throws SecurityException,
      IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Map<String, Class<?>> properties = new HashMap<String, Class<?>>();
    for (Field f : record.getFields()) {
      if (Property.class.isAssignableFrom(f.getType())) {
        Class<?> propertyType = (Class<?>) f.getType().getMethod("getType").invoke(
            f.get(null));
        properties.put(f.getName(), propertyType);
      }
    }
    return properties;
  }

  /**
   * Returns the property value, in the specified type, from the request object.
   * The value is put in the DataStore.
   */
  public Object getPropertyValueFromRequest(JSONObject recordObject, String key,
      Class<?> propertyType) throws JSONException {
    return decodeParameterValue(propertyType, recordObject.get(key).toString());
  }

  @SuppressWarnings("unchecked")
  public Class<EntityProxy> getRecordFromClassToken(String recordToken) {
    try {
      Class<?> clazz = Class.forName(recordToken, false, 
          getClass().getClassLoader());
      if (EntityProxy.class.isAssignableFrom(clazz)) {
        return (Class<EntityProxy>) clazz;
      }
      throw new SecurityException(
          "Attempt to access non-record class " + recordToken);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(
          "Non-existent record class " + recordToken);
    }
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

  public Object invokeDomainMethod(Object domainObject, Method domainMethod, Object args[])
      throws IllegalAccessException, InvocationTargetException {
    return domainMethod.invoke(domainObject, args);
  }

  public JSONObject processJsonRequest(String jsonRequestString)
      throws JSONException, NoSuchMethodException, IllegalAccessException,
      InvocationTargetException, ClassNotFoundException {
    RequestDefinition operation;
    JSONObject topLevelJsonObject = new JSONObject(jsonRequestString);

    String operationName = topLevelJsonObject.getString(RequestData.OPERATION_TOKEN);
    String propertyRefsString = topLevelJsonObject.has(RequestData.PROPERTY_REF_TOKEN)
        ? topLevelJsonObject.getString(RequestData.PROPERTY_REF_TOKEN) : "";
    propertyRefs = RequestProperty.parse(propertyRefsString);

    operation = getOperation(operationName);
    Class<?> domainClass = Class.forName(operation.getDomainClassName());
    Method domainMethod = domainClass.getMethod(
        operation.getDomainMethodName(), operation.getParameterTypes());
    if (Modifier.isStatic(domainMethod.getModifiers()) == operation.isInstance()) {
      throw new IllegalArgumentException("the " + domainMethod.getName()
          + " should " + (operation.isInstance() ? "not " : "") + "be static");
    }

    if (topLevelJsonObject.has(RequestData.CONTENT_TOKEN)) {
      // updates involvedKeys and dvsDataMap.
      decodeDVS(topLevelJsonObject.getString(RequestData.CONTENT_TOKEN));
    }
    // get the domain object (for instance methods) and args.
    Object args[][] = getObjectsFromParameterMap(operation.isInstance(),
        getParameterMap(topLevelJsonObject), operation.getRequestParameterTypes());
    // Construct beforeDataMap 
    constructBeforeDataMap();
    // Construct afterDvsDataMap.
    constructAfterDvsDataMap();

    // violations are the only sideEffects at this point.
    JSONObject sideEffectsBeforeExecution = getViolationsAsSideEffects();
    if (sideEffectsBeforeExecution.length() > 0) {
      JSONObject envelop = new JSONObject();
      envelop.put(RequestData.SIDE_EFFECTS_TOKEN, sideEffectsBeforeExecution);
      return envelop;
    }

    // resolve parameters that are so far just EntityKeys.
    // TODO: resolve paramters other than the domainInstance
    EntityKey domainEntityKey = null;
    if (args[0][0] != null) {
      domainEntityKey = (EntityKey) args[0][0];
      EntityData domainEntityData = afterDvsDataMap.get(domainEntityKey);
      assert domainEntityData != null;
      args[0][0] = domainEntityData.entityInstance;
      assert args[0][0] != null;
    }
    Object result = invokeDomainMethod(args[0][0], domainMethod, args[1]);

    JSONObject sideEffects = getSideEffects();
    
    if ((result instanceof List<?>) != operation.isReturnTypeList()) {
      throw new IllegalArgumentException(
          String.format("Type mismatch, expected %s%s, but %s returns %s",
              operation.isReturnTypeList() ? "list of " : "",
              operation.getReturnType(), domainMethod,
              domainMethod.getReturnType()));
    }

    JSONObject envelop = new JSONObject();
    if (result instanceof List<?>) {
      envelop.put(RequestData.RESULT_TOKEN, toJsonArray(operation, result));
    } else if (result instanceof Number || result instanceof Enum<?>
        || result instanceof String || result instanceof Date
        || result instanceof Character || result instanceof Boolean) {
      envelop.put(RequestData.RESULT_TOKEN, result);
    } else {
      JSONObject jsonObject = toJsonObject(operation, result);
      envelop.put(RequestData.RESULT_TOKEN, jsonObject);
    }
    envelop.put(RequestData.SIDE_EFFECTS_TOKEN, sideEffects);
    envelop.put(RequestData.RELATED_TOKEN, encodeRelatedObjectsToJson());
    return envelop;
  }

  public void setOperationRegistry(OperationRegistry operationRegistry) {
    this.operationRegistry = operationRegistry;
  }

  public void validateKeys(JSONObject recordObject,
      Set<String> declaredProperties) {
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
   * Returns true iff the after and before JSONObjects are different.
   * 
   * @throws JSONException
   */
  boolean hasChanged(JSONObject before, JSONObject after) {
    if (before == null) {
      return after != null;
    }
    // before != null
    if (after == null) {
      return true;
    }
    try {
      // before != null && after != null
      Iterator<?> keyIterator = before.keys();
      while (keyIterator.hasNext()) {
        String key = keyIterator.next().toString();
        Object beforeValue = before.get(key);
        Object afterValue = after.get(key);
        if (beforeValue == null) {
          if (afterValue == null) {
            continue;
          }
          return true;
        }
        if (afterValue == null) {
          return true;
        }
        if (!beforeValue.equals(afterValue)) {
          return true;
        }
      }
    } catch (JSONException ex) {
      log.warning("Encountered a JSONException " + ex.getMessage()
          + " in getChanged");
      return false;
    }
    return false;
  }

  private void addRelatedObject(String keyRef, Object returnValue,
      Class<? extends EntityProxy> propertyType, RequestProperty propertyContext)
      throws JSONException, IllegalAccessException, NoSuchMethodException,
      InvocationTargetException {

    relatedObjects.put(keyRef, getJsonObject(returnValue, propertyType,
        propertyContext));
  }

  @SuppressWarnings("unchecked")
  private Class<? extends EntityProxy> castToRecordClass(Class<?> propertyType) {
    return (Class<? extends EntityProxy>) propertyType;
  }

  /**
   * @throws JSONException 
   * 
   */
  private void constructAfterDvsDataMap() {
    afterDvsDataMap = new HashMap<EntityKey, EntityData>();
    for (EntityKey entityKey : involvedKeys) {
      // use the beforeDataMap and dvsDataMap
      DvsData dvsData = dvsDataMap.get(entityKey);
      if (dvsData != null) {
        EntityData entityData = getEntityDataForRecord(entityKey,
            dvsData.jsonObject, dvsData.writeOperation);
        if (entityKey.isFuture) {
          // TODO: assert that the id is null for entityData.entityInstance
        }
        afterDvsDataMap.put(entityKey, entityData);
      } else {
        assert !entityKey.isFuture;
        SerializedEntity serializedEntity = beforeDataMap.get(entityKey);
        assert serializedEntity.entityInstance != null;
        afterDvsDataMap.put(entityKey, new EntityData(
            serializedEntity.entityInstance, null));
      }
    }
  }

  /**
   * Constructs the beforeDataMap.
   * 
   * <p>
   * Algorithm: go through the involvedKeys, and find the entityData
   * corresponding to each.
   * 
   */
  private void constructBeforeDataMap() throws IllegalArgumentException,
      SecurityException, IllegalAccessException, InvocationTargetException,
      NoSuchMethodException, JSONException {
    for (EntityKey entityKey : involvedKeys) {
      if (entityKey.isFuture) {
        // the "before" is empty.
        continue;
      }
      beforeDataMap.put(entityKey, getSerializedEntity(entityKey));
    }
  }

  /**
   * Decode deltaValueStore to populate involvedKeys and dvsDataMap.
   */
  private void decodeDVS(String content) throws SecurityException {
    try {
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
          EntityKey entityKey = new EntityKey(recordObject.getLong("id"),
              (writeOperation == WriteOperation.CREATE), record);
          involvedKeys.add(entityKey);
          dvsDataMap.put(entityKey, new DvsData(recordObject, writeOperation));
        }
      }
    } catch (JSONException e) {
      throw new IllegalArgumentException("sync failed: ", e);
    }
  }

  private WriteOperation detectDeleteOrUpdate(EntityKey entityKey,
      EntityData entityData) throws IllegalArgumentException,
      SecurityException, IllegalAccessException, InvocationTargetException,
      NoSuchMethodException, JSONException {
    if (entityData.entityInstance == null) {
      return null;
    }
    
    Class<?> entityClass = getEntityFromRecordAnnotation(entityKey.record);
    // TODO: merge this lookup code with other uses.
    Object entityInstance = entityClass.getMethod(
        "find" + entityClass.getSimpleName(), Long.class).invoke(null,
        new Long(entityKey.id));
    if (entityInstance == null) {
      return WriteOperation.DELETE;
    }
    if (hasChanged(beforeDataMap.get(entityKey).serializedEntity,
        serializeEntity(entityInstance, entityKey.record))) {
      return WriteOperation.UPDATE;
    }
    return null;
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
    returnObject.put("futureId", originalEntityKey.id + "");
    // violations have already been taken care of.
    Object newId = encodePropertyValueFromDataStore(entityInstance, Long.class,
        "id", propertyRefs);
    if (newId == null) {
      log.warning("Record with futureId " + originalEntityKey.id
          + " not persisted");
      return null; // no changeRecord for this CREATE.
    }
    returnObject.put("id", getSchemaAndId(originalEntityKey.record, newId));
    returnObject.put(
        "version",
        encodePropertyValueFromDataStore(entityInstance, Integer.class,
            "version", propertyRefs));
    return returnObject;
  }

  private EntityData getEntityDataForException(Exception ex) {

    JSONObject violations = null;
    try {
      // expecting violations to be a JSON object.
      violations = new JSONObject();
      if (ex instanceof NumberFormatException) {
        violations.put("Expected a number instead of String", ex.getMessage());
      } else {
        violations.put("", "unexpected server error");
      }
    } catch (JSONException e) {
      // ignore.
      e.printStackTrace();
    }
    return new EntityData(null, violations);
  }

  /**
   * Given param0, return the EntityKey. String is of the form
   * "239-NO-com....EmployeeRecord" or "239-IS-com...EmployeeRecord".
   */
  private EntityKey getEntityKey(String string) {
    String parts[] = string.split("-");
    assert parts.length == 3;

    Long id = Long.parseLong(parts[0]);
    return new EntityKey(id, "IS".equals(parts[1]),
        getRecordFromClassToken(parts[2]));
  }

  private String getSchemaAndId(Class<? extends EntityProxy> record, Object newId) {
    return record.getName() + "-" + newId;
  }

  private SerializedEntity getSerializedEntity(EntityKey entityKey)
      throws IllegalArgumentException, SecurityException,
      IllegalAccessException, InvocationTargetException, NoSuchMethodException,
      JSONException {
    Class<?> entityClass = getEntityFromRecordAnnotation(entityKey.record);
    // TODO: merge this lookup code with other uses.
    Object entityInstance = entityClass.getMethod(
        "find" + entityClass.getSimpleName(), Long.class).invoke(null,
        new Long(entityKey.id));
    JSONObject serializedEntity = serializeEntity(entityInstance,
        entityKey.record);
    return new SerializedEntity(entityInstance, serializedEntity);
  }

  /**
   * Returns a JSONObject with at most three keys: CREATE, UPDATE, DELETE. Each
   * value is a JSONArray of JSONObjects.
   */
  private JSONObject getSideEffects() throws SecurityException, JSONException,
      IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    JSONObject sideEffects = new JSONObject();
    JSONArray createArray = new JSONArray();
    JSONArray deleteArray = new JSONArray();
    JSONArray updateArray = new JSONArray();
    for (EntityKey entityKey : involvedKeys) {
      EntityData entityData = afterDvsDataMap.get(entityKey);
      assert entityData != null;
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
        deleteRecord.put("id", getSchemaAndId(entityKey.record, entityKey.id));
        deleteArray.put(deleteRecord);
      }
      if (writeOperation == WriteOperation.UPDATE) {
        JSONObject updateRecord = new JSONObject();
        updateRecord.put("id", getSchemaAndId(entityKey.record, entityKey.id));
        updateArray.put(updateRecord);
      }
    }
    if (createArray.length() > 0) {
      sideEffects.put(WriteOperation.CREATE.name(), createArray);
    }
    if (deleteArray.length() > 0) {
      sideEffects.put(WriteOperation.DELETE.name(), deleteArray);
    }
    if (updateArray.length() > 0) {
      sideEffects.put(WriteOperation.UPDATE.name(), updateArray);
    }
    return sideEffects;
  }

  private JSONObject getViolationsAsSideEffects() throws JSONException {
    JSONObject sideEffects = new JSONObject();
    for (EntityKey entityKey : involvedKeys) {
      EntityData entityData = afterDvsDataMap.get(entityKey);
      assert entityData != null;
      if (entityData.violations == null || entityData.violations.length() == 0) {
        continue;
      }
      // find the WriteOperation
      DvsData dvsData = dvsDataMap.get(entityKey);
      if (dvsData != null && dvsData.writeOperation != null) {
        JSONObject returnObject = new JSONObject();
        returnObject.put("violations", entityData.violations);
        if (entityKey.isFuture) {
          returnObject.put("futureId", entityKey.id + "");
        } else {
          returnObject.put("id",  getSchemaAndId(entityKey.record, entityKey.id));
        }
        JSONArray arrayForOperation = null;
        if (sideEffects.has(dvsData.writeOperation.name())) {
          arrayForOperation = sideEffects.getJSONArray(dvsData.writeOperation.name());
        } else {
          arrayForOperation = new JSONArray();
          sideEffects.put(dvsData.writeOperation.name(), arrayForOperation);
        }
        arrayForOperation.put(returnObject);
      }
    }
    return sideEffects;
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
    if (EntityProxy.class.isAssignableFrom(p.getType())) {
      return propertyContext.hasProperty(p.getName());
    }

    return true;
  }

  /**
   * Return the properties of an entityInstance, visible on the client, as a
   * JSONObject.
   *<p>
   * TODO: clean up the copy-paste from getJSONObject.
   */
  private JSONObject serializeEntity(Object entityInstance,
      Class<? extends EntityProxy> recordClass) throws SecurityException,
      NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
      InvocationTargetException, JSONException {
    if (entityInstance == null) {
      return null;
    }
    JSONObject jsonObject = new JSONObject();
    for (Property<?> p : allProperties(recordClass)) {
      String propertyName = p.getName();
      String methodName = getMethodNameFromPropertyName(propertyName, "get");
      Method method = entityInstance.getClass().getMethod(methodName);
      Object returnValue = method.invoke(entityInstance);

      Object propertyValue;
      if (returnValue != null && EntityProxy.class.isAssignableFrom(p.getType())) {
        Method idMethod = returnValue.getClass().getMethod("getId");
        Long id = (Long) idMethod.invoke(returnValue);

        propertyValue = id + "-NO-" + operationRegistry.getSecurityProvider().encodeClassType(
            p.getType());
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
    JSONArray jsonArray = getJsonArray((List<?>) result,
        (Class<? extends EntityProxy>) operation.getReturnType());
    return jsonArray;
  }

  @SuppressWarnings("unchecked")
  private JSONObject toJsonObject(RequestDefinition operation, Object result)
      throws JSONException, NoSuchMethodException, IllegalAccessException,
      InvocationTargetException {
    JSONObject jsonObject = getJsonObject(result,
        (Class<? extends EntityProxy>) operation.getReturnType(), propertyRefs);
    return jsonObject;
  }

  /**
   * Update propertiesInRecord based on the types of entity.
   */
  private void updatePropertyTypes(Map<String, Class<?>> propertiesInRecord,
      Class<?> entity) {
    for (Field field : entity.getDeclaredFields()) {
      Class<?> fieldType = propertiesInRecord.get(field.getName());
      if (fieldType != null) {
        propertiesInRecord.put(field.getName(), field.getType());
      }
    }
  }
}
