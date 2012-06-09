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
  * The <code>title</code> object exposes the <a target="_blank" class=" external" rel="external" href="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-79243169" title="http://www.w3.org/TR/DOM-Level-2-HTML/html.html#ID-79243169">HTMLTitleElement</a> interface which contains the title for a document.&nbsp; This element inherits all of the properties and methods described in the <a title="en/DOM/element" class="internal" rel="internal" href="https://developer.mozilla.org/en/DOM/element">element</a> section.
  */
public interface TitleElement extends Element {


  /**
    * Gets or sets the text content of the document's title.
    */
  String getText();

  void setText(String arg);
}
