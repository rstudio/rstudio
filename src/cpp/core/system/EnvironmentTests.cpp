/*
 * EnvironmentTests.cpp
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

#include <shared_core/Error.hpp>

#include <core/system/Environment.hpp>
#include <core/system/Resources.hpp>

#define kLatexStyleLineCommentRegex ("^%+\\s*")

namespace rstudio {
namespace core {
namespace system {

TEST(EnvironmentTest, ExpandAllVariableInstances)
{
   // Simple example
   Options env;
   setenv(&env, "VAR1", "foo");
   setenv(&env, "VAR2", "bar");
   setenv(&env, "VAR3", "baz");

   std::string expanded = expandEnvVars(env,
      "Metasyntactic variables include $VAR1, $VAR2, and $VAR3, "
      "but $VAR1 is used most often.");

   ASSERT_EQ(std::string("Metasyntactic variables include foo, bar, and baz, "
               "but foo is used most often."), expanded);
}

TEST(EnvironmentTest, ReplaceOnlyFullyMatchingVars)
{
   Options env;
   setenv(&env, "VAR", "foo");

   std::string expanded = expandEnvVars(env,
       "I think $VAR is a nice name for a $VARIABLE.");

   EXPECT_EQ(std::string("I think foo is a nice name for a $VARIABLE."), expanded);
}

TEST(EnvironmentTest, ExpandVariablesInBraces)
{
   Options env;
   setenv(&env, "VAR", "foo");

   std::string expanded = expandEnvVars(env, "Don't be ${VAR}lish or ${VAR}lhardy.");

   EXPECT_EQ(std::string("Don't be foolish or foolhardy."), expanded);
}

TEST(ResourcesTest, NonzeroResourceMetrics)
{
   // Used memory should be nonzero
   long kb = 0;
   MemoryProvider provider = MemoryProviderUnknown;
   Error error = getTotalMemoryUsed(&kb, &provider);
   EXPECT_FALSE(error);
   EXPECT_GT(kb, 0);
   ASSERT_NE(provider, MemoryProviderUnknown);

   // Process used memory should be nonzero
   kb = 0;
   provider = MemoryProviderUnknown;
   error = getProcessMemoryUsed(&kb, &provider);
   EXPECT_FALSE(error);
   EXPECT_GT(kb, 0);
   ASSERT_NE(provider, MemoryProviderUnknown);

   // Total memory should be nonzero
   kb = 0;
   provider = MemoryProviderUnknown;
   error = getTotalMemory(&kb, &provider);
   EXPECT_FALSE(error);
   EXPECT_GT(kb, 0);
   ASSERT_NE(provider, MemoryProviderUnknown);
}

TEST(ResourcesTest, CongruentMemoryMetrics)
{
   long used, process, total;
   MemoryProvider provider;
   getTotalMemoryUsed(&used, &provider);
   getTotalMemory(&total, &provider);
   getProcessMemoryUsed(&process, &provider);

   // It'd be weird if there was more used memory than we had memory in the
   // first place
   EXPECT_GT(total, used);

   // It'd also be weird if the process used more memory than the total
   // amount of used memory -- well, except in the presence of cgroups. See
   // https://github.com/rstudio/rstudio/issues/9353
   if (provider != MemoryProviderLinuxCgroups)
   {
      EXPECT_GT(used, process);
   }
}

} // namespace system
} // namespace core
} // namespace rstudio
