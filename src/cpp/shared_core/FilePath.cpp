/*
 * FilePath.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant to the terms of a commercial license agreement
 * with RStudio, then this program is licensed to you under the following terms:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

#include <shared_core/FilePath.hpp>

#include <algorithm>
#include <fstream>

#ifdef _WIN32
#include <windows.h>

#include <boost/iostreams/stream.hpp>
#include <boost/iostreams/device/file_descriptor.hpp>
#include <boost/system/windows_error.hpp>

#include <shared_core/system/Win32StringUtils.hpp>
#else
#include <sys/stat.h>
#include <sys/unistd.h>

#include <shared_core/system/PosixSystem.hpp>
#endif

#define BOOST_NO_CXX11_SCOPED_ENUMS
#include <boost/filesystem.hpp>
#undef BOOST_NO_CXX11_SCOPED_ENUMS

#include <boost/algorithm/string/case_conv.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/bind.hpp>
#include <boost/make_shared.hpp>

#include <shared_core/Logger.hpp>
#include <shared_core/Error.hpp>
#include <shared_core/SafeConvert.hpp>
#include <shared_core/system/User.hpp>

typedef boost::filesystem::path path_t;

namespace rstudio {
namespace core {

// Helpers =============================================================================================================
namespace {

struct MimeType
{
   const char* extension;
   const char* contentType;
};

// NOTE: should be synced with mime type database in FileSystemItem.java
MimeType s_mimeTypes[] =
   {
      // most common web types
      { "htm",          "text/html" },
      { "html",         "text/html" },
      { "css",          "text/css" },
      { "sass",         "text/sass" },
      { "scss",         "text/scss" },
      { "gif",          "image/gif" },
      { "jpg",          "image/jpeg" },
      { "jpeg",         "image/jpeg" },
      { "jpe",          "image/jpeg" },
      { "png",          "image/png" },
      { "webp",         "image/webp" },
      { "js",           "text/javascript" },
      { "pdf",          "application/pdf" },
      { "svg",          "image/svg+xml" },
      { "swf",          "application/x-shockwave-flash" },
      { "ttf",          "application/x-font-ttf" },
      { "woff",         "application/font-woff" },
      { "woff2",        "application/font-woff2" },

      // markdown types
      { "md",           "text/x-markdown" },
      { "mdtxt",        "text/x-markdown" },
      { "markdown",     "text/x-markdown" },
      { "yaml",         "text/x-yaml" },
      { "yml",          "text/x-yaml" },

      // programming language types
      { "f",            "text/x-fortran" },
      { "py",           "text/x-python" },
      { "sh",           "text/x-shell" },
      { "sql",          "text/x-sql" },
      { "stan",         "text/x-stan" },
      { "clj",          "text/x-clojure" },
      { "ts",           "text/x-typescript"},
      { "lua",          "text/x-lua"},

      // other types we are likely to serve
      { "xml",          "text/xml" },
      { "csv",          "text/csv" },
      { "ico",          "image/x-icon" },
      { "zip",          "application/zip" },
      { "bz",           "application/x-bzip" },
      { "bz2",          "application/x-bzip2" },
      { "gz",           "application/x-gzip" },
      { "tar",          "application/x-tar" },
      { "json",         "application/json" },
      { "rstheme",      "text/css" },

      // yet more types...

      { "shtml",        "text/html" },
      { "tsv",          "text/tab-separated-values" },
      { "tab",          "text/tab-separated-values" },
      { "cl",           "text/plain" },
      { "dcf",          "text/debian-control-file" },
      { "i",            "text/plain" },
      { "ini",          "text/plain" },
      { "txt",          "text/plain" },
      { "mml",          "text/mathml" },
      { "log",          "text/plain" },
      { "out",          "text/plain" },
      { "csl",          "text/x-csl" },
      { "R",            "text/x-r-source" },
      { "S",            "text/x-r-source" },
      { "q",            "text/x-r-source" },
      { "Rd",           "text/x-r-doc" },
      { "Rnw",          "text/x-r-sweave" },
      { "Rmd",          "text/x-r-markdown" },
      { "Rhtml",        "text/x-r-html" },
      { "Rpres",        "text/x-r-presentation" },
      { "Rout",         "text/plain" },
      { "po",           "text/plain" },
      { "pot",          "text/plain" },
      { "ps1",          "text/plain" },
      { "rst",          "text/plain" },
      { "gitignore",    "text/plain" },
      { "Rbuildignore", "text/plain" },
      { "Rprofile",     "text/x-r-source" },
      { "Renviron",     "text/x-shell" },
      { "rprofvis",     "text/x-r-profile" },
      { "vcxproj",      "text/xml" },

      { "tif",          "image/tiff" },
      { "tiff",         "image/tiff" },
      { "bmp",          "image/bmp" },
      { "ps",           "application/postscript" },
      { "eps",          "application/postscript" },
      { "dvi",          "application/x-dvi" },

      { "atom",         "application/atom+xml" },
      { "rss",          "application/rss+xml" },

      { "doc",          "application/msword" },
      { "docx",         "application/vnd.openxmlformats-officedocument.wordprocessingml.document" },
      { "odt",          "application/vnd.oasis.opendocument.text" },
      { "rtf",          "application/rtf" },
      { "xls",          "application/vnd.ms-excel" },
      { "xlsx",         "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" },
      { "ods",          "application/x-vnd.oasis.opendocument.spreadsheet" },
      { "ppt",          "application/vnd.ms-powerpoint" },
      { "pps",          "application/vnd.ms-powerpoint" },
      { "pptx",         "application/vnd.openxmlformats-officedocument.presentationml.presentation" },

      { "sit",          "application/x-stuffit" },
      { "sxw",          "application/vnd.sun.xml.writer" },

      { "iso",          "application/octet-stream" },
      { "dmg",          "application/octet-stream" },
      { "exe",          "application/octet-stream" },
      { "dll",          "application/octet-stream" },
      { "deb",          "application/octet-stream" },
      { "otf",          "application/octet-stream" },
      { "xpi",          "application/x-xpinstall" },

      { "mp2",          "audio/mpeg" },

      { "mpg",          "video/mpeg" },
      { "mpeg",         "video/mpeg" },
      { "flv",          "video/x-flv" },

      { "mp4",          "video/mp4" },
      { "webm",         "video/webm" },
      { "ogv",          "video/ogg" },

      { "mp3",          "audio/mp3" },
      { "wav",          "audio/wav" },
      { "oga",          "audio/ogg" },
      { "ogg",          "audio/ogg" },

      { nullptr,        nullptr }
   };

const std::string& homePathAlias()
{
   static const std::string homePathAlias = "~/";
   return homePathAlias;
}
const std::string& homePathLeafAlias()
{
   static const std::string homePathLeafAlias = "~";
   return homePathLeafAlias;
}

// We use boost::filesystem in one of two ways:
// - On Windows, we use Filesystem v3 with wide character paths. This is
//   because narrow character paths on Windows can't cover the entire
//   Unicode space since there is no ANSI code page for UTF-8.
// - On non-Windows, we use Filesystem v3.
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
#error FilePath requires Filesystem v3
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

inline internal_string fromString(const std::string& in_value)
{
   return in_value;
}

#endif

void addErrorProperties(path_t path, Error* pError)
{
   pError->addProperty("path", BOOST_FS_PATH2STR(path));
}

bool addItemSize(const FilePath& item, boost::shared_ptr<uintmax_t> pTotal)
{
   if (!item.isDirectory())
      *pTotal = *pTotal + item.getSize();
   return true;
}

#ifndef _WIN32

int octalStrToFileMode(const std::string& fileModeStr)
{
   return safe_convert::stringTo<int>(fileModeStr, 0666, std::oct);
}

inline Error changeFileModeImpl(const std::string& filePath, mode_t mode)
{
   // change the mode
   errno = 0;
   if (::chmod(filePath.c_str(), mode) < 0)
   {
      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("path", filePath);
      return error;
   }
   else
      return Success();
}

#endif

bool copySingleItem(const FilePath& from, const FilePath& to,
                    const FilePath& path, bool overwrite)
{
   std::string relativePath = path.getRelativePath(from);
   FilePath target = to.completePath(relativePath);

   Error error = path.isDirectory() ?
                 target.ensureDirectory() :
                 path.copy(target, overwrite);
   if (error)
      log::logError(error);

   return true;
}

void logError(path_t path,
              const boost::filesystem::filesystem_error& e,
              const ErrorLocation& errorLocation)
{
   Error error(e.code(), errorLocation);
   addErrorProperties(path, &error);
   log::logError(error, errorLocation);
}

Error notFoundError(const FilePath& filePath,
                    const ErrorLocation& location)
{
   Error error = pathNotFoundError(location);
   if (!filePath.isEmpty())
      error.addProperty("path", filePath.getAbsolutePath());
   return error;
}

} // anonymous namespace

// FilePath ============================================================================================================
struct FilePath::Impl
{
   /**
    * @brief Default constructor.
    */
   Impl() = default;

   /**
    * @brief Constructor.
    *
    * @param in_path    The underlying path of this FilePath.
    */
   explicit Impl(path_t in_path) :
      Path(std::move(in_path))
   {
   }

   /** The underlying path of this FilePath. */
   path_t Path;
};

FilePath::FilePath() :
   m_impl(new Impl())
{
}

FilePath::FilePath(const std::string& in_absolutePath) :
   m_impl(new Impl(fromString(std::string(in_absolutePath.c_str())))) // thwart ref-count
{
}

#ifdef _WIN32
FilePath::FilePath(const std::wstring& absolutePath)
   : m_impl(new Impl(absolutePath)) // thwart ref-count
{
}
#endif

bool FilePath::operator==(const FilePath& in_other) const
{
   return m_impl->Path == in_other.m_impl->Path;
}

bool FilePath::operator!=(const FilePath& in_other) const
{
   return m_impl->Path != in_other.m_impl->Path;
}

bool FilePath::operator<(const FilePath& in_other) const
{
   return m_impl->Path < in_other.m_impl->Path;
}

std::string FilePath::createAliasedPath(const FilePath& in_filePath, const FilePath& in_userHomePath)
{
   // Special case for "~"
   if (in_filePath == in_userHomePath)
      return homePathLeafAlias();

#ifdef _WIN32
   // Also check for case where paths are identical
   // after normalizing separators.
   bool samePath =
       in_filePath.m_impl->Path.generic_path() ==
       in_userHomePath.m_impl->Path.generic_path();

   if (samePath)
      return homePathLeafAlias();
#endif

   // if the path is contained within the home path then alias it
   if (in_filePath.isWithin(in_userHomePath))
   {
      std::string homeRelativePath = in_filePath.getRelativePath(in_userHomePath);
      std::string aliasedPath = homePathAlias() + homeRelativePath;
      return aliasedPath;
   }
   else  // no aliasing
   {
      return in_filePath.getAbsolutePath();
   }
}

bool FilePath::exists(const std::string& in_filePath)
{
   if (in_filePath.empty())
      return false;

   path_t p(fromString(in_filePath));
   try
   {
      return boost::filesystem::exists(p);
   }
   catch (const boost::filesystem::filesystem_error& e)
   {
      logError(p, e, ERROR_LOCATION);
      return false;
   }
}

bool FilePath::isEqualCaseInsensitive(const FilePath& in_filePath1, const FilePath& in_filePath2)
{
   std::string file1Lower = boost::algorithm::to_lower_copy(in_filePath1.getAbsolutePath());
   std::string file2Lower = boost::algorithm::to_lower_copy(in_filePath2.getAbsolutePath());
   return file1Lower < file2Lower;
}

bool FilePath::isRootPath(const std::string& in_filePath)
{
   if (in_filePath.empty())
      return false;

   path_t p(fromString(in_filePath));
   try
   {
      return p.has_root_path();
   }
   catch (const boost::filesystem::filesystem_error& e)
   {
      logError(p, e, ERROR_LOCATION);
      return false;
   }
}

Error FilePath::makeCurrent(const std::string& in_filePath)
{
   return FilePath(in_filePath).makeCurrentPath();
}

FilePath FilePath::resolveAliasedPath(const std::string& in_aliasedPath, const FilePath& in_userHomePath)
{
   // Special case for empty string or "~"
   if (in_aliasedPath.empty() || (in_aliasedPath == homePathLeafAlias()))
      return in_userHomePath;

   // if the path starts with the home alias then substitute the home path
   if (in_aliasedPath.find(homePathAlias()) == 0)
   {
      std::string resolvedPath = in_userHomePath.getAbsolutePath() +
                                 in_aliasedPath.substr(1);
      return FilePath(resolvedPath);
   }
   else  // no aliasing, this is either an absolute path or path
      // relative to the current directory
   {
      return FilePath::safeCurrentPath(in_userHomePath).completePath(in_aliasedPath);
   }
}

FilePath FilePath::safeCurrentPath(const FilePath& in_revertToPath)
{
   try
   {
#ifdef _WIN32
      return FilePath(boost::filesystem::current_path().wstring());
#else
      return FilePath(boost::filesystem::current_path().string());
#endif
   }
   catch (const boost::filesystem::filesystem_error& e)
   {
      if (e.code() != boost::system::errc::no_such_file_or_directory)
         log::logError(Error(e.code(), ERROR_LOCATION));
   }
   CATCH_UNEXPECTED_EXCEPTION

   // revert to the specified path if it exists, otherwise
   // take the user home path from the system
   FilePath safePath = in_revertToPath;
   if (!safePath.exists())
      safePath = system::User::getUserHomePath();

   Error error = safePath.makeCurrentPath();
   if (error)
      log::logError(error);

   return safePath;
}

Error FilePath::tempFilePath(FilePath& out_filePath)
{
   return tempFilePath(std::string(), out_filePath);
}

Error FilePath::tempFilePath(const std::string& in_extension, FilePath& out_filePath)
{
   using namespace boost::filesystem;
   try
   {
      path_t path = temp_directory_path();
      return uniqueFilePath(path.string(), in_extension, out_filePath);
   }
   catch (const filesystem_error& e)
   {
      return Error(e.code(), ERROR_LOCATION);
   }

   // keep compiler happy
   return pathNotFoundError(ERROR_LOCATION);
}

Error FilePath::uniqueFilePath(const std::string& in_basePath, FilePath& out_filePath)
{
   return uniqueFilePath(in_basePath, std::string(), out_filePath);
}

Error FilePath::uniqueFilePath(const std::string& in_basePath, const std::string& in_extension, FilePath& out_filePath)
{
   using namespace boost::filesystem;
   try
   {
      path_t path = absolute(unique_path(), fromString(in_basePath));
      std::string pathStr = BOOST_FS_PATH2STR(path);
      if (!in_extension.empty())
         pathStr += in_extension;
      out_filePath = FilePath(pathStr);
      return Success();
   }
   catch (const filesystem_error& e)
   {
      return Error(e.code(), ERROR_LOCATION);
   }

   // keep compiler happy
   return pathNotFoundError(ERROR_LOCATION);
}

#ifndef _WIN32

Error FilePath::changeFileMode(const std::string& fileModeStr) const
{
   return changeFileModeImpl(getAbsolutePath(), octalStrToFileMode(fileModeStr));
}

Error FilePath::changeFileMode(FileMode in_fileMode, bool in_setStickyBit) const
{
   mode_t mode;
   switch (in_fileMode)
   {
      case FileMode::USER_READ_WRITE:
         mode = S_IRUSR | S_IWUSR;
         break;

      case FileMode::USER_READ_WRITE_EXECUTE:
         mode = S_IRUSR | S_IWUSR | S_IXUSR;
         break;

      case FileMode::USER_READ_WRITE_GROUP_READ:
         mode = S_IRUSR | S_IWUSR | S_IRGRP;
         break;

      case FileMode::USER_READ_WRITE_ALL_READ:
         mode = S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH;
         break;

      case FileMode::USER_READ_WRITE_EXECUTE_ALL_READ_EXECUTE:
         mode = S_IRWXU | S_IRGRP | S_IXGRP | S_IROTH | S_IXOTH;
         break;

      case FileMode::ALL_READ:
         mode = S_IRUSR | S_IRGRP | S_IROTH;
         break;

      case FileMode::ALL_READ_WRITE:
         mode = S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH;
         break;

      case FileMode::ALL_READ_WRITE_EXECUTE:
         mode = S_IRWXU | S_IRWXG | S_IRWXO;
         break;

      default:
         return systemError(ENOTSUP, ERROR_LOCATION);
   }

   // check for sticky bit
   if (in_setStickyBit)
      mode |= S_ISVTX;

   return changeFileModeImpl(getAbsolutePath(), mode);
}

Error FilePath::changeOwnership(
   const system::User& in_newUser,
   bool in_recursive,
   const RecursiveIterationFunction& in_shouldChown) const
{
   // changes ownership of file to the server user
   auto chown = [&](const std::string& in_absolutePath)
   {
      return core::system::posix::posixCall<int>(
         boost::bind(::chown,
                     in_absolutePath.c_str(),
                     in_newUser.getUserId(),
                     in_newUser.getGroupId()),
         ERROR_LOCATION);
   };

   Error error = chown(getAbsolutePath());
   if (error)
   {
      error.addProperty("path", getAbsolutePath());
      return error;
   }

   if (!in_recursive)
      return Success();

   // recurse into subdirectories
   if (isDirectory())
   {
      getChildrenRecursive([&](int depth, const FilePath& child)
                                {
                                   if (in_shouldChown && !in_shouldChown(depth, child))
                                      return true;

                                   error = chown(child.getAbsolutePath());
                                   if (error)
                                      error.addProperty("path", child.getAbsolutePath());

                                   // if there was an error, stop iterating
                                   return !error;
                                });
   }

   return error;

}

#endif

// note: this differs from complete in the following ways:
//    - the passed path can be an empty string (returns self)
//    - the passed path must be relative
FilePath FilePath::completeChildPath(const std::string& in_filePath) const
{
   FilePath childPath;
   Error error = completeChildPath(in_filePath, childPath);
   if (error)
      log::logError(error);

   return childPath;
}

Error FilePath::completeChildPath(const std::string& in_filePath, FilePath& out_childPath) const
{
   try
   {
      if (in_filePath.empty())
      {
         out_childPath = *this;
      }
      else
      {
         // confirm this is a relative path
         path_t relativePath(fromString(in_filePath));
         if (relativePath.has_root_path())
         {
            throw boost::filesystem::filesystem_error(
               "absolute path not permitted",
               boost::system::error_code(
                  boost::system::errc::no_such_file_or_directory,
                  boost::system::system_category()));
         }

         out_childPath = completePath(in_filePath);

         if (!out_childPath.isWithin(*this))
         {
            throw boost::filesystem::filesystem_error(
               "child path must be inside parent path",
               boost::system::error_code(
                  boost::system::errc::no_such_file_or_directory,
                  boost::system::system_category()));
         }
      }
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      out_childPath = *this;

      Error error(e.code(), ERROR_LOCATION);
      addErrorProperties(m_impl->Path, &error);
      error.addProperty("path", in_filePath);
      return error;
   }

   return Success();
}

FilePath FilePath::completePath(const std::string& in_filePath) const
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
      return FilePath(BOOST_FS_PATH2STR(BOOST_FS_COMPLETE(in_filePath, m_impl->Path)));
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      Error error(e.code(), ERROR_LOCATION);
      addErrorProperties(m_impl->Path, &error);
      error.addProperty("path", in_filePath);
      log::logError(error);
      return *this;
   }
}

Error FilePath::copy(const FilePath& in_targetPath, bool overwrite) const
{
   try
   {
      boost::filesystem::copy_option::enum_type option = overwrite
         ? boost::filesystem::copy_option::overwrite_if_exists
         : boost::filesystem::copy_option::fail_if_exists;
      boost::filesystem::copy_file(m_impl->Path, in_targetPath.m_impl->Path, option);
      return Success();
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      Error error(e.code(), ERROR_LOCATION);
      addErrorProperties(m_impl->Path, &error);
      error.addProperty("target-path", in_targetPath.getAbsolutePath());
      return error;
   }
}

Error FilePath::copyDirectoryRecursive(const FilePath& in_targetPath, bool overwrite) const
{
   Error error = in_targetPath.ensureDirectory();
   if (error)
      return error;

   return getChildrenRecursive(boost::bind(copySingleItem, *this, in_targetPath, _2, overwrite));
}

Error FilePath::createDirectory(const std::string& in_filePath) const
{
   try
   {
      path_t targetDirectory;
      if (in_filePath.empty())
         targetDirectory = m_impl->Path;
      else
         targetDirectory = BOOST_FS_COMPLETE(in_filePath, m_impl->Path);
      boost::filesystem::create_directories(targetDirectory);
      return Success();
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      Error error(e.code(), ERROR_LOCATION);
      addErrorProperties(m_impl->Path, &error);
      error.addProperty("target-dir", in_filePath);
      return error;
   }
}

Error FilePath::ensureDirectory() const
{
   if (!exists())
      return createDirectory(std::string());
   else
      return Success();
}

Error FilePath::ensureFile() const
{
   // nothing to do if the file already exists
   if (exists())
      return Success();

   // create output stream to ensure file creation
   std::shared_ptr<std::ostream> pStream;
   Error error = openForWrite(pStream);
   if (error)
      return error;

   // release file handle
   pStream->flush();
   pStream.reset();

   return Success();
}

bool FilePath::exists() const
{
   try
   {
      return !isEmpty() && boost::filesystem::exists(m_impl->Path);
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      if (e.code() != boost::system::errc::permission_denied)
         logError(m_impl->Path, e, ERROR_LOCATION);
      return false;
   }
}

std::string FilePath::getAbsolutePath() const
{
   if (isEmpty())
      return std::string();
   else
      return BOOST_FS_PATH2STR(m_impl->Path);
}

std::string FilePath::getAbsolutePathNative() const
{
   if (isEmpty())
      return std::string();
   else
      return BOOST_FS_PATH2STRNATIVE(m_impl->Path);
}

#ifdef _WIN32
std::wstring FilePath::getAbsolutePathW() const
{
   if (isEmpty())
      return std::wstring();
   else
      return m_impl->Path.wstring();
}
#endif

std::string FilePath::getCanonicalPath() const
{
   if (isEmpty())
      return std::string();
   else
      return BOOST_FS_PATH2STR(boost::filesystem::canonical(m_impl->Path));
}

Error FilePath::getChildren(std::vector<FilePath>& out_filePaths) const
{
   if (!exists())
      return notFoundError(*this, ERROR_LOCATION);

   try
   {
      dir_iterator end;
      for (dir_iterator itr(m_impl->Path); itr != end; ++itr)
      {
         // NOTE: The path gets round-tripped through toString/fromString, would
         //   be nice to have a direct constructor
         std::string itemPath = BOOST_FS_PATH2STR(itr->path());
         out_filePaths.emplace_back(itemPath);
      }
      return Success();
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      Error error(e.code(), ERROR_LOCATION);
      addErrorProperties(m_impl->Path, &error);
      return error;
   }
}

Error FilePath::getChildrenRecursive(const RecursiveIterationFunction& in_iterationFunction) const
{
   if (!exists())
      return notFoundError(*this, ERROR_LOCATION);

   try
   {
      recursive_dir_iterator end;

      for (recursive_dir_iterator itr(m_impl->Path); itr != end; ++itr)
      {
         // NOTE: The path gets round-tripped through toString/fromString, would
         //   be nice to have a direct constructor
         if (!in_iterationFunction(itr.depth(),
                                   FilePath(BOOST_FS_PATH2STR(itr->path()))))
         {
            // end the iteration if requested
            break;
         }
      }

      return Success();
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      Error error(e.code(), ERROR_LOCATION);
      addErrorProperties(m_impl->Path, &error);
      return error;
   }
}

std::string FilePath::getExtension() const
{
   return BOOST_FS_STRING(m_impl->Path.extension());
}

std::string FilePath::getExtensionLowerCase() const
{
   return boost::algorithm::to_lower_copy(getExtension());
}

#ifndef _WIN32

Error FilePath::getFileMode(FileMode& out_fileMode) const
{
   struct stat st;
   if (::stat(getAbsolutePath().c_str(), &st) == -1)
   {
      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("path", getAbsolutePath());
      return error;
   }

   // extract the bits
   std::string mode(9, '-');
   if ( st.st_mode & S_IRUSR ) mode[0] = 'r';
   if ( st.st_mode & S_IWUSR ) mode[1] = 'w';
   if ( st.st_mode & S_IXUSR ) mode[2] = 'x';

   if ( st.st_mode & S_IRGRP ) mode[3] = 'r';
   if ( st.st_mode & S_IWGRP ) mode[4] = 'w';
   if ( st.st_mode & S_IXGRP ) mode[5] = 'x';

   if ( st.st_mode & S_IROTH ) mode[6] = 'r';
   if ( st.st_mode & S_IWOTH ) mode[7] = 'w';
   if ( st.st_mode & S_IXOTH ) mode[8] = 'x';

   if (mode ==      "rw-------")
      out_fileMode = FileMode::USER_READ_WRITE;
   else if (mode == "rwx------")
      out_fileMode = FileMode::USER_READ_WRITE_EXECUTE;
   else if (mode == "rw-r-----")
      out_fileMode = FileMode::USER_READ_WRITE_GROUP_READ;
   else if (mode == "rw-r--r--")
      out_fileMode = FileMode::USER_READ_WRITE_ALL_READ;
   else if (mode == "r--r--r--")
      out_fileMode = FileMode::ALL_READ;
   else if (mode == "rw-rw-rw-")
      out_fileMode = FileMode::ALL_READ_WRITE;
   else if (mode == "rwxrwxrwx")
      out_fileMode = FileMode::ALL_READ_WRITE_EXECUTE;
   else if (mode == "rwxr-xr-x")
      out_fileMode = FileMode::USER_READ_WRITE_EXECUTE_ALL_READ_EXECUTE;
   else
      return systemError(boost::system::errc::not_supported, ERROR_LOCATION);

   return Success();
}

#endif

std::string FilePath::getFilename() const
{
   return BOOST_FS_STRING(m_impl->Path.filename());
}

std::time_t FilePath::getLastWriteTime() const
{
   try
   {
      if (!exists())
         return 0;
      else
         return boost::filesystem::last_write_time(m_impl->Path);
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      logError(m_impl->Path, e, ERROR_LOCATION);
      return 0;
   }
}

std::string FilePath::getLexicallyNormalPath() const
{
   if (isEmpty())
      return std::string();
   else
      return BOOST_FS_PATH2STR(m_impl->Path.lexically_normal());
}

std::string FilePath::getMimeContentType(const std::string& in_defaultType) const
{
   std::string ext = getExtensionLowerCase();
   if (!ext.empty())
   {
      ext = ext.substr(1); // remove leading .
      for (MimeType* mimeType = s_mimeTypes; mimeType->extension; ++mimeType)
      {
         if (boost::algorithm::iequals(mimeType->extension,ext))
            return mimeType->contentType;
      }
   }

   // no extension
   return in_defaultType;
}

FilePath FilePath::getParent() const
{
   try
   {
      // NOTE: The path gets round-tripped through toString/fromString, would
      //   be nice to have a direct constructor
      return FilePath(BOOST_FS_PATH2STR(m_impl->Path.parent_path()));
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      Error error(e.code(), ERROR_LOCATION);
      addErrorProperties(m_impl->Path, &error);
      log::logError(error);
      return *this;
   }
}

std::string FilePath::getRelativePath(const FilePath& in_parentPath) const
{
   // NOTE: On Windows, we need to explicitly normalize separators.
   // We use forward slashes since most of our file APIs prefer
   // these separators, and Windows APIs transparently translate
   // forward slashes to backslashes anyhow.
   path_t self = m_impl->Path.generic_path().lexically_normal();
   path_t parent = in_parentPath.m_impl->Path.generic_path().lexically_normal();
   path_t relative = self.lexically_relative(parent);
   return BOOST_FS_PATH2STR(relative);
}

uintmax_t FilePath::getSize() const
{
   try
   {
      if (!exists() || !boost::filesystem::is_regular_file(m_impl->Path))
         return 0;
      else
         return boost::filesystem::file_size(m_impl->Path);
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
#ifdef _WIN32
      if (e.code().value() == ERROR_NOT_SUPPORTED)
         return 0;
#endif
      Error err = Error(e.code(), ERROR_LOCATION);
      err.addProperty("path", getAbsolutePath());
      log::logError(err);
      return 0;
   }
}

uintmax_t FilePath::getSizeRecursive() const
{
   // no work to do if we're not a directory
   if (!isDirectory())
      return getSize();

   boost::shared_ptr<uintmax_t> pTotal = boost::make_shared<uintmax_t>(0);
   Error error = getChildrenRecursive(boost::bind(addItemSize, _2, pTotal));
   if (error)
      log::logError(error);
   return *pTotal;
}

std::string FilePath::getStem() const
{
   return BOOST_FS_STRING(m_impl->Path.stem());
}

bool FilePath::hasExtension(const std::string& in_extension) const
{
   return getExtension() == in_extension;
}

bool FilePath::hasExtensionLowerCase(const std::string& in_extension) const
{
   return getExtensionLowerCase() == in_extension;
}

bool FilePath::hasTextMimeType() const
{
   std::string mimeType = getMimeContentType("application/octet-stream");
   return boost::algorithm::starts_with(mimeType, "text/") ||
          boost::algorithm::ends_with(mimeType, "+xml");
}

bool FilePath::isDirectory() const
{
   try
   {
      if (!exists())
         return false;
      else
      {
         return boost::filesystem::is_directory(m_impl->Path)
#ifdef _WIN32
            || isJunction()
#endif
            ;
      }
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      logError(m_impl->Path, e, ERROR_LOCATION);
      return false;
   }
}

bool FilePath::isEmpty() const
{
   return m_impl->Path.empty();
}

// check for equivalence (point to the same file-system entity)
bool FilePath::isEquivalentTo(const FilePath& in_other) const
{
   if (!exists() || !in_other.exists())
      return false;

   try
   {
      return boost::filesystem::equivalent(m_impl->Path, in_other.m_impl->Path);
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      Error error(e.code(), ERROR_LOCATION);
      addErrorProperties(m_impl->Path, &error);
      error.addProperty("equivalent-to", in_other);
      return false;
   }
}

bool FilePath::isHidden() const
{
   return !getFilename().empty() && (getFilename()[0] == '.');
}

bool FilePath::isJunction() const
{
#ifndef _WIN32
   return false;
#else
   if (!exists())
      return false;

   const wchar_t* path = m_impl->Path.c_str();
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

#ifndef _WIN32

Error FilePath::isReadable(bool &out_readable) const
{
   int result = ::access(getAbsolutePath().c_str(), R_OK);
   if (result == 0)
   {
      // user has access
      out_readable = true;
   }
   else if (errno == EACCES)
   {
      // this error is expected when the user doesn't have access to the path
      out_readable = false;
   }
   else
   {
      // some other error (unexpected)
      return systemError(errno, ERROR_LOCATION);
   }

   return Success();
}

#endif

bool FilePath::isRegularFile() const
{
   try
   {
      if (!exists())
         return false;
      else
      {
         return boost::filesystem::is_regular_file(m_impl->Path);
      }
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      logError(m_impl->Path, e, ERROR_LOCATION);
      return false;
   }
}

bool FilePath::isSymlink() const
{
   try
   {
      return exists() && boost::filesystem::is_symlink(m_impl->Path);
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      logError(m_impl->Path, e, ERROR_LOCATION);
      return false;
   }
}

bool FilePath::isWithin(const FilePath& in_scopePath) const
{
   // Technically, we contain ourselves.
   if (*this == in_scopePath)
      return true;

   // Make the paths lexically normal so that e.g. foo/../bar isn't considered a child of foo.
   FilePath child(getLexicallyNormalPath());
   FilePath parent(in_scopePath.getLexicallyNormalPath());

   // Easy test: We can't possibly be in this scope path if it has more components than we do
   if (parent.m_impl->Path.size() > child.m_impl->Path.size())
      return false;

   // Find the first path element that differs. Stop when we reach the end of the parent
   // path, or a "." path component, which signifies the end of a directory (/foo/bar/.)
   for (boost::filesystem::path::iterator childIt = child.m_impl->Path.begin(),
                                          parentIt = parent.m_impl->Path.begin();
        parentIt != parent.m_impl->Path.end() && *parentIt != ".";
        parentIt++, childIt++)
   {
      if (*parentIt != *childIt)
      {
         // Found a differing path element
         return false;
      }
   }

   // No differing path element found
   return true;
}

#ifndef _WIN32

Error FilePath::isWriteable(bool &out_writeable) const
{
   int result = ::access(getAbsolutePath().c_str(), W_OK);
   if (result == 0)
   {
      // user has access
      out_writeable = true;
   }
   else if (errno == EACCES)
      out_writeable = false;
   else
      return systemError(errno, ERROR_LOCATION);

   return Success();
}

#endif

Error FilePath::makeCurrentPath(bool in_autoCreate) const
{
   if (in_autoCreate)
   {
      Error autoCreateError = ensureDirectory();
      if (autoCreateError)
         return autoCreateError;
   }

   try
   {
      boost::filesystem::current_path(m_impl->Path);
      return Success();
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      Error error(e.code(), ERROR_LOCATION);
      addErrorProperties(m_impl->Path, &error);
      return error;
   }
}

Error FilePath::move(const FilePath& in_targetPath, MoveType in_type, bool overwrite) const
{
   try
   {
      boost::filesystem::rename(m_impl->Path, in_targetPath.m_impl->Path);
      return Success();
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      if (in_type == MoveCrossDevice &&
          e.code() == boost::system::errc::cross_device_link)
      {
         // this error implies that we're trying to move a file from one
         // device to another; in this case, fall back to copy/delete
         return moveIndirect(in_targetPath, overwrite);
      }
      Error error(e.code(), ERROR_LOCATION);
      addErrorProperties(m_impl->Path, &error);
      error.addProperty("target-path", in_targetPath.getAbsolutePath());
      return error;
   }
}

Error FilePath::moveIndirect(const FilePath& in_targetPath, bool overwrite) const
{
   // when target is a directory, moving has the effect of moving *into* the
   // directory (rather than *replacing* it); simulate that behavior here
   FilePath target = in_targetPath.isDirectory() ?
                     in_targetPath.completePath(getFilename()) : in_targetPath;

   // copy the file or directory to the new location
   Error error = isDirectory() ?
                 copyDirectoryRecursive(target, overwrite) : copy(target, overwrite);
   if (error)
      return error;

   // delete the original copy of the file or directory (not considered a fatal
   // error)
   error = remove();
   if (error)
      log::logError(error);

   return Success();
}

Error FilePath::openForRead(std::shared_ptr<std::istream>& out_stream) const
{
   std::istream* pResult = nullptr;
   try
   {
#ifdef _WIN32
      using namespace boost::iostreams;
      HANDLE hFile = ::CreateFileW(m_impl->Path.wstring().c_str(),
                                   GENERIC_READ,
                                   FILE_SHARE_READ,
                                   nullptr,
                                   OPEN_EXISTING,
                                   0,
                                   nullptr);
      if (hFile == INVALID_HANDLE_VALUE)
      {
         Error error = LAST_SYSTEM_ERROR();
         error.addProperty("path", getAbsolutePath());
         return error;
      }
      boost::iostreams::file_descriptor_source fd;
      fd.open(hFile, boost::iostreams::close_handle);
      pResult = new boost::iostreams::stream<file_descriptor_source>(fd);
#else
      pResult = new std::ifstream(getAbsolutePath().c_str(), std::ios_base::in | std::ios_base::binary);
#endif

      // In case we were able to make the stream but it failed to open
      if (!(*pResult))
      {
         delete pResult;

         Error error = systemError(boost::system::errc::no_such_file_or_directory, ERROR_LOCATION);
         error.addProperty("path", getAbsolutePath());
         return error;
      }
      out_stream.reset(pResult);
   }
   catch(const std::exception& e)
   {
      delete pResult;

      Error error = systemError(boost::system::errc::io_error,
                                ERROR_LOCATION);
      error.addProperty("what", e.what());
      error.addProperty("path", getAbsolutePath());
      return error;
   }


   return Success();
}

Error FilePath::openForWrite(std::shared_ptr<std::ostream>& out_stream, bool in_truncate) const
{
   std::ostream* pResult = nullptr;
   try
   {
#ifdef _WIN32
      using namespace boost::iostreams;
      HANDLE hFile = ::CreateFileW(m_impl->Path.wstring().c_str(),
                                   in_truncate ? GENERIC_WRITE : FILE_APPEND_DATA,
                                   0, // exclusive access
                                   nullptr,
                                   in_truncate ? CREATE_ALWAYS : OPEN_ALWAYS,
                                   0,
                                   nullptr);
      if (hFile == INVALID_HANDLE_VALUE)
      {
         Error error = LAST_SYSTEM_ERROR();
         error.addProperty("path", getAbsolutePath());
         return error;
      }
      file_descriptor_sink fd;
      fd.open(hFile, close_handle);
      pResult = new boost::iostreams::stream<file_descriptor_sink>(fd);
#else
      using std::ios_base;
      ios_base::openmode flags = ios_base::out | ios_base::binary;
      if (in_truncate)
         flags |= ios_base::trunc;
      else
         flags |= ios_base::app;
      pResult = new std::ofstream(getAbsolutePath().c_str(), flags);
#endif

      if (!(*pResult))
      {
         delete pResult;

         Error error = systemError(boost::system::errc::no_such_file_or_directory, ERROR_LOCATION);
         error.addProperty("path", getAbsolutePath());
         return error;
      }

      out_stream.reset(pResult);
   }
   catch(const std::exception& e)
   {
      delete pResult;
      Error error = systemError(boost::system::errc::io_error,
                                ERROR_LOCATION);
      error.addProperty("what", e.what());
      error.addProperty("path", getAbsolutePath());
      return error;
   }

   return Success();
}

Error FilePath::remove() const
{
   try
   {
      if (isDirectory())
         boost::filesystem::remove_all(m_impl->Path);
      else
         boost::filesystem::remove(m_impl->Path);
      return Success();
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      Error error(e.code(), ERROR_LOCATION);
      addErrorProperties(m_impl->Path, &error);
      return error;
   }
}

Error FilePath::removeIfExists() const
{
   if (exists())
      return remove();
   else
      return Success();
}

Error FilePath::resetDirectory() const
{
   Error error = removeIfExists();
   if (error)
      return error;

   return ensureDirectory();
}

FilePath FilePath::resolveSymlink() const
{
   try
   {
      if (!isSymlink())
         return *this;

      return FilePath(BOOST_FS_PATH2STR(boost::filesystem::read_symlink(m_impl->Path)));
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      logError(m_impl->Path, e, ERROR_LOCATION);
      return *this;
   }
}

void FilePath::setLastWriteTime(std::time_t in_time) const
{
   try
   {
      if (!exists())
         return;
      else
         boost::filesystem::last_write_time(m_impl->Path, in_time);
   }
   catch(const boost::filesystem::filesystem_error& e)
   {
      logError(m_impl->Path, e, ERROR_LOCATION);
      return;
   }
}

Error FilePath::testWritePermissions() const
{
   std::ostream* pStream = nullptr;
   try
   {
#ifdef _WIN32
      using namespace boost::iostreams;
      HANDLE hFile = ::CreateFileW(m_impl->Path.wstring().c_str(),
                                   FILE_APPEND_DATA,
                                   0, // exclusive access
                                   nullptr,
                                   OPEN_EXISTING,
                                   0,
                                   nullptr);
      if (hFile == INVALID_HANDLE_VALUE)
      {
         Error error = LAST_SYSTEM_ERROR();
         error.addProperty("path", getAbsolutePath());
         return error;
      }
      file_descriptor_sink fd;
      fd.open(hFile, close_handle);
      pStream = new boost::iostreams::stream<file_descriptor_sink>(fd);
#else
      using std::ios_base;
      ios_base::openmode flags = ios_base::in | ios_base::out | ios_base::binary;
      pStream = new std::ofstream(getAbsolutePath().c_str(), flags);
#endif

      if (!(*pStream))
      {
         delete pStream;

         Error error = systemError(boost::system::errc::no_such_file_or_directory, ERROR_LOCATION);
         error.addProperty("path", getAbsolutePath());
         return error;
      }
   }
   catch(const std::exception& e)
   {
      delete pStream;

      Error error = systemError(boost::system::errc::io_error,
                                ERROR_LOCATION);
      error.addProperty("what", e.what());
      error.addProperty("path", getAbsolutePath());
      return error;
   }

   delete pStream;
   return Success();
}

// PathScope Classes ===================================================================================================
struct PathScopeImpl
{
   PathScopeImpl(FilePath&& in_path, ErrorLocation&& in_location) :
      Path(in_path),
      Location(in_location)
   { }

   FilePath Path;
   ErrorLocation Location;
};

void PathScopeImplDeleter::operator()(PathScopeImpl* in_toDelete)
{
   delete in_toDelete;
}

RestoreCurrentPathScope::RestoreCurrentPathScope(FilePath in_restorePath, ErrorLocation in_location) :
   m_impl(new PathScopeImpl(std::move(in_restorePath), std::move(in_location)))
{
}

RestoreCurrentPathScope::~RestoreCurrentPathScope()
{
   try
   {
      Error error = m_impl->Path.makeCurrentPath();
      if (error)
         log::logError(error, m_impl->Location);
   }
   catch(...)
   {
      log::logErrorMessage(
         "An unexpected error occurred when attempting to restore the working directory.",
         m_impl->Location);
   }
}

RemoveOnExitScope::RemoveOnExitScope(FilePath in_filePath, ErrorLocation in_location) :
   m_impl(new PathScopeImpl(std::move(in_filePath), std::move(in_location)))
{
}

RemoveOnExitScope::~RemoveOnExitScope()
{
   try
   {
      Error error = m_impl->Path.removeIfExists();
      if (error)
         log::logError(error, m_impl->Location);
   }
   catch(...)
   {
   }
}

// File system error creators ==========================================================================================
std::ostream& operator<<(std::ostream& io_stream, const FilePath& in_filePath)
{
   io_stream << in_filePath.getAbsolutePath();
   return io_stream;
}

Error fileExistsError(const ErrorLocation& in_location)
{
#ifdef _WIN32
   return systemError(boost::system::windows_error::file_exists, in_location);
#else
   return systemError(boost::system::errc::file_exists, in_location);
#endif
}

Error fileExistsError(const FilePath& in_filePath, const ErrorLocation& in_location)
{
   Error error = fileExistsError(in_location);
   error.addProperty("path", in_filePath);
   return error;
}

bool isFileNotFoundError(const Error& in_error)
{
#ifdef _WIN32
   return in_error == boost::system::windows_error::file_not_found;
#else
   return in_error == systemError(boost::system::errc::no_such_file_or_directory, ErrorLocation());
#endif
}

Error fileNotFoundError(const ErrorLocation& in_location)
{
#ifdef _WIN32
   return systemError(boost::system::windows_error::file_not_found, in_location);
#else
   return systemError(boost::system::errc::no_such_file_or_directory, in_location);
#endif
}

Error fileNotFoundError(const std::string& in_filePath, const ErrorLocation& in_location)
{
   Error error = fileNotFoundError(in_location);
   error.addProperty("path", in_filePath);
   return error;
}

Error fileNotFoundError(const FilePath& in_filePath, const ErrorLocation& in_location)
{
   Error error = fileNotFoundError(in_location);
   error.addProperty("path", in_filePath);
   return error;
}

bool isPathNotFoundError(const Error& in_error)
{
#ifdef _WIN32
   return in_error == boost::system::windows_error::path_not_found;
#else
   return in_error == systemError(boost::system::errc::no_such_file_or_directory, ErrorLocation());
#endif
}

Error pathNotFoundError(const ErrorLocation& in_location)
{
#ifdef _WIN32
   return systemError(boost::system::windows_error::path_not_found, in_location);
#else
   return systemError(boost::system::errc::no_such_file_or_directory, in_location);
#endif
}

Error pathNotFoundError(const std::string& in_path, const ErrorLocation& in_location)
{
   Error error = pathNotFoundError(in_location);
   error.addProperty("path", in_path);
   return error;
}

bool isNotFoundError(const Error& in_error)
{
#ifdef _WIN32
   return in_error == boost::system::windows_error::file_not_found ||
          in_error == boost::system::windows_error::path_not_found;
#else
   return in_error == systemError(boost::system::errc::no_such_file_or_directory, ErrorLocation());
#endif
}

} // namespace core
} // namespace rstudio
