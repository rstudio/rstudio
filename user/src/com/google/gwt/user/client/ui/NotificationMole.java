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
package com.google.gwt.user.client.ui;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;

/**
 * Simple widget for providing notification feedback.
 */
public class NotificationMole extends Composite {
  /**
   * Default CSS styles for this widget.
   */
  public interface Style extends CssResource {
    String container();

    String notificationText();
  }

  interface Binder extends UiBinder<HTMLPanel, NotificationMole> {
  }

  private class MoleAnimation extends Animation {
    private int endSize;
    private int startSize;

    @Override
    protected void onComplete() {
      if (endSize == 0) {
        borderElement.getStyle().setDisplay(Display.NONE);
        return;
      }
      borderElement.getStyle().setHeight(endSize, Unit.PX);
    }

    @Override
    protected void onUpdate(double progress) {
      double delta = (endSize - startSize) * progress;
      double newSize = startSize + delta;
      borderElement.getStyle().setHeight(newSize, Unit.PX);
    }

    void animateMole(int startSize, int endSize, int duration) {
      this.startSize = startSize;
      this.endSize = endSize;
      if (duration == 0) {
        onComplete();
        return;
      }
      run(duration);
    }
  }

  private static final Binder BINDER = GWT.create(Binder.class);

  @UiField()
  DivElement borderElement;

  @UiField
  DivElement heightMeasure;

  @UiField()
  SpanElement notificationText;

  int showAttempts = 0;

  Timer showTimer = new Timer() {
    @Override
    public void run() {
      if (showAttempts > 0) {
        showImpl();
      }
    }
  };

  private final MoleAnimation animation = new MoleAnimation();

  private int animationDuration;

  public NotificationMole() {
    initWidget(BINDER.createAndBindUi(this));
  }

  /**
   * Hides the notification.
   */
  public void hide() {
    if (showAttempts > 0) {
      --showAttempts;
    }
    if (showAttempts == 0) {
      animation.animateMole(heightMeasure.getOffsetHeight(), 0,
          animationDuration);
      return;
    }
  }

  /**
   * Force mole to hide and discard outstanding show attempts.
   */
  public void hideNow() {
    showAttempts = 0;
    animation.animateMole(heightMeasure.getOffsetHeight(), 0, animationDuration);
  }

  /**
   * Sets the animation duration in milliseconds. The animation duration
   * defaults to 0 if this method is never called.
   * 
   * @param duration the animation duration in milliseconds.
   */
  public void setAnimationDuration(int duration) {
    this.animationDuration = duration;
  }

  /**
   * Sets the message text to be displayed.
   * 
   * @param message the text to be displayed.
   */
  public void setMessage(String message) {
    notificationText.setInnerText(message);
  }

  /**
   * Display the notification with the existing message.
   */
  public void show() {
    ++showAttempts;
    showImpl();
  }

  /**
   * Set the message text and then display the notification.
   */
  public void show(String message) {
    setMessage(message);
    show();
  }

  /**
   * Display the notification, but after a delay.
   * 
   * @param delay delay in milliseconds.
   */
  public void showDelayed(int delay) {
    if (showAttempts == 0) {
      if (delay == 0) {
        show();
      } else {
        ++showAttempts;
        showTimer.schedule(delay);
      }
    }
  }

  private void showImpl() {
    borderElement.getStyle().setDisplay(Display.BLOCK);
    borderElement.getStyle().setWidth(notificationText.getOffsetWidth(),
        Unit.PX);
    animation.animateMole(0, heightMeasure.getOffsetHeight(), animationDuration);
  }
}
