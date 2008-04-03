/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.user.client.impl;

import com.google.gwt.user.client.Event;

/**
 * Safari implementation of {@link com.google.gwt.user.client.impl.DOMImpl}.
 */
class DOMImplSafari extends DOMImplStandard {

  @Override
  public native int eventGetClientX(Event evt) /*-{
    // In Safari2: clientX is wrong and pageX is returned instead.
    return evt.pageX - $doc.body.scrollLeft || -1;
  }-*/;

  @Override
  public native int eventGetClientY(Event evt) /*-{
    // In Safari2: clientY is wrong and pageY is returned instead.
    return evt.pageY - $doc.body.scrollTop || -1;
  }-*/;

  @Override
  public native int eventGetMouseWheelVelocityY(Event evt) /*-{
    return Math.round(-evt.wheelDelta / 40) || -1;
  }-*/;
}
