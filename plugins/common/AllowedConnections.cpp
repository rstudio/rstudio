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
  //skip over user:passwd@ if it exists
  int userPassword = url.find( '@', protoEnd );
  if (userPassword == std::string::npos || userPassword > hostEnd)
  {
    userPassword = protoEnd;
  }

  int colon = url.find(':', userPassword);
  if (colon == std::string::npos || colon > hostEnd) {
    colon = hostEnd;
  }
  std::string host = url.substr(userPassword, colon - userPassword);
  return host;
}

std::string AllowedConnections::getCodeServerFromUrl(const std::string& url) {
  int queryStart = url.find("?");
  if (queryStart == std::string::npos) {
    Debug::log(Debug::Debugging) << "getCodeServerFromUrl(" << url
        << ") - no ? in URL" << Debug::flush;
    return "";
  }
  ++queryStart; //skip the ?

  int paramStart = url.find("gwt.codesvr=", queryStart);
  if (paramStart == std::string::npos) {
    Debug::log(Debug::Debugging) << "getCodeServerFromUrl(" << url
        << ") - missing gwt.codesvr in URL" << Debug::flush;
    return "";
  }
  paramStart += 12;

  int colon = url.find(':', paramStart);
  int variableEnd = url.find('&', paramStart);

  if ( variableEnd == std::string::npos || colon < variableEnd) {
    variableEnd = colon; //could be std::string::npos!
  }
  Debug::log(Debug::Spam) << "getCodeServerFromUrl(" << url
      << ") - gwt.codesvr=" <<
      url.substr(paramStart, variableEnd-paramStart) << " in URL"
      << Debug::flush;

  return url.substr(paramStart, variableEnd-paramStart);
}

bool AllowedConnections::matchesRule(const std::string& webHost,
                                     const std::string& codeServer,
                                     bool* allowed) {
  std::string host = webHost;
  std::string server = codeServer;

  //Remap variants of localhost
  if (host.find("localhost.") == 0 || host == "127.0.0.1") {
    host = "localhost";
  }

  if (server.find("localhost.") == 0 || server == "127.0.0.1" )
  {
    server = "localhost";
  }

  // always allow localhost
  // TODO(jat): try and get IP addresses of local interfaces?
  if (host == "localhost" && server == "localhost") {
    *allowed = true;
    return true;
  }

  Debug::log(Debug::Spam) << "Checking webHost(" << webHost
      << "), codeServer(" << codeServer << ") " << Debug::flush;
  for (std::vector<AllowedConnections::Rule>::const_iterator it = rules.begin();
       it != rules.end(); ++it) {
    Debug::log(Debug::Spam) << "  comparing to webHost=(" << it->getWebHost()
        << ") codeServer=(" << it->getCodeServer() << ")" << Debug::flush;
    // TODO(jat): add support for regexes
  if (webHost == it->getWebHost() && codeServer == it->getCodeServer()) {
      *allowed = !it->isExcluded();
      Debug::log(Debug::Spam) << "    found! allowed=" << *allowed
          << Debug::flush;
      return true;
    }
  }
  Debug::log(Debug::Info)
      << "GWT Development Mode connection requested by unknown web server "
      << webHost << ", code server " << codeServer << Debug::flush;
  return false;
}

void AllowedConnections::addRule(const std::string& webHost, 
                                 const std::string& codeServer, 
                                 bool exclude) {
  Debug::log(Debug::Spam) << "AllowedConnections::addRule(webHost=" << webHost
      << ", codeServer=" << codeServer << ", excl=" << exclude << ")"
      << Debug::flush;
  rules.push_back(AllowedConnections::Rule(webHost, codeServer, exclude));
}

void AllowedConnections::clearRules() {
  rules.clear();
}

void AllowedConnections::initFromAccessList(const std::string& accessList) {
  Debug::log(Debug::Spam) << "initFromAccessList() accessList="
      << accessList << Debug::flush;
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
    std::string option = accessList.substr(i, comma - i);
    i = comma + 1;

    //parse the [/codeserver] optional element
    int slash = option.find( '/');
    if( slash == std::string::npos ) {
      addRule(option, "localhost", exclude);
    } else {
      addRule(option.substr(0, slash), option.substr(slash+1), exclude);
    }
  }
}
