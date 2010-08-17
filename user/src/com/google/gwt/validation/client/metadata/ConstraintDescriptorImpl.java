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
package com.google.gwt.validation.client.metadata;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.Payload;
import javax.validation.metadata.ConstraintDescriptor;

/**
 * A immutable GWT implementation of {@link ConstraintDescriptor}.
 * 
 * @param <T> the constraint annotation to describe.
 */
public class ConstraintDescriptorImpl<T extends Annotation> implements
    ConstraintDescriptor<T> {

  /**
   * Builder for {@link ConstraintDescriptorImpl}
   * 
   * @param <T> the constraint annotation to describe.
   */
  public static class Builder<T extends Annotation> {
    private T annotation;
    private Set<Class<?>> groups;
    private Set<Class<? extends Payload>> payload;
    private List<Class<? extends ConstraintValidator<T, ?>>> constraintValidatorClasses;
    private Map<String, Object> attributes;
    private Set<ConstraintDescriptor<?>> composingConstraints;
    private boolean reportAsSingleViolation;

    public ConstraintDescriptorImpl<T> build() {
      return new ConstraintDescriptorImpl<T>(
          annotation, 
          groups, 
          payload,
          constraintValidatorClasses, 
          attributes, 
          composingConstraints,
          reportAsSingleViolation);
    }

    public Builder<T> setAnnotation(T annotation) {
      this.annotation = annotation;
      return this;
    }

    public Builder<T> setAttributes(Map<String, Object> attributes) {
      this.attributes = attributes;
      return this;
    }

    public Builder<T> setComposingConstraints(
        Set<ConstraintDescriptor<?>> composingConstraints) {
      this.composingConstraints = composingConstraints;
      return this;
    }

    public Builder<T> setConstraintValidatorClasses(
        List<Class<? extends ConstraintValidator<T, ?>>> constraintValidatorClasses) {
      this.constraintValidatorClasses = constraintValidatorClasses;
      return this;
    }

    public Builder<T> setGroups(Set<Class<?>> groups) {
      this.groups = groups;
      return this;
    }

    public Builder<T> setPayload(Set<Class<? extends Payload>> payload) {
      this.payload = payload;
      return this;
    }

    public Builder<T> setReportAsSingleViolation(boolean reportAsSingleViolation) {
      this.reportAsSingleViolation = reportAsSingleViolation;
      return this;
    }
  }

  public static <T extends Annotation> Builder<T> builder() {
    return new Builder<T>();
  }

  private final T annotation;
  private final Set<Class<?>> groups;
  private final Set<Class<? extends Payload>> payload;
  private final List<Class<? extends ConstraintValidator<T, ?>>> constraintValidatorClasses;
  private final Map<String, Object> attributes;
  private final Set<ConstraintDescriptor<?>> composingConstraints;
  private final boolean reportAsSingleViolation;

  /**
   * @param annotation
   * @param groups
   * @param payload
   * @param constraintValidatorClasses
   * @param attributes
   * @param composingConstraints
   * @param reportAsSingleViolation
   */
  private ConstraintDescriptorImpl(
      T annotation,
      Set<Class<?>> groups,
      Set<Class<? extends Payload>> payload,
      List<Class<? extends ConstraintValidator<T, ?>>> constraintValidatorClasses,
      Map<String, Object> attributes,
      Set<ConstraintDescriptor<?>> composingConstraints,
      boolean reportAsSingleViolation) {
    super();
    this.annotation = annotation;
    this.groups = groups;
    this.payload = payload;
    this.constraintValidatorClasses = constraintValidatorClasses;
    this.attributes = attributes;
    this.composingConstraints = composingConstraints;
    this.reportAsSingleViolation = reportAsSingleViolation;
  }

  public T getAnnotation() {
    return annotation;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public Set<ConstraintDescriptor<?>> getComposingConstraints() {
    return composingConstraints;
  }

  public List<Class<? extends ConstraintValidator<T, ?>>> getConstraintValidatorClasses() {
    return constraintValidatorClasses;
  }

  public Set<Class<?>> getGroups() {
    return groups;
  }

  public Set<Class<? extends Payload>> getPayload() {
    return payload;
  }

  public boolean isReportAsSingleViolation() {
    return reportAsSingleViolation;
  }
}
