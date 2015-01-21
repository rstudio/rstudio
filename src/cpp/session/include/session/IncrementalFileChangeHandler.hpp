/*
 * IncrementalFileChangeHandler.hpp
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

#ifndef SESSION_INCREMENTAL_FILE_CHANGE_HANDLER_HPP
#define SESSION_INCREMENTAL_FILE_CHANGE_HANDLER_HPP

#include <queue>

#include <boost/noncopyable.hpp>
#include <boost/function.hpp>
#include <boost/bind.hpp>

#include <boost/date_time/posix_time/posix_time.hpp>

#include <core/FileInfo.hpp>

#include <core/system/FileChangeEvent.hpp>

#include <session/SessionModuleContext.hpp>

#include <session/projects/SessionProjects.hpp>

namespace rstudio {
namespace session {

class IncrementalFileChangeHandler : boost::noncopyable
{
public:
   typedef boost::function<bool(const core::FileInfo&)> Filter;
   typedef boost::function<void(const core::system::FileChangeEvent&)> Handler;

public:
   IncrementalFileChangeHandler(
         Filter filter,
         Handler handler,
         boost::posix_time::time_duration initialWorkPeriod,
         boost::posix_time::time_duration incrementalWorkPeriod,
         bool idleOnly)
      : processing_(false),
        filter_(filter),
        handler_(handler),
        initialWorkPeriod_(initialWorkPeriod),
        incrementalWorkPeriod_(incrementalWorkPeriod),
        idleOnly_(idleOnly)
   {
   }

   virtual ~IncrementalFileChangeHandler()
   {
   }

   // COPYING: prohibited


   void subscribeToFileMonitor(const std::string& featureName)
   {
      projects::FileMonitorCallbacks cb;
      cb.onMonitoringEnabled = boost::bind(
           &IncrementalFileChangeHandler::onMonitoringEnabled, this, _1);
      cb.onFilesChanged = boost::bind(
               &IncrementalFileChangeHandler::onFilesChanged, this, _1);
      cb.onMonitoringDisabled = boost::bind(
               &IncrementalFileChangeHandler::clear, this);
      projects::projectContext().subscribeToFileMonitor(featureName, cb);
   }

   template <typename ForwardIterator>
   void enqueFiles(ForwardIterator begin, ForwardIterator end)
   {
      // add all files that meet the filter to the queue
      using namespace rstudio::core::system;
      for ( ; begin != end; ++begin)
      {
         if (filter_(*begin))
         {
            FileChangeEvent addEvent(FileChangeEvent::FileAdded, *begin);
            queue_.push(addEvent);
         }
      }

      // schedule processing if necessary. perform an initial chunk of
      // work then continue in smaller chunks until we are completed
      if (!queue_.empty() && !processing_)
      {
         processing_ = true;

         module_context::scheduleIncrementalWork(
                  initialWorkPeriod_,
                  incrementalWorkPeriod_,
                  boost::bind(
                  &IncrementalFileChangeHandler::dequeAndProcess, this),
                  idleOnly_);
      }
   }

   void enqueFileChange(const core::system::FileChangeEvent& event)
   {
      // screen out files which don't pass the filter
      if (!filter_(event.fileInfo()))
         return;

      // add to the queue
      queue_.push(event);

      // schedule processing if necessary. don't process anything immediately
      // (this is to defend against large numbers of files being enqued
      // at once and typing up the main thread). rather, schedule processing
      // to occur in incrementalWorkPeriod_ chunks
      if (!processing_)
      {
         processing_ = true;

         module_context::scheduleIncrementalWork(
             incrementalWorkPeriod_,
             boost::bind(&IncrementalFileChangeHandler::dequeAndProcess, this),
             idleOnly_);
      }
   }

   void clear()
   {
      processing_ = false;
      queue_ = std::queue<core::system::FileChangeEvent>();
   }

private:

   bool dequeAndProcess()
   {
      if (!queue_.empty())
      {
         // remove the event from the queue
         core::system::FileChangeEvent event = queue_.front();
         queue_.pop();

         // process the change
         handler_(event);
      }

      // return status
      processing_ = !queue_.empty();
      return processing_;
   }

   // hooks for file monitor subscription

   void onMonitoringEnabled(const tree<core::FileInfo>& files)
   {
      enqueFiles(files.begin_leaf(), files.end_leaf());
   }

   void onFilesChanged(const std::vector<core::system::FileChangeEvent>& events)
   {
      std::for_each(
        events.begin(),
        events.end(),
        boost::bind(&IncrementalFileChangeHandler::enqueFileChange, this, _1));
   }

private:
   bool processing_;
   std::queue<core::system::FileChangeEvent> queue_;
   Filter filter_;
   Handler handler_;
   boost::posix_time::time_duration initialWorkPeriod_;
   boost::posix_time::time_duration incrementalWorkPeriod_;
   bool idleOnly_;
};


} // namespace session
} // namespace rstudio


#endif // SESSION_INCREMENTAL_FILE_CHANGE_HANDLER_HPP
