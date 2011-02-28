package org.hibernate.jsr303.tck.tests.constraints.validatorresolution;

import com.google.gwt.core.client.GWT;
import com.google.gwt.validation.client.AbstractGwtValidatorFactory;
import com.google.gwt.validation.client.GwtValidation;
import com.google.gwt.validation.client.impl.AbstractGwtValidator;

import javax.validation.Validator;

/**
 * {@link AbstractGwtValidatorFactory} implementation that uses
 * {@link com.google.gwt.validation.client.GwtValidation GwtValidation}.
 */
public final class AmbiguousValidatorFactory extends
    AbstractGwtValidatorFactory {

  /**
   * Validator for
   * {@link ValidatorResolutionTest#testAmbiguousValidatorResolution()}
   */
  @GwtValidation(value = {Foo.class})
  public static interface AmbiguousValidator extends Validator {
  }

  @Override
  public AbstractGwtValidator createValidator() {
    return GWT.create(AmbiguousValidator.class);
  }
}