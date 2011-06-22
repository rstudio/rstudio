/*
 * SessionSourceControl.cpp
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
#include "SessionSourceControl.hpp"

#include <boost/algorithm/string.hpp>
#include <boost/algorithm/string/split.hpp>

#include <core/json/JsonRpc.hpp>
#include <core/system/System.hpp>
#include <core/Exec.hpp>
#include <core/StringUtils.hpp>

#include "session/SessionModuleContext.hpp"

namespace session {
namespace modules {
namespace source_control {

namespace {

class VCSImpl
{
public:
   VCSImpl()
   {
      root_ = module_context::initialWorkingDirectory();
   }

   virtual ~VCSImpl()
   {
   }

   virtual VCS id() { return VCSNone; }
   virtual std::string name() { return std::string(); }

   virtual core::Error status(const FilePath&,
                              StatusResult* pStatusResult)
   {
      *pStatusResult = StatusResult();
      return Success();
   }

   virtual core::Error revert(const std::vector<FilePath>& filePaths,
                              std::string* pStdErr)
   {
      return Success();
   }

   virtual core::Error runCommand(const std::string& command,
                                  const std::vector<std::string>& args,
                                  std::vector<std::string>* pOutputLines)
   {
      std::string cmd("cd ");
      cmd.append(string_utils::bash_escape(root_));
      cmd.append("; ");
      cmd.append(command);
      for (std::vector<std::string>::const_iterator it = args.begin();
           it != args.end();
           it++)
      {
         cmd.append(" ");
         cmd.append(string_utils::bash_escape(*it));
      }

      std::string output;
      Error error = core::system::captureCommand(cmd, &output);
      if (error)
         return error;

      if (pOutputLines)
      {
         boost::algorithm::split(*pOutputLines, output,
                                 boost::algorithm::is_any_of("\r\n"));
      }

      return Success();
   }

protected:
   FilePath root_;
};

boost::scoped_ptr<VCSImpl> s_pVcsImpl_;

class GitVCSImpl : public VCSImpl
{
public:
   VCS id() { return VCSGit; }
   std::string name() { return "git"; }

   core::Error status(const FilePath& dir, StatusResult* pStatusResult)
   {
      using namespace boost;

      std::vector<FileWithStatus> files;

      std::vector<std::string> args;
      args.push_back("status");
      args.push_back("--porcelain");
      args.push_back("--");
      args.push_back(string_utils::utf8ToSystem(dir.absolutePath()));

      std::vector<std::string> lines;
      Error error = runCommand("git", args, &lines);
      if (error)
         return error;

      for (std::vector<std::string>::iterator it = lines.begin();
           it != lines.end();
           it++)
      {
         std::string line = *it;
         if (line.length() < 4)
            continue;
         FileWithStatus file;
         switch (line[1])
         {
         case ' ':
            file.status = VCSStatusUnmodified;
            break;
         case 'M':
            file.status = VCSStatusModified;
            break;
         case 'A':
            file.status = VCSStatusAdded;
            break;
         case 'D':
            file.status = VCSStatusDeleted;
            break;
         case 'R':
            file.status = VCSStatusRenamed;
            break;
         case 'C':
            file.status = VCSStatusCopied;
            break;
         case 'U':
            file.status = VCSStatusUnmerged;
            break;
         case '?':
            file.status = VCSStatusUntracked;
            break;
         default:
            LOG_WARNING_MESSAGE("Unparseable git-status line: " + line);
            continue;
         }

         file.path = root_.childPath(string_utils::systemToUtf8(line.substr(3)));
         files.push_back(file);
      }

      *pStatusResult = StatusResult(files);

      return Success();
   }

   core::Error revert(const std::vector<FilePath>& filePaths,
                      std::string* pStdErr)
   {
      std::vector<std::string> args;
      args.push_back("checkout");
      args.push_back("--");
      for (std::vector<FilePath>::const_iterator it = filePaths.begin();
           it != filePaths.end();
           it++)
      {
        args.push_back(string_utils::utf8ToSystem((*it).absolutePath()));
      }

      runCommand("git", args, NULL);

      // TODO: Once we capture stderr we need to set it here
      if (pStdErr)
         *pStdErr = std::string();

      return Success();
   }
};

class SubversionVCSImpl : public VCSImpl
{
   VCS id() { return VCSSubversion; }
   std::string name() { return "svn"; }

   core::Error status(const FilePath& dir, StatusResult* pStatusResult)
   {
      using namespace boost;

      std::vector<FileWithStatus> files;

      std::vector<std::string> args;
      args.push_back("status");
      args.push_back("--non-interactive");
      args.push_back("--depth=immediates");
      args.push_back("--");
      args.push_back(string_utils::utf8ToSystem(dir.absolutePath()));

      std::vector<std::string> lines;
      Error error = runCommand("svn", args, &lines);
      if (error)
         return error;

      for (std::vector<std::string>::iterator it = lines.begin();
           it != lines.end();
           it++)
      {
         std::string line = *it;
         if (line.length() < 4)
            continue;
         FileWithStatus file;
         switch (line[0])
         {
         case ' ':
            file.status = VCSStatusUnmodified;
            break;
         case 'A':
            file.status = VCSStatusAdded;
            break;
         case 'C':
            file.status = VCSStatusUnmerged;
            break;
         case 'D':
            file.status = VCSStatusDeleted;
            break;
         case 'I':
            file.status = VCSStatusIgnored;
            break;
         case 'M':
            file.status = VCSStatusModified;
            break;
         case 'R':
            file.status = VCSStatusReplaced;
            break;
         case 'X':
            file.status = VCSStatusExternal;
            break;
         case '?':
            file.status = VCSStatusUntracked;
            break;
         case '!':
            file.status = VCSStatusMissing;
            break;
         case '~':
            file.status = VCSStatusObstructed;
            break;
         default:
            LOG_WARNING_MESSAGE("Unparseable svn-status line: " + line);
            continue;
         }

         file.path = FilePath(string_utils::systemToUtf8(line.substr(8)));
         files.push_back(file);
      }

      *pStatusResult = StatusResult(files);

      return Success();
   }

   core::Error revert(const std::vector<FilePath>& filePaths,
                      std::string* pStdErr)
   {
      std::vector<std::string> args;
      args.push_back("revert");
      args.push_back("--");
      for (std::vector<FilePath>::const_iterator it = filePaths.begin();
           it != filePaths.end();
           it++)
      {
        args.push_back(string_utils::utf8ToSystem((*it).absolutePath()));
      }

      runCommand("svn", args, NULL);

      // TODO: Once we capture stderr we need to set it here
      if (pStdErr)
         *pStdErr = std::string();

      return Success();
   }
};

} // anonymous namespace


VCS activeVCS()
{
   return s_pVcsImpl_->id();
}

std::string activeVCSName()
{
   return s_pVcsImpl_->name();
}

VCSStatus StatusResult::getStatus(const FilePath& fileOrDirectory)
{
   std::map<std::string, VCSStatus>::iterator found =
         this->filesByPath_.find(fileOrDirectory.absolutePath());
   if (found != this->filesByPath_.end())
      return found->second;

   return VCSStatusUnmodified;
}

core::Error status(const FilePath& dir, StatusResult* pStatusResult)
{
   return s_pVcsImpl_->status(dir, pStatusResult);
}

Error vcsRevert(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   json::Array paths;
   Error error = json::readParam(request.params, 0, &paths);
   if (error)
      return error ;

   std::vector<FilePath> parsedPaths;
   for (json::Array::iterator it = paths.begin();
        it != paths.end();
        it++)
   {
      json::Value value = *it;
      parsedPaths.push_back(
            module_context::resolveAliasedPath(value.get_str()));
   }

   return s_pVcsImpl_->revert(parsedPaths, NULL);
}

core::Error initialize()
{
   FilePath workingDir = module_context::initialWorkingDirectory();
   if (workingDir.childPath(".git").isDirectory())
      s_pVcsImpl_.reset(new GitVCSImpl());
   else if (workingDir.childPath(".svn").isDirectory())
      s_pVcsImpl_.reset(new SubversionVCSImpl());
   else
      s_pVcsImpl_.reset(new VCSImpl());

   // install rpc methods
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "vcs_revert", vcsRevert));
   Error error = initBlock.execute();
   if (error)
      return error;

   return Success();
}

} // namespace source_control
} // namespace modules
} // namespace session
