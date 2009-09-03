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
#include <string>
#include <cstring>
#include <vector>

#include "AllowedConnections.h"


// TODO(jat): do we need to protect against parallel access to static state?
//    current browsers only use one thread, but Chrome is multithreaded, though
//    it isn't clear if plugins see parallel execution.  For now, we will
//    assume the caller takes responsibility for making sure there are no
//    concurrent calls into AllowedConnections.
std::vector<AllowedConnections::Rule> AllowedConnections::rules;

/**
 * Get the host portion of the URL, not including the port.
 *
 * @return the host portion of the URL, or the unmodified URL if it does not appear
 *     to be valid
 */
std::string AllowedConnections::getHostFromUrl(const std::string& url) {
  int protoEnd = url.find("://");
  if (protoEnd == std::string::npos) {
    Debug::log(Debug::Debugging) << "getHostFromUrl(" << url
        << ") - no :// in URL" << Debug::flush;
    return url;
  }
  protoEnd += 3; // skip over "://"
  int hostEnd = url.find('/', protoEnd);
  if (hostEnd == std::string::npos) {
    hostEnd = url.length();
  }
  int colon = url.find(':', protoEnd);
  if (colon == std::string::npos || colon > hostEnd) {
    colon = hostEnd;
  }
  std::string host = url.substr(protoEnd, colon - protoEnd);
  return host;
}

bool AllowedConnections::matchesRule(const std::string& url,
    bool* allowed) {
  std::string host = getHostFromUrl(url);
  // always allow localhost, localhost.* or 127.0.0.1 for the host
  // TODO(jat): try and get IP addresses of local interfaces?
  if (host == "localhost" || host.find("localhost.") == 0
      || host == "127.0.0.1") {
    *allowed = true;
    return true;
  }
  Debug::log(Debug::Spam) << "Checking host " << host << Debug::flush;
  for (std::vector<AllowedConnections::Rule>::const_iterator it = rules.begin();
       it != rules.end(); ++it) {
    Debug::log(Debug::Spam) << "  comparing to " << it->getPattern()
        << Debug::flush;
    // TODO(jat): add support for regexes
    if (host == it->getPattern()) {
      *allowed = !it->isExcluded();
      return true;
    }
  }
  Debug::log(Debug::Info)
      << "GWT Development Mode connection requested by unknown web server "
      << host << Debug::flush;
  return false;
}

void AllowedConnections::addRule(const std::string& pattern,
    bool exclude) {
  Debug::log(Debug::Spam) << "AllowedConnections::addRule(pattern=" << pattern
      << ", excl=" << exclude << ")" << Debug::flush;
  rules.push_back(AllowedConnections::Rule(pattern, exclude));
}

void AllowedConnections::clearRules() {
  rules.clear();
}

void AllowedConnections::initFromAccessList(const std::string& accessList) {
  clearRules();
  int n = accessList.length();
  for (int i = 0; i < n; ) {
    bool exclude = false;
    if (accessList[i] == '!') {
      exclude = true;
      ++i;
    }
    int comma = i - 1; // for pre-increment below
    while (++comma < n && accessList[comma] != ','); // empty
    addRule(accessList.substr(i, comma - i), exclude);
    i = comma + 1;
  }
}
