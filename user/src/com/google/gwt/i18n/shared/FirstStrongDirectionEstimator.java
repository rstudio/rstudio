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

import com.google.gwt.i18n.client.HasDirection.Direction;

/**
 * Direction estimator that uses the "first strong" heuristic.
 */
public class FirstStrongDirectionEstimator extends DirectionEstimator {

  /**
   * An instance of FirstStrongDirectionEstimator, to be returned by
   * {@link #get}.
   */
  private static final FirstStrongDirectionEstimator instance =
      new FirstStrongDirectionEstimator();
  
  /**
   * Get an instance of FirstStrongDirectionEstimator.
   * 
   * @return An instance of FirstStrongDirectionEstimator.
   */
  public static FirstStrongDirectionEstimator get() {
    return instance;
  }

  /**
   * Estimates the direction of a given string using the "first strong"
   * heuristic: The return value is determined by the first character in the
   * string with strong directionality. If there is no such character, the
   * return value is DEFAULT.
   *
   * @param str Input string.
   * @return Direction The estimated direction of {@code str}.
   */
  @Override
  public Direction estimateDirection(String str) {
    return BidiUtils.get().startsWithRtl(str) ? Direction.RTL :
      BidiUtils.get().startsWithLtr(str) ? Direction.LTR : Direction.DEFAULT;
  }
}
