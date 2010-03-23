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
package com.google.gwt.sample.expenses.server;

import com.google.gwt.requestfactory.shared.EntityKey;
import com.google.gwt.requestfactory.shared.impl.RequestDataManager;
import com.google.gwt.sample.expenses.server.domain.Report;
import com.google.gwt.sample.expenses.server.domain.Storage;
import com.google.gwt.sample.expenses.shared.ExpenseRequestFactory.ServerSideOperation;
import com.google.gwt.sample.expenses.shared.ReportKey;
import com.google.gwt.valuestore.shared.Property;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
 *
 */
public class ExpensesDataServlet extends HttpServlet {

  // TODO: Remove this hack
  private static final Set<String> PROPERTY_SET = new HashSet<String>();
  static {
    for (String str : new String[] {
        "id", "version", "displayName", "userName", "purpose", "created"}) {
      PROPERTY_SET.add(str);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {

    ServerSideOperation operation = null;
    try {
      response.setStatus(HttpServletResponse.SC_OK);
      JSONObject topLevelJsonObject = new JSONObject(getContent(request));
      operation = getOperationFromName(topLevelJsonObject.getString(RequestDataManager.OPERATION_TOKEN));
      PrintWriter writer = response.getWriter();
      switch (operation) {
        case SYNC:
          sync(topLevelJsonObject.getString(RequestDataManager.CONTENT_TOKEN),
              writer);
          break;
        case FIND_ALL_EMPLOYEES:
        case FIND_ALL_REPORTS:
        case FIND_EMPLOYEE:
        case FIND_REPORTS_BY_EMPLOYEE:
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
          if (!(resultList instanceof List)) {
            throw new IllegalArgumentException("return value not a list "
                + resultList);
          }
          JSONArray jsonArray = getJsonArray((List<?>) resultList,
              operation.getReturnType());
          writer.print(jsonArray.toString());
          break;
        default:
          throw new IllegalArgumentException("Unknow operation " + operation);
      }
      writer.flush();
      // TODO: clean exception handling code below.
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("unable to load the class: "
          + operation.getDomainClassName());
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
    }
  }

  private String getContent(HttpServletRequest request) throws IOException {
    int contentLength = request.getContentLength();
    byte contentBytes[] = new byte[contentLength];
    BufferedInputStream bis = new BufferedInputStream(request.getInputStream());
    int readBytes = 0;
    while (bis.read(contentBytes, readBytes, contentLength - readBytes) > 0) {
      // read the contents
    }
    // TODO: encoding issues?
    return new String(contentBytes);
  }

  /**
   * Converts the returnValue of a 'get' method to a JSONArray.
   * 
   * @param resultObject object returned by a 'get' method, must be of type
   *          List<?>
   * @return the JSONArray
   */
  private JSONArray getJsonArray(List<?> resultList,
      Class<? extends EntityKey<?>> entityKeyClass) throws JSONException,
      NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    JSONArray jsonArray = new JSONArray();
    if (resultList.size() == 0) {
      return jsonArray;
    }
    EntityKey<?> key = (EntityKey<?>) entityKeyClass.getMethod("get").invoke(
        null);
    for (Object entityElement : resultList) {
      JSONObject jsonObject = new JSONObject();
      for (Property<?, ?> p : key.getProperties()) {

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
   * an {@link Entity} object.
   * 
   * Example: "userName" returns "getUserName". "version" returns "getVersion"
   */
  private String getMethodNameFromPropertyName(String propertyName) {
    if (propertyName == null) {
      throw new NullPointerException("propertyName must not be null");
    }

    StringBuffer methodName = new StringBuffer("get");
    methodName.append(propertyName.substring(0, 1).toUpperCase());
    methodName.append(propertyName.substring(1));
    return methodName.toString();
  }

  private ServerSideOperation getOperationFromName(String operationName) {
    for (ServerSideOperation operation : ServerSideOperation.values()) {
      if (operation.name().equals(operationName)) {
        return operation;
      }
    }
    throw new IllegalArgumentException("Unknown operation " + operationName);
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
   * @param entityElement
   * @param property
   * @return
   */
  private Object getPropertyValue(Object entityElement, String propertyName)
      throws SecurityException, NoSuchMethodException, IllegalAccessException,
      InvocationTargetException {
    String methodName = getMethodNameFromPropertyName(propertyName);
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

  /**
   * returns true if the property has been requested. TODO: fix this hack.
   * 
   * @param p the field of entity ref
   * @return has the property value been requested
   */
  private boolean requestedProperty(Property<?, ?> p) {
    return PROPERTY_SET.contains(p.getName());
  }

  /**
   * @throws IOException
   */
  private void sync(String content, PrintWriter writer) throws IOException {

    try {
      JSONArray reportArray = new JSONArray(content);
      int length = reportArray.length();
      if (length > 0) {
        JSONObject report = reportArray.getJSONObject(0);
        Report r = Report.findReport(report.getLong(ReportKey.get().getId().getName()));
        r.setPurpose(report.getString(ReportKey.get().getPurpose().getName()));
        r = Storage.INSTANCE.persist(r);
        report.put(ReportKey.get().getVersion().getName(), r.getVersion());
        JSONArray returnArray = new JSONArray();
        // TODO: don't echo back everything.
        returnArray.put(report);
        writer.print(returnArray.toString());
      }
    } catch (JSONException e) {
      e.printStackTrace();
      // TODO: return an error.
    }
    return;
  }
}
