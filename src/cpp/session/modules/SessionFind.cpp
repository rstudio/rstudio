/*
 * SessionFind.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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
#include <boost/filesystem.hpp>

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

namespace errc {

enum errc_t
{
   Success = 0,
   RegexError = 1,
   PermissionsError = 2
};

const std::string& findCategory()
{
   static const std::string findCategory = "find_error";
   return findCategory;
}

}

namespace {

// This must be the same as MAX_COUNT in FindOutputPane.java
const size_t MAX_COUNT = 1000;

const size_t MAX_LINE_LENGTH = 1000;

// Reflects the estimated current progress made in performing a replace
class LocalProgress : public boost::noncopyable
{
public:

   LocalProgress(int totalReplaceCount, int updateFrequency) :
      totalReplaceCount_(totalReplaceCount),
      updateFrequency_(updateFrequency)
   {
      replacedCount_ = 0;
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
   const int updateFrequency_;
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
      gitFlag_(false),
      pReplaceProgress_(nullptr)
   {
   }

   std::string handle() const
   {
      return handle_;
   }

   std::string path() const
   {
      return path_;
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

   bool gitFlag() const
   {
      return gitFlag_;
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
                    bool ignoreCase,
                    bool gitFlag)
   {
      handle_ = handle;
      input_ = input;
      path_ = path;
      regex_ = asRegex;
      ignoreCase_ = ignoreCase;
      gitFlag_ = gitFlag;
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
                       LocalProgress* pProgress)
   {
      if (handle_ == handle)
      {
         replace_ = true;
         preview_ = previewFlag;
         replacePattern_ = replacePattern;
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
                                     "handle", handle_,
                                     "input", input_,
                                     "path", path_,
                                     "regex", regex_,
                                     "ignoreCase", ignoreCase_,
                                     "results", results,
                                     "running", running_,
                                     "replace", replace_,
                                     "preview", preview_,
                                     "gitFlag", gitFlag_,
                                     "replacePattern", replacePattern_);
      if (error)
         return error;

      error = json::readObject(results,
                               "file", files_,
                               "line", lineNums_,
                               "lineValue", contents_,
                               "matchOn", matchOns_,
                               "matchOff", matchOffs_,
                               "replaceMatchOn", replaceMatchOns_,
                               "replaceMatchOff", replaceMatchOffs_);
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

      obj["replace"] = replace_;
      obj["preview"] = preview_;
      obj["gitFlag"] = gitFlag_;
      obj["replacePattern"] = replacePattern_;

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
   bool gitFlag_;
   std::string replacePattern_;
   json::Array replaceMatchOns_;
   json::Array replaceMatchOffs_;
   // this is not tracked via json because it exclusively applies to replaces (not previews)
   // which can not currently be paused
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

   bool onContinue(const core::system::ProcessOperations& /*ops*/) const
   {
      return findResults().isRunning() && findResults().handle() == handle();
   }

   void addReplaceErrorMessage(const std::string& contents,
                               std::set<std::string>* pErrorSet,
                               json::Array* pReplaceMatchOn,
                               json::Array* pReplaceMatchOff,
                               bool* pSuccessFlag)
   {
      pErrorSet->insert(contents);
      pReplaceMatchOn->push_back(gsl::narrow_cast<int>(-1));
      pReplaceMatchOff->push_back(gsl::narrow_cast<int>(-1));
      *pSuccessFlag = false;
   }

   void addNewLine(std::string& str)
   {
      str.append("\n");
#ifdef _WIN32
      string_utils::convertLineEndings(&str, string_utils::LineEndingWindows);
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

// permissions getter/setter (only applicable to Unix platforms)
#ifndef _WIN32
   Error setPermissions(const std::string& filePath, boost::filesystem::perms permissions)
   {
      boost::filesystem::path path(filePath);
      try
      {
         boost::filesystem::permissions(path, permissions);
      }
      catch (const boost::filesystem::filesystem_error& e)
      {
         return Error(e.code(), ERROR_LOCATION);
      }

      return Success();
   }

   Error getPermissions(const std::string& filePath, boost::filesystem::perms* pPerms)
   {
      *pPerms = boost::filesystem::no_perms;

      boost::filesystem::path path(filePath);
      try
      {
         boost::filesystem::file_status fileStatus = status(path);
         *pPerms = fileStatus.permissions();
      }
      catch (const boost::filesystem::filesystem_error& e)
      {
         return (Error(
                  errc::findCategory(),
                  errc::PermissionsError,
                  "A permissions error occured during replace operation.",
                  ERROR_LOCATION));
      }
      return Success();
   }
#endif

   void adjustForPreview(std::string* contents, json::Array* pMatchOn, json::Array* pMatchOff)
   {
      size_t maxPreviewLength = 300;
      size_t firstMatchOn = pMatchOn->getValueAt(0).getInt();
      if (contents->size() > maxPreviewLength)
      {
         if (firstMatchOn > maxPreviewLength)
         {
            *contents = contents->erase(0, firstMatchOn - 30);
            contents->insert(0, "...");
            int leadingCharactersErased = gsl::narrow_cast<int>(firstMatchOn - 33);
            json::Array newMatchOnArray;
            json::Array newMatchOffArray;
            for (size_t i = 0; i < pMatchOn->getSize(); i++)
            {
               newMatchOnArray.push_back(pMatchOn->getValueAt(i).getInt() - leadingCharactersErased);
               if (i >= pMatchOff->getSize())
                  LOG_WARNING_MESSAGE("pMatchOn and pMatchOff should be the same length");
               else
                  newMatchOffArray.push_back(pMatchOff->getValueAt(i).getInt() - leadingCharactersErased);
            }
            *pMatchOn = newMatchOnArray;
            *pMatchOff = newMatchOffArray;
         }
         if (contents->size() > maxPreviewLength)
            adjustForPreview(contents);
      }
   }

   Error completeFileReplace(std::set<std::string>* pErrorMessage)
   {
      if (fileSuccess_)
      {
         if (!currentFile_.empty() &&
             !tempReplaceFile_.getAbsolutePath().empty() &&
             outputStream_->good())
         {
             Error error;
// For Windows we ignore this additional safety check
// it will always fail because we have inputStream_ reading the file
#ifndef _WIN32
            error = FilePath(currentFile_).testWritePermissions();
            if (error)
            {
               json::Array replaceMatchOn, replaceMatchOff;
               addReplaceErrorMessage(error.asString(), pErrorMessage, &replaceMatchOn,
                  &replaceMatchOff, &fileSuccess_);
               return error;
            }
#endif
            std::string line;
            while (std::getline(*inputStream_, line))
            {
               addNewLine(line);
               outputStream_->write(line.c_str(), line.size());
            }
            outputStream_->flush();
            inputStream_.reset();
            outputStream_.reset();

// Unneccesary on Windows because this only sets write permissions which we
// already know are correct if we are writing.
// This needs to happen after outputStream is flushed
#ifndef _WIN32
            error = setPermissions(tempReplaceFile_.getAbsolutePath(), filePermissions_);
#endif
            if (!error)
               error = tempReplaceFile_.move(FilePath(currentFile_));
            currentFile_.clear();
            if (error)
            {
               json::Array replaceMatchOn, replaceMatchOff;
               addReplaceErrorMessage(error.asString(), pErrorMessage, &replaceMatchOn,
                  &replaceMatchOff, &fileSuccess_);
               return error;
            }
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
      while (regex_utils::search(
               inputPos, match, getColorEncodingRegex(findResults().gitFlag())))
      {
         // decode the current match, and append it
         std::string matchedString(inputPos, inputPos + match.position());
         std::string decoded = Replacer::decode(matchedString, encoding_, firstDecodeError_);

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
         if ((match.size() > 2 && match[2] == "1" && findResults().gitFlag()) ||
             (match[1] == "01" && !findResults().gitFlag()))
            pMatchOn->push_back(gsl::narrow_cast<int>(nUtf8CharactersProcessed));
         else
            pMatchOff->push_back(gsl::narrow_cast<int>(nUtf8CharactersProcessed));
      }

      if (inputPos != end)
         decodedLine.append(
            Replacer::decode(std::string(inputPos, end), encoding_, firstDecodeError_));

      *pFullLineContent = decodedLine;
      if (!findResults().replace())
         adjustForPreview(&decodedLine, pMatchOn, pMatchOff);
      *pContent = decodedLine;
   }

   Error initializeFileForReplace(FilePath file)
   {
      fileSuccess_ = false;
      Error error = file.testWritePermissions();
      if (error)
         return error;

      if (!findResults().preview())
      {
         tempReplaceFile_ =  module_context::tempFile("replace", "txt");
         error = tempReplaceFile_.openForWrite(outputStream_);
         if (error)
            return error;
      }

      error = file.openForRead(inputStream_);
      if (error)
         return error;

      // boost only acknowledges write permissions on Windows which we already know exist
#ifndef _WIN32
      error = getPermissions(file.getAbsolutePath(), &filePermissions_);
      if (error)
         return (error);
#endif

      fileSuccess_ = true;
      inputLineNum_ = 0;
      currentFile_ = file.getAbsolutePath();

      return Success();
   }

   void subtractOffsetIntegerToJsonArray(
       json::Value newValue, int offset, json::Array* pJsonArray)
   {
      // make sure negative values (errors) don't become positve
      if (newValue.getInt() < 0)
         newValue = json::Value(newValue.getInt() - offset);
      pJsonArray->push_back(gsl::narrow_cast<int>(newValue.getInt() + offset));
   }

   void subtractOffsetIntegersToJsonArray(
         json::Array values, int offset, json::Array* pJsonArray)
   {
      for (json::Value value : values)
         subtractOffsetIntegerToJsonArray(value, offset, pJsonArray);
   }

   Error writeToFile(
      const std::string& line, const std::string& lineLeftContents,
      const std::string& lineRightContents)
   {
      Error error;
      std::string newLine(line);
      newLine.insert(0, lineLeftContents);
      newLine.insert(newLine.length(), lineRightContents);
      addNewLine(newLine);

      if (error)
         return error;
      else
      {
         try
         {
            outputStream_->write(newLine.c_str(), newLine.size());
            outputStream_->flush();
         }
         catch (const std::ios_base::failure& e)
         {
            error = systemError(errno, e.what(), ERROR_LOCATION);
         }
      }
      return error;
   }

   void cleanLineAndGetMatches(std::string* pEncodedLine,
                               json::Array* pMatchOn, json::Array* pMatchOff)
   {
      // The incoming string is assumed to have color encodings from the initial grep command.
      // These encodings are parsed out and their positions are placed in pMatchOn and pMatchOff.

      const char* inputPos = pEncodedLine->c_str();
      const char* end = inputPos + pEncodedLine->size();
      std::size_t charactersProcessed = 0;
      boost::cmatch match;
      std::string cleanLine;

      while (regex_utils::search(inputPos, match, getColorEncodingRegex(findResults().gitFlag())))
      {
         std::string matchedString(inputPos, inputPos + match.position());
         inputPos += match.position() + match.length();
         
         cleanLine.append(matchedString);

         charactersProcessed += matchedString.size();
   
         // Match now contains the regex results by capturing group. Depending on which color
         // encoding regex is used in the search, the first match will always contain '1' or '01'.
         if ((match.size() > 2 && match[2] == "1" && findResults().gitFlag()) ||
             (match[1] == "01" && !findResults().gitFlag()))
            pMatchOn->push_back(json::Value(gsl::narrow_cast<int>(charactersProcessed)));
         else
            pMatchOff->push_back(json::Value(gsl::narrow_cast<int>(charactersProcessed)));
      }
      if (inputPos != end)
         cleanLine.append(std::string(inputPos, end));
      *pEncodedLine = cleanLine;
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

      // when the system is not using utf8 we encoded the line before performing the replace
      json::Array eMatchOnArray;
      json::Array eMatchOffArray;
      size_t eMatchOn = 0;
      size_t eMatchOff = 0;

      cleanLineAndGetMatches(&pLineInfo->encodedContents, &eMatchOnArray, &eMatchOffArray);

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
            while (pos > -1)
            {
               const size_t matchOn =
                  static_cast<size_t>(matchOnArray.getValueAt(static_cast<size_t>(pos)).getInt());
               const size_t matchOff =
                  static_cast<size_t>(matchOffArray.getValueAt(static_cast<size_t>(pos)).getInt());
               const size_t matchSize = matchOff - matchOn;
               size_t replaceMatchOff = matchOff;
               Error error;
               Replacer replacer(findResults().ignoreCase(), encoding_);

               eMatchOn =
                   static_cast<size_t>(eMatchOnArray.getValueAt(static_cast<size_t>(pos)).getInt());
               eMatchOff =
                   static_cast<size_t>(eMatchOffArray.getValueAt(static_cast<size_t>(pos)).getInt());


               // If we found a different number of matches searching the encoded string,
               // we shouldn't perform the replace as the expected vs actual results may differ.
               if (eMatchOnArray.getSize() != matchOnArray.getSize())
               {
                  core::Error error(
                     errc::findCategory(),
                     errc::RegexError,
                     "Found " + std::to_string(matchOnArray.getSize()) +  " matches in line but " + std::to_string(eMatchOnArray.getSize()) + " matches in encoded line; skipping replace.",
                     ERROR_LOCATION);
                  addReplaceErrorMessage(error.asString(), pErrorMessage, pReplaceMatchOn,
                     pReplaceMatchOff, &lineSuccess);

                  return error;
               }

               // if previewing, we need to display the original and replacement text
               if (findResults().preview())
               {
                  error = replacer.replacePreview(matchOn, matchOff, eMatchOn, eMatchOff,
                      &pLineInfo->encodedContents, &pLineInfo->decodedContents, &replaceMatchOff);
                  if (error)
                     addReplaceErrorMessage(error.asString(), pErrorMessage, pReplaceMatchOn,
                        pReplaceMatchOff, &lineSuccess);
               }
               else // perform replace
               {
                  pProgress->addUnits(1);

                  if (findResults().regex())
                     error = replacer.replaceRegex(eMatchOn, eMatchOff, searchPattern,
                        replacePattern, &pLineInfo->encodedContents, &replaceMatchOff);
                  else
                     replacer.replaceLiteral(eMatchOn, eMatchOff, replacePattern,
                           &pLineInfo->encodedContents, &replaceMatchOff);

                  // calculate utf8 matchOff
                  size_t utf8Length;
                  error = string_utils::utf8Distance(pLineInfo->decodedContents.begin(),
                                                     pLineInfo->decodedContents.end(),
                                                     &utf8Length);
                  pLineInfo->decodedContents =
                     replacer.decode(pLineInfo->encodedContents);

                  size_t newUtf8Length;
                  error = string_utils::utf8Distance(pLineInfo->decodedContents.begin(),
                                                     pLineInfo->decodedContents.end(),
                                                     &newUtf8Length);
                  replaceMatchOff = matchOff + (newUtf8Length - utf8Length);
               }

               // Handle side-effects when replace is successful
               if (lineSuccess)
               {
                  // If multiple replaces in line, readjust previous match numbers to account for
                  // difference in find and replace sizes. This is only for display purposes so we
                  // don't consider encoded values.
                  size_t replaceSize = replaceMatchOff - matchOn;
                  if (pReplaceMatchOn->getSize() > 0 &&
                      matchSize != replaceSize)
                  {
                     json::Array tempMatchOn(*pReplaceMatchOn);
                     json::Array tempMatchOff(*pReplaceMatchOff);
                     pReplaceMatchOn->clear();
                     pReplaceMatchOff->clear();

                     int offset(gsl::narrow_cast<int>(replaceSize - matchSize));
                     pReplaceMatchOn->push_back(gsl::narrow_cast<int>(matchOn));
                     pReplaceMatchOff->push_back(gsl::narrow_cast<int>(replaceMatchOff));
                     subtractOffsetIntegersToJsonArray(tempMatchOn, offset, pReplaceMatchOn);
                     subtractOffsetIntegersToJsonArray(tempMatchOff, offset, pReplaceMatchOff);
                  }
                  else
                  {
                     pReplaceMatchOn->push_back(gsl::narrow_cast<int>(matchOn));
                     pReplaceMatchOff->push_back(gsl::narrow_cast<int>(replaceMatchOff));
                  }
               }
               pos--;
            }
            // write the new line to file
            if (!findResults().preview())
            {
               Error error = writeToFile(pLineInfo->encodedContents,
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
      return (file.find("/.Rproj.user/") != std::string::npos ||
              file.find("/.git/") != std::string::npos ||
              file.find("/.svn/") != std::string::npos ||
              file.find("/packrat/lib/") != std::string::npos ||
              file.find("/packrat/src/") != std::string::npos ||
              file.find("/renv/library/") != std::string::npos ||
              file.find("/.Rhistory") != std::string::npos);
   }

   void onStdout(const core::system::ProcessOperations& /*ops*/, const std::string& data)
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
         if (regex_utils::match(
               line, match, getGrepOutputRegex(findResults().gitFlag())) &&
             match.size() > 1)
         {
            std::string file = module_context::createAliasedPath(
                  FilePath(string_utils::systemToUtf8(match[1])));
            // git grep returns the path within the repo
            // we use this combined with the find request's directory
            // to locate the file on the user's system
            if (findResults().gitFlag())
            {
               file.insert(0, "/");
               file.insert(0, findResults().path());
            }

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
               std::string trimmed(lineInfo.encodedContents);
               boost::algorithm::trim(trimmed);

               size_t pos = lineInfo.encodedContents.find(trimmed);
               lineInfo.leadingWhitespace =
                  lineInfo.encodedContents.substr(0, pos);
               lineInfo.trailingWhitespace =
                  lineInfo.encodedContents.substr(pos + trimmed.length());
               lineInfo.encodedContents = trimmed;
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
               // check if we are looking at a new file
               if (currentFile_.empty() || currentFile_ != fullPath.getAbsolutePath())
               {
                  if (!currentFile_.empty())
                     completeFileReplace(&errorMessage);
                  Error error = initializeFileForReplace(fullPath);
                  if (error)
                     addReplaceErrorMessage(error.asString(), &errorMessage,
                        &replaceMatchOn, &replaceMatchOff, &fileSuccess_);
               }
               else if (!fileSuccess_)
               {
                  // the first time a file is processed it gets a more detailed initialization error
                  addReplaceErrorMessage("Cannot perform replace", &errorMessage,
                     &replaceMatchOn, &replaceMatchOff, &fileSuccess_);
               }
               if (!fileSuccess_ || lineInfo.decodedPreview.length() > MAX_LINE_LENGTH)
               {
                  // if we failed for any reason, update the progress
                  if (!findResults().preview())
                     findResults().replaceProgress()->
                        addUnits(gsl::narrow_cast<int>(matchOn.getSize()));
                  if (fileSuccess_)
                  {
                     bool lineSuccess;
                     addReplaceErrorMessage("Line exceeds maximum character length for replace",
                        &errorMessage, &replaceMatchOn, &replaceMatchOff, &lineSuccess);
                  }
               }
               else
               {
                   processReplace(lineNum,
                                  matchOn, matchOff,
                                  &lineInfo,
                                  &replaceMatchOn, &replaceMatchOff,
                                  &errorMessage);
                  lineInfo.decodedPreview = lineInfo.decodedContents;
                  adjustForPreview(&lineInfo.decodedPreview, &matchOn, &matchOff);
               }
            }

            files.push_back(file);
            lineNums.push_back(lineNum);
            contents.push_back(lineInfo.decodedPreview);
            matchOns.push_back(matchOn);
            matchOffs.push_back(matchOff);
            replaceMatchOns.push_back(replaceMatchOn);
            replaceMatchOffs.push_back(replaceMatchOff);
            json::Array combinedErrors = json::toJsonArray(errorMessage);
            errors.push_back(combinedErrors);
            recordsToProcess--;
         }
      }
      // when doing a replace, we haven't completed the replace for the last file here
      if (findResults().replace() && !currentFile_.empty() && !findResults().preview())
      {
         std::set<std::string> errorMessage;
         completeFileReplace(&errorMessage);

         // it is unlikely there will be any errors if we've made it this far,
         // but if so we must add them to the last array in errors
         // if there is an error, there will only be one
         if (!errorMessage.empty())
         {
            json::Array lastErrors = errors.getBack().getArray();
            errors.erase(--errors.end());
            lastErrors.push_back(json::Value(*errorMessage.begin()));
            errors.push_back(lastErrors);
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

   void onStderr(const core::system::ProcessOperations& /*ops*/, const std::string& data)
   {
      LOG_ERROR_MESSAGE("grep: " + data);
      if (boost::algorithm::icontains(data, "not a git repository"))
         module_context::showErrorMessage("Not a Git Repository", data);
   }

   void onExit(int /*exitCode*/)
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
   boost::filesystem::perms filePermissions_;
   int inputLineNum_;
   bool fileSuccess_;
};

} // namespace

class GrepOptions : public boost::noncopyable
{
public:

   GrepOptions(std::string search, std::string directory,
      json::Array includeFilePatterns, json::Array excludeFilePatterns,
      bool asRegex, bool ignoreCase) :
      asRegex_(asRegex),
      ignoreCase_(ignoreCase),
      searchPattern_(search),
      directory_(directory),
      includeFilePatterns_(includeFilePatterns),
      excludeFilePatterns_(excludeFilePatterns)
   {
      processIncludeFilePatterns();
      processExcludeFilePatterns();
   }

   bool asRegex() const
   {
      return asRegex_;
   }

   bool ignoreCase() const
   {
      return ignoreCase_;
   }

   const std::string& searchPattern() const
   {
      return searchPattern_;
   }

   const std::string& directory() const
   {
      return directory_;
   }

   const json::Array& includeFilePatterns() const
   {
      return includeFilePatterns_;
   }

   const json::Array& excludeFilePatterns() const
   {
      return excludeFilePatterns_;
   }

   bool packageSourceFlag() const
   {
      return packageSourceFlag_;
   }

   bool packageTestsFlag() const
   {
      return packageTestsFlag_;
   }

   bool anyPackageFlag() const
   {
      return packageSourceFlag_ || packageTestsFlag_;
   }

   const std::vector<std::string>& includeArgs() const
   {
      return includeArgs_;
   }

   bool gitFlag() const
   {
      return gitFlag_;
   }

   const std::vector<std::string>& excludeArgs() const
   {
      return excludeArgs_;
   }

private:

   bool asRegex_;
   bool ignoreCase_;

   const std::string searchPattern_;
   const std::string directory_;
   const json::Array includeFilePatterns_;
   const json::Array excludeFilePatterns_;

   // derived from includeFilePatterns
   std::vector<std::string> includeArgs_;
   bool packageSourceFlag_;
   bool packageTestsFlag_;

   // derived from excludeFilePatterns
   std::vector<std::string> excludeArgs_;
   bool gitFlag_;

   void processExcludeFilePatterns()
   {
      gitFlag_ = false;
      for (json::Value filePattern : excludeFilePatterns_)
      {
         if (filePattern.getType() != json::Type::STRING)
            LOG_DEBUG_MESSAGE("Exclude files contain non-string value");
         else
         {
            std::string excludeText = boost::algorithm::trim_copy(filePattern.getString());
            if (excludeText.compare("gitExclusions") == 0)
               gitFlag_ = true;
            else if (!excludeText.empty())
               excludeArgs_.push_back("--exclude=" + filePattern.getString());
         }
      }
   }

   void processIncludeFilePatterns()
   {
      packageSourceFlag_ = false;
      packageTestsFlag_ = false;
      for (json::Value filePattern : includeFilePatterns_)
      {
         if (filePattern.getType() != json::Type::STRING)
            LOG_DEBUG_MESSAGE("Include files contain non-string value");
         else
         {
            std::string includeText = boost::algorithm::trim_copy(filePattern.getString());
            if (includeText.compare("packageSource") == 0)
               packageSourceFlag_ = true;
            else if (includeText.compare("packageTests") == 0)
               packageTestsFlag_ = true;
            else if (!includeText.empty())
               includeArgs_.push_back("--include=" + filePattern.getString());
         }
      }
   }
};

struct ReplaceOptions
{
   ReplaceOptions() :
      empty(true),
      replacePattern("")
   {}

   ReplaceOptions(std::string replace) :
      empty(false),
      preview(false),
      replacePattern(replace)
   {}

   bool empty;
   bool preview;

   const std::string replacePattern;
};

void addDirectoriesToCommand(
   bool packageSourceFlag, bool packageTestsFlag,
   const FilePath& directoryPath, shell_utils::ShellCommand* pCmd)
{
   // not sure if EscapeFilesOnly can be removed or is necessary for an edge case
   *pCmd << shell_utils::EscapeFilesOnly << "--" << shell_utils::EscapeAll;
   if (!(packageSourceFlag || packageTestsFlag))
      *pCmd << string_utils::utf8ToSystem(directoryPath.getAbsolutePath());
   else if (packageSourceFlag)
   {
      FilePath rPath(string_utils::utf8ToSystem(directoryPath.getAbsolutePath() + "/R"));
      FilePath srcPath(string_utils::utf8ToSystem(directoryPath.getAbsolutePath() + "/src"));
      if (rPath.exists())
         *pCmd << rPath;
      if (srcPath.exists())
         *pCmd << srcPath;
      else if (!rPath.exists())
         LOG_WARNING_MESSAGE(
            "Package source directories not found in " + directoryPath.getAbsolutePath());
   }
   else
   {
      FilePath testsPath(string_utils::utf8ToSystem(directoryPath.getAbsolutePath() + "/tests"));
      if (testsPath.exists())
         *pCmd << testsPath;
      else
         LOG_WARNING_MESSAGE("Package test directory not found in " + directoryPath.getAbsolutePath());
   }
}

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
   error = r::util::iconvstr(grepOptions.searchPattern(),
                             "UTF-8",
                             encoding,
                             false,
                             &encodedString);
   if (error)
   {
      LOG_ERROR(error);
      encodedString = grepOptions.searchPattern();
   }

   *pStream << encodedString << std::endl;
   pStream.reset(); // release file handle

   boost::shared_ptr<GrepOperation> ptrGrepOp = GrepOperation::create(encoding,
                                                                      tempFile);
   core::system::ProcessCallbacks callbacks =
                                       ptrGrepOp->createProcessCallbacks();

   // Filepaths received from the client will be UTF-8 encoded;
   // convert to system encoding here.
   FilePath dirPath = module_context::resolveAliasedPath(grepOptions.directory());

#ifdef _WIN32
   shell_utils::ShellCommand cmd(gnuGrepPath.completePath("grep"));
#else
   shell_utils::ShellCommand cmd("grep");
#endif
   if (grepOptions.gitFlag())
   {
      cmd = shell_utils::ShellCommand("git");
      // -c is used to override potential user-defined git parameters
      cmd << "-c" << "grep.lineNumber=true";
      cmd << "-c" << "grep.column=false";
      cmd << "-c" << "grep.patternType=default";
      cmd << "-c" << "grep.extendedRegexp=false";
      cmd << "-c" << "grep.fullName=false";
      cmd << "-C";
      cmd << string_utils::utf8ToSystem(dirPath.getAbsolutePath());
      cmd <<  "grep";
      cmd << "-I"; // ignore binaries
      cmd << "--untracked"; // include files not tracked by git...
      cmd << "--exclude-standard"; // but exclude gitignore
      cmd << "-rHn";
      cmd << "--color=always";
      if (grepOptions.ignoreCase())
         cmd << "-i";
      // Use -f to pass pattern via file, so we don't have to worry about
      // escaping double quotes, etc.
      cmd << "-f";
      cmd << tempFile;
      if (!grepOptions.asRegex())
         cmd << "-F";
      addDirectoriesToCommand(
         grepOptions.packageSourceFlag(), grepOptions.packageTestsFlag(), dirPath, &cmd);

      if (grepOptions.anyPackageFlag() &&
          !grepOptions.includeArgs().empty())
      {
         for (std::string arg : grepOptions.includeArgs())
            LOG_DEBUG_MESSAGE(
               "Unknown include argument(s): " + boost::join(grepOptions.includeArgs(), ", "));
      }
   }
   else
   {
      cmd << "--binary-files=without-match";
      cmd << "-rHn";
      cmd << "--color=always";
#ifndef _WIN32
      cmd << "--devices=skip";
#endif
      if (grepOptions.ignoreCase())
         cmd << "-i";
      // Use -f to pass pattern via file, so we don't have to worry about
      // escaping double quotes, etc.
      cmd << "-f";
      cmd << tempFile;
      if (!grepOptions.asRegex())
         cmd << "-F";
      for (std::string arg : grepOptions.includeArgs())
         cmd << arg;
      for (std::string arg : grepOptions.excludeArgs())
         cmd << arg;
      addDirectoriesToCommand(
         grepOptions.packageSourceFlag(), grepOptions.packageTestsFlag(), dirPath, &cmd);
   }

   // Clear existing results
   findResults().clear();

   error = module_context::processSupervisor().runCommand(cmd,
                                                          options,
                                                          callbacks);
   if (error)
      return error;

   findResults().onFindBegin(ptrGrepOp->handle(),
                             grepOptions.searchPattern(),
                             grepOptions.directory(),
                             grepOptions.asRegex(),
                             grepOptions.ignoreCase(),
                             grepOptions.gitFlag());
   if (!replaceOptions.empty)
      findResults().onReplaceBegin(ptrGrepOp->handle(),
                                   replaceOptions.preview,
                                   replaceOptions.replacePattern,
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
   json::Array includeFilePatterns, excludeFilePatterns;

   Error error = json::readParams(request.params,
                                  &searchString,
                                  &asRegex,
                                  &ignoreCase,
                                  &directory,
                                  &includeFilePatterns,
                                  &excludeFilePatterns);
   if (error)
      return error;

   GrepOptions grepOptions(searchString, directory, includeFilePatterns, excludeFilePatterns,
      asRegex, ignoreCase);
   error = runGrepOperation(grepOptions, ReplaceOptions(), nullptr, pResponse);
   return error;
}

core::Error stopFind(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* /*pResponse*/)
{
   std::string handle;
   Error error = json::readParams(request.params, &handle);
   if (error)
      return error;

   findResults().onFindEnd(handle);

   return Success();
}

core::Error clearFindResults(const json::JsonRpcRequest& /*request*/,
                             json::JsonRpcResponse* /*pResponse*/)
{
   findResults().clear();
   return Success();
}

core::Error previewReplace(const json::JsonRpcRequest& request,
                           json::JsonRpcResponse* pResponse)
{
   std::string searchString;
   std::string replacePattern;
   bool asRegex, ignoreCase;
   std::string directory;
   json::Array includeFilePatterns, excludeFilePatterns;

   Error error = json::readParams(request.params,
                                  &searchString,
                                  &asRegex,
                                  &ignoreCase,
                                  &directory,
                                  &includeFilePatterns,
                                  &excludeFilePatterns,
                                  &replacePattern);
   if (error)
      return error;
   if (!asRegex)
      LOG_DEBUG_MESSAGE("Regex should be true during preview");

   GrepOptions grepOptions(searchString, directory, includeFilePatterns, excludeFilePatterns,
      asRegex, ignoreCase);
   ReplaceOptions replaceOptions(replacePattern);
   replaceOptions.preview = true;
   error = runGrepOperation(grepOptions, replaceOptions, nullptr, pResponse);

   return error;
}

core::Error completeReplace(const json::JsonRpcRequest& request,
                            json::JsonRpcResponse* pResponse)
{
   bool asRegex, ignoreCase;
   std::string searchString;
   std::string replacePattern;
   std::string directory;
   json::Array includeFilePatterns, excludeFilePatterns;
   // only used to estimate progress
   int originalFindCount;

   Error error = json::readParams(request.params,
                                  &searchString,
                                  &asRegex,
                                  &ignoreCase,
                                  &directory,
                                  &includeFilePatterns,
                                  &excludeFilePatterns,
                                  &originalFindCount,
                                  &replacePattern);
   if (error)
      return error;

   // 5 was chosen based on testing to find a value that was both responsive
   // and not overly frequent
   static const int kUpdatePercent = 5;
   LocalProgress* pProgress = new LocalProgress(originalFindCount, kUpdatePercent);
   GrepOptions grepOptions(searchString, directory, includeFilePatterns, excludeFilePatterns,
      asRegex, ignoreCase);
   ReplaceOptions replaceOptions(replacePattern);

   error = runGrepOperation(
      grepOptions, replaceOptions, pProgress, pResponse);
   return error;
}

core::Error stopReplace(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* /*pResponse*/)
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
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "begin_find", beginFind))
      (bind(registerRpcMethod, "stop_find", stopFind))
      (bind(registerRpcMethod, "clear_find_results", clearFindResults))
      (bind(registerRpcMethod, "preview_replace", previewReplace))
      (bind(registerRpcMethod, "complete_replace", completeReplace))
      (bind(registerRpcMethod, "stop_replace", stopReplace));
   return initBlock.execute();
}

boost::regex getGrepOutputRegex(bool isGitGrep)
{
   boost::regex regex;
   if (isGitGrep)
      regex = boost::regex("^((?:[a-zA-Z]:)?[^:]+)\x1B\\[\\d+?m:\x1B\\[m(\\d+)\x1B\\[36m:\x1B\\[m(.*)");
   else
      regex = boost::regex("^((?:[a-zA-Z]:)?[^:]+):(\\d+):(.*)");
   return regex;
}

boost::regex getColorEncodingRegex(bool isGitGrep)
{
   boost::regex regex;
   if (isGitGrep)
      regex = boost::regex("\x1B\\[((\\d+)?(\\;)?\\d+)?m");
   else
      regex = boost::regex("\x1B\\[(\\d\\d)?m(\x1B\\[K)?");
   return regex;
}


Error Replacer::replacePreview(const size_t dMatchOn, const size_t dMatchOff,
                               size_t eMatchOn, size_t eMatchOff,
                               std::string* pEncodedLine, std::string* pDecodedLine,
                               size_t* pReplaceMatchOff) const
{
   // attempt to perform the replace based on the encoded data
   std::string previewLine(*pEncodedLine);
   std::string originalValue = previewLine.substr(eMatchOn, eMatchOff  - eMatchOn);
   Error error = replaceRegex(eMatchOn, eMatchOff,
                              findResults().searchPattern(),
                              findResults().replacePattern(),
                              &previewLine,
                              pReplaceMatchOff);
   
   // Concatenate the replace string to the matched string and insert this into the original line
   // so it contains the before and after.
   // The preview string is always returned in the decoded string.
   if (!error)
   {
      std::string replaceString = previewLine.substr(eMatchOn, *pReplaceMatchOff - eMatchOn);
      replaceString.insert(0, originalValue);
      replaceLiteral(eMatchOn, eMatchOff, replaceString, pEncodedLine, pReplaceMatchOff);

      // adjust pReplaceMatchOff for display

      size_t originalDecodedSize;
      error = string_utils::utf8Distance(pDecodedLine->begin(),
                                         pDecodedLine->end(),
                                         &originalDecodedSize);

      *pDecodedLine = decode(*pEncodedLine);

      size_t newDecodedSize;
      error = string_utils::utf8Distance(pDecodedLine->begin(),
                                         pDecodedLine->end(),
                                         &newDecodedSize);

      *pReplaceMatchOff = dMatchOff + (newDecodedSize - originalDecodedSize);
   }

   return error;
}

core::Error Replacer::completeReplace(const boost::regex& searchRegex,
                                      const std::string& replaceRegex,
                                      size_t matchOn, size_t matchOff, std::string* pLine,
                                      size_t* pReplaceMatchOff) const
{
   std::string begin(pLine->substr(0, matchOn));
   std::string end(pLine->substr(matchOff));
   std::string newLine;

   try
   {
      newLine = boost::regex_replace(pLine->substr(matchOn), searchRegex, replaceRegex,
         boost::format_sed | boost::format_first_only);
   }
   catch (const boost::regex_error& e)
   {
      core::Error error(
         errc::findCategory(),
         errc::RegexError,
         "A regex error occurred during replace operation: " + std::string(e.what()),
         ERROR_LOCATION);

      error.addProperty("position", gsl::narrow_cast<int>(e.position()));
      return error;
   }

   newLine.insert(0, begin);
   *pReplaceMatchOff = newLine.length() - end.length();
   *pLine = newLine;

   return core::Success();
}

core::Error Replacer::replaceRegexIgnoreCase(size_t matchOn, size_t matchOff,
                                             const std::string& findRegex,
                                             const std::string& replaceRegex, std::string* pLine,
                                             size_t* pReplaceMatchOff) const
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
      core::Error error(
         errc::findCategory(),
         errc::RegexError,
         "A regex error occurred during replace operation: " + std::string(e.what()),
         ERROR_LOCATION);

      error.addProperty("position", gsl::narrow_cast<int>(e.position()));
      return error;
   }
}

core::Error Replacer::replaceRegexWithCase(size_t matchOn, size_t matchOff,
                                           const std::string& findRegex,
                                           const std::string& replaceRegex, std::string* pLine,
                                           size_t* pReplaceMatchOff) const
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
      core::Error error(
         errc::findCategory(),
         errc::RegexError,
         "A regex error occurred during replace operation: " + std::string(e.what()),
         ERROR_LOCATION);

      error.addProperty("position", gsl::narrow_cast<int>(e.position()));
      return error;
   }
}

std::string Replacer::decode(const std::string& encoded) const
{
   bool firstDecodeError = false;
   return decode(encoded, encoding_, firstDecodeError);
}

std::string Replacer::decode(const std::string& encoded, const std::string& encoding,
                             bool& firstDecodeError)
{
   if (encoded.empty())
      return encoded;

   std::string decoded;
   Error error = r::util::iconvstr(encoded, encoding, "UTF-8", true,
                                   &decoded);

   // Log error, but only once per grep operation
   if (error && firstDecodeError)
   {
      firstDecodeError = false;
      LOG_ERROR(error);
   }

   return decoded;
}


} // namespace find
} // namespace modules
} // namespace session
} // namespace rstudio
