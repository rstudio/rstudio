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
package com.google.gwt.debug.client;

import com.google.gwt.core.client.GWT;

/**
 * This class provides a set of static methods related to Debugging.
 */
public class DebugInfo {
  /**
   * Implementation class for {@link DebugInfo}.
   */
  public static class DebugInfoImpl {
    public boolean isDebugIdEnabled() {
      return false;
    }
  }

  /**
   * Implementation class for {@link DebugInfo} used when debug IDs are enabled.
   */
  public static class DebugInfoImplEnabled extends DebugInfoImpl {
    @Override
    public boolean isDebugIdEnabled() {
      return true;
    }
  }

  private static DebugInfoImpl impl = GWT.create(DebugInfoImpl.class);

  /**
   * Returns true if debug IDs are enabled such that calls to
   * {@link com.google.gwt.user.client.ui.UIObject#ensureDebugId(String)} will
   * set DOM IDs on the {@link com.google.gwt.user.client.ui.UIObject} and its
   * important sub elements.
   * 
   * @return true if debug IDs are enabled, false if disabled.
   * @see com.google.gwt.user.client.ui.UIObject#ensureDebugId(String)
   */
  public static boolean isDebugIdEnabled() {
    return impl.isDebugIdEnabled();
  }
}
