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
import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.requestfactory.shared.impl.CollectionProperty;
import com.google.gwt.requestfactory.shared.impl.Property;
import com.google.gwt.requestfactory.shared.impl.TypeLibrary;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractList;
import java.util.Date;

/**
 * List backed by a JSON Array.
 */
class JsoList<T> extends AbstractList<T> implements JsoCollection {

  static native Date dateForDouble(double millis) /*-{
    return @java.util.Date::createFrom(D)(millis);
  }-*/;

  private RequestFactoryJsonImpl rf;

  private final JavaScriptObject array;

  private DeltaValueStoreJsonImpl deltaValueStore;

  private CollectionProperty property;

  private ProxyImpl record;

  public JsoList(RequestFactoryJsonImpl rf, JavaScriptObject array) {
    this.rf = rf;
    this.array = array;
  }

  @Override
  public void add(int i, T o) {
    Object v = ProxyJsoImpl.encodeToJsType(o);
    if (v instanceof Double) {
      ProxyJsoImpl.splice(array, i, 0, ((Double) v).doubleValue());
    } else if (v instanceof Boolean) {
      ProxyJsoImpl.splice(array, i, 0, ((Boolean) v).booleanValue());
    } else {
      ProxyJsoImpl.splice(array, i, 0, v);
    }
    dvsUpdate();
  }

  public JavaScriptObject asJso() {
    return array;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T get(int i) {
    return get0(i);
  }

  @SuppressWarnings("unchecked")
  public final <V> V get(JavaScriptObject jso, int i, Class<?> type) {
    if (isNullOrUndefined(jso, i)) {
      return null;
    }

    try {
      if (Boolean.class.equals(type)) {
        return (V) Boolean.valueOf(getBoolean(jso, i));
      }
      if (Character.class.equals(type)) {
        return (V) Character.valueOf(String.valueOf(get(jso, i)).charAt(0));
      }
      if (Byte.class.equals(type)) {
        return (V) Byte.valueOf((byte) getInt(jso, i));
      }
      if (Short.class.equals(type)) {
        return (V) Short.valueOf((short) getInt(jso, i));
      }
      if (Float.class.equals(type)) {
        return (V) Float.valueOf((float) getDouble(jso, i));
      }
      if (BigInteger.class.equals(type)) {
        return (V) new BigDecimal((String) get(jso, i)).toBigInteger();
      }
      if (BigDecimal.class.equals(type)) {
        return (V) new BigDecimal((String) get(jso, i));
      }
      if (Integer.class.equals(type)) {
        return (V) Integer.valueOf(getInt(jso, i));
      }
      if (Long.class.equals(type)) {
        return (V) Long.valueOf((String) get(jso, i));
      }
      if (Double.class.equals(type)) {
        if (!isDefined(jso, i)) {
          return (V) new Double(0.0);
        }
        return (V) Double.valueOf(getDouble(jso, i));
      }
      if (Date.class.equals(type)) {
        double millis = new Date().getTime();
        if (isDefined(jso, i)) {
          millis = Double.parseDouble((String) get(jso, i));
        }
        if (GWT.isScript()) {
          return (V) dateForDouble(millis);
        } else {
          // In dev mode, we're using real JRE dates
          return (V) new Date((long) millis);
        }
      }
    } catch (final Exception ex) {
      throw new IllegalStateException(
          "Index " + i + " has invalid " + " value " + get(jso, i)
              + " for type " + type);
    }

    if (type.isEnum()) {
      // TODO: Can't we just use Enum.valueOf()?
      Enum<?>[] values = (Enum[]) type.getEnumConstants();
      int ordinal = getInt(jso, i);
      for (Enum<?> value : values) {
        if (ordinal == value.ordinal()) {
          return (V) value;
        }
      }
    }

    if (String.class == type) {
      return (V) get(jso, i);
    }
    return null;
  }

  /**
   * @param name
   */
  public final native boolean isDefined(JavaScriptObject jso, int i)/*-{
        return jso[i] !== undefined;
    }-*/;

  /**
   * @param name
   */
  public final native boolean isNullOrUndefined(JavaScriptObject jso, int i)/*-{
        return jso[i] == null;
    }-*/;

  @Override
  public T remove(int i) {
    T old = get(i);
    ProxyJsoImpl.splice(array, i, 1);
    dvsUpdate();
    return old;
  }

  @Override
  public T set(int i, T o) {
    T old = get(i);
    Object v = ProxyJsoImpl.encodeToJsType(o);
    if (v instanceof String) {
      ((JsArrayString) array).set(i, v.toString());
    } else if (v instanceof Double) {
      ((JsArrayNumber) array).set(i, (Double) v);
    } else if (v instanceof Boolean) {
      setBoolean(array, i, (Boolean) v);
    } else {
      ((JsArrayString) array).set(i, null);
    }
    dvsUpdate();
    return old;
  }

  public void setDependencies(DeltaValueStoreJsonImpl dvs, Property property,
      ProxyImpl proxy) {
    this.deltaValueStore = dvs;
    this.property = (CollectionProperty) property;
    this.record = proxy;
  }

  @Override
  public int size() {
    return ((JsArrayString) array).length();
  }

  private void dvsUpdate() {
    if (deltaValueStore != null) {
      deltaValueStore.set(property, record, this);
    }
  }

  private native Object get(JavaScriptObject jso, int i) /*-{
        return jso[i];
    }-*/;

  private T get0(int i) {
    if (TypeLibrary.isProxyType(property.getLeafType())) {
      String key[] = ((JsArrayString) array).get(i).split("-", 3);
      return (T) rf.getValueStore().getRecordBySchemaAndId(rf.getSchema(key[2]),
          key[0], rf);
    } else {
      return (T) get(array, i, property.getLeafType());
    }
  }

  private native boolean getBoolean(JavaScriptObject jso, int i) /*-{
        return jso[i];
    }-*/;

  private native double getDouble(JavaScriptObject jso, int i) /*-{
        return jso[i];
    }-*/;

  private native int getInt(JavaScriptObject jso, int i) /*-{
        return jso[i];
    }-*/;

  private native int setBoolean(JavaScriptObject jso, int i, boolean b) /*-{
        jso[i] = b;
    }-*/;
}
