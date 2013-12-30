/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.text.shared;

/**
 * A simple renderer that uses {@link #toString()} of the object.
 */
public class ToStringRenderer extends AbstractRenderer<Object> {

  private static ToStringRenderer instance;

  /**
   * Returns a {@link ToStringRenderer} that uses empty string for {@code null} objects.
   */
  public static ToStringRenderer instance() {
    if (instance == null) {
      instance = new ToStringRenderer("");
    }
    return instance;
  }

  private final String textForNull;

  /**
   * Constructs a {@code ToStringRenderer} that uses custom {@code textForNull} for {@code null}
   * objects.
   */
  public ToStringRenderer(String textForNull) {
    this.textForNull = textForNull;
  }

  @Override
  public String render(Object object) {
    return object == null ? textForNull : object.toString();
  }
}
