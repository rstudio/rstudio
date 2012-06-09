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
public interface SVGPaint extends SVGColor {

    static final int SVG_PAINTTYPE_CURRENTCOLOR = 102;

    static final int SVG_PAINTTYPE_NONE = 101;

    static final int SVG_PAINTTYPE_RGBCOLOR = 1;

    static final int SVG_PAINTTYPE_RGBCOLOR_ICCCOLOR = 2;

    static final int SVG_PAINTTYPE_UNKNOWN = 0;

    static final int SVG_PAINTTYPE_URI = 107;

    static final int SVG_PAINTTYPE_URI_CURRENTCOLOR = 104;

    static final int SVG_PAINTTYPE_URI_NONE = 103;

    static final int SVG_PAINTTYPE_URI_RGBCOLOR = 105;

    static final int SVG_PAINTTYPE_URI_RGBCOLOR_ICCCOLOR = 106;

  int getPaintType();

  String getUri();

  void setPaint(int paintType, String uri, String rgbColor, String iccColor);
}
