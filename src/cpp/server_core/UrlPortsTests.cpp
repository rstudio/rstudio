/*
 * UrlPortsTests.cpp
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

#include <tests/TestThat.hpp>
#include <server_core/UrlPorts.hpp>

namespace rstudio {
namespace server_core {

test_context("URL Port Transformation")
{
   test_that("ports are transformed")
   {
      std::string token("fb7559983669");
      expect_equal(transformPort(token, 4991), "1235cc15");
      expect_equal(transformPort(token, 5990), "1a2be938");
      expect_equal(transformPort(token, 5252), "d4167a28");
      expect_equal(transformPort(token, 6600), "f2094b4e");
      expect_equal(transformPort(token, 6600, true), "9f2094b4e");
   }

   test_that("ports are detransformed")
   {
      std::string token("fb7559983669");
      bool server;
      expect_equal(detransformPort(token, "1235cc15", server), 4991);
      expect_equal(server, false);
      expect_equal(detransformPort(token, "1a2be938", server), 5990);
      expect_equal(server, false);
      expect_equal(detransformPort(token, "d4167a28", server), 5252);
      expect_equal(server, false);
      expect_equal(detransformPort(token, "f2094b4e", server), 6600);
      expect_equal(server, false);
      expect_equal(detransformPort(token, "9f2094b4e", server), 6600);
      expect_equal(server, true);
      expect_equal(detransformPort(token, "1f2094b4e", server), -1);
      expect_equal(detransformPort(token, "nonsense", server), -1);
   }

   test_that("unique tokens are generated")
   {
      std::string token1 = generateNewPortToken();
      std::string token2 = generateNewPortToken();
      expect_true(token1.length() == 12);
      expect_true(token2.length() == 12);
      expect_false(token1 == token2);
   }

   test_that("urls are transformed")
   {
      std::string token("f670d35125b1");
      std::string path;
      portmapPathForLocalhostUrl("http://localhost:4991/foo", token, &path);
      expect_equal(path, "p/997a18f1/foo");
   }
}

} // namespace server_core
} // namespace rstudio

