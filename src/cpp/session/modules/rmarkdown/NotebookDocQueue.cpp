/*
 * NotebookDocQueue.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#include "NotebookDocQueue.hpp"

#include <boost/foreach.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

#define kDocQueueId         "doc_id"
#define kDocQueueJobDesc    "job_desc"
#define kDocQueuePixelWidth "pixel_width"
#define kDocQueueCharWidth  "pixel_width"
#define kDocQueueUnits      "units"

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

NotebookDocQueue::NotebookDocQueue(const std::string& docId, 
      const std::string& jobDesc, int pixelWidth, int charWidth) :
      docId_(docId),
      jobDesc_(jobDesc),
      pixelWidth_(pixelWidth),
      charWidth_(charWidth)
{
}

NotebookDocQueue::NotebookDocQueue()
{
}

boost::shared_ptr<NotebookQueueUnit> NotebookDocQueue::firstUnit()
{
   if (queue_.empty())
      return boost::shared_ptr<NotebookQueueUnit>();
   return *queue_.begin();
}

json::Object NotebookDocQueue::toJson() const
{
   // serialize all the queue units 
   json::Array units;
   BOOST_FOREACH(const boost::shared_ptr<NotebookQueueUnit> unit, queue_) 
   {
      units.push_back(unit->toJson());
   }

   // form JSON object for client
   json::Object queue;
   queue[kDocQueueId]         = docId_;
   queue[kDocQueueJobDesc]    = jobDesc_;
   queue[kDocQueuePixelWidth] = pixelWidth_;
   queue[kDocQueueCharWidth]  = charWidth_;
   queue[kDocQueueUnits]      = units;

   return queue;
}

core::Error NotebookDocQueue::fromJson(const core::json::Object& source, 
   boost::shared_ptr<NotebookDocQueue>* pQueue)
{
   // extract contained unit for manipulation
   NotebookDocQueue& queue = *pQueue->get();
   json::Array units; 
   Error error = json::readObject(source, 
         kDocQueueId,         &queue.docId_,
         kDocQueueJobDesc,    &queue.jobDesc_,
         kDocQueuePixelWidth, &queue.pixelWidth_,
         kDocQueueCharWidth,  &queue.charWidth_,
         kDocQueueUnits,      &units);

   BOOST_FOREACH(const json::Value val, units)
   {
      // ignore non-objects
      if (val.type() != json::ObjectType)
         continue;

      boost::shared_ptr<NotebookQueueUnit> pUnit = 
         boost::make_shared<NotebookQueueUnit>();
      Error error = NotebookQueueUnit::fromJson(val.get_obj(), &pUnit);
      if (error)
         LOG_ERROR(error);
      else
         queue.queue_.push_back(pUnit);
   }

   return Success();
}

bool chunkIdEquals(const boost::shared_ptr<NotebookQueueUnit> unit,
                   const std::string &chunkId)
{
   return unit->chunkId() == chunkId;
}

Error NotebookDocQueue::update(const boost::shared_ptr<NotebookQueueUnit> unit, 
      QueueOperation op, const std::string& before)
{
   std::list<boost::shared_ptr<NotebookQueueUnit> >::iterator it;

   switch(op)
   {
      case QueueAdd:
         // find insertion position
         it = std::find_if(queue_.begin(), queue_.end(), 
               boost::bind(chunkIdEquals, _1, before));
         queue_.insert(it, unit);
         break;

      case QueueUpdate:
         break;

      case QueueDelete:
         queue_.remove_if(boost::bind(chunkIdEquals, _1, unit->chunkId()));
         break;
   }
   return Success();
}

std::string NotebookDocQueue::docId() const
{
   return docId_;
}

int NotebookDocQueue::pixelWidth() const
{
   return pixelWidth_;
}

int NotebookDocQueue::charWidth() const
{
   return charWidth_;
}

bool NotebookDocQueue::complete() const
{
   return queue_.empty();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

