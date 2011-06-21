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

#include "core/system/System.hpp"
#include "core/StringUtils.hpp"

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

   virtual core::Error status(const FilePath&,
                              StatusResult* pStatusResult)
   {
      *pStatusResult = StatusResult();
      return Success();
   }

   // TODO: Figure out why compile fails when args is const&
   virtual core::Error runCommand(const std::string& command,
                                  std::vector<std::string> args,
                                  std::vector<std::string>* pOutputLines)
   {
      std::string cmd("cd ");
      cmd.append(string_utils::bash_escape(root_));
      cmd.append("; ");
      cmd.append(command);
      for (std::vector<std::string>::iterator it = args.begin();
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

      boost::algorithm::split(*pOutputLines, output,
                              boost::algorithm::is_any_of("\r\n"));

      return Success();
   }

protected:
   FilePath root_;
};

boost::scoped_ptr<VCSImpl> s_pVcsImpl_;

class GitVCSImpl : public VCSImpl
{
public:
   GitVCSImpl()
   {
   }

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
};

class SubversionVCSImpl : public VCSImpl
{
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
};

} // anonymous namespace


VCS activeVCS()
{
   return VCSNone;
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

core::Error initialize()
{
   switch (activeVCS())
   {
   case VCSGit:
      s_pVcsImpl_.reset(new GitVCSImpl());
      break;
   case VCSSubversion:
      s_pVcsImpl_.reset(new SubversionVCSImpl());
      break;
   default:
      s_pVcsImpl_.reset(new VCSImpl());
      break;
   }

   return Success();
}

} // namespace source_control
} // namespace modules
} // namespace session
