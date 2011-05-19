/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.core.ext.soyc.impl;

import com.google.gwt.core.ext.soyc.Member;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A factory object for the standard implementations of Member subtypes. The
 * factory methods in this type provide canonicalized instances. The maps used
 * by MemberFactory use hard, identity-based references.
 */
public class MemberFactory {
  private final Map<Class<?>, Map<?, ?>> map = new IdentityHashMap<Class<?>, Map<?, ?>>();

  public StandardClassMember get(JDeclaredType type) {
    return getOrCreate(type, StandardClassMember.class, JDeclaredType.class);
  }

  public StandardFieldMember get(JField field) {
    return getOrCreate(field, StandardFieldMember.class, JField.class);
  }

  public StandardMethodMember get(JMethod method) {
    return getOrCreate(method, StandardMethodMember.class, JMethod.class);
  }

  @SuppressWarnings("unchecked")
  private <K, V extends Member> Map<K, V> getElementMap(Class<V> clazz) {
    Map<K, V> elementMap = (Map<K, V>) map.get(clazz);
    if (elementMap == null) {
      elementMap = new IdentityHashMap<K, V>();
      map.put(clazz, elementMap);
    }
    return elementMap;
  }

  /**
   * Assumes that the implementation of Member has a two-arg constructor that
   * accepts a MemberFactory and the key.
   * 
   * @param <K> the type of key used to canonicalize the mapping
   * @param <V> the type of Member implementation to use
   * @param key the key by which the value should be canonicalized
   * @param implClazz the concrete type of Member to construct
   * @param constructorParam the declared type of the second parameter of the
   *          concrete Member type
   * @return the canonicalized instance of Member for the given key
   */
  private <K, V extends Member> V getOrCreate(K key, Class<V> implClazz,
      Class<? super K> constructorParam) {
    Map<K, V> elementMap = getElementMap(implClazz);

    V toReturn = elementMap.get(key);
    if (toReturn == null) {
      try {
        Constructor<V> ctor = implClazz.getConstructor(MemberFactory.class, constructorParam);
        toReturn = ctor.newInstance(this, key);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(implClazz.getName() + " must declare a two-arg (MemberFactory, "
            + constructorParam.getName() + ") constructor", e);
      } catch (IllegalArgumentException e) {
        // Error on the part of this type
        throw new RuntimeException(e);
      } catch (InstantiationException e) {
        // Error on the part of this type, asking for a non-instantiable type
        throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
        // Error on the part of the coder of implClazz
        throw new RuntimeException(e);
      } catch (InvocationTargetException e) {
        // Probably a RuntimeException thrown from the constructor
        if (e.getCause() instanceof RuntimeException) {
          throw (RuntimeException) e.getCause();
        }
        throw new RuntimeException(e);
      }

      elementMap.put(key, toReturn);
    }

    return toReturn;
  }
}