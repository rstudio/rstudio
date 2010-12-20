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
package com.google.gwt.validation.client.constraints;

import com.google.gwt.validation.client.ValidationClientGwtTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * A GWT test to force the compilation of all default standard
 * {@link javax.validation.ConstraintValidator}s.
 */
public class GwtCompileTest extends ValidationClientGwtTestCase {

  public void testDefaultConstraints() {
    List<Class<?>> temp = new ArrayList<Class<?>>();
    temp.add(AssertFalseValidator.class);
    temp.add(AssertTrueValidator.class);
    temp.add(DecimalMaxValidatorForNumber.class);
    temp.add(DecimalMaxValidatorForString.class);
    temp.add(DecimalMinValidatorForNumber.class);
    temp.add(DecimalMinValidatorForString.class);
    temp.add(DigitsValidatorForNumber.class);
    temp.add(DigitsValidatorForString.class);
    // FutureValidatorForCalendar GWT does not support java.util.Calendar
    temp.add(FutureValidatorForDate.class);
    temp.add(MaxValidatorForNumber.class);
    temp.add(MaxValidatorForString.class);
    temp.add(MinValidatorForNumber.class);
    temp.add(MinValidatorForString.class);
    temp.add(NotNullValidator.class);
    temp.add(NullValidator.class);
    // PastValidatorForCalendar GWT does not support java.util.Calendar
    temp.add(PastValidatorForDate.class);
    temp.add(PatternValidator.class);
    temp.add(SizeValidatorForArrayOfBoolean.class);
    temp.add(SizeValidatorForArrayOfByte.class);
    temp.add(SizeValidatorForArrayOfChar.class);
    temp.add(SizeValidatorForArrayOfDouble.class);
    temp.add(SizeValidatorForArrayOfFloat.class);
    temp.add(SizeValidatorForArrayOfInt.class);
    temp.add(SizeValidatorForArrayOfLong.class);
    temp.add(SizeValidatorForArrayOfObject.class);
    temp.add(SizeValidatorForArrayOfShort.class);
    temp.add(SizeValidatorForCollection.class);
    temp.add(SizeValidatorForMap.class);
    temp.add(SizeValidatorForString.class);

    assertEquals(29, temp.size());
  }
}
