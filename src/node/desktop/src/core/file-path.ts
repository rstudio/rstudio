/*
 * file-path.ts
 *
 * Copyright (C) 2021 by RStudio, PBC
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

import fs from 'fs';
import fsPromises from 'fs/promises';

import path from 'path';
import { Err, Success } from './err';
import { User } from './user';


export interface FilePathWithError {
  err?: Err;
  path: FilePath;
}

/**
 * Class representing a path on the system. May be any type of file (e.g. directory, symlink,
 * regular file, etc.)
 */
export class FilePath {
  private path: string;
  static homePathAlias = '~/';
  static homePathLeafAlias = '~';

  constructor(path = '') {
    this.path = path;
  }

  /**
   * Get string representation of object, for debugging purposes
   */
  toString(): string {
    return this.getAbsolutePath();
  }

  /**
   * Compare this object with another; returns true if they refer to the same
   * path (exact match).
   */
  equals(filePath: FilePath): boolean {
    return FilePath.boost_fs_path2str(this.path) == FilePath.boost_fs_path2str(filePath.path);
  }

  /**
   * Creates a path in which the user home path will be replaced by the ~ alias.
   */
  static createAliasedPath(filePath: FilePath, userHomePath: FilePath): string {
    throw Error('createAliasedPath is NYI');
  }

  /**
   * Checks whether the specified path exists.
   */
  static existsSync(filePath: string): boolean {
    if (!filePath) {
      return false;
    }

    const p = filePath;
    try {
      return fs.existsSync(p);
    } catch (err) {
      FilePath.logErrorWithPath(p, err);
      return false;
    }
  }

  /**
   * Checks whether the specified path exists.
   */
  static async exists(filePath: string): Promise<boolean> {
    if (!filePath) {
      return false;
    }

    const p = filePath;
    try {
      await fsPromises.access(p);
      return true;
    } catch (err) {
      FilePath.logErrorWithPath(p, err);
      return false;
    }
  }

  /**
   * Checks whether the two provided files are equal, ignoring case. Two files are equal 
   * if their absolute paths are equal.
   */
  static isEqualCaseInsensitive(filePath1: FilePath, filePath2: FilePath): boolean {
    throw Error('isEqualCaseInsensitive is NYI');
  }

  /**
   * Checks whether the specified path is a root path or a relative path.
   */
  static isRootPath(filePath: string): boolean {
    throw Error('isRootPath is NYI');
  }

  /**
   * Changes the current working directory to the specified path.
   */
  static makeCurrent(filePath: string): Err {
    throw Error('makeCurrent (static) is NYI');
  }

  /**
   * Resolves the '~' alias within the path to the user's home path.
   */
  static resolveAliasedPathSync(aliasedPath: string, userHomePath: FilePath): FilePath {
    // Special case for empty string or "~"
    if (!aliasedPath || aliasedPath == this.homePathLeafAlias) {
      return userHomePath;
    }

    // if the path starts with the home alias then substitute the home path
    if (aliasedPath.startsWith(this.homePathAlias)) {
      return new FilePath(path.join(userHomePath.getAbsolutePath(), aliasedPath.substr(1)));
    } else {
      // no aliasing, this is either an absolute path or path
      // relative to the current directory
      return FilePath.safeCurrentPathSync(userHomePath).completePath(aliasedPath);
    }
  }

  /**
   * Checks whether the current working directory exists. If it does not, moves the
   * current working directory to the provided path and returns the new current working
   * directory.
   */
  static safeCurrentPathSync(revertToPath: FilePath): FilePath {
    try {
      return new FilePath(process.cwd());
    } catch (err) {
      FilePath.logError(err);
    }

    // revert to the specified path if it exists, otherwise
    // take the user home path from the system
    let safePath = revertToPath;
    if (!fs.existsSync(safePath.path)) {
      safePath = User.getUserHomePath();
    }

    const error = safePath.makeCurrentPath();
    if (error) {
      FilePath.logError(error);
    }

    return safePath;
  }

  /**
   * Checks whether the current working directory exists. If it does not, moves the
   * current working directory to the provided path and returns the new current working
   * directory.
   */
  static async safeCurrentPath(revertToPath: FilePath): Promise<FilePath> {
    try {
      return new FilePath(process.cwd());
    } catch (err) {
      FilePath.logError(err);
    }

    // revert to the specified path if it exists, otherwise
    // take the user home path from the system
    let safePath = revertToPath;
    if (! await FilePath.exists(safePath.path)) {
      safePath = User.getUserHomePath();
    }

    const error = safePath.makeCurrentPath();
    if (error) {
      FilePath.logError(error);
    }

    return safePath;
  }

  /**
   * Creates a randomly named file in the temp directory.
   */
  static tempFilePath(extension?: string): FilePathWithError {
    throw Error('tempFilePath is NYI');
  }

  /**
   * Creates a file with a random name in the specified directory.
   */
  static uniqueFilePath(basePath: string, extension?: string): FilePathWithError {
    throw Error('uniqueFilePath is NYI');
  }

  /**
   * Changes the file mode to the specified file mode (Posix-only). Pass in the posix file mode
   * string, e.g. rwxr-xr-x
   */
  changeFileMode(fileModeStr: string, setStickyBit = false): Err {
    if (process.platform === 'win32')
      return Success(); // no-op on Windows

    throw Error('changeFileMode is NYI');
  }

  /**
   * Changes the ownership of the file or directory to the specified user (Posix-only).
   */
  // changeOwnership(
  //   newUser: User,
  //   recursive = false,
  //   const RecursiveIterationFunction& in_shouldChown = RecursiveIterationFunction()
  // ) {
  //   throw Error("changeOwnership is NYI");
  // }

  /**
   * Gets the provided relative path as a child of this path.
   *
   * `filePath` is the path to get as a child of this path. Must be a relative path that
   * refers to a path strictly within this one (i.e. ".." isn't allowed).
   */
  completeChildPath(filePath: string): FilePath {
    const result = this.completeChildPathWithErrorResult(filePath);
    if (result.err) {
      FilePath.logError(result.err);
    }
    return result.path;
  }

  /**
   * Gets the provided relative path as a child of this path. Returns both the
   * path and any error details.
   *
   * `filePath` is the path to get as a child of this path. Must be a relative path that
   * refers to a path strictly within this one (i.e. ".." isn't allowed).
   */
  completeChildPathWithErrorResult(filePath: string): FilePathWithError {
    try {
      if (!filePath) {
        return { path: this };
      }

      // confirm this is a relative path
      const relativePath = filePath;
      if (path.isAbsolute(relativePath)) {
        throw Error('absolute path not permitted');
      }

      const childPath = this.completePath(filePath);

      if (!childPath.isWithin(this)) {
        return { err: new Error('child path must be inside parent path'), path: this };
      }

      return { path: childPath };
    } catch (e) {
      return { err: e, path: this };
    }
  }

  /**
   * Completes the provided path relative to this path. If the provided path is not relative,
   * it will be returned as is. Relative paths such as ".." are permitted.
   */
  completePath(filePath: string): FilePath {
    try {
      return new FilePath(FilePath.boost_fs_path2str(FilePath.fs_complete(filePath, this.path)));
    } catch (err) {
      FilePath.logError(err);
      return this;
    }
  }
  /**
   * Copies this file path to the specified location.
   */
  copy(targetPath: FilePath, overwrite = false): Err {
    throw Error('copy is NYI');
  }

  /**
   * Copies this directory recursively to the specified location.
   */
  copyDirectoryRecursive(targetPath: FilePath, overwrite = false): Err {
    throw Error('copyDirectoryRecursive is NYI');
  }

  /**
   * Creates the specified directory, relative to this directory.
   */
  createDirectorySync(filePath = ''): Err {
    let targetDirectory: string;
    if (!filePath) {
      targetDirectory = this.path;
    } else {
      targetDirectory = FilePath.fs_complete(filePath, this.path);
    }

    try {
      fs.mkdirSync(targetDirectory, { recursive: true });
    } catch (err) {
      return err;
    }
    return Success();
  }

  /**
   * Creates the specified directory, relative to this directory.
   */
  async createDirectory(filePath = ''): Promise<Err> {
    let targetDirectory: string;
    if (!filePath) {
      targetDirectory = this.path;
    } else {
      targetDirectory = FilePath.fs_complete(filePath, this.path);
    }

    try {
      await fsPromises.mkdir(targetDirectory, { recursive: true });
    } catch (err) {
      return err;
    }
    return Success();
  }

  /**
   * Creates this directory, if it does not exist.
   */
  ensureDirectorySync(): Err {
    if (!this.existsSync()) {
      return this.createDirectorySync();
    } else {
      return Success();
    }
  }

  /**
   * Creates this directory, if it does not exist.
   */
  async ensureDirectory(): Promise<Err> {
    if (!await this.exists()) {
      return await this.createDirectory();
    } else {
      return Success();
    }
  }

  /**
   * Creates this file, if it does not exist.
   */
  ensureFile(): Err {
    throw Error('ensureFile is NYI');
  }

  /**
   * Checks whether this file path exists in the file system.
   */
  existsSync(): boolean {
    try {
      return !this.isEmpty() && fs.existsSync(this.path);
    } catch (err) {
      FilePath.logErrorWithPath(this.path, err);
      return false;
    }
  }

  /**
   * Checks whether this file path exists in the file system.
   */
  async exists(): Promise<boolean> {
    try {
      if (this.isEmpty()) {
        return false;
      }
      await fsPromises.access(this.path);
      return true;
    } catch (err) {
      FilePath.logErrorWithPath(this.path, err);
      return false;
    }
  }

  /**
   * Gets the full absolute representation of this file path.
   */
  getAbsolutePath(): string {
    return FilePath.boost_fs_path2str(this.path);
  }

  /**
   * Gets the full absolute representation of this file path in native format.
   */
  getAbsolutePathNative(): string {
    return FilePath.boost_fs_path2strnative(this.path);
  }

  /**
   * Gets the canonical representation of this file path. The path must exist so that its
   * canonical location on disk can be obtained.
   */
  getCanonicalPathSync(): string {
    if (this.isEmpty()) {
      return '';
    }

    try {
      return FilePath.boost_fs_path2str(fs.realpathSync(this.path));
    }
    catch (err) {
      FilePath.logErrorWithPath(this.path, err);
    }
    return '';
  }

  /**
   * Gets the canonical representation of this file path. The path must exist so that its
   * canonical location on disk can be obtained.
   */
  async getCanonicalPath(): Promise<string> {
    if (this.isEmpty()) {
      return '';
    }

    try {
      return FilePath.boost_fs_path2str(await fsPromises.realpath(this.path));
    }
    catch (err) {
      FilePath.logErrorWithPath(this.path, err);
    }
    return '';
  }

  /**
   * Gets the children of this directory. Sub-directories will not be traversed.
   */
  getChildren(filePaths: Array<FilePath>): Err {
    throw Error('getChildren is NYI');
  }

  /**
   * Gets the children of this directory recursively. Sub-directories will be traversed.
   */
  getChildrenRecursive(/*iterationFunction: RecursiveIterationFunction*/): Err {
    throw Error('getChildrenRecursive is NYI');
  }

  /**
   * Gets the extension of the file, including the leading '.'.
   */
  getExtension(): string {
    const components = path.parse(this.path);
    return components.ext;
  }

  /**
   * Gets the extension of the file in lower case, including the leading '.'.
   */
  getExtensionLowerCase(): string {
    const components = path.parse(this.path);
    return components.ext.toLowerCase();
  }

  /**
   * Gets the posix file mode of this file or directory (Posix-only)
   */
  getFileMode(/*fileMode: FileMode*/): Err {
    throw Error('getFileMode is NYI');
  }

  /**
   * Gets only the name of the file, including the extension.
   */
  getFilename(): string {
    const components = path.parse(this.path);
    return components.base;
  }

  /**
   * Get the last time this file path was written.
   */
  getLastWriteTimeSync(): number {
    try {
      const stats = fs.statSync(this.path);
      return stats.mtimeMs;
    }
    catch {
      return 0;
    }
  }

  /**
   * Gets the mime content type of this file.
   */
  getMimeContentType(defaultType = 'text/plain'): string {
    throw Error('getMimeContentType is NYI');
  }

  /**
   * Gets the parent directory of this file path.
   */
  getParent(): FilePath {
    throw Error('getParent is NYI');
  }

  /**
   * Gets the lexically normal representation of this file path, with . and ..
   * components resolved and/or removed.
   */
  getLexicallyNormalPath(): string {
    return FilePath.boost_fs_path2str(path.normalize(this.path));
  }

  /**
   * Gets the representation of this path, relative to the provided path.
   */
  getRelativePath(parentPath: FilePath): string {
    throw Error('getRelativePath is NYI');
  }

  /**
   * Gets the size of this file path in bytes.
   */
  getSize(): number {
    throw Error('getSize is NYI');
  }

  /**
   * Gets the size of this file path and all sub-directories and files in it, in bytes.
   */
  getSizeRecursive(): number {
    throw Error('getSizeRecursive is NYI');
  }

  /**
   * Gets only the name of the file, excluding the extension.
   */
  getStem(): string {
    throw Error('getStem is NYI');
  }

  /**
   * Checks whether this file has the specified extension.
   */
  hasExtension(extension: string): boolean {
    throw Error('hasExtension is NYI');
  }

  /**
   * Checks whether this file has the specified extension when it is converted to lower case.
   */
  hasExtensionLowerCase(extension: string): boolean {
    throw Error('hasExtensionLowerCase is NYI');
  }

  /**
   * Checks whether this file has a text mime content type.
   */
  hasTextMimeType(): boolean {
    throw Error('hasTextMimeType is NYI');
  }

  /**
   * Checks whether this file path is a directory.
   */
  isDirectory(): boolean {
    throw Error('isDirectory is NYI');
  }

  /**
   * Checks whether this file path contains a path or not.
   */
  isEmpty() {
    return !this.path;
  }

  /**
   * Checks whether this file path points to the same location in the filesystem as 
   * the specified file path.
   */
  isEquivalentTo(other: FilePath): boolean {
    throw Error('isEquivalentTo is NYI');
  }

  /**
   * Checks whether this file path is a hidden file or directory.
   */
  isHidden(): boolean {
    throw Error('isHidden is NYI');
  }

  /**
   * @brief Checks whether this file path is a Windows junction.
   *
   * @return True if this file path is a Windows junction; false otherwise.
   */
  isJunction(): boolean {
    throw Error('isJunction is NYI');
  }

  /**
   * @brief Checks whether this file path is readable.
   *
   * @param out_readable       True if this file path is readable by the current effective user; false if it is not.
   *                           Invalid if this method returns an error.
   *
   * @return Success if the readability of this file could be checked; Error otherwise. (e.g. EACCES).
   */
  isReadable(): Error | boolean {
    throw Error('isReadable is NYI');
  }

  /**
   * Checks whether this file path is a regular file.
   */
  isRegularFile(): boolean {
    throw Error('isRegularFile is NYI');
  }

  /**
   * Checks whether this file path is a symbolic link.
   */
  isSymlink(): boolean {
    throw Error('isSymLink is NYI');
  }

  /**
   * Checks whether this file path is within the specified file path.
   *
   * `scopePath` The potential parent path.
   *
   *  Returns `true` if this file path is within the specified path,
   *  or if the two paths are equal; false otherwise.
   */
  isWithin(scopePath: FilePath): boolean {
    // Technically, we contain ourselves.
    if (this.equals(scopePath)) {
      return true;
    }

    // Make the paths lexically normal so that e.g. foo/../bar isn't considered a child of foo.
    const child = new FilePath(this.getLexicallyNormalPath());
    const parent = new FilePath(scopePath.getLexicallyNormalPath());

    // Easy test: We can't possibly be in this scope path if it has more components than we do
    if (parent.path.length > child.path.length) {
      return false;
    }

    const childDetail = path.parse(child.path);
    const parentDetail = path.parse(parent.path);
    if (childDetail.root !== parentDetail.root) {
      return false;
    }

    // Find the first path element that differs. Stop when we reach the end of the parent
    // path, or a "." path component, which signifies the end of a directory (/foo/bar/.)
    const childDirs = childDetail.dir.split(/[/\\]/);
    const parentDirs = parentDetail.dir.split(/[/\\]/);
    childDirs.push(childDetail.base);
    parentDirs.push(parentDetail.base);
    for (let i = 0; i < parentDirs.length; i++) {
      if (parentDirs[i] === '.') {
        break;
      }
      if (parentDirs[i] !== childDirs[i]) {
        return false;
      }
    }

    // No differing path element found
    return true;
  }

  /**
   * Checks whether this file path is writeable.
   */
  isWriteable(): boolean | Error {
    throw Error('isWriteable is NYI');
  }

  /**
   * Changes the current working directory to location represented by this file path.
   */
  makeCurrentPath(autoCreate = false): Err {
    if (autoCreate) {
      const autoCreateError = this.ensureDirectorySync();
      if (autoCreateError) return autoCreateError;
    }

    try {
      process.chdir(this.path);
      return Success();
    } catch (err) {
      return err;
    }
  }

  /**
   * Moves the current directory to the specified directory.
   */
  move(/*targetPath: FilePath, type: MoveType = MoveCrossDevice, overwrite = false*/): Err {
    throw Error('move is NYI');
  }

  /**
   * Performs an indirect move by copying this directory to the target and then deleting this directory.
   */
  moveIndirect(targetPath: FilePath, overwrite = false): Err {
    throw Error('moveIndirect is NYI');
  }

  /**
   * Opens this file for read.
   */
  openForRead(/*std:: shared_ptr<std:: istream>& out_stream*/): Err {
    throw Error('openForRead is NYI');
  }

  /**
   * Opens this file for write.
   */
  openForWrite(/*std:: shared_ptr<std:: ostream>& out_stream,*/ truncate = true): Err {
    throw Error('openForWrite is NYI');
  }

  /**
   * Removes this file or directory from the filesystem.
   */
  remove(): Err {
    throw Error('remove is NYI');
  }

  /**
   * Removes this file or directory from the filesystem, if it exists.
   */
  removeIfExists(): Err {
    throw Error('removeIfExists is NYI');
  }

  /**
   * Removes the directory represented by this FilePath, if it exists, and recreates it.
   */
  resetDirectory(): Err {
    throw Error('resetDirectory is NYI');
  }

  /**
   * Resolves this symbolic link to the location to which it is pointing. If this FilePath
   * is not a symbolic link, the original FilePath is returned.
   */
  resolveSymlink(): FilePath {
    throw Error('resolveSymlink is NYI');
  }

  /**
   * Sets the last time that this file was modified to the specified time.
   */
  setLastWriteTime(/*std::time_t in_time = ::time(nullptr)*/): void {
    throw Error('setLastWriteTime is NYI');
  }

  /**
   * Checks if a file can be written to by opening the file.
   */
  testWritePermissions(): Err {
    throw Error('testWritePermissions is NYI');
  }

  // -------------------------
  // Internal helper functions
  // -------------------------

  static logErrorWithPath(path: string, error: Error): void {
    // TODO logging
    // console.error(error.message + ": " + path);
  }

  static logError(error: Error): void {
    // TODO logging
    // console.error(error.message);
  }

  /**
   * Return generic form of stored path (akin to boost's path.generic_string method)
   */
  static generic_string(p: string): string {
    if (process.platform !== 'win32') {
      return p;
    } else {
      return p.split(path.sep).join(path.posix.sep);
    }
  }

  // Analogous to BOOST_FS_COMPLETE in FilePath.cpp
  static fs_complete(p: string, base: string): string {
    return path.resolve(base, p);
  }

  static boost_fs_path2str(p: string): string {
    return FilePath.generic_string(p);
  }

  static boost_fs_path2strnative(p: string): string {
    if (process.platform === 'win32') {
      return p;
    } else {
      return FilePath.generic_string(p);
    }
  }
}
