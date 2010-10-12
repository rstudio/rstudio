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
 * This class is intended only for use in generated code where the code
 * generator guarantees that instances of this type will adhere to the
 * {@link SafeHtml} contract (hence the purposely unwieldy class name).
 */
public class OnlyToBeUsedInGeneratedCodeStringBlessedAsSafeHtml
    implements SafeHtml {
  private String html;

  /**
   * Constructs an instance from a given HTML String.
   *
   * @param html an HTML String that is assumed to be safe
   */
  public OnlyToBeUsedInGeneratedCodeStringBlessedAsSafeHtml(String html) {
    if (html == null) {
      throw new NullPointerException("html is null");
    }
    this.html = html;
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
}
