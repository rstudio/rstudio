/*
 * RConsoleHistory.cpp
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

#include <r/session/RConsoleHistory.hpp>

#include <boost/bind.hpp>
#include <boost/function.hpp>
#include <boost/tokenizer.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>
#include <shared_core/SafeConvert.hpp>
#include <gsl/gsl>

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace session {   
   
ConsoleHistory& consoleHistory()
{
   static ConsoleHistory instance;
   return instance;
}
   
ConsoleHistory::ConsoleHistory()
   : removeDuplicates_(true)
{
   setCapacity(512);
}
   
void ConsoleHistory::setCapacity(int capacity)
{
   historyBuffer_.set_capacity(capacity);
}

void ConsoleHistory::setCapacityFromRHistsize()
{
   std::string histSize = core::system::getenv("R_HISTSIZE");
   if (!histSize.empty())
   {
      setCapacity(safe_convert::stringTo<int>(histSize, capacity()));
   }
}

void ConsoleHistory::setRemoveDuplicates(bool removeDuplicates)
{
   removeDuplicates_ = removeDuplicates;
}

void ConsoleHistory::add(const std::string& command)
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
         
         // add this line if its not a duplciate
         if (!removeDuplicates_ ||
             historyBuffer_.empty() ||
             (line != historyBuffer_.back()))
         {
            // add to buffer
            historyBuffer_.push_back(line);
         
            // notify listeners
            onAdd_(line);
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
   historyBuffer_.clear();
   
   // tolerate file not found -- the user may not have any prior history
   if (filePath.exists())
   {
      return core::readCollectionFromFile<boost::circular_buffer<std::string> >(
                                                      filePath,
                                                      &historyBuffer_,
                                                      core::parseString);
   }
   else if (verifyFile)
   {
      return systemError(boost::system::errc::no_such_file_or_directory,
                         ERROR_LOCATION);
   }
   else
   {
      return Success();
   }
}
   
Error ConsoleHistory::saveToFile(const FilePath& filePath) const
{
   return core::writeCollectionToFile<boost::circular_buffer<std::string> >(
                                                      filePath,
                                                      historyBuffer_,
                                                      core::stringifyString);
}

void ConsoleHistory::safeRemove(int index)
{
   if (index >= 0 && index < (int)historyBuffer_.size())
      historyBuffer_.erase(historyBuffer_.begin() + index);
}

} // namespace session
} // namespace r
} // namespace rstudio



