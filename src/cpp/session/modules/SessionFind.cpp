/*
 * SessionFind.cpp
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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
#include <fstream>
#include <gsl/gsl>

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
#include <session/projects/SessionProjects.hpp>

#include <session/prefs/UserPrefs.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {   
namespace find {

namespace {

// This must be the same as MAX_COUNT in FindOutputPane.java
const size_t MAX_COUNT = 1000;

// Reflects the estimated current progress made in performing a replace
class LocalProgress : public boost::noncopyable
{
public:
   explicit LocalProgress(int max, int updateFrequency)
   {
      units_ = 0;
      max_ = max;
      updateFrequency_ = updateFrequency;
      updateIncrement_ = (updateFrequency * max_) / 100;
      nextUpdate_ = updateIncrement_;
   }

   void setUnits(int units)
   {
      units_ = units;
   }

   void addUnit()
   {
      units_++;
      if (units_ == nextUpdate_)
      {
         nextUpdate_ += updateIncrement_;
         notifyClient();
      }
   }

   int getUnits()
   {
      return units_;
   }

   int getMax()
   {
      return max_;
   }

private:

   void notifyClient()
   {
      json::Object data;
      data["max"] = max_;
      data["units"] = units_;
      module_context::enqueClientEvent(
            ClientEvent(client_events::kReplaceUpdated, data));
   }

   int units_;
   int max_;
   int updateFrequency_;
   int updateIncrement_;
   int nextUpdate_;
};

// Reflects the current set of Find results that are being
// displayed, in case they need to be re-fetched (i.e. browser
// refresh)
class FindInFilesState : public boost::noncopyable
{
public:

      explicit FindInFilesState() :
         regex_(false),
         running_(false),
         replace_(false),
         preview_(false),
         replaceRegex_(false)
      {
         replaceProgress_ = nullptr;
      }

      std::string handle() const
      {
         return handle_;
      }

      int resultCount() const
      {
         return gsl::narrow_cast<int>(files_.getSize());
      }

      bool isRunning() const
      {
         return running_;
      }

      bool replace()
      {
         return replace_;
      }

      bool preview()
      {
         return preview_;
      }

      bool replaceRegex()
      {
         return replaceRegex_;
      }

      std::string* searchPattern()
      {
         return &input_;
      }

      std::string* replacePattern()
      {
         return &replacePattern_;
      }

      LocalProgress* replaceProgress()
      {
         return replaceProgress_;
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

      bool addReplaceResult(const std::string& handle,
                            const json::Array& files,
                            const json::Array& lineNums,
                            const json::Array& contents,
                            const json::Array& matchOns,
                            const json::Array& matchOffs,
                            const json::Array& replaceMatchOns,
                            const json::Array& replaceMatchOffs)
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
         std::copy(replaceMatchOns.begin(), replaceMatchOns.end(),
                  std::back_inserter(replaceMatchOns_));
         std::copy(replaceMatchOffs.begin(), replaceMatchOffs.end(),
                   std::back_inserter(replaceMatchOffs_));
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

      void onReplaceBegin(const std::string& handle,
                          bool previewFlag,
                          const std::string& replacePattern,
                          bool asRegex, LocalProgress& progress)
      {
         if (handle_ == handle)
         {
            replace_ = true;
            preview_ = previewFlag;
            replacePattern_ = replacePattern;
            replaceRegex_ = asRegex;
            running_ = true;
            replaceProgress_ = &progress;
         }
      }

      void onReplaceEnd(const std::string& handle)
      {
         if (handle_ == handle)
         {
            onFindEnd(handle);
            replace_ = false;
            preview_ = false;
            replacePattern_.clear();
         }
      }

      void clear()
      {
         handle_ = std::string();
         files_.clear();
         lineNums_.clear();
         contents_.clear();
         matchOns_.clear();
         matchOffs_.clear();
         replace_ = false;
         preview_ = false;
         replacePattern_.clear();
         replaceMatchOns_.clear();
         replaceMatchOffs_.clear();
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
                                  "matchOff", &matchOffs_,
                                  "replaceMatchOn", &replaceMatchOns_,
                                  "replaceMatchOff", &replaceMatchOffs_);
         if (error)
            return error;

         if (files_.getSize() != lineNums_.getSize() || files_.getSize() != contents_.getSize())
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
         results["replaceMatchOn"] = replaceMatchOns_;
         results["replaceMatchOff"] = replaceMatchOffs_;
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
      bool replace_;
      bool preview_;
      bool replaceRegex_;
      std::string replacePattern_;
      json::Array replaceMatchOns_;
      json::Array replaceMatchOffs_;
      LocalProgress* replaceProgress_;
   };

   FindInFilesState& findResults()
   {
      static FindInFilesState* s_pFindResults = nullptr;
      if (s_pFindResults == nullptr)
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
               pMatchOn->push_back(json::Value(gsl::narrow_cast<int>(nUtf8CharactersProcessed)));
            else
               pMatchOff->push_back(json::Value(gsl::narrow_cast<int>(nUtf8CharactersProcessed)));
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

      Error processReplace(std::string* file,
                           int lineNum,
                           std::string* pContent,
                           json::Array* pReplaceMatchOn,
                           json::Array* pReplaceMatchOff,
                           std::string* pSearch,
                           std::string* pReplace,
                           LocalProgress* progress)
      {
         {
            size_t pos=0;
            if ((pos = file->find("~")) != std::string::npos)
               file->replace(pos, 1, session::options().userHomePath().getAbsolutePath());
         }
         const char* cfile = file->c_str();
         boost::shared_ptr<std::fstream> pStream(new std::fstream);
         pStream->open(cfile, std::fstream::in | std::fstream::out);

         if (!pStream->good())
            LOG_ERROR_MESSAGE(std::string("Could not open file ") + *file);
         else
         {
            std::string line;
            int currentLine=0;
            int seekPos=0;
            while (findResults().isRunning() && currentLine < lineNum && std::getline(*pStream, line))
            {
               ++currentLine;
               if (currentLine == lineNum)
               {
                  size_t linePos = line.find(*pSearch);
                  pReplaceMatchOn->push_back(json::Value(gsl::narrow_cast<int>(linePos)));
                  pReplaceMatchOff->push_back(json::Value(gsl::narrow_cast<int>(linePos) +
                                              gsl::narrow_cast<int>(pReplace->size())));
      
                  std::string newLine;
                  std::string replaceString(*pReplace);
                  if (findResults().preview())
                     replaceString.insert(0, *pSearch);
                  if (findResults().replaceRegex())
                     newLine = boost::regex_replace(line,
                                                    boost::regex(*pSearch),
                                                   replaceString);
                  else
                     newLine = line.replace(linePos, pSearch->size(), replaceString);
                  *pContent = newLine;
                  if (!findResults().preview())
                  {
                     pStream->seekg(seekPos);
                     try
                     {
                         pStream->write(newLine.c_str(), line.size());
                         progress->addUnit();
                     }
                     catch (const std::ios_base::failure& e)
                     {
                        std::string text("Failed to write to file ");
                        text.append(e.code().message());
                        LOG_ERROR_MESSAGE(text);
                     }
                  }
               }
               seekPos += line.size();
            }
         }
         pStream->close();

         return Success();
      }

      void onStdout(const core::system::ProcessOperations& ops, const std::string& data)
      {
         json::Array files;
         json::Array lineNums;
         json::Array contents;
         json::Array matchOns;
         json::Array matchOffs;
         json::Array replaceMatchOns;
         json::Array replaceMatchOffs;

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
               if (file.find("/renv/library/") != std::string::npos)
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
               json::Array replaceMatchOn, replaceMatchOff;

               processContents(&lineContents, &matchOn, &matchOff);
               if (findResults().replace())
               {
                  processReplace(&file, lineNum, &lineContents,
                                 &replaceMatchOn, &replaceMatchOff,
                                 findResults().searchPattern(),
                                 findResults().replacePattern(),
                                 findResults().replaceProgress());
               }

               files.push_back(json::Value(file));
               lineNums.push_back(json::Value(lineNum));
               contents.push_back(json::Value(lineContents));
               matchOns.push_back(matchOn);
               matchOffs.push_back(matchOff);
               replaceMatchOns.push_back(replaceMatchOn);
               replaceMatchOffs.push_back(replaceMatchOff);
               recordsToProcess--;
            }
         }

         if (nextLineStart)
         {
            stdOutBuf_.erase(0, nextLineStart);
         }

         if (files.getSize() > 0)
         {
            json::Object result;
            result["handle"] = handle();
            json::Object results;
            results["file"] = files;
            results["line"] = lineNums;
            results["lineValue"] = contents;
            results["matchOn"] = matchOns;
            results["matchOff"] = matchOffs;
            results["replaceMatchOn"] = replaceMatchOns;
            results["replaceMatchOff"] = replaceMatchOffs;
            result["results"] = results;

            findResults().addReplaceResult(handle(),
                                           files,
                                           lineNums,
                                           contents,
                                           matchOns,
                                           matchOffs,
                                           replaceMatchOns,
                                           replaceMatchOffs);

            if (!findResults().replace() || findResults().preview())
            {
               module_context::enqueClientEvent(
                        ClientEvent(client_events::kFindResult, result));
            }
            else
            {
               result["preview"] = findResults().preview();

               module_context::enqueClientEvent(
                       ClientEvent(client_events::kReplaceResult, result));
            }
         }

         if (recordsToProcess <= 0)
         {
            if (!findResults().replace())
               findResults().onFindEnd(handle());
            else
               findResults().onReplaceEnd(handle());
         }
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
         if (!tempFile_.isEmpty())
            tempFile_.removeIfExists();
      }

      bool firstDecodeError_;
      std::string encoding_;
      FilePath tempFile_;
      std::string stdOutBuf_;
      std::string handle_;
   };

   } // namespace

   core::Error retrieveFindReplaceResponse(json::JsonRpcResponse* pResponse,
         bool previewFlag, bool replaceFlag, std::string searchString, std::string replacePattern,
         bool asRegex, bool ignoreCase, bool replaceRegex, bool useGitIgnore,
         std::string directory, json::Array filePatterns, LocalProgress* progress)
   {
      if (previewFlag && !replaceFlag)
         LOG_ERROR_MESSAGE("replaceFlag must be true when previewFlag is true");

      core::system::ProcessOptions options;

      core::system::Options childEnv;
      core::system::environment(&childEnv);
      core::system::setenv(&childEnv, "GREP_COLOR", "01");
      core::system::setenv(&childEnv, "GREP_COLORS", "ne:fn=:ln=:se=:mt=01");
#ifdef _WIN32
      FilePath gnuGrepPath = session::options().gnugrepPath();
      core::system::addToPath(
               &childEnv,
               string_utils::utf8ToSystem(gnuGrepPath.getAbsolutePath()));
#endif
      options.environment = childEnv;

      // Put the grep pattern in a file
      FilePath tempFile = module_context::tempFile("rs_grep", "txt");
      std::shared_ptr<std::ostream> pStream;
      Error error = tempFile.openForWrite(pStream);
      if (error)
         return error;
      std::string encoding = projects::projectContext().hasProject() ?
                             projects::projectContext().defaultEncoding() :
                             prefs::userPrefs().defaultEncoding();
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
      shell_utils::ShellCommand cmd(gnuGrepPath.completePath("grep"));
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

      for (json::Value filePattern : filePatterns)
      {
         cmd << "--include=" + filePattern.getString();
      }

      cmd << shell_utils::EscapeFilesOnly << "--" << shell_utils::EscapeAll;
      
      // Filepaths received from the client will be UTF-8 encoded;
      // convert to system encoding here.
      FilePath dirPath = module_context::resolveAliasedPath(directory);
      cmd << string_utils::utf8ToSystem(dirPath.getAbsolutePath());

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
      if (replaceFlag)
         findResults().onReplaceBegin(ptrGrepOp->handle(),
                                      previewFlag,
                                      replacePattern,
                                      replaceRegex,
                                      *progress);
      pResponse->setResult(ptrGrepOp->handle());

      return Success();
   }

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

   error = retrieveFindReplaceResponse(pResponse,
      false, false, searchString, std::string(),
      asRegex, ignoreCase, false, false,
      directory, filePatterns, nullptr);

   if (error)
      return error;

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

core::Error previewReplace(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   std::string searchString;
   std::string replacePattern;
   bool asRegex, ignoreCase, replaceRegex, useGitIgnore = false;
   std::string directory;
   json::Array filePatterns;

   Error error = json::readParams(request.params,
                                  &searchString,
                                  &asRegex,
                                  &ignoreCase,
                                  &directory,
                                  &filePatterns,
                                  &replacePattern,
                                  &replaceRegex,
                                  &useGitIgnore);
   if (error)
      return error;
   if (!replaceRegex)
      LOG_ERROR_MESSAGE("Replace Regex must be true during preview");

   error = retrieveFindReplaceResponse(pResponse,
      true, true, searchString, replacePattern,
      asRegex, ignoreCase, replaceRegex, useGitIgnore,
      directory, filePatterns, nullptr);

   if (error)
      return error;

   return Success();
}

core::Error completeReplace(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   std::string searchString;
   std::string replacePattern;
   bool asRegex, ignoreCase, replaceRegex, useGitIgnore = false;
   std::string directory;
   json::Array filePatterns;
   // only used to estimate progress
   int originalFindCount;

   Error error = json::readParams(request.params,
                                  &searchString,
                                  &asRegex,
                                  &ignoreCase,
                                  &directory,
                                  &filePatterns,
                                  &originalFindCount,
                                  &replacePattern,
                                  &replaceRegex,
                                  &useGitIgnore);
   if (error)
      return error;

   LocalProgress* progress = new LocalProgress(originalFindCount, 25);

   error = retrieveFindReplaceResponse(pResponse,
         false, true, searchString, replacePattern,
         asRegex, ignoreCase, replaceRegex, useGitIgnore,
         directory, filePatterns, progress);

   if (error)
      return error;

   return Success();
}

core::Error stopReplace(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   std::string handle;
   Error error = json::readParams(request.params, &handle);
   if (error)
      return error;

   findResults().onReplaceEnd(handle);

   return Success();
}

void onSuspend(core::Settings* pSettings)
{
   std::ostringstream os;
   findResults().asJson().write(os);
   pSettings->set("find-in-files-state", os.str());
}

void onResume(const core::Settings& settings)
{
   std::string state = settings.get("find-in-files-state");
   if (!state.empty())
   {
      json::Value stateJson;
      if (stateJson.parse(state))
      {
         LOG_WARNING_MESSAGE("invalid find results state json");
         return;
      }

      Error error = findResults().readFromJson(stateJson.getObject());
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
      (bind(registerRpcMethod, "clear_find_results", clearFindResults))
      (bind(registerRpcMethod, "preview_replace", previewReplace))
      (bind(registerRpcMethod, "complete_replace", completeReplace))
      (bind(registerRpcMethod, "stop_replace", stopReplace));
   return initBlock.execute();
}

} // namespace find
} // namespace modules
} // namespace session
} // namespace rstudio
