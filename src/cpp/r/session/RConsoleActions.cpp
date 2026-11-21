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

#include <algorithm>

#include <gsl/gsl-lite.hpp>

#include <boost/algorithm/string/split.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/Memory.hpp>

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
   // leaked: console output is flushed here from background threads (#18318)
   static ConsoleActions& instance = core::make_leaked<ConsoleActions>();
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
      // Keep the newest actions when reducing the scrollback limit.
      actions_.rset_capacity(capacity);
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
   return writeStringToFileAtomic(filePath, contents);
}

namespace {

// The console's lines are read from its actions as if their data had been
// joined into one string. The helpers below locate and read lines within
// that string without building it, so that reading a page of lines costs
// about as much as the lines skipped to reach it (which are scanned, but not
// copied) and the lines in it, and not as much as the whole scrollback. They
// are templates only so as not to depend on the type of the actions buffer.

// The start of a line: an offset into the data of the action at an index.
// Lines needn't start at the start of an action, nor end in the action that
// they start in.
struct LineStart
{
   std::size_t index;
   std::size_t offset;

   bool operator==(const LineStart& other) const
   {
      return index == other.index && offset == other.offset;
   }
};

// A line, stripped of escape sequences, and where it starts.
struct Line
{
   LineStart start;
   std::string text;
};

// Whether the action at an index is a chunk of a long line, which continues
// in the action after it. flush() splits long lines into chunks of kChunkSize
// bytes, or of up to 3 bytes fewer where that would have cut a UTF-8 sequence
// in two; the action after such a chunk then starts with that sequence. The
// rest of the line is always the next action, and of the same type: an action
// that a different type follows (say, input of exactly kChunkSize bytes, and
// then its output) ends its line.
template <typename Actions>
bool isChunk(const Actions& actions, std::size_t index)
{
   const ConsoleAction& action = actions[index];

   std::size_t size = action.data.size();
   if (size > kChunkSize || size + 3 < kChunkSize || index + 1 == actions.size())
      return false;

   const ConsoleAction& next = actions[index + 1];
   if (next.type != action.type)
      return false;

   if (size == kChunkSize)
      return true;

   // a shorter chunk, if the bytes that would have filled it are the start,
   // and no more, of a UTF-8 sequence
   std::size_t missing = kChunkSize - size;
   return string_utils::utf8IncompleteSuffixLength(next.data.substr(0, missing)) == missing;
}

// Whether a line ends after the action at an index, though its data doesn't
// end with a newline. That's so for input, and for output whose line is still
// to be completed, but not for prompts (the input follows on the same line),
// nor for chunks of a long line. Ending the line after a chunk would break it
// apart, and could cut an escape sequence in half, hiding it from the ANSI
// stripping.
template <typename Actions>
bool endsLine(const Actions& actions, std::size_t index)
{
   const ConsoleAction& action = actions[index];
   return action.type != kConsoleActionPrompt &&
          !action.data.empty() &&
          action.data.back() != '\n' &&
          !isChunk(actions, index);
}

// Read the line starting at *pStart into *pLine (which may be null, to skip
// the line), without its newline, and advance *pStart to the line after it.
// Returns false for the last line: the one that no newline follows.
template <typename Actions>
bool readLine(const Actions& actions, LineStart* pStart, std::string* pLine)
{
   std::size_t index = pStart->index;
   std::size_t offset = pStart->offset;

   for (std::size_t n = actions.size(); index < n; index++, offset = 0)
   {
      const ConsoleAction& action = actions[index];

      std::size_t newline = action.data.find('\n', offset);
      if (newline != std::string::npos)
      {
         if (pLine)
            pLine->append(action.data, offset, newline - offset);

         *pStart = {index, newline + 1};
         return true;
      }

      if (pLine)
         pLine->append(action.data, offset, std::string::npos);

      if (endsLine(actions, index))
      {
         *pStart = {index + 1, 0};
         return true;
      }
   }

   *pStart = {index, 0};
   return false;
}

// Find the page of up to 'take' lines that ends 'skip' lines above the last
// line, by walking backwards from the end of the actions. Sets *pStart to the
// start of the page's first line, and returns the number of lines in the page:
// fewer than 'take' if the page runs off the top, and zero or less if all of
// it lies above the top. Also sets *pLastLine to the last line, which has to
// be read to find the page, so that it needn't be read again: flush() stores
// an unfinished line as one action however long it grows (as a progress bar's
// can), and this is all under the console actions mutex.
template <typename Actions>
long long findPageFromBottom(const Actions& actions,
                             long long skip,
                             long long take,
                             LineStart* pStart,
                             Line* pLastLine)
{
   // the number of line starts seen so far, and the number to see before the
   // one at the top of the page is reached
   long long found = 0;
   long long wanted = 0;

   // whether the last line is empty, and so isn't counted as a line
   bool lastLineEmpty = false;

   // called with each line start, from the last line's upwards; returns true
   // once the top of the page is reached
   auto visit = [&](std::size_t index, std::size_t offset) {
      *pStart = {index, offset};

      if (found == 0)
      {
         pLastLine->start = *pStart;
         LineStart start = *pStart;
         readLine(actions, &start, &pLastLine->text);
         text::stripAnsiCodes(&pLastLine->text);
         lastLineEmpty = pLastLine->text.empty();
         wanted = skip + take + (lastLineEmpty ? 1 : 0);
      }

      return ++found == wanted;
   };

   // a line starts after each newline, including the ones implied at the
   // ends of actions ...
   bool done = false;
   for (std::size_t index = actions.size(); index > 0 && !done; index--)
   {
      const ConsoleAction& action = actions[index - 1];

      if (endsLine(actions, index - 1))
         done = visit(index, 0);

      std::size_t newline = action.data.size();
      while (!done && newline > 0)
      {
         newline = action.data.rfind('\n', newline - 1);
         if (newline == std::string::npos)
            break;

         done = visit(index - 1, newline + 1);
      }
   }

   // ... and at the very start
   if (!done)
      visit(0, 0);

   return found - (lastLineEmpty ? 1 : 0) - skip;
}

// The lines that getConsoleLines() returns.
template <typename Actions>
std::vector<std::string> readConsoleLines(const Actions& actions,
                                          int limit,
                                          int offset,
                                          bool fromBottom,
                                          int maxChars)
{
   std::vector<std::string> result;

   // the page, as a number of lines to skip (from the bottom or the top) and
   // then a number of lines to take. a negative offset skips nothing, and
   // shortens the page instead
   long long skip = std::max(offset, 0);
   long long take = static_cast<long long>(limit) + std::min(offset, 0);
   if (take <= 0)
      return result;

   // find the first line of the page
   LineStart start = {0, 0};
   Line lastLine = {};
   if (fromBottom)
   {
      take = std::min(take, findPageFromBottom(actions, skip, take, &start, &lastLine));
   }
   else
   {
      for (long long i = 0; i < skip; i++)
      {
         if (!readLine(actions, &start, nullptr))
            return result;
      }
   }

   // read the page's lines, for as long as they fit within maxChars
   long long totalChars = 0;
   for (long long i = 0; i < take; i++)
   {
      std::string line;
      bool isLastLine;
      if (fromBottom && start == lastLine.start)
      {
         line = std::move(lastLine.text);
         isLastLine = true;
      }
      else
      {
         // escape sequences (colors, cursor positioning, etc.) are stripped a
         // line at a time, as none can contain a newline
         isLastLine = !readLine(actions, &start, &line);
         text::stripAnsiCodes(&line);
      }

      // an empty last line is what follows the final newline; not a line
      if (isLastLine && line.empty())
         break;

      long long lineChars = static_cast<long long>(line.size());
      if (totalChars + lineChars > maxChars)
         break;

      result.push_back(std::move(line));
      totalChars += lineChars;

      if (isLastLine)
         break;
   }

   return result;
}

} // anonymous namespace

std::vector<std::string> ConsoleActions::getConsoleLines(int limit,
                                                         int offset,
                                                         bool fromBottom,
                                                         int maxChars)
{
   LOCK_MUTEX(mutex_)
   {
      // Flush pending output to ensure we have all data
      flush();

      return readConsoleLines(actions_, limit, offset, fromBottom, maxChars);
   }
   END_LOCK_MUTEX

   // keep compiler happy
   return std::vector<std::string>();
}

bool ConsoleActions::containsOutput(const std::string& text)
{
   LOCK_MUTEX(mutex_)
   {
      flush();

      // concatenate in order, so text that spans several actions (one per
      // line, or per chunk of a long line) is matched as a whole
      std::string output;
      for (const auto& action : actions_)
      {
         if (action.type == kConsoleActionOutput ||
             action.type == kConsoleActionOutputError)
         {
            output.append(action.data);
         }
      }

      text::stripAnsiCodes(&output);
      return output.find(text) != std::string::npos;
   }
   END_LOCK_MUTEX

   // keep compiler happy
   return false;
}

} // namespace session
} // namespace r
} // namespace rstudio



