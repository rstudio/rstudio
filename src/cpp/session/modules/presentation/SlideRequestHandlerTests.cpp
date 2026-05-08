/*
 * SlideRequestHandlerTests.cpp
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

#include "SlideRequestHandler.hpp"

#include <gtest/gtest.h>

#include <core/FileSerializer.hpp>

#include <shared_core/FilePath.hpp>

using namespace rstudio::core;
using namespace rstudio::session::modules::presentation;

namespace {

class TempDir
{
public:
   TempDir()
   {
      FilePath::tempFilePath(path_);
      path_.ensureDirectory();
   }

   ~TempDir()
   {
      path_.removeIfExists();
   }

   const FilePath& path() const { return path_; }

   FilePath child(const std::string& name) const
   {
      return path_.completeChildPath(name);
   }

   FilePath childDir(const std::string& name) const
   {
      FilePath dir = path_.completeChildPath(name);
      dir.ensureDirectory();
      return dir;
   }

   FilePath childFile(const std::string& name, const std::string& contents = "x") const
   {
      FilePath file = path_.completeChildPath(name);
      writeStringToFile(file, contents);
      return file;
   }

private:
   FilePath path_;
};

} // anonymous namespace


TEST(SlideRequestHandlerFetchSite, AllowsSameOrigin)
{
   EXPECT_TRUE(isPresentationHelpFetchSiteAllowed("same-origin"));
}

TEST(SlideRequestHandlerFetchSite, AllowsUserInitiatedNavigation)
{
   // Sec-Fetch-Site: none -- typed URL, bookmark, etc.
   EXPECT_TRUE(isPresentationHelpFetchSiteAllowed("none"));
}

TEST(SlideRequestHandlerFetchSite, AllowsMissingHeader)
{
   // Older browsers without Fetch Metadata. Path constraint is the
   // security boundary in this case.
   EXPECT_TRUE(isPresentationHelpFetchSiteAllowed(""));
}

TEST(SlideRequestHandlerFetchSite, RejectsCrossSite)
{
   EXPECT_FALSE(isPresentationHelpFetchSiteAllowed("cross-site"));
}

TEST(SlideRequestHandlerFetchSite, RejectsSameSite)
{
   // same-site is not same-origin (different subdomain on same
   // registrable domain) -- reject for this endpoint.
   EXPECT_FALSE(isPresentationHelpFetchSiteAllowed("same-site"));
}

TEST(SlideRequestHandlerFetchSite, RejectsUnknownValue)
{
   EXPECT_FALSE(isPresentationHelpFetchSiteAllowed("malicious"));
}


TEST(SlideRequestHandlerIsPathWithin, AcceptsFileInDirectory)
{
   TempDir tmp;
   FilePath file = tmp.childFile("doc.md");

   EXPECT_TRUE(isPathWithin(file, tmp.path()));
}

TEST(SlideRequestHandlerIsPathWithin, AcceptsFileInSubdirectory)
{
   TempDir tmp;
   FilePath sub = tmp.childDir("sub");
   FilePath file = sub.completeChildPath("doc.md");
   writeStringToFile(file, "x");

   EXPECT_TRUE(isPathWithin(file, tmp.path()));
}

TEST(SlideRequestHandlerIsPathWithin, RejectsFileOutsideDirectory)
{
   TempDir parent;
   FilePath presDir = parent.childDir("presentation");
   FilePath outside = parent.childFile("outside.md");

   EXPECT_FALSE(isPathWithin(outside, presDir));
}

TEST(SlideRequestHandlerIsPathWithin, RejectsDotDotEscape)
{
   TempDir parent;
   FilePath presDir = parent.childDir("presentation");
   FilePath outside = parent.childFile("outside.md");

   // simulate `presDir.completePath("../outside.md")`
   FilePath resolved = presDir.completePath("../outside.md");
   EXPECT_FALSE(isPathWithin(resolved, presDir));
}

TEST(SlideRequestHandlerIsPathWithin, AcceptsDotDotThatStaysWithin)
{
   TempDir tmp;
   FilePath sub = tmp.childDir("sub");
   FilePath file = tmp.childFile("doc.md");

   // sub/../doc.md canonicalizes to doc.md, which is within tmp
   FilePath resolved = sub.completePath("../doc.md");
   EXPECT_TRUE(isPathWithin(resolved, tmp.path()));
}

TEST(SlideRequestHandlerIsPathWithin, RejectsNonexistentFile)
{
   TempDir tmp;
   FilePath missing = tmp.path().completeChildPath("missing.md");

   EXPECT_FALSE(isPathWithin(missing, tmp.path()));
}

TEST(SlideRequestHandlerIsPathWithin, RejectsNonexistentDirectory)
{
   TempDir tmp;
   FilePath file = tmp.childFile("doc.md");
   FilePath missingDir = tmp.path().completeChildPath("missing-dir");

   EXPECT_FALSE(isPathWithin(file, missingDir));
}

#ifndef _WIN32
TEST(SlideRequestHandlerIsPathWithin, RejectsSymlinkEscape)
{
   TempDir parent;
   FilePath presDir = parent.childDir("presentation");
   FilePath outside = parent.childFile("outside.md");

   // create a symlink inside presDir that points to a file outside it
   FilePath link = presDir.completeChildPath("escape.md");
   int rc = ::symlink(outside.getAbsolutePath().c_str(),
                      link.getAbsolutePath().c_str());
   ASSERT_EQ(rc, 0);

   // canonicalization should resolve the symlink to its real target,
   // which lives outside presDir, so isPathWithin must reject
   EXPECT_FALSE(isPathWithin(link, presDir));
}
#endif
