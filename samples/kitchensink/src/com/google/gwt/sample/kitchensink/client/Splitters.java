/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.sample.kitchensink.client;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.VerticalSplitPanel;

/**
 * Demonstrates the horizontal and vertical split panels.
 */
public class Splitters extends Sink {

  public static SinkInfo init() {
    return new SinkInfo("Splitters",
        "GWT includes horizontal and vertical split panels, which can be used "
            + "to create user-sizable regions in your application.") {

      public Sink createInstance() {
        return new Splitters();
      }
    };
  }

  private HorizontalSplitPanel hsplit = new HorizontalSplitPanel();
  private VerticalSplitPanel vsplit = new VerticalSplitPanel();

  public Splitters() {
    hsplit.setLeftWidget(new HTML("Left side of a horizontal split panel."));
    hsplit.setRightWidget(vsplit);

    vsplit.setTopWidget(new HTML("Top of a vertical split panel."));
    vsplit.setBottomWidget(new HTML("Bottom of a vertical split panel."));

    hsplit.setSize("30em", "10em");
    vsplit.setSize("100%", "100%");
    initWidget(hsplit);
  }
}
