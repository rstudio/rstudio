package com.google.gwt.sample.validationtck.messageinterpolation;

/**
 * Interface to represent the constants contained in resource bundle:
 * 'ValidationMessages.properties'.
 */
public interface ValidationMessages extends com.google.gwt.i18n.client.ConstantsWithLookup {

  /**
   * Translated "replacement worked".
   *
   * @return translated "replacement worked"
   */
  @DefaultStringValue("replacement worked")
  @Key("foo")
  String foo();

  /**
   * Translated "may not be null".
   *
   * @return translated "may not be null"
   */
  @DefaultStringValue("may not be null")
  @Key("javax.validation.constraints.NotNull.message")
  String javax_validation_constraints_NotNull_message();

  /**
   * Translated "{replace.in.user.bundle2}".
   *
   * @return translated "{replace.in.user.bundle2}"
   */
  @DefaultStringValue("{replace.in.user.bundle2}")
  @Key("replace.in.user.bundle1")
  String replace_in_user_bundle1();

  /**
   * Translated "recursion worked".
   *
   * @return translated "recursion worked"
   */
  @DefaultStringValue("recursion worked")
  @Key("replace.in.user.bundle2")
  String replace_in_user_bundle2();
}
