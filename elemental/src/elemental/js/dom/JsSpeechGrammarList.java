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
package elemental.js.dom;
import elemental.dom.SpeechGrammarList;
import elemental.dom.SpeechGrammar;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.js.stylesheets.*;
import elemental.js.events.*;
import elemental.js.util.*;
import elemental.js.dom.*;
import elemental.js.html.*;
import elemental.js.css.*;
import elemental.js.stylesheets.*;

import java.util.Date;

public class JsSpeechGrammarList extends JsElementalMixinBase  implements SpeechGrammarList {
  protected JsSpeechGrammarList() {}

  public final native int getLength() /*-{
    return this.length;
  }-*/;

  public final native void addFromString(String string) /*-{
    this.addFromString(string);
  }-*/;

  public final native void addFromString(String string, float weight) /*-{
    this.addFromString(string, weight);
  }-*/;

  public final native void addFromUri(String src) /*-{
    this.addFromUri(src);
  }-*/;

  public final native void addFromUri(String src, float weight) /*-{
    this.addFromUri(src, weight);
  }-*/;

  public final native JsSpeechGrammar item(int index) /*-{
    return this.item(index);
  }-*/;
}
