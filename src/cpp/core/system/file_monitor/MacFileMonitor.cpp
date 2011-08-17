/*
 * MacFileMonitor.cpp
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

#include <core/system/FileMonitor.hpp>

#include <dirent.h>
#include <sys/stat.h>

#include <CoreServices/CoreServices.h>

#include <algorithm>

#include <boost/foreach.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/classification.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FileInfo.hpp>
#include <core/Thread.hpp>

#include <core/system/System.hpp>

namespace core {
namespace system {
namespace file_monitor {

namespace {

int entryFilter(struct dirent *entry)
{
   if (::strcmp(entry->d_name, ".") == 0 || ::strcmp(entry->d_name, "..") == 0)
      return 0;
   else
      return 1;
}

Error scan(tree<FileInfo>* pTree,
           const tree<FileInfo>::iterator_base& node,
           bool recursive)
{
   // clear all existing
   pTree->erase_children(node);

   // create FilePath for root
   FilePath rootPath(node->absolutePath());

   // read directory contents
   struct dirent **namelist;
   int entries = ::scandir(node->absolutePath().c_str(),
                           &namelist,
                           entryFilter,
                           ::alphasort);
   if (entries == -1)
   {
      Error error = systemError(boost::system::errc::no_such_file_or_directory,
                                ERROR_LOCATION);
      error.addProperty("path", node->absolutePath());
      return error;
   }

   // iterate over entries
   for(int i=0; i<entries; i++)
   {
      // get the entry (then free it) and compute the path
      dirent entry = *namelist[i];
      ::free(namelist[i]);
      std::string name(entry.d_name, entry.d_namlen);
      std::string path = rootPath.childPath(name).absolutePath();

      // get the attributes
      struct stat st;
      int res = ::lstat(path.c_str(), &st);
      if (res == -1)
      {
         LOG_ERROR(systemError(errno, ERROR_LOCATION));
         continue;
      }

      // add the correct type of FileEntry
      if ( S_ISDIR(st.st_mode))
      {
         tree<FileInfo>::iterator_base child =
                              pTree->append_child(node, FileInfo(path, true));
         if (recursive)
         {
            Error error = scan(pTree, child, true);
            if (error)
            {
               LOG_ERROR(error);
               continue;
            }
         }
      }
      else
      {
         pTree->append_child(node, FileInfo(path,
                                            false,
                                            st.st_size,
                                            st.st_mtimespec.tv_sec));
      }
   }

   // free the namelist
   ::free(namelist);

   // return success
   return Success();
}

class FileEventContext : boost::noncopyable
{
public:
   FileEventContext() : streamRef(NULL) {}
   virtual ~FileEventContext() {}
   FSEventStreamRef streamRef;
   tree<FileInfo> fileTree;
   Callbacks::FilesChanged onFilesChanged;
};

template<typename PreviousIterator, typename CurrentIterator>
void collectFileChangeEvents(PreviousIterator prevBegin,
                             PreviousIterator prevEnd,
                             CurrentIterator currentBegin,
                             CurrentIterator currentEnd,
                             std::vector<FileChangeEvent>* pEvents)
{
   // sort both regions so we can use std::set_difference & std::set_intersection
   std::vector<FileInfo> sortedPrev;
   std::copy(prevBegin, prevEnd, std::back_inserter(sortedPrev));
   std::sort(sortedPrev.begin(), sortedPrev.end(), compareFileInfoPaths);
   std::vector<FileInfo> sortedCurrent;
   std::copy(currentBegin, currentEnd, std::back_inserter(sortedCurrent));
   std::sort(sortedCurrent.begin(), sortedCurrent.end(), compareFileInfoPaths);

   // find removed files
   std::vector<FileInfo> removedFiles ;
   std::set_difference(sortedPrev.begin(),
                       sortedPrev.end(),
                       sortedCurrent.begin(),
                       sortedCurrent.end(),
                       std::back_inserter(removedFiles),
                       compareFileInfoPaths);
   // enque removed events
   BOOST_FOREACH(const FileInfo& fileInfo, removedFiles)
   {
       pEvents->push_back(FileChangeEvent(FileChangeEvent::FileRemoved, fileInfo));
   }

   // find added files
   std::vector<FileInfo> addedFiles;
   std::set_difference(sortedCurrent.begin(),
                       sortedCurrent.end(),
                       sortedPrev.begin(),
                       sortedPrev.end(),
                       std::back_inserter(addedFiles),
                       compareFileInfoPaths);
   // enque added events
   BOOST_FOREACH(const FileInfo& fileInfo, addedFiles)
   {
      pEvents->push_back(FileChangeEvent(FileChangeEvent::FileAdded, fileInfo));
   }

   // get the subset of files in both lists and then compare for modification
   std::vector<FileInfo> commonCurrentFiles, commonPrevFiles;
   std::set_intersection(sortedCurrent.begin(),
                         sortedCurrent.end(),
                         sortedPrev.begin(),
                         sortedPrev.end(),
                         std::back_inserter(commonCurrentFiles),
                         compareFileInfoPaths);
   std::set_intersection(sortedPrev.begin(),
                         sortedPrev.end(),
                         sortedCurrent.begin(),
                         sortedCurrent.end(),
                         std::back_inserter(commonPrevFiles),
                         compareFileInfoPaths);
   // enque modified events
   for (std::size_t i=0; i<commonCurrentFiles.size(); i++)
   {
      if (commonCurrentFiles[i].lastWriteTime() !=
          commonPrevFiles[i].lastWriteTime())
      {
          pEvents->push_back(FileChangeEvent(FileChangeEvent::FileModified,
                                             commonCurrentFiles[i]));
      }
   }
}

void addEvent(FileChangeEvent::Type type,
              const FileInfo& fileInfo,
              std::vector<FileChangeEvent>* pEvents)
{
   pEvents->push_back(FileChangeEvent(type, fileInfo));
}


Error processAdded(tree<FileInfo>::iterator parentIt,
                   const FileChangeEvent& fileChange,
                   FileEventContext* pContext,
                   std::vector<FileChangeEvent>* pFileChanges)
{
   if (fileChange.fileInfo().isDirectory())
   {
      tree<FileInfo> subTree;
      Error error = scan(&subTree,
                         subTree.set_head(fileChange.fileInfo()),
                         true);
      if (error)
         return error;

      // merge in the sub-tree
      tree<FileInfo>::sibling_iterator addedIter =
         pContext->fileTree.append_child(parentIt, fileChange.fileInfo());
      pContext->fileTree.insert_subtree_after(addedIter,
                                              subTree.begin());
      pContext->fileTree.erase(addedIter);

      // generate events
      std::for_each(subTree.begin(),
                    subTree.end(),
                    boost::bind(addEvent,
                                FileChangeEvent::FileAdded,
                                _1,
                                pFileChanges));
   }
   else
   {
       pContext->fileTree.append_child(parentIt, fileChange.fileInfo());
       pFileChanges->push_back(fileChange);
   }

   return Success();
}

void processModified(tree<FileInfo>::iterator parentIt,
                     const FileChangeEvent& fileChange,
                     FileEventContext* pContext,
                     std::vector<FileChangeEvent>* pFileChanges)
{
   tree<FileInfo>::sibling_iterator modIt =
         std::find_if(
            pContext->fileTree.begin(parentIt),
            pContext->fileTree.end(parentIt),
            boost::bind(fileInfoHasPath,
                        _1,
                        fileChange.fileInfo().absolutePath()));
   if (modIt != pContext->fileTree.end(parentIt))
      pContext->fileTree.replace(modIt, fileChange.fileInfo());

   // add it to the fileChanges
   pFileChanges->push_back(fileChange);
}

void processRemoved(tree<FileInfo>::iterator parentIt,
                    const FileChangeEvent& fileChange,
                    FileEventContext* pContext,
                    std::vector<FileChangeEvent>* pFileChanges)
{
   // find the item in the current tree
   tree<FileInfo>::sibling_iterator remIt =
         std::find(pContext->fileTree.begin(parentIt),
                   pContext->fileTree.end(parentIt),
                   fileChange.fileInfo());

   if (remIt != pContext->fileTree.end(parentIt))
   {
      // if this is folder then we need to generate recursive
      // remove events, otherwise can just add single event
      if (remIt->isDirectory())
      {
         tree<FileInfo> subTree(remIt);
         std::for_each(subTree.begin(),
                       subTree.end(),
                       boost::bind(addEvent,
                                   FileChangeEvent::FileRemoved,
                                   _1,
                                   pFileChanges));
      }
      else
      {
         pFileChanges->push_back(fileChange);
      }

      // remove it from the tree
      pContext->fileTree.erase(remIt);
   }


}


Error processFileChanges(const FileInfo& fileInfo,
                         bool recursive,
                         FileEventContext* pContext)
{
   // scan this directory into a new tree which we can compare to the old tree
   tree<FileInfo> subdirTree;
   Error error = scan(&subdirTree, subdirTree.set_head(fileInfo), recursive);
   if (error)
      return error;

   // find this path in our fileTree
   tree<FileInfo>::iterator it = std::find(pContext->fileTree.begin(),
                                           pContext->fileTree.end(),
                                           fileInfo);
   if (it != pContext->fileTree.end())
   {
      // handle recursive vs. non-recursive scan differnetly
      if (recursive)
      {
         // check for changes on full subtree
         std::vector<FileChangeEvent> fileChanges;
         tree<FileInfo> existingSubtree(it);
         collectFileChangeEvents(existingSubtree.begin(),
                                 existingSubtree.end(),
                                 subdirTree.begin(),
                                 subdirTree.end(),
                                 &fileChanges);

         // fire events
         pContext->onFilesChanged(fileChanges);

         // wholesale replace subtree
         pContext->fileTree.insert_subtree_after(it, subdirTree.begin());
         pContext->fileTree.erase(it);
      }
      else
      {
         // scan for changes on just the children
         std::vector<FileChangeEvent> childrenFileChanges;
         collectFileChangeEvents(pContext->fileTree.begin(it),
                                 pContext->fileTree.end(it),
                                 subdirTree.begin(subdirTree.begin()),
                                 subdirTree.end(subdirTree.begin()),
                                 &childrenFileChanges);

         // build up actual file changes and mutate the tree as appropriate
         std::vector<FileChangeEvent> fileChanges;
         BOOST_FOREACH(const FileChangeEvent& fileChange, childrenFileChanges)
         {
            switch(fileChange.type())
            {
            case FileChangeEvent::FileAdded:
            {
               Error error = processAdded(it, fileChange, pContext, &fileChanges);
               if (error)
                  LOG_ERROR(error);
               break;
            }
            case FileChangeEvent::FileModified:
            {
               processModified(it, fileChange, pContext, &fileChanges);
               break;
            }
            case FileChangeEvent::FileRemoved:
            {
               processRemoved(it, fileChange, pContext, &fileChanges);
               break;
            }
            case FileChangeEvent::None:
            default:
               break;
            }
         }

         // fire events
         pContext->onFilesChanged(fileChanges);
      }
   }
   else
   {
      LOG_WARNING_MESSAGE("Unable to find treeItem for " +
                          fileInfo.absolutePath());
   }

   return Success();
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

   // bail if we don't have onFilesChanged (we wouldn't if a callback snuck
   // through to us even after we failed to fully initialize the file monitor
   // (e.g. if there was an error during file listing)
   if (!pContext->onFilesChanged)
      return;

   char **paths = (char**)eventPaths;
   for (std::size_t i=0; i<numEvents; i++)
   {
      // make a copy of the path and strip off trailing / if necessary
      std::string path(paths[i]);
      boost::algorithm::trim_right_if(path, boost::algorithm::is_any_of("/"));

      // get FileInfo for this directory
      FileInfo fileInfo(path, true);

      // check for need to do recursive scan
      bool recursive = eventFlags[i] & kFSEventStreamEventFlagMustScanSubDirs;

      // process changes
      Error error = processFileChanges(fileInfo, recursive, pContext);
      if (error)
         LOG_ERROR(error);
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
void registerMonitor(const core::FilePath& filePath, const Callbacks& callbacks)
{
   // allocate file path
   CFStringRef filePathRef = ::CFStringCreateWithFileSystemRepresentation(
                                       kCFAllocatorDefault,
                                       filePath.absolutePath().c_str());
   if (filePathRef == NULL)
   {
      callbacks.onRegistrationError(systemError(
                                       boost::system::errc::not_enough_memory,
                                       ERROR_LOCATION));
      return;
   }
   CFRefScope filePathRefScope(filePathRef);

   // allocate paths array
   CFArrayRef pathsArrayRef = ::CFArrayCreate(kCFAllocatorDefault,
                                              (const void **)&filePathRef,
                                              1,
                                              NULL);
   if (pathsArrayRef == NULL)
   {
      callbacks.onRegistrationError(systemError(
                                       boost::system::errc::not_enough_memory,
                                       ERROR_LOCATION));
      return;
   }
   CFRefScope pathsArrayRefScope(pathsArrayRef);


   // create and allocate FileEventContext (create auto-ptr in case we
   // return early, we'll call release later before returning)
   FileEventContext* pContext = new FileEventContext();
   std::auto_ptr<FileEventContext> autoPtrContext(pContext);
   FSEventStreamContext context;
   context.version = 0;
   context.info = (void*) pContext;
   context.retain = NULL;
   context.release = NULL;
   context.copyDescription = NULL;

   // create the stream and save a reference to it
   pContext->streamRef = ::FSEventStreamCreate(
                  kCFAllocatorDefault,
                  &fileEventCallback,
                  &context,
                  pathsArrayRef,
                  kFSEventStreamEventIdSinceNow,
                  1,
                  kFSEventStreamCreateFlagNoDefer);
   if (pContext->streamRef == NULL)
   {
      callbacks.onRegistrationError(systemError(
                                       boost::system::errc::no_stream_resources,
                                       ERROR_LOCATION));
      return;
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
      return;

   }

   // scan the files
   Error error = scan(&pContext->fileTree,
                      pContext->fileTree.set_head(FileInfo(filePath)),
                      true);
   if (error)
   {
       // stop, invalidate, release
       stopInvalidateAndReleaseEventStream(pContext->streamRef);

       // return error
       callbacks.onRegistrationError(error);
       return;
   }

   // now that we have finished the file listing we know we have a valid
   // file-monitor so set the onFilesChanged callback so that the
   // client can receive events
   pContext->onFilesChanged = callbacks.onFilesChanged;

   // we are going to pass the context pointer to the client (as the Handle)
   // so we release it here to relinquish ownership
   autoPtrContext.release();

   // notify the caller that we have successfully registered
   callbacks.onRegistered((Handle)pContext, pContext->fileTree);
}

// unregister a file monitor
void unregisterMonitor(Handle handle)
{
   // cast to context
   FileEventContext* pContext = (FileEventContext*)handle;

   // stop, invalidate, release
   stopInvalidateAndReleaseEventStream(pContext->streamRef);

   // delete context
   delete pContext;
}

void run(const boost::function<void()>& checkForInput)
{
   // ensure we have a run loop for this thread (not sure if this is
   // strictly necessary but it is not harmful)
   ::CFRunLoopGetCurrent();

   while (true)
   {
      // process the run loop for 1 second
      SInt32 reason = ::CFRunLoopRunInMode(kCFRunLoopDefaultMode, 1, false);

      // if we were stopped then break
      if (reason == kCFRunLoopRunStopped)
         break;

      // if there is nothing the run loop then sleep for 250ms (so we don't spin)
      if (reason == kCFRunLoopRunFinished)
         boost::this_thread::sleep(boost::posix_time::milliseconds(250));

      // check for input
      checkForInput();
   }
}

} // namespace detail
} // namespace file_monitor
} // namespace system
} // namespace core 

   



