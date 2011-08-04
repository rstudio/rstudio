/*
 * Copyright 2011 Google Inc.
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
package org.hibernate.jsr303.tck.tests.constraints.invalidconstraintdefinitions;

import org.hibernate.jsr303.tck.util.TckCompileTestCase;

import javax.validation.ConstraintDefinitionException;
import javax.validation.Validator;

/**
 * Test wrapper for
 * {@link org.hibernate.jsr303.tck.tests.constraints.invalidconstraintdefinitions.InvalidConstraintDefinitionsTest}
 * .
 */
public class InvalidConstraintDefinitionsCompileTest extends TckCompileTestCase {

  public void testConstraintDefinitionWithoutGroupParameter() {
    assertConstraintDefinitionException(
        ConstraintDefinitionWithoutGroupParameterFactory.TestValidator.class,
        "Unable to create a validator for org.hibernate.jsr303.tck."
            + "tests.constraints.invalidconstraintdefinitions."
            + "InvalidConstraintDefinitionsTest.DummyEntityNoGroups because "
            + "org.hibernate.jsr303.tck.tests.constraints."
            + "invalidconstraintdefinitions.NoGroups contains Constraint "
            + "annotation, but does not contain a groups parameter.");
  }

  public void testConstraintDefinitionWithoutMessageParameter() {
    assertConstraintDefinitionException(
        ConstraintDefinitionWithoutMessageParameterFactory.TestValidator.class,
        "Unable to create a validator for "
            + "org.hibernate.jsr303.tck.tests.constraints"
            + ".invalidconstraintdefinitions."
            + "InvalidConstraintDefinitionsTest"
            + ".DummyEntityNoMessage because "
            + "org.hibernate.jsr303.tck.tests.constraints"
            + ".invalidconstraintdefinitions"
            + ".NoMessage contains Constraint annotation, "
            + "but does not contain a message parameter.");
  }

  public void testConstraintDefinitionWithoutPayloadParameter() {
    assertConstraintDefinitionException(
        ConstraintDefinitionWithoutPayloadParameterFactory.TestValidator.class,
        "Unable to create a validator for org.hibernate.jsr303.tck.tests"
            + ".constraints.invalidconstraintdefinitions"
            + ".InvalidConstraintDefinitionsTest"
            + ".DummyEntityNoPayload because org.hibernate.jsr303.tck.tests"
            + ".constraints.invalidconstraintdefinitions.NoPayload contains "
            + "Constraint annotation, but does not contain a payload "
            + "parameter.");
  }

  public void testConstraintDefinitionWithParameterStartingWithValid() {
    assertConstraintDefinitionException(
        ConstraintDefinitionWithParameterStartingWithValidFactory.TestValidator.class,
        "Unable to create a validator for org.hibernate.jsr303.tck.tests"
            + ".constraints.invalidconstraintdefinitions"
            + ".InvalidConstraintDefinitionsTest.DummyEntityValidProperty "
            + "because Parameters starting with 'valid' are not allowed in a constraint.");
  }

  public void testConstraintDefinitionWithWrongDefaultGroupValue() {
    assertConstraintDefinitionException(
        ConstraintDefinitionWithWrongDefaultGroupValueFactory.TestValidator.class,
        "Unable to create a validator for "
            + "org.hibernate.jsr303.tck.tests.constraints"
            + ".invalidconstraintdefinitions.InvalidConstraintDefinitionsTest"
            + ".DummyEntityInvalidDefaultGroup "
            + "because org.hibernate.jsr303.tck.tests.constraints"
            + ".invalidconstraintdefinitions"
            + ".InvalidDefaultGroup contains Constraint annotation, "
            + "but the groups parameter default value is not the empty array.");
  }

  public void testConstraintDefinitionWithWrongDefaultPayloadValue() {
    assertConstraintDefinitionException(
        ConstraintDefinitionWithWrongDefaultPayloadValueFactory.TestValidator.class,
        "Unable to create a validator for org.hibernate.jsr303.tck.tests"
            + ".constraints.invalidconstraintdefinitions"
            + ".InvalidConstraintDefinitionsTest"
            + ".DummyEntityInvalidDefaultPayload "
            + "because org.hibernate.jsr303.tck.tests.constraints"
            + ".invalidconstraintdefinitions.InvalidDefaultPayload contains "
            + "Constraint annotation, but the payload parameter default "
            + "value is not the empty array.");
  }

  public void testConstraintDefinitionWithWrongGroupType() {
    assertConstraintDefinitionException(
        ConstraintDefinitionWithWrongGroupTypeFactory.TestValidator.class,
        "Unable to create a validator for org.hibernate.jsr303.tck.tests"
            + ".constraints.invalidconstraintdefinitions"
            + ".InvalidConstraintDefinitionsTest"
            + ".DummyEntityInvalidGroupsType because "
            + "org.hibernate.jsr303.tck.tests.constraints"
            + ".invalidconstraintdefinitions.InvalidGroupsType "
            + "contains Constraint annotation, but the groups "
            + "parameter is of wrong type.");
  }

  public void testConstraintDefinitionWithWrongMessageType() {
    assertConstraintDefinitionException(
        ConstraintDefinitionWithWrongMessageTypeFactory.TestValidator.class,
        "Unable to create a validator for org.hibernate.jsr303.tck.tests"
            + ".constraints"
            + ".invalidconstraintdefinitions.InvalidConstraintDefinitionsTest"
            + ".DummyEntityInvalidMessageType because "
            + "org.hibernate.jsr303.tck.tests.constraints"
            + ".invalidconstraintdefinitions.InvalidMessageType "
            + "contains Constraint annotation, "
            + "but the message parameter is not of type java.lang.String.");
  }

  public void testConstraintDefinitionWithWrongPayloadClass() {
    assertConstraintDefinitionException(
        ConstraintDefinitionWithWrongPayloadClassFactory.TestValidator.class,
        "Unable to create a validator for "
            + "org.hibernate.jsr303.tck.tests"
            + ".constraints.invalidconstraintdefinitions"
            + ".InvalidConstraintDefinitionsTest.DummyEntityInvalidPayloadClass "
            + "because org.hibernate.jsr303.tck.tests.constraints"
            + ".invalidconstraintdefinitions.InvalidPayloadClass "
            + "contains Constraint annotation, "
            + "but the payload parameter is of wrong type.");
  }

  private void assertConstraintDefinitionException(
      final Class<? extends Validator> validatorClass,
      final String expectedMessage) {
    assertValidatorFailsToCompile(validatorClass,
        ConstraintDefinitionException.class, expectedMessage);
  }
}
