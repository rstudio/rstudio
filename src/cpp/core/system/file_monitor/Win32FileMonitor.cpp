/*
 * Win32FileMonitor.cpp
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

// TODO: there is still some flakiness/unreliablity around copy and paste
// of folders within the Explorer. try some lower level logging to see what is
// actually being generated and how we might be mis-interpreting it

// TODO: ensure that ReadDirectoryChangesW definitely doesn't drop
// events in between calls (because we could be doing arbitrarily long
// scanning operations)

// TODO: explicitly handle case of volume type not supporting monitoring
// (on windows indicated by ERROR_INVALID_FUNCTION)

// TODO: investigate ERROR_TOO_MANY_CMDS network bios error referenced
// in article. believe it has to do with many simultaneous reads/writes.
// one possible workaround is to queue an APC for a retry. open incident
// with Microsof Professional Advisory Services (reference article)
//
//   http://support.microsoft.com/gp/advisoryservice#tab0
//

// TODO: investigate whether we need to call AdjustTokenPrivilleges in order
// to call ReadDirectoryChangesW (DirectoryChangeWatcher sample does. see
// the CPrivillegeEnabler)

// TOOD: for non-recursive mode don't do full recursive re-scan on
// dwBytesTransferred == 0 sentienl for buffer overflow


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
      : hDirectory(NULL)
   {
      receiveBuffer.resize(kBuffSize);
      handlingBuffer.resize(kBuffSize);
   }
   virtual ~FileEventContext() {}

   FilePath rootPath;
   HANDLE hDirectory;
   OVERLAPPED overlapped;
   std::vector<BYTE> receiveBuffer;
   std::vector<BYTE> handlingBuffer;
   tree<FileInfo> fileTree;
   Callbacks::ReportError onMonitoringError;
   Callbacks::FilesChanged onFilesChanged;
};

void closeDirectoryHandle(HANDLE hDirectory)
{
   if (!::CloseHandle(hDirectory))
      LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));
}

void cleanupContext(FileEventContext* pContext)
{
   if (pContext->hDirectory != NULL)
   {
      if (!::CancelIo(pContext->hDirectory))
         LOG_ERROR(systemError(::GetLastError(), ERROR_LOCATION));

      closeDirectoryHandle(pContext->hDirectory);

      pContext->hDirectory = NULL;
   }
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

   // bail if there is no parent (we never expect this to occur so log it)
   if (parentIt == pTree->end())
   {
      LOG_WARNING_MESSAGE("Unable to find parent: " +
                          parentFileInfo.absolutePath());
      return;
   }

   // handle the various types of actions
   FileInfo fileInfo(filePath);
   switch(action)
   {
      case FILE_ACTION_ADDED:
      case FILE_ACTION_RENAMED_NEW_NAME:
      {
         FileChangeEvent event(FileChangeEvent::FileAdded, fileInfo);
         Error error = impl::processFileAdded(parentIt,
                                              event,
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
         impl::processFileRemoved(parentIt, event, pTree, pFileChanges);
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

      // process the file change
      processFileChange(fileNotify.Action,
                        filePath,
                        &(pContext->fileTree),
                        &fileChanges);

      // break or advance to next notification as necessary
      if (!fileNotify.NextEntryOffset)
         break;
      else
         pBuffer += fileNotify.NextEntryOffset;
   };

   // notify client of file changes
   pContext->onFilesChanged(fileChanges);
}

void terminateWithMonitoringError(FileEventContext* pContext,
                                  const Error& error)
{
   pContext->onMonitoringError(error);
   file_monitor::unregisterMonitor((Handle)pContext);
}

// track number of active requests (we wait for this to get to zero before
// allowing the exit of the monitoring thread -- this ensures that we have
// performed full cleanup for all monitoring contexts before exiting.
volatile LONG s_activeRequests = 0;

Error readDirectoryChanges(FileEventContext* pContext);

VOID CALLBACK FileChangeCompletionRoutine(DWORD dwErrorCode,									// completion code
                                          DWORD dwNumberOfBytesTransfered,
                                          LPOVERLAPPED lpOverlapped)
{
   // get the context
   FileEventContext* pContext = (FileEventContext*)(lpOverlapped->hEvent);

   // check for aborted
   if (dwErrorCode == ERROR_OPERATION_ABORTED)
   {
      // decrement the active request counter
      ::InterlockedDecrement(&s_activeRequests);

      // we wait to delete the pContext until here because it owns the
      // OVERLAPPED structure and buffers, and so if we deleted it earlier
      // and the OS tried to access those memory regions we would crash
      delete pContext; 

      return;
   }

   // bail if we don't have an onFilesChanged callback (could have occurred
   // if we encountered an error during file scanning which caused us to
   // fail but then a file notification still snuck through)
   if (!pContext->onFilesChanged)
      return;

   // make sure the root path still exists (if it doesn't then bail)
   if (!pContext->rootPath.exists())
   {
      Error error = fileNotFoundError(pContext->rootPath.absolutePath(),
                                      ERROR_LOCATION);
      terminateWithMonitoringError(pContext, error);
      return;
   }

   // check for buffer overflow
   if(dwNumberOfBytesTransfered == 0)
   {
      // full recursive scan is required
      Error error = impl::discoverAndProcessFileChanges(
                                                  *(pContext->fileTree.begin()),
                                                  true,
                                                  &(pContext->fileTree),
                                                  pContext->onFilesChanged);
      if (error)
         LOG_ERROR(error);

      // read the next change
      error = readDirectoryChanges(pContext);
      if (error)
         terminateWithMonitoringError(pContext, error);

      return;
   }

   // copy to processing buffer (so we can immediately begin another read)
   ::CopyMemory(&(pContext->handlingBuffer[0]),
                &(pContext->receiveBuffer[0]),
                dwNumberOfBytesTransfered);

   // begin the next read -- if this fails then the file change notification
   // is effectively dead in the water
   Error error = readDirectoryChanges(pContext);

   // process file changes
   processFileChanges(pContext, dwNumberOfBytesTransfered);

   // report the (fatal) error if necessary and unregister the monitor
   // (do this here so file notifications are received prior to the error)
   if (error)
      terminateWithMonitoringError(pContext, error);
}

Error readDirectoryChanges(FileEventContext* pContext)
{
   DWORD dwBytes = 0;
   if(!::ReadDirectoryChangesW(pContext->hDirectory,
                               &(pContext->receiveBuffer[0]),
                               pContext->receiveBuffer.size(),
                               TRUE,
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
      return Success();
   }
}

} // anonymous namespace

namespace detail {

// register a new file monitor
Handle registerMonitor(const core::FilePath& filePath,
                       bool recursive,
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

   // open the directory
   pContext->hDirectory = ::CreateFile(
                     filePath.absolutePath().c_str(),
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
      return NULL;
   }

   // initialize overlapped structure to point to our context
   ::ZeroMemory(&(pContext->overlapped), sizeof(OVERLAPPED));
   pContext->overlapped.hEvent = pContext;

   // get the monitoring started
   Error error = readDirectoryChanges(pContext);
   if (error)
   {
      // cleanup
      closeDirectoryHandle(pContext->hDirectory);

      // return error
      callbacks.onRegistrationError(error);

      return NULL;
   }

   // we have passed the pContext into the system so it's ownership will
   // now be governed by the receipt of ERROR_OPERATION_ABORTED within
   // the completion callback
   autoPtrContext.release();

   // increment the number of active requests
   ::InterlockedIncrement(&s_activeRequests);

   // scan the files
   error = scanFiles(FileInfo(filePath), true, &pContext->fileTree);
   if (error)
   {
       // cleanup
       cleanupContext(pContext);

       // return error
       callbacks.onRegistrationError(error);

       return NULL;
   }

   // now that we have finished the file listing we know we have a valid
   // file-monitor so set the callbacks
   pContext->onMonitoringError = callbacks.onMonitoringError;
   pContext->onFilesChanged = callbacks.onFilesChanged;

   // notify the caller that we have successfully registered
   callbacks.onRegistered((Handle)pContext, pContext->fileTree);

   // return the handle
   return (Handle)pContext;
}

// unregister a file monitor
void unregisterMonitor(Handle handle)
{
   // this will end up calling the completion routine with
   // ERROR_OPERATION_ABORTED at which point we'll delete the context
   cleanupContext((FileEventContext*)handle);
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
      ::SleepEx(1000, TRUE);
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

   



