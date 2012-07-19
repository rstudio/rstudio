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

import java.util.Arrays;
import java.util.Set;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.ValidationException;
import javax.validation.Validator;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * Base methods for implementing {@link Validator} in GWT.
 * <p>
 * All methods that do not need to be generated go here.
 */
public abstract class AbstractGwtValidator implements Validator {

  private final Set<Class<?>> validGroups;
  private final GroupInheritanceMap groupInheritanceMap;
  private ConstraintValidatorFactory contraintValidatorFactory;
  private MessageInterpolator messageInterpolator;
  private TraversableResolver traversableResolver;

  /**
   * Creates a validator initialized with the default group inheritance map.
   * @see #AbstractGwtValidator(GroupInheritanceMap)
   */
  public AbstractGwtValidator() {
    this(new GroupInheritanceMap());
  }

  /**
   * @param groupInheritanceMap Map of groups to the groups' parents.
   */
  public AbstractGwtValidator(GroupInheritanceMap groupInheritanceMap) {
    validGroups = groupInheritanceMap.getAllGroups();
    this.groupInheritanceMap = groupInheritanceMap;
  }

  public GroupInheritanceMap getGroupInheritanceMap() {
    return groupInheritanceMap;
  }

  public void init(ConstraintValidatorFactory factory,
      MessageInterpolator messageInterpolator,
      TraversableResolver traversableResolver) {
    this.contraintValidatorFactory = factory;
    this.messageInterpolator = messageInterpolator;
    this.traversableResolver = traversableResolver;
  }

  @Override
  public <T> T unwrap(Class<T> type) {
    throw new ValidationException();
  }

  public abstract <T> Set<ConstraintViolation<T>> validate(
      GwtValidationContext<T> context, Object object, Class<?>... groups)
      throws ValidationException;

  protected void checkGroups(Class<?>... groups) {
    if (!validGroups.containsAll(Arrays.asList(groups))) {
      throw new IllegalArgumentException(this.getClass()
          + " only processes the following groups " + validGroups);
    }
  }

  protected void checkNotNull(Object object, String name)
      throws IllegalArgumentException {
    if (object == null) {
      throw new IllegalArgumentException(name + " can not be null.");
    }
  }

  protected ConstraintValidatorFactory getConstraintValidatorFactory() {
    return contraintValidatorFactory;
  }

  protected MessageInterpolator getMessageInterpolator() {
    return messageInterpolator;
  }

  protected TraversableResolver getTraversableResolver() {
    return traversableResolver;
  }
}
