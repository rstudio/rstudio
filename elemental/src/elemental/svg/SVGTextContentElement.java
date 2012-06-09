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
package elemental.svg;

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
public interface SVGTextContentElement extends SVGElement, SVGTests, SVGLangSpace, SVGExternalResourcesRequired, SVGStylable {

    static final int LENGTHADJUST_SPACING = 1;

    static final int LENGTHADJUST_SPACINGANDGLYPHS = 2;

    static final int LENGTHADJUST_UNKNOWN = 0;

  SVGAnimatedEnumeration getLengthAdjust();

  SVGAnimatedLength getTextLength();

  int getCharNumAtPosition(SVGPoint point);

  float getComputedTextLength();

  SVGPoint getEndPositionOfChar(int offset);

  SVGRect getExtentOfChar(int offset);

  int getNumberOfChars();

  float getRotationOfChar(int offset);

  SVGPoint getStartPositionOfChar(int offset);

  float getSubStringLength(int offset, int length);

  void selectSubString(int offset, int length);
}
