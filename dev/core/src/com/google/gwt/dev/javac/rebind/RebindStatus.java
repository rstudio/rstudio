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
package com.google.gwt.dev.javac.rebind;

/**
 * The returned status for a rebind result.  This status is used by the 
 * {@link com.google.gwt.dev.shell.StandardRebindOracle} implementation to 
 * determine how to integrate the result into the system.  In the descriptions 
 * below, the "products" of a rebind result can include updated type 
 * information, newly generated artifacts, and newly generated compilation units.
 */
public enum RebindStatus {
 
  /**
   * Indicates nothing new was created, use pre-existing type information.
   */ 
  USE_EXISTING,
 
  /**
   * Indicates only newly generated products should be used.
   */
  USE_ALL_NEW,
  
  /**
   * Indicates only newly generated products should be used, and no results
   * should be cached, such as in the case where no caching can be taken
   * advantage of.
   */
  USE_ALL_NEW_WITH_NO_CACHING,
  
  /**
   * Indicates only previously cached products should be used.
   */
  USE_ALL_CACHED,
  
  /**
   * Indicates that a mixture of newly generated and previously cached products
   * should be used.
   */
  USE_PARTIAL_CACHED
}
