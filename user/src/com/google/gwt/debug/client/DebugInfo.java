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
 * Provides low-level functionality to support the creation of testing and
 * diagnostic frameworks.
 * 
 * @see com.google.gwt.user.client.ui.UIObject#ensureDebugId(String)
 */
public class DebugInfo {
  /**
   * Implementation class for {@link DebugInfo}.
   */
  private static class DebugInfoImpl {
    private String debugIdPrefix = DEFAULT_DEBUG_ID_PREFIX;
    private String debugIdAttribute = "id";
    private boolean debugIdAsProperty = true;

    public String getDebugIdAttribute() {
      return debugIdAttribute;
    }

    public String getDebugIdPrefix() {
      return debugIdPrefix;
    }

    public boolean isDebugIdAsProperty() {
      return debugIdAsProperty;
    }

    public boolean isDebugIdEnabled() {
      return false;
    }

    public void setDebugIdAttribute(String attribute, boolean asProperty) {
      this.debugIdAttribute = attribute;
      this.debugIdAsProperty = asProperty;
    }

    public void setDebugIdPrefix(String prefix) {
      this.debugIdPrefix = prefix;
    }
  }

  /**
   * Implementation class for {@link DebugInfo} used when debug IDs are enabled.
   */
  @SuppressWarnings("unused")
  private static class DebugInfoImplEnabled extends DebugInfoImpl {
    @Override
    public boolean isDebugIdEnabled() {
      return true;
    }
  }

  public static final String DEFAULT_DEBUG_ID_PREFIX = "gwt-debug-";
  private static DebugInfoImpl impl = GWT.create(DebugInfoImpl.class);

  /**
   * Returns the element attribute or property where the debug ID is set.
   * Defaults to the element id property. Use {@link #isDebugIdAsProperty()} to
   * determine if the value is a property or attribute.
   */
  public static String getDebugIdAttribute() {
    return impl.getDebugIdAttribute();
  }

  /**
   * Returns the prefix string used for debug ids. Defaults to "gwt-debug-".
   */
  public static String getDebugIdPrefix() {
    return impl.getDebugIdPrefix();
  }

  /**
   * Returns true if the debug ID should be set as a property instead of an
   * attribute.
   */
  public static boolean isDebugIdAsProperty() {
    return impl.isDebugIdAsProperty();
  }

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

  /**
   * Sets the element attribute to assign the debug ID.
   * 
   * @param attribute an element property
   * @param asProperty true to set the debug ID as a property instead of an
   *          attribute
   */
  public static void setDebugIdAttribute(String attribute, boolean asProperty) {
    impl.setDebugIdAttribute(attribute, asProperty);
  }

  /**
   * Sets the prefix string used for debug IDs.
   */
  public static void setDebugIdPrefix(String prefix) {
    impl.setDebugIdPrefix(prefix);
  }
}
