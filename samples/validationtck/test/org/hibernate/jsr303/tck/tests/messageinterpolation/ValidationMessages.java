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
package org.hibernate.jsr303.tck.tests.messageinterpolation;

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
