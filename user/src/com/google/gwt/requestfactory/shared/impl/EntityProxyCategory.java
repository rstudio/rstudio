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
package com.google.gwt.requestfactory.shared.impl;

import com.google.gwt.autobean.shared.AutoBean;
import com.google.gwt.autobean.shared.AutoBeanUtils;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.requestfactory.shared.RequestFactory;

/**
 * Contains static implementation methods used by the AutoBean generator.
 */
public class EntityProxyCategory {
  static final String REQUEST_CONTEXT = "requestContext";
  static final String REQUEST_FACTORY = "requestFactory";
  static final String STABLE_ID = "stableId";

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
        && requestContext(bean) == requestContext(other);
  }

  /**
   * Hashcode is always that of the stableId, since it's stable across time.
   */
  public static int hashCode(AutoBean<? extends EntityProxy> bean) {
    return stableId(bean).hashCode();
  }

  public static AbstractRequestContext requestContext(AutoBean<?> bean) {
    return (AbstractRequestContext) bean.getTag(REQUEST_CONTEXT);
  }

  public static RequestFactory requestFactory(
      AutoBean<? extends EntityProxy> bean) {
    return (RequestFactory) bean.getTag(REQUEST_FACTORY);
  }

  @SuppressWarnings("unchecked")
  public static <T extends EntityProxy> SimpleEntityProxyId<T> stableId(
      AutoBean<? extends T> bean) {
    return (SimpleEntityProxyId<T>) bean.getTag(STABLE_ID);
  }

  /**
   * Sniff all return values and ensure that if the current bean is a mutable
   * EntityProxy, that its return values are mutable.
   */
  // CHECKSTYLE_OFF
  public static <T> T __intercept(AutoBean<?> bean, T returnValue) {
    // CHECKSTYLE_ON

    AbstractRequestContext context = requestContext(bean);

    /*
     * The context will be null if the bean is immutable. If the context is
     * locked, don't try to edit.
     */
    if (context == null || context.isLocked()) {
      return returnValue;
    }

    /*
     * EntityProxies need to be recorded specially by the RequestContext, so
     * delegate to the edit() method for wiring up the context.
     */
    if (returnValue instanceof EntityProxy) {
      @SuppressWarnings("unchecked")
      T toReturn = (T) context.edit((EntityProxy) returnValue);
      return toReturn;
    }

    /*
     * We're returning some object that's not an EntityProxy, most likely a
     * Collection type. At the very least, propagate the current RequestContext
     * so that editable chains can be constructed.
     */
    AutoBean<T> otherBean = AutoBeanUtils.getAutoBean(returnValue);
    if (otherBean != null) {
      otherBean.setTag(EntityProxyCategory.REQUEST_CONTEXT,
          bean.getTag(EntityProxyCategory.REQUEST_CONTEXT));
    }
    return returnValue;
  }
}
