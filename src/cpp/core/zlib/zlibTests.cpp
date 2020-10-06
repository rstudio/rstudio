/*
* zlibTests.cpp
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

#include <core/zlib/zlib.hpp>

#include <tests/TestThat.hpp>

namespace rstudio {
namespace core {
namespace zlib {

test_context("zlib")
{
   test_that("can compress & decompress difficult strings")
   {
      const std::string hardToCompress = "The quick brown fox jumps over the lazy dog.";

      std::vector<unsigned char> compressed;
      std::string uncompressed;
      Error error = compressString(hardToCompress, &compressed);
      REQUIRE(!error);

      error = decompressString(compressed, &uncompressed);
      REQUIRE(!error);

      CHECK(hardToCompress == uncompressed);
   }

   test_that("can compress & decompress easy strings")
   {
      const std::string easyToCompress = "easy easy easy easy easy easy easy easy easy easy";

      std::vector<unsigned char> compressed;
      std::string uncompressed;
      Error error = compressString(easyToCompress, &compressed);
      REQUIRE(!error);

      error = decompressString(compressed, &uncompressed);
      REQUIRE(!error);

      CHECK(easyToCompress == uncompressed);
   }

   test_that("can compress & decompress normal strings")
   {
      const std::string launcherJobName = "rsl-RStudio s12345678904321 (slurmUser1) - postman test-command=cat-args=-E-stdin=test\nsubmit\njob-us=e53ccc2ab4d74c8595596a90f3d2831a-tags=s12345678904321,rstudio-ide,s12345,rstudio-r-session,rstudio-r-session-name:postman test,rstudio-r-session-id:s12345678904321";

      std::vector<unsigned char> compressed;
      std::string uncompressed;
      Error error = compressString(launcherJobName, &compressed);
      REQUIRE(!error);

      error = decompressString(compressed, &uncompressed);
      REQUIRE(!error);

      CHECK(launcherJobName == uncompressed);
   }

   test_that("can compress & decompress empty string")
   {
      const std::string empty = "";

      std::vector<unsigned char> compressed;
      std::string uncompressed;
      Error error = compressString(empty, &compressed);
      REQUIRE(!error);

      error = decompressString(compressed, &uncompressed);
      REQUIRE(!error);

      CHECK(empty == uncompressed);
   }
}

} // namespace zlib
} // namespace core
} // namespace rstudio
