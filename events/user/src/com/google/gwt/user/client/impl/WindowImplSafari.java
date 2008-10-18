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

/**
 * Safari implementation of {@link com.google.gwt.user.client.impl.WindowImpl}.
 */
public class WindowImplSafari extends WindowImpl {
  @Override
  public native int getClientHeight() /*-{
    // Safari 2 and Safari 3 disagree on the clientHeight value.
    // $wnd.devicePixelRatio is only defined in Safari 3.
    // documentRoot.clientWidth works in both Safari 2 and 3, so we do not need
    // an override for the width.
    return $wnd.devicePixelRatio ?
        @com.google.gwt.user.client.impl.DocumentRootImpl::documentRoot.clientHeight :
        $wnd.innerHeight;
  }-*/;

  @Override
  public native int getScrollLeft() /*-{
    return $doc.body.scrollLeft;
  }-*/;

  @Override
  public native int getScrollTop() /*-{
    return $doc.body.scrollTop;
  }-*/;
}
