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
package com.google.gwt.i18n.shared;

/**
 * Defines a way of choosing between alternate messages based on a parameter
 * value.
 */
public interface AlternateMessageSelector {

  /**
   * Name of the "other" form.
   */
  String OTHER_FORM_NAME = "other";

  /**
   * Represents an alternate form of a message.
   */
  public static class AlternateForm implements Comparable<AlternateForm> {

    private final String name;
    private final String description;
    private final boolean warnIfMissing;

    /**
     * Create the plural form.
     *
     * @param name
     * @param description
     */
    public AlternateForm(String name, String description) {
      this(name, description, true);
    }

    /**
     * Create the plural form.
     *
     * @param name
     * @param description
     * @param warnIfMissing if false, do not warn if this form is missing from a
     *     translation.  This is used for those cases where a plural form
     *     is defined for a language, but is very rarely used.
     */
    public AlternateForm(String name, String description,
        boolean warnIfMissing) {
      this.name = name;
      this.description = description;
      this.warnIfMissing = warnIfMissing;
    }

    public int compareTo(AlternateForm o) {
      return name.compareTo(o.name);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != getClass()) {
        return false;
      }
      return compareTo((AlternateForm) obj) == 0;
    }

    /**
     * Returns the description, suitable for describing this form to
     * translators.
     */
    public String getDescription() {
      return description;
    }

    /**
     * Returns the name.
     */
    public String getName() {
      return name;
    }

    /**
     * Returns true if the generator should warn if this plural form is not
     * present.
     */
    public boolean getWarnIfMissing() {
      return warnIfMissing;
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }

    @Override
    public String toString() {
      return name;
    }
  }

  AlternateForm OTHER_FORM = new AlternateForm(OTHER_FORM_NAME,
      "Default value if no other forms apply");

  /**
   * Check if a user-supplied form is acceptable for this alternate message
   * selector.
   * 
   * @param form
   * @return true if the form is acceptable, otherwise false
   */
  boolean isFormAcceptable(String form);
}