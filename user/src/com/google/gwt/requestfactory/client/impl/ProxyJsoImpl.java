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
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.impl.Property;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * JSO implementation of {@link EntityProxy}, used to back subclasses of
 * {@link ProxyImpl}.
 */
public class ProxyJsoImpl extends JavaScriptObject implements EntityProxy {

  public static ProxyJsoImpl create(JavaScriptObject rawJsoWithIdAndVersion,
      ProxySchema<?> schema, RequestFactoryJsonImpl requestFactory) {

    ProxyJsoImpl rtn = rawJsoWithIdAndVersion.cast();
    assert rtn.getId() != null;
    assert rtn.getVersion() != null;

    rtn.setSchema(schema);
    rtn.setRequestFactory(requestFactory);
    return rtn;
  };

  public static JsArray<ProxyJsoImpl> create(
      JsArray<JavaScriptObject> rawJsos, ProxySchema<?> schema,
      RequestFactoryJsonImpl requestFactory) {
    
    for (int i = 0; i < rawJsos.length(); i++) {
      ProxyJsoImpl.create(rawJsos.get(i), schema, requestFactory);
    }
    
    return rawJsos.cast();
  }

  public static ProxyJsoImpl create(String id, Integer version,
      ProxySchema<?> schema, RequestFactoryJsonImpl requestFactory) {
    ProxyJsoImpl rtn = createEmpty();
    rtn.set(ProxyImpl.id, id);
    rtn.set(ProxyImpl.version, version);
    return create(rtn, schema, requestFactory);
  }

  public static ProxyJsoImpl emptyCopy(ProxyJsoImpl jso) {
    String tempId = jso.get(ProxyImpl.id);
    Integer tempVersion = jso.get(ProxyImpl.version);
    ProxySchema<?> schema = jso.getSchema();

    ProxyJsoImpl copy = create(tempId, tempVersion, schema, jso.getRequestFactory());
    return copy;
  }

  /** 
   * Create an empty JSO, unsafe to return.
   */
  private static native ProxyJsoImpl createEmpty() /*-{
    return {};
  }-*/;

  protected ProxyJsoImpl() {
  }

  public final native void delete(String name)/*-{
    delete this[name];
  }-*/;

  public final <V> V get(Property<V> property) {
    String name = property.getName();
    Class<V> type = property.getType();
    
    // javac 1.6.0_20 on mac has problems without the explicit parameterization
    return this.<V> get(name, type);
  }

  @SuppressWarnings("unchecked")
  public final <V> V get(String name, Class<?> type) {
    if (isNullOrUndefined(name)) {
      return null;
    }
    
    try {
      if (Boolean.class.equals(type)) {
        return (V) Boolean.valueOf(getBoolean(name));
      }
      if (Character.class.equals(type)) {
        return (V) Character.valueOf(String.valueOf(get(name)).charAt(
            0));
      }
      if (Byte.class.equals(type)) {
        return (V) Byte.valueOf((byte) getInt(name));
      }
      if (Short.class.equals(type)) {
        return (V) Short.valueOf((short) getInt(name));
      }
      if (Float.class.equals(type)) {
        return (V) Float.valueOf((float) getDouble(name));
      }
      if (BigInteger.class.equals(type)) {
        return (V) new BigDecimal((String) get(name)).toBigInteger();
      }
      if (BigDecimal.class.equals(type)) {
        return (V) new BigDecimal((String) get(name));
      }
      if (Integer.class.equals(type)) {
        return (V) Integer.valueOf(getInt(name));
      }
      if (Long.class.equals(type)) {
        return (V) Long.valueOf((String) get(name));
      }
      if (Double.class.equals(type)) {
        if (!isDefined(name)) {
          return (V) new Double(0.0);
        }
        return (V) Double.valueOf(getDouble(name));
      }
      if (Date.class.equals(type)) {
        double millis = new Date().getTime();
        if (isDefined(name)) {
          millis = Double.parseDouble((String) get(name));
        }
        if (GWT.isScript()) {
          return (V) dateForDouble(millis);
        } else {
          // In dev mode, we're using real JRE dates
          return (V) new Date((long) millis);
        }
      }
    } catch (final Exception ex) {
      throw new IllegalStateException("Property  " + name
          + " has invalid " + " value " + get(name)
          + " for type " + type);
    }

    if (type.isEnum()) {
      // TODO: Can't we just use Enum.valueOf()?
      Enum<?>[] values = (Enum[]) type.getEnumConstants();
      int ordinal = getInt(name);
      for (Enum<?> value : values) {
        if (ordinal == value.ordinal()) {
          return (V) value;
        }
      }
    }

    if (String.class == type) {
      return (V) get(name);
    }
    // at this point, we checked all the known primitive/value types we support
    // TODO: handle embedded types, List, Set, Map

    // else, it must be a record type
    String relatedId = (String) get(name);
    if (relatedId == null) {
      return null;
    } else {
      // TODO: should cache this or modify JSO field in place
      String schemaAndId[] = relatedId.split("-");
      assert schemaAndId.length == 2;
      ProxySchema<?> schema = getRequestFactory().getSchema(schemaAndId[0]);
      return (V) getRequestFactory().getValueStore().getRecordBySchemaAndId(schema,
          schemaAndId[1], getRequestFactory());
    }
  }

  public final native <T> T get(String propertyName) /*-{
    return this[propertyName] || null;
  }-*/;

  public final String getId() {
    return this.get(ProxyImpl.id);
  }

  public final native RequestFactoryJsonImpl getRequestFactory() /*-{
    return this['__rf'];
  }-*/;

  public final native ProxySchema<?> getSchema() /*-{
    return this['__key'];
  }-*/;
  
  public final Integer getVersion() {
    return this.get(ProxyImpl.version);
  }

  /**
   * @param name
   */
  public final native boolean isDefined(String name)/*-{
    return this[name] !== undefined;
  }-*/;

  public final boolean isEmpty() {
    for (Property<?> property : getSchema().allProperties()) {
      if ((property != ProxyImpl.id) && (property != ProxyImpl.version)
          && (isDefined(property.getName()))) {
        return false;
      }
    }
    return true;
  }

  /**
   * @param name
   */
  public final native boolean isNullOrUndefined(String name)/*-{
    return this[name] == null;
  }-*/;

  public final boolean merge(ProxyJsoImpl from) {
    assert getSchema() == from.getSchema();

    boolean changed = false;
    for (Property<?> property : getSchema().allProperties()) {
      if (from.isDefined(property.getName())) {
        changed |= copyPropertyIfDifferent(property.getName(), from);
      }
    }

    return changed;
  }

  public final <V> void set(Property<V> property, V value) {
    if (value == null) {
      setNull(property.getName());
      return;
    }

    if (value instanceof String) {
      setString(property.getName(), (String) value);
      return;
    }
    if (value instanceof Character) {
      setString(property.getName(), String.valueOf(value));
      return;
    }

    if (value instanceof Long || value instanceof BigDecimal
        || value instanceof BigInteger) {
      setString(property.getName(), String.valueOf(value));
      return;
    }

    if (value instanceof Integer || value instanceof Short
        || value instanceof Byte) {
      setInt(property.getName(), ((Number) value).intValue());
      return;
    }

    if (value instanceof Date) {
      long millis = ((Date) value).getTime();
      setString(property.getName(), String.valueOf(millis));
      return;
    }
    if (value instanceof Double || value instanceof Float) {
      setDouble(property.getName(), ((Number) value).doubleValue());
      return;
    }

    if (value instanceof Enum<?>) {
      setInt(property.getName(), ((Enum<?>) value).ordinal());
      return;
    }

    if (value instanceof Boolean) {
      setBoolean(property.getName(), ((Boolean) value).booleanValue());
      return;
    }

    if (value instanceof ProxyImpl) {
      setString(property.getName(), ((ProxyImpl) value).getWireFormatId());
      return;
    }

    throw new UnsupportedOperationException("Cannot set properties of type "
        + value.getClass().getName());
  }

  public final EntityProxyId stableId() {
    throw new IllegalArgumentException("Can't call stableId on the jso");
  }

  /**
   * Return JSON representation using org.json library.
   * 
   * @return returned string.
   */
  public final native String toJson() /*-{
    // Safari 4.0.5 appears not to honor the replacer argument, so we can't do this:

    //    var replacer = function(key, value) {
    //      if (key == '__key') {
    //        return;
    //      }
    //      return value;
    //    }
    // return $wnd.JSON.stringify(this, replacer);

    var key = this.__key;
    delete this.__key;
    var rf = this.__rf;
    delete this.__rf;
    var gwt = this.__gwt_ObjectId;
    delete this.__gwt_ObjectId;
    // TODO verify that the stringify() from json2.js works on IE
    var rtn = $wnd.JSON.stringify(this);
    this.__key = key;
    this.__rf = rf;
    this.__gwt_ObjectId = gwt;
    return rtn;
  }-*/;

  final boolean hasChanged(ProxyJsoImpl newJso) {
    assert getSchema() == newJso.getSchema();
    for (Property<?> property : getSchema().allProperties()) {
      if (newJso.isDefined(property.getName())) {
        if (isDefined(property.getName())) {
          if (hasValueChanged(property.getName(), newJso)) {
            return true;
          }
        } else {
          return true;
        }
      }
    }
    return false;
  }

  private native boolean copyPropertyIfDifferent(String name, ProxyJsoImpl from) /*-{
    if (this[name] == from[name]) {
      return false;
    }
    this[name] = from[name];
    return true;
  }-*/;

  private native Date dateForDouble(double millis) /*-{
    return @java.util.Date::createFrom(D)(millis);
  }-*/;

  private native boolean getBoolean(String name) /*-{
    return this[name];
  }-*/;

  private native double getDouble(String name) /*-{
    return this[name];
  }-*/;

  private native int getInt(String name) /*-{
    return this[name];
  }-*/;

  private native boolean hasValueChanged(String name, ProxyJsoImpl from) /*-{
    if (this[name] == from[name]) {
      return false;
    }
    return true;
  }-*/;

  private native void setBoolean(String name, boolean value) /*-{
    this[name] = value;
  }-*/;

  private native void setDouble(String name, double value) /*-{
    this[name] = value;
  }-*/;

  private native void setInt(String name, int value) /*-{
    this[name] = value;
  }-*/;

  private native void setNull(String name) /*-{
    this[name] = null;
  }-*/;

  private native void setRequestFactory(
      RequestFactoryJsonImpl requestFactory) /*-{
    this['__rf'] = requestFactory;
  }-*/;

  private native void setSchema(ProxySchema<?> schema) /*-{
    this['__key'] = schema;
  }-*/;

  private native void setString(String name, String value) /*-{
    this[name] = value;
  }-*/;
}
