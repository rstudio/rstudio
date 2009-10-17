/**
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
package com.google.gwt.dev;

import java.util.HashMap;
import java.util.Map;

/**
 * Class used in the module drop-down box so that we can have a unique
 * object in the dropdown.  It is important that identity is maintained, such
 * that if sessionKey, modulePanel, and moduleDisplayName are the same the
 * same SessionModule instance is returned so == means the same as equals.
 */
final class SessionModule {

  /**
   * Used to cache instances so we always return the same SessionModule
   * instance for the same key values.
   */
  private static final Map<SessionModule, SessionModule> instanceCache
      = new HashMap<SessionModule, SessionModule>();

  /**
   * Return a SessionModule instance for a given sessionKey, modulePanel, and
   * moduleDisplayName, re-using an existing instance if it exists. 
   * 
   * @param sessionKey
   * @param modulePanel
   * @param moduleDisplayName
   * @return unique SessionModule instance matching sessionKey and
   *     moduleDisplayName
   */
  public static SessionModule create(String sessionKey,
      Disconnectable modulePanel, String moduleDisplayName) {
    SessionModule sessionModule = new SessionModule(sessionKey, modulePanel,
        moduleDisplayName);
    if (instanceCache.containsKey(sessionModule)) {
      return instanceCache.get(sessionModule);
    }
    instanceCache.put(sessionModule, sessionModule);
    return sessionModule;
  }
  
  // @NotNull
  private final Disconnectable modulePanel;
  
  // @NotNull
  private final String moduleDisplayName;

  // @NotNull
  private final String sessionKey;

  private SessionModule(String sessionKey, Disconnectable modulePanel,
      String moduleDisplayName) {
    if (sessionKey == null) {
      throw new IllegalArgumentException("sessionKey cannot be null");
    }
    if (modulePanel == null) {
      throw new IllegalArgumentException("modulePanel cannot be null");
    }
    if (moduleDisplayName == null) {
      throw new IllegalArgumentException("moduleDisplayName cannot be null");
    }
    this.sessionKey = sessionKey;
    this.modulePanel = modulePanel;
    this.moduleDisplayName = moduleDisplayName;
  }

  // Even though we guarantee identity, we still need equals implemented
  // so we can do so.
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    SessionModule other = (SessionModule) obj;
    return sessionKey.equals(other.sessionKey)
        && modulePanel.equals(other.modulePanel)
        && moduleDisplayName.equals(other.moduleDisplayName);
  }

  public String getModuleDisplayName() {
    return moduleDisplayName;
  }

  public Disconnectable getModulePanel() {
    return modulePanel;
  }
  
  /**
   * @return a unique key representing the session and the module name
   *     within that session.
   */
  public String getStringKey() {
    return sessionKey + moduleDisplayName; 
  }
  
  @Override
  public int hashCode() {
    return sessionKey.hashCode() + 31 * moduleDisplayName.hashCode();
  }

  /**
   * @return a string suitable for human display only, which consists of the
   * module's short name, possibly including a disambiguator for multiple
   * instances of the module name within the same session.
   */
  @Override
  public String toString() {
    return moduleDisplayName;
  }
}