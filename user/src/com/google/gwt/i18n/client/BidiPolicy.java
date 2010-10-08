/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.i18n.client;

import com.google.gwt.core.client.GWT;

/**
 * Provides low-level functionality to determine whether to support bidi.
 */
public class BidiPolicy {
  /**
   * Implementation class for {@link BidiPolicy}.
   */
  public static class BidiPolicyImpl {
    public boolean isBidiEnabled() {
      return LocaleInfo.hasAnyRTL();
    }
  }

  /**
   * Implementation class for {@link BidiPolicy} used when bidi is always on.
   */
  public static class BidiPolicyImplOn extends BidiPolicyImpl {
    @Override
    public boolean isBidiEnabled() {
      return true;
    }
  }

  private static BidiPolicyImpl impl = GWT.create(BidiPolicyImpl.class);

  /**
   * Returns true if bidi is enabled, false if disabled.
   */
  public static boolean isBidiEnabled() {
    return impl.isBidiEnabled();
  }
}
