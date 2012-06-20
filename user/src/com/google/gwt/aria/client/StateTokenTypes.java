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

import com.google.gwt.aria.client.CommonAttributeTypes.AriaAttributeType;

/**
 * Token type enums for State ARIA attributes
 */
public final class StateTokenTypes {
  /**
   * CheckedToken type for the 'aria-checked' state.
   */
  public static enum CheckedToken implements AriaAttributeType {
    TRUE, FALSE, MIXED, UNDEFINED;

    /**
     * Gets the enum constant corresponding to {@code value} for the token type
     * CheckedToken.
     *
     * @param value Boolean value for which we want to get the corresponding enum constant.
     */
    public static CheckedToken of(boolean value) {
      return value ? TRUE : FALSE;
    }

    @Override
    public String getAriaValue() {
      switch (this) {
        case TRUE:
          return "true";
        case FALSE:
          return "false";
        case MIXED:
          return "mixed";
        case UNDEFINED:
          return "undefined";
      }
      return null; // not reachable
    }
  }

  /**
   * ExpandedToken type for the 'aria-expanded' state.
   */
  public static enum ExpandedToken implements AriaAttributeType {
    TRUE, FALSE, UNDEFINED;

    /**
     * Gets the enum constant corresponding to {@code value} for the token type
     * ExpandedToken.
     *
     * @param value Boolean value for which we want to get the corresponding enum constant.
     */
    public static ExpandedToken of(boolean value) {
      return value ? TRUE : FALSE;
    }

    @Override
    public String getAriaValue() {
      switch (this) {
        case TRUE:
          return "true";
        case FALSE:
          return "false";
        case UNDEFINED:
          return "undefined";
      }
      return null; // not reachable
    }
  }

  /**
   * GrabbedToken type for the 'aria-grabbed' state.
   */
  public static enum GrabbedToken implements AriaAttributeType {
    TRUE, FALSE, UNDEFINED;

    /**
     * Gets the enum constant corresponding to {@code value} for the token type
     * GrabbedToken.
     *
     * @param value Boolean value for which we want to get the corresponding enum constant.
     */
    public static GrabbedToken of(boolean value) {
      return value ? TRUE : FALSE;
    }

    @Override
    public String getAriaValue() {
      switch (this) {
        case TRUE:
          return "true";
        case FALSE:
          return "false";
        case UNDEFINED:
          return "undefined";
      }
      return null; // not reachable
    }
  }

  /**
   * InvalidToken type for the 'aria-invalid' state.
   */
  public static enum InvalidToken implements AriaAttributeType {
    GRAMMAR, FALSE, SPELLING, TRUE;

    @Override
    public String getAriaValue() {
      switch (this) {
        case GRAMMAR:
          return "grammar";
        case FALSE:
          return "false";
        case SPELLING:
          return "spelling";
        case TRUE:
          return "true";
      }
      return null; // not reachable
    }
  }

  /**
   * PressedToken type for the 'aria-pressed' state.
   */
  public static enum PressedToken implements AriaAttributeType {
    TRUE, FALSE, MIXED, UNDEFINED;

    /**
     * Gets the enum constant corresponding to {@code value} for the token type
     * PressedToken.
     *
     * @param value Boolean value for which we want to get the corresponding enum constant.
     */
    public static PressedToken of(boolean value) {
      return value ? TRUE : FALSE;
    }

    @Override
    public String getAriaValue() {
      switch (this) {
        case TRUE:
          return "true";
        case FALSE:
          return "false";
        case MIXED:
          return "mixed";
        case UNDEFINED:
          return "undefined";
      }
      return null; // not reachable
    }
  }

  /**
   * SelectedToken type for the 'aria-selected' state.
   */
  public static enum SelectedToken implements AriaAttributeType {
    TRUE, FALSE, UNDEFINED;

    /**
     * Gets the enum constant corresponding to {@code value} for the token type
     * SelectedToken.
     *
     * @param value Boolean value for which we want to get the corresponding enum constant.
     */
    public static SelectedToken of(boolean value) {
      return value ? TRUE : FALSE;
    }

    @Override
    public String getAriaValue() {
      switch (this) {
        case TRUE:
          return "true";
        case FALSE:
          return "false";
        case UNDEFINED:
          return "undefined";
      }
      return null; // not reachable
    }
  }
}
