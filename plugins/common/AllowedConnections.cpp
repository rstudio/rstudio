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

#include "Debug.h"

#include "AllowedConnections.h"

void AllowedConnections::init() {
}

bool AllowedConnections::isAllowed(const std::string& target) {
  return true;
}

bool AllowedConnections::isAllowed(const char* host, int port) {
  return true;
}

void AllowedConnections::parseRule(const std::string& rule) {
};
