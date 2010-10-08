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
package com.google.gwt.user.client.ui;

/**
 * A {@link Widget} that uses an animation should implement this class so users
 * can enable or disable animations.
 */
public interface HasAnimation {
  /**
   * Returns true if animations are enabled, false if not.
   */
  boolean isAnimationEnabled();

  /**
   * Enable or disable animations.
   *
   * @param enable true to enable, false to disable
   */
  void setAnimationEnabled(boolean enable);
}
