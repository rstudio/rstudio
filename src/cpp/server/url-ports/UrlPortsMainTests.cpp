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
#include <gtest/gtest.h>

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


TEST(UrlPortsMainTest, ProvidePort)
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

   EXPECT_TRUE(pass);
   EXPECT_FALSE(longOutput);
   EXPECT_EQ(getPort(), std::to_string(port));
   EXPECT_EQ(getPortTokenEnvVar(), portToken);
}

TEST(UrlPortsMainTest, ProvidePortLongOutput)
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

   EXPECT_TRUE(pass);
   EXPECT_TRUE(longOutput);
   EXPECT_EQ(getPort(), std::to_string(port));
   EXPECT_EQ(getPortTokenEnvVar(), portToken);
}

TEST(UrlPortsMainTest, ProvidePortAndToken)
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
   EXPECT_TRUE(pass);
   EXPECT_FALSE(longOutput);
   EXPECT_EQ(getPort(), std::to_string(port));
   EXPECT_EQ(getPortTokenEnvVar(), portToken);
}

TEST(UrlPortsMainTest, ProvidePortAndTokenLongOutput)
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
   EXPECT_TRUE(pass);
   EXPECT_TRUE(longOutput);
   EXPECT_EQ(getPort(), std::to_string(port));
   EXPECT_EQ(getPortTokenEnvVar(), portToken);
}

TEST(UrlPortsMainTest, BuildProxiedUrlAtDomainRoot)
{
   EXPECT_EQ(buildProxiedUrl("https://host/", "/s/abc123/", "58fab3e4"),
             "https://host/s/abc123/p/58fab3e4/");
}

TEST(UrlPortsMainTest, BuildProxiedUrlUnderProxySubPath)
{
   EXPECT_EQ(buildProxiedUrl("https://host/rstudio/", "/s/abc123/", "58fab3e4"),
             "https://host/rstudio/s/abc123/p/58fab3e4/");
}

TEST(UrlPortsMainTest, BuildProxiedUrlWithoutSessionUrl)
{
   // RStudio Server open source has no session segment, so the server url must
   // keep the slash separating the authority from the path.
   EXPECT_EQ(buildProxiedUrl("http://localhost:8787/", "", "58fab3e4"),
             "http://localhost:8787/p/58fab3e4/");
}

TEST(UrlPortsMainTest, BuildProxiedUrlWithoutServerUrl)
{
   // An unset RS_SERVER_URL reaches this as an empty string.
   EXPECT_EQ(buildProxiedUrl("", "/s/abc123/", "58fab3e4"),
             "/s/abc123/p/58fab3e4/");
}

TEST(UrlPortsMainTest, BuildProxiedUrlServerUrlWithoutTrailingSlash)
{
   EXPECT_EQ(buildProxiedUrl("https://host", "/s/abc123/", "58fab3e4"),
             "https://host/s/abc123/p/58fab3e4/");
}

TEST(UrlPortsMainTest, BuildProxiedUrlWithNeitherUrl)
{
   // Both variables can be unset, for instance before the session's first
   // client_init. The port path must still be rooted.
   EXPECT_EQ(buildProxiedUrl("", "", "58fab3e4"), "/p/58fab3e4/");
}

TEST(UrlPortsMainTest, BuildProxiedUrlServerUrlWithoutTrailingSlashOrSessionUrl)
{
   EXPECT_EQ(buildProxiedUrl("https://host", "", "58fab3e4"),
             "https://host/p/58fab3e4/");
}
