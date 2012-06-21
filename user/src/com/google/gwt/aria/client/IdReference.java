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
 * Id reference attribute type
 */
public class IdReference implements AriaAttributeType {
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
