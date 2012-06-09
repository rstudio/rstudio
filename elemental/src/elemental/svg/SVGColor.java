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
import elemental.css.RGBColor;
import elemental.css.CSSValue;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <p>This page explains more about how you can specify color in CSS.
</p><p>In your sample stylesheet, you introduce background colors.
</p>
  */
public interface SVGColor extends CSSValue {

    static final int SVG_COLORTYPE_CURRENTCOLOR = 3;

    static final int SVG_COLORTYPE_RGBCOLOR = 1;

    static final int SVG_COLORTYPE_RGBCOLOR_ICCCOLOR = 2;

    static final int SVG_COLORTYPE_UNKNOWN = 0;

  int getColorType();

  RGBColor getRgbColor();

  void setColor(int colorType, String rgbColor, String iccColor);

  void setRGBColor(String rgbColor);

  void setRGBColorICCColor(String rgbColor, String iccColor);
}
