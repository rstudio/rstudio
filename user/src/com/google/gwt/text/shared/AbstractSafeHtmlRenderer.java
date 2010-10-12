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
 * Abstract implementation of a safe HTML renderer to make implementation of
 * rendering simpler.
 *
 * @param <T> the type to render
 */
public abstract class AbstractSafeHtmlRenderer<T> implements
    SafeHtmlRenderer<T> {

  private static final SafeHtml EMPTY_STRING = SafeHtmlUtils.fromSafeConstant("");

  public void render(T object, SafeHtmlBuilder appendable) {
    appendable.append(render(object));
  }

  protected SafeHtml toSafeHtml(Object obj) {
    return obj == null ? EMPTY_STRING
        : SafeHtmlUtils.fromString(String.valueOf(obj));
  }
}
