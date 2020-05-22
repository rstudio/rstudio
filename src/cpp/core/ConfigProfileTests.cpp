/*
 * ConfigProfileTests.cpp
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

#include <core/ConfigProfile.hpp>

namespace rstudio {
namespace core {
namespace tests {

TEST_CASE("config profile")
{
   SECTION("can parse single level")
   {
      std::string profileStr = R"([*]
          param=val
          param2=val2)";

      ConfigProfile profile;
      profile.addSections({{0, "*"}});
      profile.addParams("param", std::string(),
                        "param2", std::string());

      Error error = profile.parseString(profileStr);
      REQUIRE_FALSE(error);

      std::string param;
      error = profile.getParam("param", &param, {{0, std::string()}});
      REQUIRE_FALSE(error);
      REQUIRE(param == "val");

      std::string param2;
      error = profile.getParam("param2", &param2,
                               {{0, std::string()},
                               {1, "higherLevel"}});
      REQUIRE_FALSE(error);
      REQUIRE(param2 == "val2");

      std::string param3;
      error = profile.getParam("param3", &param3,
                              {{0, std::string()}});
      REQUIRE(error);
   }

   SECTION("can parse two levels")
   {
      std::string profileStr = R"([*]
          param=val
          param2=val2

          [@user]
          param=user-param
          param3=user-param3

          [@admin]"
          adminParam=admin-param)";

      ConfigProfile profile;
      profile.addSections({{0, "*"},
                          {1, "@"}});

      profile.addParams("param", std::string(),
                        "param2", std::string(),
                        "param3", std::string(),
                        "adminParam", std::string());

      Error error = profile.parseString(profileStr);
      REQUIRE_FALSE(error);

      std::string param;
      error = profile.getParam("param", &param, {{0, std::string()}});
      REQUIRE_FALSE(error);
      REQUIRE(param == "val");

      std::string param2;
      error = profile.getParam("param2", &param2, {{0, std::string()}});
      REQUIRE_FALSE(error);
      REQUIRE(param2 == "val2");

      std::string userParam;
      error = profile.getParam("param", &userParam,
                               {{0, std::string()},
                               {1, "user"}});
      REQUIRE_FALSE(error);
      REQUIRE(userParam == "user-param");

      std::string param3;
      error = profile.getParam("param3", &param3,
                               {{0, std::string()},
                               {1, "non-user"}});
      REQUIRE_FALSE(error);
      REQUIRE(param3 == std::string());

      std::string userParam3;
      error = profile.getParam("param3", &userParam3,
                               {{0, std::string()},
                               {1, "user"}});
      REQUIRE_FALSE(error);
      REQUIRE(userParam3 == "user-param3");

      std::string adminParam;
      error = profile.getParam("adminParam", &adminParam,
                               {{0, std::string()},
                               {1, "admin"}});
      REQUIRE_FALSE(error);
      REQUIRE(adminParam == "admin-param");
   }

   SECTION("can parse three levels")
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
      REQUIRE_FALSE(error);

      std::string param;
      error = profile.getParam("param", &param, {{0, std::string()}});
      REQUIRE_FALSE(error);
      REQUIRE(param == "val");

      std::string param2;
      error = profile.getParam("param2", &param2,
                               {{0, std::string()},
                               {1, "admin"}});
      REQUIRE_FALSE(error);
      REQUIRE(param2 == "val2");

      std::string userParam;
      error = profile.getParam("param", &userParam,
                               {{0, std::string()},
                               {1, "user"}});
      REQUIRE_FALSE(error);
      REQUIRE(userParam == "user-param");

      std::string param3;
      error = profile.getParam("param3", &param3,
                               {{0, std::string()},
                               {1, "non-user"}});
      REQUIRE_FALSE(error);
      REQUIRE(param3 == std::string());

      std::string userParam3;
      error = profile.getParam("param3", &userParam3,
                               {{0, std::string()},
                               {1, "user"},
                               {2, "josh"}});
      REQUIRE_FALSE(error);
      REQUIRE(userParam3 == "user-param3");

      std::string adminParam;
      error = profile.getParam("adminParam", &adminParam,
                               {{0, std::string()},
                               {1, "admin"},
                               {1, "user"}});
      REQUIRE_FALSE(error);
      REQUIRE(adminParam == "admin-param");

      std::string bdylanAdminParam;
      error = profile.getParam("adminParam", &bdylanAdminParam,
                               {{0, std::string()},
                               {1, "admin"},
                               {2, "bdylan"}});
      REQUIRE_FALSE(error);
      REQUIRE(bdylanAdminParam == "bad-admin");

      std::string bdylanAdminParam2;
      error = profile.getParam("adminParam", &bdylanAdminParam2,
                               {{2, "bdylan"}});
      REQUIRE_FALSE(error);
      REQUIRE(bdylanAdminParam2 == "bad-admin");

      std::string regularAdminParam;
      error = profile.getParam("adminParam", &regularAdminParam,
                               {{0, std::string()},
                               {1, "admin"},
                               {2, "joseph"}});
      REQUIRE_FALSE(error);
      REQUIRE(regularAdminParam == "admin-param");

      std::string bdylanParam3;
      error = profile.getParam("param3", &bdylanParam3,
                               {{0, std::string()},
                               {1, "admin"},
                               {2, "bdylan"}});
      REQUIRE_FALSE(error);
      REQUIRE(bdylanParam3 == "admin-param3");

      std::string neverUsedParam;
      error = profile.getParam("neverUsedParam", &neverUsedParam,
                               {{0, std::string()},
                               {1, "admin"},
                               {2, "bdylan"}});
      REQUIRE_FALSE(error);
      REQUIRE(neverUsedParam == "never used");

      bool boolNeverUsedParam = false;
      error = profile.getParam("booleanNotUsed", &boolNeverUsedParam,
                               {{0, std::string()},
                               {1, "admin"},
                               {2, "bdylan"}});
      REQUIRE_FALSE(error);
      REQUIRE(boolNeverUsedParam);
   }

   SECTION("Can get level names")
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
      REQUIRE_FALSE(error);

      std::vector<std::string> level1Names = profile.getLevelNames(1);
      REQUIRE(level1Names.size() == 2);
      REQUIRE(std::find(level1Names.begin(), level1Names.end(), "user") != level1Names.end());
      REQUIRE(std::find(level1Names.begin(), level1Names.end(), "admin") != level1Names.end());

      std::vector<std::string> level2Names = profile.getLevelNames(2);
      REQUIRE(level2Names.size() == 1);
      REQUIRE(std::find(level2Names.begin(), level2Names.end(), "bdylan") != level2Names.end());
      REQUIRE(std::find(level2Names.begin(), level2Names.end(), "bdaylan") == level2Names.end());
   }
}

} // end namespace tests
} // end namespace core
} // end namespace rstudio
