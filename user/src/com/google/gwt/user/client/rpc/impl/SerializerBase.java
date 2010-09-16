/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.user.client.rpc.impl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Maps class literals to type signatures and type signatures to serialization
 * methods. Relies on monotonic behavior of hashcodes in web mode defined in
 * {@link com.google.gwt.core.client.impl.Impl#getHashCode(Object)} In hosted
 * mode, we map the underlying signature JsArray onto a proper IdentityHashMap.
 */
public abstract class SerializerBase implements Serializer {

  /**
   * Represents a collection of functions that perform type-specific functions.
   */
  protected static final class MethodMap extends JavaScriptObject {
    protected MethodMap() {
    }

    native void deserialize(SerializationStreamReader stream, Object instance,
        String signature) throws SerializationException /*-{
      this[signature][1](stream, instance);
    }-*/;

    native JsArray<JavaScriptObject> get(String signature) /*-{
      return this[signature];
    }-*/;

    native Object instantiate(SerializationStreamReader stream, String signature)
        throws SerializationException /*-{
      return this[signature][0](stream);
    }-*/;

    native void put(String signature, JsArray<JavaScriptObject> methods) /*-{
      this[signature] = methods;
    }-*/;

    native void serialize(SerializationStreamWriter stream, Object instance,
        String signature) throws SerializationException /*-{
      this[signature][2](stream, instance);
    }-*/;
  }

  private static final Map<JsArrayString, Map<Class<?>, String>> hostedSignatureMaps;

  static {
    if (GWT.isScript()) {
      hostedSignatureMaps = null;
    } else {
      hostedSignatureMaps = new IdentityHashMap<JsArrayString, Map<Class<?>, String>>();
    }
  }

  protected static final void registerMethods(MethodMap methodMap,
      String signature, JsArray<JavaScriptObject> methods) {
    assert signature != null : "signature";
    assert methodMap.get(signature) == null : "Duplicate signature "
        + signature;

    methodMap.put(signature, methods);
  }

  protected static final void registerSignature(JsArrayString signatureMap,
      Class<?> clazz, String signature) {
    assert clazz != null : "clazz";
    assert signature != null : "signature";

    if (GWT.isScript()) {
      assert signatureMap.get(clazz.hashCode()) == null : "Duplicate signature "
          + signature;
      signatureMap.set(clazz.hashCode(), signature);

    } else {
      Map<Class<?>, String> subMap = getSubMap(signatureMap);

      assert !subMap.containsKey(clazz);
      subMap.put(clazz, signature);
    }
  }

  /**
   * Hashcodes in hosted mode are unpredictable. Each signature map is
   * associated with a proper IdentityHashMap. This method should only be used
   * in hosted mode.
   */
  private static Map<Class<?>, String> getSubMap(JsArrayString signatureMap) {
    assert !GWT.isScript() : "Should only use this in hosted mode";
    Map<Class<?>, String> subMap = hostedSignatureMaps.get(signatureMap);
    if (subMap == null) {
      subMap = new IdentityHashMap<Class<?>, String>();
      hostedSignatureMaps.put(signatureMap, subMap);
    }
    return subMap;
  }

  public final void deserialize(SerializationStreamReader stream,
      Object instance, String typeSignature) throws SerializationException {
    check(typeSignature, 2);

    getMethodMap().deserialize(stream, instance, typeSignature);
  }

  public final String getSerializationSignature(Class<?> clazz) {
    assert clazz != null : "clazz";
    if (GWT.isScript()) {
      return getSignatureMap().get(clazz.hashCode());
    } else {
      return getSubMap(getSignatureMap()).get(clazz);
    }
  }

  public final Object instantiate(SerializationStreamReader stream,
      String typeSignature) throws SerializationException {
    check(typeSignature, 1);

    return getMethodMap().instantiate(stream, typeSignature);
  }

  public final void serialize(SerializationStreamWriter stream,
      Object instance, String typeSignature) throws SerializationException {
    check(typeSignature, 3);

    getMethodMap().serialize(stream, instance, typeSignature);
  }

  protected abstract MethodMap getMethodMap();

  protected abstract JsArrayString getSignatureMap();

  private void check(String typeSignature, int length)
      throws SerializationException {
    /*
     * Probably trying to serialize a type that isn't supposed to be
     * serializable.
     */
    if (getMethodMap().get(typeSignature) == null) {
      throw new SerializationException(typeSignature);
    }

    assert getMethodMap().get(typeSignature).length() >= length : "Not enough methods, expecting "
        + length + " saw " + getMethodMap().get(typeSignature).length();
  }
}
