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
package org.hibernate.jsr303.tck.tests.constraints.groups.groupsequence;

import com.google.gwt.core.client.GWT;
import com.google.gwt.validation.client.AbstractGwtValidatorFactory;
import com.google.gwt.validation.client.GwtValidation;
import com.google.gwt.validation.client.impl.AbstractGwtValidator;

import org.hibernate.jsr303.tck.tests.constraints.groups.groupsequence.SequenceResolutionTest.All;
import org.hibernate.jsr303.tck.tests.constraints.groups.groupsequence.SequenceResolutionTest.AllReverse;
import org.hibernate.jsr303.tck.tests.constraints.groups.groupsequence.SequenceResolutionTest.Car;
import org.hibernate.jsr303.tck.tests.constraints.groups.groupsequence.SequenceResolutionTest.First;
import org.hibernate.jsr303.tck.tests.constraints.groups.groupsequence.SequenceResolutionTest.InvalidGroupSequence;
import org.hibernate.jsr303.tck.tests.constraints.groups.groupsequence.SequenceResolutionTest.Mixed;
import org.hibernate.jsr303.tck.tests.constraints.groups.groupsequence.SequenceResolutionTest.Second;
import org.hibernate.jsr303.tck.tests.constraints.groups.groupsequence.SequenceResolutionTest.Third;

import javax.validation.Validator;
import javax.validation.groups.Default;

/**
 * {@link AbstractGwtValidatorFactory} implementation that uses
 * {@link com.google.gwt.validation.client.GwtValidation GwtValidation}.
 */
public final class TckTestValidatorFactory extends AbstractGwtValidatorFactory {
  /**
   * Marker Interface to {@link GWT#create(Class)}.
   */
  @GwtValidation(value = {Car.class, TestEntity.class},
      groups = {Default.class, InvalidGroupSequence.class, First.class, Second.class, Third.class,
        All.class, AllReverse.class, Mixed.class, Complete.class})
  public static interface GwtValidator extends Validator {
  }

  @Override
  public AbstractGwtValidator createValidator() {
    return GWT.create(GwtValidator.class);
  }
}
