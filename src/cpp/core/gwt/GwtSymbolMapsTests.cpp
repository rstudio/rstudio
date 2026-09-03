/*
 * GwtSymbolMapsTests.cpp
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

#include <core/gwt/GwtSymbolMaps.hpp>

#include <gtest/gtest.h>

#include <core/FileSerializer.hpp>
#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {
namespace gwt {

namespace {

const char* const kStrongName = "0123456789ABCDEF0123456789ABCDEF";

const std::string kSymbolMapContents =
      "# a comment line\n"
      "Pb,com.example.gwt.Widget::render(I)V,com.example.gwt.Widget,render,com/example/gwt/Widget.java,105,0\n"
      "Qb,com.example.gwt.Widget::layout()V,com.example.gwt.Widget,layout,com/example/gwt/Widget.java,42,0\n";

// 'gzip -n' output for kSymbolMapContents, matching what the packaged
// builds ship in www-symbolmaps
const unsigned char kSymbolMapContentsGz[] = {
   31, 139, 8, 0, 0, 0, 0, 0, 0, 3, 83, 86, 72, 84, 72, 206,
   207, 205, 77, 205, 43, 81, 200, 201, 204, 75, 229, 10, 72, 210, 1, 242,
   245, 82, 43, 18, 115, 11, 114, 82, 245, 210, 203, 75, 244, 194, 51, 83,
   210, 83, 75, 172, 172, 138, 82, 243, 82, 82, 139, 52, 60, 53, 195, 112,
   40, 209, 129, 168, 0, 201, 234, 67, 101, 245, 129, 178, 250, 16, 89, 189,
   172, 196, 178, 68, 29, 67, 3, 83, 29, 3, 174, 64, 220, 182, 228, 36,
   86, 230, 151, 150, 104, 224, 182, 4, 162, 0, 175, 37, 38, 70, 64, 59,
   0, 56, 113, 39, 166, 219, 0, 0, 0
};

} // anonymous namespace

TEST(GwtSymbolMapsTest, ResymbolizesFromPlainSymbolMap)
{
   FilePath mapsDir;
   ASSERT_FALSE(FilePath::tempFilePath(mapsDir));
   ASSERT_FALSE(mapsDir.ensureDirectory());

   FilePath mapPath = mapsDir.completeChildPath(std::string(kStrongName) + ".symbolMap");
   ASSERT_FALSE(writeStringToFile(mapPath, kSymbolMapContents));

   SymbolMaps maps;
   ASSERT_FALSE(maps.initialize(mapsDir));

   StackElement se;
   se.methodName = "Pb";
   se.lineNumber = -1;
   StackElement resymbolized = maps.resymbolize(se, kStrongName);

   EXPECT_EQ("com.example.gwt.Widget", resymbolized.className);
   EXPECT_EQ("render", resymbolized.methodName);
   EXPECT_EQ("com/example/gwt/Widget.java", resymbolized.fileName);
   EXPECT_EQ(105, resymbolized.lineNumber);

   mapsDir.removeIfExists();
}

TEST(GwtSymbolMapsTest, ResymbolizesFromGzippedSymbolMap)
{
   FilePath mapsDir;
   ASSERT_FALSE(FilePath::tempFilePath(mapsDir));
   ASSERT_FALSE(mapsDir.ensureDirectory());

   FilePath gzMapPath = mapsDir.completeChildPath(std::string(kStrongName) + ".symbolMap.gz");
   std::shared_ptr<std::ostream> pOfs;
   ASSERT_FALSE(gzMapPath.openForWrite(pOfs));
   pOfs->write(reinterpret_cast<const char*>(kSymbolMapContentsGz),
               sizeof(kSymbolMapContentsGz));
   ASSERT_TRUE(pOfs->good());
   pOfs.reset();

   SymbolMaps maps;
   ASSERT_FALSE(maps.initialize(mapsDir));

   StackElement se;
   se.methodName = "Qb";
   se.lineNumber = -1;
   StackElement resymbolized = maps.resymbolize(se, kStrongName);

   EXPECT_EQ("com.example.gwt.Widget", resymbolized.className);
   EXPECT_EQ("layout", resymbolized.methodName);
   EXPECT_EQ("com/example/gwt/Widget.java", resymbolized.fileName);
   EXPECT_EQ(42, resymbolized.lineNumber);

   mapsDir.removeIfExists();
}

TEST(GwtSymbolMapsTest, TruncatedGzippedSymbolMapReturnsOriginalElement)
{
   FilePath mapsDir;
   ASSERT_FALSE(FilePath::tempFilePath(mapsDir));
   ASSERT_FALSE(mapsDir.ensureDirectory());

   // strip the 8-byte gzip trailer (CRC + size): the deflate data is intact,
   // so the symbols would still parse if the payload were not validated
   FilePath gzMapPath = mapsDir.completeChildPath(std::string(kStrongName) + ".symbolMap.gz");
   std::shared_ptr<std::ostream> pOfs;
   ASSERT_FALSE(gzMapPath.openForWrite(pOfs));
   pOfs->write(reinterpret_cast<const char*>(kSymbolMapContentsGz),
               sizeof(kSymbolMapContentsGz) - 8);
   ASSERT_TRUE(pOfs->good());
   pOfs.reset();

   SymbolMaps maps;
   ASSERT_FALSE(maps.initialize(mapsDir));

   StackElement se;
   se.className = "obfuscated";
   se.methodName = "Pb";
   se.lineNumber = 7;
   StackElement resymbolized = maps.resymbolize(se, kStrongName);

   EXPECT_EQ(se.className, resymbolized.className);
   EXPECT_EQ(se.methodName, resymbolized.methodName);
   EXPECT_EQ(se.lineNumber, resymbolized.lineNumber);

   mapsDir.removeIfExists();
}

TEST(GwtSymbolMapsTest, CorruptGzippedSymbolMapReturnsOriginalElement)
{
   FilePath mapsDir;
   ASSERT_FALSE(FilePath::tempFilePath(mapsDir));
   ASSERT_FALSE(mapsDir.ensureDirectory());

   FilePath gzMapPath = mapsDir.completeChildPath(std::string(kStrongName) + ".symbolMap.gz");
   ASSERT_FALSE(writeStringToFile(gzMapPath, "not gzip data"));

   SymbolMaps maps;
   ASSERT_FALSE(maps.initialize(mapsDir));

   StackElement se;
   se.className = "obfuscated";
   se.methodName = "Pb";
   se.lineNumber = 7;
   StackElement resymbolized = maps.resymbolize(se, kStrongName);

   EXPECT_EQ(se.className, resymbolized.className);
   EXPECT_EQ(se.methodName, resymbolized.methodName);
   EXPECT_EQ(se.lineNumber, resymbolized.lineNumber);

   mapsDir.removeIfExists();
}

TEST(GwtSymbolMapsTest, RepairedGzippedSymbolMapIsRevalidated)
{
   FilePath mapsDir;
   ASSERT_FALSE(FilePath::tempFilePath(mapsDir));
   ASSERT_FALSE(mapsDir.ensureDirectory());

   FilePath gzMapPath = mapsDir.completeChildPath(std::string(kStrongName) + ".symbolMap.gz");
   ASSERT_FALSE(writeStringToFile(gzMapPath, "not gzip data"));

   SymbolMaps maps;
   ASSERT_FALSE(maps.initialize(mapsDir));

   // the damaged file fails validation, so the lookup comes back unchanged
   StackElement se;
   se.methodName = "Pb";
   se.lineNumber = -1;
   StackElement resymbolized = maps.resymbolize(se, kStrongName);
   EXPECT_EQ(se.methodName, resymbolized.methodName);

   // repair the file: both the symbol requested during the failure and a
   // fresh one must now resolve, since neither the validation failure nor
   // the affected symbols were cached
   std::shared_ptr<std::ostream> pOfs;
   ASSERT_FALSE(gzMapPath.openForWrite(pOfs));
   pOfs->write(reinterpret_cast<const char*>(kSymbolMapContentsGz),
               sizeof(kSymbolMapContentsGz));
   ASSERT_TRUE(pOfs->good());
   pOfs.reset();

   StackElement retried = maps.resymbolize(se, kStrongName);
   EXPECT_EQ("com.example.gwt.Widget", retried.className);
   EXPECT_EQ("render", retried.methodName);
   EXPECT_EQ(105, retried.lineNumber);

   StackElement other;
   other.methodName = "Qb";
   other.lineNumber = -1;
   StackElement recovered = maps.resymbolize(other, kStrongName);

   EXPECT_EQ("com.example.gwt.Widget", recovered.className);
   EXPECT_EQ("layout", recovered.methodName);
   EXPECT_EQ(42, recovered.lineNumber);

   mapsDir.removeIfExists();
}

TEST(GwtSymbolMapsTest, GzippedSymbolMapReadFailureIsRetriable)
{
   FilePath mapsDir;
   ASSERT_FALSE(FilePath::tempFilePath(mapsDir));
   ASSERT_FALSE(mapsDir.ensureDirectory());

   FilePath gzMapPath = mapsDir.completeChildPath(std::string(kStrongName) + ".symbolMap.gz");
   std::shared_ptr<std::ostream> pOfs;
   ASSERT_FALSE(gzMapPath.openForWrite(pOfs));
   pOfs->write(reinterpret_cast<const char*>(kSymbolMapContentsGz),
               sizeof(kSymbolMapContentsGz));
   pOfs.reset();

   // resolve one symbol so the file's validation result is memoized
   SymbolMaps maps;
   ASSERT_FALSE(maps.initialize(mapsDir));

   StackElement se;
   se.methodName = "Pb";
   se.lineNumber = -1;
   StackElement resymbolized = maps.resymbolize(se, kStrongName);
   EXPECT_EQ("render", resymbolized.methodName);

   // damage the file mid-stream: validation is already memoized, so the
   // next lookup reaches the read itself, which must fail without caching
   // the requested symbol as unknown
   ASSERT_FALSE(gzMapPath.openForWrite(pOfs));
   pOfs->write(reinterpret_cast<const char*>(kSymbolMapContentsGz), 30);
   pOfs.reset();

   StackElement other;
   other.methodName = "Qb";
   other.lineNumber = -1;
   StackElement damaged = maps.resymbolize(other, kStrongName);
   EXPECT_EQ("Qb", damaged.methodName);

   // repair the file: the same symbol must now resolve
   ASSERT_FALSE(gzMapPath.openForWrite(pOfs));
   pOfs->write(reinterpret_cast<const char*>(kSymbolMapContentsGz),
               sizeof(kSymbolMapContentsGz));
   pOfs.reset();

   StackElement recovered = maps.resymbolize(other, kStrongName);
   EXPECT_EQ("com.example.gwt.Widget", recovered.className);
   EXPECT_EQ("layout", recovered.methodName);
   EXPECT_EQ(42, recovered.lineNumber);

   mapsDir.removeIfExists();
}

TEST(GwtSymbolMapsTest, UnknownSymbolReturnsOriginalElement)
{
   FilePath mapsDir;
   ASSERT_FALSE(FilePath::tempFilePath(mapsDir));
   ASSERT_FALSE(mapsDir.ensureDirectory());

   SymbolMaps maps;
   ASSERT_FALSE(maps.initialize(mapsDir));

   StackElement se;
   se.className = "obfuscated";
   se.methodName = "Zz";
   se.lineNumber = 7;
   StackElement resymbolized = maps.resymbolize(se, kStrongName);

   EXPECT_EQ(se.className, resymbolized.className);
   EXPECT_EQ(se.methodName, resymbolized.methodName);
   EXPECT_EQ(se.lineNumber, resymbolized.lineNumber);

   mapsDir.removeIfExists();
}

} // namespace gwt
} // namespace core
} // namespace rstudio
