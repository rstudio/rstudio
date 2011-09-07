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
#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/split.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/foreach.hpp>
#include <boost/function.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/optional.hpp>
#include <boost/regex.hpp>

#include <core/json/JsonRpc.hpp>
#include <core/system/ShellUtils.hpp>
#include <core/system/System.hpp>
#include <core/system/Process.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/Scope.hpp>
#include <core/StringUtils.hpp>

#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

using namespace core;
using namespace core::shell_utils;

namespace session {
namespace modules {
namespace source_control {

namespace {

enum PatchMode
{
   PatchModeWorking = 0,
   PatchModeStage = 1
};

struct CommitInfo
{
   std::string id;
   std::string author;
   std::string subject;
   std::string description;
   std::string parent;
   boost::int64_t date; // millis since epoch, UTC
};

class VCSImpl : boost::noncopyable
{
public:
   VCSImpl(FilePath rootDir)
   {
      root_ = rootDir;
   }

   virtual ~VCSImpl()
   {
   }

   virtual VCS id() { return VCSNone; }
   virtual std::string name() { return std::string(); }
   FilePath root() { return root_; }

   virtual core::Error status(const FilePath&,
                              StatusResult* pStatusResult)
   {
      *pStatusResult = StatusResult();
      return Success();
   }

   virtual core::Error add(const std::vector<FilePath>& filePaths,
                           std::string* pStdErr)
   {
      return Success();
   }

   virtual core::Error remove(const std::vector<FilePath>& filePaths,
                              std::string* pStdErr)
   {
      return Success();
   }

   virtual core::Error discard(const std::vector<FilePath>& filePaths,
                               std::string* pStdErr)
   {
      return Success();
   }

   virtual core::Error revert(const std::vector<FilePath>& filePaths,
                              std::string* pStdErr)
   {
      return Success();
   }

   virtual core::Error stage(const std::vector<FilePath>& filePaths,
                             std::string* pStdErr)
   {
      return Success();
   }

   virtual core::Error unstage(const std::vector<FilePath>& filePaths,
                               std::string* pStdErr)
   {
      return Success();
   }

   virtual core::Error listBranches(std::vector<std::string>* pBranches,
                                    boost::optional<size_t>* pActiveBranchIndex,
                                    std::string* pStdErr)
   {
      return Success();
   }

   virtual core::Error checkout(const std::string& id,
                                std::string* pStdErr)
   {
      return Success();
   }

   core::Error runCommand(const ShellCommand& command,
                          std::vector<std::string>* pOutputLines=NULL)
   {
      std::string output;
      Error error = runCommand(command, &output, NULL);
      if (error)
         return error;

      if (pOutputLines)
      {
         boost::algorithm::split(*pOutputLines, output,
                                 boost::algorithm::is_any_of("\r\n"));
      }

      return Success();
   }

   core::Error runCommand(const ShellCommand& command,
                          std::string* pOutput,
                          std::string* pStdErr)
   {
      std::string cmd = shell_utils::join(
            ShellCommand(std::string("cd")) << root_,
            command);

      core::system::ProcessResult result;
      Error error = core::system::runCommand(cmd,
                                             core::system::ProcessOptions(),
                                             &result);
      if (error)
         return error;

      if (pOutput)
         *pOutput = result.stdOut;
      if (pStdErr)
         *pStdErr = result.stdErr;

      return Success();
   }

   virtual core::Error diffFile(const FilePath& filePath,
                                PatchMode mode,
                                int contextLines,
                                std::string* pOutput)
   {
      return Success();
   }

   virtual core::Error applyPatch(const FilePath& patchFile,
                                  PatchMode patchMode,
                                  std::string* pStdErr)
   {
      return Success();
   }

   virtual core::Error log(const std::string& rev,
                           int maxentries,
                           std::vector<CommitInfo>* pOutput)
   {
      return Success();
   }

   virtual core::Error show(const std::string& rev,
                            std::string* pOutput)
   {
      return Success();
   }

protected:
   FilePath root_;
};

boost::scoped_ptr<VCSImpl> s_pVcsImpl_;

namespace {

ShellCommand git()
{
   return ShellCommand("git");
}

ShellCommand svn()
{
   return ShellCommand("svn");
}

} // anonymous namespace

class GitVCSImpl : public VCSImpl
{
public:
   static FilePath detectGitDir(FilePath workingDir)
   {
      std::string command = shell_utils::join_and(
            ShellCommand("cd") << workingDir,
            git() << "rev-parse" << "--show-toplevel");

      core::system::ProcessResult result;
      Error error = core::system::runCommand(command,
                                             core::system::ProcessOptions(),
                                             &result);
      if (error)
         return FilePath();

      if (result.exitStatus != 0)
         return FilePath();

      return FilePath(boost::algorithm::trim_copy(result.stdOut));
   }

   GitVCSImpl(FilePath repoDir) : VCSImpl(repoDir)
   {
   }

   VCS id() { return VCSGit; }
   std::string name() { return "Git"; }

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
      Error error = runCommand(
            git() << "status" << "--porcelain" << "--" << dir,
            &lines);
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

         file.status = line.substr(0, 2);

         std::string filePath = line.substr(3);
         if (filePath.length() > 1 && filePath[filePath.length() - 1] == '/')
            filePath = filePath.substr(0, filePath.size() - 1);
         file.path = root_.childPath(string_utils::systemToUtf8(filePath));

         files.push_back(file);
      }

      *pStatusResult = StatusResult(files);

      return Success();
   }

   core::Error doSimpleCmd(const ShellCommand& command,
                           std::string* pStdErr)
   {
      return runCommand(command, NULL, pStdErr);
   }

   core::Error add(const std::vector<FilePath>& filePaths,
                   std::string* pStdErr)
   {
      return doSimpleCmd(git() << "add" << "--" << filePaths,
                         pStdErr);
   }

   core::Error remove(const std::vector<FilePath>& filePaths,
                      std::string* pStdErr)
   {
      return doSimpleCmd(git() << "rm" << "--" << filePaths,
                         pStdErr);
   }

   core::Error discard(const std::vector<FilePath>& filePaths,
                       std::string* pStdErr)
   {
      std::vector<std::string> args;
      args.push_back("-f"); // don't fail on unmerged entries
      return doSimpleCmd(
            git() << "checkout" << "-f" << "--" << filePaths,
            pStdErr);
   }

   core::Error revert(const std::vector<FilePath>& filePaths,
                      std::string* pStdErr)
   {
      Error error = doSimpleCmd(
            git() << "reset" << "HEAD" << "--" << filePaths,
            pStdErr);
      if (error)
         return error;

      std::vector<std::string> args2;
      args2.push_back("-f"); // don't fail on unmerged entries
      return doSimpleCmd(
            git() << "checkout" << "-f" << "--" << filePaths,
            pStdErr);
   }

   core::Error stage(const std::vector<FilePath> &filePaths,
                     std::string *pStdErr)
   {
      StatusResult statusResult;
      this->status(root_, &statusResult);

      std::vector<FilePath> filesToAdd;
      std::vector<FilePath> filesToRm;

      BOOST_FOREACH(const FilePath& path, filePaths)
      {
         std::string status = statusResult.getStatus(path).status();
         if (status.size() < 2)
            continue;
         if (status[1] == 'D')
            filesToRm.push_back(path);
         else if (status[1] != ' ')
            filesToAdd.push_back(path);
      }

      Error error;

      if (!filesToAdd.empty())
      {
         error = this->add(filesToAdd, pStdErr);
         if (error)
            return error;
      }

      if (!filesToRm.empty())
      {
         error = this->remove(filesToRm, pStdErr);
         if (error)
            return error;
      }

      return Success();
   }

   core::Error unstage(const std::vector<FilePath>& filePaths,
                       std::string* pStdErr)
   {
      return doSimpleCmd(git() << "reset" << "HEAD" << "--" << filePaths,
                         pStdErr);
   }

   core::Error listBranches(std::vector<std::string>* pBranches,
                            boost::optional<size_t>* pActiveBranchIndex,
                            std::string* pStdErr)
   {
      std::vector<std::string> lines;

      Error error = runCommand(git() << "branch", &lines);
      if (error)
         return error;

      for (size_t i = 0; i < lines.size(); i++)
      {
         const std::string line = lines.at(i);
         if (line.size() < 2)
            break;

         if (boost::algorithm::starts_with(line, "* "))
            *pActiveBranchIndex = i;
         pBranches->push_back(line.substr(2));
      }

      return Success();
   }

   core::Error checkout(const std::string& id,
                        std::string* pStdErr)
   {
      return runCommand(git() << "checkout" << id << "--");
   }

   core::Error commit(const std::string& message, bool amend, bool signOff)
   {
      FilePath tempFile = module_context::tempFile("gitmsg", "txt");
      boost::shared_ptr<std::ostream> pStream;

      Error error = tempFile.open_w(&pStream);
      if (error)
         return error;

      *pStream << message;
      pStream->flush();
      pStream.reset();  // release file handle

      ShellCommand command = git() << "commit" << "-F" << tempFile;
      if (amend)
         command << "--amend";
      if (signOff)
         command << "--signoff";

      std::string stdErr;
      error = doSimpleCmd(command, &stdErr);

      // clean up commit message temp file
      Error removeError = tempFile.remove();
      if (removeError)
         LOG_ERROR(removeError);

      return error;
   }

   core::Error push(std::string* pOutput, std::string* pError)
   {
      Error error = runCommand(git() << "push", pOutput, pError);
      if (error)
         return error;
      return Success();
   }

   core::Error pull(std::string* pOutput, std::string* pError)
   {
      Error error = runCommand(git() << "pull", pOutput, pError);
      if (error)
         return error;
      return Success();
   }

   core::Error diffFile(const FilePath& filePath,
                        PatchMode mode,
                        int contextLines,
                        std::string* pOutput)
   {
      ShellCommand command = git() << "diff";
      command << "-U" + boost::lexical_cast<std::string>(contextLines);
      if (mode == PatchModeStage)
         command << "--cached";
      command << "--";
      command << filePath;

      Error error = runCommand(command, pOutput, NULL);
      if (error)
         return error;

      return Success();
   }

   boost::int64_t convertGitRawDate(const std::string& time,
                          const std::string& timeZone)
   {
      boost::int64_t secs = safe_convert::stringTo<boost::int64_t>(time, 0);

      int offset = safe_convert::stringTo<int>(timeZone, 0);

      // Positive timezone offset means we have to SUBTRACT
      // the offset to get UTC time, and vice versa
      int factor = offset > 0 ? -1 : 1;

      offset = abs(offset);
      int hours = offset / 100;
      int minutes = offset % 100;

      secs += factor * (hours * 60*60);
      secs += factor * (minutes * 60);

      return secs;
   }

   core::Error applyPatch(const FilePath& patchFile,
                          PatchMode patchMode,
                          std::string* pStdErr)
   {
      ShellCommand cmd = git() << "apply";
      if (patchMode == PatchModeStage)
         cmd << "--cached";
      cmd << "--";
      cmd << patchFile;

      return doSimpleCmd(cmd, pStdErr);
   }

   core::Error log(const std::string& rev,
                   int maxentries,
                   std::vector<CommitInfo>* pOutput)
   {
      std::vector<std::string> outLines;

      std::vector<std::string> args;
      args.push_back("log");
      args.push_back("--pretty=raw");
      args.push_back("--abbrev-commit");
      args.push_back("--abbrev=8");

      ShellCommand cmd = git() << "log";
      cmd << "--pretty=raw" << "--abbrev-commit" << "--abbrev=8";
      if (maxentries >= 0)
         cmd << "-" + boost::lexical_cast<std::string>(maxentries);
      if (!rev.empty())
         cmd << rev;

      Error error = runCommand(cmd, &outLines);
      if (error)
         return error;

      boost::regex kvregex("^(\\w+) (.*)$");
      boost::regex authTimeRegex("^(.*?) (\\d+) ([+\\-]?\\d+)$");

      CommitInfo currentCommit;

      for (std::vector<std::string>::const_iterator it = outLines.begin();
           it != outLines.end();
           it++)
      {
         boost::smatch smatch;
         if (boost::regex_search(*it, smatch, kvregex))
         {
            std::string key = smatch[1];
            std::string value = smatch[2];
            if (key == "commit")
            {
               if (!currentCommit.id.empty())
                  pOutput->push_back(currentCommit);

               currentCommit = CommitInfo();

               currentCommit.id = value;
            }
            else if (key == "author" || key == "committer")
            {
               boost::smatch authTimeMatch;
               if (boost::regex_search(value, authTimeMatch, authTimeRegex))
               {
                  std::string author = authTimeMatch[1];
                  std::string time = authTimeMatch[2];
                  std::string tz = authTimeMatch[3];

                  if (key == "author")
                     currentCommit.author = author;
                  else // if (key == "committer")
                     currentCommit.date = convertGitRawDate(time, tz);
               }
            }
            else if (key == "parent")
            {
               if (!currentCommit.parent.empty())
                  currentCommit.parent.push_back(' ');
               currentCommit.parent.append(value, 0, 8);
            }
         }
         else if (boost::starts_with(*it, "    "))
         {
            if (currentCommit.subject.empty())
               currentCommit.subject = it->substr(4);

            if (!currentCommit.description.empty())
               currentCommit.description.append("\n");
            currentCommit.description.append(it->substr(4));
         }
         else if (it->length() == 0)
         {
         }
         else
         {
            LOG_ERROR_MESSAGE("Unexpected git-log output");
         }
      }

      if (!currentCommit.id.empty())
         pOutput->push_back(currentCommit);

      return Success();
   }

   virtual core::Error show(const std::string& rev,
                            std::string* pOutput)
   {
      return runCommand(git() << "show" << "--pretty=oneline" << "-M" << rev,
                        pOutput, NULL);
   }
};

class SubversionVCSImpl : public VCSImpl
{
public:
   SubversionVCSImpl(FilePath rootDir) : VCSImpl(rootDir)
   {
   }

   VCS id() { return VCSSubversion; }
   std::string name() { return "SVN"; }

   core::Error status(const FilePath& dir, StatusResult* pStatusResult)
   {
      using namespace boost;

      std::vector<FileWithStatus> files;

      std::vector<std::string> lines;
      Error error = runCommand(
            svn() << "status" << "--non-interactive" << "--depth=immediates"
                  << "--" << dir,
            &lines);
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

         file.status = line.substr(0, 7);

         std::string filePath = line.substr(8);
         if (filePath.length() > 1 && filePath[filePath.length() - 1] == '/')
            filePath = filePath.substr(0, filePath.size() - 1);
         file.path = FilePath(string_utils::systemToUtf8(filePath));
         files.push_back(file);
      }

      *pStatusResult = StatusResult(files);

      return Success();
   }

   core::Error revert(const std::vector<FilePath>& filePaths,
                      std::string* pStdErr)
   {
      return runCommand(svn() << "revert" << "--" << filePaths,
                        NULL, pStdErr);
   }
};

FilePath resolveAliasedPath(std::string path)
{
   if (boost::algorithm::starts_with(path, "~/"))
      return module_context::resolveAliasedPath(path);
   else
      return s_pVcsImpl_->root().childPath(path);
}

std::vector<FilePath> resolveAliasedPaths(json::Array paths)
{
   std::vector<FilePath> parsedPaths;
   for (json::Array::iterator it = paths.begin();
        it != paths.end();
        it++)
   {
      json::Value value = *it;
      parsedPaths.push_back(resolveAliasedPath(value.get_str()));
   }
   return parsedPaths;
}

Error ensureSshAgentRunning()
{
   if (system::getenv("SSH_AUTH_SOCK").empty())
   {
      system::ProcessResult result;
      return system::runCommand(std::string("ssh-agent -s"),
                                system::ProcessOptions(),
                                &result);
   }
   return Success();
}

} // anonymous namespace


VCS activeVCS()
{
   return s_pVcsImpl_->id();
}

std::string activeVCSName()
{
   return s_pVcsImpl_->name();
}

VCSStatus StatusResult::getStatus(const FilePath& fileOrDirectory) const
{
   std::map<std::string, VCSStatus>::const_iterator found =
         this->filesByPath_.find(fileOrDirectory.absolutePath());
   if (found != this->filesByPath_.end())
      return found->second;

   return VCSStatus();
}

core::Error status(const FilePath& dir, StatusResult* pStatusResult)
{
   return s_pVcsImpl_->status(dir, pStatusResult);
}

Error fileStatus(const FilePath& filePath, VCSStatus* pStatus)
{
   StatusResult statusResult;
   Error error = source_control::status(filePath.parent(), &statusResult);
   if (error)
      return error;

   *pStatus = statusResult.getStatus(filePath);

   return Success();
}

namespace {

struct RefreshOnExit : public boost::noncopyable
{
   ~RefreshOnExit()
   {
      module_context::enqueClientEvent(ClientEvent(client_events::kVcsRefresh));
   }
};

Error vcsAdd(const json::JsonRpcRequest& request,
             json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   json::Array paths;
   Error error = json::readParam(request.params, 0, &paths);
   if (error)
      return error ;

   return s_pVcsImpl_->add(resolveAliasedPaths(paths), NULL);
}

Error vcsRemove(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   json::Array paths;
   Error error = json::readParam(request.params, 0, &paths);
   if (error)
      return error ;

   return s_pVcsImpl_->remove(resolveAliasedPaths(paths), NULL);
}

Error vcsDiscard(const json::JsonRpcRequest& request,
                 json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   json::Array paths;
   Error error = json::readParam(request.params, 0, &paths);
   if (error)
      return error ;

   return s_pVcsImpl_->discard(resolveAliasedPaths(paths), NULL);
}

Error vcsRevert(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   json::Array paths;
   Error error = json::readParam(request.params, 0, &paths);
   if (error)
      return error ;

   return s_pVcsImpl_->revert(resolveAliasedPaths(paths), NULL);
}

Error vcsStage(const json::JsonRpcRequest& request,
               json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   json::Array paths;
   Error error = json::readParam(request.params, 0, &paths);
   if (error)
      return error ;

   return s_pVcsImpl_->stage(resolveAliasedPaths(paths), NULL);
}

Error vcsUnstage(const json::JsonRpcRequest& request,
                 json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   json::Array paths;
   Error error = json::readParam(request.params, 0, &paths);
   if (error)
      return error ;

   return s_pVcsImpl_->unstage(resolveAliasedPaths(paths), NULL);
}

Error vcsListBranches(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   std::vector<std::string> branches;
   boost::optional<size_t> activeIndex;
   std::string stdErr;
   Error error = s_pVcsImpl_->listBranches(&branches, &activeIndex, &stdErr);
   if (error)
      return error;

   json::Array jsonBranches;
   std::transform(branches.begin(), branches.end(),
                  std::back_inserter(jsonBranches),
                  json::toJsonString);

   json::Object result;
   result["branches"] = jsonBranches;
   result["activeIndex"] =
         activeIndex
            ? json::Value(static_cast<boost::uint64_t>(activeIndex.get()))
            : json::Value();

   pResponse->setResult(result);

   return Success();
}

Error vcsCheckout(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   std::string id;
   Error error = json::readParams(request.params, &id);
   if (error)
      return error;

   std::string stdErr;
   error = s_pVcsImpl_->checkout(id, &stdErr);
   if (error)
      return error;

   return Success();
}

Error vcsFullStatus(const json::JsonRpcRequest&,
                    json::JsonRpcResponse* pResponse)
{
   StatusResult statusResult;
   Error error = s_pVcsImpl_->status(s_pVcsImpl_->root(), &statusResult);
   if (error)
      return error;

   std::vector<FileWithStatus> files = statusResult.files();
   json::Array result;
   for (std::vector<FileWithStatus>::const_iterator it = files.begin();
        it != files.end();
        it++)
   {
      VCSStatus status = it->status;
      FilePath path = it->path;
      json::Object obj;
      obj["status"] = status.status();
      obj["path"] = path.relativePath(s_pVcsImpl_->root());
      obj["raw_path"] = path.absolutePath();
      obj["discardable"] = status.status()[1] != ' ' && status.status()[1] != '?';
      result.push_back(obj);
   }

   pResponse->setResult(result);

   return Success();
}

Error vcsCommitGit(const json::JsonRpcRequest& request,
                   json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   GitVCSImpl* pGit = dynamic_cast<GitVCSImpl*>(s_pVcsImpl_.get());
   if (!pGit)
      return systemError(boost::system::errc::operation_not_supported, ERROR_LOCATION);

   std::string commitMsg;
   bool amend, signOff;
   Error error = json::readParams(request.params, &commitMsg, &amend, &signOff);
   if (error)
      return error;

   error = pGit->commit(commitMsg, amend, signOff);
   if (error)
      return error;

   return Success();
}

Error vcsPush(const json::JsonRpcRequest& request,
              json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   GitVCSImpl* pGit = dynamic_cast<GitVCSImpl*>(s_pVcsImpl_.get());
   if (!pGit)
      return systemError(boost::system::errc::operation_not_supported, ERROR_LOCATION);

   Error error = pGit->push(NULL, NULL);
   if (error)
      return error;

   return Success();
}

Error vcsPull(const json::JsonRpcRequest& request,
              json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   GitVCSImpl* pGit = dynamic_cast<GitVCSImpl*>(s_pVcsImpl_.get());
   if (!pGit)
      return systemError(boost::system::errc::operation_not_supported, ERROR_LOCATION);

   Error error = pGit->pull(NULL, NULL);
   if (error)
      return error;

   return Success();
}

Error vcsDiffFile(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   std::string path;
   int mode;
   int contextLines;
   Error error = json::readParams(request.params,
                                  &path,
                                  &mode,
                                  &contextLines);
   if (error)
      return error;

   if (contextLines < 0)
      contextLines = 999999999;

   std::string output;
   error = s_pVcsImpl_->diffFile(resolveAliasedPath(path),
                                 static_cast<PatchMode>(mode),
                                 contextLines,
                                 &output);
   if (error)
      return error;

   pResponse->setResult(output);
   return Success();
}

Error vcsApplyPatch(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   std::string patch;
   int mode;
   Error error = json::readParams(request.params, &patch, &mode);
   if (error)
      return error;

   FilePath patchFile = module_context::tempFile("rstudiovcs", "patch");
   error = writeStringToFile(patchFile, patch);
   if (error)
      return error;

   error = s_pVcsImpl_->applyPatch(patchFile, static_cast<PatchMode>(mode), NULL);

   Error error2 = patchFile.remove();
   if (error2)
      LOG_ERROR(error2);

   if (error)
      return error;

   return Success();
}

Error vcsHistory(const json::JsonRpcRequest& request,
                 json::JsonRpcResponse* pResponse)
{
   std::string rev;
   int maxentries;
   Error error = json::readParams(request.params, &rev, &maxentries);
   if (error)
      return error;

   std::vector<CommitInfo> commits;
   error = s_pVcsImpl_->log(rev, maxentries, &commits);
   if (error)
      return error;

   json::Array ids;
   json::Array authors;
   json::Array parents;
   json::Array subjects;
   json::Array dates;
   json::Array descriptions;

   for (std::vector<CommitInfo>::const_iterator it = commits.begin();
        it != commits.end();
        it++)
   {
      ids.push_back(it->id);
      authors.push_back(it->author);
      parents.push_back(it->parent);
      subjects.push_back(it->subject);
      descriptions.push_back(it->description);
      dates.push_back(static_cast<double>(it->date));
   }

   json::Object result;
   result["id"] = ids;
   result["author"] = authors;
   result["parent"] = parents;
   result["subject"] = subjects;
   result["description"] = descriptions;
   result["date"] = dates;

   pResponse->setResult(result);

   return Success();
}

void vcsExecuteCommand_onStdOut(boost::shared_ptr<std::string> ptrBuffer,
              const std::string& output)
{
   *ptrBuffer += output;
}

void vcsExecuteCommand_onExit(boost::shared_ptr<std::string> ptrBuffer,
            int exitCode,
            const json::JsonRpcFunctionContinuation& continuation)
{
   json::Object result;
   result["output"] = *ptrBuffer;
   result["error"] = exitCode;
   json::JsonRpcResponse response;
   response.setResult(result);

   continuation(Success(), &response);
}

void vcsExecuteCommand(const json::JsonRpcRequest& request,
                       const json::JsonRpcFunctionContinuation& continuation)
{
   RefreshOnExit refreshOnExit;

   std::string command;
   Error error = json::readParams(request.params, &command);
   if (error)
   {
      continuation(error, NULL);
      return;
   }

   command = shell_utils::sendStdErrToStdOut(shell_utils::join(
         ShellCommand("cd") << s_pVcsImpl_->root(),
         command));

   boost::shared_ptr<std::string> ptrBuffer(new std::string);
   system::ProcessCallbacks processCallbacks;
   processCallbacks.onStdout = boost::bind(vcsExecuteCommand_onStdOut,
                                           ptrBuffer, _2);
   processCallbacks.onExit = boost::bind(vcsExecuteCommand_onExit,
                                         ptrBuffer, _1, continuation);
   error = module_context::processSupervisor().runCommand(
         command, system::ProcessOptions(), processCallbacks);

   if (error)
   {
      continuation(error, NULL);
      return;
   }
}

Error vcsShow(const json::JsonRpcRequest& request,
              json::JsonRpcResponse* pResponse)
{
   std::string rev;
   Error error = json::readParams(request.params, &rev);
   if (error)
      return error;

   std::string output;
   s_pVcsImpl_->show(rev, &output);
   pResponse->setResult(output);

   return Success();
}

void postbackGitSSH(const std::string& command,
                    const module_context::PostbackHandlerContinuation& cont)
{
   cont(EXIT_SUCCESS, "");
}

typedef std::map<std::string, module_context::PostbackHandlerContinuation>
      AskPassContinuations;
AskPassContinuations  s_askpassContinuations;

void postbackSSHAskPass(const std::string& command,
                        const module_context::PostbackHandlerContinuation& cont)
{
   std::string handle = system::generateUuid();

   s_askpassContinuations.insert(std::make_pair(handle, cont));

   json::Object payload;
   payload["handle"] = handle;
   payload["prompt"] = std::string("Enter passphrase:");
   module_context::enqueClientEvent(
         ClientEvent(client_events::kAskPass, payload));
   // TODO: Need to do something to simulate initEvent
}

Error askpassReturn(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   std::string handle;
   bool success;
   std::string value;
   Error error = json::readParams(request.params, &handle, &success, &value);
   if (error)
      return error;

   AskPassContinuations::iterator pos = s_askpassContinuations.find(handle);
   if (pos != s_askpassContinuations.end())
   {
      if (success)
         pos->second(EXIT_SUCCESS, value);
      else
         pos->second(EXIT_FAILURE, "");
      return Success();
   }

   LOG_ERROR_MESSAGE("Unknown askpass handle " + handle);
   return Success();
}

} // anonymous namespace

core::Error initialize()
{
   FilePath workingDir = projects::projectContext().directory();
   if (!userSettings().vcsEnabled())
      s_pVcsImpl_.reset(new VCSImpl(workingDir));
   else if (workingDir.empty())
      s_pVcsImpl_.reset(new VCSImpl(workingDir));
   else if (!GitVCSImpl::detectGitDir(workingDir).empty())
      s_pVcsImpl_.reset(new GitVCSImpl(GitVCSImpl::detectGitDir(workingDir)));
   else if (workingDir.childPath(".svn").isDirectory())
      s_pVcsImpl_.reset(new SubversionVCSImpl(workingDir));
   else
      s_pVcsImpl_.reset(new VCSImpl(workingDir));

   std::string gitSshCmd;
   Error error = module_context::registerPostbackHandler("gitssh",
                                                         postbackGitSSH,
                                                         &gitSshCmd);
   if (error)
      return error;
   system::setenv("GIT_SSH", gitSshCmd);

   std::string sshAskCmd;
   error = module_context::registerPostbackHandler("askpass",
                                                   postbackSSHAskPass,
                                                   &sshAskCmd);
   if (error)
      return error;
   system::setenv("SSH_ASKPASS", sshAskCmd);
   system::setenv("GIT_ASKPASS", sshAskCmd);

   // install rpc methods
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "vcs_add", vcsAdd))
      (bind(registerRpcMethod, "vcs_remove", vcsRemove))
      (bind(registerRpcMethod, "vcs_discard", vcsDiscard))
      (bind(registerRpcMethod, "vcs_revert", vcsRevert))
      (bind(registerRpcMethod, "vcs_stage", vcsStage))
      (bind(registerRpcMethod, "vcs_unstage", vcsUnstage))
      (bind(registerRpcMethod, "vcs_list_branches", vcsListBranches))
      (bind(registerRpcMethod, "vcs_checkout", vcsCheckout))
      (bind(registerRpcMethod, "vcs_full_status", vcsFullStatus))
      (bind(registerRpcMethod, "vcs_commit_git", vcsCommitGit))
      (bind(registerRpcMethod, "vcs_push", vcsPush))
      (bind(registerRpcMethod, "vcs_pull", vcsPull))
      (bind(registerRpcMethod, "vcs_diff_file", vcsDiffFile))
      (bind(registerRpcMethod, "vcs_apply_patch", vcsApplyPatch))
      (bind(registerRpcMethod, "vcs_history", vcsHistory))
      (bind(registerAsyncRpcMethod, "vcs_execute_command", vcsExecuteCommand))
      (bind(registerRpcMethod, "vcs_show", vcsShow))
      (bind(registerRpcMethod, "askpass_return", askpassReturn));
   error = initBlock.execute();
   if (error)
      return error;

   return Success();
}

} // namespace source_control
} // namespace modules
} // namespace session
