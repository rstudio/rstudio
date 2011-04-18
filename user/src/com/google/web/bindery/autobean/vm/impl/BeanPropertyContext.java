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

import java.lang.reflect.Method;

/**
 * A property context that allows setters to be called on a simple peer,
 * regardless of whether or not the interface actually has a setter.
 */
class BeanPropertyContext extends MethodPropertyContext {
  private final ProxyAutoBean<?> bean;
  private final String propertyName;

  public BeanPropertyContext(ProxyAutoBean<?> bean, Method getter) {
    super(getter);
    this.bean = bean;
    propertyName = BeanMethod.GET.inferName(getter);
  }

  @Override
  public boolean canSet() {
    return true;
  }

  @Override
  public void set(Object value) {
    Class<?> maybeAutobox = TypeUtils.maybeAutobox(getType());
    assert value == null || maybeAutobox.isInstance(value) : value.getClass().getCanonicalName()
        + " is not assignable to " + maybeAutobox.getCanonicalName();
    bean.setProperty(propertyName, maybeAutobox.cast(value));
  }
}
