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
package com.google.gwt.validation.testing.constraints;

import com.google.gwt.validation.client.constraints.SizeValidatorForCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.Size;

/**
 * Tests for {@link SizeValidatorForCollection}.
 */
public class SizeValidatorForCollectionTest extends
    ConstraintValidatorTestCase<Size, Collection<?>> {

  @SuppressWarnings("unused")
  @Size(min = 2, max = 5)
  private Date defaultField;

  protected SizeValidatorForCollection createValidator() {
    return new SizeValidatorForCollection();
  }

  public void testAssertIsValid_short() {
    assertConstraintValidator(createList(1), false);
  }

  public void testAssertIsValid_min() {
    assertConstraintValidator(createList(2), true);
  }

  public void testAssertIsValid_max() {
    assertConstraintValidator(createList(5), true);
  }

  public void testAssertIsValid_long() {
    assertConstraintValidator(createList(6), false);
  }

  private Collection<Integer> createList(int size) {
    List<Integer> list = new ArrayList<Integer>(size);
    for (int i = 1; i <= size; i++) {
      Integer key = Integer.valueOf(i);
      list.add(key);
    }
    return list;
  }

  @Override
  protected Class<Size> getAnnotationClass() {
    return Size.class;
  }
}
