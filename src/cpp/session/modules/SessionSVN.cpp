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

#ifdef _WIN32
#include <windows.h>
#include <shlobj.h>
#include <shlwapi.h>
#endif

#include <boost/bind.hpp>
#include <boost/date_time.hpp>
#include <boost/regex.hpp>

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

#include <r/RExec.hpp>

#include "SessionVCS.hpp"
#include "vcs/SessionVCSUtils.hpp"
#include "SessionConsoleProcess.hpp"

using namespace core;
using namespace core::shell_utils;
using namespace session::modules::vcs_utils;
using namespace session::modules::console_process;

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

#ifndef _WIN32
ShellCommand svn()
{
   if (!s_svnExePath.empty())
   {
      FilePath exePath(s_svnExePath);
      return ShellCommand(exePath);
   }
   else
      return ShellCommand("svn");
}
#endif


#ifdef _WIN32
std::string svnBin()
{
   if (!s_svnExePath.empty())
   {
      return FilePath(s_svnExePath).absolutePathNative();
   }
   else
      return "svn.exe";
}
#endif


Error runSvn(const ShellArgs& args,
             const FilePath& workingDir,
             bool redirectStdErrToStdOut,
             core::system::ProcessResult* pResult)
{
   core::system::ProcessOptions options = procOptions();
   if (!workingDir.empty())
      options.workingDir = workingDir;
   options.redirectStdErrToStdOut = redirectStdErrToStdOut;
#ifdef _WIN32
   // NOTE: We use consoleio.exe here in order to make sure svn.exe password
   // prompting works properly
   options.createNewConsole = true;

   FilePath consoleIoPath = session::options().consoleIoPath();

   ShellArgs winArgs;
   winArgs << svnBin();
   winArgs << args.args();
   Error error = core::system::runProgram(consoleIoPath.absolutePathNative(),
                                          winArgs.args(),
                                          std::string(),
                                          options,
                                          pResult);
#else
   Error error = core::system::runCommand(svn() << args.args(),
                                          options,
                                          pResult);
#endif
   return error;
}

Error runSvn(const ShellArgs& args,
             bool redirectStdErrToStdOut,
             core::system::ProcessResult* pResult)
{
   return runSvn(args, FilePath(), redirectStdErrToStdOut, pResult);
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

std::vector<std::string> globalArgs(
      const std::string* const pUsername=NULL,
      const std::string* const pPassword=NULL,
      bool cacheCredentials=false)
{
   std::vector<std::string> args;
   //args.push_back("--trust-server-cert");

   if (pUsername)
   {
      args.push_back("--username");
      args.push_back(*pUsername);
   }
   if (pPassword)
   {
      args.push_back("--password");
      args.push_back(*pPassword);
   }

   if (!cacheCredentials)
      args.push_back("--no-auth-cache");
   return args;
}


core::Error createConsoleProc(const ShellArgs& args,
                              const boost::optional<FilePath>& workingDir,
                              const std::string& caption,
                              bool dialog,
                              boost::shared_ptr<ConsoleProcess>* ppCP)
{
   core::system::ProcessOptions options = procOptions();
#ifdef _WIN32
   options.detachProcess = true;
#endif
   if (!workingDir)
      options.workingDir = s_workingDir;
   else if (!workingDir.get().empty())
      options.workingDir = workingDir.get();

#ifdef _WIN32
   *ppCP = ConsoleProcess::create(svnBin(),
                                  args.args(),
                                  options,
                                  caption,
                                  dialog,
                                  console_process::InteractionPossible,
                                  console_process::kDefaultMaxOutputLines);
#else
   *ppCP = ConsoleProcess::create(svn() << args.args(),
                                  options,
                                  caption,
                                  dialog,
                                  console_process::InteractionPossible,
                                  console_process::kDefaultMaxOutputLines);
#endif

   (*ppCP)->onExit().connect(boost::bind(&enqueueRefreshEvent));

   return Success();
}

core::Error createConsoleProc(const ShellArgs& args,
                              const std::string& caption,
                              bool dialog,
                              boost::shared_ptr<ConsoleProcess>* ppCP)
{
   return createConsoleProc(args,
                            boost::optional<FilePath>(),
                            caption,
                            dialog,
                            ppCP);
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
   std::string whichSvn;
   Error error = r::exec::RFunction("Sys.which", "svn").call(&whichSvn);
   if (error)
   {
      LOG_ERROR(error);
      return FilePath();
   }
   else
   {
      return FilePath(whichSvn);
   }
}

bool initSvnBin()
{
   // get the svn bin dir from user settings if it is there
   s_svnExePath = userSettings().svnExePath().absolutePath();

   // if it wasn't provided in settings then make sure we can detect it
   if (s_svnExePath.empty())
      return !svn::detectedSvnExePath().empty();
   else
      return true;
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
   Error error = runSvn(ShellArgs() << "info",
                        workingDir,
                        true,
                        &result);
   if (error)
      return error;

   if (result.exitStatus == EXIT_SUCCESS)
   {
      // break the output into lines
      boost::char_separator<char> lineSep("\n");
      boost::tokenizer<boost::char_separator<char> > lines(result.stdOut,
                                                           lineSep);
      for (boost::tokenizer<boost::char_separator<char> >::iterator
           lineIter = lines.begin();
           lineIter != lines.end();
           ++lineIter)
      {
         std::string line = *lineIter;
         if (boost::algorithm::starts_with(line, "Repository Root:"))
         {
            http::Header header;
            http::parseHeader(line, &header);
            pSvnInfo->repositoryRoot = header.value;
            break;
         }
      }
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
      return FilePath(svnExeFilePath);
   else
      return FilePath();
#endif
}

std::string nonPathSvnBinDir()
{
   if (!s_svnExePath.empty())
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
   return module_context::resolveAliasedPath(value.get_str());
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

   core::system::ProcessResult result;
   error = runSvn(ShellArgs() << "revert" << globalArgs() << "-q" <<
                  "--depth" << "infinity" <<
                  "--" << paths,
                  true, &result);
   if (error)
      return error;

   pResponse->setResult(processResultToJson(result));

   return Success();
}

Error svnStatus(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   std::string stdOut, stdErr;
   int exitCode;
   Error error = runSvn(
         ShellArgs() << "status" << globalArgs() << "--xml" << "--ignore-externals",
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

            std::string status = topStatus(item, props);

            json::Object info;
            info["status"] = status;
            // TODO: escape path relative to <target>
            info["path"] = path;
            info["raw_path"] = module_context::createAliasedPath(
                  projects::projectContext().directory().childPath(path));
            info["changelist"] = changelist;
            results.push_back(info);
         }
      }
   }

   pResponse->setResult(results);

   return Success();
}

void maybeAttachPasswordManager(boost::shared_ptr<ConsoleProcess> pCP)
{
   std::string repoURL = repositoryRoot(s_workingDir);
   if (boost::algorithm::starts_with(repoURL, "svn+ssh"))
      s_pPasswordManager->attach(pCP);
}


Error svnUpdate(const json::JsonRpcRequest& request,
                json::JsonRpcResponse* pResponse)
{
   boost::shared_ptr<ConsoleProcess> pCP;
   Error error = createConsoleProc(ShellArgs() << "update" << globalArgs(),
                                   "SVN Update",
                                   true,
                                   &pCP);
   if (error)
      return error;


   maybeAttachPasswordManager(pCP);

   pResponse->setResult(pCP->toJson());

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

   boost::shared_ptr<ConsoleProcess> pCP;
   error = createConsoleProc(args,
                             "SVN Commit",
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

   std::string extArgs = "-U " + boost::lexical_cast<std::string>(contextLines);

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

   // TODO: implement size warning
   pResponse->setResult(stdOut);

   return Success();
}

Error svnApplyPatch(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   RefreshOnExit refreshOnExit;

   std::string path, patch;
   Error error = json::readParams(request.params,
                                  &path,
                                  &patch);
   if (error)
      return error;

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

Error history(int rev,
              const std::string& searchText,
              FilePath fileFilter,
              ShellArgs options,
              std::string* pOutput)
{
   ShellArgs args;
   args << "log";
   args << "--xml";

   args << options.args();

   if (rev > 0)
      args << "-r" << boost::lexical_cast<std::string>(rev) + ":1";

   if (!fileFilter.empty())
      args << fileFilter;

   return runSvn(args, pOutput);
}

Error svnHistoryCount(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   using namespace rapidxml;

   int rev;
   json::Value fileFilterJson;
   std::string searchText;
   Error error = json::readParams(request.params,
                                  &rev,
                                  &fileFilterJson,
                                  &searchText);
   if (error)
      return error;

   ShellArgs options;
   options << "-q";
   FilePath fileFilter = fileFilterPath(fileFilterJson);
   std::string output;
   error = history(rev, searchText, fileFilter, options, &output);
   if (error)
      return error;

   std::vector<char> buffer;
   xml_document<> doc;
   error = parseXml(output, &buffer, &doc);
   if (error)
      return error;

   int count = 0;

   xml_node<>* pLog = doc.first_node("log");
   if (pLog)
   {
      FOREACH_NODE(pLog, pEntry, "logentry")
      {
         count++;
      }
   }

   json::Object result;
   result["count"] = count;
   pResponse->setResult(result);

   return Success();
}

Error svnHistory(const json::JsonRpcRequest& request,
                 json::JsonRpcResponse* pResponse)
{
   using namespace boost::posix_time;
   using namespace rapidxml;

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
      return error;

   int limit = skip + maxentries;
   ShellArgs options;
   options << "--limit" << boost::lexical_cast<std::string>(limit);

   FilePath fileFilter = fileFilterPath(fileFilterJson);
   std::string output;
   error = history(rev, searchText, fileFilter, options, &output);
   if (error)
      return error;

   std::vector<char> buffer;
   xml_document<> doc;
   error = parseXml(output, &buffer, &doc);
   if (error)
      return error;
   xml_node<>* pLog = doc.first_node("log");

   const std::string NAME_REVISION = "revision";
   const std::string NAME_AUTHOR = "author";
   const std::string NAME_MSG = "msg";
   const std::string NAME_DATE = "date";
   const ptime epoch(boost::gregorian::date(1970,1,1));

   json::Array ids;
   json::Array authors;
   json::Array subjects;
   json::Array dates;

   int count = 0;
   FOREACH_NODE(pLog, pEntry, "logentry")
   {
      if (count > skip + maxentries)
         break;
      if (count++ < skip)
         continue;

      ids.push_back(attr_value(pEntry, NAME_REVISION));
      authors.push_back(string_utils::filterControlChars(node_value(pEntry, NAME_AUTHOR)));
      subjects.push_back(string_utils::filterControlChars(node_value(pEntry, NAME_MSG)));

      ptime date = boost::date_time::parse_delimited_time<ptime>(
            node_value(pEntry, NAME_DATE), 'T');
      time_duration::sec_type t = (date - epoch).total_seconds();
      dates.push_back(t);
   }

   json::Object result;
   result["id"] = ids;
   result["author"] = authors;
   result["subject"] = subjects;
   result["date"] = dates;

   pResponse->setResult(result);

   return Success();
}

Error svnShow(const json::JsonRpcRequest& request,
              json::JsonRpcResponse* pResponse)
{
   int revision;
   bool noSizeWarning;
   Error error = json::readParams(request.params, &revision, &noSizeWarning);
   if (error)
      return error;

   ShellArgs args;
   args << "diff" << "-c" << revision;

   std::string output;
   error = runSvn(args, &output);
   if (error)
      return error;

   pResponse->setResult(output);
   return Success();
}

Error svnShowFile(const json::JsonRpcRequest& request,
                  json::JsonRpcResponse* pResponse)
{
   // TODO: Implement

   pResponse->setResult("");
   return Success();
}

Error checkout(const std::string& url,
               const std::string dirName,
               const core::FilePath& parentPath,
               boost::shared_ptr<console_process::ConsoleProcess>* ppCP)
{
   Error error = createConsoleProc(ShellArgs() << "checkout" << url << dirName,
                                   parentPath,
                                   "SVN Checkout",
                                   true,
                                   ppCP);

   if (error)
      return error;

   // attach the password manager if this is an svn+ssh url
   if (boost::algorithm::starts_with(url, "svn+ssh"))
      s_pPasswordManager->attach(*ppCP);

   return Success();
}

bool promptForPassword(const std::string& prompt,
                       std::string* pPassword,
                       bool* pRemember)
{
   std::string rememberPrompt = "Remember password for this session";
   source_control::PasswordInput input;
   Error error = source_control::askForPassword(prompt,
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

Error initialize()
{
   // initialize password manager
   s_pPasswordManager.reset(new PasswordManager(
                               boost::regex("^(.+)password: $"),
                               boost::bind(promptForPassword, _1, _2, _3)));

   // install rpc methods
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "svn_add", svnAdd))
      (bind(registerRpcMethod, "svn_delete", svnDelete))
      (bind(registerRpcMethod, "svn_revert", svnRevert))
      (bind(registerRpcMethod, "svn_status", svnStatus))
      (bind(registerRpcMethod, "svn_update", svnUpdate))
      (bind(registerRpcMethod, "svn_commit", svnCommit))
      (bind(registerRpcMethod, "svn_diff_file", svnDiffFile))
      (bind(registerRpcMethod, "svn_apply_patch", svnApplyPatch))
      (bind(registerRpcMethod, "svn_history_count", svnHistoryCount))
      (bind(registerRpcMethod, "svn_history", svnHistory))
      (bind(registerRpcMethod, "svn_show", svnShow))
      (bind(registerRpcMethod, "svn_show_file", svnShowFile))
      ;
   Error error = initBlock.execute();
   if (error)
      return error;

   return Success();
}

Error initializeSvn(const core::FilePath& workingDir)
{
   s_workingDir = workingDir;

   // set s_svnExePath if it is provied in userSettings()
   initSvnBin();

   userSettings().onChanged.connect(onUserSettingsChanged);

   return Success();
}

} // namespace svn
} // namespace modules
} //namespace session
