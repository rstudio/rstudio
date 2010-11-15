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
package com.google.gwt.resources.client;

import com.google.gwt.core.client.GWT;

/**
 * Generally useful styles and resources used throughout GWT widgets and cells.
 */
public class CommonResources {

  /**
   * The {@link ClientBundle} of resources.
   */
  static interface Bundle extends ClientBundle {

    @Source("inline-block.css")
    InlineBlockStyle inlineBlockStyle();
  }

  /**
   * Cross-browser implementation of the "display: inline-block" CSS property.
   */
  static interface InlineBlockStyle extends CssResource {

    /**
     * The inline block style.
     */
    String inlineBlock();
  }

  /**
   * Lazily loaded singleton.
   */
  private static Bundle instance;

  /**
   * Ensure that the shared {@link Bundle} is created and return it.
   * 
   * @return the {@link Bundle} of resources
   */
  private static Bundle ensureResources() {
    if (instance == null) {
      instance = GWT.create(Bundle.class);
    }
    return instance;
  }

  /**
   * Get the style class name that simulates a "display: inline-block" effect
   * across browsers.
   */
  public static String getInlineBlockStyle() {
    InlineBlockStyle style = ensureResources().inlineBlockStyle();
    style.ensureInjected();
    return style.inlineBlock();
  }
}
