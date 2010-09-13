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
package com.google.gwt.sample.showcase.client.content.i18n;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.HasDirection.Direction;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseStyle;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 */
@ShowcaseStyle(/* css style names */{".gwt-TextArea"})
public class CwBidiInput extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {
    String cwBidiInputRtlAreaLabel();

    String cwBidiInputBidiAreaLabel();

    String cwBidiInputDescription();

    String cwBidiInputName();
  }

  /**
   * An instance of the constants.
   */
  @ShowcaseData
  private final CwConstants constants;

  /**
   * Constructor.
   *
   * @param constants the constants
   */
  public CwBidiInput(CwConstants constants) {
    super(
        constants.cwBidiInputName(), constants.cwBidiInputDescription(), true);
    this.constants = constants;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    // Create a panel to layout the widgets
    VerticalPanel vpanel = new VerticalPanel();
    vpanel.setSpacing(10);

    // Add a bidi-disabled TextArea
    vpanel.add(new HTML(constants.cwBidiInputRtlAreaLabel()));
    TextArea disabled = new TextArea();
    disabled.setDirectionEstimator(false);
    disabled.setDirection(Direction.RTL);
    vpanel.add(disabled);

    // Add a bidi-enabled TextArea
    vpanel.add(new HTML(constants.cwBidiInputBidiAreaLabel()));
    TextArea enabled = new TextArea();
    // Since this application includes at least one RTL locale, this TextArea
    // has automatic direction handling on by default.
    assert enabled.getDirectionEstimator() != null;
    enabled.setText("كم عدد حبات الرمل في الشاطئ?");
    vpanel.add(enabled);

    // Return the panel
    return vpanel;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwBidiInput.class, new RunAsyncCallback() {

      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }
}
