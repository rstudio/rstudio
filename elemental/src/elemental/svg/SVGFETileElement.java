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
  * An input image is tiled and the result used to fill a target. The effect is similar to the one of a <code><a rel="custom" href="https://developer.mozilla.org/en/SVG/Element/pattern">&lt;pattern&gt;</a></code>
.
  */
public interface SVGFETileElement extends SVGElement, SVGFilterPrimitiveStandardAttributes {

  SVGAnimatedString getIn1();
}
