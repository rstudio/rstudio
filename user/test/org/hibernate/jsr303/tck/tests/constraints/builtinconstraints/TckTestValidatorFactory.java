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
package org.hibernate.jsr303.tck.tests.constraints.builtinconstraints;

import com.google.gwt.core.client.GWT;
import com.google.gwt.validation.client.AbstractGwtValidatorFactory;
import com.google.gwt.validation.client.GwtValidation;
import com.google.gwt.validation.client.impl.AbstractGwtValidator;

import org.hibernate.jsr303.tck.tests.constraints.builtinconstraints.BuiltinConstraintsGwtTest.FutureDummyEntity;
import org.hibernate.jsr303.tck.tests.constraints.builtinconstraints.BuiltinConstraintsGwtTest.PastDummyEntity;
import org.hibernate.jsr303.tck.tests.constraints.builtinconstraints.BuiltinConstraintsTest.AssertFalseDummyEntity;
import org.hibernate.jsr303.tck.tests.constraints.builtinconstraints.BuiltinConstraintsTest.AssertTrueDummyEntity;
import org.hibernate.jsr303.tck.tests.constraints.builtinconstraints.BuiltinConstraintsTest.DecimalMaxDummyEntity;
import org.hibernate.jsr303.tck.tests.constraints.builtinconstraints.BuiltinConstraintsTest.DecimalMinDummyEntity;
import org.hibernate.jsr303.tck.tests.constraints.builtinconstraints.BuiltinConstraintsTest.DigitsDummyEntity;
import org.hibernate.jsr303.tck.tests.constraints.builtinconstraints.BuiltinConstraintsTest.MaxDummyEntity;
import org.hibernate.jsr303.tck.tests.constraints.builtinconstraints.BuiltinConstraintsTest.MinDummyEntity;
import org.hibernate.jsr303.tck.tests.constraints.builtinconstraints.BuiltinConstraintsTest.NotNullDummyEntity;
import org.hibernate.jsr303.tck.tests.constraints.builtinconstraints.BuiltinConstraintsTest.NullDummyEntity;
import org.hibernate.jsr303.tck.tests.constraints.builtinconstraints.BuiltinConstraintsTest.PatternDummyEntity;
import org.hibernate.jsr303.tck.tests.constraints.builtinconstraints.BuiltinConstraintsTest.SizeDummyEntity;

import javax.validation.Validator;

/**
 * {@link AbstractGwtValidatorFactory} implementation that uses
 * {@link com.google.gwt.validation.client.GwtValidation GwtValidation}.
 */
public final class TckTestValidatorFactory extends AbstractGwtValidatorFactory {
  /**
   * Marker Interface for {@link GWT#create(Class)}.
   */
  @GwtValidation(value = {
      AssertFalseDummyEntity.class, AssertTrueDummyEntity.class,
      DecimalMinDummyEntity.class, DecimalMaxDummyEntity.class,
      DigitsDummyEntity.class, FutureDummyEntity.class, MinDummyEntity.class,
      MaxDummyEntity.class, NullDummyEntity.class, NotNullDummyEntity.class,
      PastDummyEntity.class, PatternDummyEntity.class, SizeDummyEntity.class})
  public static interface GwtValidator extends Validator {
  }

  @Override
  public AbstractGwtValidator createValidator() {
    return GWT.create(GwtValidator.class);
  }
}
