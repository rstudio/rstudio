/*
 * MacFileMonitor.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/system/FileMonitor.hpp>

#include <CoreServices/CoreServices.h>

#include <sys/stat.h>

#include <algorithm>
#include <map>
#include <vector>

#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/classification.hpp>
#include <boost/bind/bind.hpp>

#include <core/Log.hpp>
#include <shared_core/Error.hpp>
#include <core/FileInfo.hpp>
#include <core/Thread.hpp>

#include <core/system/FileScanner.hpp>
#include <core/system/System.hpp>

#include "FileMonitorImpl.hpp"

using namespace boost::placeholders;

namespace rstudio {
namespace core {
namespace system {
namespace file_monitor {

namespace {

// FSEvents delivery latency for recursive watches, in seconds. fseventsd
// coalesces events for the same path within this window before delivering
// them, so a longer latency directly reduces the number of entries
// transiting the per-client queue during sustained churn (e.g. a build) --
// overflow of that queue is what produces UserDropped events and forces
// recovery rescans. Because streams are created with
// kFSEventStreamCreateFlagNoDefer, the first event after an idle period is
// still delivered immediately; the latency only batches subsequent events.
const CFTimeInterval kFSEventsLatencySeconds = 2.0;

// Non-recursive watches (a single directory, with per-file events) don't
// generate the event volume that overflows the queue, and a longer latency
// only leaves the UI (e.g. the Files pane) staler during bursts of changes,
// so they keep the historical latency.
const CFTimeInterval kFSEventsNonRecursiveLatencySeconds = 1.0;

// Minimum interval, in seconds, between full-tree rescans triggered by
// dropped events (measured from the completion of one rescan to the start of
// the next). Dropped events tend to arrive in bursts -- the recovery rescan
// itself blocks the run loop long enough for the queue to overflow again --
// so drops arriving inside the interval are coalesced into a single deferred
// rescan rather than each triggering their own.
const CFTimeInterval kDroppedRescanIntervalSeconds = 10.0;

// Maximum number of consecutive failed deferred recovery rescans before
// giving up (see droppedRescanTimerCallback). A rescan that keeps failing
// (e.g. because the watched root became unreadable) won't start succeeding
// on its own, so retrying it forever only spams the log; a later dropped
// event re-triggers recovery through the normal path.
const int kMaxDroppedRescanAttempts = 3;

// FSEventStreamSetExclusionPaths silently fails above this documented limit
const CFIndex kMaxExclusionPaths = 8;

class DirectoryHandle : boost::noncopyable
{
public:
   DirectoryHandle(const std::string& path)
      : fd_(-1)
   {
      // O_CLOEXEC so the descriptor (held for the monitor's lifetime) is
      // closed across exec, rather than leaking into child processes where
      // it would pin the watched directory for as long as the child runs
      const char* cpath = path.c_str();
      auto f = [&]() { return ::open(cpath, O_DIRECTORY | O_CLOEXEC); };
      Error error = posix::posixCall<int>(f, ERROR_LOCATION, &fd_);
      if (error)
         LOG_ERROR(error);
   }

   ~DirectoryHandle()
   {
      if (fd_ != -1)
      {
         auto f = [&]() { return ::close(fd_); };
         safePosixCall<int>(f, ERROR_LOCATION);
      }
   }

   FilePath currentPath()
   {
      // read the path associated with the descriptor
      char path[PATH_MAX];
      auto f = [&]() { return ::fcntl(fd_, F_GETPATH, path); };
      Error error = posix::posixCall<int>(f, ERROR_LOCATION);
      if (error)
         return FilePath();

      // validate the directory still exists
      FilePath currPath(path);
      if (!currPath.isDirectory())
         return FilePath();

      // okay, return the path
      return currPath;
   }

private:
   int fd_;
};

class FileEventContext : boost::noncopyable
{
public:
   FileEventContext(const FilePath& rootPath)
      : rootPath(rootPath),
        rootHandle(rootPath.getAbsolutePathNative()),
        streamRef(nullptr),
        recursive(false),
        rescanTimerRef(nullptr),
        lastDroppedRescanTime(0),
        rescanFailureCount(0)
   {
      handle = Handle((void*)this);
   }

   virtual ~FileEventContext()
   {
   }

   Handle handle;
   FilePath rootPath;
   std::string canonicalRootPath;
   DirectoryHandle rootHandle;
   FSEventStreamRef streamRef;
   bool recursive;

   // deferred dropped-event recovery rescan (see performDroppedRescan); the
   // timer, the FSEvents callback, and unregistration all run on the file
   // monitor thread's run loop, so no synchronization is required
   CFRunLoopTimerRef rescanTimerRef;
   CFAbsoluteTime lastDroppedRescanTime;
   int rescanFailureCount;
   boost::function<bool(const FileInfo&)> filter;
   tree<FileInfo> fileTree;
   Callbacks callbacks;
};

// FSEvents reports event paths in canonical form (e.g. /private/tmp/foo for
// a watch registered as /tmp/foo, since /tmp is a symlink on macOS). The file
// tree and the paths we report to clients use the form the watch was
// registered with, so map the canonical root prefix back to the registered
// root before any tree lookup or event emission. Without this, clients that
// compare reported paths against the path they asked us to monitor (like the
// Files pane) drop every event (#17909).
std::string mapEventPath(FileEventContext* pContext, const std::string& path)
{
   const std::string& canonicalRoot = pContext->canonicalRootPath;
   const std::string& registeredRoot = pContext->rootPath.getAbsolutePath();
   if (canonicalRoot == registeredRoot)
      return path;

   if (path == canonicalRoot)
      return registeredRoot;

   if (path.length() > canonicalRoot.length() &&
       path.compare(0, canonicalRoot.length(), canonicalRoot) == 0 &&
       path[canonicalRoot.length()] == '/')
   {
      return registeredRoot + path.substr(canonicalRoot.length());
   }

   return path;
}

// Map a path under the registered root to the canonical form FSEvents works
// with (the inverse of mapEventPath). Used for exclusion paths, which
// fseventsd matches against canonical event paths. Only the root prefix is
// mapped -- symlinked components below the root are left as given, matching
// how FSEvents reports paths under the watched hierarchy.
std::string mapToCanonicalPath(FileEventContext* pContext,
                               const std::string& path)
{
   const std::string& canonicalRoot = pContext->canonicalRootPath;
   const std::string& registeredRoot = pContext->rootPath.getAbsolutePath();
   if (canonicalRoot == registeredRoot)
      return path;

   if (path == registeredRoot)
      return canonicalRoot;

   if (path.length() > registeredRoot.length() &&
       path.compare(0, registeredRoot.length(), registeredRoot) == 0 &&
       path[registeredRoot.length()] == '/')
   {
      return canonicalRoot + path.substr(registeredRoot.length());
   }

   return path;
}

// Read a FileInfo via lstat, matching what PosixFileScanner produces for the
// initial tree population. Mirroring that view (in particular: symlinks treated
// literally) keeps per-event updates consistent with the tree so we don't emit
// spurious modify events for symlinks. Returns systemError on lstat failure;
// callers special-case ENOENT to emit FileRemoved.
Error readFileInfoLStat(const std::string& path, FileInfo* pFileInfo)
{
   struct stat st;
   if (::lstat(path.c_str(), &st) == -1)
   {
      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("path", path);
      return error;
   }

   bool isSymlink = S_ISLNK(st.st_mode);
   if (S_ISDIR(st.st_mode))
   {
      *pFileInfo = FileInfo(path, true, isSymlink);
   }
   else
   {
      *pFileInfo = FileInfo(path,
                            false,
                            st.st_size,
                            st.st_mtimespec.tv_sec,
                            isSymlink);
   }
   return Success();
}

// Process a batch of FSEvents events for a non-recursive watch. With the
// kFSEventStreamCreateFlagFileEvents flag enabled, FSEvents reports the
// specific file path that changed (rather than just the enclosing directory).
// We filter to events whose parent is the watched root and update the tree
// directly using lstat, avoiding the readdir+diff that the directory-level
// callback path performs.
void processNonRecursiveFileEvents(FileEventContext* pContext,
                                   size_t numEvents,
                                   char** paths,
                                   const FSEventStreamEventFlags eventFlags[])
{
   const std::string& rootPathStr = pContext->rootPath.getAbsolutePath();

   std::vector<FileChangeEvent> fileChanges;
   bool needsFullRescan = false;

   for (std::size_t i = 0; i < numEvents; i++)
   {
      FSEventStreamEventFlags flags = eventFlags[i];

      // FSEvents indicates events were dropped (kernel/user buffer overflow,
      // or MustScanSubDirs); reconcile by rescanning the watched directory.
      if (flags & (kFSEventStreamEventFlagMustScanSubDirs |
                   kFSEventStreamEventFlagKernelDropped |
                   kFSEventStreamEventFlagUserDropped))
      {
         needsFullRescan = true;
         continue;
      }

      std::string path(paths[i]);
      boost::algorithm::trim_right_if(path, boost::algorithm::is_any_of("/"));

      // map the canonical event path back to the registered path form
      path = mapEventPath(pContext, path);

      // Only process events whose direct parent is the watched root. Subtree
      // events still wake us up, but they cost just this comparison.
      FilePath eventPath(path);
      if (eventPath.getParent().getAbsolutePath() != rootPathStr)
         continue;

      tree<FileInfo>::iterator rootIt = pContext->fileTree.begin();

      FileInfo fileInfo;
      Error statError = readFileInfoLStat(path, &fileInfo);
      if (statError)
      {
         if (statError == systemError(boost::system::errc::no_such_file_or_directory, ErrorLocation()))
         {
            // entry no longer exists; processFileRemoved finds it in the tree
            // by path and uses the stored FileInfo for the event payload
            FileChangeEvent change(FileChangeEvent::FileRemoved,
                                   FileInfo(path, false));
            impl::processFileRemoved(rootIt,
                                     change,
                                     false,
                                     &pContext->fileTree,
                                     &fileChanges);
         }
         else
         {
            LOG_ERROR(statError);
         }
         continue;
      }

      // entry exists; apply user filter
      if (pContext->filter && !pContext->filter(fileInfo))
         continue;

      // processFileAdded handles the in-tree case by emitting FileModified
      // when the FileInfo differs, and does nothing when it matches
      FileChangeEvent change(FileChangeEvent::FileAdded, fileInfo);
      Error error = impl::processFileAdded(rootIt,
                                           change,
                                           false,
                                           pContext->filter,
                                           &pContext->fileTree,
                                           &fileChanges);
      if (error)
         LOG_ERROR(error);
   }

   // Emission order matches LinuxFileMonitor's IN_Q_OVERFLOW path: rescan
   // first (so it reconciles the filesystem against the tree the loop just
   // mutated -- entries already applied have no diff and aren't re-emitted),
   // then fire the per-event vector the loop accumulated. The rescan and
   // the accumulated events are disjoint by construction, so order is just
   // a matter of cross-backend consistency.
   if (needsFullRescan)
   {
      LOG_WARNING_MESSAGE("File monitor events were dropped; rescanning " +
                          rootPathStr);

      // scan the root unconditionally, matching registration -- the filter
      // is meant for entries within the directory, and gating recovery on
      // the root itself would leave dropped events unrecovered for watches
      // whose root the filter excludes (e.g. a hidden directory)
      FileInfo rootInfo(pContext->rootPath);
      Error error = impl::discoverAndProcessFileChanges(
                                          rootInfo,
                                          false,
                                          pContext->filter,
                                          &(pContext->fileTree),
                                          pContext->callbacks.onFilesChanged);
      if (error &&
         (error != systemError(boost::system::errc::no_such_file_or_directory, ErrorLocation())))
      {
         LOG_ERROR(error);
      }
   }

   if (!fileChanges.empty())
      pContext->callbacks.onFilesChanged(fileChanges);
}

// Reconcile the tree after fseventsd reported dropped events (kernel or
// user-space queue overflow) or coalesced events onto the stream root (a
// root-flagged MustScanSubDirs) on a recursive watch. With drops the loss
// is not necessarily confined to the delivered paths, and the flagged path
// may even be excluded by our filter (e.g. .git), so rescan from the stream
// root rather than trusting the flagged path. Returns false if the scan
// failed; callers then process the delivered events normally so they are
// not lost.
bool performDroppedRescan(FileEventContext* pContext)
{
   LOG_WARNING_MESSAGE("File monitor events were dropped or coalesced; rescanning " +
                       pContext->rootPath.getAbsolutePath());

   Error error = impl::discoverAndProcessFileChanges(
                         FileInfo(pContext->rootPath.getAbsolutePath(), true),
                         true,
                         pContext->filter,
                         &(pContext->fileTree),
                         pContext->callbacks.onFilesChanged);
   if (error)
   {
      if (error != systemError(boost::system::errc::no_such_file_or_directory, ErrorLocation()))
         LOG_ERROR(error);
      return false;
   }

   // measure the debounce interval from the completion of a successful scan:
   // the scan itself can take several seconds on a large tree, and a failed
   // attempt shouldn't arm the debounce and suppress a prompt retry
   pContext->lastDroppedRescanTime = ::CFAbsoluteTimeGetCurrent();
   pContext->rescanFailureCount = 0;

   return true;
}

void releaseDroppedRescanTimer(FileEventContext* pContext)
{
   if (pContext->rescanTimerRef == nullptr)
      return;

   ::CFRunLoopTimerInvalidate(pContext->rescanTimerRef);
   ::CFRelease(pContext->rescanTimerRef);
   pContext->rescanTimerRef = nullptr;
}

void scheduleDroppedRescan(FileEventContext* pContext, CFTimeInterval delay);

void droppedRescanTimerCallback(CFRunLoopTimerRef timerRef, void* pInfo)
{
   FileEventContext* pContext = (FileEventContext*) pInfo;

   // the timer is one-shot; drop our reference before rescanning so a drop
   // delivered during the rescan can schedule a new deferred rescan
   releaseDroppedRescanTimer(pContext);

   if (performDroppedRescan(pContext))
      return;

   // retry on failure: batches delivered while this timer was pending had
   // their MustScanSubDirs recursion suppressed on the promise that this
   // rescan would cover them, so giving up early would leave those changes
   // unreconciled (unregistration cancels the timer if the monitor goes
   // away first). cap the attempts, though: a rescan that keeps failing
   // won't start succeeding on its own, and a later dropped event
   // re-triggers recovery through the normal path
   if (++pContext->rescanFailureCount < kMaxDroppedRescanAttempts)
   {
      scheduleDroppedRescan(pContext, kDroppedRescanIntervalSeconds);
   }
   else
   {
      LOG_ERROR_MESSAGE("Giving up on file monitor recovery rescan for " +
                        pContext->rootPath.getAbsolutePath());
      pContext->rescanFailureCount = 0;
   }
}

// Schedule a single deferred recovery rescan on the file monitor thread's
// run loop. Used when drops arrive inside the debounce interval: the
// trailing rescan is required for correctness, since the dropped changes may
// postdate what the previous rescan observed.
void scheduleDroppedRescan(FileEventContext* pContext, CFTimeInterval delay)
{
   CFRunLoopTimerContext timerContext;
   timerContext.version = 0;
   timerContext.info = (void*) pContext;
   timerContext.retain = nullptr;
   timerContext.release = nullptr;
   timerContext.copyDescription = nullptr;

   CFRunLoopTimerRef timerRef = ::CFRunLoopTimerCreate(
                  kCFAllocatorDefault,
                  ::CFAbsoluteTimeGetCurrent() + delay,
                  0,
                  0,
                  0,
                  droppedRescanTimerCallback,
                  &timerContext);
   if (timerRef == nullptr)
   {
      // couldn't allocate the timer; rescan immediately rather than risk
      // leaving the dropped changes unreconciled
      performDroppedRescan(pContext);
      return;
   }

   pContext->rescanTimerRef = timerRef;
   ::CFRunLoopAddTimer(::CFRunLoopGetCurrent(), timerRef, kCFRunLoopDefaultMode);
}

void fileEventCallback(ConstFSEventStreamRef streamRef,
                       void *pCallbackInfo,
                       size_t numEvents,
                       void *eventPaths,
                       const FSEventStreamEventFlags eventFlags[],
                       const FSEventStreamEventId eventIds[])
{
   // get context
   FileEventContext* pContext = (FileEventContext*)pCallbackInfo;

   // bail if we don't have callbacks (we wouldn't if a callback snuck
   // through to us even after we failed to fully initialize the file monitor
   // (e.g. if there was an error during file listing)
   if (!pContext->callbacks.onFilesChanged)
      return;

   // de-register the file monitor if the root path has changed or been
   // removed
   //
   // NOTE: on macOS Catalina, we observed spurious 'RootChanged' events
   // delivered causing the file monitor to erroneously detach. protect
   // against this by also double-checking whether the original path
   // monitored and the path reported by the file handle match up
   //
   // We check for filesystem equivalence (not path equivalence) since macOS
   // Catalina can issue a RootChanged event for conversion to/from a
   // canonicalized /System/Volumes/Data path.
   //
   // https://github.com/rstudio/rstudio/issues/4755
   if (!pContext->rootPath.isEquivalentTo(pContext->rootHandle.currentPath()))
   {
      // propagate error to client
      Error error = fileNotFoundError(pContext->rootPath.getAbsolutePath(),
                                      ERROR_LOCATION);
      pContext->callbacks.onMonitoringError(error);

      // unregister this monitor (this is done via postback from the
      // main file_monitor loop so that the monitor Handle can be tracked)
      file_monitor::unregisterMonitor(pContext->handle);

      return;
   }

   // FSEvents documents `eventPaths` (and the parallel `eventFlags` array) as
   // valid only for the duration of this callback. Both branches below copy
   // the strings they need before any cross-callback use.
   char **paths = (char**) eventPaths;

   // Non-recursive watches go through the per-file event path; see
   // processNonRecursiveFileEvents for details (#17669).
   if (!pContext->recursive)
   {
      processNonRecursiveFileEvents(pContext, numEvents, paths, eventFlags);
      return;
   }

   // Coalesce the batch before processing: fseventsd delivers batches in
   // which the same directory can appear many times (e.g. a bulk operation
   // whose changes span multiple coalescing windows, or a backlog that
   // accumulated while a previous scan was in progress). Each entry costs a
   // full readdir + diff of that directory, so processing duplicates is pure
   // waste -- worse, the redundant scans can back up event processing far
   // enough that fseventsd drops events (UserDropped), and a dropped change
   // stays invisible until unrelated later activity triggers the
   // MustScanSubDirs recovery rescan (#18260).
   std::vector<std::pair<std::string, FSEventStreamEventFlags>> events;
   std::map<std::string, std::size_t> eventIndex;
   bool needsRescan = false;
   for (std::size_t i = 0; i < numEvents; i++)
   {
      // make a copy of the path and strip off trailing / if necessary
      std::string path(paths[i]);
      boost::algorithm::trim_right_if(path, boost::algorithm::is_any_of("/"));

      // map the canonical event path back to the registered path form
      path = mapEventPath(pContext, path);

      // drops (kernel or user-space queue overflow) mean the loss is not
      // confined to the delivered paths, so recover with a rescan from the
      // stream root. MustScanSubDirs without a drop flag (hierarchical
      // coalescing per FSEvents.h) is confined to the flagged path's
      // subtree, so it is left to the scoped, filter-gated per-path
      // recursion below -- except when the flagged path is the root
      // itself, where the scoped scan would be a full-tree scan anyway;
      // route that through the same debounced rescan so root-level
      // coalescing can't trigger unbounded back-to-back full-tree scans
      if (eventFlags[i] & (kFSEventStreamEventFlagUserDropped |
                           kFSEventStreamEventFlagKernelDropped))
      {
         needsRescan = true;
      }
      else if ((eventFlags[i] & kFSEventStreamEventFlagMustScanSubDirs) &&
               path == pContext->rootPath.getAbsolutePath())
      {
         needsRescan = true;
      }

      // fold duplicate paths into a single event, merging their flags
      std::map<std::string, std::size_t>::iterator it = eventIndex.find(path);
      if (it != eventIndex.end())
      {
         events[it->second].second |= eventFlags[i];
      }
      else
      {
         eventIndex[path] = events.size();
         events.push_back(std::make_pair(path, eventFlags[i]));
      }
   }

   // If fseventsd dropped events for this stream or coalesced events onto
   // the stream root, reconcile with a recursive rescan from the stream
   // root (see performDroppedRescan). Debounce the rescans: overload tends
   // to arrive in bursts, since the rescan itself blocks the run loop long
   // enough for the queue to overflow again, and back-to-back full rescans
   // only feed that cycle. Drops arriving inside the debounce interval (or
   // while a deferred rescan is already scheduled) coalesce into a single
   // trailing rescan, and the delivered events are still processed below in
   // the meantime.
   if (needsRescan && pContext->rescanTimerRef == nullptr)
   {
      CFAbsoluteTime elapsed =
            ::CFAbsoluteTimeGetCurrent() - pContext->lastDroppedRescanTime;
      if (elapsed >= kDroppedRescanIntervalSeconds)
      {
         // the per-path scans below are redundant after a successful rescan,
         // but on rescan failure fall through to them so the delivered
         // events are still processed
         if (performDroppedRescan(pContext))
            return;
      }
      else
      {
         // CFAbsoluteTimeGetCurrent() is wall-clock time, so a backward
         // clock step can make 'elapsed' negative; clamp so the deferred
         // rescan is never scheduled more than a full debounce interval out
         scheduleDroppedRescan(pContext,
                               kDroppedRescanIntervalSeconds - std::max(elapsed, 0.0));
      }
   }

   // While a deferred recovery rescan is pending, skip MustScanSubDirs
   // recursion below: fseventsd attaches that flag (typically to the root
   // path), and honoring it would perform exactly the full-tree scan the
   // debounce is deferring. The deferred rescan runs later and from the
   // root, so it observes everything these scans would.
   bool rescanPending = pContext->rescanTimerRef != nullptr;

   for (const std::pair<std::string, FSEventStreamEventFlags>& event : events)
   {
      // get FileInfo for this directory
      FileInfo fileInfo(event.first, true);

      // apply the filter (if any)
      if (!pContext->filter || pContext->filter(fileInfo))
      {
         // check for need to do recursive scan
         bool recursive = pContext->recursive &&
                          !rescanPending &&
                          (event.second & kFSEventStreamEventFlagMustScanSubDirs);

         // process changes
         Error error = impl::discoverAndProcessFileChanges(
                                             fileInfo,
                                             recursive,
                                             pContext->filter,
                                             &(pContext->fileTree),
                                             pContext->callbacks.onFilesChanged);
         if (error &&
            (error != systemError(boost::system::errc::no_such_file_or_directory, ErrorLocation())))
         {
            LOG_ERROR(error);
         }
      }
   }
}

class CFRefScope : boost::noncopyable
{
public:
   explicit CFRefScope(CFTypeRef ref)
      : ref_(ref)
   {
   }
   virtual ~CFRefScope()
   {
      try
      {
         ::CFRelease(ref_);
      }
      catch(...)
      {
      }
   }
private:
   CFTypeRef ref_;
};

void invalidateAndReleaseEventStream(FSEventStreamRef streamRef)
{
   ::FSEventStreamInvalidate(streamRef);
   ::FSEventStreamRelease(streamRef);
}

void stopInvalidateAndReleaseEventStream(FSEventStreamRef streamRef)
{
   ::FSEventStreamStop(streamRef);
   invalidateAndReleaseEventStream(streamRef);
}

} // anonymous namespace

namespace detail {

// register a new file monitor
Handle registerMonitor(const FilePath& filePath,
                       bool recursive,
                       const boost::function<bool(const FileInfo&)>& filter,
                       const Callbacks& callbacks,
                       const std::vector<FilePath>& excludedPaths)
{
   // FSEvents reports event paths in canonical form (e.g. /private/tmp/foo
   // even when we registered /tmp/foo, because /tmp is a symlink on macOS).
   // Register the stream against the canonical path so that the WatchRoot
   // machinery agrees with what FSEvents reports, but keep the caller's path
   // form for the rootPath, the file tree built during the initial scan, and
   // the paths reported to callbacks -- clients compare reported paths
   // against the path they asked us to monitor (#17909). Event paths arriving
   // in canonical form are mapped back via mapEventPath.
   std::string canonicalRootPath = filePath.getAbsolutePath();
   if (filePath.exists())
   {
      std::string canonical = filePath.getCanonicalPath();
      if (!canonical.empty())
         canonicalRootPath = canonical;
   }

   // allocate file path
   CFStringRef filePathRef = ::CFStringCreateWithCString(
                                       kCFAllocatorDefault,
                                       canonicalRootPath.c_str(),
                                       kCFStringEncodingUTF8);
   if (filePathRef == nullptr)
   {
      callbacks.onRegistrationError(systemError(
                                       boost::system::errc::not_enough_memory,
                                       ERROR_LOCATION));
      return Handle();
   }
   CFRefScope filePathRefScope(filePathRef);

   // allocate paths array
   CFArrayRef pathsArrayRef = ::CFArrayCreate(kCFAllocatorDefault,
                                              (const void **)&filePathRef,
                                              1,
                                              nullptr);
   if (pathsArrayRef == nullptr)
   {
      callbacks.onRegistrationError(systemError(
                                       boost::system::errc::not_enough_memory,
                                       ERROR_LOCATION));
      return Handle();
   }
   CFRefScope pathsArrayRefScope(pathsArrayRef);

   // create and allocate FileEventContext (create auto-ptr in case we
   // return early, we'll call release later before returning)
   FileEventContext* pContext = new FileEventContext(filePath);
   pContext->canonicalRootPath = canonicalRootPath;
   pContext->recursive = recursive;
   pContext->filter = filter;
   std::unique_ptr<FileEventContext> autoPtrContext(pContext);
   FSEventStreamContext context;
   context.version = 0;
   context.info = (void*) pContext;
   context.retain = nullptr;
   context.release = nullptr;
   context.copyDescription = nullptr;

   // create the stream and save a reference to it
   //
   // For non-recursive watches we additionally request per-file events: without
   // this flag FSEvents reports only the enclosing directory of each change,
   // forcing a readdir+diff every time something inside the watched directory
   // changes. With the flag set the callback receives the specific file path
   // and processNonRecursiveFileEvents reconciles it against the tree via
   // lstat (it consults the per-event flags only to detect drops). Recursive
   // watches stay on directory-granularity events and the readdir+diff path.
   FSEventStreamCreateFlags streamFlags =
                  kFSEventStreamCreateFlagNoDefer |
                  kFSEventStreamCreateFlagWatchRoot;
   if (!recursive)
      streamFlags |= kFSEventStreamCreateFlagFileEvents;

   CFTimeInterval latency = recursive
         ? kFSEventsLatencySeconds
         : kFSEventsNonRecursiveLatencySeconds;

   pContext->streamRef = ::FSEventStreamCreate(
                  kCFAllocatorDefault,
                  &fileEventCallback,
                  &context,
                  pathsArrayRef,
                  kFSEventStreamEventIdSinceNow,
                  latency,
                  streamFlags);
   if (pContext->streamRef == nullptr)
   {
      callbacks.onRegistrationError(systemError(
                                       boost::system::errc::no_stream_resources,
                                       ERROR_LOCATION));
      return Handle();
   }

   // Exclude the requested directories at the fseventsd level (best effort).
   // The caller's filter already discards events under these paths, but
   // filtering happens after delivery: every event still transits the
   // per-client queue, so sustained churn in an ignored directory can
   // overflow it and force dropped-event recovery rescans. fseventsd matches
   // exclusions against canonical event paths, so map them accordingly.
   if (!excludedPaths.empty())
   {
      std::vector<CFStringRef> exclusionRefs;
      for (const FilePath& excludedPath : excludedPaths)
      {
         if ((CFIndex) exclusionRefs.size() >= kMaxExclusionPaths)
         {
            LOG_DEBUG_MESSAGE("Ignoring file monitor exclusion paths beyond the FSEvents limit");
            break;
         }

         std::string canonicalPath =
               mapToCanonicalPath(pContext, excludedPath.getAbsolutePath());
         CFStringRef pathRef = ::CFStringCreateWithCString(
                                          kCFAllocatorDefault,
                                          canonicalPath.c_str(),
                                          kCFStringEncodingUTF8);
         if (pathRef != nullptr)
            exclusionRefs.push_back(pathRef);
      }

      if (!exclusionRefs.empty())
      {
         CFArrayRef exclusionsArrayRef = ::CFArrayCreate(
                  kCFAllocatorDefault,
                  (const void**) exclusionRefs.data(),
                  exclusionRefs.size(),
                  &kCFTypeArrayCallBacks);
         if (exclusionsArrayRef != nullptr)
         {
            if (!::FSEventStreamSetExclusionPaths(pContext->streamRef, exclusionsArrayRef))
               LOG_WARNING_MESSAGE("Failed to set file monitor exclusion paths for " + filePath.getAbsolutePath());
            ::CFRelease(exclusionsArrayRef);
         }

         for (CFStringRef pathRef : exclusionRefs)
            ::CFRelease(pathRef);
      }
   }

   // schedule with the run loop
   ::FSEventStreamScheduleWithRunLoop(pContext->streamRef,
                                      ::CFRunLoopGetCurrent(),
                                      kCFRunLoopDefaultMode);

   // start the event stream (check for errors and release if necessary
   if (!::FSEventStreamStart(pContext->streamRef))
   {
      invalidateAndReleaseEventStream(pContext->streamRef);

      callbacks.onRegistrationError(systemError(
                                       boost::system::errc::no_stream_resources,
                                       ERROR_LOCATION));
      return Handle();
   }

   // scan the files
   core::system::FileScannerOptions options;
   options.recursive = recursive;
   options.yield = true;
   options.filter = filter;
   Error error = scanFiles(FileInfo(filePath), options, &pContext->fileTree);
   if (error)
   {
       // stop, invalidate, release
       stopInvalidateAndReleaseEventStream(pContext->streamRef);

       // return error
       callbacks.onRegistrationError(error);
       return Handle();
   }

   // now that we have finished the file listing we know we have a valid
   // file-monitor so set the callbacks
   pContext->callbacks = callbacks;

   // we are going to pass the context pointer to the client (as the Handle)
   // so we release it here to relinquish ownership
   autoPtrContext.release();

   // notify the caller that we have successfully registered
   callbacks.onRegistered(pContext->handle, pContext->fileTree);

   // return the handle
   return pContext->handle;
}

// unregister a file monitor
void unregisterMonitor(Handle handle)
{
   // cast to context
   FileEventContext* pContext = (FileEventContext*)(handle.pData);

   // cancel any pending deferred rescan (its callback holds pContext)
   releaseDroppedRescanTimer(pContext);

   // stop, invalidate, release
   stopInvalidateAndReleaseEventStream(pContext->streamRef);

   // let the client know we are unregistered (note this call should always
   // be prior to delete pContext below!)
   if (pContext->callbacks.onUnregistered)
      pContext->callbacks.onUnregistered(handle);

   // delete the context
   delete pContext;
}

void run(const boost::function<void()>& checkForInput)
{
   // ensure we have a run loop for this thread (not sure if this is
   // strictly necessary but it is not harmful)
   ::CFRunLoopGetCurrent();

   while (true)
   {
      // check the run loop
      SInt32 reason = ::CFRunLoopRunInMode(kCFRunLoopDefaultMode, 0, true);

      // if we handled a source then run again
      if (reason == kCFRunLoopRunHandledSource)
      {
         continue;
      }

      else if (reason == kCFRunLoopRunStopped)
      {
         LOG_WARNING_MESSAGE("Unexpected stop of file monitor run loop");
         break;
      }

      // check for input
      checkForInput();
   }
}

void stop()
{
   // no need to call CFRunLoopStop(CFRunLoopGetCurrent()) because control
   // is already outside of the run loop logic (above).
}

} // namespace detail
} // namespace file_monitor
} // namespace system
} // namespace core 
} // namespace rstudio

   



