/*
 * SessionVCSUtils.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
#include "SessionVCSUtils.hpp"

#include <boost/regex.hpp>

#include <core/json/Json.hpp>

#include <r/RUtil.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace vcs_utils {

void enqueRefreshEventWithDelay(int delay)
{
   // Sometimes on commit, the subsequent request contains outdated
   // status (i.e. as if the commit had not happened yet). No idea
   // right now what is causing this. Add a delay for commits to make
   // sure the correct state is shown.

   json::Object data;
   data["delay"] = delay;
   module_context::enqueClientEvent(ClientEvent(client_events::kVcsRefresh,
                                                data));
}

void enqueueRefreshEvent()
{
   enqueRefreshEventWithDelay(0);
}

core::json::Object processResultToJson(
      const core::system::ProcessResult& result)
{
   core::json::Object obj;
   obj["output"] = result.stdOut;
   obj["exit_code"] = result.exitStatus;
   return obj;
}

FilePath fileFilterPath(const json::Value& fileFilterJson)
{
   if (json::isType<std::string>(fileFilterJson))
   {
      // get the underlying file path
      std::string aliasedPath= fileFilterJson.get_str();
      return module_context::resolveAliasedPath(aliasedPath);
   }
   else
   {
      return FilePath();
   }
}

void splitMessage(const std::string message,
                  std::string* pSubject,
                  std::string* pDescription)
{
   boost::smatch match;
   if (!regex_utils::match(message,
                           match,
                           boost::regex("(.*?)(\r?\n)+(.*)")))
   {
      *pSubject = message;
      *pDescription = std::string();
   }
   else
   {
      *pSubject = match[1];
      *pDescription = match[3];
   }
}

std::string convertToUtf8(const std::string& content, bool allowSubst)
{
   std::string output;
   Error error = module_context::convertToUtf8(
                        content,
                        projects::projectContext().defaultEncoding(),
                        allowSubst,
                        &output);
   if (error)
   {
      return content;
   }
   else
   {
      return output;
   }
}

// Transcode the contents of a diff while leaving the structure of the diff
// untouched (including filenames and other non-diff content).
// This is implemented using a quick and dirty regex instead of with a proper
// diff parser, so there might be edge cases where stuff gets transcoded that
// should not be.
std::string convertDiff(const std::string& diff,
                        const std::string& fromEncoding,
                        const std::string& toEncoding,
                        bool allowSubst,
                        bool* pSuccess)
{
   if (pSuccess)
      *pSuccess = true;

   if (fromEncoding == toEncoding)
      return diff;

   if ((fromEncoding.empty() || fromEncoding == "UTF-8") &&
       (toEncoding.empty() || toEncoding == "UTF-8"))
   {
      return diff;
   }

   if (pSuccess)
      *pSuccess = false;

   std::string result;

   std::string transcoded;
   Error error;

   std::string::const_iterator lastMatchEnd = diff.begin();

   try
   {
      boost::regex contentLine("^(?:[+\\- ]|(?:@@[+\\-,\\d ]+@@))(.+?)$");
      boost::sregex_iterator iter(diff.begin(), diff.end(), contentLine,
                                  boost::regex_constants::match_not_dot_newline);
      boost::sregex_iterator end;
      for (; iter != end; iter++)
      {
         const boost::smatch m = *iter;

         // Copy any lines we skipped over in getting here
         std::copy(m.prefix().first, m.prefix().second,
                   std::back_inserter(result));

         std::string line = std::string(m[0].first, m[0].second);
         if (boost::algorithm::starts_with(line, "+++ ") ||
             boost::algorithm::starts_with(line, "--- "))
         {
            // This is a +++ or --- line, leave it alone
            std::copy(m[0].first, m[0].second, std::back_inserter(result));
         }
         else
         {
            // This is a content line, replace it!

            // Copy the leading part of the match verbatim
            std::copy(m[0].first, m[1].first, std::back_inserter(result));

            transcoded.clear();
            error = r::util::iconvstr(std::string(m[1].first, m[1].second),
                  fromEncoding,
                  toEncoding,
                  allowSubst,
                  &transcoded);
            if (error)
               return diff;

            // Don't allow transcoding to break diff semantics, which would happen if
            // new lines were introduced
            if (transcoded.find('\n') != std::string::npos)
               return diff;

            std::copy(transcoded.begin(), transcoded.end(),
                      std::back_inserter(result));

            // This should never copy any characters with the regex as it is
            // written today, but keeping it for symmetry.
            std::copy(m[1].second, m[0].second, std::back_inserter(result));
         }

         lastMatchEnd = m[0].second;
      }
   }
   CATCH_UNEXPECTED_EXCEPTION;

   // Copy the last set of lines we skipped over (or if there were no matches,
   // then we're actually copying the entire diff)
   std::copy(lastMatchEnd, diff.end(), std::back_inserter(result));

   if (pSuccess)
      *pSuccess = true;

   return result;
}

} // namespace vcs_utils
} // namespace modules
} // namespace session
} // namespace rstudio
