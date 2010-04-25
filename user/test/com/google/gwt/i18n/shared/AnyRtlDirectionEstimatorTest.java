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
 * Unit tests for {@link AnyRtlDirectionEstimator}.
 */
public class AnyRtlDirectionEstimatorTest extends DirectionEstimatorTestBase {
 
  @Override
  protected void assertDirectionEstimation(Direction expectedDirection,
      String str, boolean isHtml) {
    assertDirectionEstimation(expectedDirection, AnyRtlDirectionEstimator.get(),
        str, isHtml);
  }
  
  private final String containsRtlChar = EN_WORD + DIGITS_WORD +
      WORD_WITH_ONE_RTL_CHAR;
  private final String noRtlChars = EN_WORD + DIGITS_WORD + NEUTRAL_WORD;
  private final String noRtlCharsHtml = LONG_MIXED_TAG + EN_WORD + DIGITS_WORD;

  public void testEstimateDirection() {
    assertDirectionEstimation(Direction.RTL, containsRtlChar);
    assertDirectionEstimation(Direction.LTR, noRtlChars);
    
    assertDirectionEstimation(Direction.RTL, noRtlCharsHtml);
    assertDirectionEstimationHtml(Direction.LTR, noRtlCharsHtml);
  }
}
