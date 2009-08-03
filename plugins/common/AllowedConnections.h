#ifndef _H_AllowedConnections
#define _H_AllowedConnections
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

#include <string>

/**
 * Manages rules to control access to other sites.
 */
class AllowedConnections {
public:
  AllowedConnections() {
    init();
  }
  
  AllowedConnections(const std::string& rule) {
    init();
    parseRule(rule);
  }

  /**
   * Returns true if a connection to the requested target is allowed.
   * 
   * @param target host or host:port to test
   */
  bool isAllowed(const std::string& target);

  /**
   * Returns true if a connection to the requested target is allowed.
   * 
   * @param host name or address to connect to
   * @param port TCP port to connect to
   */
  bool isAllowed(const char* host, int port);

private:
  void init();
  void parseRule(const std::string& rule);
};

#endif
