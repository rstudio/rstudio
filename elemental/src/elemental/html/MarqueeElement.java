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

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * Non-standard
  */
public interface MarqueeElement extends Element {


  /**
    * Sets how the text is scrolled within the marquee. Possible values are <code>scroll</code>, <code>slide</code> and <code>alternate</code>. If no value is specified, the default value is <code>scroll</code>.
    */
  String getBehavior();

  void setBehavior(String arg);

  String getBgColor();

  void setBgColor(String arg);


  /**
    * Sets the direction of the scrolling within the marquee. Possible values are <code>left</code>, <code>right</code>, <code>up</code> and <code>down</code>. If no value is specified, the default value is <code>left</code>.
    */
  String getDirection();

  void setDirection(String arg);


  /**
    * Sets the height in pixels or percentage value.
    */
  String getHeight();

  void setHeight(String arg);


  /**
    * Sets the horizontal margin
    */
  int getHspace();

  void setHspace(int arg);


  /**
    * Sets the number of times the marquee will scroll. If no value is specified, the default value is −1, which means the marquee will scroll continuously.
    */
  int getLoop();

  void setLoop(int arg);

  int getScrollAmount();

  void setScrollAmount(int arg);

  int getScrollDelay();

  void setScrollDelay(int arg);

  boolean isTrueSpeed();

  void setTrueSpeed(boolean arg);


  /**
    * Sets the vertical margin in pixels or percentage value.
    */
  int getVspace();

  void setVspace(int arg);


  /**
    * Sets the width in pixels or percentage value.
    */
  String getWidth();

  void setWidth(String arg);


  /**
    * Starts scrolling of the marquee.
    */
  void start();


  /**
    * Stops scrolling of the marquee.
    */
  void stop();
}
