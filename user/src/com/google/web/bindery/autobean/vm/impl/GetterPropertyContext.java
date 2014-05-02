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
package com.google.web.bindery.autobean.vm.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Used by {@link ProxyAutoBean#traverseProperties()}.
 */
class GetterPropertyContext extends MethodPropertyContext {
  private final Method setter;
  private final Object shim;

  GetterPropertyContext(ProxyAutoBean<?> bean, Method setter, Type genericType, Class<?> type,
      Class<?> elementType, Class<?> keyType, Class<?> valueType) {
    super(genericType, type, elementType, keyType, valueType);

    this.setter = setter;
    this.shim = bean.as();
  }

  @Override
  public boolean canSet() {
    return setter != null;
  }

  @Override
  public void set(Object value) {
    if (!canSet()) {
      throw new UnsupportedOperationException("No setter");
    }
    try {
      setter.setAccessible(true);
      setter.invoke(shim, value);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e.getCause());
    }
  }
}
