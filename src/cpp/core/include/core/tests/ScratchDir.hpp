/*
 * ScratchDir.hpp
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

#ifndef CORE_TESTS_SCRATCH_DIR_HPP
#define CORE_TESTS_SCRATCH_DIR_HPP

// Helpers for tests only; this header pulls in gtest.

#include <gtest/gtest.h>

#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {
namespace tests {

// A new, empty directory to write test files into. The test removes it.
inline FilePath scratchDir()
{
   FilePath dir;
   EXPECT_FALSE(FilePath::tempFilePath(dir));
   EXPECT_FALSE(dir.ensureDirectory());
   return dir;
}

} // namespace tests
} // namespace core
} // namespace rstudio

#endif // CORE_TESTS_SCRATCH_DIR_HPP
