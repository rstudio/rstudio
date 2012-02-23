/**
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


/**
 * Defines additional attributes that are interpreted by readers. Such an attribute is
 * 'tabindex' which indicates the tab order position of the element.
 *
 * @param <T> The extra attribute value type
 */
public final class ExtraAttribute<T> extends Attribute<T> {
  public static final ExtraAttribute<Integer> TABINDEX =
      new ExtraAttribute<Integer>("tabIndex", "");


  public ExtraAttribute(String name) {
    super(name);
  }

  public ExtraAttribute(String name, String defaultValue) {
    super(name, defaultValue);
  }
}