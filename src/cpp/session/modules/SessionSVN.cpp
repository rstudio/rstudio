/*
 * SessionSVN.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionSVN.hpp"

#include <boost/bind.hpp>

#include <core/rapidxml/rapidxml.hpp>
#include <core/system/Environment.hpp>
#include <core/system/Process.hpp>
#include <core/system/ShellUtils.hpp>
#include <core/Exec.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>

using namespace core;
using namespace core::shell_utils;

namespace session {
namespace modules {
namespace svn {

namespace {

/** GLOBAL STATE **/
FilePath s_workingDir;

core::system::ProcessOptions procOptions()
{
   core::system::ProcessOptions options;

   // detach the session so there is no terminal
#ifndef _WIN32
   options.detachSession = true;
#endif

   // get current environment for modification prior to passing to child
   core::system::Options childEnv;
   core::system::environment(&childEnv);

   // TODO: Add Subversion bin dir to path if necessary

   // add postback directory to PATH
   FilePath postbackDir = session::options().rpostbackPath().parent();
   core::system::addToPath(&childEnv, postbackDir.absolutePath());

   options.workingDir = projects::projectContext().directory();

   // on windows set HOME to USERPROFILE
#ifdef _WIN32
   std::string userProfile = core::system::getenv(childEnv, "USERPROFILE");
   core::system::setenv(&childEnv, "HOME", userProfile);
#endif

   // set custom environment
   options.environment = childEnv;

   return options;
}

} // namespace


bool isSvnInstalled()
{
   core::system::ProcessResult result;
   Error error = core::system::runCommand("svn help", "", procOptions(), &result);

   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   return result.exitStatus == EXIT_SUCCESS;
}

bool isSvnDirectory(const core::FilePath& workingDir)
{
   if (workingDir.empty())
      return false;

   core::system::ProcessOptions options = procOptions();
   options.workingDir = workingDir;

   core::system::ProcessResult result;

   Error error = core::system::runCommand("svn info",
                                          "",
                                          options,
                                          &result);

   if (error)
      return false;

   return result.exitStatus == EXIT_SUCCESS;
}

bool isSvnEnabled()
{
   return !s_workingDir.empty();
}

std::string translateItemStatus(const std::string& status)
{
   if (status == "added")
      return "A";
   if (status == "conflicted")
      return "C";
   if (status == "deleted")
      return "D";
   if (status == "external")
      return "X";
   if (status == "ignored")
      return "I";
   if (status == "incomplete")
      return "!";
   if (status == "merged")      // ??
      return "G";
   if (status == "missing")
      return "!";
   if (status == "modified")
      return "M";
   if (status == "none")
      return " ";
   if (status == "normal")      // ??
      return " ";
   if (status == "obstructed")  // ??
      return "!";
   if (status == "replaced")
      return "~";
   if (status == "unversioned")
      return "?";

   return " ";
}

Error svnStatus(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   core::system::ProcessResult result;
   Error error = core::system::runCommand(
         ShellCommand("svn") << "status" << "--xml",
         "",
         procOptions(),
         &result);

   if (error)
      return error;

   if (result.exitStatus != EXIT_SUCCESS)
   {
      LOG_ERROR_MESSAGE(result.stdErr);
      return Success();
   }

   std::vector<char> xmlData;
   xmlData.reserve(result.stdOut.size() + 1);
   std::copy(result.stdOut.begin(),
             result.stdOut.end(),
             std::back_inserter(xmlData));
   xmlData.push_back(NULL); // null terminator

   using namespace rapidxml;
   xml_document<> doc;
   doc.parse<0>(&(xmlData[0]));

   json::Array results;

   xml_node<>* pStatus = doc.first_node("status");
   if (pStatus)
   {
      xml_node<>* pList = pStatus->first_node();
      for (; pList; pList = pList->next_sibling())
      {
         xml_node<>* pEntry = pList->first_node("entry");
         for (; pEntry; pEntry = pEntry->next_sibling("entry"))
         {
            xml_attribute<>* pPath = pEntry->first_attribute("path");
            if (!pPath)
            {
               LOG_ERROR_MESSAGE("Path attribute not found");
               continue;
            }
            std::string path(pPath->value());

            xml_node<>* pStatus = pEntry->first_node("wc-status");
            if (!pStatus)
            {
               LOG_ERROR_MESSAGE("Status node not found");
               continue;
            }

            xml_attribute<>* pItem = pStatus->first_attribute("item");
            if (!pItem)
            {
               LOG_ERROR_MESSAGE("Item attribute not found");
               continue;
            }
            std::string item(pItem->value());

            json::Object info;
            info["status"] = translateItemStatus(item);
            // TODO: escape path relative to <target>
            info["path"] = path;
            info["raw_path"] = module_context::createAliasedPath(
                  projects::projectContext().directory().childPath(path));
            results.push_back(info);
         }
      }
   }

   pResponse->setResult(results);

   return Success();
}

Error initialize()
{
   // install rpc methods
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "svn_status", svnStatus));
   Error error = initBlock.execute();
   if (error)
      return error;

   return Success();
}

Error initializeSvn(const core::FilePath& workingDir)
{
   s_workingDir = workingDir;
   return Success();
}

} // namespace svn
} // namespace modules
} //namespace session
