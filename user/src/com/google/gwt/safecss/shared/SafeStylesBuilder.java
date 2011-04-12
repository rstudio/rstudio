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
 * A builder that facilitates the building up of XSS-safe CSS attribute strings
 * from {@link SafeStyles}. It is used essentially like a {@link StringBuilder},
 * but access {@link SafeStyles} instead of Strings.
 * 
 * <p>
 * The accumulated XSS-safe {@link SafeStyles} can be obtained in the form of a
 * {@link SafeStyles} via the {@link #toSafeStyles()} method.
 * 
 * <p>
 * This class is not thread-safe.
 */
public final class SafeStylesBuilder {

  private final StringBuilder sb = new StringBuilder();

  /**
   * Constructs an empty {@link SafeStylesBuilder}.
   */
  public SafeStylesBuilder() {
  }

  /**
   * Appends the contents of another {@link SafeStyles} object, without applying
   * any escaping or sanitization to it.
   * 
   * @param styles the {@link SafeStyles} to append
   * @return a reference to this object
   */
  public SafeStylesBuilder append(SafeStyles styles) {
    sb.append(styles.asString());
    return this;
  }

  /**
   * <p>
   * Appends {@link SafeStyles} constructed from a trusted string, i.e., without
   * escaping the string. Only minimal checks are performed. The calling code
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
   * @param styles the input String
   * @return a {@link SafeStyles} instance
   */
  public SafeStylesBuilder appendTrustedString(String styles) {
    SafeStylesUtils.verifySafeStylesConstraints(styles);
    sb.append(styles);
    return this;
  }

  /**
   * Returns the safe CSS properties accumulated in the builder as a
   * {@link SafeStyles}.
   * 
   * @return a {@link SafeStyles} instance
   */
  public SafeStyles toSafeStyles() {
    return new SafeStylesString(sb.toString());
  }
}
