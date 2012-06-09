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

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * Returns a reference to the screen object associated with the window.
  */
public interface Screen {


  /**
    * Specifies the height of the screen, in pixels, minus permanent or semipermanent user interface features displayed by the operating system, such as the Taskbar on Windows.
    */
  int getAvailHeight();


  /**
    * Returns the first available pixel available from the left side of the screen.
    */
  int getAvailLeft();


  /**
    * Specifies the y-coordinate of the first pixel that is not allocated to permanent or semipermanent user interface features.
    */
  int getAvailTop();


  /**
    * Returns the amount of horizontal space in pixels available to the window.
    */
  int getAvailWidth();


  /**
    * Returns the color depth of the screen.
    */
  int getColorDepth();


  /**
    * Returns the height of the screen in pixels.
    */
  int getHeight();


  /**
    * Gets the bit depth of the screen.
    */
  int getPixelDepth();


  /**
    * Returns the width of the screen.
    */
  int getWidth();
}
