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
package com.google.gwt.validation.client.impl;

import java.lang.annotation.Annotation;

import javax.validation.MessageInterpolator;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstraintDescriptor;

/**
 * Context for a {@link com.google.gwt.validation.client.GwtValidation}.
 *
 * @param <T> the type of the root bean.
 */
public class GwtValidationContext<T> {

  private final BeanDescriptor beanDescriptor;
  private PathImpl path = new PathImpl();
  private final T rootBean;
  private final MessageInterpolator messageInterpolator;

  /**
   *
   */
  public GwtValidationContext(T rootBean, BeanDescriptor beanDescriptor,
      MessageInterpolator messageInterpolator) {
    this.rootBean = rootBean;
    this.beanDescriptor = beanDescriptor;
    this.messageInterpolator = messageInterpolator;
  }

  /**
   * Append a node named name to the path..
   *
   * @param name
   * @return the new GwtValidationContext.
   */
  public GwtValidationContext<T> append(String name) {
    GwtValidationContext<T> temp = new GwtValidationContext<T>(rootBean,
        beanDescriptor, messageInterpolator);
    temp.path = temp.path.append(name);
    return temp;
  }

  public <A extends Annotation, V> ConstraintValidatorContextImpl<A, V> createConstraintValidatorContext(
      ConstraintDescriptor<A> descriptor) {
    return new ConstraintValidatorContextImpl<A, V>(path, descriptor);
  }

  public MessageInterpolator getMessageInterpolator() {
    return messageInterpolator;
  }

  public T getRootBean() {
    return rootBean;
  }

  @SuppressWarnings("unchecked")
  public Class<T> getRootBeanClass() {
    return (Class<T>) rootBean.getClass();
  }
}
