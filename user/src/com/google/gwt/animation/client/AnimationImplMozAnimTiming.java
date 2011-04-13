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
 * Implementation using <code>mozRequestAnimationFrame</code>.
 *
 * @see <a href="https://developer.mozilla.org/en/DOM/window.mozRequestAnimationFrame">
 *      Documentation on the MDN</a>
 */
class AnimationImplMozAnimTiming extends AnimationImpl {

  private int handle;

  @Override
  public void cancel(Animation animation) {
    handle++;
  }

  @Override
  public void run(Animation animation, Element element) {
    handle++;
    nativeRun(animation);
  }

  private native void nativeRun(Animation animation) /*-{
    var self = this;
    var handle = this.@com.google.gwt.animation.client.AnimationImplMozAnimTiming::handle;
    var callback = $entry(function(time) {
      if (handle != self.@com.google.gwt.animation.client.AnimationImplMozAnimTiming::handle) {
        return; // cancelled
      }
      var complete = self.@com.google.gwt.animation.client.AnimationImpl::updateAnimation(Lcom/google/gwt/animation/client/Animation;D)(animation, time);
      if (!complete) {
        $wnd.mozRequestAnimationFrame(callback);
      }
    });

    $wnd.mozRequestAnimationFrame(callback);
  }-*/;
}
