/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.i18n.server.impl;

import com.google.gwt.i18n.server.AbstractMessage;
import com.google.gwt.i18n.server.AbstractParameter;
import com.google.gwt.i18n.server.MessageTranslation;
import com.google.gwt.i18n.server.Parameter;
import com.google.gwt.i18n.server.Type;
import com.google.gwt.i18n.shared.AlternateMessageSelector;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.i18n.shared.GwtLocaleFactory;
import com.google.gwt.safehtml.shared.SafeHtml;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link com.google.gwt.i18n.server.Message Message}
 * using reflection.
 * 
 * <p><b>NOTE: THIS CLASS IS CURRENTLY ONLY SUITABLE FOR TESTING OR IF YOU
 * DON'T CARE ABOUT ACCURATE ARGUMENT NAMES</b>
 */
public class ReflectionMessage extends AbstractMessage {

  private class ReflectionParameter extends AbstractParameter {

    private final Annotation[] annotations;

    public ReflectionParameter(int idx, java.lang.reflect.Type type,
        Annotation[] annotations) {
      super(getLocaleFactory(), idx, mapClassToType(type));
      this.annotations = annotations;
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotClass) {
      return ReflectionMessage.findAnnotation(annotations, annotClass);
    }

    @Override
    public String getName() {
      String[] names = ((ReflectionMessageInterface) getMessageInterface())
          .getParameterNames(method);
      if (names != null) {
        return names[getIndex()];
      }
      return "arg" + index;
    }
  }

  private static <A> A findAnnotation(Annotation[] annotations,
      Class<A> annotClass) {
    for (Annotation annot : annotations) {
      if (annot.annotationType().equals(annotClass)) {
        return annotClass.cast(annot);
      }
    }
    return null;
  }

  private static <A extends Annotation> A getAnnotation(Method method,
      Class<A> annotClass) {
    A result = method.getAnnotation(annotClass);
    if (result != null) {
      return result;
    }
    return ReflectionUtils.getAnnotation(method.getDeclaringClass(),
        annotClass, true);
  }

  private final Method method;

  // TODO(jat): some way of fetching messages from property files/etc
  // not needed for writing translatable messages to output file, but needed
  // for eventual goal of using this API for the generator itself.
  public ReflectionMessage(GwtLocaleFactory localeFactory,
      ReflectionMessageInterface msgIntf, Method method) {
    super(localeFactory, msgIntf);
    this.method = method;
    init();
  }

  public List<AlternateMessageSelector> getAlternateSelectors() {
    // TODO(jat): implement
    throw new UnsupportedOperationException();
  }

  @Override
  public <A extends Annotation> A getAnnotation(Class<A> annotClass) {
    return getAnnotation(method, annotClass);
  }

  @Override
  public String getMethodName() {
    return method.getName();
  }

  @Override
  public List<Parameter> getParameters() {
    java.lang.reflect.Type[] paramTypes = method.getGenericParameterTypes();
    Annotation[][] paramAnnot = method.getParameterAnnotations();
    List<Parameter> params = new ArrayList<Parameter>();
    int n = paramTypes.length;
    for (int i = 0; i < n; ++i) {
      Parameter param = new ReflectionParameter(i, paramTypes[i], paramAnnot[i]);
      params.add(param);
    }
    return Collections.unmodifiableList(params);
  }

  @Override
  public Type getReturnType() {
    Class<?> type = method.getReturnType();
    return mapClassToType(type);
  }

  @Override
  public MessageTranslation getTranslation(GwtLocale locale) {
    // TODO(jat): implement
    return this;
  }

  @Override
  public boolean isAnnotationPresent(Class<? extends Annotation> annotClass) {
    if (method.isAnnotationPresent(annotClass)) {
      return true;
    }
    return ReflectionUtils.getAnnotation(method.getDeclaringClass(),
        annotClass, true) != null;
  }

  public boolean isVarArgs() {
    return method.isVarArgs();
  }

  @Override
  public String toString() {
    return getMessageInterface().toString() + "." + method.getName();
  }

  private Type mapClassToType(java.lang.reflect.Type type) {
    if (type instanceof Class<?>) {
      Class<?> clazz = (Class<?>) type;
      if (clazz.isPrimitive()) {
        if (clazz == boolean.class) {
          return Type.BOOLEAN;
        }
        if (clazz == byte.class) {
          return Type.BYTE;
        }
        if (clazz == char.class) {
          return Type.CHAR;
        }
        if (clazz == double.class) {
          return Type.DOUBLE;
        }
        if (clazz == float.class) {
          return Type.FLOAT;
        }
        if (clazz == int.class) {
          return Type.INT;
        }
        if (clazz == long.class) {
          return Type.LONG;
        }
        if (clazz == short.class) {
          return Type.SHORT;
        }
      }
      if (clazz == String.class) {
        return Type.STRING;
      }
      if (Number.class.isAssignableFrom(clazz)) {
        return Type.NUMBER;
      }
      if (SafeHtml.class.isAssignableFrom(clazz)) {
        return Type.SAFEHTML;
      }
      if (Date.class.isAssignableFrom(clazz)) {
        return Type.DATE;
      }
      if (Enum.class.isAssignableFrom(clazz)) {
        Enum<?>[] enumConstants = (Enum<?>[]) clazz.getEnumConstants();
        int n = enumConstants.length;
        String[] names = new String[n];
        for (int i = 0; i < n; ++i) {
          names[i] = enumConstants[i].name();
        }
        return new Type.EnumType(clazz.getCanonicalName(), names);
      }
      if (List.class.isAssignableFrom(clazz)) {
        // raw list type
        Type componentType = Type.OBJECT;
        String sourceName = "java.util.List<java.util.Object>";
        return new Type.ListType(sourceName, componentType);
      }
      if (clazz.isArray()) {
        Class<?> componentClass = clazz.getComponentType();
        Type componentType = mapClassToType(componentClass);
        return new Type.ArrayType(componentType.toString() + "[]",
            componentType);
      }
    }
    if (type instanceof ParameterizedType) {
      ParameterizedType pType = (ParameterizedType) type;
      java.lang.reflect.Type rawType = pType.getRawType();
      if (rawType instanceof Class<?>) {
        Class<?> clazz = (Class<?>) rawType;
        if (List.class.isAssignableFrom(clazz)) {
          java.lang.reflect.Type[] typeArgs = pType.getActualTypeArguments();
          Type componentType = mapClassToType(typeArgs[0]);
          String sourceName = clazz.getCanonicalName() + "<"
              + componentType.getSourceName() + ">";
          return new Type.ListType(sourceName, componentType);
        }
        if (Map.class.isAssignableFrom(clazz)) {
          if (type instanceof ParameterizedType) {
            java.lang.reflect.Type[] typeArgs = pType.getActualTypeArguments();
            if (typeArgs.length == 2 && typeArgs[0] == String.class
                && typeArgs[1] == String.class) {
              return Type.STRING_MAP;
            }
          }
        }
      }
    }
    return new Type(type.toString());
  }
}
