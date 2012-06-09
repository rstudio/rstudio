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
import elemental.html.AudioListener;

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

public class JsAudioListener extends JsElementalMixinBase  implements AudioListener {
  protected JsAudioListener() {}

  public final native float getDopplerFactor() /*-{
    return this.dopplerFactor;
  }-*/;

  public final native void setDopplerFactor(float param_dopplerFactor) /*-{
    this.dopplerFactor = param_dopplerFactor;
  }-*/;

  public final native float getSpeedOfSound() /*-{
    return this.speedOfSound;
  }-*/;

  public final native void setSpeedOfSound(float param_speedOfSound) /*-{
    this.speedOfSound = param_speedOfSound;
  }-*/;

  public final native void setOrientation(float x, float y, float z, float xUp, float yUp, float zUp) /*-{
    this.setOrientation(x, y, z, xUp, yUp, zUp);
  }-*/;

  public final native void setPosition(float x, float y, float z) /*-{
    this.setPosition(x, y, z);
  }-*/;

  public final native void setVelocity(float x, float y, float z) /*-{
    this.setVelocity(x, y, z);
  }-*/;
}
