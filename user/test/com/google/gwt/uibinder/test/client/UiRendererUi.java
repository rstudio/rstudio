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
package com.google.gwt.uibinder.test.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.text.shared.UiRenderer;

/**
 * Sample use of a {@code SafeHtmlRenderer} with no dependency on
 * com.google.gwt.user.
 */
public class UiRendererUi {
  /**
   * Resources for this template.
   */
  public interface Resources extends ClientBundle {
    @Source("UiRendererUi.css")
    Style style();
  }

  /**
   * CSS for this template.
   */
  public interface Style extends CssResource {
    String bodyColor();
    String bodyFont();
  }

  interface HtmlRenderer extends UiRenderer<String> { }
  private static final HtmlRenderer renderer = GWT.create(HtmlRenderer.class);

  public UiRendererUi() {
  }

  public SafeHtml render() {
    return renderer.render(null);
  }
}
