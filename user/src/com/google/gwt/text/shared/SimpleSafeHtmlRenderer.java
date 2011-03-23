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
package com.google.gwt.text.shared;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

/**
 * A simple {@link SafeHtmlRenderer} implementation that calls
 * {@link SafeHtmlUtils#fromString(String)} to escape its arguments.
 */
public class SimpleSafeHtmlRenderer implements SafeHtmlRenderer<String> {

  private static SimpleSafeHtmlRenderer instance;

  public static SimpleSafeHtmlRenderer getInstance() {
    if (instance == null) {
      instance = new SimpleSafeHtmlRenderer();
    }
    return instance;
  }

  private SimpleSafeHtmlRenderer() {
  }

  public SafeHtml render(String object) {
    return (object == null) ? SafeHtmlUtils.EMPTY_SAFE_HTML : SafeHtmlUtils.fromString(object);
  }

  public void render(String object, SafeHtmlBuilder appendable) {
    appendable.append(SafeHtmlUtils.fromString(object));
  }
}
