/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.user.client.ui.Label;

/**
 * Introduction page.
 */
public class Info extends Sink {

  public static SinkInfo init() {
    return new SinkInfo("Intro", "<h2>Introduction to the Kitchen Sink</h2>" +
            "<p>This is the Kitchen Sink sample.  "
        + "It demonstrates many of the widgets in the Google Web Toolkit."
        + "<p>This sample also demonstrates something else really useful in GWT: "
        + "history support.  "
        + "When you click on a tab, the location bar will be "
        + "updated with the current <i>history token</i>, which keeps the app "
        + "in a bookmarkable state.  The back and forward buttons work properly "
        + "as well.  Finally, notice that you can right-click a tab and 'open "
        + "in new window' (or middle-click for a new tab in Firefox).</p></p>") {

      public Sink createInstance() {
        return new Info();
      }
    };
  }

  public Info() {
    initWidget(new Label());
  }

  public void onShow() {
  }
}
