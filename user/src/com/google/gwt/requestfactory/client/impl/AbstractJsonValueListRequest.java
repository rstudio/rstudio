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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.requestfactory.shared.Request;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

/**
 * <p> <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span> </p> Abstract implementation of {@link com.google.gwt.requestfactory.shared.RequestObject
 * RequestFactory.RequestObject} for requests that return lists of {@link
 * com.google.gwt.requestfactory.shared.EntityProxy}.
 *
 * @param <T> the type of entities returned
 */
public abstract class //
    AbstractJsonValueListRequest<T> //
    extends AbstractRequest<Collection<T>, AbstractJsonValueListRequest<T>>
    implements Request<Collection<T>> {

  static Object decodeValueType(Class<?> valueType, String value,
      Enum[] enumValues) {
    try {
      if (Boolean.class == valueType) {
        return Boolean.valueOf(value);
      }
      if (Character.class == valueType) {
        return value.charAt(0);
      }
      if (Byte.class == valueType) {
        return Byte.valueOf(value);
      }
      if (Short.class == valueType) {
        return Short.valueOf(value);
      }
      if (Float.class == valueType) {
        return Float.valueOf(value);
      }
      if (BigInteger.class == valueType) {
        return new BigDecimal(value).toBigInteger();
      }
      if (BigDecimal.class == valueType) {
        return new BigDecimal(value);
      }
      if (Integer.class == valueType) {
        return Integer.valueOf(value);
      }
      if (Long.class == valueType) {
        return Long.valueOf(value);
      }
      if (Double.class == valueType) {
        return Double.valueOf(value);
      }
      if (Date.class == valueType) {
        double millis = new Date().getTime();
        millis = Double.parseDouble(value);

        if (GWT.isScript()) {
          return ProxyJsoImpl.dateForDouble(millis);
        } else {
          // In dev mode, we're using real JRE dates
          return new Date((long) millis);
        }
      }
    } catch (final Exception ex) {
      throw new IllegalStateException(
          "Value  " + value + " cannot be converted to  " + valueType);
    }

    if (Enum.class == valueType) {
      int ordinal = Integer.parseInt(value);
      for (Enum<?> evalue : enumValues) {
        if (ordinal == evalue.ordinal()) {
          return value;
        }
      }
    }

    if (String.class == valueType) {
      return value;
    }
    return null;
  }

  private boolean isSet;

  private Class<?> leafType;

  private Enum[] enumValues;

  public AbstractJsonValueListRequest(RequestFactoryJsonImpl requestService,
      boolean isSet, Class<?> leafType, Enum[] enumValues) {
    super(requestService);
    this.isSet = isSet;
    this.leafType = leafType;
    this.enumValues = enumValues;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void handleResult(Object jsoResult) {
    JsArray<JavaScriptObject> rawJsos = (JsArray<JavaScriptObject>) jsoResult;

    Collection<T> values = isSet ? new HashSet<T>() : new ArrayList<T>();
    for (int i = 0; i < rawJsos.length(); i++) {
      values.add(
          (T) decodeValueType(leafType, getString(rawJsos, i), enumValues));
    }
    succeed(values);
  }

  @Override
  protected AbstractJsonValueListRequest getThis() {
    return this;
  }

  private native String getString(JavaScriptObject array, int index) /*-{
    return String(array[index]);
  }-*/;
}
