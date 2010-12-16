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

import java.util.HashMap;
import java.util.Map;

/**
 * Maps class literals to type signatures and type signatures to serialization
 * methods. Relies on monotonic behavior of hashcodes in Production Mode defined
 * in {@link com.google.gwt.core.client.impl.Impl#getHashCode(Object)} In hosted
 * mode, we map the underlying signature JsArray onto a proper IdentityHashMap.
 */
public abstract class SerializerBase implements Serializer {

  /**
   * Used in JavaScript to map a type to a set of serialization functions.
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

  private final Map<String, TypeHandler> handlerCache;
  
  private final Map<String, String> methodMapJava;
  
  private final MethodMap methodMapNative;

  private final Map<String, String> signatureMapJava;

  private final JsArrayString signatureMapNative;

  public SerializerBase(Map<String, String> methodMapJava,
      MethodMap methodMapNative, Map<String, String> signatureMapJava,
      JsArrayString signatureMapNative) {
    this.handlerCache = new HashMap<String, TypeHandler>();
    this.methodMapJava = methodMapJava;
    this.methodMapNative = methodMapNative;
    this.signatureMapJava = signatureMapJava;
    this.signatureMapNative = signatureMapNative;
  }

  public final void deserialize(SerializationStreamReader stream,
      Object instance, String typeSignature) throws SerializationException {
    if (GWT.isScript()) {
      check(typeSignature, 2);
      methodMapNative.deserialize(stream, instance, typeSignature);
    } else {
      TypeHandler typeHandler = getTypeHandler(typeSignature);
      typeHandler.deserial(stream, instance);
    }
  }

  public final String getSerializationSignature(Class<?> clazz) {
    assert clazz != null : "clazz";
    if (GWT.isScript()) {
      return signatureMapNative.get(clazz.hashCode());
    } else {
      return signatureMapJava.get(clazz.getName());
    }
  }

  public final Object instantiate(SerializationStreamReader stream,
      String typeSignature) throws SerializationException {
    if (GWT.isScript()) {
      check(typeSignature, 1);
      return methodMapNative.instantiate(stream, typeSignature);
    } else {
      TypeHandler typeHandler = getTypeHandler(typeSignature);
      return typeHandler.create(stream);
    }
  }

  public final void serialize(SerializationStreamWriter stream,
      Object instance, String typeSignature) throws SerializationException {
    if (GWT.isScript()) {
      check(typeSignature, 3);
      methodMapNative.serialize(stream, instance, typeSignature);
    } else {
      TypeHandler typeHandler = getTypeHandler(typeSignature);
      typeHandler.serial(stream, instance);
    }
  }

  private void check(String typeSignature, int length)
      throws SerializationException {
    /*
     * Probably trying to serialize a type that isn't supposed to be
     * serializable.
     */
    if (methodMapNative.get(typeSignature) == null) {
      throw new SerializationException(typeSignature);
    }

    assert methodMapNative.get(typeSignature).length() >= length : "Not enough methods, expecting "
        + length + " saw " + methodMapNative.get(typeSignature).length();
  }

  private TypeHandler getTypeHandler(String typeSignature)
      throws SerializationException {
    String typeHandlerClass = methodMapJava.get(typeSignature);

    if (typeHandlerClass == null) {
     /*
      * Probably trying to serialize a type that isn't supposed to be
      * serializable.
      */
      throw new SerializationException(typeSignature);
    }

    TypeHandler typeHandler = handlerCache.get(typeHandlerClass);
    
    if (typeHandler == null) {
      try {
        Class<?> klass = ReflectionHelper.loadClass(typeHandlerClass);
        typeHandler = (TypeHandler) ReflectionHelper.newInstance(klass);
        handlerCache.put(typeHandlerClass, typeHandler);
      } catch (Exception e) {
        throw new SerializationException(e);
      }
    }
    return typeHandler;
  }
}
