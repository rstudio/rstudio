/*
 * Copyright 2012 Google Inc.
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
package elemental.css;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * 
  */
public interface CSSPrimitiveValue extends CSSValue {

    static final int CSS_ATTR = 22;

    static final int CSS_CM = 6;

    static final int CSS_COUNTER = 23;

    static final int CSS_DEG = 11;

    static final int CSS_DIMENSION = 18;

    static final int CSS_EMS = 3;

    static final int CSS_EXS = 4;

    static final int CSS_GRAD = 13;

    static final int CSS_HZ = 16;

    static final int CSS_IDENT = 21;

    static final int CSS_IN = 8;

    static final int CSS_KHZ = 17;

    static final int CSS_MM = 7;

    static final int CSS_MS = 14;

    static final int CSS_NUMBER = 1;

    static final int CSS_PC = 10;

    static final int CSS_PERCENTAGE = 2;

    static final int CSS_PT = 9;

    static final int CSS_PX = 5;

    static final int CSS_RAD = 12;

    static final int CSS_RECT = 24;

    static final int CSS_RGBCOLOR = 25;

    static final int CSS_S = 15;

    static final int CSS_STRING = 19;

    static final int CSS_UNKNOWN = 0;

    static final int CSS_URI = 20;

    static final int CSS_VH = 27;

    static final int CSS_VMIN = 28;

    static final int CSS_VW = 26;

  int getPrimitiveType();

  Counter getCounterValue();

  float getFloatValue(int unitType);

  RGBColor getRGBColorValue();

  Rect getRectValue();

  String getStringValue();

  void setFloatValue(int unitType, float floatValue);

  void setStringValue(int stringType, String stringValue);
}
