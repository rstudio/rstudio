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

import java.io.Serializable;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;

/**
 * An implementation of {@link ConstraintViolation}.
 *
 * @param <T> the type of bean validated.
 */
public class ConstraintViolationImpl<T> implements ConstraintViolation<T>,
    Serializable {

  /**
   * Builder for ConstraintViolations.
   *
   * @param <T> the type of bean validated.
   */
  public static class Builder<T> {
    private String message;
    private String messageTemplate;
    private T rootBean;
    private Class<T> rootBeanClass;
    private Object leafBean;
    private Path propertyPath;
    private Object invalidValue;
    private ConstraintDescriptor<?> constraintDescriptor;

    public ConstraintViolationImpl<T> build() {
      return new ConstraintViolationImpl<T>(message, messageTemplate, rootBean,
          rootBeanClass, leafBean, propertyPath, invalidValue,
          constraintDescriptor);
    }

    public Builder<T> setConstraintDescriptor(
        ConstraintDescriptor<?> constraintDescriptor) {
      this.constraintDescriptor = constraintDescriptor;
      return this;
    }

    public Builder<T> setInvalidValue(Object invalidValue) {
      this.invalidValue = invalidValue;
      return this;
    }

    public Builder<T> setLeafBean(Object leafBean) {
      this.leafBean = leafBean;
      return this;
    }

    public Builder<T> setMessage(String message) {
      this.message = message;
      return this;
    }

    public Builder<T> setMessageTemplate(String messageTemplate) {
      this.messageTemplate = messageTemplate;
      return this;
    }

    public Builder<T> setPropertyPath(Path propertyPath) {
      this.propertyPath = propertyPath;
      return this;
    }

    public Builder<T> setRootBean(T rootBean) {
      this.rootBean = rootBean;
      return this;
    }

    public Builder<T> setRootBeanClass(Class<T> rootBeanClass) {
      this.rootBeanClass = rootBeanClass;
      return this;
    }
  }

  private static final long serialVersionUID = 1L;

  public static <T> Builder<T> builder() {
    return new Builder<T>();
  }

  private final String message;
  private final String messageTemplate;
  private final T rootBean;
  private final Class<T> rootBeanClass;
  private final Object leafBean;
  private final Path propertyPath;
  private final Object invalidValue;
  private final ConstraintDescriptor<?> constraintDescriptor;

  /**
   * @param message
   * @param messageTemplate
   * @param rootBean
   * @param rootBeanClass
   * @param leafBean
   * @param propertyPath
   * @param invalidValue
   * @param constraintDescriptor
   */
  private ConstraintViolationImpl(String message, String messageTemplate,
      T rootBean, Class<T> rootBeanClass, Object leafBean, Path propertyPath,
      Object invalidValue, ConstraintDescriptor<?> constraintDescriptor) {
    super();
    this.message = message;
    this.messageTemplate = messageTemplate;
    this.rootBean = rootBean;
    this.rootBeanClass = rootBeanClass;
    this.leafBean = leafBean;
    this.propertyPath = propertyPath;
    this.invalidValue = invalidValue;
    this.constraintDescriptor = constraintDescriptor;
  }

  public ConstraintDescriptor<?> getConstraintDescriptor() {
    return constraintDescriptor;
  }

  public Object getInvalidValue() {
    return invalidValue;
  }

  public Object getLeafBean() {
    return leafBean;
  }

  public String getMessage() {
    return message;
  }

  public String getMessageTemplate() {
    return messageTemplate;
  }

  public Path getPropertyPath() {
    return propertyPath;
  }

  public T getRootBean() {
    return rootBean;
  }

  public Class<T> getRootBeanClass() {
    return rootBeanClass;
  }
}
