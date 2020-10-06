/*
 * FileMonitor.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <list>

#include <boost/bind.hpp>
#include <boost/algorithm/string.hpp>

#include <core/Log.hpp>
#include <shared_core/Error.hpp>
#include <core/Thread.hpp>
#include <core/PeriodicCommand.hpp>

#include <core/system/System.hpp>
#include <core/system/FileScanner.hpp>

#include "FileMonitorImpl.hpp"

// NOTE: the functions below assume case-sensitive file names. this could
// in theory cause us to lose notifications on Win32 and OS X however in
// practice we can't think of an easy way for the user to specify the
// non case-sensitive variant of a file

namespace rstudio {
namespace core {
namespace system {
namespace file_monitor {

namespace {

// track active handles so we can implement unregisterAll and
// activeEventContexts. note that this  list is accessed from
// the platform-specific file-monitor thread (checkForInput and
// catch clause of fileMonitorMainThread. this is a naked pointer
// because it is static and accessed from multiple threads (so
// we don't want it to ever be destructed)
std::list<Handle>* s_pActiveHandles;

void addEvent(FileChangeEvent::Type type,
              const FileInfo& fileInfo,
              std::vector<FileChangeEvent>* pEvents)
{
   pEvents->push_back(FileChangeEvent(type, fileInfo));
}

bool notDirectories(const FileInfo& fileInfo,
                    const std::vector<std::string>& dirNames)
{
   std::string path = fileInfo.absolutePath();

   for (std::size_t i=0; i<dirNames.size(); i++)
   {
      if (fileInfo.isDirectory() && boost::algorithm::ends_with(path,
                                                                dirNames[i]))
      {
         return false;
      }
   }

   return true;
}

std::string prefixString(const std::string& str, char ch)
{
   std::string prefixed;
   prefixed.reserve(str.length() + 1);
   prefixed.append(1, ch);
   prefixed.append(str);
   return prefixed;
}

bool notHidden(const FileInfo& fileInfo)
{
   return !core::system::isHiddenFile(fileInfo);
}

bool shouldTraverse(const FileInfo& fileInfo)
{
   return fileInfo.isDirectory() &&
          !FilePath(fileInfo.absolutePath()).isSymlink();
}

bool sizeAndLastWriteTimeAreEqual(const FileInfo& a, const FileInfo& b)
{
   return a.size() == b.size() && a.lastWriteTime() == b.lastWriteTime();
}

} // anonymous namespace


boost::function<bool(const FileInfo&)> excludeDirectoryFilter(
                                                      const std::string& name)
{
   std::vector<std::string> names;
   names.push_back(name);
   return excludeDirectoriesFilter(names);
}

boost::function<bool(const FileInfo&)> excludeDirectoriesFilter(
                                     const std::vector<std::string>& names)
{
   std::vector<std::string> dirNames;
   std::transform(names.begin(),
                  names.end(),
                  std::back_inserter(dirNames),
                  boost::bind(prefixString, _1, '/'));

   return boost::bind(notDirectories, _1, dirNames);
}

boost::function<bool(const FileInfo&)> excludeHiddenFilter()
{
   return boost::bind(notHidden, _1);
}


// helpers for platform-specific implementations
namespace impl {

Error processFileAdded(
              tree<FileInfo>::iterator parentIt,
              const FileChangeEvent& fileChange,
              bool recursive,
              const boost::function<bool(const FileInfo&)>& filter,
              const boost::function<Error(const FileInfo&)>& onBeforeScanDir,
              tree<FileInfo>* pTree,
              std::vector<FileChangeEvent>* pFileChanges)
{
   // see if this node already exists. if it does then check it for changes
   // (if there are no changes then ignore). we do this because some editors
   // (for example gedit) actually save files in such a way that FileAdded
   // is generated (because they overwrite the old file with a move)
   tree<FileInfo>::sibling_iterator it = impl::findFile(pTree->begin(parentIt),
                                                        pTree->end(parentIt),
                                                        fileChange.fileInfo());
   if (it != pTree->end(parentIt))
   {
      if (fileChange.fileInfo() != *it)
      {
         pTree->replace(it, fileChange.fileInfo());

         // add it to the fileChanges
         pFileChanges->push_back(FileChangeEvent(FileChangeEvent::FileModified,
                                                 fileChange.fileInfo()));
      }
      return Success();

   }

   if (recursive && shouldTraverse(fileChange.fileInfo()))
   {
      tree<FileInfo> subTree;
      FileScannerOptions options;
      options.recursive = true;
      options.yield = true;
      options.filter = filter;
      options.onBeforeScanDir = onBeforeScanDir;
      Error error = scanFiles(fileChange.fileInfo(), options, &subTree);
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
   // search for a child with this path
   tree<FileInfo>::sibling_iterator modIt = impl::findFile(
                                                     pTree->begin(parentIt),
                                                     pTree->end(parentIt),
                                                     fileChange.fileInfo());

   // only generate actions if the data is actually new (win32 file monitoring
   // can generate redundant modified events for save operations as well as
   // when directories are copied and pasted, in which case an add is followed
   // by a modified)
   if ((modIt != pTree->end(parentIt)) &&
       !sizeAndLastWriteTimeAreEqual(fileChange.fileInfo(), *modIt))
   {
      pTree->replace(modIt, fileChange.fileInfo());

      // add it to the fileChanges
      pFileChanges->push_back(fileChange);
   }
}

void processFileRemoved(tree<FileInfo>::iterator parentIt,
                        const FileChangeEvent& fileChange,
                        bool recursive,
                        tree<FileInfo>* pTree,
                        std::vector<FileChangeEvent>* pFileChanges)
{
   // search for a child with this path
   tree<FileInfo>::sibling_iterator remIt = findFile(pTree->begin(parentIt),
                                                     pTree->end(parentIt),
                                                     fileChange.fileInfo());

   // only generate actions if the item was found in the tree
   if (remIt != pTree->end(parentIt))
   {
      // if this is folder then we need to generate recursive
      // remove events, otherwise can just add single event
      if (recursive && shouldTraverse(*remIt))
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
         // use the previous FileInfo for the event payload (since the
         // passed FileInfo might not have a correct value for isDirectory
         // since we couldn't read it from the filesystem)
         pFileChanges->push_back(FileChangeEvent(FileChangeEvent::FileRemoved,
                                                 *remIt));
      }

      // remove it from the tree
      pTree->erase(remIt);
   }
}

Error discoverAndProcessFileChanges(
   const FileInfo& fileInfo,
   bool recursive,
   const boost::function<bool(const FileInfo&)>& filter,
   const boost::function<Error(const FileInfo&)>& onBeforeScanDir,
   tree<FileInfo>* pTree,
   const  boost::function<void(const std::vector<FileChangeEvent>&)>&
                                                               onFilesChanged)
{
   // find this path in our fileTree
   tree<FileInfo>::iterator it = std::find(pTree->begin(),
                                           pTree->end(),
                                           fileInfo);

   // if we don't find it then it may have been excluded by a filter, just bail
   if (it == pTree->end())
      return Success();

   // scan this directory into a new tree which we can compare to the old tree
   tree<FileInfo> subdirTree;
   FileScannerOptions options;
   options.recursive = recursive;
   options.yield = true;
   options.filter = filter;
   options.onBeforeScanDir = onBeforeScanDir;
   Error error = scanFiles(fileInfo, options, &subdirTree);
   if (error)
      return error;

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
      for (const FileChangeEvent& fileChange : childrenFileChanges)
      {
         switch(fileChange.type())
         {
         case FileChangeEvent::FileAdded:
         {
            Error error = processFileAdded(it,
                                           fileChange,
                                           recursive,
                                           filter,
                                           pTree,
                                           &fileChanges);
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
            processFileRemoved(it,
                               fileChange,
                               recursive,
                               pTree,
                               &fileChanges);
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

   return Success();
}

std::list<void*> activeEventContexts()
{
   std::list<void*> contexts;
   std::transform(s_pActiveHandles->begin(),
                  s_pActiveHandles->end(),
                  std::back_inserter(contexts),
                  boost::bind(&Handle::pData, _1));

  return contexts;
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
                       const boost::function<bool(const FileInfo&)>& filter,
                       const Callbacks& callbacks);

// unregister a file monitor
void unregisterMonitor(Handle handle);

// stop the monitor. allows for optinal global cleanup and/or waiting
// for termination state on the monitor thread
void stop();

} // namespace detail


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
                       const boost::function<bool(const FileInfo&)>& filter,
                       const Callbacks& callbacks)
      : type_(Register),
        filePath_(filePath),
        recursive_(recursive),
        filter_(filter),
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
   const boost::function<bool(const FileInfo&)>& filter() const
   {
      return filter_;
   }
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
   boost::function<bool(const FileInfo&)> filter_;
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


void checkForInput()
{
   // wait for up to 250ms for new input (we can't block indefinitely because this
   // code runs within the context of the monitoring thread which also needs to free
   // up so that filesystem change notifications can be received)
   RegistrationCommand command;
   while (registrationCommandQueue().deque(
                              &command,
                              boost::posix_time::milliseconds(250)))
   {
      switch(command.type())
      {
      case RegistrationCommand::Register:
      {
         Handle handle = detail::registerMonitor(command.filePath(),
                                                 command.recursive(),
                                                 command.filter(),
                                                 command.callbacks());
         if (!handle.empty())
            s_pActiveHandles->push_back(handle);
         break;
      }

      case RegistrationCommand::Unregister:
      {
         // first ensure that this handle is active (protects against double
         // unregister, which can occur if we've automatically unregistered
         // as a result of an error or a call to file_monitor::stop)
         std::list<Handle>::iterator it = std::find(s_pActiveHandles->begin(),
                                                    s_pActiveHandles->end(),
                                                    command.handle());
         if (it != s_pActiveHandles->end())
         {
            detail::unregisterMonitor(*it);
            s_pActiveHandles->erase(it);
         }
         break;
      }

      case RegistrationCommand::None:
         break;
      }
   }
}

void fileMonitorThreadMain()
{
   // run the file monitor thread
   bool running = false;
   try
   {
      // first wait until there is at least one command to process
      // (makes us immediately responsive to the first request)
      if (registrationCommandQueue().isEmpty())
         registrationCommandQueue().wait();

      // now run the monitoring thread
      running = true;
      file_monitor::detail::run(boost::bind(checkForInput));
   }
   catch(const boost::thread_interrupted&)
   {
   }
   CATCH_UNEXPECTED_EXCEPTION

   // always clean up (even for unexpected exception case)
   try
   {
      // unregister all active handles. these are direct calls to
      // detail::unregisterMonitor (on the background thread)
      std::for_each(s_pActiveHandles->begin(),
                    s_pActiveHandles->end(),
                    detail::unregisterMonitor);

      // clear the list
      s_pActiveHandles->clear();

      // allow the implementation a chance to stop completely (e.g. may
      // need to wait for pending async operations to complete)
      if (running)
         detail::stop();
   }
   CATCH_UNEXPECTED_EXCEPTION
}

void enqueOnRegistered(const Callbacks& callbacks,
                       Handle handle,
                       const tree<FileInfo>& fileTree)
{
   if (callbacks.onRegistered)
   {
      callbackQueue().enque(boost::bind(callbacks.onRegistered,
                                        handle,
                                        fileTree));
   }
}

void enqueOnRegistrationError(const Callbacks& callbacks, const Error& error)
{
   if (callbacks.onRegistrationError)
   {
      callbackQueue().enque(boost::bind(callbacks.onRegistrationError, error));
   }
}

void enqueOnMonitoringError(const Callbacks& callbacks, const Error& error)
{
   if (callbacks.onMonitoringError)
   {
      callbackQueue().enque(boost::bind(callbacks.onMonitoringError, error));
   }
}

void enqueOnFilesChanged(const Callbacks& callbacks,
                         const std::vector<FileChangeEvent>& fileChanges)
{
   if (callbacks.onFilesChanged)
   {
      callbackQueue().enque(boost::bind(callbacks.onFilesChanged, fileChanges));
   }
}

void enqueOnUnregistered(const Callbacks& callbacks, Handle handle)
{
   if (callbacks.onUnregistered)
   {
      callbackQueue().enque(boost::bind(callbacks.onUnregistered, handle));
   }
}

boost::thread s_fileMonitorThread;

} // anonymous namespace


void initialize()
{
   s_pActiveHandles = new std::list<Handle>();
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
                     const boost::function<bool(const FileInfo&)>& filter,
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
   qCallbacks.onUnregistered = boost::bind(enqueOnUnregistered, callbacks, _1);

   // enque the registration
   registrationCommandQueue().enque(RegistrationCommand(filePath,
                                                        recursive,
                                                        filter,
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

namespace {

bool executeCheckForChanges()
{
   checkForChanges();
   return true;
}

} // anonymous namespace

boost::shared_ptr<ScheduledCommand> checkForChangesCommand(
                       const boost::posix_time::time_duration& interval)
{
   return boost::shared_ptr<ScheduledCommand>(
             new PeriodicCommand(interval, executeCheckForChanges, false));
}

} // namespace file_monitor
} // namespace system
} // namespace core 
} // namespace rstudio

   



