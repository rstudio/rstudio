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
import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * Interface for direction estimators.
 */
public abstract class DirectionEstimator {
  
  /**
   * Estimates the direction of a plain-text string.
   * 
   * @param str The string to check.
   * @return {@code str}'s estimated direction.
   */
  public abstract Direction estimateDirection(String str);

  /**
   * Estimates the direction of a string.
   * 
   * @param str The string to check.
   * @param isHtml Whether {@code str} is HTML / HTML-escaped. {@code false}
   *        means that {@code str} is plain-text.
   * @return {@code str}'s estimated direction.
   */
  public Direction estimateDirection(String str, boolean isHtml) {
    return estimateDirection(BidiUtils.get().stripHtmlIfNeeded(str, isHtml));
  }
  
  /**
   * Estimates the direction of a SafeHtml.
   * 
   * @param html The string to check.
   * @return {@code html}'s estimated direction.
   */
  public Direction estimateDirection(SafeHtml html) {
    return estimateDirection(BidiUtils.get().stripHtmlIfNeeded(html.asString(),
        true));
  }
}
