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
public interface SVGPathSeg {

    static final int PATHSEG_ARC_ABS = 10;

    static final int PATHSEG_ARC_REL = 11;

    static final int PATHSEG_CLOSEPATH = 1;

    static final int PATHSEG_CURVETO_CUBIC_ABS = 6;

    static final int PATHSEG_CURVETO_CUBIC_REL = 7;

    static final int PATHSEG_CURVETO_CUBIC_SMOOTH_ABS = 16;

    static final int PATHSEG_CURVETO_CUBIC_SMOOTH_REL = 17;

    static final int PATHSEG_CURVETO_QUADRATIC_ABS = 8;

    static final int PATHSEG_CURVETO_QUADRATIC_REL = 9;

    static final int PATHSEG_CURVETO_QUADRATIC_SMOOTH_ABS = 18;

    static final int PATHSEG_CURVETO_QUADRATIC_SMOOTH_REL = 19;

    static final int PATHSEG_LINETO_ABS = 4;

    static final int PATHSEG_LINETO_HORIZONTAL_ABS = 12;

    static final int PATHSEG_LINETO_HORIZONTAL_REL = 13;

    static final int PATHSEG_LINETO_REL = 5;

    static final int PATHSEG_LINETO_VERTICAL_ABS = 14;

    static final int PATHSEG_LINETO_VERTICAL_REL = 15;

    static final int PATHSEG_MOVETO_ABS = 2;

    static final int PATHSEG_MOVETO_REL = 3;

    static final int PATHSEG_UNKNOWN = 0;

  int getPathSegType();

  String getPathSegTypeAsLetter();
}
