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

   LocalProgress(int totalReplaceCount, int updateFrequency) :
      totalReplaceCount_(totalReplaceCount)
   {
      replacedCount_ = 0;
      updateFrequency_ = updateFrequency;
      updateIncrement_ = static_cast<int>((updateFrequency_ * totalReplaceCount_) / 100);
      if (updateIncrement_ < 1)
         updateIncrement_ = 1;
      nextUpdate_ = updateIncrement_;
   }

   void addUnits(int num)
   {
      replacedCount_ += num;
      while (replacedCount_ >= nextUpdate_)
      {
         nextUpdate_ += updateIncrement_;
         if (nextUpdate_ > totalReplaceCount_)
            nextUpdate_ = totalReplaceCount_;
         notifyClient();
         // prevent infinite loop when totalReplaceCount is reached
         if (replacedCount_ >= totalReplaceCount_)
            break;
      }
   }

private:

   void notifyClient()
   {
      json::Object data;
      data["totalReplaceCount"] = totalReplaceCount_;
      data["replacedCount"] = replacedCount_;
      module_context::enqueClientEvent(
            ClientEvent(client_events::kReplaceUpdated, data));
   }

   int replacedCount_;
   const int totalReplaceCount_;
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
      ignoreCase_(false),
      running_(false),
      replace_(false),
      preview_(false),
      replaceRegex_(false),
      pReplaceProgress_(nullptr)
   {
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

   bool regex()
   {
      return regex_;
   }

   bool ignoreCase()
   {
      return ignoreCase_;
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

   std::string searchPattern()
   {
      return input_;
   }

   std::string replacePattern()
   {
      return replacePattern_;
   }

   LocalProgress* replaceProgress()
   {
      return pReplaceProgress_;
   }

   bool addResult(const std::string& handle,
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
                    bool asRegex,
                    bool ignoreCase)
   {
      handle_ = handle;
      input_ = input;
      path_ = path;
      regex_ = asRegex;
      ignoreCase_ = ignoreCase;
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
                       bool asRegex,
                       LocalProgress* pProgress)
   {
      if (handle_ == handle)
      {
         replace_ = true;
         preview_ = previewFlag;
         replacePattern_ = replacePattern;
         replaceRegex_ = asRegex;
         running_ = true;
         pReplaceProgress_ = pProgress;
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
         pReplaceProgress_ = nullptr;
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
      pReplaceProgress_ = nullptr;
   }

   Error readFromJson(const json::Object& asJson)
   {
      json::Object results;
      Error error = json::readObject(asJson,
                                     "handle", &handle_,
                                     "input", &input_,
                                     "path", &path_,
                                     "regex", &regex_,
                                     "ignoreCase", &ignoreCase_,
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
      obj["ignoreCase"] = ignoreCase_;

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
   bool ignoreCase_;
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
   LocalProgress* pReplaceProgress_;
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
   struct LineInfo
   {
      std::string leadingWhitespace;
      std::string trailingWhitespace;
      std::string decodedPreview;
      std::string decodedContents;
      std::string encodedContents;
   };

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

   void addReplaceErrorMessage(const std::string& contents,
                               std::set<std::string>* pErrorSet,
                               json::Array* pReplaceMatchOn,
                               json::Array* pReplaceMatchOff,
                               bool* pSuccessFlag)
   {
      pErrorSet->insert(contents);
      pReplaceMatchOn->push_back(json::Value(gsl::narrow_cast<int>(-1)));
      pReplaceMatchOff->push_back(json::Value(gsl::narrow_cast<int>(-1)));
      *pSuccessFlag = false;
   }

   void addNewLine(std::string& str)
   {
      str.append("\n");
#ifdef _WIN32
      string_utils::convertLineEndings(str, string_utils::LineEndingWindows);
#endif
   }

   void adjustForPreview(std::string* contents)
   {
      if (contents->size() > 300)
      {
         *contents = contents->erase(300);
         contents->append("...");
      }
   }

   Error completeFileReplace()
   {
      if (fileSuccess_)
      {
         if (!currentFile_.empty() &&
             !tempReplaceFile_.getAbsolutePath().empty() &&
             outputStream_->good())
         {
            std::string line;
            while (std::getline(*inputStream_, line))
            {
               addNewLine(line);
               outputStream_->write(line.c_str(), line.size());
            }
            outputStream_->flush();
            inputStream_.reset();
            outputStream_.reset();
            Error error = tempReplaceFile_.move(FilePath(currentFile_));
            return error;
         }
      }
      return Success();
   }

   void processContents(std::string* pContent,
                        std::string* pFullLineContent,
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

      *pFullLineContent = decodedLine;
      adjustForPreview(&decodedLine);
      *pContent = decodedLine;
   }

   Error initializeFileForReplace(FilePath file)
   {
      Error error;
      fileSuccess_ = false;
      if (!findResults().preview())
      {
         tempReplaceFile_ =  module_context::tempFile("replace", "txt");
         error = tempReplaceFile_.openForWrite(outputStream_);
      }
      if (!error)
         error = file.openForRead(inputStream_);
      if (!error)
      {
         fileSuccess_ = true;
         inputLineNum_ = 0;
         currentFile_ = file.getAbsolutePath();
      }
      return error;
   }

   void substractOffsetIntegerToJsonArray(
       json::Value newValue, int offset, json::Array* pJsonArray)
   {
      // make sure negative values (errors) don't become positve
      if (newValue.getInt() < 0)
         newValue = json::Value(newValue.getInt() - offset);
      pJsonArray->push_back(json::Value(
                            gsl::narrow_cast<int>(newValue.getInt() + offset)));
   }

   void substractOffsetIntegersToJsonArray(
         json::Array values, int offset, json::Array* pJsonArray)
   {
      for (json::Value value : values)
         substractOffsetIntegerToJsonArray(value, offset, pJsonArray);
   }

   Error encodeAndWriteLineToFile(
      const std::string& decodedLine, const std::string& lineLeftContents,
      const std::string& lineRightContents)
   {
      std::string encodedNewLine;
      Error error = r::util::iconvstr(decodedLine,
         "UTF-8",
         encoding_,
         false,
         &encodedNewLine);
      encodedNewLine.insert(0, lineLeftContents);
      encodedNewLine.insert(encodedNewLine.length(), lineRightContents);
      addNewLine(encodedNewLine);

      if (error)
         return error;
      else
      {
         try
         {
            outputStream_->write(encodedNewLine.c_str(), encodedNewLine.size());
            outputStream_->flush();
         }
         catch (const std::ios_base::failure& e)
         {
            error = systemError(e.code().value(), ERROR_LOCATION);
         }
      }
      return error;
   }

   Error processReplace(const int& lineNum,
                        const json::Array& matchOnArray,
                        const json::Array& matchOffArray,
                        LineInfo* pLineInfo,
                        json::Array* pReplaceMatchOn,
                        json::Array* pReplaceMatchOff,
                        std::set<std::string>* pErrorMessage)
   {
      std::string line;
      const std::string searchPattern = findResults().searchPattern();
      const std::string replacePattern = findResults().replacePattern();
      LocalProgress* pProgress = findResults().replaceProgress();
      while (findResults().isRunning() &&
             inputLineNum_ < lineNum && std::getline(*inputStream_, line))
      {
         bool lineSuccess = true;
         ++inputLineNum_;
         // write every line prior to our match to file
         if (inputLineNum_ != lineNum)
         {
            if (!findResults().preview())
            {
               addNewLine(line);
               outputStream_->write(line.c_str(), line.size());
            }
         }
         else // perform replace
         {
            int pos = gsl::narrow_cast<int>(matchOnArray.getSize()) - 1;
            std::string newLine;
            while (pos > -1)
            {
               const size_t matchOn =
                  static_cast<size_t>(matchOnArray.getValueAt(static_cast<size_t>(pos)).getInt());
               const size_t matchOff =
                  static_cast<size_t>(matchOffArray.getValueAt(static_cast<size_t>(pos)).getInt());
               const size_t matchSize = matchOff - matchOn;
               size_t replaceMatchOff = matchOff;
               Error error;
               Replacer replacer(findResults().ignoreCase());
               std::string newLine(pLineInfo->decodedContents);

               // if previewing, we need to display the original and replacement text
               if (findResults().preview())
               {
                  std::string replaceString(replacePattern);
                  std::string previewLine(newLine);
                  error = replacer.replaceRegexWithRegex(matchOn, matchOff, searchPattern,
                     replacePattern, &previewLine, &replaceMatchOff);
                  if (!error)
                  {
                     replaceString = previewLine.substr(matchOn, (replaceMatchOff - matchOn));
                     replaceString.insert(0, pLineInfo->decodedContents.substr(matchOn, matchSize));
                     replacer.replaceLiteralWithLiteral(matchOn, matchOff, replaceString, &newLine,
                        &replaceMatchOff);
                  }
                  else
                  {
                     addReplaceErrorMessage(error.asString(), pErrorMessage, pReplaceMatchOn,
                        pReplaceMatchOff, &lineSuccess);
                     return error;
                  }
               }
               else // perform replace
               {
                  pProgress->addUnits(1);
                  if (findResults().replaceRegex())
                     error = replacer.replaceRegexWithRegex(matchOn, matchOff, searchPattern,
                        replacePattern, &newLine, &replaceMatchOff);
                  else if (findResults().regex())
                     error = replacer.replaceRegexWithLiteral(matchOn, matchOff, searchPattern,
                        replacePattern, &newLine, &replaceMatchOff);
                  else
                     replacer.replaceLiteralWithLiteral(matchOn, matchOff, replacePattern, &newLine,
                        &replaceMatchOff);

                  if (error)
                     addReplaceErrorMessage(error.asString(), pErrorMessage, pReplaceMatchOn,
                        pReplaceMatchOff, &lineSuccess);
               }

               // Handle side-effects when replace is successful
               if (lineSuccess)
               {
                  pLineInfo->decodedContents = newLine;

                  // if multiple replaces in line, readjust previous match numbers
                  size_t replaceSize = replaceMatchOff - matchOn;
                  if (pReplaceMatchOn->getSize() > 0 &&
                      matchSize != replaceSize)
                  {
                     json::Array tempMatchOn(*pReplaceMatchOn);
                     json::Array tempMatchOff(*pReplaceMatchOff);
                     pReplaceMatchOn->clear();
                     pReplaceMatchOff->clear();

                     // put back in reverse order for the frontend
                     int offset(gsl::narrow_cast<int>(replaceSize - matchSize));
                     pReplaceMatchOn->push_back(json::Value(gsl::narrow_cast<int>(matchOn)));
                     pReplaceMatchOff->push_back(json::Value(gsl::narrow_cast<int>(replaceMatchOff)));
                     substractOffsetIntegersToJsonArray(tempMatchOn, offset, pReplaceMatchOn);
                     substractOffsetIntegersToJsonArray(tempMatchOff, offset, pReplaceMatchOff);
                  }
                  else
                  {
                     pReplaceMatchOn->push_back(json::Value(gsl::narrow_cast<int>(matchOn)));
                     pReplaceMatchOff->push_back(json::Value(gsl::narrow_cast<int>(replaceMatchOff)));
                  }
               }
               pos--;
            }
            // encode and write the new line to file
            if (!findResults().preview())
            {
               Error error = encodeAndWriteLineToFile(pLineInfo->decodedContents,
                  pLineInfo->leadingWhitespace, pLineInfo->trailingWhitespace);
               if (error)
                  addReplaceErrorMessage(error.asString(), pErrorMessage, pReplaceMatchOn,
                     pReplaceMatchOff, &lineSuccess);

            }
         }
      }
      return Success();
   }

   bool shouldSkipFile(std::string file)
   {
      bool skipFile = false;
      if (file.find("/.Rproj.user/") != std::string::npos ||
          file.find("/.git/") != std::string::npos ||
          file.find("/.svn/") != std::string::npos ||
          file.find("/packrat/lib/") != std::string::npos ||
          file.find("/packrat/src/") != std::string::npos ||
          file.find("/renv/library/") != std::string::npos ||
          file.find("/.Rhistory") != std::string::npos)
         skipFile = true;
      return (skipFile);
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
      json::Array errors;

      int recordsToProcess = MAX_COUNT + 1 - findResults().resultCount();
      if (recordsToProcess < 0)
         recordsToProcess = 0;

      std::string websiteOutputDir = module_context::websiteOutputDir();
      if (!websiteOutputDir.empty())
         websiteOutputDir = "/" + websiteOutputDir + "/";

      stdOutBuf_.append(data);
      size_t nextLineStart = 0;
      size_t pos = -1;
      std::set<std::string> errorMessage;
      while (recordsToProcess &&
             std::string::npos != (pos = stdOutBuf_.find('\n', pos + 1)))
      {
         std::string line = stdOutBuf_.substr(nextLineStart, pos - nextLineStart);
         nextLineStart = pos + 1;

         errorMessage.clear();
         boost::smatch match;
         if (regex_utils::match(line, match, boost::regex("^((?:[a-zA-Z]:)?[^:]+):(\\d+):(.*)")))
         {
            std::string file = module_context::createAliasedPath(
                  FilePath(string_utils::systemToUtf8(match[1])));
            if (shouldSkipFile(file) ||
                (!websiteOutputDir.empty() &&
                 file.find(websiteOutputDir) != std::string::npos))
               continue;

            int lineNum = safe_convert::stringTo<int>(std::string(match[2]), -1);
            LineInfo lineInfo;
            lineInfo.encodedContents = match[3];
            lineInfo.decodedPreview = match[3];
            lineInfo.decodedContents = match[3];

            boost::algorithm::trim(lineInfo.decodedPreview);
            if (lineInfo.encodedContents != lineInfo.decodedPreview)
            {
               size_t pos = lineInfo.encodedContents.find(lineInfo.decodedPreview);
               lineInfo.leadingWhitespace = lineInfo.encodedContents.substr(0,pos);
               lineInfo.trailingWhitespace =
                  lineInfo.encodedContents.substr(pos + lineInfo.decodedPreview.length());
            }

            json::Array matchOn, matchOff;
            json::Array replaceMatchOn, replaceMatchOff;
            processContents(&lineInfo.decodedPreview, &lineInfo.decodedContents,
               &matchOn, &matchOff);

            if (findResults().replace() &&
                !(findResults().preview() &&
                  findResults().replacePattern().empty()))
            {
               FilePath fullPath(module_context::resolveAliasedPath(file));
               if (currentFile_.empty() || currentFile_ != fullPath.getAbsolutePath())
               {
                  if (!currentFile_.empty())
                     completeFileReplace();
                  Error error = initializeFileForReplace(fullPath);
                  if (error)
                     addReplaceErrorMessage(error.asString(), &errorMessage,
                        &replaceMatchOn, &replaceMatchOff, &fileSuccess_);
               }

               if (!fileSuccess_)
               {
                  // the first time a file is processed it gets a more detailed initialization error
                  if (inputLineNum_ != 0)
                     addReplaceErrorMessage("Cannot perform replace", &errorMessage,
                        &replaceMatchOn, &replaceMatchOff, &fileSuccess_);
                  if (!findResults().preview())
                     findResults().replaceProgress()->
                        addUnits(gsl::narrow_cast<int>(matchOn.getSize()));
               }
               else
               {
                   processReplace(lineNum,
                                  matchOn, matchOff,
                                  &lineInfo,
                                  &replaceMatchOn, &replaceMatchOff,
                                  &errorMessage);
               }
               lineInfo.decodedPreview = lineInfo.decodedContents;

               adjustForPreview(&lineInfo.decodedPreview);
            }

            files.push_back(json::Value(file));
            lineNums.push_back(json::Value(lineNum));
            contents.push_back(json::Value(lineInfo.decodedPreview));
            matchOns.push_back(matchOn);
            matchOffs.push_back(matchOff);
            replaceMatchOns.push_back(replaceMatchOn);
            replaceMatchOffs.push_back(replaceMatchOff);
            for (std::string newError : errorMessage)
               errors.push_back(json::Value(newError));
            recordsToProcess--;
         }
      }
      if (findResults().replace() && !currentFile_.empty() && !findResults().preview())
         completeFileReplace();

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
         results["errors"] = errors;
         result["results"] = results;

         findResults().addResult(handle(),
                                 files,
                                 lineNums,
                                 contents,
                                 matchOns,
                                 matchOffs,
                                 replaceMatchOns,
                                 replaceMatchOffs);

         if (!findResults().replace() || findResults().preview())
            module_context::enqueClientEvent(
                     ClientEvent(client_events::kFindResult, result));
         else
            module_context::enqueClientEvent(
                    ClientEvent(client_events::kReplaceResult, result));
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
   std::string currentFile_;
   FilePath tempReplaceFile_;
   std::shared_ptr<std::istream> inputStream_;
   std::shared_ptr<std::ostream> outputStream_;
   int inputLineNum_;
   bool fileSuccess_;
};

} // namespace

struct GrepOptions
{
   GrepOptions(std::string search, std::string directory,
      json::Array filePatterns, bool asRegex, bool ignoreCase) :
      asRegex(asRegex),
      ignoreCase(ignoreCase),
      searchPattern(search),
      directory(directory),
      filePatterns(filePatterns)
   {}

   bool asRegex;
   bool ignoreCase;

   const std::string searchPattern;
   const std::string directory;
   const json::Array filePatterns;
};

struct ReplaceOptions
{
   ReplaceOptions() :
      empty(true),
      replacePattern("")
   {}

   ReplaceOptions(std::string replace, bool asRegex, bool useGitIgnore) :
      empty(false),
      preview(false),
      asRegex(asRegex),
      useGitIgnore(useGitIgnore),
      replacePattern(replace)
   {}

   bool empty;
   bool preview;
   bool asRegex;
   bool useGitIgnore;

   const std::string replacePattern;
};

core::Error runGrepOperation(const GrepOptions& grepOptions, const ReplaceOptions& replaceOptions,
   LocalProgress* pProgress, json::JsonRpcResponse* pResponse)
{
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
   error = r::util::iconvstr(grepOptions.searchPattern,
                             "UTF-8",
                             encoding,
                             false,
                             &encodedString);
   if (error)
   {
      LOG_ERROR(error);
      encodedString = grepOptions.searchPattern;
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

   if (grepOptions.ignoreCase)
      cmd << "-i";

   // Use -f to pass pattern via file, so we don't have to worry about
   // escaping double quotes, etc.
   cmd << "-f";
   cmd << tempFile;
   if (!grepOptions.asRegex)
      cmd << "-F";

   for (json::Value filePattern : grepOptions.filePatterns)
   {
      cmd << "--include=" + filePattern.getString();
   }

   cmd << shell_utils::EscapeFilesOnly << "--" << shell_utils::EscapeAll;


   // Filepaths received from the client will be UTF-8 encoded;
   // convert to system encoding here.
   FilePath dirPath = module_context::resolveAliasedPath(grepOptions.directory);
   cmd << string_utils::utf8ToSystem(dirPath.getAbsolutePath());

   // Clear existing results
   findResults().clear();

   error = module_context::processSupervisor().runCommand(cmd,
                                                          options,
                                                          callbacks);
   if (error)
      return error;

   findResults().onFindBegin(ptrGrepOp->handle(),
                             grepOptions.searchPattern,
                             grepOptions.directory,
                             grepOptions.asRegex,
                             grepOptions.ignoreCase);
   if (!replaceOptions.empty)
      findResults().onReplaceBegin(ptrGrepOp->handle(),
                                   replaceOptions.preview,
                                   replaceOptions.replacePattern,
                                   replaceOptions.asRegex,
                                   pProgress);
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

   GrepOptions grepOptions(searchString, directory, filePatterns, asRegex, ignoreCase);
   error = runGrepOperation(grepOptions, ReplaceOptions(), nullptr, pResponse);
   return error;
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
      LOG_DEBUG_MESSAGE("Replace Regex must be true during preview");

   GrepOptions grepOptions(searchString, directory, filePatterns, asRegex, ignoreCase);
   ReplaceOptions replaceOptions(replacePattern, replaceRegex, useGitIgnore);
   replaceOptions.preview = true;
   error = runGrepOperation(grepOptions, replaceOptions, nullptr, pResponse);

   return error;
}

core::Error completeReplace(const json::JsonRpcRequest& request,
                            json::JsonRpcResponse* pResponse)
{
   bool asRegex, ignoreCase, replaceRegex, useGitIgnore = false;
   std::string searchString;
   std::string replacePattern;
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

   static const int kUpdatePercent = 5;
   LocalProgress* pProgress = new LocalProgress(originalFindCount, kUpdatePercent);
   GrepOptions grepOptions(searchString, directory, filePatterns, asRegex, ignoreCase);
   ReplaceOptions replaceOptions(replacePattern, replaceRegex, useGitIgnore);

   error = runGrepOperation(
      grepOptions, replaceOptions, pProgress, pResponse);
   return error;
}

core::Error stopReplace(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   std::string handle;
   Error error = json::readParams(request.params, &handle);
   if (!error)
      findResults().onReplaceEnd(handle);
   return error;
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

core::Error Replacer::completeReplace(const boost::regex& searchRegex,
                                      const std::string& replaceRegex,
                                      size_t matchOn, size_t matchOff, std::string* pLine,
                                      size_t* pReplaceMatchOff)
{
   std::string temp;
   try
   {
      temp = boost::regex_replace(pLine->substr(matchOn), searchRegex, replaceRegex,
         boost::format_sed | boost::format_first_only);
   }
   catch (const boost::regex_error& e)
   {
      return core::Error(e.position(), e.what(), ERROR_LOCATION);
   }

   temp.insert(0, pLine->substr(0, matchOn));
   std::string endOfString = pLine->substr(matchOff).c_str();
   size_t replaceMatchOff;
   if (endOfString.empty())
      replaceMatchOff = temp.length();
   else
      replaceMatchOff = temp.find(endOfString);

   *pLine = temp;
   std::string replaceString = temp.substr(matchOn, (replaceMatchOff - matchOn));
   *pReplaceMatchOff = matchOn  + replaceString.size();
   return core::Success();
}

core::Error Replacer::replaceRegexIgnoreCase(size_t matchOn, size_t matchOff,
                                             const std::string& findRegex,
                                             const std::string& replaceRegex, std::string* pLine,
                                             size_t* pReplaceMatchOff)
{
   try
   {
      boost::regex find(findRegex, boost::regex::grep | boost::regex::icase);
      core::Error error = completeReplace(find, replaceRegex, matchOn, matchOff, pLine,
         pReplaceMatchOff);
      return error;
   }
   catch (const boost::regex_error& e)
   {
      return core::Error(e.position(), e.what(), ERROR_LOCATION);
   }
}

core::Error Replacer::replaceRegexWithCase(size_t matchOn, size_t matchOff,
                                           const std::string& findRegex,
                                           const std::string& replaceRegex, std::string* pLine,
                                           size_t* pReplaceMatchOff)
{
   try
   {
      boost::regex find(findRegex, boost::regex::grep);
      core::Error error = completeReplace(find, replaceRegex, matchOn, matchOff, pLine,
         pReplaceMatchOff);
      return error;
   }
   catch (const boost::regex_error& e)
   {
      return core::Error(e.position(), e.what(), ERROR_LOCATION);
   }
}

} // namespace find
} // namespace modules
} // namespace session
} // namespace rstudio
