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
 * Unit tests for {@link FirstStrongDirectionEstimator}.
 */
public class FirstStrongDirectionEstimatorTest extends DirectionEstimatorTestBase {

  @Override
  protected void assertDirectionEstimation(Direction expectedDirection,
      String str, boolean isHtml) {
    assertDirectionEstimation(expectedDirection,
        FirstStrongDirectionEstimator.get(), str, isHtml);
  }
  
  private String firstStrongLtr = DIGITS_WORD + EN_WORD + IW_WORD + IW_WORD;
  private String firstStrongRtl = DIGITS_WORD + IW_WORD + EN_WORD + EN_WORD;
  private String firstStrongRtlHtml = DIGITS_WORD + LONG_LTR_TAG + IW_WORD +
      EN_WORD;
  private String noStrongChars = NEUTRAL_WORD + DIGITS_WORD;
 
  public void testEstimateDirection() {
    assertDirectionEstimation(Direction.LTR, firstStrongLtr);
    assertDirectionEstimation(Direction.RTL, firstStrongRtl);
    assertDirectionEstimation(Direction.DEFAULT, noStrongChars);
    
    assertDirectionEstimation(Direction.LTR, firstStrongRtlHtml);
    assertDirectionEstimationHtml(Direction.RTL, firstStrongRtlHtml);
  }
}
