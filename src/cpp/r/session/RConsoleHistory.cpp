/*
 * RConsoleHistory.cpp
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

#include <r/session/RConsoleHistory.hpp>

#include <boost/function.hpp>
#include <boost/tokenizer.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>

using namespace core;

namespace r {
namespace session {   
   
ConsoleHistory& consoleHistory()
{
   static ConsoleHistory instance ;
   return instance ;
}
   
ConsoleHistory::ConsoleHistory()
{
   setCapacity(250);
}
   
void ConsoleHistory::setCapacity(int capacity)
{
   historyBuffer_.set_capacity(capacity);
}

void ConsoleHistory::add(const std::string& command)
{
   if (!command.empty())
   {
      // split input into list of commands
      std::vector<std::string> commands;
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
         
         // add to buffer
         historyBuffer_.push_back(line);
         
         // notify listeners
         onAdd_(line);
      }
   }
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

} // namespace session
} // namespace r



