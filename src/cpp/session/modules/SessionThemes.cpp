/*
 * SessionThemes.cpp
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

#include "SessionThemes.hpp"

#include <boost/algorithm/string.hpp>
#include <boost/algorithm/string/join.hpp>
#include <boost/algorithm/algorithm.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/bind.hpp>
#include <boost/regex.hpp>

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>
#include <shared_core/FilePath.hpp>
#include <core/json/JsonRpc.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <core/system/Xdg.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/prefs/UserPrefs.hpp>
#include <session/prefs/UserState.hpp>

#include <r/RExec.hpp>
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

bool s_deferredInitComplete = false;
module_context::WaitForMethodFunction s_waitForThemeColors;

const std::string kDefaultThemeLocation = "theme/default/";
const std::string kGlobalCustomThemeLocation = "theme/custom/global/";
const std::string kLocalCustomThemeLocation = "theme/custom/local/";

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
 * @brief Gets an error out of the object, if there is one, and updates pResponse.
 *
 * @param object        The object to check for an error.
 * @param pResponse     The response to update.
 *
 * @return true if an error is found; false otherwise.
 */
bool extractError(SEXP object, json::JsonRpcResponse* pResponse)
{
   if (r::sexp::isList(object))
   {
      std::vector<std::string> classes;
      r::sexp::fillVectorString(r::sexp::getAttrib(object, R_ClassSymbol), &classes);
      if (std::find(classes.begin(), classes.end(), "error") != classes.end())
      {
         std::string errorMessage;
         r::sexp::getNamedListElement(object, "message", &errorMessage);

         json::Object errorDesc;
         errorDesc["code"] = json::errc::ExecutionError;
         errorDesc["message"] = errorMessage;

         json::Object error;
         error["error"] = errorDesc;
         pResponse->setResponse(error);

         return true;
      }
   }

   return false;
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
      location.getChildren(locationChildren);
      for (const FilePath& themeFile: locationChildren)
      {
         if (themeFile.hasExtensionLowerCase(".rstheme"))
         {
#ifdef _WIN32
            const std::wstring k_themeFileStr = themeFile.getAbsolutePathW();
#else
            const std::string k_themeFileStr = themeFile.getCanonicalPath();
#endif
            std::ifstream themeIFStream(k_themeFileStr);
            std::string themeContents(
               (std::istreambuf_iterator<char>(themeIFStream)),
               (std::istreambuf_iterator<char>()));
            themeIFStream.close();

            boost::smatch matches;
            bool found = boost::regex_search(
               themeContents,
               matches,
               boost::regex("rs-theme-name\\s*:\\s*([^\\*]+?)\\s*(?:\\*|$)"));

            // If there's no name specified,use the name of the file
            std::string name;
            if (!found || (matches.size() < 2) || (matches[1] == ""))
            {
               name = themeFile.getStem();
            }
            else
            {
               // If there's at least one name specified, get the first one.
               name = matches[1];
            }

            // Find out if the theme is dark or not.
            found = boost::regex_search(
                     themeContents,
                     matches,
                     boost::regex("rs-theme-is-dark\\s*:\\s*([^\\*]+?)\\s*(?:\\*|$)"));

            bool isDark = false;
            if (found && (matches.size() >= 2))
            {
               try
               {
                  isDark = convertToBool(matches[1].str());
               }
               catch (boost::bad_lexical_cast&)
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
               urlPrefix + http::util::urlEncode(themeFile.getFilename()),
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
   return session::options().rResourcesPath().completeChildPath("themes");
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

   return core::system::xdg::systemConfigFile("themes");
}

/**
 * @brief Gets the location of custom themes from an environment variable.
 *
 * @return The location of custom themes defined by the environment variable, or an empty path if
 * the variable is not set.
 */
FilePath getEnvCustomThemePath()
{
   const char* kLocalPathAlt = std::getenv("RS_THEME_LOCAL_HOME");
   if (kLocalPathAlt)
   {
      return FilePath(kLocalPathAlt);
   }

   return FilePath();
}

/**
 * @brief Gets the location of custom themes that are installed for the current user (legacy RStudio
 * 1.2 version)
 *
 * @return The location of custom themes that are installed for the current user.
 */
FilePath getLegacyLocalCustomThemePath()
{
   return module_context::userHomePath().completeChildPath(".R/rstudio/themes/");
}

/**
 * @brief Gets the location of custom themes that are installed for the current user.
 *
 * @return The location of custom themes that are installed for the current user.
 */
FilePath getLocalCustomThemePath()
{
   return core::system::xdg::userConfigDir().completePath("themes");
}

/**
 * @brief Gets the local custom theme from either the configured location or one of the two default locations (legacy or
 *        current.
 *
 * @param themeFileName     The name of the theme file to get.
 *
 * @return The theme FilePath.
 */
FilePath getLocalCustomTheme(std::string themeFileName)
{
   // Check if there is an local custom theme path override configured in the environment.
   FilePath envDir = getEnvCustomThemePath();

   // Look in the configured location, if there was a configured value. Other wise check the defaults.
   FilePath requestedTheme;
   if (envDir.isEmpty())
   {
      // Check first in the local custom theme path; if the theme isn't found there, try the legacy
      // theme path (where RStudio 1.2 wrote custom themes)
      requestedTheme = getLocalCustomThemePath().completeChildPath(themeFileName);
      if (!requestedTheme.exists())
         requestedTheme = getLegacyLocalCustomThemePath().completeChildPath(themeFileName);
   }
   else
   {
      requestedTheme = envDir.completeChildPath(themeFileName);
   }

   return requestedTheme;
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

   // Check for an explicit path set from an environment variable. If set, this overrides the
   // less specific built-in/XDG defaults.
   FilePath envPath = getEnvCustomThemePath();
   if (envPath.isEmpty())
   {
      // No specific theme path set from environment variable, use defaults
      getThemesInLocation(getLegacyLocalCustomThemePath(), kLocalCustomThemeLocation, &themeMap);
      getThemesInLocation(getLocalCustomThemePath(), kLocalCustomThemeLocation, &themeMap);
   }
   else
   {
      // Use the specific theme path set from the environment variable
      getThemesInLocation(envPath, kLocalCustomThemeLocation, &themeMap);
   }

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
   r::sexp::Protect protect;
   r::sexp::ListBuilder themeListBuilder(&protect);

   for (auto theme: themeMap)
   {
      r::sexp::ListBuilder themeDetailsListBuilder(&protect);
      themeDetailsListBuilder.add("name", std::get<0>(theme.second));
      themeDetailsListBuilder.add("url", std::get<1>(theme.second));
      themeDetailsListBuilder.add("isDark", std::get<2>(theme.second));

      themeListBuilder.add(theme.first, themeDetailsListBuilder);
   }

   return rstudio::r::sexp::create(themeListBuilder, &protect);
}

/**
 * @brief Returns the foreground and background color of the active theme.
 *
 * @return An R list with the foreground and background, or NULL if the color could not be
 *         determined.
 */
SEXP rs_getThemeColors()
{
   r::sexp::Protect protect;
   json::JsonRpcRequest request;
   r::sexp::ListBuilder themeColors(&protect);

   // Don't attempt to call the WaitForMethod unless the session has fully initialized.
   if (!s_deferredInitComplete)
      return R_NilValue;

   // Query the client for its current theme colors
   if (!s_waitForThemeColors(&request, 
            ClientEvent(client_events::kComputeThemeColors, json::Value())))
   {
      // Client did not return colors
      r::exec::warning("Active theme colors not available.");
      return R_NilValue;
   }
   
   // Parse the theme colors returned by the client
   std::string foreground, background;
   Error error = json::readParams(request.params, &foreground, &background);
   if (error)
   {
      // Client returned something we didn't understand
      r::exec::warning("No theme colors could be determined: " + error.getSummary());
      return R_NilValue;
   }

   // Form the list and return to caller 
   themeColors.add("foreground", foreground);
   themeColors.add("background", background);
   return r::sexp::create(themeColors, &protect);
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
   catch (boost::bad_lexical_cast&)
   {
      LOG_WARNING_MESSAGE("\"dark\" parameter for request is missing or not a true or false value: " + isDarkStr);
   }

   if (isDark)
   {
      return getDefaultThemePath().completeChildPath("tomorrow_night.rstheme");
   }
   else
   {
      return getDefaultThemePath().completeChildPath("textmate.rstheme");
   }
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

/**
 * @brief RPC that lets the client add a theme for the current user.
 *
 * @param request       The request from the client to add a theme. The theme should already exist
 *                      on the server and the only parameter on the request should be the location
 *                      of the theme to add.
 * @param pResponse     The response from the server. Will contain the name of the newly added theme.
 *
 * @return `Success` on success; an error otherwise.
 */
Error addTheme(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   std::string themeToAdd;
   Error error = json::readParams(request.params, &themeToAdd);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }

   FilePath themeFile = module_context::resolveAliasedPath(themeToAdd);

   // Find out whether to convert or add.
   std::string funcName = ".rs.internal.convertTheme";
   if (!themeFile.exists())
   {
      error = Error(json::errc::ParamInvalid, ERROR_LOCATION);
      error.addProperty("queryParam", themeToAdd);
      error.addProperty("details", "Theme file does not exist.");
   }
   else if (themeFile.getExtensionLowerCase() == ".rstheme")
   {
      funcName = ".rs.internal.addTheme";
   }
   else if (!(themeFile.getExtensionLowerCase() == ".tmtheme"))
   {
      assert(false);
      error = Error(json::errc::ParamInvalid, ERROR_LOCATION);
      error.addProperty("queryParam", themeToAdd);
      error.addProperty("details", "Invalid file type for theme.");
   }

   std::string result;
   if (!error)
   {
      r::exec::RFunction rfunc(funcName);
      rfunc.addParam("themePath", themeToAdd);
      SEXP sexpResult;
      r::sexp::Protect protect;
      error = rfunc.call(&sexpResult, &protect);
      if (!error)
         if (extractError(sexpResult, pResponse))
            return error;

      // Check if the result is an error
      if (!error)
         error = r::sexp::extract(sexpResult, &result);
   }

   if (error)
      LOG_ERROR(error);
   else
      pResponse->setResult(result);

   return error;
}

/**
 * @brief An RPC allowing the client to remove a custom theme.
 *
 * @param request       The request to remove the theme. The first parameter should be the name of
 *                      the theme.
 * @param pResponse     The response. Empty if succesful; error otherwise.
 *
 * @return `Success` on a successful removal; an error otherwise.
 */
Error removeTheme(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   std::string themeName;
   Error error = json::readParams(request.params, &themeName);

   if (!error)
   {
      r::exec::RFunction removeFunc(".rs.internal.removeTheme");
      removeFunc.addParam("name", themeName);
      removeFunc.addParam("themeList", rs_getThemes());

      r::sexp::Protect protect;
      SEXP result;
      error = removeFunc.call(&result, &protect);
      if (!error)
         extractError(result, pResponse);
   }

   return error;
}

void onDeferredInit(bool)
{
   s_deferredInitComplete = true;
}

void setCacheableFile(const FilePath& filePath,
                      const http::Request& request,
                      http::Response* pResponse)
{
   pResponse->setCacheableFile(filePath, request);
}

} // anonymous namespace

/**
 * @brief Gets a theme that is installed with RStudio.
 *
 * @param request       The HTTP request from the client.
 * @param pResponse     The HTTP response, which will contain the theme CSS.
 */
void handleDefaultThemeRequest(const http::Request& request,
                                     http::Response* pResponse)
{
   std::string prefix = "/" + kDefaultThemeLocation;
   std::string fileName = http::util::pathAfterPrefix(request, prefix);
   setCacheableFile(getDefaultThemePath().completeChildPath(fileName), request, pResponse);
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
   std::string prefix = "/" + kGlobalCustomThemeLocation;
   std::string fileName = http::util::pathAfterPrefix(request, prefix);
   FilePath requestedTheme = getGlobalCustomThemePath().completeChildPath(fileName);
   setCacheableFile(
      requestedTheme.exists() ? requestedTheme : getDefaultTheme(request),
      request,
      pResponse);
}

/**
 * @brief Gets a custom theme that is installed for this user.
 *
 * @param request       The HTTP request from the client.
 * @param pResponse     The HTTP response, which will contain the theme CSS.
 */
void handleLocalCustomThemeRequest(const http::Request& request,
                                         http::Response* pResponse)
{
   // Note: we probably want to return a warning code instead of success so the client has the
   // ability to pop up a warning dialog or something to the user.
   std::string prefix = "/" + kLocalCustomThemeLocation;
   std::string fileName = http::util::pathAfterPrefix(request, prefix);

   FilePath requestedTheme = getLocalCustomTheme(fileName);
   setCacheableFile(
      requestedTheme.exists() ? requestedTheme : getDefaultTheme(request),
      request,
      pResponse);
}

Error syncThemePrefs()
{
   // Determine whether the preference storing the theme is out of sync with the theme details in
   // user state.
   Error err;
   std::string prefTheme = prefs::userPrefs().editorTheme();
   json::Object stateTheme = prefs::userState().theme();
   auto themeName = stateTheme.find(kThemeName);
   if (themeName != stateTheme.end() &&
       (*themeName).getValue().getString() != prefTheme)
   {
      bool found = false;
      ThemeMap themes = getAllThemes();
      json::Array jsonThemeArray;
      for (auto theme: themes)
      {
         if (std::get<0>(theme.second) == prefTheme)
         {
            found = true;
            json::Object jsonTheme;
            jsonTheme["name"] = std::get<0>(theme.second);
            jsonTheme["url"] = std::get<1>(theme.second);
            jsonTheme["isDark"] = std::get<2>(theme.second);
            err = prefs::userState().setTheme(jsonTheme);
            break;
         }
      }

      if (!found)
      {
         LOG_WARNING_MESSAGE("The theme preference was set to '" + prefTheme + "' "
               "but no theme with that name was found.");
      }
   }

   return err;
}

SEXP rs_getGlobalThemeDir()
{
   r::sexp::Protect protect;
   return r::sexp::create(getGlobalCustomThemePath().getAbsolutePath(), &protect);
}

SEXP rs_getLocalThemeDir()
{
   // Check for a configured custom location before returning the default custom location. Never return the legacy
   // default custom location because we don't want to add new files there.
   FilePath themeDir = getEnvCustomThemePath();
   if (themeDir.isEmpty())
      themeDir = getLocalCustomThemePath();

   r::sexp::Protect protect;
   return r::sexp::create(themeDir.getAbsolutePath(), &protect);
}

SEXP rs_getLocalThemePath(SEXP themeFileSEXP)
{
   std::string themeFile = r::sexp::asString(themeFileSEXP);

   if (themeFile.empty())
      return R_NilValue;

   FilePath requestedTheme = getLocalCustomTheme(themeFile);
   if (requestedTheme.isEmpty())
      return R_NilValue;


   r::sexp::Protect protect;
   return r::sexp::create(requestedTheme.getAbsolutePath(), &protect);
}

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   s_waitForThemeColors = registerWaitForMethod("set_computed_theme_colors");

   RS_REGISTER_CALL_METHOD(rs_getThemes);
   RS_REGISTER_CALL_METHOD(rs_getLocalThemeDir);
   RS_REGISTER_CALL_METHOD(rs_getGlobalThemeDir);
   RS_REGISTER_CALL_METHOD(rs_getThemeColors);
   RS_REGISTER_CALL_METHOD(rs_getLocalThemePath);

   events().onDeferredInit.connect(onDeferredInit);

   // We need to register our URI handlers twice to cover the data viewer grid document because those
   // links have a different prefix.
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(
         sourceModuleRFile,
         session::options().rResourcesPath().completeChildPath("themes").completeChildPath("compile-themes.R").getAbsolutePath()))
      (bind(sourceModuleRFile, "SessionThemes.R"))
      (bind(registerRpcMethod, "get_themes", getThemes))
      (bind(registerRpcMethod, "add_theme", addTheme))
      (bind(registerRpcMethod, "remove_theme", removeTheme))
      (bind(registerUriHandler, "/" + kDefaultThemeLocation, handleDefaultThemeRequest))
      (bind(registerUriHandler, "/" + kGlobalCustomThemeLocation, handleGlobalCustomThemeRequest))
      (bind(registerUriHandler, "/" + kLocalCustomThemeLocation, handleLocalCustomThemeRequest))
      (bind(syncThemePrefs));

   return initBlock.execute();
}

} // namespace themes
} // namespace modules
} // namespace session
} // namespace rstudio
