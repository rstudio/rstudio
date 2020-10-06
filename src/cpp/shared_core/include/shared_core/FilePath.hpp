/*
 * FilePath.hpp
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

#ifndef SHARED_CORE_FILE_PATH_HPP
#define SHARED_CORE_FILE_PATH_HPP

#include <cstdint>
#include <ctime>

#include <functional>
#include <iosfwd>
#include <memory>
#include <string>
#include <vector>

#include <boost/utility.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/Logger.hpp>
#include <shared_core/PImpl.hpp>

namespace rstudio {
namespace core {
namespace system {

class User;

} // namespace system
} // namespace core
} // namespace rstudio

namespace rstudio {
namespace core {

#ifndef _WIN32

/**
 * @enum FileMode
 * Represents all possible posix file modes.
 */
enum class FileMode
{
   USER_READ_WRITE,
   USER_READ_WRITE_EXECUTE,
   USER_READ_WRITE_GROUP_READ,
   USER_READ_WRITE_ALL_READ,
   USER_READ_WRITE_EXECUTE_ALL_READ_EXECUTE,
   ALL_READ,
   ALL_READ_WRITE,
   ALL_READ_WRITE_EXECUTE
};

#endif

/**
 * @brief Class which represents a path on the system. May be any type of file (e.g. directory, symlink, regular file,
 *        etc.)
 */
class FilePath
{
public:
   /**
    * @brief Enum which represents the type of move to perform.
    */
   enum MoveType
   {
      /** Attempt to perform an ordinary move */
      MoveDirect,

      /** Perform an ordinary move, but fallback to copy/delete on cross-device errors */
      MoveCrossDevice
   };

   /**
    * @brief Function which recursively iterates over FilePath objects.
    *
    * @param int            The depth of the iteration.
    * @param FilePath       The current FilePath object in the recursive iteration.
    *
    * @return True if the computation can continue; false otherwise.
    */
   typedef std::function<bool(int, const FilePath&)> RecursiveIterationFunction;

   /**
    * @brief Default constructor.
    */
   FilePath();

   /**
    * @brief Constructor.
    *
    * @param in_absolutePath    The string representation of the path.
    */
   explicit FilePath(const std::string& in_absolutePath);

#ifdef _WIN32
   explicit FilePath(const std::wstring& in_absolutePath);
#endif

   /**
    * @brief Comparison operator. File paths are equal if their absolute representations are equal.
    *
    * @param in_other   The file path to compare with this file path.
    *
    * @return True if the file paths are equal; false otherwise.
    */
   bool operator==(const FilePath& in_other) const;

   /**
    * @brief Comparison operator. File paths are equal if their absolute representations are equal.
    *
    * @param in_other   The file path to compare with this file path.
    *
    * @return True if the file paths are not equal; false otherwise.
    */
   bool operator!=(const FilePath& in_other) const;

   /**
    * @brief Less-than operator to establish natural order. The natural order is based on the absolute representation of
    *        the paths.
    *
    * @param in_other   The path to which to compare this path.
    *
    * @return True if the absolute representation of this path is less, alphabetically, than the absolute representation
    *         of the other path; false otherwise.
    */
   bool operator<(const FilePath& in_other) const;

   /**
    * @brief Creates a path in which the user home path will be replaced by the ~ alias.
    *
    * @param in_filePath       The path to convert to an aliased path.
    * @param in_userHomePath   The user home path.
    *
    * @return If the path is within the user home path, an aliased path; the original path otherwise.
    */
   static std::string createAliasedPath(const FilePath& in_filePath, const FilePath& in_userHomePath);

   /**
    * @brief Checks whether the specified path exists.
    *
    * @param in_filePath       The path to check.
    *
    * @return True if the specified path exists; false otherwise.
    */
   static bool exists(const std::string& in_filePath);

   /**
    * @brief Checks whether the two provided files are equal, ignoring case. Two files are equal if their absolute paths
    *        are equal.
    *
    * @param in_filePath1   The first file to compare.
    * @param in_filePath2   The second file to compare.
    *
    * @return True if the absolute representations of the paths are equal, case insensitively; false otherwise.
    */
   static bool isEqualCaseInsensitive(const FilePath& in_filePath1, const FilePath& in_filePath2);

   /**
    * @brief Checks whether the specified path is a root path or a relative path.
    *
    * @param in_filePath   The path to check.
    *
    * @return True if the path is a root path; false if the path is a relative path.
    */
   static bool isRootPath(const std::string& in_filePath);

   /**
    * @brief Changes the current working directory to the specified path.
    *
    * @param in_filePath    The path to which to change the current working directory.
    *
    * @return Success if in_path exists and can be moved to; Error otherwise.
    */
   static Error makeCurrent(const std::string& in_filePath);

   /**
    * @brief Resolves the '~' alias within the path to the user's home path.
    *
    * @param in_aliasedPath     The aliased path to resolve.
    * @param in_userHomePath    The user's home path.
    *
    * @return The resolved path.
    */
   static FilePath resolveAliasedPath(const std::string& in_aliasedPath, const FilePath& in_userHomePath);

   /**
    * @brief Checks whether the current working directory exists. If it does not, moves the current working directory to
    *        the specified path.
    *
    * @param in_revertToPath    The path to revert to if the current working directory no longer exists.
    *
    * @return The current working directory.
    */
   static FilePath safeCurrentPath(const FilePath& in_revertToPath);

   /**
    * @brief Creates a randomly named file in the temp directory.
    *
    * @param out_filePath   The absolute path of the newly created file, or an empty file path if the file could not be
    *                       created.
    *
    * @return Success if the file could be created; Error otherwise.
    */
   static Error tempFilePath(FilePath& out_filePath);

   /**
    * @brief Creates a randomly named file with the specified extension in the temp directory.
    *
    * @param in_extension   The extension with which to create the file. The extension should include the leading '.',
    *                       e.g. '.zip'.
    * @param out_filePath   The absolute path of the newly created file, or an empty file path if the file could not be
    *                       created.
    *
    * @return Success if the file could be created; Error otherwise.
    */
   static Error tempFilePath(const std::string& in_extension, FilePath& out_filePath);

   /**
    * @brief Creates a file with a random name in the specified directory.
    *
    * @param in_basePath    The path at which to create the file.
    * @param out_filePath   The absolute path of the newly created file, or an empty file path if the file could not be
    *                       created.
    *
    * @return Success if the file could be created; Error otherwise.
    */
   static Error uniqueFilePath(const std::string& in_basePath, FilePath& out_filePath);

   /**
    * @brief Creates a file with a random name and the specified extension in the specified directory.
    *
    * @param in_basePath    The path at which to create the file.
    * @param in_extension   The extension with which to create the file. The extension should include the leading '.',
    *                       e.g. '.zip'.
    * @param out_filePath   The absolute path of the newly created file, or an empty file path if the file could not be
    *                       created.
    *
    * @return Success if the file could be created; Error otherwise.
    */
   static Error uniqueFilePath(const std::string& in_basePath, const std::string& in_extension, FilePath& out_filePath);

#ifndef _WIN32
   /**
    * @brief Changes the file mode to the specified file mode.
    *
    * @param in_fileModeStr     The posix file mode string. e.g. rwxr-xr-x.
    *
    * @return Success if the file mode could be changed; Error otherwise.
    */
   Error changeFileMode(const std::string& in_fileModeStr) const;

   /**
    * @brief Changes the file mode to the specified file mode.
    *
    * @param in_fileMode        The new file mode.
    * @param in_setStickyBit    Whether to set the sticky bit on this file.
    *
    * @return Success if the file mode could be changed; Error otherwise.
    */
   Error changeFileMode(FileMode in_fileMode, bool in_setStickyBit = false) const;

   /**
    * @brief Changes the ownership of the file or directory to the specified user.
    *
    * @param in_newUser         The user who should own the file.
    * @param in_recursive       If this FilePath is a directory, whether to recursively change ownership on all files
    *                           and directories within this directory.
    * @param in_shouldChown     A recursive iteration function which allows the caller to filter files and directories.
    *                           If a file or directory should have its ownership changed, this function should return
    *                           true.
    *
    * @return Success if the file, and optionally all nested files and directories, had their ownership changed; Error
    *         otherwise.
    */
   Error changeOwnership(
      const system::User& in_newUser,
      bool in_recursive = false,
      const RecursiveIterationFunction& in_shouldChown = RecursiveIterationFunction()) const;
#endif

   /**
    * @brief Gets the provided relative path as a child of this path.
    *
    * @param in_filePath    The path to get as a child of this path. Must be a relative path.
    *
    * @return The completed child path, or this path if the provided path was not relative or another error occurred.
    */
   FilePath completeChildPath(const std::string& in_filePath) const;

   /**
    * @brief Gets the provided relative path as a child of this path.
    *
    * @param in_filePath        The path to get as a child of this path. Must be a relative path
    *                           that refers to a path strictly within this one (i.e. ".." isn't
    *                           allowed)
    * @param out_childPath      The completed child path. Not valid if an error is returned.
    *
    * @return Success if the child path could be completed; Error otherwise.
    */
   Error completeChildPath(const std::string& in_filePath, FilePath& out_childPath) const;

   /**
    * @brief Completes the provided path relative to this path. If the provided path is not relative, it will be
    *        returned as is. Relative paths such as ".." are permitted.
    *
    * @param in_filePath    in_filePathThe path to complete.
    *
    * @return The completed path if the provided path was relative, or the provided path if it was not relative.
    */
   FilePath completePath(const std::string& in_filePath) const;

   /**
    * @brief Copies this file path to the specified location.
    *
    * @param in_targetPath      The location to copy this file path to.
    * @param overwrite          Whether to overwrite the file if one exists in target path.
    *
    * @return Success if the copy could be completed; Error otherwise.
    */
   Error copy(const FilePath& in_targetPath, bool overwrite = false) const;

   /**
    * @brief Copies this directory recursively to the specified location.
    *
    * @param in_targetPath      The location to which to copy this directory and its contents.
    * @param overwrite          Whether to overwrite the file if one exists in target path.
    *
    * @return Success if the copy could be completed; Error otherwise.
    */
   Error copyDirectoryRecursive(const FilePath& in_targetPath, bool overwrite = false) const;

   /**
    * @brief Creates the specified directory.
    *
    * @param in_filePath    The directory to create, relative to this directory.
    *
    * @return Success if the directory could be created; Error if it could not be created for any reason.
    */
   Error createDirectory(const std::string& in_filePath) const;

   /**
    * @brief Creates this directory, if it does not exist.
    *
    * @return Success if the directory could be created or it exists already; Error otherwise.
    */
   Error ensureDirectory() const;

   /**
    * @brief Creates this file, if it does not exist.
    *
    * @return Success if the file could be created or it exists already; Error otherwise.
    */
   Error ensureFile() const;

   /**
    * @brief Checks whether this file path exists in the file system.
    *
    * @return True if this file path exists; false otherwise.
    */
   bool exists() const;

   /**
    * @brief Gets the full absolute representation of this file path.
    *
    * @return The absolute representation of this file path.
    */
   std::string getAbsolutePath() const;

   /**
    * @brief Gets the full absolute representation of this file path in native format.
    *
    * @return The absolute representation of this file path in native format.
    */
   std::string getAbsolutePathNative() const;

#ifdef _WIN32
   std::wstring getAbsolutePathW() const;
#endif

   /**
    * @brief Gets the canonical representation of this file path. The path must exist so that its
    *    canonical location on disk can be obtained.
    *
    * @return The canonical representation of this file path.
    */
   std::string getCanonicalPath() const;

   /**
    * @brief Gets the children of this directory. Sub-directories will not be traversed.
    *
    * @param out_filePaths      The children of this directory.
    *
    * @return Success if the children could be retrieved; Error otherwise (e.g. if this path does not exist).
    */
   Error getChildren(std::vector<FilePath>& out_filePaths) const;

   /**
    * @brief Gets the children of this directory recursively. Sub-directories will be traversed.
    *
    * @param in_iterationFunction   The function to perform for each child of this directory.
    *
    * @return Success if the children could be iterated; Error otherwise (e.g. if tis path does not exist).
    */
   Error getChildrenRecursive(const RecursiveIterationFunction& in_iterationFunction) const;

   /**
    * @brief Gets the extension of the file, including the leading '.'.
    *
    * @return The extension of the file.
    */
   std::string getExtension() const;

   /**
    * @brief Gets the extension of the file in lower case, including the leading '.'.
    *
    * @return The extension of the file in lower case.
    */
   std::string getExtensionLowerCase() const;

#ifndef _WIN32
   /**
    * @brief Gets the posix file mode of this file or directory.
    *
    * @param out_fileMode   The file mode of this file or directory. Invalid if an error is returned.
    *
    * @return Success if the file mode could be retrieved; Error otherwise.
    */
   Error getFileMode(FileMode& out_fileMode) const;
#endif

   /**
    * @brief Gets only the name of the file, including the extension.
    *
    * @return The name of the file, including the extension.
    */
   std::string getFilename() const;

   /**
    * @brief Get the last time this file path was written.
    *
    * @return The time of the last write.
    */
   std::time_t getLastWriteTime() const;

   /**
    * @brief Gets the lexically normal representation of this file path, with . and ..
    *    components resolved and/or removed.
    *
    * @return The lexically normal representation of this file path. 
    */
   std::string getLexicallyNormalPath() const;

   /**
    * @brief Gets the mime content type of this file.
    *
    * @param in_defaultType     The default mime content type to return if this file does not have a mime content type.
    *                           Default: "text/plain".
    *
    * @return The mime content type of this file, or the default type if the file does not have a mime content type.
    */
   std::string getMimeContentType(const std::string& in_defaultType = "text/plain") const;

   /**
    * @brief Gets the parent directory of this file path.
    *
    * @return The parent directory of this file path.
    */
   FilePath getParent() const;

   /**
    * @brief Gets the representation of this path, relative to the provided path.
    *
    * @param in_parentPath      The parent of this path.
    *
    * @return The representation of this path, relative to the provided parent, or empty if this path is not within the
    *         provided parent.
    */
   std::string getRelativePath(const FilePath& in_parentPath) const;

   /**
    * @brief Gets the size of this file path in bytes.
    *
    * @return The size of this file path in bytes.
    */
   uintmax_t getSize() const;

   /**
    * @brief Gets the size of this file path and all sub-directories and files in it, in bytes.
    *
    * @return The size of this file path and all sub-directories and files in it, in bytes.
    */
   uintmax_t getSizeRecursive() const;

   /**
    * @brief Gets only the name of the file, excluding the extension.
    *
    * @return The name of the file, excluding the extension.
    */
   std::string getStem() const;

   /**
    * @brief Checks whether this file has the specified extension.
    *
    * @param in_extension   The extension to check this file for.
    *
    * @return True if the extension of this file matches the specified extension; false otherwise.
    */
   bool hasExtension(const std::string& in_extension) const;

   /**
    * @brief Checks whether this file has the specified extension when it is converted to lower case.
    *
    * @param in_extension   The extension to check this file for.
    *
    * @return True if the lower case extension of this file matches the specified extension; false otherwise.
    */
   bool hasExtensionLowerCase(const std::string& in_extension) const;

   /**
    * @brief Checks whether this file has a text mime content type.
    *
    * @return True if this file has a text mime content type; false otherwise.
    */
   bool hasTextMimeType() const;

   /**
    * @brief Checks whether this file path is a directory.
    *
    * @return True if this file path is a directory; false otherwise.
    */
   bool isDirectory() const;

   /**
    * @brief Checks whether this file path contains a path or not.
    *
    * @return True if this file path does not contain a path; false otherwise.
    */
   bool isEmpty() const;

   /**
    * @brief Checks whether this file path points to the same location in the filesystem as the specified file path.
    *
    * @param in_other   The file path to which to compare this file path to.
    *
    * @return True if this file path points to the same location in the filesystem as the specified file path; false
    *         otherwise.
    */
   bool isEquivalentTo(const FilePath& in_other) const;

   /**
    * @brief Checks whether this file path is a hidden file or directory.
    *
    * @return True if this file path is a hidden file or directory; false otherwise.
    */
   bool isHidden() const;

   /**
    * @brief Checks whether this file path is a Windows junction.
    *
    * @return True if this file path is a Windows junction; false otherwise.
    */
   bool isJunction() const;

#ifndef _WIN32

   /**
    * @brief Checks whether this file path is readable.
    *
    * @param out_readable       True if this file path is readable by the current effective user; false if it is not.
    *                           Invalid if this method returns an error.
    *
    * @return Success if the readability of this file could be checked; Error otherwise. (e.g. EACCES).
    */
   Error isReadable(bool& out_readable) const;

#endif

   /**
    * @brief Checks whether this file path is a regular file.
    *
    * @return True if this file path is a regular file; false otherwise.
    */
   bool isRegularFile() const;

   /**
    * @brief Checks whether this file path is a symbolic link.
    *
    * @return True if this file path is a symbolic link; false otherwise.
    */
   bool isSymlink() const;

   /**
    * @brief Checks whether this file path is within the specified file path.
    *
    * @param in_scopePath   The potential parent path.
    *
    * @return True if this file path is within the specified path, or if the two paths are equal; false otherwise.
    */
   bool isWithin(const FilePath& in_scopePath) const;

#ifndef _WIN32

   /**
    * @brief Checks whether this file path is writeable.
    *
    * @param out_writeable      True if this file path is writeable by the current effective user; false if it is not.
    *                           Invalid if this method returns an error.
    *
    * @return Success if the writeability of this file could be checked; Error otherwise. (e.g. EACCES).
    */
   Error isWriteable(bool& out_writeable) const;

#endif

   /**
    * @brief Changes the current working directory to location represented by this file path.
    *
    * @param in_autoCreate       Controls whether to create the location represented by this file path if it does not
    *                            exist. Default: false.
    *
    * @return Success if the working directory was changed; Error otherwise.
    */
   Error makeCurrentPath(bool in_autoCreate = false) const;

   /**
    * @brief Moves the current directory to the specified directory.
    *
    * @param in_targetPath      The location to which to move this directory.
    * @param in_type            The type of move to perform, direct or cross device. See MoveType for more details.
    *                           Default: MoveCrossDevice.
    * @param overwrite          Whether to overwrite the file if one exists in target path.
    *
    * @return Success if this directory could be moved to the target; Error otherwise.
    */
   Error move(const FilePath& in_targetPath, MoveType in_type = MoveCrossDevice, bool overwrite = false) const;

   /**
    * @brief Performs an indirect move by copying this directory to the target and then deleting this directory.
    *
    * @param in_targetPath     The location to which to move this directory.
    * @param overwrite          Whether to overwrite the file if one exists in target path.
    *
    * @return Success if this directory could be moved to the target; Error otherwise.
    */
   Error moveIndirect(const FilePath& in_targetPath, bool overwrite = false) const;

   /**
    * @brief Opens this file for read.
    *
    * @param out_stream     The input stream for this open file.
    *
    * @return Success if the file was opened; system error otherwise (e.g. EPERM, ENOENT, etc.)
    */
   Error openForRead(std::shared_ptr<std::istream>& out_stream) const;

   /**
    * @brief Opens this file for write.
    *
    * @param out_stream     The output stream for this open file.
    * @param in_truncate    Whether to truncate the existing contents of the file. Default: true.
    *
    * @return Success if the file was opened; system error otherwise (e.g. EPERM, ENOENT, etc.)
    */
   Error openForWrite(std::shared_ptr<std::ostream>& out_stream, bool in_truncate = true) const;

   /**
    * @brief Removes this file or directory from the filesystem.
    *
    * @return Success if the file or directory was removed; Error otherwise.
    */
   Error remove() const;

   /**
    * @brief Removes this file or directory from the filesystem, if it exists.
    *
    * @return Success if the file or directory was removed, or if the file did not exist; Error otherwise.
    */
   Error removeIfExists() const;

   /**
    * @brief Removes the directory represented by this FilePath, if it exists, and recreates it.
    *
    * @return Success if the directory was able to be created freshly; Error otherwise.
    */
   Error resetDirectory() const;

   /**
    * @brief Resolves this symbolic link to the location to which it is pointing. If this FilePath is not a symbolic
    *        link, the original FilePath is returned.
    *
    * @return The resolved symbolic link, or this path if it is not a symbolic link.
    */
   FilePath resolveSymlink() const;

   /**
    * @brief Sets the last time that this file was modified to the specified time.
    *
    * @param in_time    The time to which to set the last write time of this file. Default: now.
    */
   void setLastWriteTime(std::time_t in_time = ::time(nullptr)) const;

   /**
    * @brief Checks if a file can be written to by opening the file.
    *
    * To be successful, the file must already exist on the system.
    * If write access is not absolutely necessary, use isWriteable instead.
    *
    * @return Success if file can be written to; system error otherwise (e.g. EPERM, ENOENT, etc.)
    */
   Error testWritePermissions() const;

private:
   // The private implementation of FilePath.
   PRIVATE_IMPL_SHARED(m_impl);
};

/**
 * @brief Forward declaration for the implementation of RAII path scope classes.
 */
struct PathScopeImpl;

/**
 * @brief Struct which implements the deleter for PathScopeImpl.
 */
struct PathScopeImplDeleter
{
   /**
    * @brief Deletion operator.
    */
   void operator()(PathScopeImpl*);
};

/**
 * @brief RAII class for restoring the current working directory.
 */
class RestoreCurrentPathScope : boost::noncopyable
{
public:
   /**
    * @brief Constructor.
    *
    * @param in_restorePath     The path to which to restore the current working directory on destruction of this
    *                           object.
    * @param in_location        The location where this object was constructed, for logging purposes.
    */
   RestoreCurrentPathScope(FilePath in_restorePath, ErrorLocation in_location);

   /**
    * @brief Destructor. Returns the working directory to the original path.
    */
   virtual ~RestoreCurrentPathScope();

private:
   // The private implementation of RestoreCurrentPathScope
   std::unique_ptr<PathScopeImpl, PathScopeImplDeleter> m_impl;
};

/**
 * @brief RAII class for restoring the current working directory.
 */
class RemoveOnExitScope : boost::noncopyable
{
public:
   /**
    * @brief Constructor.
    *
    * @param in_restorePath     The path to which to restore the current working directory on destruction of this
    *                           object.
    * @param in_location        The location where this object was constructed, for logging purposes.
    */
   RemoveOnExitScope(FilePath in_restorePath, ErrorLocation in_location);

   /**
    * @brief Destructor. Removes the path that was provided in the constructor from the filesystem.
    */
   virtual ~RemoveOnExitScope();

private:
   // The private implementation of RemoveOnExitScope
   std::unique_ptr<PathScopeImpl, PathScopeImplDeleter> m_impl;
};

/**
 * @brief Output stream operator for FilePath objects.
 *
 * @param io_ostream         The output stream to which to write the FilePath.
 * @param in_filePath       The FilePath to write.
 *
 * @return A reference to io_stream.
 */
std::ostream& operator<<(std::ostream& io_ostream, const FilePath& in_filePath);

/**
 * @brief Error creation function to be used when a file or directory exists but it should not.
 *
 * @param in_location   The location at which the error occurred.
 *
 * @return The "file exists" error.
 */
Error fileExistsError(const ErrorLocation& in_location);

/**
 * @brief Error creation function to be used when a file or directory exists but it should not.
 *
 * @param in_filePath   The file or directory which already existed.
 * @param in_location   The location at which the error occurred.
 *
 * @return The "file exists" error.
 */
Error fileExistsError(const FilePath& in_filePath, const ErrorLocation& in_location);

/**
 * @brief Checks whether the provided error is a "file not found" error.
 *
 * @param in_error      The error to check.
 *
 * @return True if the specified error is a file not found error; false otherwise.
 */
bool isFileNotFoundError(const Error& in_error);

/**
 * @brief Error creation function to be used when a file could not be found.
 *
 * @param in_location   The location at which the error occurred.
 *
 * @return The "file not found" error.
 */
Error fileNotFoundError(const ErrorLocation& in_location);

/**
 * @brief Error creation function to be used when a file could not be found.
 *
 * @param in_filePath   The file which could not be found.
 * @param in_location   The location at which the error occurred.
 *
 * @return The "file not found" error.
 */
Error fileNotFoundError(const std::string& in_filePath, const ErrorLocation& in_location);

/**
 * @brief Error creation function to be used when a file could not be found.
 *
 * @param in_filePath   The file which could not be found.
 * @param in_location   The location at which the error occurred.
 *
 * @return The "file not found" error.
 */
Error fileNotFoundError(const FilePath& in_filePath, const ErrorLocation& in_location);

/**
 * @brief Checks whether the provided error is a "file not found" error.
 *
 * @param in_error      The error to check.
 *
 * @return True if the specified error is a file not found error; false otherwise.
 */
bool isPathNotFoundError(const Error& error);

/**
 * @brief Error creation function to be used when a directory could not be found.
 *
 * @param in_location   The location at which the error occurred.
 *
 * @return The "path not found" error.
 */
Error pathNotFoundError(const ErrorLocation& in_location);

/**
 * @brief Error creation function to be used when a directory could not be found.
 *
 * @param in_filePath   The directory which could not be found.
 * @param in_location   The location at which the error occurred.
 *
 * @return The "path not found" error.
 */
Error pathNotFoundError(const std::string& in_filePath, const ErrorLocation& in_location);

/**
 * @brief Checks whether the error is either a file not found error or a path not found error.
 *
 * @param in_error      The error to check.
 *
 * @return True if the specified error is a file not found error or a path not found error.
 */
bool isNotFoundError(const Error& in_error);

} // namespace core
} // namespace rstudio

#endif
