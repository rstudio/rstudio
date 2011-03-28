/*
 * FilePath.cpp
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

#include <core/FilePath.hpp>

#include <algorithm>

#ifdef _WIN32
#include <windows.h>
#endif

#include <boost/filesystem.hpp>

// detect filesystem3 so we can conditionally compile away breaking changes
#if defined(BOOST_FILESYSTEM_VERSION) && BOOST_FILESYSTEM_VERSION != 2
#define BOOST_FS_STRING(str) toString((str).string())
#define BOOST_FS_COMPLETE(p, base) boost::filesystem::absolute(fromString(p), base)
#else
#define BOOST_FS_STRING(str) toString(str)
#define BOOST_FS_COMPLETE(p, base) boost::filesystem::complete(fromString(p), base)
#endif

#include <core/StringUtils.hpp>
#include <core/system/System.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>

namespace core {

namespace {

#ifdef _WIN32

// For Windows only, we need to use the wide character versions of the file
// APIs in order to deal properly with characters that cannot be represented
// in the default system encoding. (It would be preferable if UTF-8 were the
// system encoding, but Windows doesn't support that.) However, we can't give
// FilePath a wide character API because Mac needs to use narrow characters
// (see note below). So we use wstring internally, and translate to/from UTF-8
// narrow strings that are used in the API.

typedef boost::filesystem::wpath path_type;
typedef std::wstring internal_string;

std::string toString(const internal_string& value)
{
   if (value.size() == 0)
      return std::string();

   const wchar_t * cstr = value.c_str();
   int chars = ::WideCharToMultiByte(CP_UTF8, 0,
                                     cstr, -1,
                                     NULL, 0, NULL, NULL);
   if (chars == 0)
   {
      LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));
      return std::string();
   }

   std::vector<char> result(chars, 0);
   chars = ::WideCharToMultiByte(CP_UTF8, 0,
                                 cstr, -1,
                                 &(result[0]), result.size(),
                                 NULL, NULL);

   return std::string(&(result[0]));
}

internal_string fromString(const std::string& value)
{
   if (value.size() == 0)
      return std::wstring();

   const char * cstr = value.c_str();
   int chars = ::MultiByteToWideChar(CP_UTF8, 0,
                                     cstr, -1,
                                     NULL, 0);
   if (chars == 0)
   {
      LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));
      return std::wstring();
   }

   std::vector<wchar_t> result(chars, 0);
   chars = ::MultiByteToWideChar(CP_UTF8, 0,
                                 cstr, -1,
                                 &(result[0]), result.size());

   return std::wstring(&(result[0]));
}

#else

// Mac can't use wide strings because of Boost asserts that cause the process
// to exit unless we compile with -DBOOST_FILESYSTEM_NARROW_ONLY. That's OK
// because we only support running with UTF-8 codeset on Mac and Linux.

typedef boost::filesystem::path path_type;
typedef std::string internal_string;

std::string toString(const internal_string& value)
{
   return value;
}

internal_string fromString(const std::string& value)
{
   return value;
}

#endif

void logError(path_type path,
              const boost::filesystem::filesystem_error& e,
              const ErrorLocation& errorLocation);
void addErrorProperties(path_type path, Error* pError) ;
}

struct FilePath::Impl
{
   Impl()
   {
   }
   Impl(path_type path)
      : path(path)
   {
   }
   path_type path ;
};
  
FilePath FilePath::initialPath() 
{
   return FilePath(boost::filesystem::initial_path().string()) ;
}

FilePath FilePath::safeCurrentPath(const FilePath& revertToPath)
{
   try
   {
      return FilePath(boost::filesystem::current_path().string()) ;
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      if (e.code() == boost::system::errc::no_such_file_or_directory)
      {
         // revert to the specified path if it exists, otherwise
         // take the user home path from the system
         FilePath safePath = revertToPath;
         if (!safePath.exists())
            safePath = core::system::userHomePath();

         Error error = safePath.makeCurrentPath();
         if (error)
            LOG_ERROR(error);

         return safePath;
      }
      else
      {
         throw;
      }
   }
}
   
Error FilePath::makeCurrent(const std::string& path)
{
   return FilePath(path).makeCurrentPath();
}

#define kHomePathAlias "~/"
#define kHomePathLeafAlias "~"

std::string FilePath::createAliasedPath(const FilePath& path,
                              const FilePath& userHomePath)
{
   // Special case for "~"
   if (path == userHomePath)
      return kHomePathLeafAlias;

   // if the path is contained within the home path then alias it
   std::string homeRelativePath = path.relativePath(userHomePath);
   if (!homeRelativePath.empty())
   {
      std::string aliasedPath = kHomePathAlias + homeRelativePath;
      return aliasedPath;
   }
   else  // no aliasing
   {
      return path.absolutePath();
   }
}

FilePath FilePath::resolveAliasedPath(const std::string& aliasedPath,
                            const FilePath& userHomePath)
{
   // Special case for  empty string or "~"
   if (aliasedPath.empty() || (aliasedPath.compare(kHomePathLeafAlias) == 0))
      return userHomePath;

   // if the path starts with the home alias then substitute the home path
   if (aliasedPath.find(kHomePathAlias) == 0)
   {
      std::string resolvedPath = userHomePath.absolutePath() +
      aliasedPath.substr(1);
      return FilePath(resolvedPath);
   }
   else  // no aliasing, this is either an absolute path or path
         // relative to the current directory
   {
      return FilePath::safeCurrentPath(userHomePath).complete(aliasedPath);
   }
}
 
FilePath::FilePath()
   : pImpl_(new Impl())
{
}
   
FilePath::FilePath(const std::string& absolutePath)
   : pImpl_(new Impl(fromString(std::string(absolutePath.c_str())))) // thwart ref-count
{
}

FilePath::~FilePath()
{
}


bool FilePath::empty() const
{
   return pImpl_->path.empty() ;
}

bool FilePath::exists() const
{
    try 
    {
       return !empty() && boost::filesystem::exists(pImpl_->path) ;
    }
    catch(const boost::filesystem::filesystem_error& e) 
    {
       logError(pImpl_->path, e, ERROR_LOCATION) ;
       return false ;
    }
}

uintmax_t FilePath::size() const
{
   try 
   {
      return boost::filesystem::file_size(pImpl_->path) ;
   }
   catch(const boost::filesystem::filesystem_error& e) 
   {
      logError(pImpl_->path, e, ERROR_LOCATION) ;
      return 0;
   }
}

std::string FilePath::filename() const
{
   return BOOST_FS_STRING(pImpl_->path.filename()) ;
}

std::string FilePath::stem() const 
{
   return BOOST_FS_STRING(pImpl_->path.stem());
}

std::string FilePath::extension() const
{
   return BOOST_FS_STRING(pImpl_->path.extension()) ;
}
   
std::string FilePath::extensionLowerCase() const
{
   return string_utils::toLower(extension());
}
   
namespace {

struct MimeType
{
   const char* extension;
   const char* contentType;
} ;

MimeType s_mimeTypes[] =
{
   // most common web types
   { "htm",   "text/html" },
   { "html",  "text/html" },
   { "css",   "text/css" },
   { "gif",   "image/gif" },
   { "jpg",   "image/jpeg" },
   { "png",   "image/png" },
   { "js",    "application/x-javascript" },
   { "pdf",   "application/pdf" },
   { "svg",   "image/svg+xml" },
   { "swf",   "application/x-shockwave-flash" },
   
   // other types we are likely to serve
   { "xml",   "text/xml" },
   { "csv",   "text/csv" },
   { "ico",   "image/x-icon" },
   { "zip",   "application/zip" },
    
   // yet more types...
   
   { "shtml", "text/html" },
   { "tsv",   "text/tab-separated-values" },
   { "tab",   "text/tab-separated-values" },
   { "txt",   "text/plain" },
   { "mml",   "text/mathml" },
  
   { "tif",   "image/tiff" },
   { "tiff",  "image/tiff" },
      
   { "atom",  "application/atom+xml" },
   { "rss",   "application/rss+xml" },
   
   { "doc",   "application/msword" },
   { "docx",  "application/vnd.openxmlformats-officedocument.wordprocessingml.document" },
   { "odt",   "application/vnd.oasis.opendocument.text" },
   { "rtf",   "application/rtf" },
   { "xls",   "application/vnd.ms-excel" },
   { "xlsx",  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" },
   { "ods",   "application/x-vnd.oasis.opendocument.spreadsheet" },
   { "ppt",   "application/vnd.ms-powerpoint" },
   { "pps",   "application/vnd.ms-powerpoint" },
   { "pptx",  "application/vnd.openxmlformats-officedocument.presentationml.presentation" },
 
   { "sit",   "application/x-stuffit" },
   { "sxw",   "application/vnd.sun.xml.writer" },
   
   { "iso",   "application/octet-stream" },
   { "dmg",   "application/octet-stream" },
   { "exe",   "application/octet-stream" },
   { "dll",   "application/octet-stream" },
   { "deb",   "application/octet-stream" },
   { "xpi",   "application/x-xpinstall" },
   
   { "mp3",   "audio/mpeg" },
   
   { "mpg",   "video/mpeg" },
   { "mpeg",  "video/mpeg" },
   { "flv",   "video/x-flv" },
  
   { NULL, NULL } 
};
   
}
   
std::string FilePath::mimeContentType(const std::string& defaultType) const
{
   std::string ext = extensionLowerCase();
   if (!ext.empty())
   {
      ext = ext.substr(1); // remove leading .
      for (MimeType* mimeType = s_mimeTypes; mimeType->extension; ++mimeType)
      {
         if (mimeType->extension == ext)
            return mimeType->contentType;
      }
      
      // none found
      return defaultType;
   }
   else
   {
      // no extension
      return defaultType;
   }
}
   
std::time_t FilePath::lastWriteTime() const
{
   try 
   {
     return boost::filesystem::last_write_time(pImpl_->path) ;
   }
   catch(const boost::filesystem::filesystem_error& e) 
   {
      logError(pImpl_->path, e, ERROR_LOCATION) ;
      return 0;
   }
}

// NOTE: this does not properly handle .. and . path elements
std::string FilePath::relativePath(const FilePath& parentPath) const
{
   // get iterators to this path and parent path
   path_type::iterator thisBegin = pImpl_->path.begin() ;
   path_type::iterator thisEnd = pImpl_->path.end() ;
   path_type::iterator parentBegin = parentPath.pImpl_->path.begin();
   path_type::iterator parentEnd = parentPath.pImpl_->path.end() ;
   
   // if the child is fully prefixed by the parent
   path_type::iterator it = std::search(thisBegin, thisEnd, parentBegin, parentEnd);
   if ( it == thisBegin )
   {
      // search for mismatch location
      std::pair<path_type::iterator,path_type::iterator> mmPair =
                              std::mismatch(thisBegin, thisEnd, parentBegin);
      
      // build relative path from mismatch on
      path_type relativePath ;
      path_type::iterator mmit = mmPair.first ;
      while (mmit != thisEnd)
      {
         relativePath /= *mmit ;
         mmit++ ;
      }
      return toString(relativePath.string()) ;
   }
   else
   {
      return std::string() ;
   }
}
   
bool FilePath::isWithin(const FilePath& scopePath) const
{
   if (*this == scopePath)
      return true ;
   
   std::string relativePath = this->relativePath(scopePath);
   return !relativePath.empty();
}


std::string FilePath::absolutePath() const
{
   if (empty())
      return std::string();
   else
      return toString(pImpl_->path.string()) ;
}

Error FilePath::remove() const
{
   try
   {
      if (isDirectory())
         boost::filesystem::remove_all(pImpl_->path);
      else
         boost::filesystem::remove(pImpl_->path) ;
      return Success() ;
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      Error error(e.code(), ERROR_LOCATION) ;
      addErrorProperties(pImpl_->path, &error) ;   
      return error ;
   }
}
   
Error FilePath::removeIfExists() const
{
   if (exists())
      return remove();
   else
      return Success();
}
 
Error FilePath::move(const FilePath& targetPath) const
{
   try
   {
      boost::filesystem::rename(pImpl_->path, targetPath.pImpl_->path) ;
      return Success() ;
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      Error error(e.code(), ERROR_LOCATION) ;
      addErrorProperties(pImpl_->path, &error) ;   
      error.addProperty("target-path", targetPath.absolutePath()) ;
      return error ;
   }
}
   

Error FilePath::copy(const FilePath& targetPath) const
{
   try
   {
      boost::filesystem::copy_file(pImpl_->path, targetPath.pImpl_->path) ;
      return Success() ;
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      Error error(e.code(), ERROR_LOCATION) ;
      addErrorProperties(pImpl_->path, &error) ;   
      error.addProperty("target-path", targetPath.absolutePath()) ;
      return error ;
   }   
}


   
bool FilePath::isHidden() const 
{
   return system::isHiddenFile(*this) ;
}
   
   
bool FilePath::isDirectory() const 
{
   try 
   {
      return boost::filesystem::is_directory(pImpl_->path) ;
   }
   catch(const boost::filesystem::filesystem_error& e) 
   {
      logError(pImpl_->path, e, ERROR_LOCATION) ;
      return false;
   }
}

   
Error FilePath::ensureDirectory() const
{
   if ( !exists() )
      return createDirectory(std::string()) ;
   else
      return Success() ;
}


Error FilePath::createDirectory(const std::string& name) const
{
   try
   {
      path_type targetDirectory ;
      if (name.empty())
         targetDirectory = pImpl_->path ;
      else
         targetDirectory = BOOST_FS_COMPLETE(name, pImpl_->path) ;
      boost::filesystem::create_directories(targetDirectory) ;
      return Success() ;
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      Error error(e.code(), ERROR_LOCATION) ;
      addErrorProperties(pImpl_->path, &error) ;   
      error.addProperty("target-dir", name) ;
      return error ;
   }
}
   
Error FilePath::resetDirectory() const
{
   Error error = removeIfExists();
   if (error)
      return error;
   
   return ensureDirectory();
}


FilePath FilePath::complete(const std::string& path) const
{
   // in-theory boost::filesystem::complete can throw but the conditions
   // are very obscure and are in any case a programming error. therefore,
   // we log silently if there is an error so that clients don't have to
   // deal with any error states (if there an error then a copy of 
   // this path is returned)
   try
   {
      // NOTE: The path gets round-tripped through toString/fromString, would
      //   be nice to have a direct constructor
      return FilePath(toString(BOOST_FS_COMPLETE(path, pImpl_->path).string()));
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      Error error(e.code(), ERROR_LOCATION) ;
      addErrorProperties(pImpl_->path, &error) ;   
      error.addProperty("path", path) ;
      LOG_ERROR(error) ;
      return *this ; 
   }
}
   
FilePath FilePath::parent() const
{
   try
   {
      // NOTE: The path gets round-tripped through toString/fromString, would
      //   be nice to have a direct constructor
      return FilePath(toString(pImpl_->path.parent_path().string()));
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      Error error(e.code(), ERROR_LOCATION) ;
      addErrorProperties(pImpl_->path, &error) ;   
      LOG_ERROR(error);
      return *this ; 
   }
}
   
// note: this differs from complete in the following ways:
//    - the passed path can be an empty string (returns self)
//    - the passed path must be relative
FilePath FilePath::childPath(const std::string& path) const 
{
   try
   {
      if (path.empty())
      {
         return *this ;
      }
      else
      {
         // confirm this is a relative path
         path_type relativePath(fromString(path));
         if (relativePath.has_root_path())
         {
            throw boost::filesystem::filesystem_error(
                           "absolute path not permitted",
                           boost::system::error_code(
                              boost::system::errc::no_such_file_or_directory,
                              boost::system::get_system_category()));
         }
         
         return complete(path);
      }
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      Error error(e.code(), ERROR_LOCATION) ;
      addErrorProperties(pImpl_->path, &error) ;   
      error.addProperty("path", path) ;
      LOG_ERROR(error);
      return *this ; 
   }
   
}

namespace {

Error notFoundError(const FilePath& filePath,
                        const ErrorLocation& location)
{
   Error error = pathNotFoundError(location);
   if (!filePath.empty())
      error.addProperty("path", filePath.absolutePath());
   return error;
}

}

Error FilePath::children(std::vector<FilePath>* pFilePaths) const
{
   if (!exists())
      return notFoundError(*this, ERROR_LOCATION);

   using boost::filesystem::basic_directory_iterator ;

   try 
   {
      basic_directory_iterator<path_type> end ;
      for (basic_directory_iterator<path_type> itr(pImpl_->path); itr != end; ++itr)
      {
         // NOTE: The path gets round-tripped through toString/fromString, would
         //   be nice to have a direct constructor
         std::string itemPath = toString(itr->path().string());
         pFilePaths->push_back(FilePath(itemPath)) ;
      }
      return Success() ;
   }
   catch(const boost::filesystem::filesystem_error& e) 
   {
      Error error(e.code(), ERROR_LOCATION) ;
      addErrorProperties(pImpl_->path, &error) ;   
      return error ;
   }
}
   
 
Error FilePath::childrenRecursive(
                        RecursiveIterationFunction iterationFunction) const
{
   if (!exists())
      return notFoundError(*this, ERROR_LOCATION);

   using boost::filesystem::basic_recursive_directory_iterator ;
   
   try 
   {
      basic_recursive_directory_iterator<path_type> end ;
      
      for (basic_recursive_directory_iterator<path_type> itr(pImpl_->path); itr != end; ++itr)
      {
         // NOTE: The path gets round-tripped through toString/fromString, would
         //   be nice to have a direct constructor
         iterationFunction(itr.level(),FilePath(toString(itr->path().string())));
      }
      
      return Success() ;
   }
   catch(const boost::filesystem::filesystem_error& e) 
   {
      Error error(e.code(), ERROR_LOCATION) ;
      addErrorProperties(pImpl_->path, &error) ;   
      return error ;
   }
}


Error FilePath::makeCurrentPath(bool autoCreate) const 
{
   if (autoCreate)
   {
      Error autoCreateError = ensureDirectory();
      if (autoCreateError)
         return autoCreateError ;
   }
   
   try 
   {
      boost::filesystem::current_path(pImpl_->path) ;
      return Success() ;
   }
   catch(const boost::filesystem::filesystem_error& e) 
   {
      Error error(e.code(), ERROR_LOCATION) ;
      addErrorProperties(pImpl_->path, &error) ;
      return error ;
   }
}

bool FilePath::operator== (const FilePath& filePath) const 
{
   return pImpl_->path == filePath.pImpl_->path ;
}

bool FilePath::operator!= (const FilePath& filePath) const
{
   return pImpl_->path != filePath.pImpl_->path ;
}
   
bool FilePath::operator < (const FilePath& other) const 
{
   return pImpl_->path < other.pImpl_->path ;
}

std::ostream& operator << (std::ostream& stream, const FilePath& fp)
{
   stream << fp.absolutePath();
   return stream ;
}

bool compareAbsolutePathNoCase(const FilePath& file1, const FilePath& file2)
{
   std::string file1Lower = string_utils::toLower(file1.absolutePath());
   std::string file2Lower = string_utils::toLower(file2.absolutePath());
   return file1Lower < file2Lower;
}

namespace { 
void logError(path_type path,
              const boost::filesystem::filesystem_error& e,
              const core::ErrorLocation& errorLocation)
{
   Error error(e.code(), errorLocation) ;
   addErrorProperties(path, &error) ;   
   core::log::logError(error, errorLocation) ;
}

void addErrorProperties(path_type path, Error* pError)
{
   pError->addProperty("path", toString(path.string())) ;
}
}

} // namespace core

