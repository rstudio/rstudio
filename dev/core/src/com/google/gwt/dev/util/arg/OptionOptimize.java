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
package com.google.gwt.dev.util.arg;

/**
 * An option that can indicates to restrict optimization in favor of a 
 * faster compile time.
 */
public interface OptionOptimize {
  int OPTIMIZE_LEVEL_DRAFT = 0;
  int OPTIMIZE_LEVEL_MAX = 9;

  int getOptimizationLevel();

  void setOptimizationLevel(int level);
}
