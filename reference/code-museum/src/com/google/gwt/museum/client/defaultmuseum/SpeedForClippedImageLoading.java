/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.museum.client.defaultmuseum;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.museum.client.common.ControlInputPanel;
import com.google.gwt.museum.client.common.SimpleLogger;
import com.google.gwt.museum.client.common.ControlInputPanel.IntegerInput;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TreeImages;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the speed of clipped images.
 * 
 */
public class SpeedForClippedImageLoading extends AbstractIssue {
  private SimpleLogger log = new SimpleLogger();
  List<Image> images = new ArrayList<Image>();

  @Override
  public Widget createIssue() {
    VerticalPanel v = new VerticalPanel();
    ControlInputPanel p = new ControlInputPanel();
    v.add(p);
    v.add(log);
    final IntegerInput size = new IntegerInput("how many clipped images", 10,
        30, p);

    Button createClippedImages = new Button("time the creation",
        new ClickHandler() {
          boolean firstTime = true;

          public void onClick(ClickEvent event) {
            final Duration d = new Duration();

            final int numImages = size.getValue();
            final TreeImages test = GWT.<TreeImages> create(TreeImages.class);

            for (int i = 0; i < numImages; i++) {
              Image current = test.treeClosed().createImage();

              if (i == numImages - 1) {
                current.addLoadHandler(new LoadHandler() {
                  public void onLoad(LoadEvent event) {
                    if (firstTime) {
                      log.report("Took " + d.elapsedMillis()
                          + " milliseconds to create the images");
                    }
                    firstTime = false;
                  }
                });
              }
              images.add(current);
              RootPanel.get().add(current);
            }
          }
        });

    v.add(createClippedImages);
    return v;
  }

  @Override
  public String getInstructions() {
    return "IE has traditionally been very slow compared to the other browsers,  this speed test in intended to allow us to capture and document improvements in the speed of clipped images";
  }

  @Override
  public String getSummary() {
    return "clear() speed check";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

}
