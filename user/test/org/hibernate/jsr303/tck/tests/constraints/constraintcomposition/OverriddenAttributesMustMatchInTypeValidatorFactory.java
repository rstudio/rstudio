package org.hibernate.jsr303.tck.tests.constraints.constraintcomposition;

import com.google.gwt.core.client.GWT;
import com.google.gwt.validation.client.AbstractGwtValidatorFactory;
import com.google.gwt.validation.client.GwtValidation;
import com.google.gwt.validation.client.impl.AbstractGwtValidator;

import org.hibernate.jsr303.tck.tests.constraints.constraintcomposition.ConstraintCompositionTest.DummyEntityWithZipCode;

import javax.validation.Validator;

/**
 * {@link AbstractGwtValidatorFactory} implementation that uses
 * {@link com.google.gwt.validation.client.GwtValidation GwtValidation}.
 */
public final class OverriddenAttributesMustMatchInTypeValidatorFactory extends
    AbstractGwtValidatorFactory {

  /**
   * Validator for
   * {@link ConstraintCompositionTest#testOverriddenAttributesMustMatchInType()}
   */
  @GwtValidation(value = {DummyEntityWithZipCode.class})
  public static interface OverriddenAttributesMustMatchInTypeValidator extends
      Validator {
  }

  @Override
  public AbstractGwtValidator createValidator() {
    return GWT.create(OverriddenAttributesMustMatchInTypeValidator.class);
  }
}