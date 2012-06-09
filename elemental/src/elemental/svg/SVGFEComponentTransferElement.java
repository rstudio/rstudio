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
  * The color of each pixel is modified by changing each channel (R, G, B, and A) to the result of what the children <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/feFuncR">&lt;feFuncR&gt;</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/feFuncB">&lt;feFuncB&gt;</a></code>
, <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/feFuncG">&lt;feFuncG&gt;</a></code>
, and <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/feFuncA">&lt;feFuncA&gt;</a></code>
 return.
  */
public interface SVGFEComponentTransferElement extends SVGElement, SVGFilterPrimitiveStandardAttributes {

  SVGAnimatedString getIn1();
}
