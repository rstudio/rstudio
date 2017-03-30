/*
 * SessionSVN.cpp
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

#include "SessionSVN.hpp"

#ifdef _WIN32
#include <windows.h>
#include <shlobj.h>
#include <shlwapi.h>
#endif

#include <boost/algorithm/string.hpp>
#include <boost/bind.hpp>
#include <boost/date_time.hpp>
#include <boost/regex.hpp>
#include <core/BoostLamda.hpp>

#include <core/FileSerializer.hpp>
#include <core/rapidxml/rapidxml.hpp>
#include <core/system/Environment.hpp>
#include <core/system/Process.hpp>
#include <core/system/ShellUtils.hpp>
#include <core/Exec.hpp>
#include <core/http/Header.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>
#include <session/SessionUserSettings.hpp>
#include <session/SessionConsoleProcess.hpp>

#include <r/RExec.hpp>

#include "SessionVCS.hpp"
#include "vcs/SessionVCSUtils.hpp"

#include "SessionAskPass.hpp"
#include "SessionWorkbench.hpp"
#include "SessionGit.hpp"

using namespace rstudio::core;
using namespace rstudio::core::shell_utils;
using namespace rstudio::session::modules::vcs_utils;
using namespace rstudio::session::console_process;

namespace rstudio {
namespace session {
namespace modules {
namespace svn {

const char * const kVcsId = "SVN";

namespace {

// password manager for caching svn+ssh credentials
boost::scoped_ptr<PasswordManager> s_pPasswordManager;

// svn exe which we detect at startup. note that if the svn exe
// is already in the path then this will be empty
std::string s_svnExePath;

// is the current repository svn+ssh
bool s_isSvnSshRepository = false;



/** GLOBAL STATE **/
FilePath s_workingDir;

FilePath resolveAliasedPath(const std::string& path)
{
   if (boost::algorithm::starts_with(path, "~/"))
      return module_context::resolveAliasedPath(path);
   else
      return s_workingDir.childPath(path);
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
      results.push_back(resolveAliasedPath(it->get_str()));
   }
   return results;
}

core::system::ProcessOptions procOptions(bool requiresSsh)
{
   core::system::ProcessOptions options;

   // detach the session so there is no terminal
#ifndef _WIN32
   options.detachSession = true;
#endif

   // get current environment for modification prior to passing to child
   core::system::Options childEnv;
   core::system::environment(&childEnv);

   // add postback directory to PATH
   FilePath postbackDir = session::options().rpostbackPath().parent();
   core::system::addToPath(&childEnv, postbackDir.absolutePath());

   // on windows add gnudiff directory to the path
#ifdef _WIN32
   core::system::addToPath(&childEnv,
                           session::options().gnudiffPath().absolutePath());
#endif

   // on windows add msys_ssh to the path if we need ssh
#ifdef _WIN32
   if (requiresSsh)
   {
      core::system::addToPath(&childEnv,
                              session::options().msysSshPath().absolutePath());
   }
#endif

   if (!s_workingDir.empty())
      options.workingDir = s_workingDir;
   else
      options.workingDir = projects::projectContext().directory();

   // on windows set HOME to USERPROFILE
#ifdef _WIN32
   std::string userProfile = core::system::getenv(childEnv, "USERPROFILE");
   core::system::setenv(&childEnv, "HOME", userProfile);
#endif

   // set the SVN_EDITOR if it is available
   std::string editFileCommand = workbench::editFileCommand();
   if (!editFileCommand.empty())
      core::system::setenv(&childEnv, "SVN_EDITOR", editFileCommand);

   // set custom environment
   options.environment = childEnv;

   return options;
}

core::system::ProcessOptions procOptions()
{
   return procOptions(s_isSvnSshRepository);
}

void initEnvironment()
{
#ifdef _WIN32
   r::exec::RFunction sysSetenv("Sys.setenv");
   sysSetenv.addParam("RSTUDIO_MSYS_SSH",
                      session::options().msysSshPath().absolutePath());
   Error error = sysSetenv.call();
   if (error)
      LOG_ERROR(error);
#endif
}


void maybeAttachPasswordManager(boost::shared_ptr<ConsoleProcess> pCP)
{
   if (s_isSvnSshRepository)
      s_pPasswordManager->attach(pCP);
}

ShellCommand svn()
{
   FilePath exePath(s_svnExePath);
   return ShellCommand(exePath);
}

Error runSvn(const ShellArgs& args,
             const FilePath& workingDir,
             bool redirectStdErrToStdOut,
             core::system::ProcessResult* pResult)
{
   core::system::ProcessOptions options = procOptions();
   if (!workingDir.empty())
      options.workingDir = workingDir;
   options.redirectStdErrToStdOut = redirectStdErrToStdOut;
   Error error = core::system::runCommand(svn() << args.args(),
                                          options,
                                          pResult);
   return error;
}

Error runSvn(const ShellArgs& args,
             bool redirectStdErrToStdOut,
             core::system::ProcessResult* pResult)
{
   FilePath workingDir;
   if (!s_workingDir.empty())
      workingDir = s_workingDir;

   return runSvn(args, workingDir, redirectStdErrToStdOut, pResult);
}

Error runSvn(const ShellArgs& args,
             std::string* pStdOut=NULL,
             std::string* pStdErr=NULL,
             int* pExitCode=NULL)
{
   core::system::ProcessResult result;
   Error error = runSvn(args, false, &result);
   if (error)
      return error;

   if (pStdOut)
      *pStdOut = result.stdOut;
   if (pStdErr)
      *pStdErr = result.stdErr;
   if (pExitCode)
      *pExitCode = result.exitStatus;

   return Success();
}

std::vector<std::string> globalArgs()
{
   std::vector<std::string> args;
   return args;
}


core::Error createConsoleProc(const ShellArgs& args,
                              const FilePath& outputFile,
                              const boost::optional<FilePath>& workingDir,
                              const std::string& caption,
                              bool requiresSsh,
                              bool enqueueRefreshOnExit,
                              boost::shared_ptr<ConsoleProcess>* ppCP)
{
   core::system::ProcessOptions options = procOptions(requiresSsh);
   if (!workingDir)
      options.workingDir = s_workingDir;
   else if (!workingDir.get().empty())
      options.workingDir = workingDir.get();

   // NOTE: we use runCommand style process creation on both windows and posix
   // so that we can redirect standard output to a file -- this works on
   // windows because we are not specifying options.detachProcess (not
   // necessary because ConsoleProcess specifies options.createNewConsole
   // which overrides options.detachProcess)

   // build command
   std::string command = svn() << args.args();

   // redirect stdout to a file
   if (!outputFile.empty())
      options.stdOutFile = outputFile;

   using namespace session::console_process;
   boost::shared_ptr<ConsoleProcessInfo> pCPI =
           boost::make_shared<ConsoleProcessInfo>(caption, InteractionPossible);

   // create the process
   *ppCP = ConsoleProcess::create(command, options, pCPI);

   if (enqueueRefreshOnExit)
      (*ppCP)->onExit().connect(boost::bind(&enqueueRefreshEvent));

   return Success();
}

core::Error createConsoleProc(const ShellArgs& args,
                              const std::string& caption,
                              bool requiresSsh,
                              bool enqueueRefreshOnExit,
                              boost::shared_ptr<ConsoleProcess>* ppCP)
{
   return createConsoleProc(args,
                            FilePath(),
                            boost::optional<FilePath>(),
                            caption,
                            requiresSsh,
                            enqueueRefreshOnExit,
                            ppCP);
}

typedef boost::function<void(const core::Error&,
                             const core::system::ProcessResult&)>
                                                            ProcResultCallback;

void onAsyncSvnExit(int exitCode,
                            const FilePath& outputFile,
                            ProcResultCallback completionCallback)
{
   if (exitCode == EXIT_SUCCESS)
   {
      // read the file
      std::string contents;
      Error error = core::readStringFromFile(outputFile, &contents);
      if (error)
      {
         completionCallback(error, core::system::ProcessResult());
         return;
      }

      core::system::ProcessResult result;
      result.exitStatus = exitCode;
      result.stdOut = contents;
      completionCallback(Success(), result);
   }
   else
   {
      completionCallback(
        systemError(boost::system::errc::operation_canceled,
                    ERROR_LOCATION),
        core::system::ProcessResult());
   }
}

void runSvnAsync(const ShellArgs& args,
                 const std::string& caption,
                 bool enqueueRefreshOnExit,
                 ProcResultCallback completionCallback)
{
   // allocate a temporary file for holding the output
   FilePath outputFile = module_context::tempFile("svn", "out");

   // create a console process so that we can either do terminal based
   // auth prompting or do PasswordManager based prompting if necessary
   boost::shared_ptr<ConsoleProcess> pCP;
   Error error = createConsoleProc(args,
                                   outputFile,
                                   boost::optional<FilePath>(),
                                   caption,
                                   s_isSvnSshRepository,
                                   enqueueRefreshOnExit,
                                   &pCP);
   if (error)
      completionCallback(error, core::system::ProcessResult());

   // set showOnOutput
   pCP->setShowOnOutput(true);

   // attach a password manager if this is svn+ssh
   maybeAttachPasswordManager(pCP);

   // add an exitHandler for returning the file contents to the completion
   pCP->onExit().connect(
     boost::bind(onAsyncSvnExit, _1, outputFile, completionCallback));

   // notify the client about the console process
   json::Object data;
   data["process_info"] = pCP->toJson();
   data["target_window"] = ask_pass::activeWindow();
   ClientEvent event(client_events::kConsoleProcessCreated, data);
   module_context::enqueClientEvent(event);
}

#ifdef _WIN32
bool detectSvnExeOnPath(FilePath* pPath)
{
   std::vector<wchar_t> path(MAX_PATH+2);
   wcscpy(&(path[0]), L"svn.exe");
   if (::PathFindOnPathW(&(path[0]), NULL))
   {
      *pPath = FilePath(&(path[0]));
      return true;
   }
   else
   {
      return false;
   }
}
#endif

FilePath whichSvnExe()
{
   return module_context::findProgram("svn");
}

void initSvnBin()
{
   // get the svn exe from user settings if it is there
   if (session::options().allowVcsExecutableEdit())
      s_svnExePath = userSettings().svnExePath().absolutePath();

   // if it wasn't provided in settings try to detect it
   if (s_svnExePath.empty())
      s_svnExePath = svn::detectedSvnExePath().absolutePath();
}

Error parseXml(const std::string strData,
              std::vector<char>* pDataBuffer,
              rapidxml::xml_document<>* pDoc)
{
   pDataBuffer->reserve(strData.size() + 1);
   std::copy(strData.begin(),
             strData.end(),
             std::back_inserter(*pDataBuffer));
   pDataBuffer->push_back('\0'); // null terminator

   try
   {
      pDoc->parse<0>(&((*pDataBuffer)[0]));
      return Success();
   }
   catch (rapidxml::parse_error)
   {
      return systemError(boost::system::errc::protocol_error,
                         "Could not parse XML",
                         ERROR_LOCATION);
   }
}


} // namespace


bool isSvnInstalled()
{
   // special check on osx mavericks to make sure we don't run the fake svn
   if (module_context::isOSXMavericks() &&
       !module_context::hasOSXMavericksDeveloperTools() &&
       whichSvnExe().empty())
   {
      return false;
   }

   int exitCode;
   Error error = runSvn(ShellArgs() << "help", NULL, NULL, &exitCode);

   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   return exitCode == EXIT_SUCCESS;
}

struct SvnInfo
{
   bool empty() const { return repositoryRoot.empty(); }

   std::string repositoryRoot;
};


Error runSvnInfo(const core::FilePath& workingDir, SvnInfo* pSvnInfo)
{
   if (workingDir.empty())
      return Success();

   core::system::ProcessResult result;
   Error error = runSvn(ShellArgs() << "info" << "--xml",
                        workingDir,
                        true,
                        &result);
   if (error)
      return error;

   if (result.exitStatus == EXIT_SUCCESS)
   {
      // parse the xml
      std::vector<char> xmlData;
      using namespace rapidxml;
      xml_document<> doc;
      Error error = parseXml(result.stdOut, &xmlData, &doc);
      if (error)
         return error;

      // traverse to repository root
      xml_node<>* pInfo = doc.first_node("info");
      if (!pInfo)
         return systemError(boost::system::errc::invalid_seek, ERROR_LOCATION);
      xml_node<>* pEntry = pInfo->first_node("entry");
      if (!pEntry)
         return systemError(boost::system::errc::invalid_seek, ERROR_LOCATION);
      xml_node<>* pRepository = pEntry->first_node("repository");
      if (!pRepository)
         return systemError(boost::system::errc::invalid_seek, ERROR_LOCATION);
      xml_node<>* pRoot = pRepository->first_node("root");
      if (!pRoot)
         return systemError(boost::system::errc::invalid_seek, ERROR_LOCATION);

      // get the value
      pSvnInfo->repositoryRoot = pRoot->value();
   }

   return Success();
}

bool isSvnDirectory(const core::FilePath& workingDir)
{
   return !repositoryRoot(workingDir).empty();
}

std::string repositoryRoot(const FilePath& workingDir)
{
   SvnInfo svnInfo;
   Error error = runSvnInfo(workingDir, &svnInfo);
   if (error)
   {
      LOG_ERROR(error);
      return std::string();
   }

   return svnInfo.repositoryRoot;
}

bool isSvnEnabled()
{
   return !s_workingDir.empty();
}

FilePath detectedSvnExePath()
{
#ifdef _WIN32
   FilePath path;
   if (detectSvnExeOnPath(&path))
   {
      return path;
   }
   else
   {
      return FilePath();
   }
#else
   FilePath svnExeFilePath = whichSvnExe();
   if (!svnExeFilePath.empty())
   {
      // extra check on mavericks to make sure it's not the fake svn
      if (module_context::isOSXMavericks())
      {
         if (module_context::hasOSXMavericksDeveloperTools())
            return FilePath(svnExeFilePath);
         else
            return FilePath();
      }
      else
      {
         return FilePath(svnExeFilePath);
      }
   }
   else
      return FilePath();
#endif
}

std::string nonPathSvnBinDir()
{
   if (s_svnExePath != svn::detectedSvnExePath().absolutePath())
      return FilePath(s_svnExePath).parent().absolutePath();
   else
      return std::string();
}

void onUserSettingsChanged()
{
   initSvnBin();
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
        parent && varname; \
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

std::string node_value(rapidxml::xml_node<>* pNode, const std::string& nodeName)
{
   using namespace rapidxml;

   xml_node<>* pChild = pNode->first_node(nodeName.c_str());
   if (!pChild)
      return std::string();

   return pChild->value();
}

FilePath resolveAliasedJsonPath(const json::Value& value)
{
   std::string path = value.get_str();
   if (boost::algorithm::starts_with(path, "~/"))
      return module_context::resolveAliasedPath(path);
   else
      return s_workingDir.childPath(path);
}

Error svnAdd(const json::JsonRpcRequest& request,
             json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   json::Array files;
   Error error = json::readParams(request.params, &files);
   if (error)
      return error;

   std::vector<FilePath> paths;
   std::transform(files.begin(), files.end(), std::back_inserter(paths),
                  &resolveAliasedJsonPath);

   core::system::ProcessResult result;
   error = runSvn(ShellArgs() << "add" << globalArgs() << "-q" << "--" << paths,
                  true, &result);
   if (error)
      return error;

   pResponse->setResult(processResultToJson(result));

   return Success();
}

Error svnDelete(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   json::Array files;
   Error error = json::readParams(request.params, &files);
   if (error)
      return error;

   std::vector<FilePath> paths;
   std::transform(files.begin(), files.end(), std::back_inserter(paths),
                  &resolveAliasedJsonPath);

   core::system::ProcessResult result;
   error = runSvn(ShellArgs() << "delete" << globalArgs() << "-q" << "--" << paths,
                  true, &result);
   if (error)
      return error;

   pResponse->setResult(processResultToJson(result));

   return Success();
}

Error status(const FilePath& filePath,
             std::vector<source_control::FileWithStatus>* pFiles);

Error svnRevert(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   json::Array files;
   Error error = json::readParams(request.params, &files);
   if (error)
      return error;

   std::vector<FilePath> paths;
   std::transform(files.begin(), files.end(), std::back_inserter(paths),
                  &resolveAliasedJsonPath);

   // split into two groups -- those for which we desire
   // a recursive revert, and those for which we desire
   // a non-recursive revert
   std::vector<source_control::FileWithStatus> fileStatusVector;
   error = status(FilePath(), &fileStatusVector);
   if (error)
   {
      LOG_ERROR(error);
      return error;
   }
   
   // build map (indexed on file path) for easy lookup
   std::map<std::string, source_control::FileWithStatus> fileStatusMap;
   BOOST_FOREACH(const source_control::FileWithStatus& file, fileStatusVector)
   {
      fileStatusMap[file.path.absolutePath()] = file;
   }
   
   std::vector<FilePath> recursiveReverts;
   std::vector<FilePath> nonRecursiveReverts;
   BOOST_FOREACH(const FilePath& filePath, paths)
   {
      if (!filePath.isDirectory())
      {
         recursiveReverts.push_back(filePath);
         continue;
      }
      
      bool shouldRevertRecursively = false;
      std::string key = filePath.absolutePath();
      if (fileStatusMap.count(key))
      {
         const source_control::FileWithStatus& fileStatus =
               fileStatusMap[key];
         
         shouldRevertRecursively = fileStatus.status.status() == "A";
      }
      
      if (shouldRevertRecursively)
         recursiveReverts.push_back(filePath);
      else
         nonRecursiveReverts.push_back(filePath);
   }
   
   // perform non-recursive reverts
   core::system::ProcessResult result;
   if (!nonRecursiveReverts.empty())
   {
      error = runSvn(ShellArgs() << "revert" << globalArgs() << "-q" <<
                     "--" << nonRecursiveReverts,
                     true,
                     &result);
      if (error)
         return error;
   }
   
   // perform recursive reverts
   if (!recursiveReverts.empty())
   {
      error = runSvn(ShellArgs() << "revert" << globalArgs() << "-q" <<
                     "--depth" << "infinity" <<
                     "--" << recursiveReverts,
                     true,
                     &result);
      if (error)
         return error;
   }

   pResponse->setResult(processResultToJson(result));

   return Success();
}

Error svnResolve(const json::JsonRpcRequest& request,
                 json::JsonRpcResponse* pResponse)
 {
    RefreshOnExit refreshOnExit;

    std::string accept;
    json::Array files;
    Error error = json::readParams(request.params, &accept, &files);
    if (error)
       return error;

    std::vector<FilePath> paths;
    std::transform(files.begin(), files.end(), std::back_inserter(paths),
                   &resolveAliasedJsonPath);

    core::system::ProcessResult result;
    error = runSvn(ShellArgs() << "resolve" << globalArgs() << "-q" <<
                   "--accept" << accept <<
                   "--" << paths,
                   true, &result);
    if (error)
       return error;

    pResponse->setResult(processResultToJson(result));

    return Success();
 }

Error statusToJson(const core::FilePath &path,
                   const source_control::VCSStatus &status,
                   core::json::Object *pObject)
{
   json::Object& obj = *pObject;
   obj["status"] = status.status();
   obj["path"] = path.relativePath(s_workingDir);
   obj["raw_path"] = module_context::createAliasedPath(path);
   obj["is_directory"] = path.isDirectory();
   if (!status.changelist().empty())
      obj["changelist"] = status.changelist();
   return Success();
}

Error status(const FilePath& filePath,
             std::vector<source_control::FileWithStatus>* pFiles)
{
   using namespace source_control;

   ShellArgs args;
   args << "status" << globalArgs() << "--xml" << "--ignore-externals";
   if (!filePath.empty())
      args << "--" << filePath;

   std::string stdOut, stdErr;
   int exitCode;
   Error error = runSvn(
         args,
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
   using namespace rapidxml;
   xml_document<> doc;
   error = parseXml(stdOut, &xmlData, &doc);
   if (error)
      return error;

   const std::string CHANGELIST_NAME("changelist");

   json::Array results;

   xml_node<>* pStatus = doc.first_node("status");
   if (pStatus)
   {
      FOREACH_NODE(pStatus, pList,)
      {
         std::string changelist;
         if (pList->name() == CHANGELIST_NAME)
         {
            changelist = attr_value(pList, "name");
         }

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

            std::string treeConf = attr_value(pStatus, "tree-conflicted");
            if (treeConf == "true")
               item = topStatus(item, "C");

            std::string status = topStatus(item, props);

            if (status.empty() || status == " ")
               continue;

            VCSStatus vcsStatus(status);
            vcsStatus.changelist() = changelist;
            FileWithStatus fileWithStatus;
            fileWithStatus.status = status;
            fileWithStatus.path = s_workingDir.complete(path);

            pFiles->push_back(fileWithStatus);
         }
      }
   }

   return Success();
}

Error status(const FilePath& filePath,
             json::Array* pResults)
{
   std::vector<source_control::FileWithStatus> files;
   Error error = status(filePath, &files);
   if (error)
      return error;

   BOOST_FOREACH(source_control::FileWithStatus file, files)
   {
      json::Object fileObj;
      error = statusToJson(file.path, file.status, &fileObj);
      if (error)
         return error;
      pResults->push_back(fileObj);
   }
   return Success();
}

Error svnStatus(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   json::Array results;
   Error error = status(FilePath(), &results);
   if (error)
      return error;

   pResponse->setResult(results);

   return Success();
}

Error svnUpdate(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   boost::shared_ptr<ConsoleProcess> pCP;
   Error error = createConsoleProc(ShellArgs() << "update" << globalArgs(),
                                   "SVN Update",
                                   s_isSvnSshRepository,
                                   true,
                                   &pCP);
   if (error)
      return error;

   ask_pass::setActiveWindow(request.sourceWindow);

   maybeAttachPasswordManager(pCP);

   pResponse->setResult(pCP->toJson());

   return Success();
}

Error svnCleanup(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   core::system::ProcessResult result;
   Error error = runSvn(ShellArgs() << "cleanup" << globalArgs(),
                        true,
                        &result);
   if (error)
      return error;

   pResponse->setResult(processResultToJson(result));

   return Success();
}


Error svnCommit(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   json::Array paths;
   std::string message;

   Error error = json::readParams(request.params, &paths, &message);
   if (error)
      return error;

   ask_pass::setActiveWindow(request.sourceWindow);

   FilePath tempFile = module_context::tempFile("svnmsg", "txt");
   boost::shared_ptr<std::ostream> pStream;

   error = tempFile.open_w(&pStream);
   if (error)
      return error;

   *pStream << message;

   pStream->flush();
   pStream.reset();  // release file handle


   ShellArgs args;
   args << "commit" << globalArgs();
   args << "-F" << tempFile;

   args << "--";
   if (!paths.empty())
      args << resolveAliasedPaths(paths);

   // TODO: ensure tempFile is deleted when the commit process exits

   boost::shared_ptr<ConsoleProcess> pCP;
   error = createConsoleProc(args,
                             "SVN Commit",
                             s_isSvnSshRepository,
                             true,
                             &pCP);
   if (error)
      return error;

   maybeAttachPasswordManager(pCP);

   pResponse->setResult(pCP->toJson());

   return Success();
}

Error svnDiffFile(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   std::string path;
   int contextLines;
   bool noSizeWarning;
   Error error = json::readParams(request.params,
                                  &path,
                                  &contextLines,
                                  &noSizeWarning);
   if (error)
      return error;

   FilePath filePath = resolveAliasedPath(path);

   if (contextLines < 0)
      contextLines = 999999999;

   std::string extArgs = "-U " + safe_convert::numberToString(contextLines);

   std::string stdOut, stdErr;
   int exitCode;
   error = runSvn(ShellArgs() << "diff" <<
                  "--depth" << "empty" <<
                  "--diff-cmd" << "diff" <<
                  "-x" << extArgs <<
                  "--" << filePath,
                  &stdOut, &stdErr, &exitCode);
   if (error)
      return error;

   if (exitCode != EXIT_SUCCESS)
   {
      LOG_ERROR_MESSAGE(stdErr);
   }

   std::string sourceEncoding = projects::projectContext().defaultEncoding();
   bool usedSourceEncoding;
   stdOut = convertDiff(stdOut, sourceEncoding, "UTF-8", false,
                        &usedSourceEncoding);
   if (!usedSourceEncoding)
      sourceEncoding = "";

   if (!noSizeWarning && stdOut.size() > source_control::WARN_SIZE)
   {
      error = systemError(boost::system::errc::file_too_large,
                          ERROR_LOCATION);
      pResponse->setError(error,
                          json::Value(static_cast<boost::uint64_t>(stdOut.size())));
   }
   else
   {
      json::Object result;
      result["source_encoding"] = sourceEncoding;
      result["decoded_value"] = stdOut;
      pResponse->setResult(result);
   }

   return Success();
}

Error svnApplyPatch(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   std::string path, patch, sourceEncoding;
   Error error = json::readParams(request.params,
                                  &path,
                                  &patch,
                                  &sourceEncoding);
   if (error)
      return error;

   bool converted;
   patch = convertDiff(patch, "UTF-8", sourceEncoding, false, &converted);
   if (!converted)
      return systemError(boost::system::errc::illegal_byte_sequence, ERROR_LOCATION);

   FilePath filePath = resolveAliasedPath(path);

   FilePath tempFile = module_context::tempFile("svnpatch", "txt");
   boost::shared_ptr<std::ostream> pStream;

   error = tempFile.open_w(&pStream);
   if (error)
      return error;

   *pStream << patch;

   pStream->flush();
   pStream.reset();  // release file handle

   ShellCommand cmd("patch");
   cmd << "-i" << tempFile;
   cmd << filePath;

   core::system::ProcessOptions options = procOptions();

   core::system::ProcessResult result;
   error = core::system::runCommand(cmd,
                                    options,
                                    &result);
   if (error)
      return error;

   if (result.exitStatus != EXIT_SUCCESS)
   {
      LOG_ERROR_MESSAGE(result.stdErr);
   }

   return Success();
}

struct CommitInfo
{
   std::string id;
   std::string author;
   std::string subject;
   std::string description;
   boost::posix_time::time_duration::sec_type date;
};

bool commitIsMatch(const std::vector<std::string>& patterns,
                   const CommitInfo& commit)
{
   BOOST_FOREACH(std::string pattern, patterns)
   {
      if (!boost::algorithm::ifind_first(commit.author, pattern)
          && !boost::algorithm::ifind_first(commit.description, pattern)
          && !boost::algorithm::ifind_first(commit.id, pattern)
          && !boost::algorithm::ifind_first(commit.subject, pattern))
      {
         return false;
      }
   }

   return true;
}

boost::function<bool(const CommitInfo&)> createSearchTextPredicate(
      const std::string& searchText)
{
   if (searchText.empty())
      return boost::lambda::constant(true);

   std::vector<std::string> results;
   boost::algorithm::split(results, searchText,
                           boost::algorithm::is_any_of(" \t\r\n"));
   return boost::bind(commitIsMatch, results, _1);
}

Error parseHistoryXml(int skip,
                      int maxentries,
                      const std::string& searchText,
                      const std::string& output,
                      const boost::function<Error(const CommitInfo&)>& callback)
{
   using namespace rapidxml;
   using namespace boost::posix_time;

   std::vector<char> buffer;
   xml_document<> doc;
   Error error = parseXml(output, &buffer, &doc);
   if (error)
      return error;

   xml_node<>* pLog = doc.first_node("log");

   boost::function<bool(const CommitInfo&)> filter =
         createSearchTextPredicate(searchText);

   const std::string NAME_REVISION = "revision";
   const std::string NAME_AUTHOR = "author";
   const std::string NAME_MSG = "msg";
   const std::string NAME_DATE = "date";
   const ptime epoch(boost::gregorian::date(1970,1,1));

   CommitInfo commit;

   int count = 0;
   FOREACH_NODE(pLog, pEntry, "logentry")
   {
      if (count > maxentries)
         break;

      // This is not strictly necessary as "skip > 0" below would catch
      // this case. But this saves us from doing some work for the common
      // case of not searching.
      if (searchText.empty() && skip > 0)
      {
         skip--;
         continue;
      }

      commit.id = attr_value(pEntry, NAME_REVISION);
      commit.author = string_utils::filterControlChars(node_value(pEntry, NAME_AUTHOR));
      std::string message = string_utils::filterControlChars(node_value(pEntry, NAME_MSG));
      splitMessage(message, &commit.subject, &commit.description);

      ptime date = boost::date_time::parse_delimited_time<ptime>(
            node_value(pEntry, NAME_DATE), 'T');
      commit.date = (date - epoch).total_seconds();

      // If we're searching and this doesn't match, skip it and don't decrement
      // the skip--it's as if this one doesn't count
      if (!filter(commit))
      {
         continue;
      }

      if (skip > 0)
      {
         skip--;
         continue;
      }

      error = callback(commit);
      if (error)
         return error;

      count++;
   }

   return Success();
}

void historyEnd(boost::function<void(Error, const std::string&)> callback,
                const Error& error,
                const core::system::ProcessResult& result)
{
   if (!error && result.exitStatus != EXIT_SUCCESS && !result.stdErr.empty())
      LOG_ERROR_MESSAGE(result.stdErr);

   if (error)
      LOG_ERROR(error);

   callback(error, result.stdOut);
}

void history(int rev,
             FilePath fileFilter,
             ShellArgs options,
             boost::function<void(Error, const std::string&)> callback)
{
   ShellArgs args;
   args << "log";
   args << "--xml";

   args << options.args();

   if (rev > 0)
      args << "-r" << safe_convert::numberToString(rev) + ":1";
   else
      args << "-r" << "HEAD:1";

   if (!fileFilter.empty())
      args << fileFilter;

   runSvnAsync(args,
               "SVN History",
               false,
               boost::bind(historyEnd, callback, _1, _2));
}

Error svnHistoryCountEnd_CommitCallback(int* pCount, const CommitInfo&)
{
   (*pCount)++;
   return Success();
}

void svnHistoryCountEnd(const std::string& searchText,
                        const json::JsonRpcFunctionContinuation& cont,
                        Error error, const std::string& output)
{
   using namespace rapidxml;

   json::JsonRpcResponse response;
   if (error)
   {
      cont(error, &response);
      return;
   }

   int count = 0;

   if (searchText.empty())
   {
      std::vector<char> buffer;
      xml_document<> doc;
      error = parseXml(output, &buffer, &doc);
      if (error)
      {
         cont(error, &response);
         return;
      }

      xml_node<>* pLog = doc.first_node("log");
      if (pLog)
      {
         FOREACH_NODE(pLog, pEntry, "logentry")
         {
            count++;
         }
      }
   }
   else
   {
      error = parseHistoryXml(0, 999999999, searchText, output,
                              boost::bind(svnHistoryCountEnd_CommitCallback,
                                          &count,
                                          _1));
      if (error)
      {
         cont(error, &response);
         return;
      }
   }

   json::Object result;
   result["count"] = count;
   response.setResult(result);

   cont(Success(), &response);
}

void svnHistoryCount(const json::JsonRpcRequest& request,
                     const json::JsonRpcFunctionContinuation& cont)
{
   int rev;
   json::Value fileFilterJson;
   std::string searchText;
   Error error = json::readParams(request.params,
                                  &rev,
                                  &fileFilterJson,
                                  &searchText);
   if (error)
   {
      json::JsonRpcResponse response;
      cont(error, &response);
      return;
   }

   ask_pass::setActiveWindow(request.sourceWindow);

   ShellArgs options;
   options << "-q";
   FilePath fileFilter = fileFilterPath(fileFilterJson);
   history(rev, fileFilter, options,
           boost::bind(svnHistoryCountEnd, searchText, cont, _1, _2));
}

Error svnHistoryEnd_CommitCallback(json::Array *pIds,
                                   json::Array *pAuthors,
                                   json::Array *pSubjects,
                                   json::Array *pDescriptions,
                                   json::Array *pDates,
                                   const CommitInfo& commit)
{
   pIds->push_back(commit.id);
   pAuthors->push_back(commit.author);
   pSubjects->push_back(commit.subject);
   pDescriptions->push_back(commit.description);
   pDates->push_back(commit.date);
   return Success();
}

void svnHistoryEnd(int skip,
                   int maxentries,
                   const std::string& searchText,
                   const json::JsonRpcFunctionContinuation& cont,
                   Error error,
                   const std::string& output)
{
   using namespace boost::posix_time;
   using namespace rapidxml;

   json::JsonRpcResponse response;

   if (error)
   {
      cont(error, &response);
      return;
   }

   json::Array ids;
   json::Array authors;
   json::Array subjects;
   json::Array descriptions;
   json::Array dates;

   error = parseHistoryXml(skip, maxentries, searchText, output,
                           boost::bind(svnHistoryEnd_CommitCallback,
                                       &ids,
                                       &authors,
                                       &subjects,
                                       &descriptions,
                                       &dates,
                                       _1));
   if (error)
   {
      cont(error, &response);
      return;
   }

   json::Object result;
   result["id"] = ids;
   result["author"] = authors;
   result["subject"] = subjects;
   result["description"] = descriptions;
   result["date"] = dates;

   response.setResult(result);
   cont(Success(), &response);
}

void svnHistory(const json::JsonRpcRequest& request,
                const json::JsonRpcFunctionContinuation& cont)
{
   int rev;
   json::Value fileFilterJson;
   std::string searchText;
   int skip, maxentries;
   Error error = json::readParams(request.params,
                                  &rev,
                                  &fileFilterJson,
                                  &skip,
                                  &maxentries,
                                  &searchText);
   if (error)
   {
      cont(error, NULL);
      return;
   }

   ask_pass::setActiveWindow(request.sourceWindow);

   ShellArgs options;
   if (searchText.empty())
   {
      int limit = skip + maxentries;
      options << "--limit" << safe_convert::numberToString(limit);
   }

   FilePath fileFilter = fileFilterPath(fileFilterJson);
   history(rev, fileFilter, options,
           boost::bind(svnHistoryEnd,
                       skip,
                       maxentries,
                       searchText,
                       cont,
                       _1,
                       _2));
}

void svnShowEnd(bool noSizeWarning,
                const json::JsonRpcFunctionContinuation& cont,
                Error error,
                const core::system::ProcessResult& result)
{
   json::JsonRpcResponse response;

   if (error)
   {
      LOG_ERROR(error);
      cont(error, &response);
      return;
   }

   if (!noSizeWarning && result.stdOut.size() > source_control::WARN_SIZE)
   {
      response.setError(
            systemError(boost::system::errc::file_too_large, ERROR_LOCATION),
            json::Value(static_cast<boost::uint64_t>(result.stdOut.size())));
   }
   else
   {
      response.setResult(
            convertDiff(result.stdOut,
                        projects::projectContext().defaultEncoding(),
                        "UTF-8",
                        true));
   }

   cont(error, &response);
}

void svnShow(const json::JsonRpcRequest& request,
             const json::JsonRpcFunctionContinuation& cont)
{
   int revision;
   bool noSizeWarning;
   Error error = json::readParams(request.params, &revision, &noSizeWarning);
   if (error)
   {
      json::JsonRpcResponse response;
      cont(error, &response);
      return;
   }

   ShellArgs args;
   args << "diff" << "-c" << revision;

   runSvnAsync(args,
               "SVN History",
               false,
               boost::bind(svnShowEnd, noSizeWarning, cont, _1, _2));
}

void svnShowFileEnd(const json::JsonRpcFunctionContinuation& cont,
                    Error error,
                    const core::system::ProcessResult& result)
{
   json::JsonRpcResponse response;

   if (error)
   {
      LOG_ERROR(error);
      cont(error, &response);
      return;
   }

   response.setResult(convertToUtf8(result.stdOut, false));
   cont(error, &response);
}

void svnShowFile(const json::JsonRpcRequest& request,
                 const json::JsonRpcFunctionContinuation& cont)
{
   int rev;
   std::string filename;
   Error error = json::readParams(request.params, &rev, &filename);
   if (error)
   {
      json::JsonRpcResponse response;
      cont(error, &response);
      return;
   }

   ShellArgs args;
   args << "cat" << "-r" << safe_convert::numberToString(rev)
        << "--" << filename;
   runSvnAsync(args,
               "SVN Show File",
               false,
               boost::bind(svnShowFileEnd, cont, _1, _2));
}

Error getIgnores(const FilePath& filePath,
                    core::system::ProcessResult* pResult)
{
   return runSvn(ShellArgs() << "propget" << "svn:ignore"
                              << filePath << globalArgs(),
                 true,
                 pResult);
}

Error svnGetIgnores(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   std::string path;
   Error error = json::readParam(request.params, 0, &path);
   if (error)
      return error;

   // resolve path
   FilePath filePath = module_context::resolveAliasedPath(path);

   core::system::ProcessResult result;
   error = getIgnores(filePath, &result);
   if (error)
      return error;

  // success
   pResponse->setResult(processResultToJson(result));
   return Success();
}

Error setIgnores(const FilePath& filePath,
                 const std::string& ignores,
                 core::system::ProcessResult* pResult)
{
   // write the ignores to a temporary file
   FilePath ignoresFile = module_context::tempFile("svn-ignore", "txt");
   Error error = core::writeStringToFile(ignoresFile, ignores);
   if (error)
      return error;

   // set them
   error = runSvn(ShellArgs() << "propset" << "svn:ignore"
                              << filePath << "-F" << ignoresFile
                              << globalArgs(),
                  true,
                  pResult);

   // always remove the temporary file
   Error removeError = ignoresFile.remove();
   if (removeError)
      LOG_ERROR(error);

   // return svn error status
   return error;
}

Error svnSetIgnores(const json::JsonRpcRequest& request,
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

   core::system::ProcessResult result;
   error = setIgnores(filePath, ignores, &result);
   if (error)
      return error;

   // success
   pResponse->setResult(processResultToJson(result));
   return Success();
}

Error checkout(const std::string& url,
               const std::string& username,
               const std::string dirName,
               const core::FilePath& parentPath,
               boost::shared_ptr<console_process::ConsoleProcess>* ppCP)
{
   // optional username arg
   ShellArgs args;
   if (!username.empty())
      args << "--username" << username;

   // checkout command
   args << "checkout" << url;

   // optional target directory arg
   if (!dirName.empty())
      args << dirName;

   bool requiresSsh = boost::algorithm::starts_with(url, "svn+ssh");

   Error error = createConsoleProc(args,
                                   FilePath(),
                                   parentPath,
                                   "SVN Checkout",
                                   requiresSsh,
                                   true,
                                   ppCP);

   if (error)
      return error;

   // attach the password manager if this is an svn+ssh url
   if (requiresSsh)
      s_pPasswordManager->attach(*ppCP, false);

   return Success();
}

bool promptForPassword(const std::string& prompt,
                       bool showRememberOption,
                       std::string* pPassword,
                       bool* pRemember)
{
   std::string rememberPrompt = showRememberOption ?
                                "Remember for this session" : "";
   ask_pass::PasswordInput input;
   Error error = ask_pass::askForPassword(prompt,
                                          rememberPrompt,
                                          &input);
   if (!error)
   {
      if (!input.cancelled)
      {
         *pPassword = input.password;
         *pRemember = input.remember;
         return true;
      }
      else
      {
         return false;
      }
   }
   else
   {
      LOG_ERROR(error);
      return false;
   }
}

SvnFileDecorationContext::SvnFileDecorationContext(
                                                 const core::FilePath& rootDir)
{
   using namespace source_control;

   std::vector<FileWithStatus> results;
   Error error = status(rootDir, &results);
   if (error)
      return;

   vcsResult_ = StatusResult(results);
}

SvnFileDecorationContext::~SvnFileDecorationContext()
{
}

void SvnFileDecorationContext::decorateFile(const core::FilePath& filePath,
                                            core::json::Object* pFileObject)
{
   using namespace source_control;

   VCSStatus status = vcsResult_.getStatus(filePath);

   json::Object jsonStatus;
   Error error = statusToJson(filePath, status, &jsonStatus);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   (*pFileObject)["svn_status"] = jsonStatus;
}

Error augmentSvnIgnore()
{
   // check for existing svn:ignore
   core::system::ProcessResult result;
   Error error = getIgnores(s_workingDir, &result);
   if (error)
      return error;
   if (result.exitStatus != EXIT_SUCCESS)
   {
      LOG_ERROR_MESSAGE(result.stdErr);
      return Success();
   }
   std::string svnIgnore = boost::algorithm::trim_copy(result.stdOut);

   // if it's empty then set our default
   if (svnIgnore.empty())
   {
      // If no svn:ignore exists, add this stuff
      svnIgnore += ".Rproj.user\n";
      svnIgnore += ".Rhistory\n";
      svnIgnore += ".RData\n";
      svnIgnore += ".Ruserdata\n";
   }
   else
   {
      // If svn:ignore exists, add .Rproj.user unless it's already there
      if (regex_utils::search(svnIgnore, boost::regex("^\\.Rproj\\.user$")))
         return Success();

      bool addExtraNewline = svnIgnore.size() > 0
                             && svnIgnore[svnIgnore.size() - 1] != '\n';
      if (addExtraNewline)
         svnIgnore += "\n";

      svnIgnore += ".Rproj.user\n";
   }

   // write back svn:ignore
   core::system::ProcessResult setResult;
   error = setIgnores(s_workingDir, svnIgnore, &setResult);
   if (error)
      return error;

   if (result.exitStatus != EXIT_SUCCESS)
      LOG_ERROR_MESSAGE(result.stdErr);

   return Success();
}

Error initialize()
{
   initEnvironment();

   initSvnBin();

   // initialize password manager
   s_pPasswordManager.reset(new PasswordManager(
                         boost::regex("^(.+): $"),
                         boost::bind(promptForPassword, _1, _2, _3, _4)));

   // install rpc methods
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "svn_add", svnAdd))
      (bind(registerRpcMethod, "svn_delete", svnDelete))
      (bind(registerRpcMethod, "svn_revert", svnRevert))
      (bind(registerRpcMethod, "svn_resolve", svnResolve))
      (bind(registerRpcMethod, "svn_status", svnStatus))
      (bind(registerRpcMethod, "svn_update", svnUpdate))
      (bind(registerRpcMethod, "svn_cleanup", svnCleanup))
      (bind(registerRpcMethod, "svn_commit", svnCommit))
      (bind(registerRpcMethod, "svn_diff_file", svnDiffFile))
      (bind(registerRpcMethod, "svn_apply_patch", svnApplyPatch))
      (bind(registerAsyncRpcMethod, "svn_history_count", svnHistoryCount))
      (bind(registerAsyncRpcMethod, "svn_history", svnHistory))
      (bind(registerAsyncRpcMethod, "svn_show", svnShow))
      (bind(registerAsyncRpcMethod, "svn_show_file", svnShowFile))
      (bind(registerRpcMethod, "svn_get_ignores", svnGetIgnores))
      (bind(registerRpcMethod, "svn_set_ignores", svnSetIgnores))
      ;
   Error error = initBlock.execute();
   if (error)
      return error;

   return Success();
}

Error initializeSvn(const core::FilePath& workingDir)
{
   s_workingDir = workingDir;

   Error error = augmentSvnIgnore();
   if (error)
      LOG_ERROR(error);

   std::string repoURL = repositoryRoot(s_workingDir);
   s_isSvnSshRepository = boost::algorithm::starts_with(repoURL, "svn+ssh");

   userSettings().onChanged.connect(onUserSettingsChanged);

   return Success();
}

} // namespace svn
} // namespace modules
} //namespace session
} // namespace rstudio
