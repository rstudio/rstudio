/*
 * Copyright 2007 Google Inc.
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
 * Implement this interface to receive closing events from the browser window.
 *
 * @see com.google.gwt.user.client.Window#addWindowCloseListener(WindowCloseListener)
 * @deprecated use {@link Window.ClosingHandler} and
 *             {@link com.google.gwt.event.logical.shared.CloseHandler} instead
 *
 */
@Deprecated
public interface WindowCloseListener extends java.util.EventListener {

  /**
   * Fired just before the browser window closes or navigates to a different
   * site. No user-interface may be displayed during shutdown.
   *
   * @return non-<code>null</code> to present a confirmation dialog that asks
   *         the user whether or not she wishes to navigate away from the page.
   *         The string returned will be displayed in the close confirmation
   *         dialog box. If multiple listeners return messages, the first will
   *         be displayed; all others will be ignored.
   */
  @Deprecated
  String onWindowClosing();

  /**
   * Fired after the browser window closes or navigates to a different site.
   * This event cannot be canceled, and is used mainly to clean up application
   * state and/or save state to the server.
   */
  @Deprecated
  void onWindowClosed();
}
