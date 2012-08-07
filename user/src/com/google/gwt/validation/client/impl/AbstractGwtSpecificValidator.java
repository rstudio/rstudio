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

import com.google.gwt.validation.client.Group;
import com.google.gwt.validation.client.GroupChain;
import com.google.gwt.validation.client.GroupChainGenerator;
import com.google.gwt.validation.client.GroupValidator;
import com.google.gwt.validation.client.ValidationGroupsMetadata;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator;
import javax.validation.ValidationException;
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

  protected static Class<?>[] groupsToClasses(Group... groups) {
    int numGroups = groups.length;
    Class<?>[] array = new Class<?>[numGroups];
    for (int i = 0; i < numGroups; i++) {
      array[i] = groups[i].getGroup();
    }
    return array;
  }

  @Override
  public <T> Set<ConstraintViolation<T>> validate(
      GwtValidationContext<T> context,
      G object,
      Class<?>... groups) {
    context.addValidatedObject(object);
    try {
      GroupValidator classGroupValidator = new ClassGroupValidator(object);
      GroupChain groupChain = createGroupChainFromGroups(context, groups);
      BeanMetadata beanMetadata = getBeanMetadata();
      List<Class<?>> defaultGroupSeq = beanMetadata.getDefaultGroupSequence();
      if (beanMetadata.defaultGroupSequenceIsRedefined()) {
        // only need to check this on class-level validation
        groupChain.checkDefaultGroupSequenceIsExpandable(defaultGroupSeq);
      }
      return validateGroups(context, classGroupValidator, groupChain);
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (ValidationException e) {
      throw e;
    } catch (Exception e) {
      throw new ValidationException("Error validating " + object.getClass(), e);
    }
  }

  @Override
  public <T> Set<ConstraintViolation<T>> validateProperty(
      GwtValidationContext<T> context,
      G object,
      String propertyName,
      Class<?>... groups) throws ValidationException {
    try {
      GroupValidator propertyGroupValidator = new PropertyGroupValidator(object, propertyName);
      GroupChain groupChain = createGroupChainFromGroups(context, groups);
      return validateGroups(context, propertyGroupValidator, groupChain);
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (ValidationException e) {
      throw e;
    } catch (Exception e) {
      throw new ValidationException("Error validating property " + propertyName +
          " of " + object.getClass(), e);
    }
  }

  @Override
  public <T> Set<ConstraintViolation<T>> validateValue(
      GwtValidationContext<T> context,
      Class<G> beanType,
      String propertyName,
      Object value,
      Class<?>... groups) throws ValidationException {
    try {
      GroupValidator valueGroupValidator = new ValueGroupValidator(beanType, propertyName, value);
      GroupChain groupChain = createGroupChainFromGroups(context, groups);
      return validateGroups(context, valueGroupValidator, groupChain);
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (ValidationException e) {
      throw e;
    } catch (Exception e) {
      throw new ValidationException("Error validating property " + propertyName +
          " with value " + value + " of " + beanType, e);
    }
  }

  protected List<Class<?>> addDefaultGroupWhenEmpty(List<Class<?>> groups) {
    if (groups.isEmpty()) {
      groups = new ArrayList<Class<?>>();
      groups.add(Default.class);
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
      ConstraintDescriptorImpl<A> constraintDescriptor, Class<?>... groups) {
    validator.initialize(constraintDescriptor.getAnnotation());
    ConstraintValidatorContextImpl<A, V> constraintValidatorContext =
        context.createConstraintValidatorContext(constraintDescriptor);

    List<Class<?>> groupsList = Arrays.asList(groups);
    ValidationGroupsMetadata validationGroupsMetadata =
        context.getValidator().getValidationGroupsMetadata();
    Set<Class<?>> constraintGroups = constraintDescriptor.getGroups();

    // check groups requested are in the set of constraint groups (including the implicit group)
    if (!containsAny(groupsList, constraintGroups)
        && !groupsList.contains(getConstraints(validationGroupsMetadata).getElementClass())) {
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

  private <T> GroupChain createGroupChainFromGroups(GwtValidationContext<T> context, Class<?>... groups) {
    List<Class<?>> groupsList = addDefaultGroupWhenEmpty(Arrays.asList(groups));
    ValidationGroupsMetadata validationGroupsMetadata =
        context.getValidator().getValidationGroupsMetadata();
    return new GroupChainGenerator(validationGroupsMetadata).getGroupChainFor(groupsList);
  }

  /**
   * Performs the top-level validation using a helper {@link GroupValidator}. This takes
   * group sequencing and Default group overriding into account.
   */
  private <T> Set<ConstraintViolation<T>> validateGroups(
      GwtValidationContext<T> context,
      GroupValidator groupValidator,
      GroupChain groupChain) {

    Set<ConstraintViolation<T>> violations = new HashSet<ConstraintViolation<T>>();

    Collection<Group> allGroups = groupChain.getAllGroups();
    Group[] allGroupsArray = allGroups.toArray(new Group[allGroups.size()]);
    groupValidator.validateGroups(context, violations, allGroupsArray);

    // handle sequences
    Iterator<List<Group>> sequenceIterator = groupChain.getSequenceIterator();
    while (sequenceIterator.hasNext()) {
      List<Group> sequence = sequenceIterator.next();
      for (Group group : sequence) {
        int numberOfViolations = violations.size();
        groupValidator.validateGroups(context, violations, group);
        if (violations.size() > numberOfViolations) {
          // stop processing when an error occurs
          break;
        }
      }
    }
    return violations;
  }

  private class ClassGroupValidator implements GroupValidator {
    private final G object;

    public ClassGroupValidator(G object) {
      this.object = object;
    }

    @Override
    public <T> void validateGroups(GwtValidationContext<T> context,
        Set<ConstraintViolation<T>> violations, Group... groups) {
      expandDefaultAndValidateClassGroups(context, object, violations, groups);
    }
  }

  private class PropertyGroupValidator implements GroupValidator {
    private final G object;
    private final String propertyName;

    public PropertyGroupValidator(G object, String propertyName) {
      this.object = object;
      this.propertyName = propertyName;
    }

    @Override
    public <T> void validateGroups(GwtValidationContext<T> context,
        Set<ConstraintViolation<T>> violations, Group... groups) {
      expandDefaultAndValidatePropertyGroups(context, object, propertyName, violations, groups);
    }
  }

  private class ValueGroupValidator implements GroupValidator {
    private final Class<G> beanType;
    private final String propertyName;
    private final Object value;

    public ValueGroupValidator(Class<G> beanType, String propertyName, Object value) {
      this.beanType = beanType;
      this.propertyName = propertyName;
      this.value = value;
    }

    @Override
    public <T> void validateGroups(GwtValidationContext<T> context,
        Set<ConstraintViolation<T>> violations, Group... groups) {
      expandDefaultAndValidateValueGroups(context, beanType, propertyName, value, violations, //
          groups);
    }
  }
}
