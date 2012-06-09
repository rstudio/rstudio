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
package elemental.js.html;
import elemental.html.AudioParam;
import elemental.html.AudioNode;
import elemental.html.AudioContext;

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

public class JsAudioNode extends JsElementalMixinBase  implements AudioNode {
  protected JsAudioNode() {}

  public final native JsAudioContext getContext() /*-{
    return this.context;
  }-*/;

  public final native int getNumberOfInputs() /*-{
    return this.numberOfInputs;
  }-*/;

  public final native int getNumberOfOutputs() /*-{
    return this.numberOfOutputs;
  }-*/;

  public final native void connect(AudioNode destination, int output, int input) /*-{
    this.connect(destination, output, input);
  }-*/;

  public final native void connect(AudioParam destination, int output) /*-{
    this.connect(destination, output);
  }-*/;

  public final native void disconnect(int output) /*-{
    this.disconnect(output);
  }-*/;
}
