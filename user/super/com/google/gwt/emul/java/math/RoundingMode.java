/*
 * Copyright 2009 Google Inc.
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

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * INCLUDES MODIFICATIONS BY RICHARD ZSCHECH AS WELL AS GOOGLE.
 */
package java.math;

/**
 * Specifies the rounding behavior for operations whose results cannot be
 * represented exactly.
 */
public enum RoundingMode {

  /**
   * Rounding mode where positive values are rounded towards positive infinity
   * and negative values towards negative infinity. <br>
   * Rule: {@code x.round().abs() >= x.abs()}
   */
  UP(BigDecimal.ROUND_UP),

  /**
   * Rounding mode where the values are rounded towards zero. <br>
   * Rule: {@code x.round().abs() <= x.abs()}
   */
  DOWN(BigDecimal.ROUND_DOWN),

  /**
   * Rounding mode to round towards positive infinity. For positive values this
   * rounding mode behaves as {@link #UP}, for negative values as {@link #DOWN}. <br>
   * Rule: {@code x.round() >= x}
   */
  CEILING(BigDecimal.ROUND_CEILING),

  /**
   * Rounding mode to round towards negative infinity. For positive values this
   * rounding mode behaves as {@link #DOWN}, for negative values as {@link #UP}. <br>
   * Rule: {@code x.round() <= x}
   */
  FLOOR(BigDecimal.ROUND_FLOOR),

  /**
   * Rounding mode where values are rounded towards the nearest neighbor. Ties
   * are broken by rounding up.
   */
  HALF_UP(BigDecimal.ROUND_HALF_UP),

  /**
   * Rounding mode where values are rounded towards the nearest neighbor. Ties
   * are broken by rounding down.
   */
  HALF_DOWN(BigDecimal.ROUND_HALF_DOWN),

  /**
   * Rounding mode where values are rounded towards the nearest neighbor. Ties
   * are broken by rounding to the even neighbor.
   */
  HALF_EVEN(BigDecimal.ROUND_HALF_EVEN),

  /**
   * Rounding mode where the rounding operations throws an ArithmeticException
   * for the case that rounding is necessary, i.e. for the case that the value
   * cannot be represented exactly.
   */
  UNNECESSARY(BigDecimal.ROUND_UNNECESSARY);
  
  /**
   * Some constant char arrays for optimized comparisons
   */
  private static final char[] chCEILING = {'C','E','I','L','I','N','G'};
  private static final char[] chDOWN = {'D','O','W','N'};
  private static final char[] chFLOOR = {'F','L','O','O','R'};
  private static final char[] chHALF_DOWN = {'H','A','L','F','_','D','O','W','N'};
  private static final char[] chHALF_EVEN = {'H','A','L','F','_','E','V','E','N'};
  private static final char[] chHALF_UP = {'H','A','L','F','_','U','P'};
  private static final char[] chUNNECESSARY = {'U','N','N','E','C','E','S','S','A','R','Y'};
  private static final char[] chUP = {'U','P'};
  
  /**
   * Converts rounding mode constants from class {@code BigDecimal} into {@code
   * RoundingMode} values.
   * 
   * @param mode rounding mode constant as defined in class {@code BigDecimal}
   * @return corresponding rounding mode object
   */
  public static RoundingMode valueOf(int mode) {
    switch (mode) {
      case BigDecimal.ROUND_CEILING:
        return CEILING;
      case BigDecimal.ROUND_DOWN:
        return DOWN;
      case BigDecimal.ROUND_FLOOR:
        return FLOOR;
      case BigDecimal.ROUND_HALF_DOWN:
        return HALF_DOWN;
      case BigDecimal.ROUND_HALF_EVEN:
        return HALF_EVEN;
      case BigDecimal.ROUND_HALF_UP:
        return HALF_UP;
      case BigDecimal.ROUND_UNNECESSARY:
        return UNNECESSARY;
      case BigDecimal.ROUND_UP:
        return UP;
      default:
        // math.00=Invalid rounding mode
        throw new IllegalArgumentException("Invalid rounding mode"); //$NON-NLS-1$
    }
  }
  
  /**
   * Bypasses calls to the implicit valueOf(String) method, which will break
   * if enum name obfuscation is enabled.  This should be package visible only.
   * 
   * @param mode rounding mode string as defined in class {@code BigDecimal}
   * @return corresponding rounding mode object
   */
  static RoundingMode valueOfExplicit(String mode) {
    /*
     * Note this is optimized to avoid multiple String compares, 
     * using specific knowledge of the set of allowed enum constants.
     */
    
    if (mode == null) {
      throw new NullPointerException();
    } 
    
    char[] modeChars = mode.toCharArray();
    int len = modeChars.length;
    if (len < chUP.length || len > chUNNECESSARY.length) {
      throw new IllegalArgumentException();
    }
    
    char[] targetChars = null;
    RoundingMode target = null;
    if (modeChars[0] == 'C') {
      target = RoundingMode.CEILING;
      targetChars = chCEILING;
    } else if (modeChars[0] == 'D') {
      target = RoundingMode.DOWN;
      targetChars = chDOWN;
    } else if (modeChars[0] == 'F') {
      target = RoundingMode.FLOOR;
      targetChars = chFLOOR;
    } else if (modeChars[0] == 'H') {
      if (len > 6) {
        if (modeChars[5] == 'D') {
          target = RoundingMode.HALF_DOWN;
          targetChars = chHALF_DOWN;
        } else if (modeChars[5] == 'E') {
          target = RoundingMode.HALF_EVEN;
          targetChars = chHALF_EVEN;
        } else if (modeChars[5] == 'U') {
          target = RoundingMode.HALF_UP;
          targetChars = chHALF_UP;
        }
      }
    } else if (modeChars[0] == 'U') {
      if (modeChars[1] == 'P') {
        target = RoundingMode.UP;
        targetChars = chUP;
      } else if (modeChars[1] == 'N') {
        target = RoundingMode.UNNECESSARY;
        targetChars = chUNNECESSARY;
      }
    }
    
    if (target != null && len == targetChars.length) {
      int i;
      for (i = 1; i < len && modeChars[i] == targetChars[i]; i++) {
      }
      if (i == len) {
        return target;
      }
    }
    
    throw new IllegalArgumentException();
  }
  
  /**
   * Set the old constant.
   * @param rm unused
   */
  RoundingMode(int rm) {
    // Note that we do not need the old-style rounding mode, so we ignore it.
  }
}
