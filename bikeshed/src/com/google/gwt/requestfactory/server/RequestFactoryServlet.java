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

import com.google.gwt.requestfactory.shared.RequestFactory;
import com.google.gwt.requestfactory.shared.RequestFactory.Config;
import com.google.gwt.requestfactory.shared.RequestFactory.RequestDefinition;
import com.google.gwt.requestfactory.shared.RequestFactory.WriteOperation;
import com.google.gwt.requestfactory.shared.impl.RequestDataManager;
import com.google.gwt.sample.expenses.gwt.request.ServerType;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Record;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handles GWT RequestFactory JSON requests. Configured via servlet context
 * param <code>servlet.serverOperation</code>, which must be set to the name of
 * a default instantiable class implementing
 * com.google.gwt.requestfactory.shared.RequestFactory.Config.
 * <p>
 * e.g.
 *
 * <pre>  &lt;context-param>
    &lt;param-name>servlet.serverOperation&lt;/param-name>
    &lt;param-value>com.myco.myapp.MyAppServerSideOperations&lt;/param-value>
  &lt;/context-param>

 * </pre>
 */
@SuppressWarnings("serial")
public class RequestFactoryServlet extends HttpServlet {

  /**
   * A class representing the pair of a domain entity and its corresponding
   * record class on the client side.
   */
  protected static class EntityRecordPair {
    public final Class<?> entity;
    public final Class<? extends Record> record;

    EntityRecordPair(Class<?> entity, Class<? extends Record> record) {
      this.entity = entity;
      this.record = record;
    }
  }

  private static final String SERVER_OPERATION_CONTEXT_PARAM = "servlet.serverOperation";
  // TODO: Remove this hack
  private static final Set<String> PROPERTY_SET = new HashSet<String>();

  static {
    for (String str : new String[] {
        "id", "version", "displayName", "userName", "purpose", "created"}) {
      PROPERTY_SET.add(str);
    }
  }

  private Config config = null;

  protected Map<String, EntityRecordPair> tokenToEntityRecord;

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {

    initDb(); // temporary place-holder
    ensureConfig();

    RequestDefinition operation = null;
    try {
      response.setStatus(HttpServletResponse.SC_OK);
      PrintWriter writer = response.getWriter();
      JSONObject topLevelJsonObject = new JSONObject(getContent(request));
      String operationName = topLevelJsonObject.getString(RequestDataManager.OPERATION_TOKEN);
      if (operationName.equals(RequestFactory.UPDATE_STRING)) {
        sync(topLevelJsonObject.getString(RequestDataManager.CONTENT_TOKEN),
            writer);
      } else {
        operation = getOperation(operationName);
        Class<?> domainClass = Class.forName(operation.getDomainClassName());
        Method domainMethod = domainClass.getMethod(
            operation.getDomainMethodName(), operation.getParameterTypes());
        if (!Modifier.isStatic(domainMethod.getModifiers())) {
          throw new IllegalArgumentException("the " + domainMethod.getName()
              + " is not static");
        }
        Object args[] = RequestDataManager.getObjectsFromParameterMap(
            getParameterMap(topLevelJsonObject),
            domainMethod.getParameterTypes());
        Object resultList = domainMethod.invoke(null, args);
        if (!(resultList instanceof List<?>)) {
          throw new IllegalArgumentException("return value not a list "
              + resultList);
        }
        JSONArray jsonArray = getJsonArray((List<?>) resultList,
            operation.getReturnType());
        writer.print(jsonArray.toString());
      }
      writer.flush();
      // TODO: clean exception handling code below.
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(e);
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException(e);
    } catch (InvocationTargetException e) {
      throw new IllegalArgumentException(e);
    } catch (SecurityException e) {
      throw new IllegalArgumentException(e);
    } catch (JSONException e) {
      throw new IllegalArgumentException(e);
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException(e);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Allow subclass to initialize database.
   */
  protected void initDb() {
  }

  /**
   * Persist a recordObject of token "recordToken" and return useful information
   * as a JSONObject to return back.
   * <p>
   * Example: recordToken = "Employee", entity = Employee.class, record =
   * EmployeeRecord.class
   *<p>
   * Steps:
   * <ol>
   * <li>assert that each property is present in "EmployeeRecord"
   * <li>invoke "findEmployee (id)" OR new Employee()
   * <li>set various fields on the attached entity and persist OR remove()
   * <li>return data
   * </ol>
   */
  JSONObject updateRecordInDataStore(String recordToken,
      JSONObject recordObject, WriteOperation writeOperation)
      throws SecurityException, NoSuchMethodException, IllegalAccessException,
      InvocationTargetException, JSONException, InstantiationException {

    Class<?> entity = tokenToEntityRecord.get(recordToken).entity;
    Class<? extends Record> record = tokenToEntityRecord.get(recordToken).record;
    Map<String, Class<?>> propertiesInRecord = getPropertiesFromRecord(record);
    validateKeys(recordObject, propertiesInRecord);

    // get entityInstance
    Object entityInstance = getEntityInstance(writeOperation, entity,
        recordObject.getString("id"), propertiesInRecord.get("id"));

    // persist
    if (writeOperation == WriteOperation.DELETE) {
      entity.getMethod("remove").invoke(entityInstance);
    } else {
      Iterator<?> keys = recordObject.keys();
      while (keys.hasNext()) {
        String key = (String) keys.next();
        Object value = recordObject.getString(key);
        Class<?> propertyType = propertiesInRecord.get(key);
        // TODO: hack to work around the GAE integer bug.
        if ("version".equals(key)) {
          propertyType = Long.class;
          value = new Long(value.toString());
        }
        if (writeOperation == WriteOperation.CREATE && ("id".equals(key))) {
          // ignored. id is assigned by default.
        } else {
          entity.getMethod(getMethodNameFromPropertyName(key, "set"),
              propertyType).invoke(entityInstance, value);
        }
      }
      entity.getMethod("persist").invoke(entityInstance);
    }

    // return data back.
    return getReturnRecord(writeOperation, entity, entityInstance, recordObject);
  }

  private Collection<Property<?>> allProperties(Class<? extends Record> clazz) {
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

  @SuppressWarnings("unchecked")
  private void ensureConfig() {
    if (config == null) {
      synchronized (this) {
        if (config != null) {
          return;
        }
        try {
          final String serverOperation = getServletContext().getInitParameter(
              SERVER_OPERATION_CONTEXT_PARAM);
          if (null == serverOperation) {
            failConfig();
          }
          Class<?> clazz = Class.forName(serverOperation);
          if (Config.class.isAssignableFrom(clazz)) {
            config = ((Class<? extends Config>) clazz).newInstance();

            // initialize tokenToEntity map
            tokenToEntityRecord = new HashMap<String, EntityRecordPair>();
            for (Class<? extends Record> recordClass : config.recordTypes()) {
              ServerType serverType = recordClass.getAnnotation(ServerType.class);
              String token = serverType.token();
              if ("[UNASSIGNED]".equals(token)) {
                token = recordClass.getSimpleName();
              }
              tokenToEntityRecord.put(token, new EntityRecordPair(
                  serverType.type(), recordClass));
            }
          }

        } catch (ClassNotFoundException e) {
          failConfig(e);
        } catch (InstantiationException e) {
          failConfig(e);
        } catch (IllegalAccessException e) {
          failConfig(e);
        } catch (SecurityException e) {
          failConfig(e);
        } catch (ClassCastException e) {
          failConfig(e);
        }
      }
    }
  }

  private void failConfig() {
    failConfig(null);
  }

  private void failConfig(Throwable e) {
    final String message = String.format("Context parameter \"%s\" must name "
        + "a default instantiable configuration class implementing %s",
        SERVER_OPERATION_CONTEXT_PARAM, RequestFactory.Config.class.getName());

    throw new IllegalStateException(message, e);
  }

  private String getContent(HttpServletRequest request) throws IOException {
    int contentLength = request.getContentLength();
    byte contentBytes[] = new byte[contentLength];
    BufferedInputStream bis = new BufferedInputStream(request.getInputStream());
    try {
      int readBytes = 0;
      while (bis.read(contentBytes, readBytes, contentLength - readBytes) > 0) {
        // read the contents
      }
      // TODO: encoding issues?
      return new String(contentBytes);
    } finally {
      bis.close();
    }
  }

  private Object getEntityInstance(WriteOperation writeOperation,
      Class<?> entity, String idValue, Class<?> idType)
      throws SecurityException, InstantiationException, IllegalAccessException,
      InvocationTargetException, NoSuchMethodException {

    if (writeOperation == WriteOperation.CREATE) {
      return entity.getConstructor().newInstance();
    }

    // TODO: check "version" validity.
    return entity.getMethod("find" + entity.getSimpleName(), idType).invoke(
        null, idValue);
  }

  /**
   * Converts the returnValue of a 'get' method to a JSONArray.
   *
   * @param resultObject object returned by a 'get' method, must be of type
   *          List<?>
   * @return the JSONArray
   */
  private JSONArray getJsonArray(List<?> resultList,
      Class<? extends Record> entityKeyClass) throws IllegalArgumentException,
      SecurityException, IllegalAccessException, JSONException,
      NoSuchMethodException, InvocationTargetException {
    JSONArray jsonArray = new JSONArray();
    if (resultList.size() == 0) {
      return jsonArray;
    }

    for (Object entityElement : resultList) {
      JSONObject jsonObject = new JSONObject();
      for (Property<?> p : allProperties(entityKeyClass)) {

        if (requestedProperty(p)) {
          String propertyName = p.getName();
          jsonObject.put(propertyName, getPropertyValue(entityElement,
              propertyName));
        }
      }
      jsonArray.put(jsonObject);
    }
    return jsonArray;
  }

  /**
   * Returns methodName corresponding to the propertyName that can be invoked on
   * an entity.
   *
   * Example: "userName" returns prefix + "UserName". "version" returns prefix +
   * "Version"
   */
  private String getMethodNameFromPropertyName(String propertyName,
      String prefix) {
    if (propertyName == null) {
      throw new NullPointerException("propertyName must not be null");
    }

    StringBuffer methodName = new StringBuffer(prefix);
    methodName.append(propertyName.substring(0, 1).toUpperCase());
    methodName.append(propertyName.substring(1));
    return methodName.toString();
  }

  private RequestDefinition getOperation(String operationName) {
    RequestDefinition operation;
    operation = config.requestDefinitions().get(operationName);
    if (null == operation) {
      throw new IllegalArgumentException("Unknown operation " + operationName);
    }
    return operation;
  }

  /**
   * @param jsonObject
   * @return
   * @throws JSONException
   */
  private Map<String, String> getParameterMap(JSONObject jsonObject)
      throws JSONException {
    Map<String, String> parameterMap = new HashMap<String, String>();
    Iterator<?> keys = jsonObject.keys();
    while (keys.hasNext()) {
      String key = keys.next().toString();
      if (key.startsWith(RequestDataManager.PARAM_TOKEN)) {
        parameterMap.put(key, jsonObject.getString(key));
      }
    }
    return parameterMap;
  }

  /**
   * Returns the property fields (name => type) for a record.
   */
  private Map<String, Class<?>> getPropertiesFromRecord(
      Class<? extends Record> record) throws SecurityException,
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
   * @param entityElement
   * @param property
   * @return
   */
  private Object getPropertyValue(Object entityElement, String propertyName)
      throws SecurityException, NoSuchMethodException, IllegalAccessException,
      InvocationTargetException {
    String methodName = getMethodNameFromPropertyName(propertyName, "get");
    Method method = entityElement.getClass().getMethod(methodName);
    Object returnValue = method.invoke(entityElement);
    /*
     * TODO: make these conventions more prominent. 1. encoding long as String
     * 2. encoding Date as Double
     */
    if (returnValue instanceof java.lang.Long) {
      return returnValue.toString();
    }
    if (returnValue instanceof java.util.Date) {
      return new Double(((java.util.Date) returnValue).getTime());
    }
    return returnValue;
  }

  private JSONObject getReturnRecord(WriteOperation writeOperation,
      Class<?> entity, Object entityInstance, JSONObject recordObject)
      throws SecurityException, JSONException, IllegalAccessException,
      InvocationTargetException, NoSuchMethodException {

    JSONObject returnObject = new JSONObject();
    returnObject.put("id", entity.getMethod("getId").invoke(entityInstance));
    returnObject.put("version", entity.getMethod("getVersion").invoke(
        entityInstance));
    if (writeOperation == WriteOperation.CREATE) {
      returnObject.put("futureId", recordObject.getString("id"));
    }
    return returnObject;
  }

  /**
   * returns true if the property has been requested. TODO: fix this hack.
   *
   * @param p the field of entity ref
   * @return has the property value been requested
   */
  private boolean requestedProperty(Property<?> p) {
    return PROPERTY_SET.contains(p.getName());
  }

  private void sync(String content, PrintWriter writer)
      throws SecurityException, NoSuchMethodException, IllegalAccessException,
      InvocationTargetException, InstantiationException {

    try {
      JSONObject jsonObject = new JSONObject(content);
      JSONObject returnJsonObject = new JSONObject();
      for (WriteOperation writeOperation : WriteOperation.values()) {
        if (!jsonObject.has(writeOperation.name())) {
          continue;
        }
        JSONArray reportArray = new JSONArray(
            jsonObject.getString(writeOperation.name()));
        JSONArray returnArray = new JSONArray();

        int length = reportArray.length();
        if (length == 0) {
          throw new IllegalArgumentException("No json array for "
              + writeOperation.name() + " should have been sent");
        }
        for (int i = 0; i < length; i++) {
          JSONObject recordWithSchema = reportArray.getJSONObject(i);
          // iterator has just one element.
          Iterator<?> iterator = recordWithSchema.keys();
          iterator.hasNext();
          String recordToken = (String) iterator.next();
          JSONObject recordObject = recordWithSchema.getJSONObject(recordToken);
          JSONObject returnObject = updateRecordInDataStore(recordToken,
              recordObject, writeOperation);
          returnArray.put(returnObject);
          if (iterator.hasNext()) {
            throw new IllegalArgumentException(
                "There cannot be more than one record token");
          }
        }
        returnJsonObject.put(writeOperation.name(), returnArray);
      }
      writer.print(returnJsonObject.toString());
    } catch (JSONException e) {
      throw new IllegalArgumentException("sync failed: ", e);
    }
  }

  private void validateKeys(JSONObject recordObject,
      Map<String, Class<?>> declaredProperties) {
    Iterator<?> keys = recordObject.keys();
    while (keys.hasNext()) {
      String key = (String) keys.next();
      if (declaredProperties.get(key) == null) {
        throw new IllegalArgumentException("key " + key
            + " is not permitted to be set");
      }
    }
  }
}
