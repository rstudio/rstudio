/*
 * UrlPortsTests.cpp
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

#include <gtest/gtest.h>
#include <server_core/UrlPorts.hpp>

namespace rstudio {
namespace server_core {

TEST(UrlPortsTest, PortsAreTransformed)
{
   std::string token("fb7559983669");
   EXPECT_EQ(transformPort(token, 4991), "1235cc15");
   EXPECT_EQ(transformPort(token, 5990), "1a2be938");
   EXPECT_EQ(transformPort(token, 5252), "d4167a28");
   EXPECT_EQ(transformPort(token, 6600), "f2094b4e");
   EXPECT_EQ(transformPort(token, 6600, true), "9f2094b4e");
}

TEST(UrlPortsTest, PortsAreDetransformed)
{
   std::string token("fb7559983669");
   bool server;
   EXPECT_EQ(4991, detransformPort(token, "1235cc15", server));
   EXPECT_FALSE(server);
   EXPECT_EQ(5990, detransformPort(token, "1a2be938", server));
   EXPECT_FALSE(server);
   EXPECT_EQ(5252, detransformPort(token, "d4167a28", server));
   EXPECT_FALSE(server);
   EXPECT_EQ(6600, detransformPort(token, "f2094b4e", server));
   EXPECT_FALSE(server);
   EXPECT_EQ(6600, detransformPort(token, "9f2094b4e", server));
   EXPECT_TRUE(server);
   EXPECT_EQ(-1, detransformPort(token, "1f2094b4e", server));
   EXPECT_EQ(-1, detransformPort(token, "nonsense", server));
}

TEST(UrlPortsTest, UniqueTokensAreGenerated)
{
   std::string token1 = generateNewPortToken();
   std::string token2 = generateNewPortToken();
   EXPECT_EQ(12, token1.length());
   EXPECT_EQ(12, token2.length());
   EXPECT_NE(token1, token2);
}

TEST(UrlPortsTest, UrlsAreTransformed)
{
   std::string token("f670d35125b1");
   std::string path;
   portmapPathForLocalhostUrl("http://localhost:4991/foo", token, &path);
   EXPECT_EQ("p/997a18f1/foo", path);
}

} // namespace server_core
} // namespace rstudio

