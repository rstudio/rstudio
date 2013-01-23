/*
 * Win32FileMonitor.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <core/system/FileMonitor.hpp>

#include <windows.h>

#include <memory>

#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/classification.hpp>

#include <core/FilePath.hpp>

#include <core/system/FileScanner.hpp>
#include <core/system/System.hpp>

#include "FileMonitorImpl.hpp"


namespace core {
namespace system {
namespace file_monitor {

namespace {

// buffer size for notifications (cannot be > 64kb for network drives)
const std::size_t kBuffSize = 32768;

class FileEventContext : boost::noncopyable
{
public:
   FileEventContext()
      : recursive(false),
        hDirectory(NULL),
        readDirChangesPending(false),
        hRestartTimer(NULL),
        restartCount(0)
   {
      receiveBuffer.resize(kBuffSize);
      handlingBuffer.resize(kBuffSize);
      handle = Handle((void*)this);
   }
   virtual ~FileEventContext() {}

   // handle
   Handle handle;

   // path we are monitoring, recursive flag, and handle to the directory
   FilePath rootPath;
   bool recursive;
   HANDLE hDirectory;

   // structures/buffers used to reach changes (and flag used to
   // determine whether the system may write into these buffers)
   OVERLAPPED overlapped;
   std::vector<BYTE> receiveBuffer;
   std::vector<BYTE> handlingBuffer;
   bool readDirChangesPending;

   // our own snapshot of the file tree
   tree<FileInfo> fileTree;

   // timer for attempting restarts on a delayed basis (and counter
   // to enforce a maximum number of retries)
   HANDLE hRestartTimer;
   int restartCount;

   // filter/callbacks
   boost::function<bool(const FileInfo&)> filter;
   Callbacks callbacks;
};

void safeCloseHandle(HANDLE hObject, const ErrorLocation& location)
{
   if (hObject != NULL)
   {
      if (!::CloseHandle(hObject))
         LOG_ERROR(systemError(::GetLastError(), location));
   }
}

void cleanupContext(FileEventContext* pContext)
{
   if (pContext->hDirectory != NULL)
   {
      if (!::CancelIo(pContext->hDirectory))
         LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));

      safeCloseHandle(pContext->hDirectory, ERROR_LOCATION);

      pContext->hDirectory = NULL;
   }

   if (pContext->hRestartTimer != NULL)
   {
      // make sure timer APC is never called after a cleanupContext
      if (!::CancelWaitableTimer(pContext->hRestartTimer))
         LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));

      safeCloseHandle(pContext->hRestartTimer, ERROR_LOCATION);

      pContext->hRestartTimer = NULL;
   }

   // delete pContext only if there are no read dir changes operations
   // pending -- if there are then we wait to delete pContext until
   // the completion routine gets ERROR_OPERATION_ABORTED
   if (!pContext->readDirChangesPending)
      delete pContext;
}

void removeTrailingSlash(std::wstring* pPath)
{
   boost::algorithm::trim_right_if(*pPath, boost::algorithm::is_any_of(L"\\"));
}

void ensureLongFilePath(FilePath* pFilePath)
{
   // get the filename, if it is 12 characters or less and it contains
   // a "~" then it may be a short file name. in that case do the conversion
   std::string filename = pFilePath->filename();
   if (filename.length() <= 12 && filename.find('~') != std::string::npos)
   {
      const std::size_t kBuffSize = (MAX_PATH*2) + 1;
      char buffer[kBuffSize];
      if (::GetLongPathName(pFilePath->absolutePath().c_str(),
                            buffer,
                            kBuffSize) > 0)
      {
         *pFilePath = FilePath(buffer);
      }
   }
}

void processFileChange(DWORD action,
                       const FilePath& filePath,
                       bool recursive,
                       const boost::function<bool(const FileInfo&)>& filter,
                       tree<FileInfo>* pTree,
                       std::vector<FileChangeEvent>* pFileChanges)
{
   // ignore all directory modified actions (we rely instead on the
   // actions which occur inside the directory)
   if (filePath.isDirectory() && (action == FILE_ACTION_MODIFIED))
      return;

   // screen out the root directory (this should never occur but if it
   // does for any reason we want to prevent it from interfering
   // with the logic below (which assumes a child path)
   if (filePath.isDirectory() &&
      (filePath.absolutePath() == pTree->begin()->absolutePath()))
   {
      return;
   }

   // get an iterator to this file's parent
   FileInfo parentFileInfo = FileInfo(filePath.parent());
   tree<FileInfo>::iterator parentIt = impl::findFile(pTree->begin(),
                                                      pTree->end(),
                                                      parentFileInfo);

   // if we can't find a parent then return (this directory may have
   // been excluded from scanning due to a filter)
   if (parentIt == pTree->end())
      return;

   // get the file info
   FileInfo fileInfo(filePath);

   // apply the filter
   if (filter && !filter(fileInfo))
      return;

   switch(action)
   {
      case FILE_ACTION_ADDED:
      case FILE_ACTION_RENAMED_NEW_NAME:
      {
         FileChangeEvent event(FileChangeEvent::FileAdded, fileInfo);
         Error error = impl::processFileAdded(parentIt,
                                              event,
                                              recursive,
                                              filter,
                                              pTree,
                                              pFileChanges);
         if (error)
            LOG_ERROR(error);
         break;
      }
      case FILE_ACTION_REMOVED:
      case FILE_ACTION_RENAMED_OLD_NAME:
      {
         FileChangeEvent event(FileChangeEvent::FileRemoved, fileInfo);
         impl::processFileRemoved(parentIt,
                                  event,
                                  recursive,
                                  pTree,
                                  pFileChanges);
         break;
      }
      case FILE_ACTION_MODIFIED:
      {
         FileChangeEvent event(FileChangeEvent::FileModified, fileInfo);
         impl::processFileModified(parentIt, event, pTree, pFileChanges);
         break;
      }
   }
}

void processFileChanges(FileEventContext* pContext,
                        DWORD dwNumberOfBytesTransfered)
{
   // accumulate file changes
   std::vector<FileChangeEvent> fileChanges;

   // cycle through the entries in the buffer
   char* pBuffer = (char*)&pContext->handlingBuffer[0];
   while(true)
   {
      // check for buffer pointer which has overflowed the end (apparently this
      // can happen if the underlying directory is deleted)
      if( (DWORD)((BYTE*)pBuffer - &(pContext->handlingBuffer[0])) >
          dwNumberOfBytesTransfered )
      {
         Error error = systemError(ERROR_BUFFER_OVERFLOW, ERROR_LOCATION);
         LOG_ERROR(error);
         break;
      }

      // get file notify struct
      FILE_NOTIFY_INFORMATION& fileNotify = (FILE_NOTIFY_INFORMATION&)*pBuffer;

      // compute a full wide path
      std::wstring name(fileNotify.FileName,
                        fileNotify.FileNameLength/sizeof(wchar_t));
      removeTrailingSlash(&name);
      FilePath filePath(pContext->rootPath.absolutePathW() + L"\\" + name);

      // ensure this is a long file name (docs say it could be short or long!)
      // (note that the call to GetLongFileNameW will fail if the file has
      // already been deleted, therefore if a delete notification using a
      // short file name comes in we may not successfully match it to
      // our in-memory tree and thus "miss" the delete
      ensureLongFilePath(&filePath);

      // apply filter if we have one
      if (!pContext->filter || pContext->filter(FileInfo(filePath)))
      {
         // process the file change
         processFileChange(fileNotify.Action,
                           filePath,
                           pContext->recursive,
                           pContext->filter,
                           &(pContext->fileTree),
                           &fileChanges);
      }

      // break or advance to next notification as necessary
      if (!fileNotify.NextEntryOffset)
         break;
      else
         pBuffer += fileNotify.NextEntryOffset;
   };

   // notify client of file changes
   pContext->callbacks.onFilesChanged(fileChanges);
}

void terminateWithMonitoringError(FileEventContext* pContext,
                                  const Error& error)
{
   pContext->callbacks.onMonitoringError(error);

   // unregister this monitor (this is done via postback from the
   // main file_monitor loop so that the monitor Handle can be tracked)
   file_monitor::unregisterMonitor(pContext->handle);
}


bool isRecoverableByRestart(const Error& error)
{
   return
      // undocumented return value that indicates we should do a restart
      // (see: http://blogs.msdn.com/b/oldnewthing/archive/2011/08/12/10195186.aspx)
      error.code().value() == ERROR_NOTIFY_ENUM_DIR ||

      // error which some users have observed occuring if a network
      // volume is being monitored and there are too many simultaneous
      // reads and writes
      error.code().value() == ERROR_TOO_MANY_CMDS;
}

Error readDirectoryChanges(FileEventContext* pContext);
void enqueRestartMonitoring(FileEventContext* pContext);

void restartMonitoring(FileEventContext* pContext)
{
   // start monitoring again (always do this before the scan so we don't
   // miss any events which occur while we are scanning)
   Error error = readDirectoryChanges(pContext);
   if (error)
   {
      // try to recover up to 10 times if the error is known to be recoverable
      // (note the enque delays the attempted restart by 1 second to give
      // the volume/system the chance to catch up from too many file changes)
      if (isRecoverableByRestart(error) && (++(pContext->restartCount) <= 10))
         enqueRestartMonitoring(pContext);
      else
         terminateWithMonitoringError(pContext, error);

      return;
   }

   // successfully restarted monitoring, reset the restart count to 0
   pContext->restartCount = 0;

   // full recursive scan to detect changes and refresh the tree
   error = impl::discoverAndProcessFileChanges(
                                       *(pContext->fileTree.begin()),
                                       pContext->recursive,
                                       pContext->filter,
                                       &(pContext->fileTree),
                                       pContext->callbacks.onFilesChanged);
   if (error)
      terminateWithMonitoringError(pContext, error);
}

VOID CALLBACK restartMonitoringApcProc(LPVOID lpArg, DWORD, DWORD)
{
   // get context
   FileEventContext* pContext = (FileEventContext*)lpArg;

   // close the timer handle
   safeCloseHandle(pContext->hRestartTimer, ERROR_LOCATION);
   pContext->hRestartTimer = NULL;

   // attempt the restart
   restartMonitoring(pContext);
}


void enqueRestartMonitoring(FileEventContext* pContext)
{
   // create the restart timer (1 second from now)
   pContext->hRestartTimer = ::CreateWaitableTimer(NULL, true, NULL);
   if (pContext->hRestartTimer == NULL)
   {
      Error error = systemError(::GetLastError(), ERROR_LOCATION);
      terminateWithMonitoringError(pContext, error);
      return;
   }

   // setup large integer to indicate 1 second from now
   const __int64 kSECOND = 10000000;
   __int64 qwDueTime = -1 * kSECOND;
   LARGE_INTEGER dueTime;
   dueTime.LowPart  = (DWORD) ( qwDueTime & 0xFFFFFFFF );
   dueTime.HighPart = (LONG)  ( qwDueTime >> 32 );

   // enque the restart proc to run after time timer expires
   BOOL success = ::SetWaitableTimer(pContext->hRestartTimer,
                                     &dueTime,
                                     0,
                                     restartMonitoringApcProc,
                                     (PVOID)pContext,
                                     FALSE);

   if (!success)
   {
      Error error = systemError(::GetLastError(), ERROR_LOCATION);
      terminateWithMonitoringError(pContext, error);
   }
}

// track number of active requests (we wait for this to get to zero before
// allowing the exit of the monitoring thread -- this ensures that we have
// performed full cleanup for all monitoring contexts before exiting.
volatile LONG s_activeRequests = 0;

VOID CALLBACK FileChangeCompletionRoutine(DWORD dwErrorCode,									// completion code
                                          DWORD dwNumberOfBytesTransfered,
                                          LPOVERLAPPED lpOverlapped)
{
   // get the context
   FileEventContext* pContext = (FileEventContext*)(lpOverlapped->hEvent);

   // note that read changes is no longer pending
   pContext->readDirChangesPending = false;

   // check for aborted
   if (dwErrorCode == ERROR_OPERATION_ABORTED)
   {
      // decrement the active request counter
      ::InterlockedDecrement(&s_activeRequests);

      // let the client know we are unregistered (note this call should always
      // be prior to delete pContext below!)
      pContext->callbacks.onUnregistered(pContext->handle);

      // we wait to delete the pContext until here because it owns the
      // OVERLAPPED structure and buffers, and so if we deleted it earlier
      // and the OS tried to access those memory regions we would crash
      delete pContext; 

      return;
   }

   // bail if we don't have callbacks installed yet (could have occurred
   // if we encountered an error during file scanning which caused us to
   // fail but then a file notification still snuck through)
   if (!pContext->callbacks.onFilesChanged)
      return;

   // make sure the root path still exists (if it doesn't then bail)
   if (!pContext->rootPath.exists())
   {
      Error error = fileNotFoundError(pContext->rootPath.absolutePath(),
                                      ERROR_LOCATION);
      terminateWithMonitoringError(pContext, error);
      return;
   }

   // check for buffer overflow. this means there are too many file changes
   // for the systme to keep up with -- in this case try to restart monitoring
   // (after a 1 second delay) and repeat the restart up to 10 times
   if(dwNumberOfBytesTransfered == 0)
   {
      // attempt to restart monitoring
      enqueRestartMonitoring(pContext);
      return;
   }

   // copy to processing buffer (so we can immediately begin another read)
   ::CopyMemory(&(pContext->handlingBuffer[0]),
                &(pContext->receiveBuffer[0]),
                dwNumberOfBytesTransfered);

   // begin the next read -- if this fails then enque a restart
   Error error = readDirectoryChanges(pContext);
   if (isRecoverableByRestart(error))
   {
      enqueRestartMonitoring(pContext);
      error = Success();
   }

   // process file changes
   processFileChanges(pContext, dwNumberOfBytesTransfered);

   // report the (fatal) error if necessary
   if (error)
      terminateWithMonitoringError(pContext, error);
}

Error readDirectoryChanges(FileEventContext* pContext)
{
   DWORD dwBytes = 0;
   if(!::ReadDirectoryChangesW(pContext->hDirectory,
                               &(pContext->receiveBuffer[0]),
                               pContext->receiveBuffer.size(),
                               pContext->recursive ? TRUE : FALSE,
                               FILE_NOTIFY_CHANGE_FILE_NAME |
                               FILE_NOTIFY_CHANGE_DIR_NAME |
                               FILE_NOTIFY_CHANGE_LAST_WRITE,
                               &dwBytes,
                               &(pContext->overlapped),
                               &FileChangeCompletionRoutine))
   {
      return systemError(::GetLastError(), ERROR_LOCATION);
   }
   else
   {
      pContext->readDirChangesPending = true;
      return Success();
   }
}

} // anonymous namespace

namespace detail {

// register a new file monitor
Handle registerMonitor(const core::FilePath& filePath,
                       bool recursive,
                       const boost::function<bool(const FileInfo&)>& filter,
                       const Callbacks& callbacks)
{
   // create and allocate FileEventContext (create auto-ptr in case we
   // return early, we'll call release later before returning)
   FileEventContext* pContext = new FileEventContext();
   std::auto_ptr<FileEventContext> autoPtrContext(pContext);

   // save the wide absolute path (notifications only come in wide strings)
   // strip any trailing slash for predictable append semantics
   std::wstring wpath = filePath.absolutePathW();
   removeTrailingSlash(&wpath);
   pContext->rootPath = FilePath(wpath);
   pContext->recursive = recursive;

   // open the directory
   pContext->hDirectory = ::CreateFileW(
                     filePath.absolutePathW().c_str(),
                     FILE_LIST_DIRECTORY,
                     FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE,
                     NULL,
                     OPEN_EXISTING,
                     FILE_FLAG_BACKUP_SEMANTICS | FILE_FLAG_OVERLAPPED,
                     NULL);
   if (pContext->hDirectory == INVALID_HANDLE_VALUE)
   {
      callbacks.onRegistrationError(
                     systemError(::GetLastError(),ERROR_LOCATION));
      return Handle();
   }

   // initialize overlapped structure to point to our context
   ::ZeroMemory(&(pContext->overlapped), sizeof(OVERLAPPED));
   pContext->overlapped.hEvent = pContext;

   // get the monitoring started
   Error error = readDirectoryChanges(pContext);
   if (error)
   {
      // cleanup
      safeCloseHandle(pContext->hDirectory, ERROR_LOCATION);

      // return error
      callbacks.onRegistrationError(error);

      return Handle();
   }

   // we have passed the pContext into the system so it's ownership will
   // now be governed by the receipt of ERROR_OPERATION_ABORTED within
   // the completion callback
   autoPtrContext.release();

   // increment the number of active requests
   ::InterlockedIncrement(&s_activeRequests);

   // scan the files
   core::system::FileScannerOptions options;
   options.recursive = recursive;
   options.yield = true;
   options.filter = filter;
   error = scanFiles(FileInfo(filePath), options, &pContext->fileTree);
   if (error)
   {
       // cleanup
       cleanupContext(pContext);

       // return error
       callbacks.onRegistrationError(error);

       return Handle();
   }

   // now that we have finished the file listing we know we have a valid
   // file-monitor so set the callbacks
   pContext->filter = filter;
   pContext->callbacks = callbacks;

   // notify the caller that we have successfully registered
   callbacks.onRegistered(pContext->handle, pContext->fileTree);

   // return the handle
   return pContext->handle;
}

// unregister a file monitor
void unregisterMonitor(Handle handle)
{
   // this will end up calling the completion routine with
   // ERROR_OPERATION_ABORTED at which point we'll delete the context
   cleanupContext((FileEventContext*)(handle.pData));
}

void run(const boost::function<void()>& checkForInput)
{
   // initialize active requests to zero
   s_activeRequests = 0;

   // loop waiting for:
   //   - completion routine callbacks (occur during SleepEx); or
   //   - inbound commands (occur during checkForInput)
   while (true)
   {
      // look for changes and keep calling SleepEx as long as we have them
      while(::SleepEx(1, TRUE) == WAIT_IO_COMPLETION) ;

      checkForInput();
   }
}

void stop()
{
   // call ::SleepEx until all active requests hae terminated
   while (s_activeRequests > 0)
   {
      ::SleepEx(100, TRUE);
   }
}

} // namespace detail
} // namespace file_monitor
} // namespace system
} // namespace core 

   



