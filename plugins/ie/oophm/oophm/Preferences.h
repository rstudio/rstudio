#ifndef _H_Preferences
#define _H_Preferences
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

#include <string>

/**
 * Deal with getting/storing/updating preferences in the Windows registry.
 */
class Preferences {
private:
  // prevent instantiation
  Preferences() {}

public:
  static void loadAccessList();
  static void addNewRule(const std::string& pattern, bool exclude);
};

#endif
