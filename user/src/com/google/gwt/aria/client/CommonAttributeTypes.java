/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.aria.client;
/////////////////////////////////////////////////////////
// This is auto-generated code.  Do not manually edit! //
/////////////////////////////////////////////////////////

/**
 * ARIA attributes common non primitive types as defined by the W3C specification
 * <a href="http://www.w3.org/TR/wai-aria/">W3C ARIA specification</a>.
 * Users should use the types defined in this class to get instances of {@link Tristate},
 * {@link BooleanAndUndefined}, {@link IdReference} and {@link IdReferenceList}.
 * For more details about ARIA states and properties check
 * <a href="http://www.w3.org/TR/wai-aria/states_and_properties"> Supported States and Properties
 * </a>.
 */
public final class CommonAttributeTypes {
  public static final String TRUE_VALUE = "true";
  public static final String FALSE_VALUE = "false";
  public static final String MIXED_TRISTATE_VALUE = "mixed";
  public static final String UNDEFINED_VALUE = "undefined";

  // This class cannot be instanted
  private CommonAttributeTypes() {
  }

  /**
   * Interface that is and needs to be implemented by ALL non primitive attribute types
   */
  public static interface AriaAttributeType {
    String getAriaValue();
  }

  /**
   * Tristate enum type
   */
  public static enum Tristate implements AriaAttributeType {
    TRUE(TRUE_VALUE), FALSE(FALSE_VALUE), MIXED(MIXED_TRISTATE_VALUE);

    public static Tristate of(boolean value) {
      return value ? TRUE : FALSE;
    }

    private final String value;

    private Tristate(String value) {
      this.value = value;
    }

    @Override
    public String getAriaValue() {
      return value;
    }
  }

  /**
   * Boolean and undefined enum type
   */
  public static enum BooleanAndUndefined implements AriaAttributeType {
    TRUE(TRUE_VALUE), FALSE(FALSE_VALUE), UNDEFINED(UNDEFINED_VALUE);

    public static BooleanAndUndefined of(boolean value) {
      return value ? TRUE : FALSE;
    }

    private final String value;

    private BooleanAndUndefined(String value) {
      this.value = value;
    }

    @Override
    public String getAriaValue() {
      return value;
    }
  }

  /**
   * Id reference type
   */
  public static class IdReference implements AriaAttributeType {
    public static IdReference of(String value) {
      return new IdReference(value);
    }

    private final String id;

    /**
     * An instance of {@link IdReference} is created.
     *
     * @param value String id value
     */
    private IdReference(String value) {
      this.id = value;
    }

    @Override
    public String getAriaValue() {
      return id;
    }
  }

  /**
   * Id reference list type
   */
  public static class IdReferenceList implements AriaAttributeType {
    public static IdReferenceList of(String... values) {
      return new IdReferenceList(values);
    }

    private final String ids;

    /**
     * An instance of {@link IdReferenceList} is created.
     *
     * @param values String ids array
     */
    private IdReferenceList(String... values) {
      assert values.length > 0 : "The ids cannot be empty";
      StringBuffer ariaValue = new StringBuffer();
      for (String value : values) {
        ariaValue.append(value).append(" ");
      }
      ids = ariaValue.toString().trim();
    }

    /**
     * Creates a String token list
     */
    @Override
    public String getAriaValue() {
      return ids;
    }
  }
}
