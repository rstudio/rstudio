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
 * @see <a
 *      href="https://developer.mozilla.org/en/DOM/window.mozRequestAnimationFrame">
 *      Documentation on the MDN</a>
 */
class AnimationSchedulerImplMozilla extends AnimationSchedulerImpl {

  /**
   * Mozilla implementation of {@link AnimationScheduler.AnimationHandle}.
   * Mozilla does not provide a request ID, so we mark a boolean in the handle
   * and check it in the callback wrapper.
   */
  private class AnimationHandleImpl extends AnimationHandle {
    @SuppressWarnings("unused")
    private boolean canceled;

    @Override
    public void cancel() {
      canceled = true;
    }
  }

  @Override
  public AnimationHandle requestAnimationFrame(AnimationCallback callback, Element element) {
    AnimationHandleImpl handle = new AnimationHandleImpl();
    requestAnimationFrameImpl(callback, handle);
    return handle;
  }

  @Override
  protected native boolean isNativelySupported() /*-{
    return !!$wnd.mozRequestAnimationFrame;
  }-*/;

  /**
   * Request an animation frame. Firefox does not return a request ID, so we
   * create a JavaScriptObject and add an expando named "canceled" to inidicate
   * if the request was canceled. The callback wrapper checks the expando before
   * executing the user callback.
   * 
   * @param callback the user callback to execute
   * @param handle the handle object
   */
  private native void requestAnimationFrameImpl(AnimationCallback callback,
      AnimationHandleImpl handle) /*-{
    var wrapper = $entry(function() {
      if (!handle.@com.google.gwt.animation.client.AnimationSchedulerImplMozilla.AnimationHandleImpl::canceled) {
        // Older versions of firefox pass the current timestamp, but the spec has changed to pass a
        // high resolution timer instead, and newer versions of Firefox will eventually change.
        var now = @com.google.gwt.core.client.Duration::currentTimeMillis()();
        callback.@com.google.gwt.animation.client.AnimationScheduler.AnimationCallback::execute(D)(now);
      }
    });
    $wnd.mozRequestAnimationFrame(wrapper);
  }-*/;
}
