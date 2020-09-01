/*
 * UrlPorts.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SERVER_CORE_URL_PORTS_HPP
#define SERVER_CORE_URL_PORTS_HPP

// See notes in UrlPorts.cpp for an explanation of how these methods are used to obscure ports in
// URLs.

#define kDefaultPortToken "a433e59dc087"
#define kPortTokenCookie  "port-token"

#include <string>

namespace rstudio {
namespace server_core {

std::string transformPort(const std::string& token, int port, bool server = false);

int detransformPort(const std::string& token, const std::string& port, bool& server);

bool portmapPathForLocalhostUrl(const std::string& url, const std::string& token, 
      std::string* pPath);

std::string generateNewPortToken();

}  // namespace server_core
}  // namespace rstudio

#endif
