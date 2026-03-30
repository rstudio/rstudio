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

TEST(ConfigProfileTest, CanParseCompoundValue)
{
   std::string profileStr = R"([*]
          param/foo=val
          param/bar=val2)";

   ConfigProfile profile;
   profile.addSections({{0, "*"}});
   profile.addParams("param", ConfigProfile::ValuesMap{});

   Error error = profile.parseString(profileStr);
   ASSERT_FALSE(error);

   ConfigProfile::ValuesMap param;
   ConfigProfile::ValuesMap expected{
      { "foo", "val" },
      { "bar", "val2" },
   };
   error = profile.getCompoundParam("param", &param, {{0, std::string()}});
   ASSERT_FALSE(error);
   ASSERT_EQ(expected, param);
}

TEST(ConfigProfileTest, CanDetectCompoundValueError)
{
   std::string profileStr = R"([*]
          param/foo=val
          param/=val2)";

   ConfigProfile profile;
   profile.addSections({{0, "*"}});
   profile.addParams("param", ConfigProfile::ValuesMap{});

   Error error = profile.parseString(profileStr);
   ASSERT_TRUE(error);
}

TEST(ConfigProfileTest, CanParseTwoLevelCompoundValue)
{
   std::string profileStr = R"([*]
          param/foo=val
          param/bar=val2

          [@user]
          param/foo=user-param

          [@admin]
          adminParam=admin-param)";

   ConfigProfile profile;
   profile.addSections({{0, "*"},
                  {1, "@"}});

   profile.addParams("param", ConfigProfile::ValuesMap{},
                  "adminParam", std::string());

   Error error = profile.parseString(profileStr);
   ASSERT_FALSE(error);

   ConfigProfile::ValuesMap param;
   ConfigProfile::ValuesMap expected{
      { "foo", "val" },
      { "bar", "val2" },
   };
   error = profile.getCompoundParam("param", &param, {{0, std::string()}});
   ASSERT_FALSE(error);
   ASSERT_EQ(expected, param);

   ConfigProfile::ValuesMap userParam;
   ConfigProfile::ValuesMap userExpected{
      { "foo", "user-param" },
   };
   error = profile.getCompoundParam("param", &userParam,
                  {{0, std::string()},
                  {1, "user"}});
   ASSERT_FALSE(error);
   ASSERT_EQ(userExpected, userParam);

   ConfigProfile::ValuesMap adminParam;
   error = profile.getCompoundParam("param", &adminParam,
                  {{0, std::string()},
                  {1, "admin"}});
   ASSERT_FALSE(error);
   ASSERT_EQ(expected, adminParam);

   ConfigProfile::ValuesMap nonUserParam;
   error = profile.getCompoundParam("param", &nonUserParam,
                  {{0, std::string()},
                  {1, "non-user"}});
   ASSERT_FALSE(error);
   ASSERT_EQ(expected, nonUserParam);
}


TEST(ConfigProfileTest, GetAllLevelValues)
{
   // Profile with all three levels; max-cpus set at global and group but not at user level.
   // @everyone and bob have a different param set (max-mem-mb) so the section is present in
   // levels_ but max-cpus is absent → getAllLevelValues should return nullopt for those levels.
   // Note: Boost's read_ini silently drops completely empty sections, so we must have at least
   // one param in each section to ensure it is retained.
   std::string profileStr = R"([*]
          max-cpus=4.0

          [@scientists]
          max-cpus=8.0

          [@everyone]
          max-mem-mb=100.0

          [alice]
          max-cpus=2.0

          [bob]
          max-mem-mb=200.0)";

   ConfigProfile profile;
   profile.addSections({{0, "*"}, {1, "@"}, {2, std::string()}});
   profile.addParams("max-cpus", 0.0, "max-mem-mb", 0.0);

   Error error = profile.parseString(profileStr);
   ASSERT_FALSE(error);

   // alice in scientists: global=4, scientists=8, everyone=nullopt, user=2
   // Order: global, groups (scientists, everyone), user
   {
      std::vector<std::optional<double>> values;
      error = profile.getAllLevelValues("max-cpus", &values,
                                        {{0, std::string()}, {1, "scientists"}, {1, "everyone"}, {2, "alice"}});
      ASSERT_FALSE(error);
      ASSERT_EQ(4u, values.size());
      ASSERT_TRUE(values[0].has_value());  ASSERT_EQ(4.0, *values[0]);   // global
      ASSERT_TRUE(values[1].has_value());  ASSERT_EQ(8.0, *values[1]);   // @scientists
      ASSERT_FALSE(values[2].has_value());                                // @everyone: section present, param not set
      ASSERT_TRUE(values[3].has_value());  ASSERT_EQ(2.0, *values[3]);   // alice
   }

   // bob in scientists: global=4, scientists=8, bob section present but max-cpus not set
   {
      std::vector<std::optional<double>> values;
      error = profile.getAllLevelValues("max-cpus", &values,
                                        {{0, std::string()}, {1, "scientists"}, {2, "bob"}});
      ASSERT_FALSE(error);
      ASSERT_EQ(3u, values.size());
      ASSERT_TRUE(values[0].has_value());  ASSERT_EQ(4.0, *values[0]);
      ASSERT_TRUE(values[1].has_value());  ASSERT_EQ(8.0, *values[1]);
      ASSERT_FALSE(values[2].has_value());  // bob section present, param not set
   }

   // absent section (no config block) is omitted entirely
   {
      std::vector<std::optional<double>> values;
      error = profile.getAllLevelValues("max-cpus", &values,
                                        {{0, std::string()}, {2, "charlie"}});  // charlie has no section
      ASSERT_FALSE(error);
      ASSERT_EQ(1u, values.size());  // only global; charlie's absent section is omitted
      ASSERT_TRUE(values[0].has_value());  ASSERT_EQ(4.0, *values[0]);
   }

   // unregistered param returns error
   {
      std::vector<std::optional<double>> values;
      error = profile.getAllLevelValues("nonexistent", &values, {{0, std::string()}});
      ASSERT_TRUE(error);
   }

   // bad value (non-numeric string where double expected) returns error
   {
      ConfigProfile badProfile;
      badProfile.addSections({{0, "*"}, {2, std::string()}});
      badProfile.addParams("max-cpus", 0.0);

      Error parseError = badProfile.parseString("[*]\nmax-cpus=not-a-number\n");
      ASSERT_FALSE(parseError);

      std::vector<std::optional<double>> values;
      error = badProfile.getAllLevelValues("max-cpus", &values, {{0, std::string()}});
      ASSERT_TRUE(error);
      // getMessage() returns the system error string ("Invalid argument"), not our description.
      // The custom message with context is stored as the "description" property.
      EXPECT_NE(std::string::npos, error.getProperty("description").find("not-a-number"));
      EXPECT_NE(std::string::npos, error.getProperty("description").find("max-cpus"));
   }
}

TEST(ConfigProfileTest, GetAllLevelValuesOutOfOrder)
{
   // Sections listed in reverse level order (user first, then group, then global).
   // getAllLevelValues must still return values in ascending level order
   // regardless of the order sections appear in the INI string.
   std::string profileStr = R"([alice]
          max-cpus=2.0

          [@scientists]
          max-cpus=8.0

          [*]
          max-cpus=4.0)";

   ConfigProfile profile;
   profile.addSections({{0, "*"}, {1, "@"}, {2, std::string()}});
   profile.addParams("max-cpus", 0.0);

   Error error = profile.parseString(profileStr);
   ASSERT_FALSE(error);

   std::vector<std::optional<double>> values;
   error = profile.getAllLevelValues("max-cpus", &values,
                                    {{0, std::string()}, {1, "scientists"}, {2, "alice"}});
   ASSERT_FALSE(error);
   ASSERT_EQ(3u, values.size());
   // Output must be in ascending level order: global (0), group (1), user (2)
   ASSERT_TRUE(values[0].has_value());  ASSERT_EQ(4.0, *values[0]);  // global — level 0
   ASSERT_TRUE(values[1].has_value());  ASSERT_EQ(8.0, *values[1]);  // @scientists — level 1
   ASSERT_TRUE(values[2].has_value());  ASSERT_EQ(2.0, *values[2]);  // alice — level 2
}

TEST(ConfigProfileTest, GetAllCompoundLevelValues)
{
   // @everyone and bob have a simple param (max-cpus) rather than the compound param so the
   // section is retained by the INI parser but constraints is absent → nullopt.
   std::string profileStr = R"([*]
          constraints/cpu=x86

          [@scientists]
          constraints/gpu=nvidia

          [@everyone]
          max-cpus=5.0

          [alice]
          constraints/node=gpu-node

          [bob]
          max-cpus=10.0)";

   ConfigProfile profile;
   profile.addSections({{0, "*"}, {1, "@"}, {2, std::string()}});
   profile.addParams("constraints", ConfigProfile::ValuesMap{}, "max-cpus", 0.0);

   Error error = profile.parseString(profileStr);
   ASSERT_FALSE(error);

   // alice in scientists: global, scientists, everyone (no compound), alice
   {
      std::vector<std::optional<ConfigProfile::ValuesMap>> values;
      error = profile.getAllCompoundLevelValues("constraints", &values,
                                               {{0, std::string()}, {1, "scientists"}, {1, "everyone"}, {2, "alice"}});
      ASSERT_FALSE(error);
      ASSERT_EQ(4u, values.size());
      ASSERT_TRUE(values[0].has_value());
      ASSERT_EQ("x86", (*values[0]).at("cpu"));   // global
      ASSERT_TRUE(values[1].has_value());
      ASSERT_EQ("nvidia", (*values[1]).at("gpu")); // @scientists
      ASSERT_FALSE(values[2].has_value());          // @everyone: section present, compound param not set
      ASSERT_TRUE(values[3].has_value());
      ASSERT_EQ("gpu-node", (*values[3]).at("node")); // alice
   }

   // bob: global, bob section present but compound param not set
   {
      std::vector<std::optional<ConfigProfile::ValuesMap>> values;
      error = profile.getAllCompoundLevelValues("constraints", &values,
                                               {{0, std::string()}, {2, "bob"}});
      ASSERT_FALSE(error);
      ASSERT_EQ(2u, values.size());
      ASSERT_TRUE(values[0].has_value());
      ASSERT_FALSE(values[1].has_value());  // bob section present, param not set
   }

   // charlie has no section — absent section omitted
   {
      std::vector<std::optional<ConfigProfile::ValuesMap>> values;
      error = profile.getAllCompoundLevelValues("constraints", &values,
                                               {{0, std::string()}, {2, "charlie"}});
      ASSERT_FALSE(error);
      ASSERT_EQ(1u, values.size());
      ASSERT_TRUE(values[0].has_value());
   }

   // unregistered param returns error
   {
      std::vector<std::optional<ConfigProfile::ValuesMap>> values;
      error = profile.getAllCompoundLevelValues("nonexistent", &values, {{0, std::string()}});
      ASSERT_TRUE(error);
   }
}

TEST(ConfigProfileTest, GetAllCompoundLevelValuesOutOfOrder)
{
   // Sections listed in reverse level order (user first, then group, then global).
   // getAllCompoundLevelValues must still return values in ascending level order
   // regardless of the order sections appear in the INI string.
   std::string profileStr = R"([alice]
          constraints/node=gpu-node

          [@scientists]
          constraints/gpu=nvidia

          [*]
          constraints/cpu=x86)";

   ConfigProfile profile;
   profile.addSections({{0, "*"}, {1, "@"}, {2, std::string()}});
   profile.addParams("constraints", ConfigProfile::ValuesMap{});

   Error error = profile.parseString(profileStr);
   ASSERT_FALSE(error);

   std::vector<std::optional<ConfigProfile::ValuesMap>> values;
   error = profile.getAllCompoundLevelValues("constraints", &values,
                                            {{0, std::string()}, {1, "scientists"}, {2, "alice"}});
   ASSERT_FALSE(error);
   ASSERT_EQ(3u, values.size());
   // Output must be in ascending level order: global (0), group (1), user (2)
   ASSERT_TRUE(values[0].has_value());
   ASSERT_EQ("x86",      (*values[0]).at("cpu"));   // global — level 0
   ASSERT_TRUE(values[1].has_value());
   ASSERT_EQ("nvidia",   (*values[1]).at("gpu"));   // @scientists — level 1
   ASSERT_TRUE(values[2].has_value());
   ASSERT_EQ("gpu-node", (*values[2]).at("node"));  // alice — level 2
}

} // namespace tests
} // namespace core
} // namespace rstudio
