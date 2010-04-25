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
 * Unit tests for {@link WordCountDirectionEstimator}.
 */
public class WordCountDirectionEstimatorTest extends DirectionEstimatorTestBase {
  
  @Override
  protected void assertDirectionEstimation(Direction expectedDirection,
      String str, boolean isHtml) {
    assertDirectionEstimation(expectedDirection,
        WordCountDirectionEstimator.get(), str, isHtml);
  }
  
  private String pureNeutral = NEUTRAL_WORD;
  private String rtlAboveThreshold = EN_WORD + IW_WORD;
  private String rtlBelowThreshold = IW_WORD + EN_WORD + EN_WORD;
  private String rtlHtml = LONG_LTR_TAG + IW_WORD;
  private String weaklyLtr = NEUTRAL_WORD + DIGITS_WORD;
 
  public void testEstimateDirection() {
    assertDirectionEstimation(Direction.RTL, rtlAboveThreshold);
    assertDirectionEstimation(Direction.LTR, rtlBelowThreshold);
    assertDirectionEstimation(Direction.LTR, weaklyLtr);
    assertDirectionEstimation(Direction.DEFAULT, pureNeutral);
    
    assertDirectionEstimation(Direction.LTR, rtlHtml);
    assertDirectionEstimationHtml(Direction.RTL, rtlHtml);
  }
}
