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
package com.google.gwt.requestfactory.client.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.ValueCodex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Analogous to {@link ValueCodex}, but for object types.
 */
public class EntityCodex {
  /**
   * Collection support is limited to value types and resolving ids.
   */
  public static Object decode(Class<?> type, Class<?> elementType,
      final AbstractRequestContext requestContext, Object jso) {
    if (jso == null) {
      return null;
    }

    // Collection support
    if (elementType != null) {
      Collection<Object> collection = null;
      if (List.class == type) {
        collection = new ArrayList<Object>();
      } else if (Set.class == type) {
        collection = new HashSet<Object>();
      }

      // OK, here's a Java typesystem wart, we need an JsAny type
      if (ValueCodex.canDecode(elementType)) {
        @SuppressWarnings("unchecked")
        JsArray<JavaScriptObject> array = (JsArray<JavaScriptObject>) jso;
        for (int i = 0, j = array.length(); i < j; i++) {
          if (isNull(array, i)) {
            collection.add(null);
          } else {
            // Use getString() to make DevMode not complain about primitives
            Object element = ValueCodex.convertFromString(elementType,
                getString(array, i));
            collection.add(element);
          }
        }
      } else {
        JsArrayString array = (JsArrayString) jso;
        for (int i = 0, j = array.length(); i < j; i++) {
          Object element = decode(elementType, null, requestContext,
              array.get(i));
          collection.add(element);
        }
      }
      return collection;
    }

    // Really want EntityProxy.isAssignableFrom(type)
    if (requestContext.getRequestFactory().getTypeToken(type) != null) {
      EntityProxyId<?> id = requestContext.getRequestFactory().getProxyId(
          (String) jso);
      return requestContext.getSeenEntityProxy((SimpleEntityProxyId<?>) id);
    }

    // Fall back to values
    return ValueCodex.convertFromString(type, String.valueOf(jso));
  }

  /**
   * Create a wire-format representation of an object.
   */
  public static String encodeForJsonPayload(Object value) {
    if (value == null) {
      return "null";
    }

    if (value instanceof Iterable<?>) {
      StringBuffer toReturn = new StringBuffer();
      toReturn.append('[');
      boolean first = true;
      for (Object val : ((Iterable<?>) value)) {
        if (!first) {
          toReturn.append(',');
        } else {
          first = false;
        }
        toReturn.append(encodeForJsonPayload(val));
      }
      toReturn.append(']');
      return toReturn.toString();
    }

    if (value instanceof EntityProxy) {
      String historyToken = AbstractRequestFactory.getHistoryToken((EntityProxy) value);
      return "\"" + historyToken + "\"";
    }

    return ValueCodex.encodeForJsonPayload(value);
  }

  private static native String getString(JavaScriptObject array, int index) /*-{
    return String(array[index]);
  }-*/;

  /**
   * Looks for an explicit null value.
   */
  private static native boolean isNull(JavaScriptObject array, int index) /*-{
    return array[index] === null;
  }-*/;
}
