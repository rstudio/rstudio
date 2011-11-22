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

   if (!s_workingDir.empty())
      options.workingDir = s_workingDir;
   else
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

Error runSvn(const ShellArgs& args,
             std::string* pStdOut=NULL,
             std::string* pStdErr=NULL,
             int* pExitCode=NULL)
{
   core::system::ProcessOptions options = procOptions();
#ifdef _WIN32
   options.detachProcess = true;
#endif

   core::system::ProcessResult result;

#ifdef _WIN32
   Error error = core::system::runProgram("svn.exe",
                                          args.args(),
                                          std::string(),
                                          options,
                                          &result);
#else
   Error error = core::system::runCommand(ShellCommand("svn") << args.args(),
                                          options,
                                          &result);
#endif
   if (error)
      return error;

   if (pStdOut)
      *pStdOut = result.stdOut;
   if (pStdErr)
      *pStdErr = result.stdErr;
   if (pExitCode)
      *pExitCode = result.exitStatus;

   if (result.exitStatus != EXIT_SUCCESS)
   {
      LOG_DEBUG_MESSAGE(result.stdErr);
   }

   return Success();
}

} // namespace


bool isSvnInstalled()
{
   int exitCode;
   Error error = runSvn(ShellArgs() << "help", NULL, NULL, &exitCode);

   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   return exitCode == EXIT_SUCCESS;
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
   if (status == "obstructed")
      return "~";
   if (status == "replaced")
      return "~";
   if (status == "unversioned")
      return "?";

   return " ";
}

int rankItemStatus(const std::string& status)
{
   if (status == " " || status.empty())
      return 10;

   if (status == "I")
      return 7;

   if (status == "M")
      return 1;

   if (status == "C")
      return 0;

   return 5;
}

std::string topStatus(const std::string& a, const std::string& b)
{
   if (rankItemStatus(a) <= rankItemStatus(b))
      return a;
   return b;
}

#define FOREACH_NODE(parent, varname, name) \
   for (rapidxml::xml_node<>* varname = parent->first_node(name); \
        varname; \
        varname = varname->next_sibling(name))

std::string attr_value(rapidxml::xml_node<>* pNode, const std::string& attrName)
{
   if (!pNode)
      return std::string();
   rapidxml::xml_attribute<>* pAttr = pNode->first_attribute(attrName.c_str());
   if (!pAttr)
      return std::string();
   return std::string(pAttr->value());
}

FilePath resolveAliasedJsonPath(const json::Value& value)
{
   return module_context::resolveAliasedPath(value.get_str());
}

Error svnAdd(const json::JsonRpcRequest& request,
             json::JsonRpcResponse* pResponse)
{
   json::Array files;
   Error error = json::readParams(request.params, &files);
   if (error)
      return error;

   std::vector<FilePath> paths;
   std::transform(files.begin(), files.end(), std::back_inserter(paths),
                  &resolveAliasedJsonPath);

   error = runSvn(ShellArgs() << "add" << "--" << paths);
   if (error)
      return error;

   return Success();
}

Error svnDelete(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   json::Array files;
   Error error = json::readParams(request.params, &files);
   if (error)
      return error;

   std::vector<FilePath> paths;
   std::transform(files.begin(), files.end(), std::back_inserter(paths),
                  &resolveAliasedJsonPath);

   error = runSvn(ShellArgs() << "delete" << "--" << paths);
   if (error)
      return error;

   return Success();
}

Error svnRevert(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   json::Array files;
   Error error = json::readParams(request.params, &files);
   if (error)
      return error;

   std::vector<FilePath> paths;
   std::transform(files.begin(), files.end(), std::back_inserter(paths),
                  &resolveAliasedJsonPath);

   error = runSvn(ShellArgs() << "revert" << "--" << paths);
   if (error)
      return error;

   return Success();
}

Error svnStatus(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   std::string stdOut, stdErr;
   int exitCode;
   Error error = runSvn(
         ShellArgs() << "status" << "--xml" << "--ignore-externals",
         &stdOut,
         &stdErr,
         &exitCode);
   if (error)
      return error;

   if (exitCode != EXIT_SUCCESS)
   {
      LOG_ERROR_MESSAGE(stdErr);
      return Success();
   }

   std::vector<char> xmlData;
   xmlData.reserve(stdOut.size() + 1);
   std::copy(stdOut.begin(),
             stdOut.end(),
             std::back_inserter(xmlData));
   xmlData.push_back('\0'); // null terminator

   using namespace rapidxml;
   xml_document<> doc;
   doc.parse<0>(&(xmlData[0]));

   json::Array results;

   xml_node<>* pStatus = doc.first_node("status");
   if (pStatus)
   {
      FOREACH_NODE(pStatus, pList,)
      {
         FOREACH_NODE(pList, pEntry, "entry")
         {
            std::string path = attr_value(pEntry, "path");
            if (path.empty())
            {
               LOG_ERROR_MESSAGE("Path attribute not found");
               continue;
            }

            xml_node<>* pStatus = pEntry->first_node("wc-status");
            if (!pStatus)
            {
               LOG_ERROR_MESSAGE("Status node not found");
               continue;
            }

            std::string item = attr_value(pStatus, "item");
            if (item.empty())
            {
               LOG_ERROR_MESSAGE("Item attribute not found");
               continue;
            }
            item = translateItemStatus(item);

            std::string props = attr_value(pStatus, "props");
            if (props.empty())
            {
               LOG_ERROR_MESSAGE("Item properties not found");
               continue;
            }
            props = translateItemStatus(props);

            std::string status = topStatus(item, props);

            json::Object info;
            info["status"] = status;
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
      (bind(registerRpcMethod, "svn_add", svnAdd))
      (bind(registerRpcMethod, "svn_delete", svnDelete))
      (bind(registerRpcMethod, "svn_revert", svnRevert))
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
