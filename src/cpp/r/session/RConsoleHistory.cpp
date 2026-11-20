/*
 * RConsoleHistory.cpp
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

#include <r/session/RConsoleHistory.hpp>

#include <algorithm>
#include <vector>

#include <boost/function.hpp>
#include <boost/tokenizer.hpp>
#include <boost/bind/bind.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>
#include <shared_core/SafeConvert.hpp>
#include <gsl/gsl-lite.hpp>

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace r {
namespace session {

namespace {

// R's default for R_HISTSIZE
const int kDefaultHistorySize = 512;

// R_HISTSIZE as an int; like R, a negative or invalid value means the default.
// R also accepts a k/K/M/G suffix (R_Decode2Long(), which isn't part of R's
// API); such a value is invalid here
int rHistorySize()
{
   std::string histSize = core::system::getenv("R_HISTSIZE");
   int size = safe_convert::stringTo<int>(histSize, kDefaultHistorySize);
   return size < 0 ? kDefaultHistorySize : size;
}

} // anonymous namespace

ConsoleHistory& consoleHistory()
{
   static ConsoleHistory instance;
   return instance;
}

ConsoleHistory::ConsoleHistory()
   : removeDuplicates_(true)
{
   setCapacity(kDefaultHistorySize);
}

void ConsoleHistory::setCapacity(int capacity)
{
   // loading a history file into a capacity of 0 crashes (#19690), and a
   // negative one converts to a huge size_t
   historyBuffer_.set_capacity(std::max(capacity, 1));
}

void ConsoleHistory::setCapacityFromRHistsize()
{
   // in R, R_HISTSIZE limits only the commands saved to a history file, not
   // those kept during the session (see saveToHistoryFile()), so a smaller
   // value doesn't shrink the session's history
   setCapacity(std::max(kDefaultHistorySize, rHistorySize()));
}

void ConsoleHistory::setRemoveDuplicates(bool removeDuplicates)
{
   removeDuplicates_ = removeDuplicates;
}

void ConsoleHistory::add(const std::string& command, bool update)
{
   if (!command.empty())
   {
      // split input into list of commands
      boost::char_separator<char> lineSep("\n");
      boost::tokenizer<boost::char_separator<char> > lines(command, lineSep);
      for (boost::tokenizer<boost::char_separator<char> >::iterator 
           lineIter = lines.begin();
           lineIter != lines.end();
           ++lineIter)
      {
         // get line (skip empty lines)
         std::string line(*lineIter);
         boost::algorithm::trim(line);
         if (line.empty())
            continue;
         
         // add this line if its not a duplicate
         if (!removeDuplicates_ ||
             historyBuffer_.empty() ||
             (line != historyBuffer_.back()))
         {
            // add to buffer
            historyBuffer_.push_back(line);
         
            // notify listeners
            onAdd_(line, update);
         }
      }
   }
}

void ConsoleHistory::clear()
{
   historyBuffer_.clear();
}


void ConsoleHistory::remove(const std::vector<int>& indexes)
{
   // make a copy and sort descending
   std::vector<int> sortedIndexes;
   std::copy(indexes.begin(),
             indexes.end(),
             std::back_inserter(sortedIndexes));
   std::sort(sortedIndexes.begin(), sortedIndexes.end(),std::greater<int>());

   // remove them all
   std::for_each(sortedIndexes.begin(),
                 sortedIndexes.end(),
                 boost::bind(&ConsoleHistory::safeRemove, this, _1));
}
   
void ConsoleHistory::subset(int beginIndex, // inclusive
                            int endIndex,   // exclusive,
                            std::vector<std::string>* pEntries) const
{
   // clear existing
   pEntries->clear();

   // bail if begin index exceeds our number of entries
   if (beginIndex >= size())
      return;

   // cap end index at our size
   endIndex = std::min(endIndex, size());

   // copy
   std::copy(historyBuffer_.begin() + beginIndex,
             historyBuffer_.begin() + endIndex,
             std::back_inserter(*pEntries));
}



void ConsoleHistory::asJson(json::Array* pHistoryArray) const
{
   const_iterator it = begin();
   while(it != end())
      pHistoryArray->push_back(*it++);
}
      
Error ConsoleHistory::loadFromFile(const FilePath& filePath,
                                   bool verifyFile)
{
   // a failed load keeps the current history; it would otherwise be what the
   // session saves to .Rhistory on quit
   if (!filePath.exists())
   {
      if (verifyFile)
         return systemError(boost::system::errc::no_such_file_or_directory, ERROR_LOCATION);

      // tolerate file not found -- the user may not have any prior history
      historyBuffer_.clear();
      return Success();
   }

   // libc++ opens a directory and reads it as an empty file, which would
   // replace the history with nothing
   if (filePath.isDirectory())
      return systemError(boost::system::errc::is_a_directory, ERROR_LOCATION);

   HistoryBuffer loaded(historyBuffer_.capacity());
   Error error = core::readCollectionFromFile<HistoryBuffer>(
                                                      filePath,
                                                      &loaded,
                                                      core::parseString);
   if (error)
      return error;

   historyBuffer_.swap(loaded);
   return Success();
}
   
Error ConsoleHistory::saveToFile(const FilePath& filePath, bool atomic) const
{
   return core::writeCollectionToFile<HistoryBuffer>(
                                                      filePath,
                                                      historyBuffer_,
                                                      core::stringifyString,
                                                      atomic);
}

Error ConsoleHistory::saveToHistoryFile(const FilePath& filePath) const
{
   std::vector<std::string> commands;
   subset(size() - std::min(size(), rHistorySize()), size(), &commands);

   return core::writeCollectionToFile<std::vector<std::string> >(
                                                      filePath,
                                                      commands,
                                                      core::stringifyString,
                                                      false /* atomic */);
}

void ConsoleHistory::safeRemove(int index)
{
   if (index >= 0 && index < (int)historyBuffer_.size())
      historyBuffer_.erase(historyBuffer_.begin() + index);
}

} // namespace session
} // namespace r
} // namespace rstudio



