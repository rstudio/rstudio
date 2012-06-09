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
  * The <code>feBlend</code> filter composes two objects together ruled by a certain blending mode. This is similar to what is known from image editing software when blending two layers. The mode is defined by the 
<code><a rel="internal" href="https://developer.mozilla.org/en/SVG/Attribute/mode" class="new">mode</a></code> attribute.
  */
public interface SVGFEBlendElement extends SVGElement, SVGFilterPrimitiveStandardAttributes {

    static final int SVG_FEBLEND_MODE_DARKEN = 4;

    static final int SVG_FEBLEND_MODE_LIGHTEN = 5;

    static final int SVG_FEBLEND_MODE_MULTIPLY = 2;

    static final int SVG_FEBLEND_MODE_NORMAL = 1;

    static final int SVG_FEBLEND_MODE_SCREEN = 3;

    static final int SVG_FEBLEND_MODE_UNKNOWN = 0;

  SVGAnimatedString getIn1();

  SVGAnimatedString getIn2();

  SVGAnimatedEnumeration getMode();
}
