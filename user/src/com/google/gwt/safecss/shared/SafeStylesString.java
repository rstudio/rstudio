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
package com.google.gwt.safecss.shared;

/**
 * A string wrapped as an object of type {@link SafeStyles}.
 * 
 * <p>
 * This class is package-private and intended for internal use by the
 * {@link com.google.gwt.safecss} package.
 * 
 * <p>
 * All implementors must implement .equals and .hashCode so that they operate
 * just like String.equals() and String.hashCode().
 */
class SafeStylesString implements SafeStyles {

  private String css;

  /**
   * Constructs a {@link SafeStylesString} from a string. Callers are
   * responsible for ensuring that the string passed as the argument to this
   * constructor satisfies the constraints of the contract imposed by the
   * {@link SafeStyles} interface.
   * 
   * @param css the string to be wrapped as a {@link SafeStyles}
   */
  SafeStylesString(String css) {
    SafeStylesUtils.verifySafeStylesConstraints(css);
    this.css = css;
  }

  /**
   * No-arg constructor for compatibility with GWT serialization.
   */
  @SuppressWarnings("unused")
  private SafeStylesString() {
  }

  /**
   * {@inheritDoc}
   */
  public String asString() {
    return css;
  }

  /**
   * Compares this string to the specified object.
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SafeStyles)) {
      return false;
    }
    return css.equals(((SafeStyles) obj).asString());
  }

  /**
   * Returns a hash code for this string.
   */
  @Override
  public int hashCode() {
    return css.hashCode();
  }
}
