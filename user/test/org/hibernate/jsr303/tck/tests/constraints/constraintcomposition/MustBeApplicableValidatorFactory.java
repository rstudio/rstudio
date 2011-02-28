package org.hibernate.jsr303.tck.tests.constraints.constraintcomposition;

import com.google.gwt.core.client.GWT;
import com.google.gwt.validation.client.AbstractGwtValidatorFactory;
import com.google.gwt.validation.client.GwtValidation;
import com.google.gwt.validation.client.impl.AbstractGwtValidator;

import javax.validation.Validator;

/**
 * {@link AbstractGwtValidatorFactory} implementation that uses
 * {@link com.google.gwt.validation.client.GwtValidation GwtValidation}.
 */
public final class MustBeApplicableValidatorFactory extends
    AbstractGwtValidatorFactory {

  /**
   * Validator for
   * {@link ConstraintCompositionTest#testAllComposingConstraintsMustBeApplicableToAnnotatedType()}
   */
  @GwtValidation(value = {Shoe.class})
  public static interface MustBeApplicableValidator extends Validator {
  }

  @Override
  public AbstractGwtValidator createValidator() {
    return GWT.create(MustBeApplicableValidator.class);
  }
}