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

import com.google.gwt.sample.expenses.gen.MethodName;
import com.google.gwt.sample.expenses.gen.UrlParameterManager;
import com.google.gwt.sample.expenses.server.domain.Report;
import com.google.gwt.sample.expenses.server.domain.Storage;

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
import java.util.HashSet;
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
        "ID", "VERSION", "DISPLAY_NAME", "USER_NAME", "PURPOSE", "CREATED"}) {
      PROPERTY_SET.add(str);
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {

    response.setStatus(HttpServletResponse.SC_OK);
    PrintWriter writer = response.getWriter();
    MethodName operation = getMethodName(request.getParameter("methodName"));
    try {
      Class<?> classOperation = Class.forName("com.google.gwt.sample.expenses.server.domain."
          + operation.getClassName());
      Method methodOperation = null;
      // TODO: check if method names must be unique in a class.
      for (Method method : classOperation.getDeclaredMethods()) {
        if (method.getName().equals(operation.getMethodName())) {
          methodOperation = method;
          break;
        }
      }
      if (methodOperation == null) {
        throw new IllegalArgumentException("unable to find "
            + operation.getMethodName() + " in " + classOperation);
      }
      if (!Modifier.isStatic(methodOperation.getModifiers())) {
        throw new IllegalArgumentException("the " + methodOperation.getName()
            + " is not static");
      }
      Map<String, String[]> parameterMap = request.getParameterMap();
      Object args[] = UrlParameterManager.getObjectsFromFragment(parameterMap,
          methodOperation.getParameterTypes());
      Object resultList = methodOperation.invoke(null, args);
      if (!(resultList instanceof List)) {
        throw new IllegalArgumentException("return value not a list "
            + resultList);
      }
      JSONArray jsonArray = getJsonArray((List<?>) resultList);
      writer.print(jsonArray.toString());
      writer.flush();
      // TODO: clean exception handling code below.
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("unable to load the class: "
          + operation);
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

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {

    response.setStatus(HttpServletResponse.SC_OK);
    MethodName methodName = getMethodName(request.getParameter("methodName"));
    PrintWriter writer = response.getWriter();
    switch (methodName) {
      case SYNC:
        sync(request, writer);
        break;
      default:
        System.err.println("POST: unknown method " + methodName);
        break;
    }
    writer.flush();
  }

  /**
   * Converts the returnValue of a 'get' method to a JSONArray.
   * 
   * @param resultObject object returned by a 'get' method, must be of type
   *          List<? extends Entity>
   * @return the JSONArray
   * @throws ClassNotFoundException
   * @throws InvocationTargetException
   * @throws IllegalAccessException
   * @throws NoSuchMethodException
   * @throws JSONException
   * @throws SecurityException
   */
  private JSONArray getJsonArray(List<?> resultList)
      throws ClassNotFoundException, SecurityException, JSONException,
      NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    JSONArray jsonArray = new JSONArray();
    if (resultList.size() == 0) {
      return jsonArray;
    }
    Object firstElement = resultList.get(0);
    Class<?> entityClass = firstElement.getClass();
    Class<?> entityRefClass = Class.forName("com.google.gwt.sample.expenses.shared."
        + entityClass.getSimpleName() + "Ref");
    for (Object entityElement : resultList) {
      JSONObject jsonObject = new JSONObject();
      for (Field field : entityRefClass.getFields()) {
        // TODO: perhaps get the property names from javax.persistence.Id fields
        if (isProperty(field) && requestedProperty(field)) {
          String propertyName = getPropertyName(field);
          jsonObject.put(propertyName, getPropertyValue(entityElement,
              propertyName));
        }
      }
      jsonArray.put(jsonObject);
    }
    return jsonArray;
  }

  /**
   * @param request
   * @return
   */
  private MethodName getMethodName(String methodString) {
    for (MethodName method : MethodName.values()) {
      if (method.name().equals(methodString)) {
        return method;
      }
    }
    throw new IllegalArgumentException("unknown methodName: " + methodString);
  }

  /**
   * Returns methodName corresponding to the propertyName that can be invoked on
   * an {@link Entity} object.
   *
   * Example: "USER_NAME" returns "getUserName". "VERSION" returns "getVersion"
   */
  private String getMethodNameFromPropertyName(String propertyName) {
    assert propertyName != null;
    assert propertyName.equals(propertyName.toUpperCase());
    StringBuffer methodName = new StringBuffer("get");
    int index = 0;
    int length = propertyName.length();
    while (index < length) {
      int underscore = propertyName.indexOf('_', index);
      if (underscore == -1) {
        underscore = length;
      }
      methodName.append(propertyName.charAt(index));
      methodName.append(propertyName.substring(index + 1, underscore).toLowerCase());
      index = underscore + 1; // skip the '_' character.
    }
    return methodName.toString();
  }

  /**
   * returns the property name.
   */
  private String getPropertyName(Field field) {
    return field.getName();
  }

  /**
   * @param entityElement
   * @param property
   * @return
   * @throws NoSuchMethodException
   * @throws SecurityException
   * @throws InvocationTargetException
   * @throws IllegalAccessException
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
   * @param field
   * @return
   */
  private boolean isProperty(Field field) {
    int modifiers = field.getModifiers();
    if (!Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers)
        || !Modifier.isPublic(modifiers)) {
      return false;
    }
    return "Property".equals(field.getType().getSimpleName());
  }

  /**
   * returns true if the property has been requested. TODO: fix this hack.
   *
   * @param field the field of entity ref
   * @return has the property value been requested
   */
  private boolean requestedProperty(Field field) {
    return PROPERTY_SET.contains(field.getName());
  }

  /**
   * @param request
   * @param writer
   * @throws IOException
   */
  private void sync(HttpServletRequest request, PrintWriter writer)
      throws IOException {
    int contentLength = request.getContentLength();
    byte contentBytes[] = new byte[contentLength];
    BufferedInputStream bis = new BufferedInputStream(request.getInputStream());
    int readBytes = 0;
    while (bis.read(contentBytes, readBytes, contentLength - readBytes) > 0) {
      // read the contents
    }
    // TODO: encoding issues?
    String content = new String(contentBytes);
    try {
      JSONArray reportArray = new JSONArray(content);
      int length = reportArray.length();
      if (length > 0) {
        JSONObject report = reportArray.getJSONObject(0);
        Report r = Report.findReport(report.getLong(com.google.gwt.sample.expenses.shared.ReportRef.ID.getName()));
        r.setPurpose(report.getString(com.google.gwt.sample.expenses.shared.ReportRef.PURPOSE.getName()));
        r = Storage.INSTANCE.persist(r);
        report.put(
            com.google.gwt.sample.expenses.shared.ReportRef.VERSION.getName(),
            r.getVersion());
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
