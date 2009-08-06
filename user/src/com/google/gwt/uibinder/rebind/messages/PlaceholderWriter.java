/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.uibinder.rebind.messages;

/**
 * Represents a parameter in a Messages interface method, and
 * can write out its declaration.
 */
class PlaceholderWriter {
  private final String name;
  private final String example;
  private final String value;

  /**
   * @param name Parameter name for this placeholder
   * @param example Contents of the {@literal @}Example annotation
   * @param value The value to provide for this param when writing
   * an invocation of its message method.
   */
  public PlaceholderWriter(String name, String example, String value) {
    this.name = name;
    this.example = inQuotes(example);
    this.value = inQuotes(value);
  }

  public String getDeclaration() {
    return String.format("@Example(%s) String %s", example, name);
  }

  public String getValue() {
    return value;
  }

  private String inQuotes(String s) {
    return String.format("\"%s\"", s);
  }
}
