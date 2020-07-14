/*
 * FileChangeEvent.hpp
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

#ifndef CORE_SYSTEM_FILE_CHANGE_EVENT_HPP
#define CORE_SYSTEM_FILE_CHANGE_EVENT_HPP

#include <string>
#include <iostream>
#include <vector>

#include <boost/function.hpp>

#include <core/FileInfo.hpp>

namespace rstudio {
namespace core {

class Error;
   
namespace system {

// event struct
class FileChangeEvent
{
public:
   // NOTE: skip 2 for compatibility with old clients (used to be FileRenamed)
   enum Type 
   { 
      None = 0,
      FileAdded = 1,
      FileRemoved = 3,
      FileModified = 4
   };
   
public:
   FileChangeEvent(Type type, const core::FileInfo& fileInfo)
      : type_(type), fileInfo_(fileInfo)
   {
   }
   
   // COPYING: via compiler
   
   FileChangeEvent& operator=(const FileChangeEvent& rhs)
   {
      if (&rhs != this)
      {
         type_ = rhs.type_;
         fileInfo_ = rhs.fileInfo_;
      }
      return *this;
   }
   
public:
   Type type() const { return type_; }
   const FileInfo& fileInfo() const { return fileInfo_; }
   
private:
   Type type_;
   core::FileInfo fileInfo_;
};

inline std::ostream& operator << (std::ostream& ostr, 
                                  const FileChangeEvent& event)
{
   if (event.type() == FileChangeEvent::FileAdded)
      ostr << "FileAdded: ";
   else if (event.type() == FileChangeEvent::FileRemoved)
      ostr << "FileRemoved: ";
   else if (event.type() == FileChangeEvent::FileModified)
      ostr << "FileModified: ";
      
   ostr << event.fileInfo();
   
   if (event.fileInfo().isDirectory())
      ostr << " (directory)";
      
   return ostr;
}

template<typename PreviousIterator, typename CurrentIterator>
void collectFileChangeEvents(PreviousIterator prevBegin,
                             PreviousIterator prevEnd,
                             CurrentIterator currBegin,
                             CurrentIterator currEnd,
                             const boost::function<bool(const FileInfo&)>& filter,
                             std::vector<FileChangeEvent>* pEvents)
{
   // sort the ranges
   std::vector<FileInfo> prev;
   std::copy(prevBegin, prevEnd, std::back_inserter(prev));
   std::sort(prev.begin(), prev.end(), fileInfoPathLessThan);
   std::vector<FileInfo> curr;
   std::copy(currBegin, currEnd, std::back_inserter(curr));
   std::sort(curr.begin(), curr.end(), fileInfoPathLessThan);

   // initalize the iterators
   std::vector<FileInfo>::iterator prevIt = prev.begin();
   std::vector<FileInfo>::iterator currIt = curr.begin();

   FileInfo noFile;
   while (prevIt != prev.end() || currIt != curr.end())
   {
      const FileInfo& prevFile = prevIt != prev.end() ? *prevIt : noFile;
      const FileInfo& currFile = currIt != curr.end() ? *currIt : noFile;

      int comp;
      if (prevFile.empty())
         comp = 1;
      else if (currFile.empty())
         comp = -1;
      else
         comp = fileInfoPathCompare(prevFile, currFile);

      if (comp == 0)
      {
         if (currFile.lastWriteTime() != prevFile.lastWriteTime())
         {
            if (!filter || filter(currFile))
            {
               pEvents->push_back(FileChangeEvent(FileChangeEvent::FileModified,
                                                  currFile));
            }
         }
         prevIt++;
         currIt++;
      }
      else if (comp < 0)
      {
         if (!filter || filter(prevFile))
         {
            pEvents->push_back(FileChangeEvent(FileChangeEvent::FileRemoved,
                                               prevFile));
         }
         prevIt++;
      }
      else // comp > 1
      {
         if (!filter || filter(currFile))
         {
            pEvents->push_back(FileChangeEvent(FileChangeEvent::FileAdded,
                                               currFile));
         }
         currIt++;
      }
   }
}

template<typename PreviousIterator, typename CurrentIterator>
void collectFileChangeEvents(PreviousIterator prevBegin,
                             PreviousIterator prevEnd,
                             CurrentIterator currBegin,
                             CurrentIterator currEnd,
                             std::vector<FileChangeEvent>* pEvents)
{
   collectFileChangeEvents(prevBegin,
                           prevEnd,
                           currBegin,
                           currEnd,
                           boost::function<bool(const FileInfo&)>(),
                           pEvents);
}
  
} // namespace system
} // namespace core 
} // namespace rstudio

#endif // CORE_SYSTEM_FILE_CHANGE_EVENT_HPP


