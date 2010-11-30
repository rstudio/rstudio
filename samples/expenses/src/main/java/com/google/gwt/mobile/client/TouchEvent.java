/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.mobile.client;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.NativeEvent;

/**
 * TODO: doc.
 */
public class TouchEvent extends NativeEvent {

  protected TouchEvent() {
  }

  public final native double getTimeStamp() /*-{
    return this.timeStamp;
  }-*/;

  public final native JsArray<Touch> getOldTouchesUntilMyFriendFredSauerCleansUpTheSample()/*-{
    return this.touches;
  }-*/;

  public final native void setTimeStamp(double t)/*-{
    this.timeStamp = t;
  }-*/;
}
