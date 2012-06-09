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
  * The <code>Plugin</code> interface provides information about a browser plugin.
  */
public interface DOMPlugin {


  /**
    * A human readable description of the plugin. <strong>Read only.</strong>
    */
  String getDescription();


  /**
    * The filename of the plugin file. <strong>Read only.</strong>
    */
  String getFilename();

  int getLength();


  /**
    * The name of the plugin. <strong>Read only.</strong>
    */
  String getName();


  /**
    * Returns the MIME&nbsp;type of a supported content type, given the index number into a list of supported types.
    */
  DOMMimeType item(int index);


  /**
    * Returns the MIME&nbsp;type of a supported item.
    */
  DOMMimeType namedItem(String name);
}
