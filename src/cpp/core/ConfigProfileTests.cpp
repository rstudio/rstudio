/*
 * ConfigProfileTests.cpp
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

#include <core/ConfigProfile.hpp>
#include <algorithm>

namespace rstudio {
namespace core {
namespace tests {

TEST(ConfigProfileTest, CanParseSingleLevel)
{
   std::string profileStr = R"([*]
          param=val
          param2=val2)";

   ConfigProfile profile;
   profile.addSections({{0, "*"}});
   profile.addParams("param", std::string(),
                  "param2", std::string());

   Error error = profile.parseString(profileStr);
   ASSERT_FALSE(error);

   std::string param;
   error = profile.getParam("param", &param, {{0, std::string()}});
   ASSERT_FALSE(error);
   ASSERT_EQ("val", param);

   std::string param2;
   error = profile.getParam("param2", &param2,
                  {{0, std::string()},
                  {1, "higherLevel"}});
   ASSERT_FALSE(error);
   ASSERT_EQ("val2", param2);

   std::string param3;
   error = profile.getParam("param3", &param3,
                  {{0, std::string()}});
   ASSERT_TRUE(error);
}

TEST(ConfigProfileTest, CanParseTwoLevels)
{
   std::string profileStr = R"([*]
          param=val
          param2=val2

          [@user]
          param=user-param
          param3=user-param3

          [@admin]
          adminParam=admin-param)";

   ConfigProfile profile;
   profile.addSections({{0, "*"},
                  {1, "@"}});

   profile.addParams("param", std::string(),
                  "param2", std::string(),
                  "param3", std::string(),
                  "adminParam", std::string());

   Error error = profile.parseString(profileStr);
   ASSERT_FALSE(error);

   std::string param;
   error = profile.getParam("param", &param, {{0, std::string()}});
   ASSERT_FALSE(error);
   ASSERT_EQ("val", param);

   std::string param2;
   error = profile.getParam("param2", &param2, {{0, std::string()}});
   ASSERT_FALSE(error);
   ASSERT_EQ("val2", param2);

   std::string userParam;
   error = profile.getParam("param", &userParam,
                  {{0, std::string()},
                  {1, "user"}});
   ASSERT_FALSE(error);
   ASSERT_EQ("user-param", userParam);

   std::string param3;
   error = profile.getParam("param3", &param3,
                  {{0, std::string()},
                  {1, "non-user"}});
   ASSERT_FALSE(error);
   ASSERT_EQ(std::string(), param3);

   std::string userParam3;
   error = profile.getParam("param3", &userParam3,
                  {{0, std::string()},
                  {1, "user"}});
   ASSERT_FALSE(error);
   ASSERT_EQ("user-param3", userParam3);

   std::string adminParam;
   error = profile.getParam("adminParam", &adminParam,
                  {{0, std::string()},
                  {1, "admin"}});
   ASSERT_FALSE(error);
   ASSERT_EQ("admin-param", adminParam);
}

TEST(ConfigProfileTest, CanParseThreeLevels)
{
   std::string profileStr = R"([*]
          param=val
          param2=val2

          [@user]
          param=user-param
          param3=user-param3

          [@admin]
          adminParam=admin-param
          param3=admin-param3

          [bdylan]
          adminParam=bad-admin)";

   ConfigProfile profile;
   profile.addSections({{0, "*"},
                  {1, "@"},
                  {2, std::string()}});

   profile.addParams("param", std::string(),
                  "param2", std::string(),
                  "param3", std::string(),
                  "adminParam", std::string(),
                  "neverUsedParam", std::string("never used"),
                  "booleanNotUsed", true);

   Error error = profile.parseString(profileStr);
   ASSERT_FALSE(error);

   std::string param;
   error = profile.getParam("param", &param, {{0, std::string()}});
   ASSERT_FALSE(error);
   ASSERT_EQ("val", param);

   std::string param2;
   error = profile.getParam("param2", &param2,
                  {{0, std::string()},
                  {1, "admin"}});
   ASSERT_FALSE(error);
   ASSERT_EQ("val2", param2);

   std::string userParam;
   error = profile.getParam("param", &userParam,
                  {{0, std::string()},
                  {1, "user"}});
   ASSERT_FALSE(error);
   ASSERT_EQ("user-param", userParam);

   std::string param3;
   error = profile.getParam("param3", &param3,
                  {{0, std::string()},
                  {1, "non-user"}});
   ASSERT_FALSE(error);
   ASSERT_EQ(std::string(), param3);

   std::string userParam3;
   error = profile.getParam("param3", &userParam3,
                  {{0, std::string()},
                  {1, "user"},
                  {2, "josh"}});
   ASSERT_FALSE(error);
   ASSERT_EQ("user-param3", userParam3);

   std::string adminParam;
   error = profile.getParam("adminParam", &adminParam,
                  {{0, std::string()},
                  {1, "admin"},
                  {1, "user"}});
   ASSERT_FALSE(error);
   ASSERT_EQ("admin-param", adminParam);

   std::string bdylanAdminParam;
   error = profile.getParam("adminParam", &bdylanAdminParam,
                  {{0, std::string()},
                  {1, "admin"},
                  {2, "bdylan"}});
   ASSERT_FALSE(error);
   ASSERT_EQ("bad-admin", bdylanAdminParam);

   std::string bdylanAdminParam2;
   error = profile.getParam("adminParam", &bdylanAdminParam2,
                  {{2, "bdylan"}});
   ASSERT_FALSE(error);
   ASSERT_EQ("bad-admin", bdylanAdminParam2);

   std::string regularAdminParam;
   error = profile.getParam("adminParam", &regularAdminParam,
                  {{0, std::string()},
                  {1, "admin"},
                  {2, "joseph"}});
   ASSERT_FALSE(error);
   ASSERT_EQ("admin-param", regularAdminParam);

   std::string bdylanParam3;
   error = profile.getParam("param3", &bdylanParam3,
                  {{0, std::string()},
                  {1, "admin"},
                  {2, "bdylan"}});
   ASSERT_FALSE(error);
   ASSERT_EQ("admin-param3", bdylanParam3);

   std::string neverUsedParam;
   error = profile.getParam("neverUsedParam", &neverUsedParam,
                  {{0, std::string()},
                  {1, "admin"},
                  {2, "bdylan"}});
   ASSERT_FALSE(error);
   ASSERT_EQ("never used", neverUsedParam);

   bool boolNeverUsedParam = false;
   error = profile.getParam("booleanNotUsed", &boolNeverUsedParam,
                  {{0, std::string()},
                  {1, "admin"},
                  {2, "bdylan"}});
   ASSERT_FALSE(error);
   ASSERT_TRUE(boolNeverUsedParam);
}

TEST(ConfigProfileTest, CanGetLevelNames)
{
   std::string profileStr = R"([*]
          param=val
          param2=val2

          [@user]
          param=user-param
          param3=user-param3

          [@admin]
          adminParam=admin-param
          param3=admin-param3

          [bdylan]
          adminParam=bad-admin)";

   ConfigProfile profile;
   profile.addSections({{0, "*"},
                  {1, "@"},
                  {2, std::string()}});

   profile.addParams("param", std::string(),
                  "param2", std::string(),
                  "param3", std::string(),
                  "adminParam", std::string(),
                  "neverUsedParam", std::string("never used"),
                  "booleanNotUsed", true);

   Error error = profile.parseString(profileStr);
   ASSERT_FALSE(error);

   std::vector<std::string> level1Names = profile.getLevelNames(1);
   ASSERT_EQ(2u, level1Names.size());
   ASSERT_NE(level1Names.end(), std::find(level1Names.begin(), level1Names.end(), "user"));
   ASSERT_NE(level1Names.end(), std::find(level1Names.begin(), level1Names.end(), "admin"));

   std::vector<std::string> level2Names = profile.getLevelNames(2);
   ASSERT_EQ(1u, level2Names.size());
   ASSERT_NE(level2Names.end(), std::find(level2Names.begin(), level2Names.end(), "bdylan"));
   ASSERT_EQ(level2Names.end(), std::find(level2Names.begin(), level2Names.end(), "bdaylan"));
}

TEST(ConfigProfileTest, CanCheckIfParameterDefined)
{
   std::string profileStr = R"([*]
       param=val
       param2=val2

       [@user]
       param=user-param
       param1=user-param1
       param3=user-param3

       [@admin]
       adminParam=admin-param
       param3=admin-param3

       [kcobain]
       adminParam=bad-admin)";

   ConfigProfile profile;

   // Does checking for a parameter before loading a profile cause any issues?
   ASSERT_FALSE(profile.isParamDefined("adminParam"));

   profile.addSections({{0, "*"},
                       {1, "@"},
                       {2, std::string()}});

   profile.addParams("param", std::string(),
                     "param1", std::string(),
                     "param2", std::string(),
                     "param3", std::string(),
                     "adminParam", std::string(),
                     "neverUsedParam", std::string("never used"),
                     "booleanNotUsed", true);

   Error error = profile.parseString(profileStr);
   ASSERT_FALSE(error);

   // Param that's not defined.
   ASSERT_FALSE(profile.isParamDefined("neverUsedParam"));

   // Param that doesn't exist.
   ASSERT_FALSE(profile.isParamDefined("nonExistentParam"));

   // Param that exists in multiple levels.
   ASSERT_TRUE(profile.isParamDefined("param3"));

   // Param that exists in one level.
   ASSERT_TRUE(profile.isParamDefined("param1"));

   // Empty param name.
   ASSERT_FALSE(profile.isParamDefined(""));

   // Check a profile with only a single level and single param (potential edge cases).
   ConfigProfile singleLevelProfile;
   singleLevelProfile.addSections({{0, "*"}});
   singleLevelProfile.addParams("param", std::string());
   error = singleLevelProfile.parseString("[*]\nparam=val");
   ASSERT_FALSE(error);

   ASSERT_TRUE(singleLevelProfile.isParamDefined("param"));
   ASSERT_FALSE(singleLevelProfile.isParamDefined("nonExistentParam"));
}

} // namespace tests
} // namespace core
} // namespace rstudio
