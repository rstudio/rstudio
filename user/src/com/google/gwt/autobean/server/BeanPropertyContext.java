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
package com.google.gwt.autobean.server;

import com.google.gwt.autobean.server.impl.TypeUtils;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * A property context that allows setters to be called on a simple peer,
 * regardless of whether or not the interface actually has a setter.
 */
class BeanPropertyContext extends MethodPropertyContext {
  private final String propertyName;
  private final Map<String, Object> map;

  public BeanPropertyContext(ProxyAutoBean<?> bean, Method getter) {
    super(getter);
    propertyName = getter.getName().substring(3);
    map = bean.getPropertyMap();
  }

  @Override
  public boolean canSet() {
    return true;
  }

  @Override
  public void set(Object value) {
    map.put(propertyName, TypeUtils.maybeAutobox(getType()).cast(value));
  }
}
