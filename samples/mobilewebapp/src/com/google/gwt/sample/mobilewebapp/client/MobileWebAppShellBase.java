/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.sample.mobilewebapp.client;

import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ResizeComposite;

/**
 * Base class for UI shell.
 */
public abstract class MobileWebAppShellBase extends ResizeComposite implements
    MobileWebAppShell {

  /**
   * Calculate the orientation based on the screen dimensions.
   * 
   * @return true if portrait, false if lansdcape
   */
  private static boolean calculateOrientationPortrait() {
    return Window.getClientHeight() > Window.getClientWidth();
  }

  /**
   * The registration for the window resize handler.
   */
  private HandlerRegistration windowResizeHandler;

  /**
   * The current orientation of the app.
   */
  private boolean isOrientationPortrait;

  /**
   * Adjust the orientation based on the current window height and width. This
   * is a no-op by default, but subclasses can override it.
   * 
   * @param isPortrait true if in portrait orientation, false if landscape
   */
  protected void adjustOrientation(boolean isPortrait) {
    // No-op by default.
  }

  /**
   * Check if we are in portrait or landscape orientation.
   * 
   * @return true if portrait, false if lansdcape
   */
  protected boolean isOrientationPortrait() {
    return isOrientationPortrait;
  }

  @Override
  protected void onLoad() {
    super.onLoad();

    /*
     * Listen for changes in the window size so we can adjust the orientation of
     * the app. This will also catch orientation changes on mobile devices.
     */
    windowResizeHandler = Window.addResizeHandler(new ResizeHandler() {
      public void onResize(ResizeEvent event) {
        if (isOrientationPortrait != calculateOrientationPortrait()) {
          isOrientationPortrait = !isOrientationPortrait;
          adjustOrientation(isOrientationPortrait);
        }
      }
    });

    // Initialize the orientation. Do not animate when we first load the shell.
    isOrientationPortrait = calculateOrientationPortrait();
    adjustOrientation(isOrientationPortrait);
  }

  @Override
  protected void onUnload() {
    super.onUnload();
    windowResizeHandler.removeHandler();
    windowResizeHandler = null;
  }
}
