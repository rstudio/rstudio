/*
 * ChatConstantsTests.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#include "ChatConstants.hpp"

#include <gtest/gtest.h>

using namespace rstudio::session::modules::chat::constants;

// -- assembleWebSocketPath ---------------------------------------------------

TEST(AssembleWebSocketPath, NoRootPathNoSessionUrl)
{
   // Open-source RStudio Server, no subpath proxy
   EXPECT_EQ(
      assembleWebSocketPath("/", "", "/p/58fab3e4"),
      "/p/58fab3e4/ai-chat");
}

TEST(AssembleWebSocketPath, RootPathWithoutTrailingSlash)
{
   EXPECT_EQ(
      assembleWebSocketPath("/rstudio", "", "/p/58fab3e4"),
      "/rstudio/p/58fab3e4/ai-chat");
}

TEST(AssembleWebSocketPath, RootPathWithTrailingSlash)
{
   EXPECT_EQ(
      assembleWebSocketPath("/rstudio/", "", "/p/58fab3e4"),
      "/rstudio/p/58fab3e4/ai-chat");
}

TEST(AssembleWebSocketPath, SessionUrlWithoutRootPath)
{
   // Workbench without subpath proxy
   EXPECT_EQ(
      assembleWebSocketPath("/", "/s/abc123", "/p/58fab3e4"),
      "/s/abc123/p/58fab3e4/ai-chat");
}

TEST(AssembleWebSocketPath, SessionUrlWithRootPath)
{
   // Workbench behind subpath proxy
   EXPECT_EQ(
      assembleWebSocketPath("/rstudio", "/s/abc123", "/p/58fab3e4"),
      "/rstudio/s/abc123/p/58fab3e4/ai-chat");
}

TEST(AssembleWebSocketPath, SessionUrlWithTrailingSlash)
{
   EXPECT_EQ(
      assembleWebSocketPath("/", "/s/abc123/", "/p/58fab3e4"),
      "/s/abc123/p/58fab3e4/ai-chat");
}

TEST(AssembleWebSocketPath, PortmappedPathWithoutLeadingSlash)
{
   // mapUrlPorts() can return "p/58fab3e4" without leading slash
   EXPECT_EQ(
      assembleWebSocketPath("/", "", "p/58fab3e4"),
      "/p/58fab3e4/ai-chat");
}

TEST(AssembleWebSocketPath, PortmappedPathWithTrailingSlash)
{
   EXPECT_EQ(
      assembleWebSocketPath("/", "", "/p/58fab3e4/"),
      "/p/58fab3e4/ai-chat");
}

TEST(AssembleWebSocketPath, AllComponentsWithTrailingSlashes)
{
   EXPECT_EQ(
      assembleWebSocketPath("/rstudio/", "/s/abc123/", "/p/58fab3e4/"),
      "/rstudio/s/abc123/p/58fab3e4/ai-chat");
}

TEST(AssembleWebSocketPath, IPv6PortmappedPath)
{
   EXPECT_EQ(
      assembleWebSocketPath("/", "", "/p6/58fab3e4"),
      "/p6/58fab3e4/ai-chat");
}

TEST(AssembleWebSocketPath, PortmappedPathIsJustSlash)
{
   // Degenerate case: "/" should collapse to empty, not produce "//ai-chat"
   EXPECT_EQ(
      assembleWebSocketPath("/", "", "/"),
      "/ai-chat");
}

TEST(AssembleWebSocketPath, DeepRootPath)
{
   EXPECT_EQ(
      assembleWebSocketPath("/org/team/rstudio", "", "/p/58fab3e4"),
      "/org/team/rstudio/p/58fab3e4/ai-chat");
}
