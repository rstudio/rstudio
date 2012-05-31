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

import static com.google.web.bindery.requestfactory.shared.impl.BaseProxyCategory.requestContext;
import static com.google.web.bindery.requestfactory.shared.impl.Constants.STABLE_ID;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;
import com.google.web.bindery.requestfactory.shared.EntityProxy;

/**
 * Contains static implementation of EntityProxy-specific methods.
 */
public class EntityProxyCategory {

  /**
   * EntityProxies are equal if they are from the same RequestContext and their
   * stableIds are equal.
   */
  public static boolean equals(AutoBean<? extends EntityProxy> bean, Object o) {
    if (!(o instanceof EntityProxy)) {
      return false;
    }
    AutoBean<EntityProxy> other = AutoBeanUtils.getAutoBean((EntityProxy) o);
    if (other == null) {
      // Unexpected, could be an user-provided implementation?
      return false;
    }

    // Object comparison intentional. True if both null or both the same
    return stableId(bean).equals(stableId(other))
        && nonDiffingRequestContext(bean) == nonDiffingRequestContext(other);
  }

  /**
   * Hashcode is always that of the stableId, since it's stable across time.
   */
  public static int hashCode(AutoBean<? extends EntityProxy> bean) {
    return stableId(bean).hashCode();
  }

  /**
   * Effectively overrides {@link BaseProxyCategory#stableId(AutoBean)} to
   * return a narrower bound.
   */
  public static <T extends EntityProxy> SimpleEntityProxyId<T> stableId(
      AutoBean<? extends T> bean) {
    return bean.getTag(STABLE_ID);
  }

  private static AbstractRequestContext nonDiffingRequestContext(AutoBean<?> bean) {
    AbstractRequestContext context = requestContext(bean);
    if (context != null && context.isDiffing()) {
      context = null;
    }
    return context;
  }
}
