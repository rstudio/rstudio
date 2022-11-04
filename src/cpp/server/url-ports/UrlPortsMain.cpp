/*
 * UrlPortsMain.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <iostream> 
#include <cstdlib>

#include <core/Log.hpp>
#include <core/system/System.hpp>
#include <shared_core/Error.hpp>
#include <server_core/UrlPorts.hpp>
#include <url-ports/UrlPorts.hpp>

using namespace rstudio;
using namespace rstudio::core;

/**
 * This executable is used by programs such as quarto, that need to launch a server on behalf of a given session user. This program takes the port number and a secure token and returns an obfuscated URL that rserver will accept in order to proxy traffic to the specific port for the session user.  
 * 
 * Parameters are provided as command line arguments.
 * @param $1 The port number to be transformed
 * @param $2 (optional) The token to use to transform the port. If no port is provided,
 * the token will be read from RS_PORT_TOKEN
 * 
 * @return The obfuscated port value
 */

int main(int argc, char * const argv[])
{
   int port;
   bool longOutput = false;
   std::string portToken;
   if (!parseArguments(argc, argv, longOutput, &port, &portToken))
   {
      std::cerr << "\nrserver-url: invalid options\n\n"
         "Usage: rserver-url -<OPTIONAL FLAGS> <PORT> <TOKEN>\n"
         "TOKEN is optional when the environment variable RS_PORT_TOKEN is set.\n\n" 
         "Optional flags:\n"
         "   l: long - displays the complete URL.\n\n";

      return EXIT_FAILURE;
   }

   std::string transformedPort = server_core::transformPort(portToken, port);
   if (!longOutput)
   {
      std::cout << transformedPort;
   }
   else
   {
      char* pSessionPath = std::getenv("RS_SESSION_URL");
      std::string sessionPath = pSessionPath != NULL ? std::string(pSessionPath) : std::string();

      char* pServerPath = std::getenv("RS_SERVER_URL");
      std::string serverPath = pServerPath != NULL ? std::string(pServerPath) : std::string();
      if (serverPath.back() == '/')
      {
         serverPath.pop_back();
      }

      std::cout << serverPath + sessionPath + "p/" + transformedPort + "/" << std::endl;
   }
   return EXIT_SUCCESS;
}

