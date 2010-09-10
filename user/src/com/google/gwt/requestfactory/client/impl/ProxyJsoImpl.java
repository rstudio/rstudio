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
import com.google.gwt.requestfactory.shared.EnumProperty;
import com.google.gwt.requestfactory.shared.Property;
import com.google.gwt.requestfactory.shared.PropertyReference;

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

  public static ProxyJsoImpl create(Long id, Integer version,
      ProxySchema<?> schema, RequestFactoryJsonImpl requestFactory) {
    ProxyJsoImpl rtn = createEmpty();
    rtn.set(EntityProxy.id, id);
    rtn.set(EntityProxy.version, version);
    return create(rtn, schema, requestFactory);
  }

  public static ProxyJsoImpl emptyCopy(ProxyJsoImpl jso) {
    Long id = jso.get(EntityProxy.id);
    Integer version = jso.get(EntityProxy.version);
    ProxySchema<?> schema = jso.getSchema();

    ProxyJsoImpl copy = create(id, version, schema, jso.getRequestFactory());
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

  @SuppressWarnings("unchecked")
  public final <V> V get(Property<V> property) {
    if (isNullOrUndefined(property.getName())) {
      return null;
    }

    try {
      if (Boolean.class.equals(property.getType())) {
        return (V) Boolean.valueOf(getBoolean(property.getName()));
      }
      if (Character.class.equals(property.getType())) {
        return (V) Character.valueOf(String.valueOf(get(property.getName())).charAt(
            0));
      }
      if (Byte.class.equals(property.getType())) {
        return (V) Byte.valueOf((byte) getInt(property.getName()));
      }
      if (Short.class.equals(property.getType())) {
        return (V) Short.valueOf((short) getInt(property.getName()));
      }
      if (Float.class.equals(property.getType())) {
        return (V) Float.valueOf((float) getDouble(property.getName()));
      }
      if (BigInteger.class.equals(property.getType())) {
        return (V) new BigDecimal((String) get(property.getName())).toBigInteger();
      }
      if (BigDecimal.class.equals(property.getType())) {
        return (V) new BigDecimal((String) get(property.getName()));
      }
      if (Integer.class.equals(property.getType())) {
        return (V) Integer.valueOf(getInt(property.getName()));
      }
      if (Long.class.equals(property.getType())) {
        return (V) Long.valueOf((String) get(property.getName()));
      }
      if (Double.class.equals(property.getType())) {
        if (!isDefined(property.getName())) {
          return (V) new Double(0.0);
        }
        return (V) Double.valueOf(getDouble(property.getName()));
      }
      if (Date.class.equals(property.getType())) {
        double millis = new Date().getTime();
        if (isDefined(property.getName())) {
          millis = Double.parseDouble((String) get(property.getName()));
        }
        if (GWT.isScript()) {
          return (V) dateForDouble(millis);
        } else {
          // In dev mode, we're using real JRE dates
          return (V) new Date((long) millis);
        }
      }
    } catch (final Exception ex) {
      throw new IllegalStateException("Property  " + property.getName()
          + " has invalid " + " value " + get(property.getName())
          + " for type " + property.getType());
    }

    if (property instanceof EnumProperty) {
      EnumProperty<V> eProperty = (EnumProperty<V>) property;
      Enum<?>[] values = (Enum[]) eProperty.getValues();
      int ordinal = getInt(property.getName());
      for (Enum<?> value : values) {
        if (ordinal == value.ordinal()) {
          return (V) value;
        }
      }
    }

    if (String.class == property.getType()) {
      return (V) get(property.getName());
    }
    // at this point, we checked all the known primitive/value types we support
    // TODO: handle embedded types, List, Set, Map

    // else, it must be a record type
    String relatedId = (String) get(property.getName());
    if (relatedId == null) {
      return null;
    } else {
      // TODO: should cache this or modify JSO field in place
      String schemaAndId[] = relatedId.split("-");
      assert schemaAndId.length == 2;
      ProxySchema<?> schema = getRequestFactory().getSchema(schemaAndId[0]);
      return (V) getRequestFactory().getValueStore().getRecordBySchemaAndId(schema,
          Long.valueOf(schemaAndId[1]), getRequestFactory());
    }
  }

  public final native <T> T get(String propertyName) /*-{
    return this[propertyName] || null;
  }-*/;

  public final Long getId() {
    return this.get(id);
  }

  public final <V> PropertyReference<V> getRef(Property<V> property) {
    return new PropertyReference<V>(this, property);
  }

  public final native RequestFactoryJsonImpl getRequestFactory() /*-{
    return this['__rf'];
  }-*/;

  public final native ProxySchema<?> getSchema() /*-{
    return this['__key'];
  }-*/;
  
  public final Integer getVersion() {
    return this.get(version);
  }

  /**
   * @param name
   */
  public final native boolean isDefined(String name)/*-{
    return this[name] !== undefined;
  }-*/;

  public final boolean isEmpty() {
    for (Property<?> property : getSchema().allProperties()) {
      if ((property != EntityProxy.id) && (property != EntityProxy.version)
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

  private final native void setRequestFactory(
      RequestFactoryJsonImpl requestFactory) /*-{
    this['__rf'] = requestFactory;
  }-*/;

  private final native void setSchema(ProxySchema<?> schema) /*-{
    this['__key'] = schema;
  }-*/;

  private native void setString(String name, String value) /*-{
    this[name] = value;
  }-*/;
}
