package org.hibernate.validator;

/**
 * Interface to represent the constants contained in resource bundle:
 * 'validation/ValidationMessages.properties'.
 */
public interface ValidationMessages extends com.google.gwt.i18n.client.ConstantsWithLookup {

  /**
   * Translated "must be false".
   *
   * @return translated "must be false"
   */
  @DefaultStringValue("must be false")
  @Key("javax.validation.constraints.AssertFalse.message")
  String javax_validation_constraints_AssertFalse_message();

  /**
   * Translated "must be true".
   *
   * @return translated "must be true"
   */
  @DefaultStringValue("must be true")
  @Key("javax.validation.constraints.AssertTrue.message")
  String javax_validation_constraints_AssertTrue_message();

  /**
   * Translated "must be less than or equal to {value}".
   *
   * @return translated "must be less than or equal to {value}"
   */
  @DefaultStringValue("must be less than or equal to {value}")
  @Key("javax.validation.constraints.DecimalMax.message")
  String javax_validation_constraints_DecimalMax_message();

  /**
   * Translated "must be greater than or equal to {value}".
   *
   * @return translated "must be greater than or equal to {value}"
   */
  @DefaultStringValue("must be greater than or equal to {value}")
  @Key("javax.validation.constraints.DecimalMin.message")
  String javax_validation_constraints_DecimalMin_message();

  /**
   * Translated "numeric value out of bounds (<{integer} digits>.<{fraction} digits> expected)".
   *
   * @return translated "numeric value out of bounds (<{integer} digits>.<{fraction} digits> expected)"
   */
  @DefaultStringValue("numeric value out of bounds (<{integer} digits>.<{fraction} digits> expected)")
  @Key("javax.validation.constraints.Digits.message")
  String javax_validation_constraints_Digits_message();

  /**
   * Translated "must be in the future".
   *
   * @return translated "must be in the future"
   */
  @DefaultStringValue("must be in the future")
  @Key("javax.validation.constraints.Future.message")
  String javax_validation_constraints_Future_message();

  /**
   * Translated "must be less than or equal to {value}".
   *
   * @return translated "must be less than or equal to {value}"
   */
  @DefaultStringValue("must be less than or equal to {value}")
  @Key("javax.validation.constraints.Max.message")
  String javax_validation_constraints_Max_message();

  /**
   * Translated "must be greater than or equal to {value}".
   *
   * @return translated "must be greater than or equal to {value}"
   */
  @DefaultStringValue("must be greater than or equal to {value}")
  @Key("javax.validation.constraints.Min.message")
  String javax_validation_constraints_Min_message();

  /**
   * Translated "may not be null".
   *
   * @return translated "may not be null"
   */
  @DefaultStringValue("may not be null")
  @Key("javax.validation.constraints.NotNull.message")
  String javax_validation_constraints_NotNull_message();

  /**
   * Translated "must be null".
   *
   * @return translated "must be null"
   */
  @DefaultStringValue("must be null")
  @Key("javax.validation.constraints.Null.message")
  String javax_validation_constraints_Null_message();

  /**
   * Translated "must be in the past".
   *
   * @return translated "must be in the past"
   */
  @DefaultStringValue("must be in the past")
  @Key("javax.validation.constraints.Past.message")
  String javax_validation_constraints_Past_message();

  /**
   * Translated "must match \"{regexp}\"".
   *
   * @return translated "must match \"{regexp}\""
   */
  @DefaultStringValue("must match \"{regexp}\"")
  @Key("javax.validation.constraints.Pattern.message")
  String javax_validation_constraints_Pattern_message();

  /**
   * Translated "size must be between {min} and {max}".
   *
   * @return translated "size must be between {min} and {max}"
   */
  @DefaultStringValue("size must be between {min} and {max}")
  @Key("javax.validation.constraints.Size.message")
  String javax_validation_constraints_Size_message();

  /**
   * Translated "invalid credit card number".
   *
   * @return translated "invalid credit card number"
   */
  @DefaultStringValue("invalid credit card number")
  @Key("org.hibernate.validator.constraints.CreditCardNumber.message")
  String org_hibernate_validator_constraints_CreditCardNumber_message();

  /**
   * Translated "not a well-formed email address".
   *
   * @return translated "not a well-formed email address"
   */
  @DefaultStringValue("not a well-formed email address")
  @Key("org.hibernate.validator.constraints.Email.message")
  String org_hibernate_validator_constraints_Email_message();

  /**
   * Translated "length must be between {min} and {max}".
   *
   * @return translated "length must be between {min} and {max}"
   */
  @DefaultStringValue("length must be between {min} and {max}")
  @Key("org.hibernate.validator.constraints.Length.message")
  String org_hibernate_validator_constraints_Length_message();

  /**
   * Translated "may not be empty".
   *
   * @return translated "may not be empty"
   */
  @DefaultStringValue("may not be empty")
  @Key("org.hibernate.validator.constraints.NotBlank.message")
  String org_hibernate_validator_constraints_NotBlank_message();

  /**
   * Translated "may not be empty".
   *
   * @return translated "may not be empty"
   */
  @DefaultStringValue("may not be empty")
  @Key("org.hibernate.validator.constraints.NotEmpty.message")
  String org_hibernate_validator_constraints_NotEmpty_message();

  /**
   * Translated "must be between {min} and {max}".
   *
   * @return translated "must be between {min} and {max}"
   */
  @DefaultStringValue("must be between {min} and {max}")
  @Key("org.hibernate.validator.constraints.Range.message")
  String org_hibernate_validator_constraints_Range_message();

  /**
   * Translated "script expression \"{script}\" didn't evaluate to true".
   *
   * @return translated "script expression \"{script}\" didn't evaluate to true"
   */
  @DefaultStringValue("script expression \"{script}\" didn't evaluate to true")
  @Key("org.hibernate.validator.constraints.ScriptAssert.message")
  String org_hibernate_validator_constraints_ScriptAssert_message();

  /**
   * Translated "must be a valid URL".
   *
   * @return translated "must be a valid URL"
   */
  @DefaultStringValue("must be a valid URL")
  @Key("org.hibernate.validator.constraints.URL.message")
  String org_hibernate_validator_constraints_URL_message();
}
