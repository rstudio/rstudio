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

import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Demonstrates {"@link com.google.gwt.user.client.ui.TabPanel}.
 */
public class Tabs extends Sink {

  public static SinkInfo init() {
    return new SinkInfo("Tabs",
      "GWT's built-in <code>TabPanel</code> class makes it easy to build tabbed dialogs "
        + "and the like.  Notice that no page load occurs when you select the "
        + "different tabs in this page.  That's the magic of dynamic HTML.") {
      public Sink createInstance() {
        return new Tabs();
      }
    };
  }

  private TabPanel tabs = new TabPanel();

  public Tabs() {
    tabs.add(createImage("rembrandt/JohannesElison.jpg"), "1634");
    tabs.add(createImage("rembrandt/SelfPortrait1640.jpg"), "1640");
    tabs.add(createImage("rembrandt/LaMarcheNocturne.jpg"), "1642");
    tabs.add(createImage("rembrandt/TheReturnOfTheProdigalSon.jpg"), "1662");
    tabs.selectTab(0);

    tabs.setWidth("100%");
    tabs.setHeight("100%");
    initWidget(tabs);
  }

  public void onShow() {
  }

  private Widget createImage(String imageUrl) {
    Image image = new Image(imageUrl);
    image.setStyleName("ks-images-Image");

    VerticalPanel p = new VerticalPanel();
    p.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
    p.setVerticalAlignment(VerticalPanel.ALIGN_MIDDLE);
    p.add(image);
    return p;
  }
}
