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

import junit.framework.TestCase;

/**
 * TestCase extension that includes some useful variables for estimation tests.
 */
public abstract class DirectionEstimatorTestBase extends TestCase {
 
  protected final String DIGITS_WORD = " 012";
  protected final String EN_WORD = " abc";
  protected final String IW_WORD = " \u05e0\u05e1\u05e2";
  protected final String LONG_LTR_TAG = "<some nasty tag>";
  
  protected final String LONG_MIXED_TAG = "<some nasty tag" + IW_WORD + ">";
  protected final String NEUTRAL_WORD = " ___";
  protected final String WORD_WITH_ONE_RTL_CHAR = " ab\u05e0cd";
  
  /**
   * Asserts that the estimated direction of a given String matches the expected
   * direction.
   * 
   * @param expectedDirection The expected direction.
   * @param directionEstimator A direction estimator object. 
   * @param str A String to estimate its direction.
   * @param isHtml Whether {@code str} is HTML / HTML-escaped.
   */
  protected void assertDirectionEstimation(Direction expectedDirection,
      DirectionEstimator directionEstimator, String str, boolean isHtml) {
    assertEquals(expectedDirection,
        directionEstimator.estimateDirection(str, isHtml));
  }
  
  /**
   * Asserts that the estimated direction of a given String matches the expected
   * direction.
   * The implementation will usually call {@link #assertDirectionEstimation(
   * Direction, DirectionEstimator, String, boolean)} with the {@code
   * DirectionEstimator} object to be tested.
   * 
   * @param expectedDirection The expected direction.
   * @param str A String whose direction is estimated.
   * @param isHtml Whether {@code str} is HTML / HTML-escaped.
   */
  protected abstract void assertDirectionEstimation(Direction expectedDirection,
      String str, boolean isHtml);
  
  /**
   * Operates like {@link #assertDirectionEstimation(Direction, String,
   * boolean)}, but assuming  {@code str} is not HTML / HTML-escaped.
   * 
   * @param expectedDirection The expected direction.
   * @param str A String whose direction is estimated.
   */
  protected void assertDirectionEstimation(Direction expectedDirection,
      String str) {
    assertDirectionEstimation(expectedDirection, str, false);
  }

  /**
   * Operates like {@link #assertDirectionEstimation(Direction, String,
   * boolean)}, but assuming  {@code str} is HTML / HTML-escaped.
   * 
   * @param expectedDirection The expected direction.
   * @param str A String whose direction is estimated.
   */
  protected void assertDirectionEstimationHtml(Direction expectedDirection,
      String str) {
    assertDirectionEstimation(expectedDirection, str, true);
  }
}
