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
package com.google.gwt.sample.showcase.client.content.other;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.Showcase;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Example file.
 */
public class CwAnimation extends ContentWidget {
  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants,
      ContentWidget.CwConstants {
    String cwAnimationDescription();

    String cwAnimationName();
  }

  /**
   * A custom animation that moves a small image around a circle in an
   * {@link AbsolutePanel}.
   */
  @ShowcaseSource
  public class CustomAnimation extends Animation {
    /**
     * The x-coordinate of the center of the circle.
     */
    private int centerX = 120;

    /**
     * The y-coordinate of the center of the circle.
     */
    private int centerY = 120;

    /**
     * The radius of the circle.
     */
    private int radius = 100;

    @Override
    protected void onComplete() {
      super.onComplete();
      startButton.setEnabled(true);
      cancelButton.setEnabled(false);
    }

    @Override
    protected void onStart() {
      super.onStart();
      startButton.setEnabled(false);
      cancelButton.setEnabled(true);
    }

    @Override
    protected void onUpdate(double progress) {
      double radian = 2 * Math.PI * progress;
      updatePosition(animateeLeft, radian, 0);
      updatePosition(animateeBottom, radian, 0.5 * Math.PI);
      updatePosition(animateeRight, radian, Math.PI);
      updatePosition(animateeTop, radian, 1.5 * Math.PI);
    }

    /**
     * Update the position of the widget, adding a rotational offset.
     * 
     * @param w the widget to move
     * @param radian the progress in radian
     * @param offset the offset in radian
     */
    private void updatePosition(Widget w, double radian, double offset) {
      radian += offset;
      double x = radius * Math.cos(radian) + centerX;
      double y = radius * Math.sin(radian) + centerY;
      absolutePanel.setWidgetPosition(w, (int) x, (int) y);
    }
  }

  /**
   * The absolute panel used in the example.
   */
  @ShowcaseData
  private AbsolutePanel absolutePanel = null;

  /**
   * The widget that is being animated.
   */
  @ShowcaseData
  private Widget animateeBottom = null;

  /**
   * The widget that is being animated.
   */
  @ShowcaseData
  private Widget animateeLeft = null;

  /**
   * The widget that is being animated.
   */
  @ShowcaseData
  private Widget animateeRight = null;

  /**
   * The widget that is being animated.
   */
  @ShowcaseData
  private Widget animateeTop = null;

  /**
   * The instance of an animation.
   */
  @ShowcaseData
  private CustomAnimation animation = null;

  /**
   * The {@link Button} used to cancel the {@link Animation}.
   */
  @ShowcaseData
  private Button cancelButton = null;

  /**
   * An instance of the constants.
   */
  @ShowcaseData
  private CwConstants constants;

  /**
   * The {@link Button} used to start the {@link Animation}.
   */
  @ShowcaseData
  private Button startButton = null;

  /**
   * Constructor.
   * 
   * @param constants the constants
   */
  public CwAnimation(CwConstants constants) {
    super(constants);
    this.constants = constants;
  }

  @Override
  public String getDescription() {
    return constants.cwAnimationDescription();
  }

  @Override
  public String getName() {
    return constants.cwAnimationName();
  }

  @Override
  public boolean hasStyle() {
    return false;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    // Create a new panel
    absolutePanel = new AbsolutePanel();
    absolutePanel.setSize("250px", "250px");
    absolutePanel.ensureDebugId("cwAbsolutePanel");

    // Add a widget that we will animate
    animateeTop = Showcase.images.gwtLogoThumb().createImage();
    animateeBottom = Showcase.images.gwtLogoThumb().createImage();
    animateeLeft = Showcase.images.gwtLogoThumb().createImage();
    animateeRight = Showcase.images.gwtLogoThumb().createImage();
    absolutePanel.add(animateeTop);
    absolutePanel.add(animateeBottom);
    absolutePanel.add(animateeLeft);
    absolutePanel.add(animateeRight);

    // Wrap the absolute panel in a DecoratorPanel
    DecoratorPanel absolutePanelWrapper = new DecoratorPanel();
    absolutePanelWrapper.setWidget(absolutePanel);

    // Create the options bar
    DecoratorPanel optionsWrapper = new DecoratorPanel();
    optionsWrapper.setWidget(createOptionsBar());

    // Add the components to a panel and return it
    HorizontalPanel mainLayout = new HorizontalPanel();
    mainLayout.setSpacing(10);
    mainLayout.add(optionsWrapper);
    mainLayout.add(absolutePanelWrapper);

    // Create the custom animation
    animation = new CustomAnimation();

    // Set the start position of the widgets
    animation.onComplete();

    // Return the layout
    return mainLayout;
  }

  /**
   * Create an options panel that allows users to select a widget and reposition
   * it.
   * 
   * @return the new options panel
   */
  @ShowcaseSource
  private Widget createOptionsBar() {
    // Create a panel to move components around
    VerticalPanel optionsBar = new VerticalPanel();
    optionsBar.setSpacing(5);
    optionsBar.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

    // Add a title
    optionsBar.add(new HTML("<b>Animation Options</b>"));

    // Add start button
    startButton = new Button("Start");
    startButton.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        animation.run(2000);
      }
    });
    optionsBar.add(startButton);

    // Add cancel button
    cancelButton = new Button("Cancel");
    cancelButton.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        animation.cancel();
      }
    });
    optionsBar.add(cancelButton);

    // Return the options bar
    return optionsBar;
  }

}
