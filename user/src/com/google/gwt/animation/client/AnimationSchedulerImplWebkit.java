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
 * @see <a href="http://webstuff.nfshost.com/anim-timing/Overview.html"> webkit
 *      draft spec</a>
 */
class AnimationSchedulerImplWebkit extends AnimationSchedulerImpl {

  /**
   * Webkit implementation of {@link AnimationScheduler.AnimationHandle}. Webkit
   * provides the request ID as a double.
   */
  private class AnimationHandleImpl extends AnimationHandle {
    private final double requestId;

    public AnimationHandleImpl(double requestId) {
      this.requestId = requestId;
    }

    @Override
    public void cancel() {
      cancelAnimationFrameImpl(requestId);
    }
  }

  @Override
  public AnimationHandle requestAnimationFrame(AnimationCallback callback, Element element) {
    double requestId = requestAnimationFrameImpl(callback, element);
    return new AnimationHandleImpl(requestId);
  }

  @Override
  protected native boolean isNativelySupported() /*-{
    return !!($wnd.webkitRequestAnimationFrame && $wnd.webkitCancelRequestAnimationFrame);
  }-*/;

  private native void cancelAnimationFrameImpl(double requestId) /*-{
    $wnd.webkitCancelRequestAnimationFrame(requestId);
  }-*/;

  private native double requestAnimationFrameImpl(AnimationCallback callback, Element element) /*-{
    var _callback = callback;
    var wrapper = $entry(function() {
      // Older versions of Chrome pass the current timestamp, but newer versions pass a
      // high resolution timer. We normalize on the current timestamp.
      var now = @com.google.gwt.core.client.Duration::currentTimeMillis()();
      _callback.@com.google.gwt.animation.client.AnimationScheduler.AnimationCallback::execute(D)(now);
    });
    return $wnd.webkitRequestAnimationFrame(wrapper, element);
  }-*/;
}
