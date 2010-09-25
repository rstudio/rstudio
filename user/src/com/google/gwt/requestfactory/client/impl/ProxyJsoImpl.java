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
import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.EntityProxyId;
import com.google.gwt.requestfactory.shared.impl.CollectionProperty;
import com.google.gwt.requestfactory.shared.impl.Property;
import com.google.gwt.requestfactory.shared.impl.RequestData;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

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

  public static final String REQUEST_FACTORY_FIELD = "__rf";

  public static final String SCHEMA_FIELD = "__key";

  public static ProxyJsoImpl create(JavaScriptObject rawJsoWithIdAndVersion,
      ProxySchema<?> schema, RequestFactoryJsonImpl requestFactory) {

    ProxyJsoImpl rtn = rawJsoWithIdAndVersion.cast();
    rtn.setSchema(schema);
    rtn.setRequestFactory(requestFactory);
    rtn.assertValid();
    return rtn;
  };

  public static JsArray<ProxyJsoImpl> create(JsArray<JavaScriptObject> rawJsos,
      ProxySchema<?> schema, RequestFactoryJsonImpl requestFactory) {

    for (int i = 0; i < rawJsos.length(); i++) {
      ProxyJsoImpl.create(rawJsos.get(i), schema, requestFactory);
    }

    return rawJsos.cast();
  }

  public static ProxyJsoImpl create(String encodedId, Integer version,
      ProxySchema<?> schema, RequestFactoryJsonImpl requestFactory) {
    ProxyJsoImpl rtn = createEmpty();
    rtn.putEncodedId(encodedId);
    rtn.putVersion(version);
    return create(rtn, schema, requestFactory);
  }

  public static ProxyJsoImpl emptyCopy(ProxyJsoImpl jso) {
    ProxySchema<?> schema = jso.getSchema();
    return create(jso.encodedId(), jso.version(), schema,
        jso.getRequestFactory());
  }

  static native Date dateForDouble(double millis) /*-{
    return @java.util.Date::createFrom(D)(millis);
  }-*/;

  static Object encodeToJsType(Object o) {
     if (o instanceof BigDecimal || o instanceof BigInteger || o instanceof Long
         || o instanceof String || o instanceof Character) {
      return o.toString();
    } else if (o instanceof Number) {
      return ((Number) o).doubleValue();
    } else if (o instanceof Date) {
      return String.valueOf(((Date) o).getTime());
    } else if (o instanceof ProxyImpl) {
      return ((ProxyImpl) o).wireFormatId();
    } else if (o instanceof Enum) {
      return (double) ((Enum) o).ordinal();
    }
    return o;
  }

  static native void splice(JavaScriptObject array, int index, int deleteCount) /*-{
     array.splice(index, deleteCount);
   }-*/; 

   static native void splice(JavaScriptObject array, int index, int deleteCount,
       boolean value) /*-{
     array.splice(index, deleteCount, value);
   }-*/;

  static native void splice(JavaScriptObject array, int index, int deleteCount,
       double value) /*-{
     array.splice(index, deleteCount, value);
   }-*/;

  static native void splice(JavaScriptObject array, int index, int deleteCount,
       Object value) /*-{
     array.splice(index, deleteCount, value);
   }-*/;

  /** 
   * Create an empty JSO, unsafe to return.
   */
  private static native ProxyJsoImpl createEmpty() /*-{
    return {};
  }-*/;

  private static native void pushBoolean(JavaScriptObject jso, boolean b) /*-{
    jso.push(b);
  }-*/;

  protected ProxyJsoImpl() {
  }

  public final void assertValid() {
    assert encodedId() != null : "encodedId required";
    assert getRequestFactory() != null : "requestFactory required";
    assert version() != null : "version required";
    assert getSchema() != null : "schema required";
  }

  public final native void delete(String name)/*-{
    delete this[name];
  }-*/;

  public final native String encodedId() /*-{
    return this[@com.google.gwt.requestfactory.shared.impl.RequestData::ENCODED_ID_PROPERTY];
  }-*/;

  public final <V> V get(Property<V> property) {
    String name = property.getName();
    Class<V> type = property.getType();
    if (property instanceof CollectionProperty) {
      assert type == List.class || type == Set.class;
      JsoCollection col = (JsoCollection) getCollection(
          (CollectionProperty) property);
      col.setDependencies(property, null);
      return (V) col;
    }
    // javac 1.6.0_20 on mac has problems without the explicit parameterization
    return this.<V>get(name, type);
  }

  public final native <T> T get(String propertyName) /*-{
    return this[propertyName] || null;
  }-*/;

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
        return (V) Character.valueOf(String.valueOf(get(name)).charAt(0));
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
      throw new IllegalStateException("Property  " + name + " has invalid "
          + " value " + get(name) + " for type " + type);
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

    // else, it must be a record type
    String relatedId = (String) get(name);
    if (relatedId == null) {
      return null;
    } else {
      // TODO: should cache this or modify JSO field in place
      String schemaAndId[] = relatedId.split("@");
      assert schemaAndId.length == 3;
      ProxySchema<?> schema = getRequestFactory().getSchema(schemaAndId[2]);
      EntityProxy toReturn = getRequestFactory().getValueStore().getRecordBySchemaAndId(schema,
          schemaAndId[0], schemaAndId[1].equals("IS"), getRequestFactory());
      return (V) toReturn;
    }
  }                                                                                                 

  @SuppressWarnings("unchecked")
  public final  <L extends Collection<V>, V> L getCollection(CollectionProperty<L, V> property) {
    JsArrayString array = get(property.getName());
    if (array == null) {
      return null;
    }
    if (property.getType().equals(List.class)) {
      return (L) new JsoList<V>(getRequestFactory(), array);
    } else if (property.getType().equals(Set.class)) {
      return (L) new JsoSet<V>(getRequestFactory(), array);
    }
    throw new IllegalArgumentException("Collection type " + property.getType()
        + " for property " + property.getName() + " not supported.");
  }

  public final native RequestFactoryJsonImpl getRequestFactory() /*-{
    return this[@com.google.gwt.requestfactory.client.impl.ProxyJsoImpl::REQUEST_FACTORY_FIELD];
  }-*/;

  public final native ProxySchema<?> getSchema() /*-{
    return this[@com.google.gwt.requestfactory.client.impl.ProxyJsoImpl::SCHEMA_FIELD];
  }-*/;

  public final native boolean isDefined(String name)/*-{
    return this[name] !== undefined;
  }-*/;

  public final boolean isEmpty() {
    for (Property<?> property : getSchema().allProperties()) {
      if (isDefined(property.getName())) {
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
      setString(property.getName(), ((ProxyImpl) value).wireFormatId());
      return;
    }

    if (value instanceof JsoCollection) {
      setJso(property.getName(), ((JsoCollection) value).asJso());
      return;
    } else if (value instanceof Collection) {
      JavaScriptObject jso = JavaScriptObject.createArray();
      for (Object o : ((Collection) value)) {
        o = encodeToJsType(o);
        if (o instanceof String) {
          ((JsArrayString) jso).push((String) o);
        } else if (o instanceof Double) {
          ((JsArrayNumber) jso).push((Double) o);
        } else if (o instanceof Boolean) {
          pushBoolean(jso, (Boolean) o);
        }
      }
      setJso(property.getName(), jso);
      return;
    }
    throw new UnsupportedOperationException("Cannot set properties of type "
        + value.getClass().getName());
  }

  public final EntityProxyId<?> stableId() {
    throw new IllegalArgumentException("Can't call stableId on the jso");
  }

  /**
   * Return JSON representation using org.json library.
   * 
   * @return returned string.
   */
  public final native String toJson() /*-{
    var rtn = @com.google.gwt.requestfactory.client.impl.json.ClientJsonUtil::stringify(Lcom/google/gwt/core/client/JavaScriptObject;)(this);
    return rtn;
  }-*/;

  public final Integer version() {
    return Integer.valueOf(getInt(RequestData.ENCODED_VERSION_PROPERTY));
  }

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

  final native void putEncodedId(String id) /*-{
    this[@com.google.gwt.requestfactory.shared.impl.RequestData::ENCODED_ID_PROPERTY] = id;
  }-*/;

  final void putVersion(Integer version) {
    setInt(RequestData.ENCODED_VERSION_PROPERTY, version);
  }

  private native boolean copyPropertyIfDifferent(String name, ProxyJsoImpl from) /*-{
    if (this[name] == from[name]) {
      return false;
    }
    this[name] = from[name];
    return true;
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

  private native void setJso(String name, JavaScriptObject value) /*-{
     this[name] = value;
   }-*/;

  private native void setNull(String name) /*-{
    this[name] = null;
  }-*/;

  private native void setRequestFactory(RequestFactoryJsonImpl requestFactory) /*-{
    this[@com.google.gwt.requestfactory.client.impl.ProxyJsoImpl::REQUEST_FACTORY_FIELD] = requestFactory;
  }-*/;

  private native void setSchema(ProxySchema<?> schema) /*-{
    this[@com.google.gwt.requestfactory.client.impl.ProxyJsoImpl::SCHEMA_FIELD] = schema;
  }-*/;

  private native void setString(String name, String value) /*-{
    this[name] = value;
  }-*/;

}
