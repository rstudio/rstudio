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
package com.google.gwt.i18n.client;

import com.google.gwt.event.dom.client.HasKeyUpHandlers;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.HasDirection.Direction;
import com.google.gwt.i18n.shared.DirectionEstimator;
import com.google.gwt.i18n.shared.HasDirectionEstimator;
import com.google.gwt.i18n.shared.WordCountDirectionEstimator;
import com.google.gwt.user.client.ui.HasText;

/**
 * Utility class for handling auto-direction adjustment.
 *
 * This class is useful for automatically adjusting the direction of an object
 * that takes text input, while the text is being entered.
 */
public class AutoDirectionHandler implements KeyUpHandler,
    HasDirectionEstimator {

  /**
   * The interface an object must implement in order to add an
   * AutoDirectionHandler to it.
   *
   * TODO(tomerigo): add Paste and Input events once they're available in GWT.
   */
  public interface Target extends HasText, HasDirection, HasKeyUpHandlers {
  }

  /**
   * Operates like {@link #addTo(Target, DirectionEstimator)}, but always uses
   * a default DirectionEstimator, {@link
   * com.google.gwt.i18n.shared.WordCountDirectionEstimator}.
   *
   * @param target Object whose direction should be automatically adjusted on
   *     relevant events.
   * @return AutoDirectionHandler An instance of AutoDirectionHandler for the
   *     given object.
   */
  public static AutoDirectionHandler addTo(Target target) {
    return addTo(target, true);
  }

  /**
   * Operates like {@link #addTo(Target, DirectionEstimator)}, but uses a
   * default DirectionEstimator, {@link
   * com.google.gwt.i18n.shared.WordCountDirectionEstimator} if {@code enabled},
   * or else a null DirectionEstimator, which means disabling direction
   * estimation.
   *
   * @param target Object whose direction should be automatically adjusted on
   *     relevant events.
   * @param enabled Whether the handler is enabled upon creation.
   * @return AutoDirectionHandler An instance of AutoDirectionHandler for the
   *     given object.
   */
  public static AutoDirectionHandler addTo(Target target, boolean enabled) {
    return addTo(target, enabled ? WordCountDirectionEstimator.get() : null);
  }

  /**
   * Adds auto-direction adjustment to a given object:
   * - Creates an AutoDirectionHandler.
   * - Initializes it with the given DirectionEstimator.
   * - Adds it as an event handler for the relevant events on the given object.
   * - Returns the AutoDirectionHandler, so its setAutoDir() method can be
   * called when the object's text changes by means other than the handled
   * events.
   *
   * @param target Object whose direction should be automatically adjusted on
   *     relevant events.
   * @param directionEstimator A DirectionEstimator object used for direction
   *     estimation (use null to disable direction estimation).
   * @return AutoDirectionHandler An instance of AutoDirectionHandler for the
   *     given object.
   */
  public static AutoDirectionHandler addTo(Target target, DirectionEstimator
      directionEstimator) {
    AutoDirectionHandler autoDirHandler = new AutoDirectionHandler(target,
        directionEstimator);
    return autoDirHandler;
  }

  /**
   * A DirectionEstimator object used for direction estimation.
   */
  private DirectionEstimator directionEstimator;

  /**
   * A HandlerRegistration object used to remove this handler.
   */
  private HandlerRegistration handlerRegistration;

  /**
   * The object being handled.
   */
  private Target target;

  /**
   * Private constructor. Instantiate using one of the addTo() methods.
   *
   * @param target Object whose direction should be automatically adjusted on
   *     relevant events.
   * @param directionEstimator A DirectionEstimator object used for direction
   *     estimation.
   */
  private AutoDirectionHandler(Target target, DirectionEstimator
      directionEstimator) {
    this.target = target;
    this.handlerRegistration = null;
    setDirectionEstimator(directionEstimator);
  }

  /**
   * Returns the DirectionEstimator object.
   */
  public DirectionEstimator getDirectionEstimator() {
    return directionEstimator;
  }

  /**
   * Automatically adjusts hasDirection's direction on KeyUpEvent events.
   * Implementation of KeyUpHandler interface method.
   */
  public void onKeyUp(KeyUpEvent event) {
    refreshDirection();
  }

  /**
   * Adjusts target's direction according to the estimated direction of the text
   * it supplies.
   */
  public void refreshDirection() {
    if (directionEstimator != null) {
      Direction dir = directionEstimator.estimateDirection(target.getText());
      if (dir != target.getDirection()) {
        target.setDirection(dir);
      }
    }
  }

  /**
   * Toggles direction estimation on (using a default estimator) and off.
   */
  public void setDirectionEstimator(boolean enabled) {
    setDirectionEstimator(enabled ? WordCountDirectionEstimator.get() : null);
  }

  /**
   * Sets the DirectionEstimator object.
   */
  public void setDirectionEstimator(DirectionEstimator directionEstimator) {
    this.directionEstimator = directionEstimator;
    if ((directionEstimator == null) != (handlerRegistration == null)) {
      if (directionEstimator == null) {
        handlerRegistration.removeHandler();
        handlerRegistration = null;
      } else {
        handlerRegistration = target.addKeyUpHandler(this);
      }
    }
    refreshDirection();
  }
}
