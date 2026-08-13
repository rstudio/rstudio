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

#include <stdlib.h>

#include <boost/thread.hpp>

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

TEST(EnvironmentTest, EnvironmentScopeRestoresPreviousValue)
{
   setenv("RSTUDIO_ENV_SCOPE_TEST", "original");
   {
      EnvironmentScope scope("RSTUDIO_ENV_SCOPE_TEST", "temporary");
      EXPECT_EQ("temporary", getenv("RSTUDIO_ENV_SCOPE_TEST"));

      // overwrite another variable so the environ array is reshuffled while
      // the scope holds its captured copy of the previous value
      setenv("RSTUDIO_ENV_SCOPE_TEST_OTHER", "other");
   }
   EXPECT_EQ("original", getenv("RSTUDIO_ENV_SCOPE_TEST"));

   unsetenv("RSTUDIO_ENV_SCOPE_TEST");
   unsetenv("RSTUDIO_ENV_SCOPE_TEST_OTHER");
}

TEST(EnvironmentTest, EnvironmentScopeUnsetsAbsentValue)
{
   unsetenv("RSTUDIO_ENV_SCOPE_TEST");
   {
      EnvironmentScope scope("RSTUDIO_ENV_SCOPE_TEST", "temporary");
      EXPECT_EQ("temporary", getenv("RSTUDIO_ENV_SCOPE_TEST"));
   }
   EXPECT_EQ("", getenv("RSTUDIO_ENV_SCOPE_TEST"));
}

TEST(EnvironmentTest, GetenvOverloadDistinguishesUnset)
{
   // the two-argument getenv must observe values written by setenv; on
   // Windows this means reading the process environment block, not the
   // CRT's startup snapshot (which raw ::getenv reads)
   unsetenv("RSTUDIO_ENV_SCOPE_TEST");

   std::string value = "sentinel";
   EXPECT_FALSE(getenv("RSTUDIO_ENV_SCOPE_TEST", &value));
   EXPECT_EQ("sentinel", value);

   setenv("RSTUDIO_ENV_SCOPE_TEST", "value");
   EXPECT_TRUE(getenv("RSTUDIO_ENV_SCOPE_TEST", &value));
   EXPECT_EQ("value", value);

#ifndef _WIN32
   // set-but-empty is distinguishable from unset on POSIX only: on Windows,
   // setting a variable to the empty string deletes it
   setenv("RSTUDIO_ENV_SCOPE_TEST", "");
   value = "sentinel";
   EXPECT_TRUE(getenv("RSTUDIO_ENV_SCOPE_TEST", &value));
   EXPECT_EQ("", value);
#endif

   unsetenv("RSTUDIO_ENV_SCOPE_TEST");
}

TEST(EnvironmentTest, AccessorsAgreeOnNonAsciiNames)
{
   // "\xC3\x84" is UTF-8 for 'A' with umlaut; on Windows the name must be
   // UTF-8 decoded on its way to the wide-character APIs, so an accessor
   // that widens the name byte-by-byte instead would address a different
   // variable than the one setenv wrote
   std::string name = "RSTUDIO_ENV_SCOPE_TEST_\xC3\x84";

   setenv(name, "value");
   EXPECT_EQ("value", getenv(name));

   std::string value;
   EXPECT_TRUE(getenv(name, &value));
   EXPECT_EQ("value", value);

   unsetenv(name);
   EXPECT_FALSE(getenv(name, &value));
}

#ifdef _WIN32

TEST(EnvironmentTest, SetenvIsVisibleToCRuntime)
{
   // core::system::setenv writes the Win32 environment block, but raw
   // ::getenv reads the C runtime's own copy of the environment, which
   // SetEnvironmentVariable does not update; setenv must write through
   // both so that CRT readers (in-process libraries, and R itself when
   // it shares our C runtime) observe the update
   setenv("RSTUDIO_ENV_CRT_TEST", "value");
   const char* value = ::getenv("RSTUDIO_ENV_CRT_TEST");
   ASSERT_NE(nullptr, value);
   EXPECT_EQ(std::string("value"), value);

   // unsetenv must scrub the CRT copy as well: read-then-scrub secret
   // handling relies on the value being gone from every store
   unsetenv("RSTUDIO_ENV_CRT_TEST");
   EXPECT_EQ(nullptr, ::getenv("RSTUDIO_ENV_CRT_TEST"));
}

#endif

TEST(EnvironmentTest, ConcurrentAccessorsAreSerialized)
{
   // getenv walking environ while setenv reallocates it is the crash class
   // behind #10756; with the environment lock this is well-defined. a
   // regression (e.g. recursive locking) shows up here as a deadlock, which
   // the test harness watchdog reports with a stack trace.
   boost::thread reader([]()
   {
      for (int i = 0; i < 5000; i++)
      {
         getenv("RSTUDIO_ENV_HAMMER_" + std::to_string(i % 50));
         Options env;
         if (i % 500 == 0)
            environment(&env);
      }
   });

   for (int i = 0; i < 5000; i++)
      setenv("RSTUDIO_ENV_HAMMER_" + std::to_string(i % 50), std::to_string(i));

   reader.join();

   for (int i = 0; i < 50; i++)
      unsetenv("RSTUDIO_ENV_HAMMER_" + std::to_string(i));
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
