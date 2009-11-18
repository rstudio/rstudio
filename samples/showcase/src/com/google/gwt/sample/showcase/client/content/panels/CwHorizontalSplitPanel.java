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
package com.google.gwt.sample.showcase.client.content.panels;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseStyle;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 */
@ShowcaseStyle(".gwt-HorizontalSplitPanel")
public class CwHorizontalSplitPanel extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants,
      ContentWidget.CwConstants {
    String cwHorizontalSplitPanelDescription();

    String cwHorizontalSplitPanelName();

    String cwHorizontalSplitPanelText();
  }

  /**
   * An instance of the constants.
   */
  @ShowcaseData
  private CwConstants constants;

  /**
   * Constructor.
   * 
   * @param constants the constants
   */
  public CwHorizontalSplitPanel(CwConstants constants) {
    super(constants);
    this.constants = constants;
  }

  @Override
  public String getDescription() {
    return constants.cwHorizontalSplitPanelDescription();
  }

  @Override
  public String getName() {
    return constants.cwHorizontalSplitPanelName();
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    // Create a Horizontal Split Panel
    HorizontalSplitPanel hSplit = new HorizontalSplitPanel();
    hSplit.ensureDebugId("cwHorizontalSplitPanel");
    hSplit.setSize("500px", "350px");
    hSplit.setSplitPosition("30%");

    // Add some content
    String randomText = constants.cwHorizontalSplitPanelText();
    for (int i = 0; i < 2; i++) {
      randomText += randomText;
    }
    hSplit.setRightWidget(new HTML(randomText));
    hSplit.setLeftWidget(new HTML(randomText));

    // Wrap the split panel in a decorator panel
    DecoratorPanel decPanel = new DecoratorPanel();
    decPanel.setWidget(hSplit);

    // Return the content
    return decPanel;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwHorizontalSplitPanel.class, new RunAsyncCallback() {

      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }

  @Override
  protected void setRunAsyncPrefetches() {
    prefetchPanels();
  }
}
