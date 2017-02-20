/*
 * FilePath.hpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#ifndef CORE_FILE_PATH_HPP
#define CORE_FILE_PATH_HPP

#include <stdint.h>
#include <ctime>

#include <string>
#include <vector>
#include <iosfwd>

#include <boost/shared_ptr.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/function.hpp>

#include <boost/utility.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>

namespace rstudio {
namespace core {

class Error ;

class FilePath
{
public:
   
   // NOTE: function returns 'true' if computation can continue;
   // false if computation should stop
   typedef boost::function<bool(int, const FilePath&)>  
                                                RecursiveIterationFunction;

public:
   // special accessor which detects when the current path no longer exists
   // and switches to the specified alternate path if it doesn't
   static FilePath safeCurrentPath(const FilePath& revertToPath) ;
   
   static Error makeCurrent(const std::string& path);

   static std::string createAliasedPath(const core::FilePath& path,
                                        const FilePath& userHomePath);
   static FilePath resolveAliasedPath(const std::string& aliasedPath,
                                      const FilePath& userHomePath) ;

   static bool exists(const std::string& path);

   static bool isRootPath(const std::string& path);

   static Error tempFilePath(FilePath* pFilePath);
   
public:
   FilePath() ;
   explicit FilePath(const std::string& absolutePath) ;
#if _WIN32
   explicit FilePath(const std::wstring& absolutePath) ;
#endif
   virtual ~FilePath() ;
   // COPYING: via shared_ptr (immutable)
   
public:
   // does this instance contain a path?
   bool empty() const;

   // does this file exist?
   bool exists() const;

   // is the file a symlink?
   bool isSymlink() const;

   // size of file in bytes
   uintmax_t size() const;
  
   // filename only
   std::string filename() const ;
  
   // filename without extension
   std::string stem() const ;

   // file extensions
   std::string extension() const ;
   std::string extensionLowerCase() const;
   bool hasExtension(const std::string& ext) const;
   bool hasExtensionLowerCase(const std::string& ext) const;
   
   // mime types
   std::string mimeContentType(
                     const std::string& defaultType = "text/plain") const;

   bool hasTextMimeType() const;
   
   // set last write time
   void setLastWriteTime(std::time_t time = ::time(NULL)) const;
   
   // last write time
   std::time_t lastWriteTime() const;
  
   // full filesystem absolute path
   std::string absolutePath() const ;

   // full filesystem absolute path in native format
   std::string absolutePathNative() const ;

#if _WIN32
   std::wstring absolutePathW() const;
#endif

   // path relative to parent directory. returns empty string if this path
   // is not a child of the passed parent path
   std::string relativePath(const FilePath& parentPath) const ;
   
   // is this path within the scope of the passed scopePath (returns true
   // if the two paths are equal)
   bool isWithin(const FilePath& scopePath) const;

   // delete file
   Error remove() const ;
   Error removeIfExists() const;
   
   // move to path; optionally with resiliency for cross-device 
   enum MoveType 
   {
      // attempt to perform an ordinary move
      MoveDirect,

      // perform an ordinary move, but fallback to copy/delete on cross-device
      // errors 
      MoveCrossDevice
   };
   Error move(const FilePath& targetPath, MoveType type = MoveCrossDevice) const;
   
   // explicitly perform a two-stage move (copy/delete)
   Error moveIndirect(const FilePath& target) const;

   // copy to path
   Error copy(const FilePath& targetPath) const;

   // is this a hidden file?
   bool isHidden() const ;

   // is this a Windows junction point?
   bool isJunction() const ;
   
   // is this a directory?
   bool isDirectory() const ;

   // create this directory if it doesn't already exist
   Error ensureDirectory() const ;

   // create directory at relative path
   Error createDirectory(const std::string& path) const ;
   
   // remove the directory (if it exists) and create a new one in its place
   Error resetDirectory() const;

   // copy this directory to another one, recursively
   Error copyDirectoryRecursive(const FilePath& targetPath) const;

   // create this file if it doesn't already exist
   Error ensureFile() const;
   
   // complete a path (if input path is relative, returns path relative
   // to this one; if input path is absolute returns that path)
   FilePath complete(const std::string& path) const;
   
   // get child path relative to this one.
   FilePath childPath(const std::string& path) const ;
   
   // get this path's parent
   FilePath parent() const;

   // list child paths
   Error children(std::vector<FilePath>* pFilePaths) const ;  
   
   // recursively iterate over child paths
   Error childrenRecursive(RecursiveIterationFunction iterationFunction) const;

   // total size of directory and all sub-directories, etc.
   uintmax_t sizeRecursive() const;

   // make this path the system current directory
   Error makeCurrentPath(bool autoCreate = false) const ;

   Error open_r(boost::shared_ptr<std::istream>* pStream) const;
   Error open_w(boost::shared_ptr<std::ostream>* pStream, bool truncate = true) const;

   // check for equivalence (point to the same file-system entity)
   bool isEquivalentTo(const FilePath& filePath) const;

   // compare two instances (equal if absolutePath == absolutePath)
   bool operator== (const FilePath& filePath) const ;
   bool operator!= (const FilePath& filePath) const ;
   
   // natural order is based on absolute path
   bool operator < (const FilePath& other) const ;

private:

   friend class RecursiveDirectoryIterator;

private:
   struct Impl ;
   boost::shared_ptr<const Impl> pImpl_ ;
};
   
std::ostream& operator << (std::ostream& stream, const FilePath& fp) ;

bool compareAbsolutePathNoCase(const FilePath& file1, const FilePath& file2);

class RestoreCurrentPathScope : boost::noncopyable
{
public:
   RestoreCurrentPathScope(const FilePath& restorePath)
      : restorePath_(restorePath) 
   {
   }
   
   virtual ~RestoreCurrentPathScope()
   {
      try
      {
         Error error = restorePath_.makeCurrentPath();
         if (error)
            LOG_ERROR(error);
      }
      catch(...)
      {
      }
   }
private:
   FilePath restorePath_ ;
};

class RecursiveDirectoryIterator : boost::noncopyable
{
public:
   explicit RecursiveDirectoryIterator(const FilePath& filePath);
   virtual ~RecursiveDirectoryIterator();

   Error next(FilePath* pFilePath);
   bool finished() const;

private:
   struct Impl;
   boost::scoped_ptr<Impl> pImpl_;
};

class RemoveOnExitScope : boost::noncopyable
{
public:
   explicit RemoveOnExitScope(const FilePath& filePath,
                              const ErrorLocation& errorLocation)
      : filePath_(filePath), errorLocation_(errorLocation)
   {
   }
   virtual ~RemoveOnExitScope()
   {
      try
      {
         Error error = filePath_.removeIfExists();
         if (error)
            core::log::logError(error, errorLocation_);
      }
      catch(...)
      {
      }
   }

private:
   FilePath filePath_;
   ErrorLocation errorLocation_;
};

}
}

#endif // CORE_FILE_PATH_HPP



