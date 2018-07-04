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

#include <boost/algorithm/algorithm.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/bind.hpp>
#include <boost/regex.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FilePath.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/system/System.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <session/SessionModuleContext.hpp>

#include <r/RRoutines.hpp>
#include <r/RSexp.hpp>

#include <fstream>
#include <map>
#include <string>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace themes {

namespace {

const std::string kDefaultThemeLocation = "/theme/default/";
const std::string kGlobalCustomThemeLocation = "/theme/custom/global/";
const std::string kLocalCustomThemeLocation = "/theme/custom/local/";

// A map from the name of the theme to the location of the file and a boolean representing
// whether or not the theme is dark.
typedef std::map<std::string, std::tuple<std::string, std::string, bool> > ThemeMap;

/**
 * @brief Converts a string to a boolean value. Throws an bad_lexical_cast exception if the string
 *        is not valid.
 *
 * @param toConvert     The string to convert to boolean.
 *
 * @throw bad_lexical_cast    If the string cannot be converted to boolean.
 *
 * @return The converted value.
 */
bool convertToBool(const std::string& toConvert)
{
   std::string preppedStr = boost::regex_replace(
            toConvert,
            boost::regex("true", boost::regex::icase),
            "1");
   preppedStr = boost::regex_replace(
            preppedStr,
            boost::regex("false", boost::regex::icase),
            "0");
   return boost::lexical_cast<bool>(preppedStr);
}

/**
 * @brief Gets themes in the specified location.
 *
 * @param location         The location in which to look for themes.
 * @param urlPrefix        The URL prefix for the theme. Must end with "/"
 * @param themeMap         The map which will contain all found themes after the call. (NOT OWN)
 */
void getThemesInLocation(
      const rstudio::core::FilePath& location,
      const std::string& urlPrefix,
      ThemeMap* themeMap)
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

            boost::smatch matches;
            boost::regex_search(
               themeContents,
               matches,
               boost::regex("rs-theme-name\\s*:\\s*([^\\*]+?)\\s*(?:\\*|$)"));

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
            boost::regex_search(
                     themeContents,
                     matches,
                     boost::regex("rs-theme-is-dark\\s*:\\s*([^\\*]+?)\\s*(?:\\*|$)"));

            bool isDark = false;
            if (matches.size() >= 2)
            {
               try
               {
                  isDark = convertToBool(matches[1].str());
               }
               catch (boost::bad_lexical_cast)
               {
                  LOG_WARNING_MESSAGE("rs-theme-is-dark value is not a valid boolean string for theme \"" + name + "\".");
               }
            }
            else
            {
               LOG_WARNING_MESSAGE("rs-theme-is-dark is not set for theme \"" + name + "\".");
            }

            (*themeMap)[boost::algorithm::to_lower_copy(name)] = std::make_tuple(
               name,
               urlPrefix + themeFile.filename(),
               isDark);
         }
      }
   }
}

/**
 * @brief Gets the location of themes that are installed with RStudio.
 *
 * @return The location of themes that are installed with RStudio.
 */
FilePath getDefaultThemePath()
{
   return session::options().rResourcesPath().childPath("themes");
}

/**
 * @brief Gets the location of custom themes that are installed for all users.
 *
 * @return The location of custom themes that are installed for all users.
 */
FilePath getGlobalCustomThemePath()
{
   using rstudio::core::FilePath;

   const char* kGlobalPathAlt = std::getenv("RS_THEME_GLOBAL_HOME");
   if (kGlobalPathAlt)
   {
      return FilePath(kGlobalPathAlt);
   }

#ifdef _WIN32
   return core::system::systemSettingsPath("RStudio\\themes", false);
#else
   return FilePath("/etc/rstudio/themes/");
#endif
}

/**
 * @brief Gets the location of custom themes that are installed for the current user.
 *
 * @return The location of custom themes that are installed for the current user.
 */
FilePath getLocalCustomThemePath()
{
   using rstudio::core::FilePath;
   const char* kLocalPathAlt = std::getenv("RS_THEME_LOCAL_HOME");
   if (kLocalPathAlt)
   {
      return FilePath(kLocalPathAlt);
   }

#ifdef _WIN32
   return core::system::userHomePath().childPath(".R\\rstudio\\themes");
#else
   return core::system::userHomePath().childPath(".R/rstudio/themes/");
#endif
}

/**
 * @brief Gets a map of all available themes, keyed by the unique name of the theme. If a theme is
 *        found in multiple locations, the theme in the most specific folder will be given
 *        precedence.
 *
 * @return The map of all available themes.
 */
ThemeMap getAllThemes()
{
   // Intentionally get global themes before getting user specific themes so that user specific
   // themes will override global ones.
   ThemeMap themeMap;
   getThemesInLocation(getDefaultThemePath(), kDefaultThemeLocation, &themeMap);
   getThemesInLocation(getGlobalCustomThemePath(), kGlobalCustomThemeLocation, &themeMap);
   getThemesInLocation(getLocalCustomThemePath(), kLocalCustomThemeLocation, &themeMap);

   return themeMap;
}

/**
 * @brief Gets the list of all RStudio editor themes.
 *
 * @return The list of all RStudio editor themes.
 */
SEXP rs_getThemes()
{
   ThemeMap themeMap = getAllThemes();

   // Convert to an R list.
   rstudio::r::sexp::Protect protect;
   rstudio::r::sexp::ListBuilder themeListBuilder(&protect);

   for (auto theme: themeMap)
   {
      rstudio::r::sexp::ListBuilder themeDetailsListBuilder(&protect);
      themeDetailsListBuilder.add("name", std::get<0>(theme.second));
      themeDetailsListBuilder.add("url", std::get<1>(theme.second));
      themeDetailsListBuilder.add("isDark", std::get<2>(theme.second));

      themeListBuilder.add(theme.first, themeDetailsListBuilder);
   }

   return rstudio::r::sexp::create(themeListBuilder, &protect);
}

/**
 * @brief Gets the default theme based on the request from the client.
 *
 * @param request    The request from the client.
 *
 * @return The default theme. "Tomorrow Night" if the request is for a dark theme; "Textmate" if
 *         the request is for a light theme.
 */
FilePath getDefaultTheme(const http::Request& request)
{
   std::string isDarkStr = request.queryParamValue("dark");
   bool isDark = false;
   try
   {
      isDark = convertToBool(isDarkStr);
   }
   catch (boost::bad_lexical_cast)
   {
      LOG_WARNING_MESSAGE("\"dark\" parameter for request is missing or not a true or false value: " + isDarkStr);
   }

   if (isDark)
   {
      return getDefaultThemePath().childPath("tomorrow_night.rstheme");
   }
   else
   {
      return getDefaultThemePath().childPath("textmate.rstheme");
   }
}

/**
 * @brief Gets a theme that is installed with RStudio.
 *
 * @param request       The HTTP request from the client.
 * @param pResponse     The HTTP response, which will contain the theme CSS.
 */
void handleDefaultThemeRequest(const http::Request& request,
                                     http::Response* pResponse)
{
   std::string fileName = http::util::pathAfterPrefix(request, kDefaultThemeLocation);
   pResponse->setCacheableFile(getDefaultThemePath().childPath(fileName), request);
}

/**
 * @brief Gets a custom theme that is installed for all users.
 *
 * @param request       The HTTP request from the client.
 * @param pResponse     The HTTP response, which will contain the theme CSS.
 */
void handleGlobalCustomThemeRequest(const http::Request& request,
                                          http::Response* pResponse)
{
   // Note: we probably want to return a warning code instead of success so the client has the
   // ability to pop up a warning dialog or something to the user.
   std::string fileName = http::util::pathAfterPrefix(request, kGlobalCustomThemeLocation);
   FilePath requestedTheme = getGlobalCustomThemePath().childPath(fileName);
   pResponse->setCacheableFile(
      requestedTheme.exists() ? requestedTheme : getDefaultTheme(request),
      request);
}

/**
 * @brief Gets a custom theme that is installed for all users.
 *
 * @param request       The HTTP request from the client.
 * @param pResponse     The HTTP response, which will contain the theme CSS.
 */
void handleLocalCustomThemeRequest(const http::Request& request,
                                         http::Response* pResponse)
{
   // Note: we probably want to return a warning code instead of success so the client has the
   // ability to pop up a warning dialog or something to the user.
   std::string fileName = http::util::pathAfterPrefix(request, kLocalCustomThemeLocation);
   FilePath requestedTheme = getLocalCustomThemePath().childPath(fileName);
   pResponse->setCacheableFile(
      requestedTheme.exists() ? requestedTheme : getDefaultTheme(request),
      request);
}

/**
 * @brief Gets the list of all the avialble themes for the client.
 *
 * @param request       The JSON request from the client.
 * @param pResponse     The JSON response, which will contain the list of themes.
 *
 * @return The error that occurred, if any; otherwise Success().
 */
Error getThemes(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   ThemeMap themes = getAllThemes();

   // Convert the theme to a json array.
   json::Array jsonThemeArray;
   for (auto theme: themes)
   {
      json::Object jsonTheme;
      jsonTheme["name"] = std::get<0>(theme.second);
      jsonTheme["url"] = std::get<1>(theme.second);
      jsonTheme["isDark"] = std::get<2>(theme.second);
      jsonThemeArray.push_back(jsonTheme);
   }

   pResponse->setResult(jsonThemeArray);
   return Success();
}

} // anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   RS_REGISTER_CALL_METHOD(rs_getThemes, 0);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionThemes.R"))
      (bind(registerRpcMethod, "get_themes", getThemes))
      (bind(registerUriHandler, kDefaultThemeLocation, handleDefaultThemeRequest))
      (bind(registerUriHandler, kGlobalCustomThemeLocation, handleGlobalCustomThemeRequest))
      (bind(registerUriHandler, kLocalCustomThemeLocation, handleLocalCustomThemeRequest));

   return initBlock.execute();
}

} // namespace themes
} // namespace modules
} // namespace session
} // namespace rstudio
