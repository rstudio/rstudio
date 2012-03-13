/*
 * SessionFind.cpp
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

#include "SessionFind.hpp"

#include <algorithm>

#include <boost/algorithm/string.hpp>
#include <boost/bind.hpp>
#include <boost/enable_shared_from_this.hpp>

#include <core/Exec.hpp>
#include <core/StringUtils.hpp>
#include <core/system/Environment.hpp>
#include <core/system/Process.hpp>
#include <core/system/ShellUtils.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

using namespace core;

namespace session {
namespace modules {   
namespace find {

namespace {

// This must be the same as MAX_COUNT in FindOutputPane.java
const size_t MAX_COUNT = 1000;

// Reflects the current set of Find results that are being
// displayed, in case they need to be re-fetched (i.e. browser
// refresh)
class FindInFilesState : public boost::noncopyable
{
public:

   explicit FindInFilesState() : running_(false)
   {
   }

   std::string handle() const
   {
      return handle_;
   }

   size_t resultCount() const
   {
      return files_.size();
   }

   bool isRunning() const
   {
      return running_;
   }

   bool addResult(const std::string& handle,
                  const json::Array& files,
                  const json::Array& lineNums,
                  const json::Array& contents,
                  const json::Array& matchOns,
                  const json::Array& matchOffs)
   {
      if (handle_.empty())
         handle_ = handle;
      else if (handle_ != handle)
         return false;

      std::copy(files.begin(), files.end(), std::back_inserter(files_));
      std::copy(lineNums.begin(), lineNums.end(), std::back_inserter(lineNums_));
      std::copy(contents.begin(), contents.end(), std::back_inserter(contents_));
      std::copy(matchOns.begin(), matchOns.end(), std::back_inserter(matchOns_));
      std::copy(matchOffs.begin(), matchOffs.end(), std::back_inserter(matchOffs_));
      return true;
   }

   void onFindBegin(const std::string& handle,
                    const std::string& input)
   {
      handle_ = handle;
      input_ = input;
      running_ = true;
   }

   void onFindEnd(const std::string& handle)
   {
      if (handle_ == handle)
         running_ = false;
   }

   void clear()
   {
      handle_ = std::string();
      files_.clear();
      lineNums_.clear();
      contents_.clear();
      matchOns_.clear();
      matchOffs_.clear();
   }

   Error readFromJson(const json::Object& asJson)
   {
      json::Object results;
      Error error = json::readObject(asJson,
                                     "handle", &handle_,
                                     "input", &input_,
                                     "results", &results,
                                     "running", &running_);
      if (error)
         return error;

      error = json::readObject(results,
                               "file", &files_,
                               "line", &lineNums_,
                               "lineValue", &contents_,
                               "matchOn", &matchOns_,
                               "matchOff", &matchOffs_);
      if (error)
         return error;

      if (files_.size() != lineNums_.size() || files_.size() != contents_.size())
      {
         files_.clear();
         lineNums_.clear();
         contents_.clear();
      }

      return Success();
   }

   json::Object asJson()
   {
      json::Object obj;
      obj["handle"] = handle_;
      obj["input"] = input_;

      json::Object results;
      results["file"] = files_;
      results["line"] = lineNums_;
      results["lineValue"] = contents_;
      results["matchOn"] = matchOns_;
      results["matchOff"] = matchOffs_;
      obj["results"] = results;

      obj["running"] = running_;

      return obj;
   }

private:
   std::string handle_;
   std::string input_;
   json::Array files_;
   json::Array lineNums_;
   json::Array contents_;
   json::Array matchOns_;
   json::Array matchOffs_;
   bool running_;
};

FindInFilesState& findResults()
{
   static FindInFilesState s_findResults;
   return s_findResults;
}

class GrepOperation : public boost::enable_shared_from_this<GrepOperation>
{
public:
   static boost::shared_ptr<GrepOperation> create(const FilePath& tempFile)
   {
      return boost::shared_ptr<GrepOperation>(new GrepOperation(tempFile));
   }

private:
   GrepOperation(const FilePath& tempFile)
      : tempFile_(tempFile)
   {
      handle_ = core::system::generateUuid(false);
   }

public:
   std::string handle() const
   {
      return handle_;
   }

   core::system::ProcessCallbacks createProcessCallbacks()
   {
      core::system::ProcessCallbacks callbacks;
      callbacks.onContinue = boost::bind(&GrepOperation::onContinue,
                                         shared_from_this(),
                                         _1);
      callbacks.onStdout = boost::bind(&GrepOperation::onStdout,
                                       shared_from_this(),
                                       _1, _2);
      callbacks.onStderr = boost::bind(&GrepOperation::onStderr,
                                       shared_from_this(),
                                       _1, _2);
      callbacks.onExit = boost::bind(&GrepOperation::onExit,
                                     shared_from_this(),
                                     _1);
      return callbacks;
   }

private:
   bool onContinue(const core::system::ProcessOperations& ops) const
   {
      return findResults().isRunning() && findResults().handle() == handle();
   }

   void processContents(std::string* pContent,
                        json::Array* pMatchOn,
                        json::Array* pMatchOff)
   {
      using namespace boost;

      smatch match;
      while (regex_search(*pContent, match, regex("\x1B\\[(\\d\\d)m")))
      {
         int pos = static_cast<int>(match.position());
         if (match[1] == "01")
            pMatchOn->push_back(pos);
         else
            pMatchOff->push_back(pos);

         *pContent = pContent->erase(pos, match.length());
      }

      if (pContent->size() > 100)
      {
         *pContent = pContent->erase(100);
         pContent->append("...");
      }
   }

   void onStdout(const core::system::ProcessOperations& ops, const std::string& data)
   {
      json::Array files;
      json::Array lineNums;
      json::Array contents;
      json::Array matchOns;
      json::Array matchOffs;

      size_t recordsToProcess = MAX_COUNT + 1 - findResults().resultCount();
      if (recordsToProcess < 0)
         recordsToProcess = 0;

      stdOutBuf_.append(data);
      size_t nextLineStart = 0;
      size_t pos = -1;
      while (recordsToProcess &&
             std::string::npos != (pos = stdOutBuf_.find('\n', pos + 1)))
      {
         std::string line = stdOutBuf_.substr(nextLineStart, pos - nextLineStart);
         nextLineStart = pos + 1;

         boost::smatch match;
         if (boost::regex_match(line, match, boost::regex("^([^:]+):(\\d+):(.*)")))
         {
            std::string file = module_context::createAliasedPath(
                  FilePath(string_utils::systemToUtf8(match[1])));
            int lineNum = safe_convert::stringTo<int>(std::string(match[2]), -1);
            std::string lineContents = match[3];
            boost::algorithm::trim(lineContents);
            json::Array matchOn, matchOff;
            processContents(&lineContents, &matchOn, &matchOff);

            files.push_back(file);
            lineNums.push_back(lineNum);
            contents.push_back(lineContents);
            matchOns.push_back(matchOn);
            matchOffs.push_back(matchOff);

            recordsToProcess--;
         }
      }

      if (nextLineStart)
      {
         stdOutBuf_.erase(0, nextLineStart);
      }

      json::Object result;
      result["handle"] = handle();
      json::Object results;
      results["file"] = files;
      results["line"] = lineNums;
      results["lineValue"] = contents;
      results["matchOn"] = matchOns;
      results["matchOff"] = matchOffs;
      result["results"] = results;

      findResults().addResult(handle(),
                              files,
                              lineNums,
                              contents,
                              matchOns,
                              matchOffs);

      module_context::enqueClientEvent(
            ClientEvent(client_events::kFindResult, result));

      if (recordsToProcess <= 0)
         findResults().onFindEnd(handle());
   }

   void onStderr(const core::system::ProcessOperations& ops, const std::string& data)
   {
      LOG_ERROR_MESSAGE("grep: " + data);
   }

   void onExit(int exitCode)
   {
      findResults().onFindEnd(handle());
      module_context::enqueClientEvent(
            ClientEvent(client_events::kFindOperationEnded, handle()));
      if (!tempFile_.empty())
         tempFile_.removeIfExists();
   }

   FilePath tempFile_;
   std::string stdOutBuf_;
   std::string handle_;
};

} // namespace

core::Error beginFind(const json::JsonRpcRequest& request,
                      json::JsonRpcResponse* pResponse)
{
   std::string searchString;
   bool asRegex, ignoreCase;
   std::string directory;
   json::Array filePatterns;

   Error error = json::readParams(request.params,
                                  &searchString,
                                  &asRegex,
                                  &ignoreCase,
                                  &directory,
                                  &filePatterns);
   if (error)
      return error;

   core::system::ProcessOptions options;

   core::system::Options childEnv;
   core::system::environment(&childEnv);
   core::system::setenv(&childEnv, "GREP_COLOR", "01");
#ifdef _WIN32
   core::system::addToPath(
            &childEnv,
            string_utils::utf8ToSystem(
               session::options().gnugrepPath().absolutePath()));
#endif
   options.environment = childEnv;

   // TODO: Encode the pattern using the project encoding

   // Put the grep pattern in a file
   FilePath tempFile = module_context::tempFile("rs_grep", "txt");
   boost::shared_ptr<std::ostream> pStream;
   error = tempFile.open_w(&pStream);
   if (error)
      return error;
   *pStream << searchString << std::endl;
   pStream.reset(); // release file handle

   boost::shared_ptr<GrepOperation> ptrGrepOp = GrepOperation::create(tempFile);
   core::system::ProcessCallbacks callbacks =
                                       ptrGrepOp->createProcessCallbacks();

   shell_utils::ShellCommand cmd("grep");
   cmd << "-rHn" << "--binary-files=without-match" << "--color=always";
#ifndef _WIN32
   cmd << "--devices=skip";
#endif

   if (ignoreCase)
      cmd << "-i";

   // Use -f to pass pattern via file, so we don't have to worry about
   // escaping double quotes, etc.
   cmd << "-f";
   cmd << tempFile;
   if (!asRegex)
      cmd << "-F";

   BOOST_FOREACH(json::Value filePattern, filePatterns)
   {
      cmd << "--include=" + filePattern.get_str();
   }

   cmd << shell_utils::EscapeFilesOnly << "--" << shell_utils::EscapeAll;
   cmd << module_context::resolveAliasedPath(directory);

   // Clear existing results
   findResults().clear();

   error = module_context::processSupervisor().runCommand(cmd,
                                                          options,
                                                          callbacks);
   if (error)
      return error;

   findResults().onFindBegin(ptrGrepOp->handle(), searchString);
   pResponse->setResult(ptrGrepOp->handle());

   return Success();
}

core::Error stopFind(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   std::string handle;
   Error error = json::readParams(request.params, &handle);
   if (error)
      return error;

   findResults().onFindEnd(handle);

   return Success();
}

core::Error clearFindResults(const json::JsonRpcRequest& request,
                             json::JsonRpcResponse* pResponse)
{
   findResults().clear();
   return Success();
}

void onSuspend(core::Settings* pSettings)
{
   std::ostringstream os;
   json::write(findResults().asJson(), os);
   pSettings->set("find-in-files-state", os.str());
}

void onResume(const core::Settings& settings)
{
   std::string state = settings.get("find-in-files-state");
   if (!state.empty())
   {
      json::Value stateJson;
      if (!json::parse(state, &stateJson))
      {
         LOG_WARNING_MESSAGE("invalid find results state json");
         return;
      }

      Error error = findResults().readFromJson(stateJson.get_obj());
      if (error)
         LOG_ERROR(error);
   }
}

json::Object findInFilesStateAsJson()
{
   return findResults().asJson();
}

core::Error initialize()
{
   using namespace session::module_context;

   // register suspend handler
   addSuspendHandler(SuspendHandler(onSuspend, onResume));

   // install handlers
   using boost::bind;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "begin_find", beginFind))
      (bind(registerRpcMethod, "stop_find", stopFind))
      (bind(registerRpcMethod, "clear_find_results", clearFindResults));
   return initBlock.execute();
}

} // namespace find
} // namespace modules
} // namespace session
