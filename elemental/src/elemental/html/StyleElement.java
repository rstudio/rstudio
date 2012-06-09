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
import elemental.stylesheets.StyleSheet;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * See <a title="en/DOM/Using_dynamic_styling_information" rel="internal" href="https://developer.mozilla.org/en/DOM/Using_dynamic_styling_information">Using dynamic styling information</a> for an overview of the objects used to manipulate specified CSS properties using the DOM.
  */
public interface StyleElement extends Element {


  /**
    * Returns true if the stylesheet is disabled, and false if not
    */
  boolean isDisabled();

  void setDisabled(boolean arg);


  /**
    * Specifies the intended destination medium for style information.
    */
  String getMedia();

  void setMedia(String arg);

  boolean isScoped();

  void setScoped(boolean arg);

  StyleSheet getSheet();


  /**
    * Returns the type of style being applied by this statement.
    */
  String getType();

  void setType(String arg);
}
