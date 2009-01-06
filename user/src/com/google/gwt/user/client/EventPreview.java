/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.user.client;

/**
 * A listener interface for previewing browser events.
 * 
 * @see com.google.gwt.user.client.DOM#addEventPreview(EventPreview)
 * @deprecated replaced by
 *             {@link com.google.gwt.user.client.Event.NativePreviewHandler}
 */
@Deprecated
public interface EventPreview {

  /**
   * Called when a browser event occurs and this event preview is on top of the
   * preview stack.
   * 
   * @param event the browser event
   * @return <code>false</code> to cancel the event
   * @see DOM#addEventPreview(EventPreview)
   * @deprecated replaced by
   *             {@link com.google.gwt.user.client.Event.NativePreviewHandler#onPreviewNativeEvent(com.google.gwt.user.client.Event.NativePreviewEvent)}
   */
  @Deprecated
  boolean onEventPreview(Event event);
}
