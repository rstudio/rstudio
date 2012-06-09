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
public interface WebKitCSSFilterValue extends CSSValueList {

    static final int CSS_FILTER_BLUR = 10;

    static final int CSS_FILTER_BRIGHTNESS = 8;

    static final int CSS_FILTER_CONTRAST = 9;

    static final int CSS_FILTER_CUSTOM = 12;

    static final int CSS_FILTER_DROP_SHADOW = 11;

    static final int CSS_FILTER_GRAYSCALE = 2;

    static final int CSS_FILTER_HUE_ROTATE = 5;

    static final int CSS_FILTER_INVERT = 6;

    static final int CSS_FILTER_OPACITY = 7;

    static final int CSS_FILTER_REFERENCE = 1;

    static final int CSS_FILTER_SATURATE = 4;

    static final int CSS_FILTER_SEPIA = 3;

  int getOperationType();
}
