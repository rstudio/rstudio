/*
 * FileChangeEvent.hpp
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

#ifndef CORE_SYSTEM_FILE_CHANGE_EVENT_HPP
#define CORE_SYSTEM_FILE_CHANGE_EVENT_HPP

#include <string>
#include <iostream>

#include <core/FileInfo.hpp>

namespace core {

class Error;
   
namespace system {

// event struct
class FileChangeEvent
{
public:
   // NOTE: skip 2 for compatabilty with old clients (used to be FileRenamed)
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
      ostr << "FileAdded: " ;
   else if (event.type() == FileChangeEvent::FileRemoved)
      ostr << "FileRemoved: " ;
   else if (event.type() == FileChangeEvent::FileModified)
      ostr << "FileModified: ";
      
   ostr << event.fileInfo();
   
   if (event.fileInfo().isDirectory())
      ostr << " (directory)";
      
   return ostr;
}
  
} // namespace system
} // namespace core 

#endif // CORE_SYSTEM_FILE_CHANGE_EVENT_HPP


