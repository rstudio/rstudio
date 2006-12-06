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

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.LoadListener;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Demonstrates {@link com.google.gwt.user.client.ui.Image}.
 */
public class Images extends Sink implements ClickListener, LoadListener {

  private static final String[] sImages = new String[]{
    "rembrandt/JohannesElison.jpg", "rembrandt/LaMarcheNocturne.jpg",
    "rembrandt/SelfPortrait1628.jpg", "rembrandt/SelfPortrait1640.jpg",
    "rembrandt/TheArtistInHisStudio.jpg",
    "rembrandt/TheReturnOfTheProdigalSon.jpg"};

  public static SinkInfo init() {
    return new SinkInfo("Images",
      "This page demonstrates GWT's support for images.  Notice in "
        + "particular how it uses the image's onLoad event to display a 'wait "
        + "spinner' between the back and forward buttons.") {
      public Sink createInstance() {
        return new Images();
      }
    };
  }

  private int curImage;
  private Image image = new Image();
  private Image loadingImage = new Image("images/blanksearching.gif");
  private Image nextButton = new Image("rembrandt/forward.gif");
  private Image prevButton = new Image("rembrandt/back.gif");

  public Images() {
    image.addLoadListener(this);
    prevButton.addClickListener(this);
    nextButton.addClickListener(this);

    DockPanel topPanel = new DockPanel();
    topPanel.setVerticalAlignment(DockPanel.ALIGN_MIDDLE);
    topPanel.add(prevButton, DockPanel.WEST);
    topPanel.add(nextButton, DockPanel.EAST);
    topPanel.add(loadingImage, DockPanel.CENTER);

    VerticalPanel panel = new VerticalPanel();
    panel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
    panel
      .add(new HTML("<h2>A Bit of Rembrandt</h2>", true));
    panel.add(topPanel);
    panel.add(image);

    panel.setWidth("100%");
    initWidget(panel);
    image.setStyleName("ks-images-Image");
    nextButton.setStyleName("ks-images-Button");
    prevButton.setStyleName("ks-images-Button");

    loadImage(0);
  }

  public void onClick(Widget sender) {
    if (sender == prevButton)
      loadImage(curImage - 1);
    else if (sender == nextButton)
      loadImage(curImage + 1);
  }

  public void onError(Widget sender) {
  }

  public void onLoad(Widget sender) {
    loadingImage.setUrl("images/blanksearching.gif");
  }

  public void onShow() {
  }

  private void loadImage(int index) {
    if (index < 0)
      index = sImages.length - 1;
    else if (index > sImages.length - 1)
      index = 0;

    curImage = index;
    loadingImage.setUrl("images/searching.gif");
    image.setUrl(sImages[curImage]);
  }
}
