/*
 * RConsoleActions.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <r/session/RConsoleActions.hpp>

#include <gsl/gsl-lite.hpp>

#include <boost/algorithm/string/split.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/Log.hpp>
#include <core/FileSerializer.hpp>
#include <core/StringUtils.hpp>
#include <core/Thread.hpp>
#include <core/text/AnsiCodeParser.hpp>

#include <r/ROptions.hpp>

using namespace rstudio::core;

#define kChunkSize 512

namespace rstudio {
namespace r {
namespace session {

namespace {
const char * const kActionType = "type";
const char * const kActionData = "data";
}
   
ConsoleActions& consoleActions()
{
   static ConsoleActions instance;
   return instance;
}
   
ConsoleActions::ConsoleActions()
{
   setCapacity(1000);
}
   
int ConsoleActions::capacity() const
{
   LOCK_MUTEX(mutex_)
   {
      return gsl::narrow_cast<int>(actions_.capacity());
   }
   END_LOCK_MUTEX

   // keep compiler happy
   return 0;
}

void ConsoleActions::setCapacity(int capacity)
{
   LOCK_MUTEX(mutex_)
   {
      actions_.set_capacity(capacity);
   }
   END_LOCK_MUTEX
}

void ConsoleActions::pushAction(int type, std::string data)
{
   // NOTE: helper method assumes mutex is already locked

   // ensure the action data is valid UTF-8, as console actions are
   // round-tripped through JSON -- both when session state is suspended
   // and resumed, and on every client init (sessionInfo's console_actions)
   // -- and the JSON parser rejects invalid UTF-8 on restore
   string_utils::utf8Clean(data.begin(), data.end(), '?');

   ConsoleAction action;
   action.type = type;
   action.data = std::move(data);
   actions_.push_back(std::move(action));
}

void ConsoleActions::flush()
{
   // NOTE: helper method assumes mutex is already locked

   // iterate over buffer data in chunks, split via newlines
   std::size_t lhs = 0;
   std::size_t rhs = buffer_.data.find('\n');
   while (rhs != std::string::npos)
   {
      // consume this line of data in chunks, taking care not to
      // split multi-byte UTF-8 sequences across chunk boundaries
      while (lhs + kChunkSize < rhs)
      {
         std::string data = buffer_.data.substr(lhs, kChunkSize);
         data.resize(data.size() - string_utils::utf8IncompleteSuffixLength(data));

         lhs += data.size();
         pushAction(buffer_.type, std::move(data));
      }

      // consume any remaining data on this line
      // (include the newline character)
      if (lhs <= rhs)
         pushAction(buffer_.type, buffer_.data.substr(lhs, rhs - lhs + 1));

      // advance to next line
      lhs = rhs + 1;
      rhs = buffer_.data.find('\n', lhs);
   }

   // push any remaining partial line, holding back a trailing incomplete
   // UTF-8 sequence: output reaches add() as arbitrary byte windows over a
   // pipe, so the rest of the character may still be in flight, and
   // pushAction() would otherwise sanitize the torn character to '?'
   if (lhs < buffer_.data.size())
   {
      std::string data = buffer_.data.substr(lhs);
      data.resize(data.size() - string_utils::utf8IncompleteSuffixLength(data));

      lhs += data.size();
      if (!data.empty())
         pushAction(buffer_.type, std::move(data));
   }

   // remove consumed data, retaining any held-back suffix for the next add()
   buffer_.data.erase(0, lhs);
}

void ConsoleActions::add(int type, const std::string& data)
{
   LOCK_MUTEX(mutex_)
   {
      // manage pending input buffer
      if (type == kConsoleActionPrompt &&
          data == r::options::getOption<std::string>("prompt"))
      {
         pendingInput_.clear();
      }
      else if (type == kConsoleActionInput)
      {
         std::vector<std::string> input;
         boost::algorithm::split(input, data, boost::algorithm::is_any_of("\n"));
         pendingInput_.insert(pendingInput_.end(), input.begin(), input.end());
      }
      else if (data.empty())
      {
         return;
      }

      // if we've received more output of the same type, then
      // we'll append that to our action buffer
      bool isOutputType =
         type == kConsoleActionOutput ||
         type == kConsoleActionOutputError;

      if (isOutputType && type == buffer_.type)
      {
         buffer_.data.append(data);
      }
      else
      {
         // the output type has changed; flush our buffer
         flush();

         // an incomplete UTF-8 suffix held back by flush() can no longer be
         // completed once the type changes; push it (sanitized) rather than
         // gluing it to data of a different type
         if (!buffer_.data.empty())
            pushAction(buffer_.type, std::move(buffer_.data));

         // update the action with the newly-provided data
         buffer_.type = type;
         buffer_.data = data;
      }
   }
   END_LOCK_MUTEX
}

void ConsoleActions::notifyInterrupt()
{
   LOCK_MUTEX(mutex_)
   {
      pendingInput_.clear();
   }
   END_LOCK_MUTEX
}

void ConsoleActions::reset()
{
   LOCK_MUTEX(mutex_)
   {
      // clear the existing actions
      actions_.clear();
      buffer_.data.clear();
   }
   END_LOCK_MUTEX
}
   
void ConsoleActions::asJson(json::Object* pActions) 
{
   LOCK_MUTEX(mutex_)
   {
      // clear inbound
      pActions->clear();

      // flush any pending console output
      flush();

      // copy actions and insert into destination
      json::Array actionsType;
      json::Array actionsData;
      
      // pull from existing actions
      for (auto&& action : actions_)
      {
         actionsType.push_back(json::Value(action.type));
         actionsData.push_back(json::Value(action.data));
      }
      
      pActions->operator[](kActionType) = actionsType;
      pActions->operator[](kActionData) = actionsData;
   }
   END_LOCK_MUTEX
}

Error ConsoleActions::loadFromFile(const FilePath& filePath)
{
   LOCK_MUTEX(mutex_)
   {
      Error error;
      
      // read from file
      std::string contents;
      error = readStringFromFile(filePath, &contents);
      if (error && !isFileNotFoundError(error))
         return error;

      // bail if file is empty
      if (contents.empty())
         return Success();
      
      // parse JSON; note that the parser validates UTF-8, and a console
      // actions file can contain invalid UTF-8 -- e.g. one written by a
      // version predating the UTF-8-aware chunking above, or a file that
      // was truncated on disk. console actions are merely a replay of prior
      // console output, so treat unusable files as empty rather than
      // failing session restore
      json::Object value;
      error = value.parse(contents);
      if (error)
      {
         WLOGF("Discarding unparseable console actions file '{}': {}",
               filePath.getAbsolutePath(),
               error.getSummary());
         return Success();
      }

      // read type + data fields
      json::Value typeJson = value.getObject()[kActionType];
      json::Value dataJson = value.getObject()[kActionData];
      if (!typeJson.isArray() || !dataJson.isArray())
      {
         WLOGF("Discarding console actions file '{}': expected 'type' and 'data' arrays",
               filePath.getAbsolutePath());
         return Success();
      }

      // a length mismatch cannot be produced by asJson(), which fills both
      // arrays in lockstep, so it implies a broken writer -- log loudly
      json::Array typeArray = typeJson.getArray();
      json::Array dataArray = dataJson.getArray();
      if (typeArray.getSize() != dataArray.getSize())
      {
         ELOGF("Discarding console actions file '{}': 'type' has {} entries but 'data' has {}",
               filePath.getAbsolutePath(),
               typeArray.getSize(),
               dataArray.getSize());
         return Success();
      }

      std::size_t skipped = 0;
      for (std::size_t i = 0, n = typeArray.getSize(); i < n; i++)
      {
         if (!typeArray[i].isInt() || !dataArray[i].isString())
         {
            skipped++;
            continue;
         }

         ConsoleAction action;
         action.type = typeArray[i].getInt();
         action.data = dataArray[i].getString();
         actions_.push_back(std::move(action));
      }

      if (skipped != 0)
      {
         WLOGF("Skipped {} malformed entries in console actions file '{}'",
               skipped,
               filePath.getAbsolutePath());
      }

   }
   END_LOCK_MUTEX
   
   return Success();
}
   
Error ConsoleActions::saveToFile(const core::FilePath& filePath)
{
   // write actions
   json::Object actionsObject;
   asJson(&actionsObject);
   std::string contents = actionsObject.writeFormatted();

   // the sanitization in pushAction() is lenient -- it accepts some byte
   // sequences (e.g. overlong encodings, UTF-16 surrogate halves) that the
   // JSON parser rejects -- and the JSON writer does not validate encoding,
   // so confirm the serialized actions actually parse. otherwise the file
   // would be silently discarded by loadFromFile() on restore; better to
   // save an empty replay and log the loss here, while the cause is live
   json::Object validationObject;
   Error error = validationObject.parse(contents);
   if (error)
   {
      ELOGF("Console actions did not serialize as valid JSON; discarding console replay: {}",
            error.getSummary());

      json::Object emptyObject;
      emptyObject[kActionType] = json::Array();
      emptyObject[kActionData] = json::Array();
      contents = emptyObject.writeFormatted();
   }

   // write to file
   return writeStringToFile(filePath, contents);
}

std::vector<std::string> ConsoleActions::getConsoleLines(int limit,
                                                         int offset,
                                                         bool fromBottom,
                                                         int maxChars)
{
   std::vector<std::string> result;

   LOCK_MUTEX(mutex_)
   {
      // Flush pending output to ensure we have all data
      flush();

      // Concatenate all action data into a single string
      // Only add newlines after input/output/error actions that don't have them
      // (prompts should NOT get newlines since input follows on the same line)
      std::string allContent;
      for (const auto& action : actions_)
      {
         allContent.append(action.data);

         // Add newline after input/output/error if missing (but not prompts).
         // Actions of exactly kChunkSize bytes are also excluded: flush()
         // splits long lines into kChunkSize chunks, so such an action
         // continues in the next one -- a synthetic newline here would break
         // the line apart and could cut an escape sequence in half, defeating
         // the ANSI stripping below
         if (action.type != kConsoleActionPrompt &&
             !action.data.empty() &&
             action.data.back() != '\n' &&
             action.data.size() != kChunkSize)
         {
            allContent.push_back('\n');
         }
      }

      // Strip ANSI escape sequences (colors, cursor positioning, etc.)
      text::stripAnsiCodes(&allContent);

      // Split by newlines to get individual lines
      std::vector<std::string> allLines;
      boost::algorithm::split(allLines, allContent, boost::algorithm::is_any_of("\n"));

      // Remove trailing empty line if present (from trailing newline)
      if (!allLines.empty() && allLines.back().empty())
      {
         allLines.pop_back();
      }

      // Calculate start and end indices based on direction
      int totalLines = gsl::narrow_cast<int>(allLines.size());
      int startIdx, endIdx;

      if (fromBottom)
      {
         // Start from bottom (most recent)
         // offset=0 means start at the last line
         endIdx = totalLines - offset;
         startIdx = std::max(0, endIdx - limit);
      }
      else
      {
         // Start from top (oldest)
         startIdx = offset;
         endIdx = std::min(totalLines, startIdx + limit);
      }

      // Collect lines within bounds, respecting maxChars
      int totalChars = 0;
      for (int i = startIdx; i < endIdx && i < totalLines; ++i)
      {
         if (i < 0)
            continue;

         const std::string& line = allLines[i];
         int lineChars = gsl::narrow_cast<int>(line.size());

         // Check if adding this line would exceed maxChars
         if (totalChars + lineChars > maxChars)
            break;

         result.push_back(line);
         totalChars += lineChars;
      }
   }
   END_LOCK_MUTEX

   return result;
}
   
} // namespace session
} // namespace r
} // namespace rstudio



