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
package com.google.web.bindery.requestfactory.shared;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;
import com.google.web.bindery.requestfactory.shared.impl.Constants;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintViolation;

/**
 * Callback object for {@link Request#fire(Receiver)} and
 * {@link RequestContext#fire(Receiver)}.
 * 
 * @param <V> value type
 */
public abstract class Receiver<V> {
  /**
   * Receives general failure notifications. The default implementation looks at
   * {@link ServerFailure#isFatal()}, and throws a runtime exception with the
   * failure object's error message if it is true.
   * 
   * @param error a {@link ServerFailure} instance
   */
  public void onFailure(ServerFailure error) {
    if (error.isFatal()) {
      throw new RuntimeException(error.getMessage());
    }
  }

  /**
   * Called when a Request has been successfully executed on the server.
   * 
   * @param response a response of type V
   */
  public abstract void onSuccess(V response);

  /**
   * Called if an object sent to the server could not be validated. The default
   * implementation calls {@link #onFailure(ServerFailure)} if <code>errors
   * </code> is not empty.
   * 
   * @param errors a Set of {@link Violation} instances
   * @deprecated Use {@link #onConstraintViolation(Set)} instead
   */
  @Deprecated
  public void onViolation(Set<Violation> errors) {
    if (!errors.isEmpty()) {
      onFailure(new ServerFailure("The call failed on the server due to a ConstraintViolation"));
    }
  }

  /**
   * Called if an object sent to the server could not be validated. The default
   * implementation calls {@link #onViolation(Set)}, converting the
   * {@link ConstraintViolation} objects to the deprecated {@link Violation}
   * type.
   * 
   * @param violations a Set of {@link ConstraintViolation} instances
   */
  @SuppressWarnings("deprecation")
  public void onConstraintViolation(Set<ConstraintViolation<?>> violations) {
    Set<Violation> converted = new HashSet<Violation>();
    for (final ConstraintViolation<?> v : violations) {
      converted.add(new Violation() {
        public BaseProxy getInvalidProxy() {
          return (BaseProxy) v.getRootBean();
        }

        public String getMessage() {
          return v.getMessage();
        }

        public BaseProxy getOriginalProxy() {
          AutoBean<? extends BaseProxy> parent =
              AutoBeanUtils.getAutoBean(v.getRootBean()).getTag(Constants.PARENT_OBJECT);
          return parent == null ? null : parent.as();
        }

        public String getPath() {
          return v.getPropertyPath().toString();
        }

        public EntityProxyId<?> getProxyId() {
          return v.getRootBean() instanceof EntityProxy ? ((EntityProxy) v.getRootBean())
              .stableId() : null;
        }
      });
    }
    onViolation(Collections.unmodifiableSet(converted));
  }
}
