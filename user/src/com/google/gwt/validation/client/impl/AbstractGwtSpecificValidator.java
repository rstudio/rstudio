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

import com.google.gwt.validation.client.GroupInheritanceMap;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator;
import javax.validation.groups.Default;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * Base methods for implementing a {@link GwtSpecificValidator}.
 * <p>
 * All methods that do not need to be generated go here.
 * 
 * @param <G> the type object to validate
 */
public abstract class AbstractGwtSpecificValidator<G> implements
    GwtSpecificValidator<G> {

  /**
   * Builds attributes one at a time.
   * <p>
   * Used to create a attribute map for annotations.
   */
  public static final class AttributeBuilder {
   private final HashMap<String, Object> tempMap = new HashMap<String, Object>();

    private AttributeBuilder() {
    }

    public Map<String, Object> build() {
      return Collections.unmodifiableMap(tempMap);
    }

    public AttributeBuilder put(String key, Object value) {
      tempMap.put(key, value);
      return this;
    }
  }

  public static AttributeBuilder attributeBuilder() {
    return new AttributeBuilder();
  }

  protected Class<?>[] addDefaultGroupWhenEmpty(Class<?>[] groups) {
    if (groups.length == 0) {
      groups = new Class<?>[]{Default.class};
    }
    return groups;
  }

  protected <V, T, A extends Annotation> void addSingleViolation(
      GwtValidationContext<T> context, Set<ConstraintViolation<T>> violations,
      G object, V value, ConstraintDescriptorImpl<A> constraintDescriptor) {
    ConstraintValidatorContextImpl<A, V> constraintValidatorContext =
      context.createConstraintValidatorContext(constraintDescriptor);
    addViolations(context, violations, object, value, constraintDescriptor,
        constraintValidatorContext);
  }

  /**
   * Perform the actual validation of a single {@link ConstraintValidator}.
   * <p>
   * As a side effect {@link ConstraintViolation}s may be added to
   * {@code violations}.
   * 
   * @return true if there was any constraint violations
   */
  protected <A extends Annotation, T, V> boolean validate(
      GwtValidationContext<T> context, Set<ConstraintViolation<T>> violations,
      G object, V value, ConstraintValidator<A, ? super V> validator,
      ConstraintDescriptorImpl<A> constraintDescriptor, Class<?>[] groups) {
    validator.initialize(constraintDescriptor.getAnnotation());
    ConstraintValidatorContextImpl<A, V> constraintValidatorContext =
        context.createConstraintValidatorContext(constraintDescriptor);

    GroupInheritanceMap groupInheritanceMap = context.getValidator().getGroupInheritanceMap();
    // check against the groups passed in as well as their parent (super) interfaces
    Set<Class<?>> extendedGroups = groupInheritanceMap.findAllExtendedGroups(Arrays.asList(groups));
    Set<Class<?>> constraintGroups = constraintDescriptor.getGroups();

    // check groups requested are in the set of constraint groups (including the implicit group)
    if (!containsAny(extendedGroups, constraintGroups)
        && !Arrays.asList(groups).contains(getConstraints().getElementClass())) {
      return false;
    }

    if (!validator.isValid(value, constraintValidatorContext)) {
      addViolations(//
          context, //
          violations, //
          object, //
          value, //
          constraintDescriptor, //
          constraintValidatorContext);
      return true;
    }
    return false;
  }

  private <V, T, A extends Annotation> void addViolations(
      GwtValidationContext<T> context, Set<ConstraintViolation<T>> violations,
      G object, V value, ConstraintDescriptorImpl<A> constraintDescriptor,
      ConstraintValidatorContextImpl<A, V> constraintValidatorContext) {
    Set<MessageAndPath> mps = constraintValidatorContext.getMessageAndPaths();
    for (MessageAndPath messageAndPath : mps) {
      ConstraintViolation<T> violation = createConstraintViolation(//
          context, //
          object, //
          value, //
          constraintDescriptor, //
          messageAndPath);
      violations.add(violation);
    }
  }

  private <T> boolean containsAny(Collection<T> left, Collection<T> right) {
    for (T t : left) {
      if (right.contains(t)) {
        return true;
      }
    }
    return false;
  }

  private <T, V, A extends Annotation> ConstraintViolation<T> createConstraintViolation(
      GwtValidationContext<T> context, G object, V value,
      ConstraintDescriptorImpl<A> constraintDescriptor,
      MessageAndPath messageAndPath) {
    MessageInterpolator messageInterpolator = context.getMessageInterpolator();
    com.google.gwt.validation.client.impl.MessageInterpolatorContextImpl messageContext = new MessageInterpolatorContextImpl(
        constraintDescriptor, value);
    String message = messageInterpolator.interpolate(
        messageAndPath.getMessage(), messageContext);
    ConstraintViolation<T> violation = ConstraintViolationImpl.<T> builder() //
        .setConstraintDescriptor(constraintDescriptor) //
        .setInvalidValue(value) //
        .setLeafBean(object) //
        .setMessage(message) //
        .setMessageTemplate(messageAndPath.getMessage()) //
        .setPropertyPath(messageAndPath.getPath()) //
        .setRootBean(context.getRootBean()) //
        .setRootBeanClass(context.getRootBeanClass()) //
        .setElementType(constraintDescriptor.getElementType()) //
        .build();
    return violation;
  }
}
