/*
 * FilePath.cpp
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

#include <core/FilePath.hpp>

#include <algorithm>
#include <fstream>

#ifdef _WIN32
#include <windows.h>
#endif

#define BOOST_FILESYSTEM_NO_DEPRECATED

#define BOOST_NO_CXX11_SCOPED_ENUMS
#include <boost/filesystem.hpp>
#undef BOOST_NO_CXX11_SCOPED_ENUMS

#include <boost/algorithm/string/predicate.hpp>

#include <boost/bind.hpp>
#include <boost/make_shared.hpp>
#include <boost/iostreams/stream.hpp>
#include <boost/iostreams/device/file_descriptor.hpp>

#include <core/StringUtils.hpp>
#include <core/system/System.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>


typedef boost::filesystem::path path_t;

namespace rstudio {
namespace core {

namespace {

// We use boost::filesystem in one of three different ways:
// - On Windows, we use Filesystem v3 with wide character paths. This is
//   because narrow character paths on Windows can't cover the entire
//   Unicode space since there is no ANSI code page for UTF-8.
// - On non-Windows, if Filesystem v3 is available, we use it.
// - Otherwise, we use Filesystem v2. This is necessary for older versions
//   of Boost that come preinstalled on some long-term-stable Linux distro
//   versions that we still want to support.
#ifdef _WIN32

#define BOOST_FS_STRING(path) toString((path).generic_wstring())
#define BOOST_FS_PATH2STR(path) toString((path).generic_wstring())
#define BOOST_FS_PATH2STRNATIVE(path) toString((path).wstring())
#define BOOST_FS_COMPLETE(p, base) boost::filesystem::absolute(fromString(p), base)
typedef boost::filesystem::directory_iterator dir_iterator;
typedef boost::filesystem::recursive_directory_iterator recursive_dir_iterator;

#elif defined(BOOST_FILESYSTEM_VERSION) && BOOST_FILESYSTEM_VERSION != 2

#define BOOST_FS_STRING(path) ((path).generic_string())
#define BOOST_FS_PATH2STR(path) ((path).generic_string())
#define BOOST_FS_PATH2STRNATIVE(path) ((path).generic_string())
#define BOOST_FS_COMPLETE(p, base) boost::filesystem::absolute(p, base)
typedef boost::filesystem::directory_iterator dir_iterator;
typedef boost::filesystem::recursive_directory_iterator recursive_dir_iterator;

#else

#define BOOST_FS_STRING(str) (str)
#define BOOST_FS_PATH2STR(str) ((str).string())
#define BOOST_FS_PATH2STRNATIVE(str) ((str).string())
#define BOOST_FS_COMPLETE(p, base) boost::filesystem::complete(p, base)
typedef boost::filesystem::basic_directory_iterator<path_t> dir_iterator;
typedef boost::filesystem::basic_recursive_directory_iterator<path_t> recursive_dir_iterator;

#endif



#ifdef _WIN32

// For Windows only, we need to use the wide character versions of the file
// APIs in order to deal properly with characters that cannot be represented
// in the default system encoding. (It would be preferable if UTF-8 were the
// system encoding, but Windows doesn't support that.) However, we can't give
// FilePath a wide character API because Mac needs to use narrow characters
// (see note below). So we use wstring internally, and translate to/from UTF-8
// narrow strings that are used in the API.

typedef std::wstring internal_string;

std::string toString(const internal_string& value)
{
   return string_utils::wideToUtf8(value);
}

internal_string fromString(const std::string& value)
{
   return string_utils::utf8ToWide(value);
}

#else

// We only support running with UTF-8 codeset on Mac and Linux, so
// strings are a passthrough.

typedef std::string internal_string;

internal_string fromString(const std::string& value)
{
   return value;
}

#endif

void logError(path_t path,
              const boost::filesystem::filesystem_error& e,
              const ErrorLocation& errorLocation);
void addErrorProperties(path_t path, Error* pError) ;

bool copySingleItem(const FilePath& from, const FilePath& to, 
                    const FilePath& path)
{
   std::string relativePath = path.relativePath(from);
   FilePath target = to.complete(relativePath);

   Error error = path.isDirectory() ?
                     target.ensureDirectory() :
                     path.copy(target);
   if (error)
      LOG_ERROR(error);

   return true;
}

bool addItemSize(const FilePath& item, boost::shared_ptr<int> pTotal)
{
   if (!item.isDirectory())
      *pTotal = *pTotal + item.size();
   return true;
}

}

struct FilePath::Impl
{
   Impl()
   {
   }
   Impl(path_t path)
      : path(path)
   {
   }
   path_t path ;
};

FilePath FilePath::safeCurrentPath(const FilePath& revertToPath)
{
   try
   {
#ifdef _WIN32
      return FilePath(boost::filesystem::current_path().wstring()) ;
#elif defined(BOOST_FILESYSTEM_VERSION) && BOOST_FILESYSTEM_VERSION != 2
      return FilePath(boost::filesystem::current_path().string()) ;
#else
      return FilePath(boost::filesystem::current_path<path_t>().string()) ;
#endif
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      if (e.code() != boost::system::errc::no_such_file_or_directory)
         LOG_ERROR(Error(e.code(), ERROR_LOCATION));
   }
   CATCH_UNEXPECTED_EXCEPTION

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

bool FilePath::exists(const std::string& path)
{
   if (path.empty())
      return false;

   path_t p(fromString(path));
   try
   {
      return boost::filesystem::exists(p);
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      logError(p, e, ERROR_LOCATION) ;
      return false ;
   }
}

bool FilePath::isRootPath(const std::string& path)
{
   if (path.empty())
      return false;

   path_t p(fromString(path));
   try
   {
      return p.has_root_path();
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      logError(p, e, ERROR_LOCATION) ;
      return false ;
   }
}

Error FilePath::tempFilePath(FilePath* pFilePath)
{
   using namespace boost::filesystem;
   try
   {
      path_t path = absolute(unique_path(), temp_directory_path());
      *pFilePath = FilePath(BOOST_FS_PATH2STR(path));
      return Success();
   }
   catch(const filesystem_error& e)
   {
      return Error(e.code(), ERROR_LOCATION) ;
   }

   // keep compiler happy
   return pathNotFoundError(ERROR_LOCATION);
}

FilePath::FilePath()
   : pImpl_(new Impl())
{
}

FilePath::FilePath(const std::string& absolutePath)
   : pImpl_(new Impl(fromString(std::string(absolutePath.c_str())))) // thwart ref-count
{
}

#if _WIN32
FilePath::FilePath(const std::wstring& absolutePath)
   : pImpl_(new Impl(absolutePath)) // thwart ref-count
{
}
#endif

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
       if (e.code() != boost::system::errc::permission_denied)
         logError(pImpl_->path, e, ERROR_LOCATION) ;
       return false ;
    }
}

bool FilePath::isSymlink() const
{
   try
   {
      return exists() && boost::filesystem::is_symlink(pImpl_->path);
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      logError(pImpl_->path, e, ERROR_LOCATION);
      return false;
   }
}

uintmax_t FilePath::size() const
{
   try
   {
      if (!exists() || !boost::filesystem::is_regular_file(pImpl_->path))
         return 0;
      else
         return boost::filesystem::file_size(pImpl_->path) ;
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
#ifdef _WIN32
      if (e.code().value() == ERROR_NOT_SUPPORTED)
         return 0;
#endif
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

bool FilePath::hasExtension(const std::string& ext) const
{
   return extension() == ext;
}

bool FilePath::hasExtensionLowerCase(const std::string& ext) const
{
   return extensionLowerCase() == ext;
}

namespace {

struct MimeType
{
   const char* extension;
   const char* contentType;
} ;

// NOTE: should be synced with mime type database in FileSystemItem.java
MimeType s_mimeTypes[] =
{
   // most common web types
   { "htm",   "text/html" },
   { "html",  "text/html" },
   { "css",   "text/css" },
   { "gif",   "image/gif" },
   { "jpg",   "image/jpeg" },
   { "jpeg",  "image/jpeg" },
   { "jpe",   "image/jpeg" },
   { "png",   "image/png" },
   { "js",    "text/javascript" },
   { "pdf",   "application/pdf" },
   { "svg",   "image/svg+xml" },
   { "swf",   "application/x-shockwave-flash" },
   { "ttf",   "application/x-font-ttf" },
   { "woff",  "application/font-woff" },

   // markdown types
   { "md",       "text/x-markdown" },
   { "mdtxt",    "text/x-markdown" },
   { "markdown", "text/x-markdown" },
   { "yaml",     "text/x-yaml" },
   { "yml",      "text/x-yaml" },

   // programming language types
   { "f",        "text/x-fortran" },
   { "py",       "text/x-python" },
   { "sh",       "text/x-shell" },
   { "sql",      "text/x-sql" },
   { "stan",     "text/x-stan" },
   { "clj",      "text/x-clojure" },

   // other types we are likely to serve
   { "xml",   "text/xml" },
   { "csv",   "text/csv" },
   { "ico",   "image/x-icon" },
   { "zip",   "application/zip" },
   { "bz",    "application/x-bzip" },
   { "bz2",   "application/x-bzip2" },
   { "gz",    "application/x-gzip" },
   { "tar",   "application/x-tar" },
   { "json",  "application/json" },

   // yet more types...

   { "shtml", "text/html" },
   { "tsv",   "text/tab-separated-values" },
   { "tab",   "text/tab-separated-values" },
   { "dcf",   "text/debian-control-file" },
   { "txt",   "text/plain" },
   { "mml",   "text/mathml" },
   { "log",   "text/plain" },
   { "out",   "text/plain" },
   { "csl",   "text/x-csl" },
   { "R",     "text/x-r-source"},
   { "S",     "text/x-r-source"},
   { "q",     "text/x-r-source"},
   { "Rd",    "text/x-r-doc"},
   { "Rnw",   "text/x-r-sweave"},
   { "Rmd",   "text/x-r-markdown"},
   { "Rhtml", "text/x-r-html"},
   { "Rpres", "text/x-r-presentation"},
   { "Rout",  "text/plain" },
   { "po",    "text/plain" },
   { "pot",   "text/plain"},
   { "gitignore", "text/plain"},
   { "Rbuildignore", "text/plain"},
   { "Rprofile", "text/x-r-source"},
   { "Renviron", "text/x-shell" },
   { "rprofvis",   "text/x-r-profile" },

   { "tif",   "image/tiff" },
   { "tiff",  "image/tiff" },
   { "bmp",   "image/bmp"  },
   { "ps",    "application/postscript" },
   { "eps",   "application/postscript" },
   { "dvi"    "application/x-dvi" },

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
   { "otf",   "application/octet-stream" },
   { "xpi",   "application/x-xpinstall" },

   { "mp2",   "audio/mpeg" },

   { "mpg",   "video/mpeg" },
   { "mpeg",  "video/mpeg" },
   { "flv",   "video/x-flv" },

   { "mp4",   "video/mp4" },
   { "webm",  "video/webm" },
   { "ogv",   "video/ogg" },

   { "mp3",   "audio/mp3" },
   { "wav",   "audio/wav" },
   { "oga",   "audio/ogg" },
   { "ogg",   "audio/ogg" },

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
         if (boost::algorithm::iequals(mimeType->extension,ext))
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

bool FilePath::hasTextMimeType() const
{
   std::string mimeType = mimeContentType("application/octet-stream");
   return boost::algorithm::starts_with(mimeType, "text/") ||
          boost::algorithm::ends_with(mimeType, "+xml");
}

void FilePath::setLastWriteTime(std::time_t time) const
{
   try
   {
      if (!exists())
         return;
      else
         boost::filesystem::last_write_time(pImpl_->path, time);
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      logError(pImpl_->path, e, ERROR_LOCATION) ;
      return;
   }
}

std::time_t FilePath::lastWriteTime() const
{
   try
   {
      if (!exists())
         return 0;
      else
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
   path_t::iterator thisBegin = pImpl_->path.begin() ;
   path_t::iterator thisEnd = pImpl_->path.end() ;
   path_t::iterator parentBegin = parentPath.pImpl_->path.begin();
   path_t::iterator parentEnd = parentPath.pImpl_->path.end() ;

   // if the child is fully prefixed by the parent
   path_t::iterator it = std::search(thisBegin, thisEnd, parentBegin, parentEnd);
   if ( it == thisBegin )
   {
      // search for mismatch location
      std::pair<path_t::iterator,path_t::iterator> mmPair =
                              std::mismatch(thisBegin, thisEnd, parentBegin);

      // build relative path from mismatch on
      path_t relativePath ;
      path_t::iterator mmit = mmPair.first ;
      while (mmit != thisEnd)
      {
         relativePath /= *mmit ;
         mmit++ ;
      }
      return BOOST_FS_PATH2STR(relativePath) ;
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
      return BOOST_FS_PATH2STR(pImpl_->path) ;
}

std::string FilePath::absolutePathNative() const
{
   if (empty())
      return std::string();
   else
      return BOOST_FS_PATH2STRNATIVE(pImpl_->path) ;
}

#if _WIN32
std::wstring FilePath::absolutePathW() const
{
   if (empty())
      return std::wstring();
   else
      return pImpl_->path.wstring();
}
#endif

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

Error FilePath::move(const FilePath& targetPath, MoveType type) const
{
   try
   {
      boost::filesystem::rename(pImpl_->path, targetPath.pImpl_->path) ;
      return Success() ;
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      if (type == MoveCrossDevice &&
          e.code() == boost::system::errc::cross_device_link)
      {
         // this error implies that we're trying to move a file from one 
         // device to another; in this case, fall back to copy/delete
         return moveIndirect(targetPath);
      }
      Error error(e.code(), ERROR_LOCATION) ;
      addErrorProperties(pImpl_->path, &error) ;
      error.addProperty("target-path", targetPath.absolutePath()) ;
      return error ;
   }
}

Error FilePath::moveIndirect(const FilePath& targetPath) const 
{
   // when target is a directory, moving has the effect of moving *into* the
   // directory (rather than *replacing* it); simulate that behavior here
   FilePath target = targetPath.isDirectory() ?
      targetPath.complete(filename()) : targetPath;

   // copy the file or directory to the new location
   Error error = isDirectory() ? 
      copyDirectoryRecursive(target) : copy(target);
   if (error)
      return error;

   // delete the original copy of the file or directory (not considered a fatal
   // error)
   error = remove();
   if (error)
      LOG_ERROR(error);

   return Success();
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

bool FilePath::isJunction() const
{
#ifndef _WIN32
   return false;
#else
   if (!exists())
      return false;

   const wchar_t* path = pImpl_->path.c_str();
   DWORD fa = GetFileAttributesW(path);
   if (fa == INVALID_FILE_ATTRIBUTES)
   {
      return false;
   }
   if (fa & FILE_ATTRIBUTE_REPARSE_POINT &&
       fa & FILE_ATTRIBUTE_DIRECTORY)
   {
      return true;
   }
   else
   {
      return false;
   }
#endif
}

bool FilePath::isDirectory() const
{
   try
   {
      if (!exists())
         return false;
      else
      {
         return boost::filesystem::is_directory(pImpl_->path)
      #ifdef _WIN32
               || isJunction()
      #endif
               ;
      }
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
      path_t targetDirectory ;
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

Error FilePath::copyDirectoryRecursive(const FilePath& target) const
{
   Error error = target.ensureDirectory();
   if (error)
      return error;

   return childrenRecursive(boost::bind(copySingleItem, *this, target, _2));
}

uintmax_t FilePath::sizeRecursive() const
{
   // no work to do if we're not a directory
   if (!isDirectory())
      return size();

   boost::shared_ptr<int> pTotal = boost::make_shared<int>(0);
   Error error = childrenRecursive(boost::bind(addItemSize, _2, pTotal)); 
   if (error)
      LOG_ERROR(error);
   return *pTotal;
}

Error FilePath::ensureFile() const
{
   // nothing to do if the file already exists
   if (exists())
      return Success();

   // create output stream to ensure file creation
   boost::shared_ptr<std::ostream> pStream;
   Error error = open_w(&pStream);
   if (error)
      return error;

   // release file handle
   pStream->flush();
   pStream.reset(); 

   return Success();
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
      return FilePath(BOOST_FS_PATH2STR(BOOST_FS_COMPLETE(path, pImpl_->path)));
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
      return FilePath(BOOST_FS_PATH2STR(pImpl_->path.parent_path()));
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
         path_t relativePath(fromString(path));
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

   try
   {
      dir_iterator end ;
      for (dir_iterator itr(pImpl_->path); itr != end; ++itr)
      {
         // NOTE: The path gets round-tripped through toString/fromString, would
         //   be nice to have a direct constructor
         std::string itemPath = BOOST_FS_PATH2STR(itr->path());
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

   try
   {
      recursive_dir_iterator end ;

      for (recursive_dir_iterator itr(pImpl_->path); itr != end; ++itr)
      {
         // NOTE: The path gets round-tripped through toString/fromString, would
         //   be nice to have a direct constructor
         if (!iterationFunction(itr.level(),
                                FilePath(BOOST_FS_PATH2STR(itr->path()))))
         {
            // end the iteration if requested
            break;
         }
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

Error FilePath::open_r(boost::shared_ptr<std::istream>* pStream) const
{
   try
   {
      std::istream* pResult = NULL;
   #ifdef _WIN32
      using namespace boost::iostreams;
      HANDLE hFile = ::CreateFileW(pImpl_->path.wstring().c_str(),
                                   GENERIC_READ,
                                   FILE_SHARE_READ,
                                   NULL,
                                   OPEN_EXISTING,
                                   0,
                                   NULL);
      if (hFile == INVALID_HANDLE_VALUE)
      {
         Error error = systemError(::GetLastError(), ERROR_LOCATION);
         error.addProperty("path", absolutePath());
         return error;
      }
      boost::iostreams::file_descriptor_source fd;
      fd.open(hFile, boost::iostreams::close_handle);
      pResult = new boost::iostreams::stream<file_descriptor_source>(fd);
   #else
      pResult = new std::ifstream(absolutePath().c_str(),
                                  std::ios_base::in | std::ios_base::binary);
   #endif

      // In case we were able to make the stream but it failed to open
      if (!(*pResult))
      {
         delete pResult;

         Error error = systemError(boost::system::errc::no_such_file_or_directory, ERROR_LOCATION);
         error.addProperty("path", absolutePath());
         return error;
      }
      pStream->reset(pResult);
   }
   catch(const std::exception& e)
   {
      Error error = systemError(boost::system::errc::io_error,
                                ERROR_LOCATION);
      error.addProperty("what", e.what());
      error.addProperty("path", absolutePath());
      return error;
   }


   return Success();
}

Error FilePath::open_w(boost::shared_ptr<std::ostream>* pStream, bool truncate) const
{
   try
   {
      std::ostream* pResult = NULL;
   #ifdef _WIN32
      using namespace boost::iostreams;
      HANDLE hFile = ::CreateFileW(pImpl_->path.wstring().c_str(),
                                   truncate ? GENERIC_WRITE : FILE_APPEND_DATA,
                                   0, // exclusive access
                                   NULL,
                                   truncate ? CREATE_ALWAYS : OPEN_ALWAYS,
                                   0,
                                   NULL);
      if (hFile == INVALID_HANDLE_VALUE)
      {
         Error error = systemError(::GetLastError(), ERROR_LOCATION);
         error.addProperty("path", absolutePath());
         return error;
      }
      file_descriptor_sink fd;
      fd.open(hFile, close_handle);
      pResult = new boost::iostreams::stream<file_descriptor_sink>(fd);
   #else
      using std::ios_base;
      ios_base::openmode flags = ios_base::out | ios_base::binary;
      if (truncate)
         flags |= ios_base::trunc;
      else
         flags |= ios_base::app;
      pResult = new std::ofstream(absolutePath().c_str(), flags);
   #endif

      if (!(*pResult))
      {
         delete pResult;

         Error error = systemError(boost::system::errc::no_such_file_or_directory, ERROR_LOCATION);
         error.addProperty("path", absolutePath());
         return error;
      }

      pStream->reset(pResult);
   }
   catch(const std::exception& e)
   {
      Error error = systemError(boost::system::errc::io_error,
                                ERROR_LOCATION);
      error.addProperty("what", e.what());
      error.addProperty("path", absolutePath());
      return error;
   }

   return Success();
}

// check for equivalence (point to the same file-system entity)
bool FilePath::isEquivalentTo(const FilePath& filePath) const
{
   if (!exists() || !filePath.exists())
      return false;

   try
   {
      return boost::filesystem::equivalent(pImpl_->path, filePath.pImpl_->path);
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      Error error(e.code(), ERROR_LOCATION) ;
      addErrorProperties(pImpl_->path, &error) ;
      error.addProperty("equivilant-to", filePath);
      return error ;
   }

   // keep compiler happy
   return false;
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

struct RecursiveDirectoryIterator::Impl
{
   explicit Impl(path_t path)
      : itr_(path), end_()
   {
   }
   recursive_dir_iterator itr_;
   recursive_dir_iterator end_;
   std::string lastPath_;
};


RecursiveDirectoryIterator::RecursiveDirectoryIterator(
                                                   const FilePath& filePath)
    : pImpl_(new Impl(filePath.pImpl_->path))
{
}

RecursiveDirectoryIterator::~RecursiveDirectoryIterator()
{
}

Error RecursiveDirectoryIterator::next(FilePath* pFilePath)
{
   try
   {
      // calling next() when we are already finished is illegal
      if (finished())
      {
         return systemError(boost::system::errc::operation_not_permitted,
                            ERROR_LOCATION);
      }

      // get the next file path (save it so we can use it in error messages)
      pImpl_->lastPath_ = BOOST_FS_PATH2STR(pImpl_->itr_->path());
      *pFilePath = FilePath(pImpl_->lastPath_);

      // increment the iterator
      ++(pImpl_->itr_);

      // success
      return Success();
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      Error error(e.code(), ERROR_LOCATION);
      error.addProperty("last-path", pImpl_->lastPath_);
      return error;
   }
}

bool RecursiveDirectoryIterator::finished() const
{
   return pImpl_->itr_ == pImpl_->end_;
}


namespace {
void logError(path_t path,
              const boost::filesystem::filesystem_error& e,
              const core::ErrorLocation& errorLocation)
{
   Error error(e.code(), errorLocation) ;
   addErrorProperties(path, &error) ;
   core::log::logError(error, errorLocation) ;
}

void addErrorProperties(path_t path, Error* pError)
{
   pError->addProperty("path", BOOST_FS_PATH2STR(path)) ;
}
}

} // namespace core
} // namespace rstudio
