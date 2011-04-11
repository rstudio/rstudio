/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.animation.client;

import com.google.gwt.dom.client.Element;

/**
 * Implementation using <code>webkitRequestAnimationFrame</code> and
 * <code>webkitCancelRequestAnimationFrame</code>.
 *
 * @see <a
 *      href="http://www.chromium.org/developers/web-platform-status#TOC-requestAnimationFrame">
 *      Chromium Web Platform Status</a>
 * @see <a href="http://webstuff.nfshost.com/anim-timing/Overview.html">
 *      webkit draft spec</a>
 */
class AnimationImplWebkitAnimTiming extends AnimationImpl {

  private static native void cancel(double handle) /*-{
    $wnd.webkitCancelRequestAnimationFrame(handle);
  }-*/;

  private double handle;

  @Override
  public void cancel(Animation animation) {
    cancel(handle);
  }

  @Override
  public void run(Animation animation, Element element) {
    handle = nativeRun(animation, element);
  }

  private native double nativeRun(Animation animation, Element element) /*-{
    var self = this;
    var callback = $entry(function(time) {
      // Chrome 10 does not pass the 'time' argument, so we fake it.
      time = time || @com.google.gwt.core.client.Duration::currentTimeMillis()();
      var complete = self.@com.google.gwt.animation.client.AnimationImpl::updateAnimation(Lcom/google/gwt/animation/client/Animation;D)(animation, time);
      if (!complete) {
        self.@com.google.gwt.animation.client.AnimationImplWebkitAnimTiming::handle = $wnd.webkitRequestAnimationFrame(callback, element);
      }
    });

    return $wnd.webkitRequestAnimationFrame(callback, element);
  }-*/;
}
