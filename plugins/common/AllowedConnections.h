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
#include <vector>
#include <utility>

/**
 * Manages rules to control access to other sites from the plugin.  This is
 * important since arbitrary web pages could try and use the plugin to connect
 * to hosts the browser's machine has access to, such as doing port scanning
 * behind a firewall.
 */
class AllowedConnections {
public:
  /**
   * Add a rule to match new requests against.
   *
   * @param pattern pattern to match
   * @param exclude true if matches should be excluded instead of included
   */
  static void addRule(const std::string& webHost,
      const std::string& codeServer,
      bool exclude = false);

  /**
   * Clear all rules.
   */
  static void clearRules();

  /**
   * Get the host portion of the URL, not including the port.
   *
   * @return the host portion of the URL, or the unmodified URL if it does not
   *     appear to be valid
   */
  static std::string getHostFromUrl(const std::string& url);

  /**
   * Get the code server  value from the URL, not including the port
   *
   * @return the first found server in the URL, or the unmodified URL if it
   *     does not appear to be valid
   */
  static std::string getCodeServerFromUrl(const std::string& url);

  /**
   * Clear any existing rules and reinitialize from the supplied access list.
   *
   * This access list is of the form:
   *    [!]pattern,[!]pattern...
   * where the optional exclamation indicates the following pattern is to be
   * excluded, and an arbitrary number of patterns may be supplied with the
   * first match being used.  Each pattern currently is only an exact literal
   * match against the host name, but will be extended to support simple
   * wildcard patterns.
   */
  static void initFromAccessList(const std::string& accessList);

  /**
   * Returns true if the server for the requested URL matched any rule in
   * our access list, and sets a flag based on whether that rule permits or
   * denies the request.  A host name of localhost or 127.0.0.1 is always
   * allowed.
   *
   * @param hostname host name of webserver or codeserver
   * @param allowed pointer to return value indiciating that this URL should
   *     be allowed to initiate GWT development mode connections
   * @return true if url matched a rule
   */
  static bool matchesRule(const std::string& webHost, 
      const std::string& codeServer,
      bool* allowed);

private:
  AllowedConnections() {
  }

  /**
   * Internal class used for representing a rule.
   */
  class Rule {
  public:
    Rule(const std::string& webHost,
        const std::string& codeServer,
        bool exclude)
        : webhost(webHost), codesvr(codeServer), excluded(exclude) {}

    const std::string& getWebHost() const {
      return webhost;
    }

    const std::string& getCodeServer() const {
      return codesvr;
    }

    bool isExcluded() const {
      return excluded;
    }

  private:
    std::string webhost;
    std::string codesvr;
    bool        excluded;
  };

  static std::vector<Rule> rules;
};

#endif
