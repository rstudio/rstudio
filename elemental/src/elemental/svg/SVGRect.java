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
  * <p>The <code>SVGRect</code> represents rectangular geometry. Rectangles are defined as consisting of a (x,y) coordinate pair identifying a minimum X value, a minimum Y value, and a width and height, which are usually constrained to be non-negative.</p>
<p>An <code>SVGRect</code> object can be designated as read only, which means that attempts to modify the object will result in an exception being thrown.</p>
  */
public interface SVGRect {


  /**
    * The <em>height</em> coordinate of the rectangle, in user units.
    */
  float getHeight();

  void setHeight(float arg);


  /**
    * The <em>width</em> coordinate of the rectangle, in user units.
    */
  float getWidth();

  void setWidth(float arg);

  float getX();

  void setX(float arg);

  float getY();

  void setY(float arg);
}
