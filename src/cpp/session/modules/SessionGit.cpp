/*
 * SessionGit.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
#include "SessionGit.hpp"

#include <signal.h>
#include <sys/stat.h>

#ifdef _WIN32
#include <windows.h>
#include <shlobj.h>
#include <shlwapi.h>
#endif

#ifndef _WIN32
# include <core/system/PosixNfs.hpp>
#endif

#include <boost/algorithm/string.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/split.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/foreach.hpp>
#include <boost/format.hpp>
#include <boost/function.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/optional.hpp>
#include <boost/regex.hpp>

#include <core/Algorithm.hpp>
#include <core/BoostLamda.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/system/Crypto.hpp>
#include <core/system/ShellUtils.hpp>
#include <core/system/System.hpp>
#include <core/system/Process.hpp>
#include <core/system/Environment.hpp>
#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/GitGraph.hpp>
#include <core/Scope.hpp>
#include <core/StringUtils.hpp>


#include <r/RExec.hpp>
#include <r/RUtil.hpp>

#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>
#include <session/SessionConsoleProcess.hpp>

#include "SessionAskPass.hpp"

#include "SessionVCS.hpp"

#include "vcs/SessionVCSCore.hpp"
#include "vcs/SessionVCSUtils.hpp"

#include "session-config.h"

using namespace rstudio::core;
using namespace rstudio::core::shell_utils;
using rstudio::session::console_process::ConsoleProcess;
using namespace rstudio::session::modules::vcs_utils;
using rstudio::session::modules::source_control::FileWithStatus;
using rstudio::session::modules::source_control::VCSStatus;
using rstudio::session::modules::source_control::StatusResult;

namespace rstudio {
namespace session {
namespace modules {
namespace git {

const char * const kVcsId = "Git";

namespace {

// git bin dir which we detect at startup. note that if the git bin
// is already in the path then this will be empty
std::vector<std::string> s_branches;
std::string s_gitExePath;
uint64_t s_gitVersion;
const uint64_t GIT_1_7_2 = ((uint64_t)1 << 48) |
                           ((uint64_t)7 << 32) |
                           ((uint64_t)2 << 16);

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

   // add git bin dir to PATH if necessary
   std::string nonPathGitBinDir = git::nonPathGitBinDir();
   if (!nonPathGitBinDir.empty())
      core::system::addToPath(&childEnv, nonPathGitBinDir);

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
   std::vector<std::string> refs;
   std::vector<std::string> tags;
   std::string graph;
};

struct RemoteBranchInfo
{
   RemoteBranchInfo() : commitsBehind(0) {}

   RemoteBranchInfo(const std::string& name, int commitsBehind)
      : name(name), commitsBehind(commitsBehind)
   {
   }

   bool empty() const { return name.empty(); }

   std::string name;
   int commitsBehind;

   json::Value toJson() const
   {
      if (!empty())
      {
         json::Object remoteInfoJson;
         remoteInfoJson["name"] = name;
         remoteInfoJson["commits_behind"] = commitsBehind;
         return remoteInfoJson;
      }
      else
      {
         return json::Value();
      }
   }
};

class Git;

std::vector<PidType> s_pidsToTerminate_;

ShellCommand git()
{
   if (!s_gitExePath.empty())
   {
      FilePath fullPath(s_gitExePath);
      return ShellCommand(fullPath);
   }
   else
      return ShellCommand("git");
}


#ifdef _WIN32
std::string gitBin()
{
   if (!s_gitExePath.empty())
   {
      return FilePath(s_gitExePath).absolutePathNative();
   }
   else
      return "git.exe";
}
#endif

bool waitForIndexLock(const FilePath& workingDir)
{
   using namespace boost::posix_time;
   
   // count the number of retries (avoid getting stuck in wait loops when
   // an index.lock file exists and is never cleaned up)
   static int retryCount = 0;
   
   FilePath lockPath = workingDir.childPath(".git/index.lock");
   
   // first stab attempt to see if the lockfile exists
   if (!lockPath.exists())
   {
      retryCount = 0;
      return true;
   }

#ifndef _WIN32
   // attempt to clear nfs cache -- don't log errors as this is done
   // just to ensure that we have a 'fresh' view of the index.lock file
   // in the later codepaths
   struct stat info;
   bool cleared;
   core::system::nfs::statWithCacheClear(lockPath, &cleared, &info);
#endif
   
   // otherwise, retry for 1s
   for (std::size_t i = 0; i < 5; ++i)
   {
      // if there's no lockfile, we can proceed
      if (!lockPath.exists())
      {
         retryCount = 0;
         return true;
      }
      
      // if there is a stale lockfile, then try cleaning it up
      // if we're able to remove a stale lockfile, then we can
      // escape early
      else
      {
         double diff = ::difftime(::time(NULL), lockPath.lastWriteTime());
         if (diff > 600)
         {
            Error error = lockPath.remove();
            if (!error)
            {
               retryCount = 0;
               return true;
            }
         }
      }
      
      // if we've tried too many times, just bail out (avoid stalling the
      // process on what seems to be a stale index.lock)
      if (retryCount > 100)
         break;

      // sleep for a bit, then retry
      boost::this_thread::sleep(milliseconds(200));
      ++retryCount;
   }
   
   return false;
}

Error gitExec(const ShellArgs& args,
              const core::FilePath& workingDir,
              core::system::ProcessResult* pResult)
{
   // if we see an 'index.lock' file within the associated
   // git repository, try waiting a bit until it's removed
   waitForIndexLock(workingDir);
   
   core::system::ProcessOptions options = procOptions();
   options.workingDir = workingDir;
   // Important to ensure SSH_ASKPASS works
#ifdef _WIN32
   options.detachProcess = true;
#endif

#ifdef _WIN32
      return runProgram(gitBin(),
                        args.args(),
                        "",
                        options,
                        pResult);
#else
      return runCommand(git() << args.args(),
                        "",
                        options,
                        pResult);
#endif
}

std::string gitText(const ShellArgs& args)
{
   std::stringstream ss;
   
   ss << ">>> ";
   
   if (s_gitExePath.empty())
      ss << "git ";
   else
      ss << s_gitExePath << " ";
   
   std::string arguments = core::algorithm::join(args, " ");
   ss << arguments << "\n";
   
   return ss.str();
}

bool commitIsMatch(const std::vector<std::string>& patterns,
                   const CommitInfo& commit)
{
   BOOST_FOREACH(std::string pattern, patterns)
   {
      if (!boost::algorithm::ifind_first(commit.author, pattern)
          && !boost::algorithm::ifind_first(commit.description, pattern)
          && !boost::algorithm::ifind_first(commit.id, pattern))
      {
         return false;
      }
   }

   return true;
}

boost::function<bool(CommitInfo)> createSearchTextPredicate(
      const std::string& searchText)
{
   if (searchText.empty())
      return boost::lambda::constant(true);

   std::vector<std::string> results;
   boost::algorithm::split(results, searchText,
                           boost::algorithm::is_any_of(" \t\r\n"));
   return boost::bind(commitIsMatch, results, _1);
}

bool isUntracked(const source_control::StatusResult& statusResult,
                 const FilePath& filePath)
{
   return statusResult.getStatus(filePath).status() == "??";
}

class Git : public boost::noncopyable
{
private:
   FilePath root_;

protected:
   core::Error runGit(const ShellArgs& args,
                      std::string* pStdOut=NULL,
                      std::string* pStdErr=NULL,
                      int* pExitCode=NULL)
   {
      using namespace rstudio::core::system;

      ProcessResult result;
      Error error = gitExec(args, root_, &result);
      if (error)
         return error;

      if (pStdOut)
         *pStdOut = result.stdOut;
      if (pStdErr)
         *pStdErr = result.stdErr;
      if (pExitCode)
         *pExitCode = result.exitStatus;

      if (result.exitStatus != EXIT_SUCCESS &&
          !result.stdErr.empty())
      {
         LOG_DEBUG_MESSAGE(result.stdErr);
      }

      return Success();
   }

   core::Error createConsoleProc(const ShellArgs& args,
                                 const std::string& caption,
                                 boost::shared_ptr<ConsoleProcess>* ppCP,
                                 const boost::optional<FilePath>& workingDir=boost::optional<FilePath>())
   {
      using namespace session::console_process;

      core::system::ProcessOptions options = procOptions();
#ifdef _WIN32
      options.detachProcess = true;
#endif
      if (!workingDir)
         options.workingDir = root_;
      else if (!workingDir.get().empty())
         options.workingDir = workingDir.get();

      boost::shared_ptr<ConsoleProcessInfo> pCPI =
            boost::make_shared<ConsoleProcessInfo>(caption,
                                                   console_process::InteractionNever);
      
#ifdef _WIN32
      *ppCP = ConsoleProcess::create(gitBin(), args.args(), options, pCPI);
#else
      *ppCP = ConsoleProcess::create(git() << args.args(), options, pCPI);
#endif
      
      (*ppCP)->enquePrompt(gitText(args));
      (*ppCP)->onExit().connect(boost::bind(&enqueueRefreshEvent));

      return Success();
   }

   std::vector<std::string> split(const std::string& str)
   {
      std::vector<std::string> output;
      boost::algorithm::split(output, str,
                              boost::algorithm::is_any_of("\r\n"));
      return output;
   }


   void appendPathArgs(const std::vector<FilePath>& filePaths,
                       ShellArgs* pArgs)
   {
      // On OSX we observed that staging and unstaging operations involving
      // directories that didn't exist would fail with an "unable to switch
      // to directory" message from git. We discovered this at the very
      // end of the v0.95 cycle so wanted to make as targeted a fix as
      // we could, so below we use git root relative paths whenever we can
      // on OSX, but on other platforms continue to use full absolute paths
#ifdef __APPLE__
      BOOST_FOREACH(const FilePath& filePath, filePaths)
      {
         if (filePath.isWithin(root_))
            *pArgs << filePath.relativePath(root_);
         else
            *pArgs << filePath;
      }
#else
      *pArgs << filePaths;
#endif
   }

public:

   Git() : root_(FilePath())
   {
   }

   Git(const FilePath& root) : root_(root)
   {
   }

   std::string name() { return kVcsId; }

   FilePath root() const
   {
      return root_;
   }

   void setRoot(const FilePath& path)
   {
      root_ = path;
   }

   core::Error status(const FilePath& dir,
                      StatusResult* pStatusResult)
   {
      using namespace boost;

      // objects to be populated from git's output
      std::vector<FileWithStatus> files;
      
      // build shell arguments
      ShellArgs arguments;
      
      // on some platforms, git will return paths which contain characters
      // not in the ASCII set with a so-called 'quoted octal encoding'.
      // this is controlled by the 'core.quotepath' configuration option;
      // by setting this to off we ensure that git will return us a
      // plain UTF-8 encoded path which requires no further processing
      arguments << "-c" << "core.quotepath=off"
                << "status" << "--porcelain" << "--" << dir;
      
      std::string output;
      Error error = runGit(arguments, &output);
      if (error)
         return error;
      
      // split and parse each line of status output
      std::vector<std::string> lines = split(output);

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

         // remove trailing slashes
         if (filePath.length() > 1 && filePath[filePath.length() - 1] == '/')
            filePath = filePath.substr(0, filePath.size() - 1);

         // file paths are returned as UTF-8 encoded paths,
         // so no need to re-encode here
         file.path = root_.childPath(filePath);

         files.push_back(file);
      }

      *pStatusResult = StatusResult(files);

      return Success();
   }

   core::Error add(const std::vector<FilePath>& filePaths)
   {
      return runGit(ShellArgs() << "add" << "--" << filePaths);
   }

   core::Error remove(const std::vector<FilePath>& filePaths)
   {
      ShellArgs args;
      args << "rm" << "--";
      appendPathArgs(filePaths, &args);
      return runGit(args);
   }

   core::Error discard(const std::vector<FilePath>& filePaths)
   {
      source_control::StatusResult statusResult;
      Error error = status(root_, &statusResult);
      if (error)
         return error;

      std::vector<FilePath> trackedPaths;
      std::remove_copy_if(filePaths.begin(),
                          filePaths.end(),
                          std::back_inserter(trackedPaths),
                          boost::bind(isUntracked, statusResult, _1));

      if (!trackedPaths.empty())
      {
         // -f means don't fail on unmerged entries
         return runGit(ShellArgs() << "checkout" << "-f" << "--" << trackedPaths);
      }
      else
      {
         return Success();
      }
   }

   core::Error stage(const std::vector<FilePath> &filePaths)
   {
      StatusResult statusResult;
      this->status(root_, &statusResult);

      std::vector<FilePath> filesToAdd;
      std::vector<FilePath> filesToRm;

      BOOST_FOREACH(const FilePath& path, filePaths)
      {
         std::string status = statusResult.getStatus(path).status();
         if (status.size() < 2)
         {
            // In the case of renames, getStatus(path) might not return
            // anything
            StatusResult individualStatusResult;
            this->status(path, &individualStatusResult);
            status = individualStatusResult.getStatus(path).status();
            if (status.size() < 2)
               continue;
         }
         if (status[1] == 'D')
            filesToRm.push_back(path);
         else if (status[1] != ' ')
            filesToAdd.push_back(path);
      }

      Error error;

      if (!filesToAdd.empty())
      {
         error = this->add(filesToAdd);
         if (error)
            return error;
      }

      if (!filesToRm.empty())
      {
         error = this->remove(filesToRm);
         if (error)
            return error;
      }

      return Success();
   }

   core::Error unstage(const std::vector<FilePath>& filePaths)
   {
      source_control::StatusResult statusResult;
      Error error = status(root_, &statusResult);
      if (error)
         return error;

      std::vector<FilePath> trackedPaths;
      std::remove_copy_if(filePaths.begin(),
                          filePaths.end(),
                          std::back_inserter(trackedPaths),
                          boost::bind(isUntracked, statusResult, _1));

      // Detect if HEAD does not exist (i.e. no commits in repo yet)
      int exitCode;
      error = runGit(ShellArgs() << "rev-parse" << "HEAD", NULL, NULL,
                     &exitCode);
      if (error)
         return error;

      ShellArgs args;
      if (exitCode == 0)
         args << "reset" << "HEAD" << "--" ;
      else
         args << "rm" << "--cached" << "--";

      if (!trackedPaths.empty())
      {
         appendPathArgs(trackedPaths, &args);
         return runGit(args);
      }
      else
      {
         return Success();
      }
   }
   
   core::Error createBranch(const std::string& branch,
                            boost::shared_ptr<ConsoleProcess>* ppCP)
   {
      return createConsoleProc(
               ShellArgs() << "checkout" << "-B" << branch,
               "Git Branch",
               ppCP);
   }

   core::Error listBranches(std::vector<std::string>* pBranches,
                            boost::optional<size_t>* pActiveBranchIndex)
   {
      std::vector<std::string> lines;

      std::string output;
      Error error = runGit(ShellArgs() << "branch" << "-a", &output);
      if (error)
         return error;
      lines = split(output);

      for (size_t i = 0; i < lines.size(); i++)
      {
         const std::string line = lines.at(i);
         if (line.size() < 2)
            break;

         if (boost::algorithm::starts_with(line, "* "))
            *pActiveBranchIndex = i;
         pBranches->push_back(line.substr(2));
      }

      s_branches = *pBranches;
      return Success();
   }
   
   core::Error listRemotes(json::Array* pRemotes)
   {
      std::string output;
      Error error = runGit(ShellArgs() << "remote" << "--verbose", &output);
      if (error)
         return error;
      
      std::string trimmed = string_utils::trimWhitespace(output);
      if (trimmed.empty())
         return Success();
      
      boost::regex reSpaces("\\s+");
      std::vector<std::string> splat = split(trimmed);
      BOOST_FOREACH(const std::string& line, splat)
      {
         boost::smatch match;
         if (!regex_utils::search(line, match, reSpaces))
            continue;
         
         std::string remote = std::string(line.begin(), match[0].first);
         std::string url = std::string(match[0].second, line.end());
         std::string type = "(unknown)";
         
         if (boost::algorithm::ends_with(url, "(fetch)"))
         {
            url = url.substr(0, url.size() - strlen("(fetch)"));
            type = "fetch";
         }
         else if (boost::algorithm::ends_with(url, "(push)"))
         {
            url = url.substr(0, url.size() - strlen("(push)"));
            type = "push";
         }
         
         json::Object objectJson;
         objectJson["remote"] = string_utils::trimWhitespace(remote);
         objectJson["url"] = string_utils::trimWhitespace(url);
         objectJson["type"] = string_utils::trimWhitespace(type);
         pRemotes->push_back(objectJson);
      }

      return Success();
   }
   
   core::Error addRemote(const std::string& name,
                         const std::string& url)
   {
      return runGit(ShellArgs() << "remote" << "add" << name << url);
   }

   core::Error checkout(const std::string& id,
                        boost::shared_ptr<ConsoleProcess>* ppCP)
   {
      ShellArgs args;
      if (id.find("remotes/") == 0)
      {
         std::vector<std::string> splat = core::algorithm::split(id, "/");
         if (splat.size() > 2)
         {
            std::string localBranch = core::algorithm::join(splat.begin() + 2, splat.end(), "/");
            std::string remoteBranch = core::algorithm::join(splat.begin() + 1, splat.end(), "/");
            
            // if we don't have a local copy of this branch, then
            // check out a local copy of branch, tracking remote;
            // otherwise just check out our local copy
            if (core::algorithm::contains(s_branches, localBranch))
            {
               args << "checkout" << localBranch;
            }
            else
            {
               args << "checkout" << "-b" << localBranch << remoteBranch;
            }
         }
         else
         {
            // shouldn't happen, but provide valid shell command regardless
            args << "checkout" << id;
         }
      }
      else
      {
         args << "checkout" << id;
      }
      
      return createConsoleProc(args,
                               "Git Checkout " + id,
                               ppCP);
   }
   
   core::Error checkoutRemote(const std::string& branch,
                              const std::string& remote,
                              boost::shared_ptr<ConsoleProcess>* ppCP)
   {
      std::string localBranch  = branch;
      std::string remoteBranch = remote + "/" + branch;
      
      return createConsoleProc(
               ShellArgs() << "checkout" << "-b" << localBranch << remoteBranch,
               "Git Checkout " + branch,
               ppCP);
   }

   core::Error commit(std::string message,
                      bool amend,
                      bool signOff,
                      boost::shared_ptr<ConsoleProcess>* ppCP)
   {
      using namespace string_utils;
      
      // detect the active commit encoding for this project
      std::string encoding;
      int exitCode;
      Error error = runGit(ShellArgs() << "config" << "i18n.commitencoding",
                           &encoding,
                           NULL,
                           &exitCode);
      
      // normalize output (no config specified implies UTF-8 default)
      encoding = toUpper(trimWhitespace(encoding));
      if (encoding.empty())
         encoding = "UTF-8";
      
      // convert from UTF-8 to user encoding if required
      if (encoding != "UTF-8")
      {
         error = r::util::iconvstr(message, "UTF-8", encoding, false, &message);
         if (error)
         {
            return systemError(
                     boost::system::errc::illegal_byte_sequence,
                     "The commit message could not be encoded to " + encoding +
                     ".\n\n You can correct this by calling "
                     "'git config i18n.commitencoding UTF-8' "
                     "and committing again.",
                     ERROR_LOCATION);
         }
      }

      // write commit message to file
      FilePath tempFile = module_context::tempFile("git-commit-message-", "txt");
      boost::shared_ptr<std::ostream> pStream;

      error = tempFile.open_w(&pStream);
      if (error)
         return error;

      *pStream << message;

      // append merge commit message when appropriate
      FilePath gitDir = root_.childPath(".git");
      if (gitDir.childPath("MERGE_HEAD").exists())
      {
         FilePath mergeMsg = gitDir.childPath("MERGE_MSG");
         if (mergeMsg.exists())
         {
            std::string mergeMsgStr;
            error = core::readStringFromFile(mergeMsg, &mergeMsgStr);
            if (!error)
            {
               if (!message.empty())
                  *pStream << std::endl << std::endl;
               *pStream << mergeMsgStr;
            }
         }
      }

      pStream->flush();
      pStream.reset();  // release file handle

      // override a user-specified default encoding if necessary
      ShellArgs args;
      if (encoding != "UTF-8")
         args << "-c" << "i18n.commitencoding=utf-8";
      args << "commit" << "-F" << tempFile;

      if (amend)
         args << "--amend";
      if (signOff)
         args << "--signoff";

      return createConsoleProc(args,
                               "Git Commit",
                               ppCP);
   }

   core::Error clone(const std::string& url,
                     const std::string dirName,
                     const FilePath& parentPath,
                     boost::shared_ptr<ConsoleProcess>* ppCP)
   {
      // SPECIAL: happens in different working directory than usual

      return
            createConsoleProc(ShellArgs() << "clone" << "--progress" << url << dirName,
                              "Clone Repository",
                              ppCP,
                              boost::optional<FilePath>(parentPath));
   }

   Error currentBranch(std::string* pBranch)
   {
      std::vector<std::string> branches;
      boost::optional<size_t> index;
      Error error = listBranches(&branches, &index);
      if (error)
         return error;
      if (!index)
      {
         pBranch->clear();
         return Success();
      }

      *pBranch = branches.at(*index);
      if (*pBranch == "(no branch)")
      {
         pBranch->clear();
      }
      return Success();
   }

   bool remoteMerge(const std::string& branch,
                    std::string* pRemote,
                    std::string* pMerge)
   {
      int exitStatus;
      Error error = runGit(ShellArgs() << "config" << "--get" << "branch." + branch + ".remote",
                           pRemote,
                           NULL,
                           &exitStatus);
      if (error)
      {
         LOG_ERROR(error);
         return false;
      }
      if (exitStatus != EXIT_SUCCESS)
         return false;
      boost::algorithm::trim(*pRemote);

      error = runGit(ShellArgs() << "config" << "--get" << "branch." + branch + ".merge",
                     pMerge,
                     NULL,
                     &exitStatus);
      if (error)
      {
         LOG_ERROR(error);
         return false;
      }
      if (exitStatus != EXIT_SUCCESS)
         return false;
      boost::algorithm::trim(*pMerge);

      return true;
   }

   core::Error push(boost::shared_ptr<ConsoleProcess>* ppCP)
   {
      std::string branch;
      Error error = currentBranch(&branch);
      if (error)
         return error;

      ShellArgs args = ShellArgs() << "push";

      std::string remote, merge;
      if (remoteMerge(branch, &remote, &merge))
      {
         args << remote << merge;
      }

      return createConsoleProc(args, "Git Push", ppCP);
   }
   
   core::Error pushBranch(const std::string& branch,
                          const std::string& remote,
                          boost::shared_ptr<ConsoleProcess>* ppCP)
   {
      return createConsoleProc(
               ShellArgs() << "push" << "-u" << remote << branch,
               "Git Push",
               ppCP);
   }

   core::Error pull(boost::shared_ptr<ConsoleProcess>* ppCP)
   {
      return createConsoleProc(ShellArgs() << "pull",
                               "Git Pull", ppCP);
   }

   core::Error doDiffFile(const FilePath& filePath,
                          const FilePath* pCompareTo,
                          PatchMode mode,
                          int contextLines,
                          std::string* pOutput)
   {
      ShellArgs args = ShellArgs() << "diff";
      args << "-U" + safe_convert::numberToString(contextLines);
      if (mode == PatchModeStage)
         args << "--cached";
      args << "--";
      if (pCompareTo)
         args << *pCompareTo;
      args << filePath;

      return runGit(args, pOutput, NULL, NULL);
   }

   core::Error diffFile(const FilePath& filePath,
                        PatchMode mode,
                        int contextLines,
                        std::string* pOutput)
   {
      Error error = doDiffFile(filePath, NULL, mode, contextLines, pOutput);
      if (error)
         return error;

      if (pOutput->empty())
      {
         // detect add case
         VCSStatus status;
         error = git::fileStatus(filePath, &status);
         if (error)
            return error;
         if (status.status() == "??" && mode == PatchModeWorking)
         {
            error = doDiffFile(filePath,
                               &(shell_utils::devnull()),
                               mode,
                               contextLines,
                               pOutput);
            if (error)
               return error;
         }
      }

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
                          PatchMode patchMode)
   {
      ShellArgs args = ShellArgs() << "apply";
      if (patchMode == PatchModeStage)
         args << "--cached";
      args << "--";
      args << patchFile;

      return runGit(args);
   }

   void parseCommitValue(const std::string& value,
                         CommitInfo* pCommitInfo)
   {
      static boost::regex commitRegex("^([a-z0-9]+)(\\s+\\((.*)\\))?");
      boost::smatch smatch;
      if (regex_utils::match(value, smatch, commitRegex))
      {
         pCommitInfo->id = smatch[1];
         std::vector<std::string> refs;
         if (smatch[3].matched)
         {
            boost::algorithm::split(refs,
                                    static_cast<const std::string>(smatch[3]),
                                    boost::algorithm::is_any_of(","));
            BOOST_FOREACH(std::string ref, refs)
            {
               boost::algorithm::trim(ref);
               if (boost::algorithm::starts_with(ref, "tag: "))
                  pCommitInfo->tags.push_back(ref.substr(5));
               else if (boost::algorithm::starts_with(ref, "refs/tags/"))
               {
                  // Sometimes with git 1.7.0 tags appear without the "tags: "
                  // prefix, e.g. plyr-1.6
                  pCommitInfo->tags.push_back(ref);
               }
               else if (!boost::algorithm::starts_with(ref, "refs/bisect/"))
                  pCommitInfo->refs.push_back(ref);
            }
         }
      }
      else
      {
         pCommitInfo->id = value;
      }
   }

   core::Error logLength(const std::string &rev,
                         const FilePath& fileFilter,
                         const std::string &searchText,
                         int *pLength)
   {
      if (searchText.empty())
      {
         ShellArgs args = ShellArgs() << "log";
         args << "--pretty=oneline";
         if (!rev.empty())
            args << rev;

         if (!fileFilter.empty())
            args << "--" << fileFilter;

         std::string output;
         Error error = runGit(args, &output);
         if (error)
            return error;

         *pLength = static_cast<int>(std::count(output.begin(), output.end(), '\n'));
         return Success();
      }
      else
      {
         std::vector<CommitInfo> output;
         Error error = log(rev, fileFilter, 0, -1, searchText, &output);
         if (error)
            return error;
         *pLength = output.size();
         return Success();
      }
   }

   core::Error log(const std::string& rev,
                   const FilePath& fileFilter,
                   int skip,
                   int maxentries,
                   const std::string& searchText,
                   std::vector<CommitInfo>* pOutput)
   {
      ShellArgs args = ShellArgs() << "log" << "--encoding=UTF-8"
                       << "--pretty=raw" << "--decorate=full"
                       << "--date-order";

      ShellArgs revListArgs = ShellArgs() << "rev-list" << "--date-order" << "--parents";
      int revListSkip = skip;

      if (!fileFilter.empty())
      {
         args << "--" << fileFilter;
         revListArgs << "--" << fileFilter;
      }

      if (searchText.empty() && fileFilter.empty())
      {
         // This is a way more efficient way to implement skip and maxentries
         // if we know that all commits are included.
         if (skip > 0)
         {
            args << "--skip=" + safe_convert::numberToString(skip);
            skip = 0;
         }
         if (maxentries >= 0)
         {
            args << "--max-count=" + safe_convert::numberToString(maxentries);
            maxentries = -1;

            revListArgs << "--max-count=" + safe_convert::numberToString(
                  (skip < 0 ? 0 : skip) + maxentries);
         }
      }

      if (!rev.empty())
      {
         args << rev;
         revListArgs << rev;
      }
      else
      {
         revListArgs << "HEAD";
      }

      if (maxentries < 0)
         maxentries = std::numeric_limits<int>::max();

      std::vector<std::string> outLines;
      std::string output;
      Error error = runGit(args, &output);
      if (error)
         return error;
      outLines = split(output);
      output.clear();

      std::vector<std::string> graphLines;
      if (searchText.empty() && fileFilter.empty())
      {
         std::vector<std::string> revOutLines;
         std::string revOutput;
         error = runGit(revListArgs, &revOutput);
         if (error)
            return error;
         revOutLines = split(revOutput);
         revOutput.clear();

         gitgraph::GitGraph graph;
         for (size_t i = 0; i < revOutLines.size(); i++)
         {
            typedef std::vector<std::string> find_vector_type;
            find_vector_type parents;
            boost::algorithm::split(parents, revOutLines[i],
                                    boost::algorithm::is_any_of(" "));
            if (parents.size() < 1)
               break;

            std::string commit = parents.front();
            parents.erase(parents.begin());

            gitgraph::Line line = graph.addCommit(commit, parents);
            if (revListSkip <= 0 || static_cast<int>(i) >= revListSkip)
               graphLines.push_back(line.string());
         }
      }

      boost::function<bool(CommitInfo)> filter = createSearchTextPredicate(searchText);

      boost::regex kvregex("^(\\w+) (.*)$");
      boost::regex authTimeRegex("^(.*?) (\\d+) ([+\\-]?\\d+)$");

      size_t graphLineIndex = 0;
      int skipped = 0;
      CommitInfo currentCommit;

      for (std::vector<std::string>::const_iterator it = outLines.begin();
           it != outLines.end() && pOutput->size() < static_cast<size_t>(maxentries);
           it++)
      {
         boost::smatch smatch;
         if (regex_utils::search(*it, smatch, kvregex))
         {
            std::string key = smatch[1];
            std::string value = smatch[2];
            if (key == "commit")
            {
               if (!currentCommit.id.empty() && filter(currentCommit))
               {
                  if (skipped < skip)
                     skipped++;
                  else
                  {
                     if (graphLineIndex < graphLines.size())
                        currentCommit.graph = graphLines[graphLineIndex];
                     pOutput->push_back(currentCommit);
                  }

                  graphLineIndex++;
               }

               currentCommit = CommitInfo();
               parseCommitValue(value, &currentCommit);
            }
            else if (key == "author" || key == "committer")
            {
               boost::smatch authTimeMatch;
               if (regex_utils::search(value, authTimeMatch, authTimeRegex))
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

      if (pOutput->size() < static_cast<size_t>(maxentries)
          && !currentCommit.id.empty()
          && filter(currentCommit))
      {
         if (skipped < skip)
            skipped++;
         else
         {
            if (graphLineIndex < graphLines.size())
               currentCommit.graph = graphLines[graphLineIndex];
            pOutput->push_back(currentCommit);
         }
         graphLineIndex++;
      }

      return Success();
   }

   virtual core::Error show(const std::string& rev,
                            std::string* pOutput)
   {
      ShellArgs args = ShellArgs() << "show" << "--pretty=oneline" << "-M";
      if (s_gitVersion >= GIT_1_7_2)
         args << "-c";
      args << rev;

      return runGit(args, pOutput);
   }

   virtual core::Error showFile(const std::string& rev,
                                const std::string& filename,
                                std::string* pOutput)
   {
      boost::format fmt("%1%:%2%");
      ShellArgs args =
            ShellArgs() << "show" << boost::str(fmt % rev % filename);

      return runGit(args, pOutput);
   }

   virtual core::Error remoteBranchInfo(RemoteBranchInfo* pRemoteBranchInfo)
   {
      // default to none
      *pRemoteBranchInfo = RemoteBranchInfo();

      std::string branch;
      Error error = currentBranch(&branch);
      if (error)
         return error;

      if (branch.empty())
         return Success();

      std::string remote, merge;
      if (remoteMerge(branch, &remote, &merge))
      {
         // branch name is simple concatenation
         std::string name = remote + "/" + branch;

         // list the commits between the current upstream and HEAD
         ShellArgs args = ShellArgs() << "log" << "@{u}..HEAD" << "--oneline";
         std::string output;
         Error error = runGit(args, &output);
         if (error)
            return error;

         // commits == number of lines (since we used --oneline mode)
         int commitsBehind = safe_convert::numberTo<int>(
                        std::count(output.begin(), output.end(), '\n'), 0);

         *pRemoteBranchInfo = RemoteBranchInfo(name, commitsBehind);
      }

      return Success();
   }
};

Git s_git_;

FilePath resolveAliasedPath(const std::string& path)
{
   if (boost::algorithm::starts_with(path, "~/"))
      return module_context::resolveAliasedPath(path);
   else
      return s_git_.root().childPath(path);
}

bool splitRename(const std::string& path, std::string* pOld, std::string* pNew)
{
   const std::string RENAME(" -> ");

   size_t index = path.find(RENAME);
   if (index == path.npos)
      return false;

   if (pOld)
      *pOld = std::string(path.begin(), path.begin() + index);
   if (pNew)
      *pNew = std::string(path.begin() + index + RENAME.size(), path.end());

   return true;
}

std::vector<FilePath> resolveAliasedPaths(const json::Array& paths,
                                          bool includeRenameOld = false,
                                          bool includeRenameNew = true)
{
   std::vector<FilePath> results;
   for (json::Array::const_iterator it = paths.begin();
        it != paths.end();
        it++)
   {
      std::string oldPath, newPath;
      if (splitRename(it->get_str(), &oldPath, &newPath))
      {
         if (includeRenameOld)
            results.push_back(resolveAliasedPath(oldPath));
         if (includeRenameNew)
            results.push_back(resolveAliasedPath(newPath));
      }
      else
      {
         results.push_back(resolveAliasedPath(it->get_str()));
      }
   }
   return results;
}

FilePath detectGitDir(const FilePath& workingDir)
{
   core::system::ProcessOptions options = procOptions();
   options.workingDir = workingDir;
#ifndef _WIN32
   options.detachSession = true;
#endif

   core::system::ProcessResult result;
   Error error = core::system::runCommand(
            git() << "rev-parse" << "--show-toplevel",
            "",
            options,
            &result);

   if (error)
      return FilePath();

   if (result.exitStatus != 0)
      return FilePath();

   return FilePath(boost::algorithm::trim_copy(
                      string_utils::systemToUtf8(result.stdOut)));
}

} // anonymous namespace

GitFileDecorationContext::GitFileDecorationContext(const FilePath& rootDir)
   : fullRefreshRequired_(false)
{
   // get source control status (merely log errors doing this)
   Error error = git::status(rootDir, &vcsStatus_);
   if (error)
      LOG_ERROR(error);
}

GitFileDecorationContext::~GitFileDecorationContext()
{
   if (fullRefreshRequired_)
      enqueueRefreshEvent();
}

void GitFileDecorationContext::decorateFile(const FilePath &filePath,
                                            json::Object *pFileObject)
{
   VCSStatus status = vcsStatus_.getStatus(filePath);

   if (status.status().empty() && !fullRefreshRequired_)
   {
      // Special edge case when file is inside an untracked directory
      // that may or may not be known to the client. (It wouldn't be
      // known if the directory was empty until this file event.)

      FilePath parent = filePath;
      while (true)
      {
         if (parent == parent.parent())
            break;

         parent = parent.parent();
         if (vcsStatus_.getStatus(parent).status() == "??")
         {
            fullRefreshRequired_ = true;
            break;
         }
      }
   }

   json::Object vcsObj;
   Error error = statusToJson(filePath, status, &vcsObj);
   if (error)
      LOG_ERROR(error);
   (*pFileObject)["git_status"] = vcsObj;
}

core::Error status(const FilePath& dir, StatusResult* pStatusResult)
{
   if (s_git_.root().empty())
      return Success();

   return s_git_.status(dir, pStatusResult);
}

Error fileStatus(const FilePath& filePath, VCSStatus* pStatus)
{
   StatusResult statusResult;
   Error error = git::status(filePath.parent(), &statusResult);
   if (error)
      return error;

   *pStatus = statusResult.getStatus(filePath);

   return Success();
}

namespace {

Error vcsAdd(const json::JsonRpcRequest& request,
             json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   json::Array paths;
   Error error = json::readParam(request.params, 0, &paths);
   if (error)
      return error ;

   return s_git_.add(resolveAliasedPaths(paths));
}

Error vcsRemove(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   json::Array paths;
   Error error = json::readParam(request.params, 0, &paths);
   if (error)
      return error ;

   return s_git_.remove(resolveAliasedPaths(paths));
}

Error vcsDiscard(const json::JsonRpcRequest& request,
                 json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   json::Array paths;
   Error error = json::readParam(request.params, 0, &paths);
   if (error)
      return error ;

   return s_git_.discard(resolveAliasedPaths(paths));
}

Error vcsRevert(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   json::Array paths;
   Error error = json::readParam(request.params, 0, &paths);
   if (error)
      return error ;

   error = s_git_.unstage(resolveAliasedPaths(paths, true, true));
   if (error)
      LOG_ERROR(error);
   error = s_git_.discard(resolveAliasedPaths(paths, true, false));
   if (error)
      LOG_ERROR(error);

   return Success();
}

Error vcsStage(const json::JsonRpcRequest& request,
               json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   json::Array paths;
   Error error = json::readParam(request.params, 0, &paths);
   if (error)
      return error ;

   return s_git_.stage(resolveAliasedPaths(paths));
}

Error vcsUnstage(const json::JsonRpcRequest& request,
                 json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   json::Array paths;
   Error error = json::readParam(request.params, 0, &paths);
   if (error)
      return error ;

   return s_git_.unstage(resolveAliasedPaths(paths, true, true));
}

Error vcsCreateBranch(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   Error error;
   std::string branch;
   
   error = json::readParams(request.params, &branch);
   if (error)
      return error;
   
   boost::shared_ptr<ConsoleProcess> pCP;
   error = s_git_.createBranch(branch, &pCP);
   if (error)
      return error;
   
   pResponse->setResult(pCP->toJson());

   return Success();
}

Error vcsListBranches(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   std::vector<std::string> branches;
   boost::optional<size_t> activeIndex;
   Error error = s_git_.listBranches(&branches, &activeIndex);
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

   boost::shared_ptr<ConsoleProcess> pCP;
   error = s_git_.checkout(id, &pCP);
   if (error)
      return error;

   pResponse->setResult(pCP->toJson());

   return Success();
}

Error vcsCheckoutRemote(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   RefreshOnExit scope;
   
   std::string branch, remote;
   Error error = json::readParams(request.params, &branch, &remote);
   if (error)
      return error;
   
   boost::shared_ptr<ConsoleProcess> pCP;
   error = s_git_.checkoutRemote(branch, remote, &pCP);
   if (error)
      return error;
   
   pResponse->setResult(pCP->toJson());
   
   return Success();
}


Error vcsFullStatus(const json::JsonRpcRequest&,
                    json::JsonRpcResponse* pResponse)
{
   StatusResult statusResult;
   Error error = s_git_.status(s_git_.root(), &statusResult);
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
      error = statusToJson(path, status, &obj);
      if (error)
         return error;
      result.push_back(obj);
   }

   pResponse->setResult(result);

   return Success();
}

Error vcsAllStatus(const json::JsonRpcRequest& request,
                   json::JsonRpcResponse* pResponse)
{
   json::Object result;
   json::JsonRpcResponse tmp;

   Error error = vcsFullStatus(request, &tmp);
   if (error)
      return error;
   result["status"] = tmp.result();

   error = vcsListBranches(request, &tmp);
   if (error)
      return error;
   result["branches"] = tmp.result();

   RemoteBranchInfo remoteBranchInfo;
   error = s_git_.remoteBranchInfo(&remoteBranchInfo);
   if (error)
      return error;
   result["remote_branch_info"] = remoteBranchInfo.toJson();

   pResponse->setResult(result);

   return Success();
}

Error vcsCommit(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   std::string commitMsg;
   bool amend, signOff;
   Error error = json::readParams(request.params, &commitMsg, &amend, &signOff);
   if (error)
      return error;

   boost::shared_ptr<ConsoleProcess> pCP;
   error = s_git_.commit(commitMsg, amend, signOff, &pCP);
   if (error)
   {
      if (error.code() == boost::system::errc::illegal_byte_sequence)
      {
         pResponse->setError(error, error.getProperty("description"));
         return Success();
      }

      return error;
   }

   pResponse->setResult(pCP->toJson());

   return Success();
}

Error vcsPush(const json::JsonRpcRequest& request,
              json::JsonRpcResponse* pResponse)
{
   boost::shared_ptr<ConsoleProcess> pCP;
   Error error = s_git_.push(&pCP);
   if (error)
      return error;

   ask_pass::setActiveWindow(request.sourceWindow);

   pResponse->setResult(pCP->toJson());

   return Success();
}

Error vcsPushBranch(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   std::string branch, remote;
   Error error = json::readParams(request.params, &branch, &remote);
   if (error)
      return error;
   
   boost::shared_ptr<ConsoleProcess> pCP;
   error = s_git_.pushBranch(branch, remote, &pCP);
   if (error)
      return error;

   ask_pass::setActiveWindow(request.sourceWindow);

   pResponse->setResult(pCP->toJson());

   return Success();
}

Error vcsPull(const json::JsonRpcRequest& request,
              json::JsonRpcResponse* pResponse)
{
   boost::shared_ptr<ConsoleProcess> pCP;
   Error error = s_git_.pull(&pCP);
   if (error)
      return error;

   ask_pass::setActiveWindow(request.sourceWindow);

   pResponse->setResult(pCP->toJson());

   return Success();
}

Error vcsDiffFile(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   std::string path;
   int mode;
   int contextLines;
   bool noSizeWarning;
   Error error = json::readParams(request.params,
                                  &path,
                                  &mode,
                                  &contextLines,
                                  &noSizeWarning);
   if (error)
      return error;

   if (contextLines < 0)
      contextLines = 999999999;

   splitRename(path, NULL, &path);

   std::string output;
   error = s_git_.diffFile(resolveAliasedPath(path),
                                 static_cast<PatchMode>(mode),
                                 contextLines,
                                 &output);
   if (error)
      return error;

   std::string sourceEncoding = projects::projectContext().defaultEncoding();
   bool usedSourceEncoding;
   output = convertDiff(output, sourceEncoding, "UTF-8", false,
                        &usedSourceEncoding);
   if (!usedSourceEncoding)
      sourceEncoding = "";

   if (!noSizeWarning && output.size() > source_control::WARN_SIZE)
   {
      error = systemError(boost::system::errc::file_too_large,
                          ERROR_LOCATION);
      pResponse->setError(error,
                          json::Value(static_cast<boost::uint64_t>(output.size())));
   }
   else
   {
      json::Object result;
      result["source_encoding"] = sourceEncoding;
      result["decoded_value"] = output;
      pResponse->setResult(result);
   }
   return Success();
}

Error vcsApplyPatch(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   std::string patch;
   int mode;
   std::string sourceEncoding;
   Error error = json::readParams(request.params, &patch, &mode, &sourceEncoding);
   if (error)
      return error;

   bool converted;
   patch = convertDiff(patch, "UTF-8", sourceEncoding, false, &converted);
   if (!converted)
      return systemError(boost::system::errc::illegal_byte_sequence, ERROR_LOCATION);

   FilePath patchFile = module_context::tempFile("rstudiovcs", "patch");
   error = writeStringToFile(patchFile, patch);
   if (error)
      return error;

   error = s_git_.applyPatch(patchFile, static_cast<PatchMode>(mode));

   Error error2 = patchFile.remove();
   if (error2)
      LOG_ERROR(error2);

   if (error)
      return error;

   return Success();
}

Error vcsGetIgnores(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   std::string path;
   Error error = json::readParam(request.params, 0, &path);
   if (error)
      return error;

   // resolve path
   FilePath filePath = module_context::resolveAliasedPath(path);
   FilePath gitIgnorePath = filePath.complete(".gitignore");

   // setup result (default to empty)
   core::system::ProcessResult result;
   result.exitStatus = EXIT_SUCCESS;
   result.stdOut = "";

   // read the file if it exists
   if (gitIgnorePath.exists())
   {
      Error error = core::readStringFromFile(gitIgnorePath,
                                             &result.stdOut,
                                             string_utils::LineEndingPosix);
      if (error)
         return error;
   }

   // return contents
   pResponse->setResult(processResultToJson(result));
   return Success();
}

Error vcsSetIgnores(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   // get the params
   std::string path, ignores;
   Error error = json::readParams(request.params, &path, &ignores);
   if (error)
      return error;

   // resolve path
   FilePath filePath = module_context::resolveAliasedPath(path);
   FilePath gitIgnorePath = filePath.complete(".gitignore");

   // write the .gitignore file
   error = core::writeStringToFile(gitIgnorePath,
                                   ignores,
                                   string_utils::LineEndingNative);
   if (error)
      return error;

   // always return an empty (successful) ProcessResult
   core::system::ProcessResult result;
   result.exitStatus = EXIT_SUCCESS;
   pResponse->setResult(processResultToJson(result));
   return Success();
}

Error vcsListRemotes(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   pResponse->setResult(json::Array());
   
   json::Array remotesJson;
   Error error = s_git_.listRemotes(&remotesJson);
   if (error)
      LOG_ERROR(error);
   
   pResponse->setResult(remotesJson);
   return Success();
}

Error vcsAddRemote(const json::JsonRpcRequest& request,
                   json::JsonRpcResponse* pResponse)
{
   std::string name, url;
   Error error = json::readParams(request.params, &name, &url);
   if (error)
      return error;
   
   error = s_git_.addRemote(name, url);
   if (error)
      return error;
   
   return vcsListRemotes(request, pResponse);
}

std::string getUpstream(const std::string& branch = std::string())
{
   // determine the query (no explicit branch means current branch)
   std::string query = "@{upstream}";
   if (!branch.empty())
      query = branch + query;

   // get the upstream
   std::string upstream;
   core::system::ProcessResult result;
   Error error = gitExec(ShellArgs() <<
                           "rev-parse" << "--abbrev-ref" << query,
                         s_git_.root(),
                         &result);
   if (error)
   {
      LOG_ERROR(error);
      return std::string();
   }
   else if (result.exitStatus == EXIT_SUCCESS)
   {
      upstream = boost::algorithm::trim_copy(result.stdOut);
   }

   return upstream;
}

std::string githubUrl(const std::string& view,
                      const FilePath& filePath = FilePath())
{
   if (!isGitEnabled())
      return std::string();

   // get the upstream for the current branch
   std::string upstream = getUpstream();

   // if there is none then get the upstream for master
   if (upstream.empty())
      upstream = getUpstream("master");

   // if there still isn't one then fall back to origin/master
   if (upstream.empty())
      upstream = "origin/master";

   // parse out the upstream name and branch
   std::string::size_type pos = upstream.find_first_of('/');
   if (pos == std::string::npos)
   {
      LOG_ERROR_MESSAGE("No / in upstream name: " + upstream);
      return std::string();
   }
   std::string upstreamName = upstream.substr(0, pos);
   std::string upstreamBranch = upstream.substr(pos + 1);

   // now get the remote url
   core::system::ProcessResult result;
   Error error = gitExec(ShellArgs() <<
                   "config" << "--get" << ("remote." + upstreamName + ".url"),
                   s_git_.root(),
                   &result);
   if (error)
   {
      LOG_ERROR(error);
      return std::string();
   }
   else if (result.exitStatus != 0)
   {
      if (!result.stdErr.empty())
         LOG_ERROR_MESSAGE(result.stdErr);
      return std::string();
   }

   // get the url
   std::string remoteUrl = boost::algorithm::trim_copy(result.stdOut);

   // check for github

   // check for ssh url
   std::string repo;
   const std::string kSSHPrefix = "git@github.com:";
   if (boost::algorithm::starts_with(remoteUrl, kSSHPrefix))
      repo = remoteUrl.substr(kSSHPrefix.length());

   // check for https url
   const std::string kHTTPSPrefix = "https://github.com/";
   if (boost::algorithm::starts_with(remoteUrl, kHTTPSPrefix))
      repo = remoteUrl.substr(kHTTPSPrefix.length());

   // bail if we didn't get a repo
   if (repo.empty())
      return std::string();

   // if the repo starts with / then remove it
   if (repo[0] == '/')
      repo = repo.substr(1);

   // strip the .git off the end and form the github url from repo and branch
   boost::algorithm::replace_last(repo, ".git", "");
   std::string url = "https://github.com/" +
                     repo + "/" + view + "/" +
                     upstreamBranch;

   if (!filePath.empty())
   {
      std::string relative = filePath.relativePath(s_git_.root());
      if (relative.empty())
         return std::string();

      url = url + "/" + relative;
   }

   return url;
}


Error vcsGithubRemoteUrl(const json::JsonRpcRequest& request,
                         json::JsonRpcResponse* pResponse)
{
   // get the params
   std::string view, path;
   Error error = json::readParams(request.params, &view, &path);
   if (error)
      return error;

   // resolve path
   FilePath filePath = module_context::resolveAliasedPath(path);

   // return the github url
   pResponse->setResult(githubUrl(view, filePath));
   return Success();
}


Error vcsHistoryCount(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   std::string rev, searchText;
   json::Value fileFilterJson;
   Error error = json::readParams(request.params,
                                  &rev,
                                  &fileFilterJson,
                                  &searchText);
   if (error)
      return error;

   FilePath fileFilter = fileFilterPath(fileFilterJson);

   boost::algorithm::trim(searchText);

   int count = 0;
   error = s_git_.logLength(rev, fileFilter, searchText, &count);
   if (error)
      return error;

   json::Object result;
   result["count"] = count;
   pResponse->setResult(result);

   return Success();
}

Error vcsHistory(const json::JsonRpcRequest& request,
                 json::JsonRpcResponse* pResponse)
{
   std::string rev, searchText;
   json::Value fileFilterJson;
   int skip, maxentries;
   Error error = json::readParams(request.params,
                                  &rev,
                                  &fileFilterJson,
                                  &skip,
                                  &maxentries,
                                  &searchText);
   if (error)
      return error;

   FilePath fileFilter = fileFilterPath(fileFilterJson);

   boost::algorithm::trim(searchText);

   std::vector<CommitInfo> commits;
   error = s_git_.log(rev, fileFilter, skip, maxentries, searchText, &commits);
   if (error)
      return error;

   json::Array ids;
   json::Array authors;
   json::Array parents;
   json::Array subjects;
   json::Array dates;
   json::Array descriptions;
   json::Array refs;
   json::Array tags;
   json::Array graphs;

   for (std::vector<CommitInfo>::const_iterator it = commits.begin();
        it != commits.end();
        it++)
   {
      ids.push_back(it->id.substr(0, 8));
      authors.push_back(string_utils::filterControlChars(it->author));
      parents.push_back(string_utils::filterControlChars(it->parent));
      subjects.push_back(string_utils::filterControlChars(it->subject));
      descriptions.push_back(string_utils::filterControlChars(it->description));
      dates.push_back(static_cast<double>(it->date));
      graphs.push_back(it->graph);

      json::Array theseRefs;
      std::copy(it->refs.begin(), it->refs.end(), std::back_inserter(theseRefs));
      refs.push_back(theseRefs);

      json::Array theseTags;
      std::copy(it->tags.begin(), it->tags.end(), std::back_inserter(theseTags));
      tags.push_back(theseTags);
   }

   json::Object result;
   result["id"] = ids;
   result["author"] = authors;
   result["parent"] = parents;
   result["subject"] = subjects;
   result["description"] = descriptions;
   result["date"] = dates;
   result["refs"] = refs;
   result["tags"] = tags;
   result["graph"] = graphs;

   pResponse->setResult(result);

   return Success();
}

Error vcsShow(const json::JsonRpcRequest& request,
              json::JsonRpcResponse* pResponse)
{
   std::string rev;
   bool noSizeWarning;
   Error error = json::readParams(request.params, &rev, &noSizeWarning);
   if (error)
      return error;

   std::string output;
   s_git_.show(rev, &output);
   output = convertDiff(output, projects::projectContext().defaultEncoding(),
                        "UTF-8", true);
   output = string_utils::filterControlChars(output);

   if (!noSizeWarning && output.size() > source_control::WARN_SIZE)
   {
      error = systemError(boost::system::errc::file_too_large,
                          ERROR_LOCATION);
      pResponse->setError(error,
                          json::Value(static_cast<boost::uint64_t>(output.size())));
   }
   else
   {
      pResponse->setResult(output);
   }
   return Success();
}


Error vcsShowFile(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   std::string rev,filename;
   Error error = json::readParams(request.params, &rev, &filename);
   if (error)
      return error;

   std::string output;
   error = s_git_.showFile(rev, filename, &output);
   if (error)
      return error;

   // convert to utf8
   output = convertToUtf8(output, false);

   output = string_utils::filterControlChars(output);

   pResponse->setResult(output);

   return Success();
}


Error vcsExportFile(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   // read parameters
   std::string rev, filename, targetPath;
   Error error = json::readParams(request.params,
                                  &rev,
                                  &filename,
                                  &targetPath);
   if (error)
      return error;

   // get the contents of the file
   std::string output;
   error = s_git_.showFile(rev, filename, &output);
   if (error)
      return error;

   // write it
   return core::writeStringToFile(
                  module_context::resolveAliasedPath(targetPath),
                  output);
}



Error vcsSshPublicKey(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   // get public key path
   std::string aliasedPath;
   Error error = json::readParam(request.params, 0, &aliasedPath);
   if (error)
      return error;

   // unalias it and check for existence
   FilePath publicKeyPath = module_context::resolveAliasedPath(aliasedPath);
   if (!publicKeyPath.exists())
   {
      return core::fileNotFoundError(publicKeyPath.absolutePath(),
                                     ERROR_LOCATION);
   }

   // read the key
   std::string publicKeyContents;
   error = core::readStringFromFile(publicKeyPath, &publicKeyContents);
   if (error)
      return error;

   pResponse->setResult(publicKeyContents);

   return Success();
}

Error vcsHasRepo(const json::JsonRpcRequest& request,
                 json::JsonRpcResponse* pResponse)
{
   // get directory
   std::string directory;
   Error error = json::readParam(request.params, 0, &directory);
   if (error)
      return error;
   FilePath dirPath = module_context::resolveAliasedPath(directory);

   FilePath gitDir = detectGitDir(dirPath);

   pResponse->setResult(!gitDir.empty());

   return Success();
}


Error vcsInitRepo(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   // get directory
   std::string directory;
   Error error = json::readParam(request.params, 0, &directory);
   if (error)
      return error;
   FilePath dirPath = module_context::resolveAliasedPath(directory);

   core::system::ProcessOptions options = procOptions();
   options.workingDir = dirPath;

   // run it
   core::system::ProcessResult result;
   error = runCommand(git() << "init", options, &result);
   if (error)
      return error;

   // verify success
   if (result.exitStatus != 0)
   {
      LOG_ERROR_MESSAGE("Error creating git repo: " + result.stdErr);
      return systemError(boost::system::errc::operation_not_permitted,
                         ERROR_LOCATION);
   }
   else
   {
      return Success();
   }
}

bool ensureSSHAgentIsRunning()
{
   // Use "ssh-add -l" to see if ssh-agent is running
   core::system::ProcessResult result;
   Error error = runCommand(shell_utils::sendStdErrToNull("ssh-add -l"),
                            procOptions(), &result);
   if (error)
   {
      // We couldn't even launch ssh-add.
      return false;
   }

   if (result.exitStatus == 1)
   {
      // exitStatus == 1 means ssh-agent was running but no identities were
      // present.
      return true;
   }
   else if (result.exitStatus == EXIT_SUCCESS)
   {
      return true;
   }

   // Start ssh-agent using bash-style output
   error = runCommand("ssh-agent -s", procOptions(), &result);
   if (error)
   {
      // Failed to start ssh-agent, give up.
      LOG_ERROR(error);
      return false;
   }
   if (result.exitStatus != EXIT_SUCCESS)
   {
      return false;
   }

   // In addition to dumping the ssh-agent output, we also need to parse
   // it so we can modify rsession's environment to use the new ssh-agent
   // as well.
   try
   {
      boost::sregex_iterator it(result.stdOut.begin(), result.stdOut.end(),
                                boost::regex("^([A-Za-z0-9_]+)=([^;]+);"));
      boost::sregex_iterator end;
      for (; it != end; it++)
      {
         std::string name = (*it).str(1);
         std::string value = (*it).str(2);
         core::system::setenv(name, value);

         if (name == "SSH_AGENT_PID")
         {
            int pid = safe_convert::stringTo<int>(value, 0);
            if (pid)
               s_pidsToTerminate_.push_back(pid);
         }
      }
   }
   CATCH_UNEXPECTED_EXCEPTION;

   return true;
}

void addKeyToSSHAgent_onCompleted(const core::system::ProcessResult& result)
{
   if (result.exitStatus != EXIT_SUCCESS)
      LOG_ERROR_MESSAGE(result.stdErr);
}

void addKeyToSSHAgent(const FilePath& keyFile,
                      const std::string& passphrase)
{
   core::system::ProcessOptions options = procOptions();
   core::system::setenv(options.environment.get_ptr(),
                        "__ASKPASS_PASSTHROUGH_RESULT",
                        passphrase);
   core::system::setenv(options.environment.get_ptr(),
                        "SSH_ASKPASS",
                        "askpass-passthrough");

   ShellCommand cmd("ssh-add");
   cmd << "--" << keyFile;

   // Fire and forget. We don't care about the outcome.
   // But we want to run it async in case it does somehow end up blocking;
   // if we were running it synchronously, this would block the main thread.
   module_context::processSupervisor().runCommand(
         shell_utils::sendNullToStdIn(cmd),
         options,
         &addKeyToSSHAgent_onCompleted);
}


std::string transformKeyPath(const std::string& path)
{
#ifdef _WIN32
   boost::smatch match;
   if (regex_utils::match(path, match, boost::regex("/([a-zA-Z])/.*")))
   {
      return match[1] + std::string(":") + path.substr(2);
   }
#endif
   return path;
}

void postbackSSHAskPass(const std::string& prompt,
                        const module_context::PostbackHandlerContinuation& cont)
{
   // default to failure unless we successfully receive a passphrase
   int retcode = EXIT_FAILURE;
   std::string passphrase;

   bool promptToRemember;
   boost::smatch match;
   FilePath keyFile;

   // This is what the prompt looks like on OpenSSH_4.6p1 (Windows)
   if (regex_utils::match(prompt, match, boost::regex("Enter passphrase for key '(.+)': ")))
   {
      promptToRemember = true;
      keyFile = FilePath(transformKeyPath(match[1]));
   }
   // This is what the prompt looks like on OpenSSH_5.8p1 Debian-7ubuntu1 (Ubuntu 11.10)
   else if (regex_utils::match(prompt, match, boost::regex("Enter passphrase for (.+): ")))
   {
      promptToRemember = true;
      keyFile = FilePath(transformKeyPath(match[1]));
   }
   else
      promptToRemember = false;

   promptToRemember = promptToRemember && keyFile.exists();

   std::string rememberPrompt;
   if (promptToRemember)
      rememberPrompt = "Remember passphrase for this session";

   std::string askPrompt = !prompt.empty() ? prompt :
                                             std::string("Enter passphrase:");

   // prompt
   ask_pass::PasswordInput input;
   Error error = ask_pass::askForPassword(askPrompt,
                                          rememberPrompt,
                                          &input);
   if (!error)
   {
      if (!input.cancelled)
      {
         retcode = EXIT_SUCCESS;
         passphrase = input.password;

         if (input.remember)
         {
            ensureSSHAgentIsRunning();
            addKeyToSSHAgent(keyFile, passphrase);
         }
      }
   }
   else
   {
      LOG_ERROR(error);
   }

   // satisfy continuation
   cont(retcode, passphrase);
}

#ifdef _WIN32

template <typename T>
class AutoRelease
{
public:
   AutoRelease(T* pUnk) : pUnk_(pUnk)
   {
   }

   ~AutoRelease()
   {
      if (pUnk_)
         pUnk_->Release();
   }

private:
   T* pUnk_;
};


bool detectGitExeDirOnPath(FilePath* pPath)
{
   std::vector<wchar_t> path(MAX_PATH+2);
   wcscpy(&(path[0]), L"git.exe");
   if (::PathFindOnPathW(&(path[0]), NULL))
   {
      // As of version 20120710 of msysgit, the cmd directory contains a
      // git.exe wrapper that, if used by us, causes console windows to
      // flash
      FilePath filePath(&(path[0]));
      if (filePath.parent().filename() == "cmd")
        return false;

      *pPath = filePath.parent();
      return true;
   }
   else
   {
      return false;
   }
}

bool isGitExeOnPath()
{
   FilePath gitExeDir;
   return detectGitExeDirOnPath(&gitExeDir);
}


bool detectGitBinDirFromPath(FilePath* pPath)
{
   std::vector<wchar_t> path(MAX_PATH+2);
   wcscpy(&(path[0]), L"git.cmd");

   if (::PathFindOnPathW(&(path[0]), NULL))
   {
      *pPath = FilePath(&(path[0])).parent().parent().childPath("bin");
      return true;
   }

   // Look for cmd/git.exe and redirect to bin/
   wcscpy(&(path[0]), L"git.exe");

   if (::PathFindOnPathW(&(path[0]), NULL))
   {
      *pPath = FilePath(&(path[0])).parent().parent().childPath("bin");
      return true;
   }

   return false;
}

HRESULT detectGitBinDirFromShortcut(FilePath* pPath)
{
   using namespace boost;

   CoInitialize(NULL);

   // Step 1. Find the Git Bash shortcut on the Start menu
   std::vector<wchar_t> data(MAX_PATH+2);
   HRESULT hr = ::SHGetFolderPathW(NULL,
                                   CSIDL_COMMON_PROGRAMS,
                                   NULL,
                                   SHGFP_TYPE_CURRENT,
                                   &(data[0]));
   if (FAILED(hr))
      return hr;

   // look for Git Bash or Git GUI link
   std::wstring path(&(data[0]));
   path.append(L"\\Git\\Git Bash.lnk");
   if (::GetFileAttributesW(path.c_str()) == INVALID_FILE_ATTRIBUTES)
   {
      // try for Git GUI
      path = std::wstring(&(data[0]));
      path.append(L"\\Git\\Git GUI.lnk");
      if (::GetFileAttributesW(path.c_str()) == INVALID_FILE_ATTRIBUTES)
         return E_FAIL;
   }

   // Step 2. read the shortcut
   IShellLinkW* pShellLink;
   hr = CoCreateInstance(CLSID_ShellLink,
                         NULL,
                         CLSCTX_INPROC_SERVER,
                         IID_IShellLinkW,
                         (void**)&pShellLink);
   if (FAILED(hr))
      return hr;
   AutoRelease<IShellLinkW> arShellLink(pShellLink);

   IPersistFile* pPersistFile;
   hr = pShellLink->QueryInterface(IID_IPersistFile, (void**)&pPersistFile);
   if (FAILED(hr))
      return hr;
   AutoRelease<IPersistFile> arPersistFile(pPersistFile);

   pPersistFile->Load(path.c_str(), STGM_READ);
   if (FAILED(hr))
      return hr;

   hr = pShellLink->Resolve(NULL, SLR_NO_UI | 0x10000);
   if (FAILED(hr))
      return hr;

   // Step 3. Extract the git/bin directory from the shortcut's path or
   // arguments (depending on the version of Git that is installed)

   // check the path of the shortcut
   std::vector<wchar_t> pathbuff(1024);
   hr = pShellLink->GetPath(&(pathbuff[0]), pathbuff.capacity() - 1, NULL, 0L);
   if (FAILED(hr))
      return hr;

   // if this is git-bash then take the child bin dir
   // (this is compatible with msysgit ~ 2.6)
   if (boost::algorithm::contains(pathbuff, L"git-bash.exe"))
   {
      *pPath = FilePath(std::wstring(&(pathbuff[0])));
      if (!pPath->exists())
         return E_FAIL;
      // go up a level then down to bin
      *pPath = pPath->parent().childPath("bin");
      if (!pPath->exists())
         return E_FAIL;

      return S_OK;
   }
   // same check for git-gui
   else if (boost::algorithm::contains(pathbuff, L"git-gui.exe"))
   {
      *pPath = FilePath(std::wstring(&(pathbuff[0])));
      if (!pPath->exists())
         return E_FAIL;
      // this is located in \cmd so we need to go up two levels
      *pPath = pPath->parent().parent().childPath("bin");
      if (!pPath->exists())
         return E_FAIL;

      return S_OK;
   }
   // if this is a binary in the git bin directory then take it's parent
   // (this is compatible with msysgit ~ 1.9)
   else if (boost::algorithm::contains(pathbuff, L"sh.exe") ||
            boost::algorithm::contains(pathbuff, L"wish.exe"))
   {
      *pPath = FilePath(std::wstring(&(pathbuff[0])));
      if (!pPath->exists())
         return E_FAIL;
      *pPath = pPath->parent();
      if (!pPath->exists())
         return E_FAIL;

      return S_OK;
   }
   // if we invoke via cmd.exe then check the arguments
   // (compatible with ~ msysgit 1.7)
   else if (boost::algorithm::contains(pathbuff, L"cmd.exe"))
   {
      std::vector<wchar_t> argbuff(1024);
      hr = pShellLink->GetArguments(&(argbuff[0]), argbuff.capacity() - 1);
      if (FAILED(hr))
         return hr;

      wcmatch match;
      if (!regex_search(&(argbuff[0]), match, wregex(L"\"\"([^\"]*)\"")))
      {
         LOG_ERROR_MESSAGE("Unexpected git bash argument format: " +
                           string_utils::wideToUtf8(&(argbuff[0])));
         return E_FAIL;
      }

      *pPath = FilePath(match[1]);
      if (!pPath->exists())
         return E_FAIL;
      // The path we have is to sh.exe or wish.exe, we want the parent
      *pPath = pPath->parent();
      if (!pPath->exists())
         return E_FAIL;

      return S_OK;
   }
   // not found
   else
   {
      return E_FAIL;
   }
}

bool detectGitBinDirFromStandardLocation(FilePath* pPath)
{
   FilePath standardGitBinPath("C:\\Program Files\\Git\\bin");
   if (standardGitBinPath.exists())
   {
      *pPath = standardGitBinPath;
      return true;
   }

   standardGitBinPath = FilePath("C:\\Program Files (x86)\\Git\\bin");
   if (standardGitBinPath.exists())
   {
      *pPath = standardGitBinPath;
      return true;
   }

   return false;
}

Error discoverGitBinDir(FilePath* pPath)
{
   if (detectGitBinDirFromPath(pPath))
      return Success();

   HRESULT hr = detectGitBinDirFromShortcut(pPath);
   if (SUCCEEDED(hr))
      return Success();

   if (detectGitBinDirFromStandardLocation(pPath))
      return Success();

   return systemError(boost::system::errc::no_such_file_or_directory,
                      ERROR_LOCATION);
}

Error detectAndSaveGitExePath()
{
   if (isGitExeOnPath())
      return Success();

   FilePath path;
   Error error = discoverGitBinDir(&path);
   if (error)
      return error;

   // save it
   s_gitExePath = path.complete("git.exe").absolutePath();

   return Success();
}


#endif

void onShutdown(bool)
{
   std::for_each(s_pidsToTerminate_.begin(), s_pidsToTerminate_.end(),
                 &core::system::terminateProcess);
   s_pidsToTerminate_.clear();
}

Error addFilesToGitIgnore(const FilePath& gitIgnoreFile,
                          const std::vector<std::string>& filesToIgnore,
                          bool addExtraNewline)
{
#ifdef _WIN32
   const char * const kNewline = "\r\n";
#else
   const char * const kNewline = "\n";
#endif

   if (filesToIgnore.empty())
      return Success();

   boost::shared_ptr<std::ostream> ptrOs;
   Error error = gitIgnoreFile.open_w(&ptrOs, false);
   if (error)
      return error;

   ptrOs->seekp(0, std::ios::end);
   if (ptrOs->good())
   {
      if (addExtraNewline)
         *ptrOs << kNewline;

      BOOST_FOREACH(const std::string& line, filesToIgnore)
      {
         *ptrOs << line << kNewline;
      }

      ptrOs->flush();
   }

   return Success();
}

Error augmentGitIgnore(const FilePath& gitIgnoreFile)
{
   // Add stuff to .gitignore
   std::vector<std::string> filesToIgnore;
   if (!gitIgnoreFile.exists())
   {
      // If no .gitignore exists, add this stuff

      // standard R and RStudio files
      filesToIgnore.push_back(".Rproj.user");
      filesToIgnore.push_back(".Rhistory");
      filesToIgnore.push_back(".RData");
      filesToIgnore.push_back(".Ruserdata");

      // if this is a package dir with a src directory then
      // also ignore native code build artifacts
      FilePath gitIgnoreParent = gitIgnoreFile.parent();
      if (gitIgnoreParent.childPath("DESCRIPTION").exists() &&
          gitIgnoreParent.childPath("src").exists())
      {
         filesToIgnore.push_back("src/*.o");
         filesToIgnore.push_back("src/*.so");
         filesToIgnore.push_back("src/*.dll");
      }

      return addFilesToGitIgnore(gitIgnoreFile, filesToIgnore, false);
   }
   else
   {
      // If .gitignore exists, add .Rproj.user unless it's already there

      std::string strIgnore;
      Error error = core::readStringFromFile(gitIgnoreFile, &strIgnore);
      if (error)
         return error;

      if (regex_utils::search(strIgnore, boost::regex("^\\.Rproj\\.user$")))
         return Success();

      bool addExtraNewline = strIgnore.size() > 0
                             && strIgnore[strIgnore.size() - 1] != '\n';

      std::vector<std::string> filesToIgnore;
      filesToIgnore.push_back(".Rproj.user");
      return addFilesToGitIgnore(gitIgnoreFile,
                                 filesToIgnore,
                                 addExtraNewline);
   }
}

FilePath whichGitExe()
{
   // find git
   FilePath whichGit = module_context::findProgram("git");
   if (whichGit.empty())
   {
      return whichGit;
   }
   else
   {
      // if we are on osx mavericks we need to do a further check to make
      // sure this isn't the fake version of git installed by default
      if (module_context::isOSXMavericks())
      {
         if (module_context::hasOSXMavericksDeveloperTools())
            return whichGit;
         else
            return FilePath();
      }
      else
      {
         return whichGit;
      }
   }
}

} // anonymous namespace

bool isGitInstalled()
{
   if (!userSettings().vcsEnabled())
      return false;

   // special handling for mavericks for case where there is /usr/bin/git
   // but it's the fake on installed by osx
   if ((s_gitExePath.empty() || s_gitExePath == "/usr/bin/git") &&
       module_context::isOSXMavericks() &&
       !module_context::hasOSXMavericksDeveloperTools() &&
       whichGitExe().empty())
   {
      return false;
   }

   core::system::ProcessResult result;
   Error error = core::system::runCommand(git() << "--version",
                                          procOptions(),
                                          &result);
   if (error)
      return false;
   return result.exitStatus == EXIT_SUCCESS;
}

bool isGitEnabled()
{
   return !s_git_.root().empty();
}

bool isWithinGitRoot(const core::FilePath& filePath)
{
   return isGitEnabled() && filePath.isWithin(s_git_.root());
}

FilePath detectedGitExePath()
{
#ifdef _WIN32
   FilePath path;
   if (detectGitExeDirOnPath(&path))
   {
      return path.complete("git.exe");
   }
   else
   {
      Error error = discoverGitBinDir(&path);
      if (!error)
      {
         return path.complete("git.exe");
      }
      else
      {
         return FilePath();
      }
   }
#else
   FilePath gitExeFilePath = whichGitExe();
   if (!gitExeFilePath.empty())
      return FilePath(gitExeFilePath);
   else
      return FilePath();
#endif
}


std::string nonPathGitBinDir()
{
   if (!s_gitExePath.empty())
      return FilePath(s_gitExePath).parent().absolutePath();
   else
      return std::string();
}

void onUserSettingsChanged()
{
   FilePath gitExePath = userSettings().gitExePath();
   if (session::options().allowVcsExecutableEdit() && !gitExePath.empty())
   {
      // if there is an explicit value then set it
      s_gitExePath = gitExePath.absolutePath();
   }
   else
   {
      // if we are relying on an auto-detected value then scan on windows
      // and reset to empty on posix
#ifdef _WIN32
      detectAndSaveGitExePath();
#else
      s_gitExePath = "";
#endif
   }
}

Error statusToJson(const core::FilePath &path,
                   const VCSStatus &status,
                   core::json::Object *pObject)
{
   json::Object& obj = *pObject;
   obj["status"] = status.status();
   obj["path"] = path.relativePath(s_git_.root());
   obj["raw_path"] = module_context::createAliasedPath(path);
   obj["discardable"] = status.status()[1] != ' ' && status.status()[1] != '?';
   obj["is_directory"] = path.isDirectory();
   return Success();
}

void onSuspend(core::Settings*)
{
}

void onResume(const core::Settings&)
{
   enqueueRefreshEvent();
}

bool initGitBin()
{
   Error error;

   // get the git bin dir from settings if it is there
   if (session::options().allowVcsExecutableEdit())
      s_gitExePath = userSettings().gitExePath().absolutePath();

   // if it wasn't provided in settings then make sure we can detect it
   if (s_gitExePath.empty())
   {
#ifdef _WIN32
      error = detectAndSaveGitExePath();
      if (error)
         return false; // no Git install detected
#else
      FilePath gitExeFilePath = whichGitExe();
      if (gitExeFilePath.empty())
         return false; // no Git install detected
#endif
   }

   // Save version
   s_gitVersion = GIT_1_7_2;
   core::system::ProcessResult result;
   error = core::system::runCommand(git() << "--version",
                                    procOptions(),
                                    &result);
   if (error)
      LOG_ERROR(error);
   else
   {
      if (result.exitStatus == 0)
      {
         boost::smatch matches;
         if (regex_utils::search(result.stdOut,
                                 matches,
                                 boost::regex("\\d+(\\.\\d+)+")))
         {
            string_utils::parseVersion(matches[0], &s_gitVersion);
         }
      }
   }

   return true;
}

bool isGitDirectory(const core::FilePath& workingDir)
{
   return !detectGitDir(workingDir).empty();
}


std::string remoteOriginUrl(const FilePath& workingDir)
{
   // default to none
   std::string remoteOriginUrl;

   core::system::ProcessResult result;
   Error error = gitExec(ShellArgs() <<
                           "config" << "--get" << "remote.origin.url",
                         workingDir,
                         &result);

   if (error)
   {
      LOG_ERROR(error);
   }
   else if (result.exitStatus == 0)
   {
      remoteOriginUrl = boost::algorithm::trim_copy(result.stdOut);
   }

   // return any url we discovered
   return remoteOriginUrl;
}


bool isGithubRepository()
{
   return !githubUrl("blob").empty();
}


core::Error initializeGit(const core::FilePath& workingDir)
{
   s_git_.setRoot(detectGitDir(workingDir));

   if (!s_git_.root().empty())
   {
      FilePath gitIgnore = s_git_.root().childPath(".gitignore");
      Error error = augmentGitIgnore(gitIgnore);
      if (error)
         LOG_ERROR(error);
   }

   return Success();
}


Error clone(const std::string& url,
            const std::string dirName,
            const FilePath& parentPath,
            boost::shared_ptr<console_process::ConsoleProcess>* ppCP)
{
   Git git(options().userHomePath());
   Error error = git.clone(url, dirName, parentPath, ppCP);
   if (error)
      return error;

   return Success();
}

core::Error initialize()
{
   using namespace session::module_context;

   Error error;

   module_context::events().onShutdown.connect(onShutdown);

   initGitBin();

   bool interceptAskPass;

   if (options().programMode() == kSessionProgramModeServer)
   {
      interceptAskPass = true;
   }
   else
   {
#ifdef _WIN32
      // Windows probably unlikely to have either ssh-agent or askpass
      interceptAskPass = true;
#else
      // Everything fine on Mac and Linux
      interceptAskPass = false;
#endif
   }

   // register postback handler
   std::string sshAskCmd;
   error = module_context::registerPostbackHandler("askpass",
                                                   postbackSSHAskPass,
                                                   &sshAskCmd);
   if (error)
      return error;

   // setup environment
   BOOST_ASSERT(boost::algorithm::ends_with(sshAskCmd, "rpostback-askpass"));
   core::system::setenv("GIT_ASKPASS", "rpostback-askpass");

   if (interceptAskPass)
   {
      core::system::setenv("SSH_ASKPASS", "rpostback-askpass");
   }

   // add suspend/resume handler
   addSuspendHandler(SuspendHandler(boost::bind(onSuspend, _2), onResume));

   // add settings changed handler
   userSettings().onChanged.connect(onUserSettingsChanged);

   // install rpc methods
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "git_add", vcsAdd))
      (bind(registerRpcMethod, "git_remove", vcsRemove))
      (bind(registerRpcMethod, "git_discard", vcsDiscard))
      (bind(registerRpcMethod, "git_revert", vcsRevert))
      (bind(registerRpcMethod, "git_stage", vcsStage))
      (bind(registerRpcMethod, "git_unstage", vcsUnstage))
      (bind(registerRpcMethod, "git_create_branch", vcsCreateBranch))
      (bind(registerRpcMethod, "git_list_branches", vcsListBranches))
      (bind(registerRpcMethod, "git_checkout", vcsCheckout))
      (bind(registerRpcMethod, "git_checkout_remote", vcsCheckoutRemote))
      (bind(registerRpcMethod, "git_full_status", vcsFullStatus))
      (bind(registerRpcMethod, "git_all_status", vcsAllStatus))
      (bind(registerRpcMethod, "git_commit", vcsCommit))
      (bind(registerRpcMethod, "git_push", vcsPush))
      (bind(registerRpcMethod, "git_push_branch", vcsPushBranch))
      (bind(registerRpcMethod, "git_pull", vcsPull))
      (bind(registerRpcMethod, "git_diff_file", vcsDiffFile))
      (bind(registerRpcMethod, "git_apply_patch", vcsApplyPatch))
      (bind(registerRpcMethod, "git_history_count", vcsHistoryCount))
      (bind(registerRpcMethod, "git_history", vcsHistory))
      (bind(registerRpcMethod, "git_show", vcsShow))
      (bind(registerRpcMethod, "git_show_file", vcsShowFile))
      (bind(registerRpcMethod, "git_export_file", vcsExportFile))
      (bind(registerRpcMethod, "git_ssh_public_key", vcsSshPublicKey))
      (bind(registerRpcMethod, "git_has_repo", vcsHasRepo))
      (bind(registerRpcMethod, "git_init_repo", vcsInitRepo))
      (bind(registerRpcMethod, "git_get_ignores", vcsGetIgnores))
      (bind(registerRpcMethod, "git_set_ignores", vcsSetIgnores))
      (bind(registerRpcMethod, "git_list_remotes", vcsListRemotes))
      (bind(registerRpcMethod, "git_add_remote", vcsAddRemote))
      (bind(registerRpcMethod, "git_github_remote_url", vcsGithubRemoteUrl));
   error = initBlock.execute();
   if (error)
      return error;

   return Success();
}

} // namespace git
} // namespace modules
} // namespace session
} // namespace rstudio
