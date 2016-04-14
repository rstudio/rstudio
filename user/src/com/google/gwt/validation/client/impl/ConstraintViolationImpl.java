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
import java.lang.annotation.ElementType;
import java.util.Objects;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;

/**
 * An implementation of {@link ConstraintViolation}.
 * 
 * @param <T> the type of bean validated.
 */
public final class ConstraintViolationImpl<T> implements ConstraintViolation<T>,
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
    private ElementType elementType;
    private ConstraintDescriptor<?> constraintDescriptor;

    public ConstraintViolationImpl<T> build() {
      return new ConstraintViolationImpl<T>(message, messageTemplate, rootBean,
          rootBeanClass, leafBean, propertyPath, invalidValue, elementType,
          constraintDescriptor);
    }

    public Builder<T> setConstraintDescriptor(
        ConstraintDescriptor<?> constraintDescriptor) {
      this.constraintDescriptor = constraintDescriptor;
      return this;
    }

    public Builder<T> setElementType(ElementType elementType) {
      this.elementType = elementType;
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
  private final ElementType elementType;
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
      Object invalidValue, ElementType elementType,
      ConstraintDescriptor<?> constraintDescriptor) {
    super();
    this.message = message;
    this.messageTemplate = messageTemplate;
    this.rootBean = rootBean;
    this.rootBeanClass = rootBeanClass;
    this.leafBean = leafBean;
    this.propertyPath = propertyPath;
    this.invalidValue = invalidValue;
    this.elementType = elementType;
    this.constraintDescriptor = constraintDescriptor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ConstraintViolationImpl)) {
      return false;
    }
    ConstraintViolationImpl<?> other = (ConstraintViolationImpl<?>) o;
    return Objects.equals(message, other.message)
        && Objects.equals(propertyPath, other.propertyPath)
        && Objects.equals(rootBean, other.rootBean)
        && Objects.equals(leafBean, other.leafBean)
        && Objects.equals(elementType, other.elementType)
        && Objects.equals(invalidValue, other.invalidValue);
  }

  @Override
  public ConstraintDescriptor<?> getConstraintDescriptor() {
    return constraintDescriptor;
  }

  @Override
  public Object getInvalidValue() {
    return invalidValue;
  }

  @Override
  public Object getLeafBean() {
    return leafBean;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public String getMessageTemplate() {
    return messageTemplate;
  }

  @Override
  public Path getPropertyPath() {
    return propertyPath;
  }

  @Override
  public T getRootBean() {
    return rootBean;
  }

  @Override
  public Class<T> getRootBeanClass() {
    return rootBeanClass;
  }

  @Override
  public int hashCode() {
    int result = message != null ? message.hashCode() : 0;
    result = 31 * result + (propertyPath != null ? propertyPath.hashCode() : 0);
    result = 31 * result + (rootBean != null ? rootBean.hashCode() : 0);
    result = 31 * result + (leafBean != null ? leafBean.hashCode() : 0);
    result = 31 * result + (elementType != null ? elementType.hashCode() : 0);
    result = 31 * result + (invalidValue != null ? invalidValue.hashCode() : 0);
    return result;
  }

  /**
   * For debugging only. Do not rely on the format. It can change at any time.
   */
  @Override
  public String toString() {
    return "ConstraintViolationImpl(message= " + message //
        + ", path= " + propertyPath //
        + ", invalidValue=" + invalidValue //
        + ", desc=" + constraintDescriptor //
        + ", elementType=" + elementType + ")";
  }
}
