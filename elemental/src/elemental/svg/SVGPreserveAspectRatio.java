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
  * <p>The <code>SVGPreserveAspectRatio</code> interface corresponds to the 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/preserveAspectRatio">preserveAspectRatio</a></code> attribute, which is available for some of SVG's elements.</p>
<p>An <code>SVGPreserveAspectRatio</code> object can be designated as read only, which means that attempts to modify the object will result in an exception being thrown.</p>
  */
public interface SVGPreserveAspectRatio {

  /**
    * Corresponds to value <code>meet</code> for attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/preserveAspectRatio">preserveAspectRatio</a></code>.
    */

    static final int SVG_MEETORSLICE_MEET = 1;

  /**
    * Corresponds to value <code>slice</code> for attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/preserveAspectRatio">preserveAspectRatio</a></code>.
    */

    static final int SVG_MEETORSLICE_SLICE = 2;

  /**
    * The enumeration was set to a value that is not one of predefined types. It is invalid to attempt to define a new value of this type or to attempt to switch an existing value to this type.
    */

    static final int SVG_MEETORSLICE_UNKNOWN = 0;

  /**
    * Corresponds to value <code>none</code> for attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/preserveAspectRatio">preserveAspectRatio</a></code>.
    */

    static final int SVG_PRESERVEASPECTRATIO_NONE = 1;

  /**
    * The enumeration was set to a value that is not one of predefined types. It is invalid to attempt to define a new value of this type or to attempt to switch an existing value to this type.
    */

    static final int SVG_PRESERVEASPECTRATIO_UNKNOWN = 0;

  /**
    * Corresponds to value <code>xMaxYMax</code> for attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/preserveAspectRatio">preserveAspectRatio</a></code>.
    */

    static final int SVG_PRESERVEASPECTRATIO_XMAXYMAX = 10;

  /**
    * Corresponds to value <code>xMaxYMid</code> for attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/preserveAspectRatio">preserveAspectRatio</a></code>.
    */

    static final int SVG_PRESERVEASPECTRATIO_XMAXYMID = 7;

  /**
    * Corresponds to value <code>xMaxYMin</code> for attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/preserveAspectRatio">preserveAspectRatio</a></code>.
    */

    static final int SVG_PRESERVEASPECTRATIO_XMAXYMIN = 4;

  /**
    * Corresponds to value <code>xMidYMax</code> for attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/preserveAspectRatio">preserveAspectRatio</a></code>.
    */

    static final int SVG_PRESERVEASPECTRATIO_XMIDYMAX = 9;

  /**
    * Corresponds to value <code>xMidYMid</code> for attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/preserveAspectRatio">preserveAspectRatio</a></code>.
    */

    static final int SVG_PRESERVEASPECTRATIO_XMIDYMID = 6;

  /**
    * Corresponds to value <code>xMidYMin</code> for attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/preserveAspectRatio">preserveAspectRatio</a></code>.
    */

    static final int SVG_PRESERVEASPECTRATIO_XMIDYMIN = 3;

  /**
    * Corresponds to value <code>xMinYMax</code> for attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/preserveAspectRatio">preserveAspectRatio</a></code>.
    */

    static final int SVG_PRESERVEASPECTRATIO_XMINYMAX = 8;

  /**
    * Corresponds to value <code>xMinYMid</code> for attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/preserveAspectRatio">preserveAspectRatio</a></code>.
    */

    static final int SVG_PRESERVEASPECTRATIO_XMINYMID = 5;

  /**
    * Corresponds to value <code>xMinYMin</code> for attribute 
<code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Attribute/preserveAspectRatio">preserveAspectRatio</a></code>.
    */

    static final int SVG_PRESERVEASPECTRATIO_XMINYMIN = 2;


  /**
    * The type of the alignment value as specified by one of the SVG_PRESERVEASPECTRATIO_* constants defined on this interface.
    */
  int getAlign();

  void setAlign(int arg);


  /**
    * The type of the meet-or-slice value as specified by one of the SVG_MEETORSLICE_* constants defined on this interface.
    */
  int getMeetOrSlice();

  void setMeetOrSlice(int arg);
}
