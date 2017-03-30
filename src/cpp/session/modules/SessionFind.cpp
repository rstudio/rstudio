/*
 * SessionFind.cpp
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

#include <r/RUtil.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionUserSettings.hpp>
#include <session/projects/SessionProjects.hpp>

using namespace rstudio::core;

namespace rstudio {
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

   int resultCount() const
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
                    const std::string& input,
                    const std::string& path,
                    bool asRegex)
   {
      handle_ = handle;
      input_ = input;
      path_ = path;
      regex_ = asRegex;
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
                                     "path", &path_,
                                     "regex", &regex_,
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
      obj["path"] = path_;
      obj["regex"] = regex_;

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
   std::string path_;
   bool regex_;
   json::Array files_;
   json::Array lineNums_;
   json::Array contents_;
   json::Array matchOns_;
   json::Array matchOffs_;
   bool running_;
};

FindInFilesState& findResults()
{
   static FindInFilesState* s_pFindResults = NULL;
   if (s_pFindResults == NULL)
      s_pFindResults = new FindInFilesState();
   return *s_pFindResults;
}

class GrepOperation : public boost::enable_shared_from_this<GrepOperation>
{
public:
   static boost::shared_ptr<GrepOperation> create(const std::string& encoding,
                                                  const FilePath& tempFile)
   {
      return boost::shared_ptr<GrepOperation>(new GrepOperation(encoding,
                                                                tempFile));
   }

private:
   GrepOperation(const std::string& encoding,
                 const FilePath& tempFile)
      : firstDecodeError_(true), encoding_(encoding), tempFile_(tempFile)
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

   std::string decode(const std::string& encoded)
   {
      if (encoded.empty())
         return encoded;

      std::string decoded;
      Error error = r::util::iconvstr(encoded, encoding_, "UTF-8", true,
                                      &decoded);

      // Log error, but only once per grep operation
      if (error && firstDecodeError_)
      {
         firstDecodeError_ = false;
         LOG_ERROR(error);
      }

      return decoded;
   }

   void processContents(std::string* pContent,
                        json::Array* pMatchOn,
                        json::Array* pMatchOff)
   {
      // initialize some state
      std::string decodedLine;
      std::size_t nUtf8CharactersProcessed = 0;
      
      const char* inputPos = pContent->c_str();
      const char* end = inputPos + pContent->size();
      
      boost::cmatch match;
      while (regex_utils::search(inputPos, match, boost::regex("\x1B\\[(\\d\\d)?m(\x1B\\[K)?")))
      {
         // decode the current match, and append it
         std::string matchedString(inputPos, inputPos + match.position());
         std::string decoded = decode(matchedString);
         
         // append and update
         decodedLine.append(decoded);
         inputPos += match.position() + match.length();
         
         // count the number of UTF-8 characters processed
         std::size_t charSize;
         Error error = string_utils::utf8Distance(decoded.begin(),
                                                  decoded.end(),
                                                  &charSize);
         if (error)
            charSize = decoded.size();
         nUtf8CharactersProcessed += charSize;

         // update the match state
         if (match[1] == "01")
            pMatchOn->push_back(static_cast<int>(nUtf8CharactersProcessed));
         else
            pMatchOff->push_back(static_cast<int>(nUtf8CharactersProcessed));
      }
      
      if (inputPos != end)
         decodedLine.append(decode(std::string(inputPos, end)));

      if (decodedLine.size() > 300)
      {
         decodedLine = decodedLine.erase(300);
         decodedLine.append("...");
      }

      *pContent = decodedLine;
   }

   void onStdout(const core::system::ProcessOperations& ops, const std::string& data)
   {
      json::Array files;
      json::Array lineNums;
      json::Array contents;
      json::Array matchOns;
      json::Array matchOffs;

      int recordsToProcess = MAX_COUNT + 1 - findResults().resultCount();
      if (recordsToProcess < 0)
         recordsToProcess = 0;

      std::string websiteOutputDir = module_context::websiteOutputDir();
      if (!websiteOutputDir.empty())
         websiteOutputDir = "/" + websiteOutputDir + "/";

      stdOutBuf_.append(data);
      size_t nextLineStart = 0;
      size_t pos = -1;
      while (recordsToProcess &&
             std::string::npos != (pos = stdOutBuf_.find('\n', pos + 1)))
      {
         std::string line = stdOutBuf_.substr(nextLineStart, pos - nextLineStart);
         nextLineStart = pos + 1;

         boost::smatch match;
         if (regex_utils::match(line, match, boost::regex("^((?:[a-zA-Z]:)?[^:]+):(\\d+):(.*)")))
         {
            std::string file = module_context::createAliasedPath(
                  FilePath(string_utils::systemToUtf8(match[1])));

            if (file.find("/.Rproj.user/") != std::string::npos)
               continue;
            if (file.find("/.git/") != std::string::npos)
               continue;
            if (file.find("/.svn/") != std::string::npos)
               continue;
            if (file.find("/packrat/lib/") != std::string::npos)
               continue;
            if (file.find("/packrat/src/") != std::string::npos)
               continue;
            if (file.find("/.Rhistory") != std::string::npos)
               continue;

            if (!websiteOutputDir.empty() &&
                file.find(websiteOutputDir) != std::string::npos)
               continue;

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

      if (files.size() > 0)
      {
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
      }

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

   bool firstDecodeError_;
   std::string encoding_;
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
   core::system::setenv(&childEnv, "GREP_COLORS", "ne:fn=:ln=:se=:mt=01");
#ifdef _WIN32
   FilePath gnuGrepPath = session::options().gnugrepPath();
   core::system::addToPath(
            &childEnv,
            string_utils::utf8ToSystem(gnuGrepPath.absolutePath()));
#endif
   options.environment = childEnv;

   // Put the grep pattern in a file
   FilePath tempFile = module_context::tempFile("rs_grep", "txt");
   boost::shared_ptr<std::ostream> pStream;
   error = tempFile.open_w(&pStream);
   if (error)
      return error;
   std::string encoding = projects::projectContext().hasProject() ?
                          projects::projectContext().defaultEncoding() :
                          userSettings().defaultEncoding();
   std::string encodedString;
   error = r::util::iconvstr(searchString,
                             "UTF-8",
                             encoding,
                             false,
                             &encodedString);
   if (error)
   {
      LOG_ERROR(error);
      encodedString = searchString;
   }

   *pStream << encodedString << std::endl;
   pStream.reset(); // release file handle

   boost::shared_ptr<GrepOperation> ptrGrepOp = GrepOperation::create(encoding,
                                                                      tempFile);
   core::system::ProcessCallbacks callbacks =
                                       ptrGrepOp->createProcessCallbacks();

#ifdef _WIN32
   shell_utils::ShellCommand cmd(gnuGrepPath.complete("grep"));
#else
   shell_utils::ShellCommand cmd("grep");
#endif
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

   findResults().onFindBegin(ptrGrepOp->handle(),
                             searchString,
                             directory,
                             asRegex);
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
   using boost::bind;
   using namespace session::module_context;

   // register suspend handler
   addSuspendHandler(SuspendHandler(bind(onSuspend, _2), onResume));

   // install handlers
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
} // namespace rstudio
