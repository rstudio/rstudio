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
package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.typeinfo.JClassType;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * An implementation of ClientFieldSerializer that handles the jdoDetachedState
 * field in the JDO API, version 2.2.
 */
final class JdoDetachedStateClientDataSerializer extends
    ClientDataSerializer {

  private static Class<? extends Annotation> annotationClass;
  private static Method detachableMethod;
  
  /**
   * The singleton instance.
   */
  private static final JdoDetachedStateClientDataSerializer theInstance =
    new JdoDetachedStateClientDataSerializer();
  
  static {
    try {
      annotationClass = Class.forName(
          "javax.jdo.annotations.PersistenceCapable").asSubclass(
          Annotation.class);
      detachableMethod = annotationClass.getDeclaredMethod("detachable",
          (Class[]) null);
    } catch (ClassNotFoundException e) {
      // Ignore, annotationClass will be null
    } catch (NoSuchMethodException e) {
      // Set annotationClass to null, don't do serialization
      annotationClass = null;
    }
  }

  /**
   * Return the unique instance of this class.
   */
  public static JdoDetachedStateClientDataSerializer getInstance() {
    return theInstance;
  }
  
  /**
   * Ensure this class has a singleton instance only.
   */
  private JdoDetachedStateClientDataSerializer() {
  }

  @Override
  public String getName() {
    return "gwt-jdo-jdoDetachedState";
  }
  
  /**
   * Returns true if the given classType should be processed by a
   * ClientDataSerializer.
   * 
   * @param classType the class type to be queried.
   */
  @Override
  public boolean shouldSerialize(JClassType classType) {
    try {
      if (annotationClass == null) {
        return false;
      }
      Annotation annotation = classType.getAnnotation(annotationClass);
      if (annotation == null) {
        return false;
      }
      Object value = detachableMethod.invoke(annotation, (Object[]) null);
      if (value instanceof String) {
        return "true".equalsIgnoreCase((String) value);
      } else {
        return false;
      }
    } catch (IllegalAccessException e) {
      // will return false
    } catch (InvocationTargetException e) {
      // will return false
    }

    return false;
  }
}
