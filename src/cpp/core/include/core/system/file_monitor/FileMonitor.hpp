/*
 * FileMonitor.hpp
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

#ifndef CORE_SYSTEM_FILE_MONITOR_HPP
#define CORE_SYSTEM_FILE_MONITOR_HPP

#include <string>
#include <set>
#include <vector>

#include <boost/scoped_ptr.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/function.hpp>

#include <core/FilePath.hpp>

#include <core/system/FileChangeEvent.hpp>

namespace core {   
namespace system {
namespace file_monitor {

class FileEntry;
namespace detail {
core::Error scanChildren(FileEntry*,bool);
}

// initialize the file monitoring service (creates a background thread
// which performs the monitoring)
void initialize();

// file attributes
class FileAttribs
{
public:
   FileAttribs()
      : size_(0), modified_(-1)
   {
   }

   FileAttribs(uintmax_t size, std::time_t modified)
      : size_(size), modified_(modified)
   {
   }

   virtual ~FileAttribs()
   {
   }

   // COPYING: via compiler

   uintmax_t size() const { return size_; }
   std::time_t modified() const { return modified_; }

private:
   uintmax_t size_;
   std::time_t modified_;
};

class FileEntry
{
public:
   FileEntry()
      : isDirectory_(false)
   {
   }

   FileEntry(const std::string& path)
      : path_(path), isDirectory_(true)
   {
   }

   FileEntry(const std::string &path, const FileAttribs& attribs)
      : path_(path), isDirectory_(false), attribs_(attribs)
   {
   }

   virtual ~FileEntry()
   {
   }

   // COPYING: via compiler

public:
   const std::string& path() const { return path_; }

   bool isDirectory() const { return isDirectory_; }

   const FileAttribs& attribs() const { return attribs_; }

   const std::set<FileEntry>& children() const { return children_; }

   bool operator < (const FileEntry& other) const
   {
      return path_ < other.path_;
   }

   bool operator == (const FileEntry& other) const
   {
      return path_ == other.path_;
   }

   bool operator != (const FileEntry& other) const
   {
      return path_ != other.path_;
   }

private:
   // allow platform specific file scanner access to internals
   friend core::Error core::system::file_monitor::detail::scanChildren(
                                                               FileEntry*,bool);

   // relative path to file
   std::string path_;

   // is this a directory
   bool isDirectory_;

   // has attribs if it is not a directory
   FileAttribs attribs_;

   // has children if it is a directory
   std::set<FileEntry> children_;
};

class FileChange
{
public:
   enum Type
   {
      None = 0,
      Added = 1,
      Removed = 2,
      Modified = 3
   };

public:
   FileChange()
      : type_(None)
   {
   }

   FileChange(Type type, const FileEntry& fileEntry)
      : type_(type), fileEntry_(fileEntry)
   {
   }

   virtual ~FileChange()
   {
   }

   // COPYING: via compiler

public:
   Type type() const { return type_; }
   const FileEntry& fileEntry() const { return fileEntry_; }

private:
   Type type_;
   FileEntry fileEntry_;
};


// opaque handle to a registration (used to unregister)
typedef void* Handle;

struct Callbacks
{
   // callback which occurs after a successful registration (includes an initial
   // listing of all of the files in the directory)
   boost::function<void(Handle, const FileEntry&)> onRegistered;

   // callback which occurs if a registration error occurs
   boost::function<void(const core::Error&)> onRegistrationError;

   // callback which occurs when files change
   typedef boost::function<void(const std::vector<FileChange>&)> FilesChanged;
   FilesChanged onFilesChanged;
};

// register a new file monitor
void registerMonitor(const core::FilePath& filePath, const Callbacks& callbacks);

// unregister a file monitor
void unregisterMonitor(Handle handle);

// check for changes (will cause onRegistered and/or onFilesChanged calls on
// the same thread that called checkForChanges)
void checkForChanges();


} // namespace file_monitor
} // namespace system
} // namespace core 

#endif // CORE_SYSTEM_FILE_MONITOR_HPP


