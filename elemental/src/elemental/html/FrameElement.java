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
package elemental.html;
import elemental.dom.Element;
import elemental.svg.SVGDocument;
import elemental.dom.Document;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <p><code>&lt;frame&gt;</code> is an HTML element which defines a particular area in which another HTML document can be displayed. A frame should be used within a <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/frameset">&lt;frameset&gt;</a></code>
.</p>
<p>Using the <code>&lt;frame&gt;</code> element is not encouraged because of certain disadvantages such as performance problems and lack of accessibility for users with screen readers. Instead of the <code>&lt;frame&gt;</code> element, <code><a rel="custom" href="https://developer.mozilla.org/en/HTML/Element/iframe">&lt;iframe&gt;</a></code>
&nbsp;may be preferred.</p>
  */
public interface FrameElement extends Element {

  Document getContentDocument();

  Window getContentWindow();

  String getFrameBorder();

  void setFrameBorder(String arg);

  int getHeight();

  String getLocation();

  void setLocation(String arg);

  String getLongDesc();

  void setLongDesc(String arg);

  String getMarginHeight();

  void setMarginHeight(String arg);

  String getMarginWidth();

  void setMarginWidth(String arg);


  /**
    * This attribute is used to labeling frames. Without labeling all links will open in the frame that they are in.
    */
  String getName();

  void setName(String arg);

  boolean isNoResize();

  void setNoResize(boolean arg);


  /**
    * This attribute defines existence of scrollbar. If this attribute is not used, browser put a scrollbar when necessary. There are two choices; "yes" for showing a scrollbar even when it is not necessary and "no" for do not showing a scrollbar even when it is necessary.
    */
  String getScrolling();

  void setScrolling(String arg);


  /**
    * This attribute is specify document which will be displayed by frame.
    */
  String getSrc();

  void setSrc(String arg);

  int getWidth();

  SVGDocument getSVGDocument();
}
