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
 * Direction estimator that uses the "word count" heuristic.
 * 
 * <p> Note: this is probably the recommended estimator for most use cases.
 */
public class WordCountDirectionEstimator extends DirectionEstimator {

  /**
   * An instance of WordCountDirectionEstimator, to be returned by {@link #get}.
   */
  private static final WordCountDirectionEstimator instance =
      new WordCountDirectionEstimator();
  
  /**
   * Get an instance of WordCountDirectionEstimator.
   * 
   * @return An instance of WordCountDirectionEstimator.
   */
  public static WordCountDirectionEstimator get() {
    return instance;
  }

  /**
   * Estimates the direction of a given string using the "word count" heuristic,
   * as defined at {@link BidiUtils#estimateDirection}.
   *
   * @param str Input string.
   * @return Direction The estimated direction of {@code str}.
   */
  @Override
  public Direction estimateDirection(String str) {
    return BidiUtils.get().estimateDirection(str);
  }
}
