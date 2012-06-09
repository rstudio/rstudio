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
import elemental.events.Event;

import elemental.events.*;
import elemental.util.*;
import elemental.dom.*;
import elemental.html.*;
import elemental.css.*;
import elemental.stylesheets.*;

import java.util.Date;

/**
  * A <code>DeviceOrientationEvent</code> object describes an event that provides information about the current orientation of the device as compared to the Earth coordinate frame. See <a title="Orientation and motion data explained" rel="internal" href="https://developer.mozilla.org/en/DOM/Orientation_and_motion_data_explained">Orientation and motion data explained</a> for details.
  */
public interface DeviceOrientationEvent extends Event {


  /**
    * This attribute's value is <code>true</code> if the orientation is provided as a difference between the device coordinate frame and the Earth coordinate frame; if the device can't detect the Earth coordinate frame, this value is <code>false</code>. <strong>Read only.</strong>
    */
  boolean isAbsolute();


  /**
    * The current orientation of the device around the Z axis; that is, how far the device is rotated around a line perpendicular to the device. <strong>Read only.</strong>
    */
  double getAlpha();


  /**
    * The current orientation of the device around the X axis; that is, how far the device is tipped forward or backward. <strong>Read only.</strong>
    */
  double getBeta();


  /**
    * <dl><dd>The current orientation of the device around the Y axis; that is, how far the device is turned left or right. <strong>Read only.</strong></dd>
</dl>
<div class="note"><strong>Note:</strong> If the browser is not able to provide notification information, all values are 0.</div>
    */
  double getGamma();

  void initDeviceOrientationEvent(String type, boolean bubbles, boolean cancelable, double alpha, double beta, double gamma, boolean absolute);
}
