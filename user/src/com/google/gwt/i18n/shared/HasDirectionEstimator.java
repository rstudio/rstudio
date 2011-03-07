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
package com.google.gwt.i18n.shared;

/**
 * Interface for objects that have a direction estimator.
 */
public interface HasDirectionEstimator {

  /**
   * Returns the {@code DirectionEstimator} object.
   */
  DirectionEstimator getDirectionEstimator();

  /**
   * Toggles on / off direction estimation.
   *
   * @param enabled Whether to enable direction estimation. If {@code true},
   *          sets the {@link DirectionEstimator} object to a default
   *          {@code DirectionEstimator}.
   */
  void setDirectionEstimator(boolean enabled);

  /**
   * Sets the {@link DirectionEstimator} object.
   *
   * @param directionEstimator The {@code DirectionEstimator} to be set. {@code
   *        null} means turning off direction estimation.
   */
  void setDirectionEstimator(DirectionEstimator directionEstimator);
}
