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
  * HTMLOptionsCollection is an interface representing a collection of HTML option elements (in document order)&nbsp;and offers methods and properties for traversing the list as well as optionally altering its items. This type is returned solely by the "options" property of <a title="En/DOM/select" rel="internal" href="https://developer.mozilla.org/en/DOM/HTMLSelectElement">select</a>.
  */
public interface HTMLOptionsCollection extends HTMLCollection {


  /**
    * As optionally allowed by the spec, Mozilla allows this property to be set, either removing options at the end when using a shorter length, or adding blank options at the end when setting a longer length. Other implementations could potentially throw a <a title="En/DOM/DOMException" rel="internal" href="https://developer.mozilla.org/En/DOM/DOMException">DOMException</a>.
    */
  int getLength();

  void setLength(int arg);

  int getSelectedIndex();

  void setSelectedIndex(int arg);

  void remove(int index);
}
