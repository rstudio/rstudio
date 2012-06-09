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
package elemental.dom;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * <code><a title="En/DOM/Text" rel="internal" href="https://developer.mozilla.org/En/DOM/Text">Text</a></code>, <code><a title="En/DOM/Comment" rel="internal" href="https://developer.mozilla.org/En/DOM/Comment">Comment</a></code>, and <code><a title="en/DOM/CDATASection" rel="internal" href="https://developer.mozilla.org/en/DOM/CDATASection">CDATASection</a></code> all implement CharacterData, which in turn also implements <code><a class="internal" title="En/DOM/Node" rel="internal" href="https://developer.mozilla.org/en/DOM/Node">Node</a></code>. See <code>Node</code> for the remaining methods, properties, and constants.
  */
public interface CharacterData extends Node {

  String getData();

  void setData(String arg);

  int getLength();

  void appendData(String data);

  void deleteData(int offset, int length);

  void insertData(int offset, String data);

  void replaceData(int offset, int length, String data);

  String substringData(int offset, int length);
}
