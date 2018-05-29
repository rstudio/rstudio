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
#include <boost/filesystem.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/json/JsonRpc.hpp>

#include <session/SessionModuleContext.hpp>

#include <r/RRoutines.hpp>
#include <r/RSexp.hpp>

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
void getThemesInLocation(const boost::filesystem::path& location, ThemeMap& themeMap)
{
   using namespace boost::filesystem;
   if (is_directory(location))
   {
      for (directory_entry& themeFile : boost::make_iterator_range(directory_iterator(location), {}))
      {
         std::string fileLocation = themeFile.path().generic_string();
         if (!std::regex_match(
                fileLocation,
                std::regex(".*\\.rstheme$", std::regex_constants::icase)))
         {
            std::ifstream themeIFStream(fileLocation);
            std::string themeContents(
               (std::istreambuf_iterator<char>(themeIFStream)),
               (std::istreambuf_iterator<char>()));
            themeIFStream.close();

            std::smatch matches;
            std::regex_search(
               themeContents,
               matches,
               std::regex("rs-theme-name\\s*:\\s*([^\\*]+?)\\s*(?:\\*|$)"));

            // If there's at least one name specified, get the first one.
            if (matches.size() < 2)
            {
               std::regex_search(
                  fileLocation,
                  matches,
                  std::regex("([^/\\\\]+?)\\.rstheme"),
                  std::regex_constants::match_default);

               if (matches.size() < 2)
               {
                  // TODO: warning / logging
                  // No name so skip.
                  continue;
               }
            }

            std::string name = matches[1];

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

            themeMap[name] = std::pair<std::string, bool>(fileLocation, isDark);
         }
      }
   }
}

/**
 * @brief Gets the list of all RStudio editor themes.
 *
 * @return The list of all RStudio editor themes.
 */
SEXP rs_getThemes()
{
   // List all files in the global location first.
   // TODO: Windows
   using namespace boost::filesystem;

#if defined(_WIN32) || defined(_WIN64)
#else
   path globalPath("/etc/rstudio/themes/");
   path localPath("~/.R/rstudio/themes/");
#endif

   // Intentionally get global themes before getting user specific themes so that user specific
   // themes will override global ones.
   ThemeMap themeMap;
   getThemesInLocation(globalPath, themeMap);
   getThemesInLocation(localPath, themeMap);
   // TODO: get default themes.

   // Convert to an R list.
   rstudio::r::sexp::Protect protect;
   rstudio::r::sexp::ListBuilder themeListBuilder(&protect);

   for (auto theme: themeMap)
   {
      rstudio::r::sexp::ListBuilder themeDetailsListBuilder(&protect);
      themeDetailsListBuilder.add("fileName", theme.second.first);
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
