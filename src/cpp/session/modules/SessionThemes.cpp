/*
 * SessionThemes.cpp
 *
 * Copyright (C) 2018 by RStudio, Inc.
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

#include "SessionThemes.hpp"

#include <boost/bind.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FilePath.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/system/System.hpp>

#include <session/SessionModuleContext.hpp>

#include <r/RRoutines.hpp>
#include <r/RSexp.hpp>

#include <fstream>
#include <map>
#include <regex>
#include <string>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace themes {

namespace {

// A map from the name of the theme to the location of the file and a boolean representing
// whether or not the theme is dark.
typedef std::map<std::string, std::pair<std::string, bool> > ThemeMap;

/**
 * @brief Gets themes in the specified location.
 *
 * @param location         The location in which to look for themes.
 * @param themeMap         The map which will contain all found themes after the call.
 * @param urlPrefix        The URL prefix for the theme. Must end with "/"
 */
void getThemesInLocation(
      const rstudio::core::FilePath& location,
      ThemeMap& themeMap,
      const std::string& urlPrefix = "")
{
   using rstudio::core::FilePath;
   if (location.isDirectory())
   {
      std::vector<FilePath> locationChildren;
      location.children(&locationChildren);
      for (const FilePath& themeFile: locationChildren)
      {
         if (themeFile.hasExtensionLowerCase(".rstheme"))
         {
            const std::string k_themeFileStr = themeFile.canonicalPath();
            std::ifstream themeIFStream(k_themeFileStr);
            std::string themeContents(
               (std::istreambuf_iterator<char>(themeIFStream)),
               (std::istreambuf_iterator<char>()));
            themeIFStream.close();

            std::smatch matches;
            std::regex_search(
               themeContents,
               matches,
               std::regex("rs-theme-name\\s*:\\s*([^\\*]+?)\\s*(?:\\*|$)"));

            // If there's no name specified,use the name of the file
            std::string name;
            if (matches.size() < 2)
            {
               name = themeFile.stem();
            }
            else
            {
               // If there's at least one name specified, get the first one.
               name = matches[1];
            }

            // Find out if the theme is dark or not.
            std::regex_search(
                     themeContents,
                     matches,
                     std::regex("rs-theme-is-dark\\s*:\\s*([^\\*]+?)\\s*(?:\\*|$)"));

            bool isDark = false;
            if (matches.size() >= 2)
            {
               isDark = std::regex_match(
                  std::string(matches[1]),
                  std::regex("(?:1|t(?:rue)?))", std::regex_constants::icase));
            }
            if ((matches.size() < 2) ||
                (!isDark &&
                !std::regex_match(
                   std::string(matches[1]),
                   std::regex("(?:0|f(?:alse)?))", std::regex_constants::icase))))
            {
               // TODO: warning / logging about using default isDark value.
            }

            themeMap[name] = std::tuple<std::string, bool>(urlPrefix + themeFile.filename(), isDark);
         }
      }
   }
}

void getAllThemes(ThemeMap& themeMap)
{
   using rstudio::core::FilePath;

   FilePath preInstalledPath = session::options().rResourcesPath().childPath("themes");
#ifdef _WIN32
   FilePath globalPath = core::system::systemSettingsPath("RStudio\\themes", false);
   FilePath localPath = core::system::userHomePath().childPath(".R\\rstudio\\themes");
#else
   FilePath globalPath("/etc/rstudio/themes/");
   FilePath localPath = core::system::userHomePath().childPath(".R/rstudio/themes/");
#endif

   const char* k_globalPathAlt = std::getenv("RS_THEME_GLOBAL_HOME");
   const char* k_localPathAlt = std::getenv("RS_THEME_LOCAL_HOME");
   if (k_globalPathAlt)
   {
      globalPath = FilePath(k_globalPathAlt);
   }
   if (k_localPathAlt)
   {
      localPath = FilePath(k_localPathAlt);
   }

   // Intentionally get global themes before getting user specific themes so that user specific
   // themes will override global ones.
   getThemesInLocation(preInstalledPath, themeMap, "theme/default/");
   getThemesInLocation(globalPath, themeMap, "theme/custom/global/");
   getThemesInLocation(localPath, themeMap, "theme/custom/local/");
}

/**
 * @brief Gets the list of all RStudio editor themes.
 *
 * @return The list of all RStudio editor themes.
 */
SEXP rs_getThemes()
{
   ThemeMap themeMap;
   getAllThemes(themeMap);

   // Convert to an R list.
   rstudio::r::sexp::Protect protect;
   rstudio::r::sexp::ListBuilder themeListBuilder(&protect);

   for (auto theme: themeMap)
   {
      rstudio::r::sexp::ListBuilder themeDetailsListBuilder(&protect);
      themeDetailsListBuilder.add("url", theme.second.first);
      themeDetailsListBuilder.add("isDark", theme.second.second);

      themeListBuilder.add(theme.first, themeDetailsListBuilder);
   }

   return rstudio::r::sexp::create(themeListBuilder, &protect);
}

} // anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   RS_REGISTER_CALL_METHOD(rs_getThemes, 0);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionThemes.R"));

   return initBlock.execute();
}

} // namespace themes
} // namespace modules
} // namespace session
} // namespace rstudio
