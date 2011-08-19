/*
 * FileMonitor.cpp
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

// TODO: see if there are filesystems/scenarios where filemon won't work

// TODO: think more deeply about failure cases during scanning

#include <core/system/FileMonitor.hpp>

#include <list>

#include <boost/bind.hpp>
#include <boost/foreach.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>

#include <core/Thread.hpp>

#include <core/system/FileScanner.hpp>

#include "FileMonitorImpl.hpp"

namespace core {
namespace system {
namespace file_monitor {

namespace {

void addEvent(FileChangeEvent::Type type,
              const FileInfo& fileInfo,
              std::vector<FileChangeEvent>* pEvents)
{
   pEvents->push_back(FileChangeEvent(type, fileInfo));
}

} // anonymous namespace

// helpers for platform-specific implementations
namespace impl {

Error processFileAdded(tree<FileInfo>::iterator parentIt,
                       const FileChangeEvent& fileChange,
                       tree<FileInfo>* pTree,
                       std::vector<FileChangeEvent>* pFileChanges)
{
   if (fileChange.fileInfo().isDirectory())
   {
      tree<FileInfo> subTree;
      Error error = scanFiles(fileChange.fileInfo(),
                              true,
                              &subTree);
      if (error)
         return error;

      // merge in the sub-tree
      tree<FileInfo>::sibling_iterator addedIter =
         pTree->append_child(parentIt, fileChange.fileInfo());
      pTree->insert_subtree_after(addedIter, subTree.begin());
      pTree->erase(addedIter);

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
       pTree->append_child(parentIt, fileChange.fileInfo());
       pFileChanges->push_back(fileChange);
   }

   // sort the container after insert
   pTree->sort(pTree->begin(parentIt),
               pTree->end(parentIt),
               fileInfoPathLessThan,
               false);

   return Success();
}

void processFileModified(tree<FileInfo>::iterator parentIt,
                         const FileChangeEvent& fileChange,
                         tree<FileInfo>* pTree,
                         std::vector<FileChangeEvent>* pFileChanges)
{
   tree<FileInfo>::sibling_iterator modIt =
         std::find_if(
            pTree->begin(parentIt),
            pTree->end(parentIt),
            boost::bind(fileInfoHasPath,
                        _1,
                        fileChange.fileInfo().absolutePath()));
   if (modIt != pTree->end(parentIt))
      pTree->replace(modIt, fileChange.fileInfo());

   // add it to the fileChanges
   pFileChanges->push_back(fileChange);
}

void processFileRemoved(tree<FileInfo>::iterator parentIt,
                        const FileChangeEvent& fileChange,
                        tree<FileInfo>* pTree,
                        std::vector<FileChangeEvent>* pFileChanges)
{
   // find the item in the current tree
   tree<FileInfo>::sibling_iterator remIt =
         std::find(pTree->begin(parentIt),
                   pTree->end(parentIt),
                   fileChange.fileInfo());

   if (remIt != pTree->end(parentIt))
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
      pTree->erase(remIt);
   }


}

Error discoverAndProcessFileChanges(const FileInfo& fileInfo,
                                    bool recursive,
                                    tree<FileInfo>* pTree,
                                    const Callbacks::FilesChanged& onFilesChanged)
{
   // scan this directory into a new tree which we can compare to the old tree
   tree<FileInfo> subdirTree;
   Error error = scanFiles(fileInfo, recursive, &subdirTree);
   if (error)
      return error;

   // find this path in our fileTree
   tree<FileInfo>::iterator it = std::find(pTree->begin(),
                                           pTree->end(),
                                           fileInfo);
   if (it != pTree->end())
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
         onFilesChanged(fileChanges);

         // wholesale replace subtree
         pTree->insert_subtree_after(it, subdirTree.begin());
         pTree->erase(it);
      }
      else
      {
         // scan for changes on just the children
         std::vector<FileChangeEvent> childrenFileChanges;
         collectFileChangeEvents(pTree->begin(it),
                                 pTree->end(it),
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
               Error error = processFileAdded(it, fileChange, pTree, &fileChanges);
               if (error)
                  LOG_ERROR(error);
               break;
            }
            case FileChangeEvent::FileModified:
            {
               processFileModified(it, fileChange, pTree, &fileChanges);
               break;
            }
            case FileChangeEvent::FileRemoved:
            {
               processFileRemoved(it, fileChange, pTree, &fileChanges);
               break;
            }
            case FileChangeEvent::None:
            default:
               break;
            }
         }

         // fire events
         onFilesChanged(fileChanges);
      }
   }
   else
   {
      LOG_WARNING_MESSAGE("Unable to find treeItem for " +
                          fileInfo.absolutePath());
   }

   return Success();
}


} // namespace impl


// these are implemented per-platform
namespace detail {

// run the monitor, calling back checkForInput periodically to see if there are
// new registrations or unregistrations
void run(const boost::function<void()>& checkForInput);

// register a new file monitor
Handle registerMonitor(const core::FilePath& filePath,
                       bool recursive,
                       const Callbacks& callbacks);

// unregister a file monitor
void unregisterMonitor(Handle handle);


} // namespace impl


namespace {

class RegistrationCommand
{
public:
   enum Type { None, Register, Unregister };

public:
   RegistrationCommand()
      : type_(None)
   {
   }

   RegistrationCommand(const core::FilePath& filePath,
                       bool recursive,
                       const Callbacks& callbacks)
      : type_(Register),
        filePath_(filePath),
        recursive_(recursive),
        callbacks_(callbacks)
   {
   }

   explicit RegistrationCommand(Handle handle)
      : type_(Unregister), handle_(handle)
   {
   }

   Type type() const { return type_; }

   const core::FilePath& filePath() const { return filePath_; }
   bool recursive() const { return recursive_; }
   const Callbacks& callbacks() const { return callbacks_; }

   Handle handle() const
   {
      return handle_;
   }

private:
   // command type
   Type type_;

   // register command data
   core::FilePath filePath_;
   bool recursive_;
   Callbacks callbacks_;

   // unregister command data
   Handle handle_;
};

typedef core::thread::ThreadsafeQueue<RegistrationCommand>
                                                      RegistrationCommandQueue;
RegistrationCommandQueue& registrationCommandQueue()
{
   static core::thread::ThreadsafeQueue<RegistrationCommand> instance;
   return instance;
}

typedef core::thread::ThreadsafeQueue<boost::function<void()> > CallbackQueue;
CallbackQueue& callbackQueue()
{
   static core::thread::ThreadsafeQueue<boost::function<void()> > instance;
   return instance;
}


// track active handles so we can implement unregisterAll
std::list<Handle> s_activeHandles;

void checkForInput()
{
   // this function is called from the platform specific thread run-loop
   // so we take as a chance to sleep for 100ms (so we don't spin and so
   // we have a boost thread-interruption point)
   boost::this_thread::sleep(boost::posix_time::milliseconds(100));

   RegistrationCommand command;
   while (registrationCommandQueue().deque(&command))
   {
      switch(command.type())
      {
      case RegistrationCommand::Register:
      {
         Handle handle = detail::registerMonitor(command.filePath(),
                                                 command.recursive(),
                                                 command.callbacks());
         if (handle != NULL)
            s_activeHandles.push_back(handle);
         break;
      }

      case RegistrationCommand::Unregister:
      {
         detail::unregisterMonitor(command.handle());
         s_activeHandles.remove(command.handle());
         break;
      }

      case RegistrationCommand::None:
         break;
      }
   }
}

void unregisterAll()
{
   // make a copy of all active handles so we can unregister them
   // (unregistering mutates the list so that's why we need a copy)
   std::vector<Handle> activeHandles;
   std::copy(s_activeHandles.begin(),
             s_activeHandles.end(),
             std::back_inserter(activeHandles));

   // unregister all
   std::for_each(activeHandles.begin(),
                 activeHandles.end(),
                 detail::unregisterMonitor);
}

void fileMonitorThreadMain()
{
   try
   {
      file_monitor::detail::run(boost::bind(checkForInput));   
   }
   catch(const boost::thread_interrupted& e)
   {
      unregisterAll();
   }
   CATCH_UNEXPECTED_EXCEPTION
}

void enqueOnRegistered(const Callbacks& callbacks,
                       Handle handle,
                       const tree<FileInfo>& fileTree)
{
   callbackQueue().enque(boost::bind(callbacks.onRegistered,
                                     handle,
                                     fileTree));
}

void enqueOnRegistrationError(const Callbacks& callbacks, const Error& error)
{
   callbackQueue().enque(boost::bind(callbacks.onRegistrationError, error));
}

void enqueOnMonitoringError(const Callbacks& callbacks, const Error& error)
{
   callbackQueue().enque(boost::bind(callbacks.onMonitoringError, error));
}

void enqueOnFilesChanged(const Callbacks& callbacks,
                         const std::vector<FileChangeEvent>& fileChanges)
{
   callbackQueue().enque(boost::bind(callbacks.onFilesChanged, fileChanges));
}

boost::thread s_fileMonitorThread;

} // anonymous namespace


void initialize()
{
   core::thread::safeLaunchThread(fileMonitorThreadMain, &s_fileMonitorThread);
}

void stop()
{
   if (s_fileMonitorThread.joinable())
   {
      s_fileMonitorThread.interrupt();

      // wait for for the thread to stop
      if (!s_fileMonitorThread.timed_join(boost::posix_time::seconds(3)))
      {
         LOG_WARNING_MESSAGE("file monitor thread didn't stop on its own");
      }

      s_fileMonitorThread.detach();
   }
}

void registerMonitor(const FilePath& filePath,
                     bool recursive,
                     const Callbacks& callbacks)
{
   // bind a new version of the callbacks that puts them on the callback queue
   Callbacks qCallbacks;
   qCallbacks.onRegistered = boost::bind(enqueOnRegistered, callbacks, _1, _2);
   qCallbacks.onRegistrationError = boost::bind(enqueOnRegistrationError,
                                                callbacks,
                                                _1);
   qCallbacks.onMonitoringError = boost::bind(enqueOnMonitoringError,
                                              callbacks,
                                              _1);
   qCallbacks.onFilesChanged = boost::bind(enqueOnFilesChanged, callbacks, _1);

   // enque the registration
   registrationCommandQueue().enque(RegistrationCommand(filePath,
                                                        recursive,
                                                        qCallbacks));
}

void unregisterMonitor(Handle handle)
{
   registrationCommandQueue().enque(RegistrationCommand(handle));
}

void checkForChanges()
{
   boost::function<void()> callback;
   while (callbackQueue().deque(&callback))
      callback();
}

} // namespace file_monitor
} // namespace system
} // namespace core 

   



