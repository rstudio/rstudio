/*
 * Win32StringUtils.cpp
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

#include <shared_core/system/User.hpp>

#include <shlobj.h>

#include <boost/algorithm/string.hpp>
#include <boost/bind.hpp>

#include <shared_core/FilePath.hpp>
#include <shared_core/Logger.hpp>
#include <shared_core/SafeConvert.hpp>
#include <shared_core/system/Win32StringUtils.hpp>

namespace rstudio {
namespace core {
namespace system {

// home path strategies
namespace {

FilePath environmentHomePath(std::string envVariables)
{
   using namespace boost::algorithm;

   // use environment override if specified
   if (!envVariables.empty())
   {
      for (split_iterator<std::string::iterator> it =
         make_split_iterator(envVariables, first_finder("|", is_iequal()));
           it != split_iterator<std::string::iterator>();
           ++it)
      {
         std::string envHomePath =
            detail::getenv(boost::copy_range<std::string>(*it));
         if (!envHomePath.empty())
         {
            FilePath userHomePath(envHomePath);
            if (userHomePath.exists())
               return userHomePath;
         }
      }
   }

   // no override
   return FilePath();
}

FilePath currentCSIDLPersonalHomePath()
{
   // query for My Documents directory
   const DWORD SHGFP_TYPE_CURRENT = 0;
   wchar_t homePath[MAX_PATH];
   HRESULT hr = ::SHGetFolderPathW(nullptr,
                                   CSIDL_PERSONAL,
                                   nullptr,
                                   SHGFP_TYPE_CURRENT,
                                   homePath);
   if (SUCCEEDED(hr))
   {
      return FilePath(homePath);
   }
   else
   {
      log::logWarningMessage("Unable to retreive user home path. HRESULT:  " +
                          safe_convert::numberToString(hr));
      return FilePath();
   }
}

FilePath defaultCSIDLPersonalHomePath()
{
   // query for default and force creation (works around situations
   // where redirected path is not available)
   const DWORD SHGFP_TYPE_DEFAULT = 1;
   wchar_t homePath[MAX_PATH];
   HRESULT hr = ::SHGetFolderPathW(nullptr,
                                   CSIDL_PERSONAL|CSIDL_FLAG_CREATE,
                                   nullptr,
                                   SHGFP_TYPE_DEFAULT,
                                   homePath);
   if (SUCCEEDED(hr))
   {
      return FilePath(homePath);
   }
   else
   {
      log::logWarningMessage("Unable to retreive user home path. HRESULT:  " +
                          safe_convert::numberToString(hr));
      return FilePath();
   }
}

FilePath homepathHomePath()
{
   std::string homeDrive = detail::getenv("HOMEDRIVE");
   std::string homePath = detail::getenv("HOMEPATH");
   if (!homeDrive.empty() && !homePath.empty())
      return FilePath(homeDrive + homePath);
   else
      return FilePath();
}

FilePath homedriveHomePath()
{
   std::string homeDrive = detail::getenv("HOMEDRIVE");
   if (homeDrive.empty())
      homeDrive = "C:";
   return FilePath(homeDrive);
}

typedef std::pair<std::string,boost::function<FilePath()> > HomePathSource;

} // anonymous namespace

namespace detail {

std::string getenv(const std::string& name)
{
   std::wstring nameWide(name.begin(), name.end());

   // get the variable
   DWORD nSize = 256;
   std::vector<wchar_t> buffer(nSize);
   DWORD result = ::GetEnvironmentVariableW(nameWide.c_str(), &(buffer[0]), nSize);
   if (result == 0) // not found
   {
      return std::string();
   }
   if (result > nSize) // not enough space in buffer
   {
      nSize = result;
      buffer.resize(nSize);
      result = ::GetEnvironmentVariableW(nameWide.c_str(), &(buffer[0]), nSize);
      if (result == 0 || result > nSize)
         return std::string(); // VERY unexpected failure case
   }

   // return it
   return string_utils::wideToUtf8(&(buffer[0]));
}

} // namespace detail


FilePath User::getUserHomePath(const std::string& in_envOverride)
{
   using boost::bind;
   std::vector<HomePathSource> sources;
   sources.push_back(std::make_pair("R_USER|HOME",
                                    bind(environmentHomePath, in_envOverride)));
   sources.push_back(std::make_pair("SHGFP_TYPE_CURRENT",
                                    currentCSIDLPersonalHomePath));
   sources.push_back(std::make_pair("SHGFP_TYPE_DEFAULT",
                                    defaultCSIDLPersonalHomePath));
   std::string envFallback = "USERPROFILE";
   sources.push_back(std::make_pair(envFallback,
                                    bind(environmentHomePath, envFallback)));
   sources.push_back(std::make_pair("HOMEPATH",
                                    homepathHomePath));
   sources.push_back(std::make_pair("HOMEDRIVE",
                                    homedriveHomePath));

   for (const HomePathSource& source : sources)
   {
      FilePath homePath = source.second();
      if (!homePath.isEmpty())
      {
         // return if we found one that exists
         if (homePath.exists())
         {
            std::string path = homePath.getAbsolutePath();

            // standardize drive letter capitalization if in X:/y/z format
            if (path.length() > 1 && path[1] == ':')
            {
               path[0] = toupper(path[0]);
               homePath = FilePath(path);
            }

            return homePath;
         }

         // otherwise warn that we got a value that didn't exist
         log::logWarningMessage("Home path returned by " + source.first + " (" +
                             homePath.getAbsolutePath() + ") does not exist.");
      }
   }

   // no luck!
   log::logErrorMessage("No valid home path found for user");
   return FilePath();
}


} // namesapce system
} // namespace core
} // namespace rstudio
