/*
 * Copyright 2013 Google Inc.
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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;

/**
 * {@link AnimationScheduler} implementation that uses standard {@code requestAnimationFrame} API.
 */
class AnimationSchedulerImplStandard extends AnimationScheduler {

  @Override
  public AnimationHandle requestAnimationFrame(AnimationCallback callback, Element element) {
    final JavaScriptObject handle = requestImpl(callback, element);
    return new AnimationHandle() {
      @Override public void cancel() {
        cancelImpl(handle);
      }
    };
  }

  private static native JavaScriptObject requestImpl(AnimationCallback cb, Element element) /*-{
    var callback = $entry(function() {
      var time = @com.google.gwt.core.client.Duration::currentTimeMillis()();
      cb.@com.google.gwt.animation.client.AnimationScheduler.AnimationCallback::execute(D)(time);
    });

    var handle = $wnd.requestAnimationFrame(callback, element);

    // We can not treat handle as JSO in dev-mode if it is a number. Ensure that it is not:
    return {id: handle};
  }-*/;

  private static native void cancelImpl(JavaScriptObject holder) /*-{
    $wnd.cancelAnimationFrame(holder.id);
  }-*/;
}
