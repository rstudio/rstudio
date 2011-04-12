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
 * Utility class containing static methods for creating {@link SafeStyles}.
 */
public final class SafeStylesUtils {
  /*
   * TODO(jlabanca): add context specific utility methods to create SafeStyles
   * (ex. #forHeight(double height, Unit unit).
   */

  /**
   * <p>
   * Returns a {@link SafeStyles} constructed from a trusted string, i.e.,
   * without escaping the string. No checks are performed. The calling code
   * should be carefully reviewed to ensure the argument meets the
   * {@link SafeStyles} contract.
   * 
   * <p>
   * Generally, {@link SafeStyles} should be of the form
   * {@code cssPropertyName:value;}, where neither the name nor the value
   * contain malicious scripts.
   * 
   * <p>
   * {@link SafeStyles} may never contain literal angle brackets. Otherwise, it
   * could be unsafe to place a {@link SafeStyles} into a &lt;style&gt; tag
   * (where it can't be HTML escaped). For example, if the {@link SafeStyles}
   * containing "
   * <code>font: 'foo &lt;style&gt;&lt;script&gt;evil&lt;/script&gt;</code>'" is
   * used in a style sheet in a &lt;style&gt; tag, this could then break out of
   * the style context into HTML.
   * 
   * <p>
   * The following example values comply with this type's contract:
   * <ul>
   * <li><code>width: 1em;</code></li>
   * <li><code>height:1em;</code></li>
   * <li><code>width: 1em;height: 1em;</code></li>
   * <li><code>background:url('http://url');</code></li>
   * </ul>
   * In addition, the empty string is safe for use in a CSS attribute.
   * 
   * <p>
   * The following example values do <em>not</em> comply with this type's contract:
   * <ul>
   * <li><code>background: red</code> (missing a trailing semi-colon)</li>
   * <li><code>background:</code> (missing a value and a trailing semi-colon)</li>
   * <li><code>1em</code> (missing an attribute name, which provides context for the value)</li>
   * </ul>
   * 
   * @param s the input String
   * @return a {@link SafeStyles} instance
   */
  public static SafeStyles fromTrustedString(String s) {
    return new SafeStylesString(s);
  }

  /**
   * Verify that the basic constraints of a {@link SafeStyles} are met. This
   * method is not a guarantee that the specified css is safe for use in a CSS
   * style attribute. It is a minimal set of assertions to check for common
   * errors.
   * 
   * @param styles the CSS properties string
   * @throws NullPointerException if the css is null
   * @throws AssertionError if the css does not meet the contraints
   */
  static void verifySafeStylesConstraints(String styles) {
    if (styles == null) {
      throw new NullPointerException("css is null");
    }

    // CSS properties must end in a semi-colon or they cannot be safely
    // composed with other properties.
    assert ((styles.trim().length() == 0) || styles.endsWith(";")) : "Invalid CSS Property: '"
        + styles + "'. CSS properties must be an empty string or end with a semi-colon (;).";
    assert !styles.contains("<") && !styles.contains(">") : "Invalid CSS Property: '" + styles
        + "'. CSS should not contain brackets (< or >).";
  }

  // prevent instantiation
  private SafeStylesUtils() {
  }
}
