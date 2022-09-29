/*
 * UrlPortsMainTests.cpp
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

#include <stdlib.h>
#include <url-ports/UrlPorts.hpp>
#include <tests/TestThat.hpp>

namespace {

char* getPort()
{
   return (char*) "8050";
}

char* getPortTokenEnvVarSetter()
{
   return (char*) "RS_PORT_TOKEN=91c63048efb0";
}

char* getPortTokenEnvVar()
{
   return (char*) "91c63048efb0";
}

} // anonymous namespace

TEST_CASE("Url Ports Main")
{

   SECTION("Provide port")
   {
      int argc = 2;
      char *args[] = {
         (char*)"",
         getPort(),
         NULL
      };
      putenv(getPortTokenEnvVarSetter());

      bool longOutput = false;
      int port;
      std::string portToken;
      bool pass = parseArguments(argc, args, longOutput, &port, &portToken);

      CHECK(pass == true);
      CHECK(longOutput == false);
      CHECK(std::to_string(port) == getPort());
      CHECK(portToken == getPortTokenEnvVar());
   }

   SECTION("Provide port, long ouput")
   {
      int argc = 3;
      char *args[] = {
         (char*)"",
         (char*)"-l",
         getPort(),
         NULL
      };
      putenv(getPortTokenEnvVarSetter());

      bool longOutput = false;
      int port;
      std::string portToken;
      bool pass = parseArguments(argc, args, longOutput, &port, &portToken);

      CHECK(pass == true);
      CHECK(longOutput == true);
      CHECK(std::to_string(port) == getPort());
      CHECK(portToken == getPortTokenEnvVar());
   }

   SECTION("Provide port and token")
   {
      int argc = 3;
      char *args[] = {
         (char*)"",
         getPort(),
         getPortTokenEnvVar(),
         NULL
      };

      bool longOutput = false;
      int port;
      std::string portToken;

      bool pass = parseArguments(argc, args, longOutput, &port, &portToken);
      CHECK(pass == true);
      CHECK(longOutput == false);
      CHECK(std::to_string(port) == getPort());
      CHECK(portToken == getPortTokenEnvVar());
   }

   SECTION("Provide port and token, long output")
   {
      int argc = 4;
      char *args[] = {
         (char*)"",
         (char*)"-l",
         getPort(),
         getPortTokenEnvVar(),
         NULL
      };

      bool longOutput = false;
      int port;
      std::string portToken;

      bool pass = parseArguments(argc, args, longOutput, &port, &portToken);
      CHECK(pass == true);
      CHECK(longOutput == true);
      CHECK(std::to_string(port) == getPort());
      CHECK(portToken == getPortTokenEnvVar());
   }
}
