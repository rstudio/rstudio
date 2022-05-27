/*
 * file-path.ts
 *
 * Copyright (C) 2022 by RStudio, PBC
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

import { logger } from './logger';
import path, { sep } from 'path';
import { Err, success, safeError } from './err';
import { userHomePath } from './user';
import { err, Expected, ok } from './expected';

/** An Error containing 'path' that triggered the error */
export class FilePathError extends Error {
  path: string;
  constructor(message: string, path: string) {
    super(message);
    this.path = path;
  }
}

const homePathAlias = '~/';
const homePathLeafAlias = '~';

/**
 * Normalize separators of a given path.
 *
 * @param {string} path
 * @param {string} [separator='/']
 * @return {*} 
 */
function normalizeSeparators(path: string, separator = '/') {
  return path.replace(/[\\]/g, separator);
}

/**
 * Normalizes separatos of a given path based on the current platform.
 *
 * @export
 * @param {string} path
 * @return {*} 
 */
export function normalizeSeparatorsNative(path: string) {
  return normalizeSeparators(path, sep);
}

/**
 * Class representing a path on the system. May be any type of file (e.g. directory, symlink,
 * regular file, etc.)
 */
export class FilePath {
  constructor(private path: string = '') {}

  /**
   * Get string representation of object, for debugging purposes
   */
  toString(): string {
    return this.getAbsolutePath();
  }

  /**
   * Compare this object with another; returns true if they refer to the same
   * path (exact match).
   *
   * Note that paths which resolve to the same path, but are not stored the
   * same, are considered different -- for example, /a/b and /a/b/../b are
   * not considered identical.
   */
  equals(filePath: FilePath): boolean {
    return normalizeSeparators(this.path) === normalizeSeparators(filePath.path);
  }

  /**
   * Creates a path in which the user home path will be replaced by the ~ alias.
   */
  static createAliasedPath(filePath: FilePath, userHomePath: FilePath): string {
    // first, retrieve and normalize paths
    const file = filePath.getAbsolutePath();
    const home = userHomePath.getAbsolutePath();
    if (file === home) {
      return homePathLeafAlias;
    }

    // try to compute home-relative path -- if that fails,
    // or the computed path does not appear to be relative,
    // then just return the original file path
    const relative = path.relative(home, file);
    if (!relative || path.isAbsolute(relative) || relative.startsWith('..')) {
      return file;
    }

    // we computed a relative path; prefix it with tilde
    const aliased = path.join(homePathLeafAlias, relative);
    return normalizeSeparators(aliased);
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
    } catch (err: unknown) {
      logger().logError(err);
      return false;
    }
  }

  /**
   * Checks whether the specified path exists.
   *
   * Returns Promise<boolean>; do not use without 'await' or * .then().
   *
   * For example, this can give the WRONG result:
   *
   * if (FilePath.existsAync(file)) { WRONG USAGE always true, a Promise is truthy }
   *
   * Use either:
   *
   * if (await FilePath.existsAsync(file)) { ...}
   *
   * or
   *
   * if (FilePath.existsAsync(file).then((result) => { if (result) { ... } }
   */
  static async existsAsync(filePath: string): Promise<boolean> {
    if (!filePath) {
      return false;
    }

    const p = filePath;
    try {
      await fsPromises.access(p);
      return true;
    } catch (err: unknown) {
      logger().logError(err);
      return false;
    }
  }

  /**
   * Checks whether the two provided files are equal, ignoring case. Two files are equal
   * if their absolute paths are equal.
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  static isEqualCaseInsensitive(filePath1: FilePath, filePath2: FilePath): boolean {
    throw Error('isEqualCaseInsensitive is NYI');
  }

  /**
   * Checks whether the specified path is a root path or a relative path.
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  static isRootPath(filePath: string): boolean {
    throw Error('isRootPath is NYI');
  }

  /**
   * Changes the current working directory to the specified path.
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  static makeCurrent(filePath: string): Err {
    throw Error('makeCurrent (static) is NYI');
  }

  /**
   * Resolves the '~' alias within the path to the user's home path.
   */
  static resolveAliasedPathSync(aliasedPath: string, userHomePath: FilePath): FilePath {
    // Special case for empty string or "~"
    if (!aliasedPath || aliasedPath == homePathLeafAlias) {
      return userHomePath;
    }

    // if the path starts with the home alias then substitute the home path
    if (aliasedPath.startsWith(homePathAlias)) {
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
    } catch (err: unknown) {
      logger().logError(err);
    }

    // revert to the specified path if it exists, otherwise
    // take the user home path from the system
    let safePath = revertToPath;
    if (!fs.existsSync(safePath.path)) {
      safePath = userHomePath();
    }

    const error = safePath.makeCurrentPath();
    if (error) {
      logger().logError(error);
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
    } catch (err: unknown) {
      logger().logError(err);
    }

    // revert to the specified path if it exists, otherwise
    // take the user home path from the system
    let safePath = revertToPath;
    if (!(await FilePath.existsAsync(safePath.path))) {
      safePath = userHomePath();
    }

    const error = safePath.makeCurrentPath();
    if (error) {
      logger().logError(error);
    }

    return safePath;
  }

  /**
   * Creates a randomly named file in the temp directory.
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  static tempFilePath(extension?: string): Expected<FilePath> {
    throw Error('tempFilePath is NYI');
  }

  /**
   * Creates a file with a random name in the specified directory.
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  static uniqueFilePath(basePath: string, extension?: string): Expected<FilePath> {
    throw Error('uniqueFilePath is NYI');
  }

  /**
   * Changes the file mode to the specified file mode (Posix-only). Pass in the posix file mode
   * string, e.g. rwxr-xr-x
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  changeFileMode(fileModeStr: string, setStickyBit = false): Err {
    if (process.platform === 'win32') return success(); // no-op on Windows

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
    const [path, error] = this.completeChildPathWithErrorResult(filePath);
    if (error) {
      logger().logError(error);
      return this;
    }
    return path;
  }

  /**
   * Gets the provided relative path as a child of this path. Returns both the
   * path and any error details.
   *
   * `filePath` is the path to get as a child of this path. Must be a relative path that
   * refers to a path strictly within this one (i.e. ".." isn't allowed).
   */
  completeChildPathWithErrorResult(filePath: string): Expected<FilePath> {
    try {
      if (!filePath) {
        return ok(this);
      }

      // confirm this is a relative path
      const relativePath = filePath;
      if (path.isAbsolute(relativePath)) {
        throw Error('absolute path not permitted');
      }

      const childPath = this.completePath(filePath);

      if (!childPath.isWithin(this)) {
        return err(new FilePathError('child path must be inside parent path', this.getAbsolutePath()));
      }

      return ok(childPath);
    } catch (e: unknown) {
      const error = safeError(e);
      return err(new FilePathError(error.message, this.getAbsolutePath()));
    }
  }

  /**
   * Completes the provided path relative to this path. If the provided path is not relative,
   * it will be returned as is. Relative paths such as ".." are permitted.
   */
  completePath(stem: string): FilePath {
    try {
      return new FilePath(normalizeSeparators(path.resolve(this.path, stem)));
    } catch (err: unknown) {
      logger().logError(err);
      return this;
    }
  }
  /**
   * Copies this file path to the specified location.
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  copy(targetPath: FilePath, overwrite = false): Err {
    throw Error('copy is NYI');
  }

  /**
   * Copies this directory recursively to the specified location.
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
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
      targetDirectory = path.resolve(this.path, filePath);
    }

    try {
      fs.mkdirSync(targetDirectory, { recursive: true });
    } catch (err: unknown) {
      return safeError(err);
    }
    return success();
  }

  /**
   * Creates the specified directory, relative to this directory.
   */
  async createDirectory(filePath = ''): Promise<Err> {
    let targetDirectory: string;
    if (!filePath) {
      targetDirectory = this.path;
    } else {
      targetDirectory = path.resolve(this.path, filePath);
    }

    try {
      await fsPromises.mkdir(targetDirectory, { recursive: true });
    } catch (err: unknown) {
      return safeError(err);
    }
    return success();
  }

  /**
   * Creates this directory, if it does not exist.
   */
  ensureDirectorySync(): Err {
    if (!this.existsSync()) {
      return this.createDirectorySync();
    } else {
      return success();
    }
  }

  /**
   * Creates this directory, if it does not exist.
   */
  async ensureDirectory(): Promise<Err> {
    if (!(await this.existsAsync())) {
      return this.createDirectory();
    } else {
      return success();
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
    } catch (err: unknown) {
      logger().logError(err);
      return false;
    }
  }

  /**
   * Checks whether this file path exists in the file system.
   *
   * Returns Promise<boolean>; do not use without 'await' or * .then().
   *
   * For example, this can give the WRONG result:
   *
   * if (file.existsAync()) { WRONG USAGE always true, a Promise is truthy }
   *
   * Use either:
   *
   * if (await file.existsAsync()) { ...}
   *
   * or
   *
   * if (file.existsAsync().then((result) => { if (result) { ... } }
   */
  async existsAsync(): Promise<boolean> {
    try {
      if (this.isEmpty()) {
        return false;
      }
      await fsPromises.access(this.path);
      return true;
    } catch (err: unknown) {
      logger().logError(err);
      return false;
    }
  }

  /**
   * Gets the full absolute representation of this file path.
   */
  getAbsolutePath(): string {
    return normalizeSeparators(this.path);
  }

  /**
   * Gets the full absolute representation of this file path in native format.
   */
  getAbsolutePathNative(): string {
    return normalizeSeparatorsNative(this.path);
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
      return normalizeSeparators(fs.realpathSync(this.path));
    } catch (err: unknown) {
      logger().logError(err);
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
      return await fsPromises.realpath(this.path);
    } catch (err: unknown) {
      logger().logError(err);
    }

    return '';
  }

  /**
   * Gets the children of this directory. Sub-directories will not be traversed.
   */
  getChildren(filePaths: Array<FilePath>): Err {
    if (!this.existsSync()) {
      return new Error(`File not found: ${this.getAbsolutePath()}`);
    }

    let dir: fs.Dir | undefined = undefined;
    try {
      dir = fs.opendirSync(this.getAbsolutePath());
      const files = fs.readdirSync(this.getAbsolutePath());
      for (const file of files) {
        filePaths.push(this.completeChildPath(file));
      }
    } catch (err: unknown) {
      return safeError(err);
    } finally {
      if (dir) {
        dir.closeSync();
      }
    }
    return success();
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
    } catch {
      return 0;
    }
  }

  /**
   * Gets the mime content type of this file.
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  getMimeContentType(defaultType = 'text/plain'): string {
    throw Error('getMimeContentType is NYI');
  }

  /**
   * Gets the parent directory of this file path.
   */
  getParent(): FilePath {
    return new FilePath(path.dirname(this.path));
  }

  /**
   * Gets the lexically normal representation of this file path, with . and ..
   * components resolved and/or removed.
   */
  getLexicallyNormalPath(): string {
    return normalizeSeparators(path.normalize(this.path));
  }

  /**
   * Gets the representation of this path, relative to the provided path.
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
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
    // If this is just a path to a directory, then there is no filename or stem.
    if (this.path.endsWith('/') || this.path.endsWith('\\')) {
      return '';
    }

    const components = path.parse(this.path);
    return components.name;
  }

  /**
   * Checks whether this file has the specified extension.
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  hasExtension(extension: string): boolean {
    throw Error('hasExtension is NYI');
  }

  /**
   * Checks whether this file has the specified extension when it is converted to lower case.
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
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
    const stat = fs.lstatSync(this.path, {
      throwIfNoEntry: false,
    });

    return stat != null && stat.isDirectory();
  }

  /**
   * Checks whether this file path contains a path or not.
   */
  isEmpty(): boolean {
    return !this.path;
  }

  /**
   * Checks whether this file path points to the same location in the filesystem as
   * the specified file path.
   */
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
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

    // Try to resolve scopePath within our parent
    const parent = path.resolve(scopePath.path);
    const child = path.resolve(this.path);

    // Form relative path.
    const relative = path.relative(parent, child);
    if (!relative) {
      return false;
    }

    return !relative.startsWith('..') && !path.isAbsolute(relative);
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
      return success();
    } catch (err: unknown) {
      return safeError(err);
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
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
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
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  openForWrite(/*std:: shared_ptr<std:: ostream>& out_stream,*/ truncate = true): Err {
    throw Error('openForWrite is NYI');
  }

  /**
   * Removes this file or directory from the filesystem.
   */
  async remove(): Promise<Err> {
    try {
      await fsPromises.rm(this.path, { recursive: true });
    } catch (err: unknown) {
      return safeError(err);
    }
    return success();
  }

  /**
   * Removes this file or directory from the filesystem, if it exists.
   */
  async removeIfExists(): Promise<Err> {
    try {
      await fsPromises.rm(this.path, { force: true, recursive: true });
    } catch (err: unknown) {
      return safeError(err);
    }
    return success();
  }

  /**
   * Removes this file or directory from the filesystem.
   */
  removeSync(): Err {
    try {
      fs.rmSync(this.path, { recursive: true });
    } catch (err: unknown) {
      return safeError(err);
    }
    return success();
  }

  /**
   * Removes this file or directory from the filesystem, if it exists.
   */
  removeIfExistsSync(): Err {
    try {
      fs.rmSync(this.path, { force: true, recursive: true });
    } catch (err: unknown) {
      return safeError(err);
    }
    return success();
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
}
