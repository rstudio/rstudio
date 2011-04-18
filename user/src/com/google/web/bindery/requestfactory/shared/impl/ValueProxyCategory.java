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
package com.google.web.bindery.requestfactory.shared.impl;

import static com.google.web.bindery.requestfactory.shared.impl.BaseProxyCategory.stableId;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;
import com.google.web.bindery.requestfactory.shared.ValueProxy;

/**
 * Contains static implementation of ValueProxy-specific methods.
 */
public class ValueProxyCategory {

  /**
   * ValueProxies are equal if they are from the same RequestContext and all of
   * their properties are equal.
   */
  public static boolean equals(AutoBean<? extends ValueProxy> bean, Object o) {
    if (!(o instanceof ValueProxy)) {
      return false;
    }
    AutoBean<ValueProxy> other = AutoBeanUtils.getAutoBean((ValueProxy) o);
    if (other == null) {
      // Unexpected, could be an user-provided implementation?
      return false;
    }
    if (!stableId(bean).getProxyClass().equals(stableId(other).getProxyClass())) {
      // Compare AppleProxies to AppleProxies
      return false;
    }

    // Compare the entire object graph
    return AutoBeanUtils.deepEquals(bean, other);
  }

  /**
   * Hashcode depends on property values.
   */
  public static int hashCode(AutoBean<? extends ValueProxy> bean) {
    return AutoBeanUtils.getAllProperties(bean).hashCode();
  }
}
