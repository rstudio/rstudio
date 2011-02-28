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
package com.google.gwt.validation.rebind;

import static com.google.gwt.validation.rebind.GwtSpecificValidatorCreator.getValidatorForType;

import com.google.gwt.thirdparty.guava.common.collect.ImmutableList;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;
import com.google.gwt.validation.client.constraints.SizeValidatorForCollection;
import com.google.gwt.validation.client.constraints.SizeValidatorForString;

import junit.framework.TestCase;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintValidator;
import javax.validation.constraints.Size;

/**
 * NonGWT tests for {@link GwtSpecificValidatorCreator}.
 */
public class GwtSpecificValidatorCreatorTest extends TestCase {

  private static Set<Class<? extends ConstraintValidator<? extends Annotation, ?>>> copyOf(
      Class<? extends ConstraintValidator<? extends Annotation, ?>>... classes) {
    return ImmutableSet.copyOf(classes);
  }

  ImmutableList<Class<? extends ConstraintValidator<Size, ?>>> sizeValidators = ImmutableList.<Class<? extends ConstraintValidator<Size, ?>>> of(
      SizeValidatorForCollection.class, SizeValidatorForString.class);

  @SuppressWarnings("unchecked")
  public void testGetValidatorForType_collection() throws Exception {
    ImmutableSet<Class<? extends ConstraintValidator<Size, ?>>> actual = getValidatorForType(
        List.class, sizeValidators);
    assertEquals(copyOf(SizeValidatorForCollection.class), actual);
  }

  @SuppressWarnings("unchecked")
  public void testGetValidatorForType_string() throws Exception {
    Class<String> target = String.class;
    ImmutableSet<Class<? extends ConstraintValidator<Size, ?>>> actual = getValidatorForType(
        target, sizeValidators);
    assertEquals(copyOf(SizeValidatorForString.class), actual);
  }
}
