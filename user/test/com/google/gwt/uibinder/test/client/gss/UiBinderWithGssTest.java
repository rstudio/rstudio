/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.uibinder.test.client.gss;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.uibinder.test.client.gss.WidgetUsingCss.CssStyle;
import com.google.gwt.uibinder.test.client.gss.WidgetUsingGss.GssStyle;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Test case for GSS integration in UiBinder.
 */
public class UiBinderWithGssTest extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "com.google.gwt.uibinder.test.UiBinderWithGss";
  }

  public void testGssIntegration() {
    WidgetUsingGss widgetUsingGss = new WidgetUsingGss();
    GssStyle style = widgetUsingGss.style;
    RootPanel.get().add(widgetUsingGss);

    assertEquals(style.main(), widgetUsingGss.getStyleName());
    String expectedCss = "." + style.main() + "{background-color:black;color:white;width:200px;" +
        "height:300px}";
    assertEquals(expectedCss, widgetUsingGss.style.getText());
  }

  public void testCssConversion() {
    WidgetUsingCss widgetUsingCss = new WidgetUsingCss();
    CssStyle style = widgetUsingCss.style;
    RootPanel.get().add(widgetUsingCss);

    assertEquals(style.main(), widgetUsingCss.getStyleName());
    String expectedCss = "." + style.main() + "{background-color:black;color:white}";
    assertEquals(expectedCss, widgetUsingCss.style.getText());
  }
}
