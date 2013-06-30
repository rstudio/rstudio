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
package com.google.gwt.safehtml.shared;

/**
 * A string wrapped as an object of type {@link SafeHtml}.
 *
 * <p>
 * This class is package-private and intended for internal use by the
 * {@link com.google.gwt.safehtml} package.
 *
 * <p>
 * All implementors must implement .equals and .hashCode so that they operate
 * just like String.equals() and String.hashCode().
 */
class SafeHtmlString implements SafeHtml {
  private String html;

  /**
   * Constructs a {@link SafeHtmlString} from a string. Callers are responsible
   * for ensuring that the string passed as the argument to this constructor
   * satisfies the constraints of the contract imposed by the {@link SafeHtml}
   * interface.
   *
   * @param html the string to be wrapped as a {@link SafeHtml}
   */
  SafeHtmlString(String html) {
    if (html == null) {
      throw new NullPointerException("html is null");
    }
    this.html = html;
  }

  /**
   * No-arg constructor for compatibility with GWT serialization.
   */
  @SuppressWarnings("unused")
  private SafeHtmlString() {
  }

  /**
   * {@inheritDoc}
   */
  public String asString() {
    return html;
  }

  /**
   * Compares this string to the specified object.
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SafeHtml)) {
      return false;
    }
    return html.equals(((SafeHtml) obj).asString());
  }

  /**
   * Returns a hash code for this string.
   */
  @Override
  public int hashCode() {
    return html.hashCode();
  }

  @Override
  public String toString() {
    return "safe: \"" + asString() + "\"";
  }
}
