/*
 * MruList.cpp
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

#include <core/collection/MruList.hpp>

#include <core/FileSerializer.hpp>
#include <core/Log.hpp>


#include <shared_core/Error.hpp>

namespace rstudio {
namespace core {
namespace collection {

MruList::MruList(const FilePath& file, size_t maxSize) :
   file_(file),
   maxSize_(maxSize)
{
}

void MruList::flush()
{
   Error error = writeCollectionToFile<std::list<std::string>>(file_, contents_, stringifyString);
   if (error)
      LOG_ERROR(error);
}

Error MruList::initialize()
{
   // create the file if it does not already exist
   Error error = file_.ensureFile();
   if (error)
      return error;

#ifndef _WIN32
   // ensure we have write access to the file
   bool writeable = false;
   error = file_.isWriteable(writeable);
   if (error)
      return error;

   if (!writeable)
      return systemError(boost::system::errc::permission_denied, ERROR_LOCATION);
#endif

   // read the contents
   error = readCollectionFromFile<std::list<std::string>>(file_, &contents_, parseString);
   if (error)
      return error;

   // resize the list if necessary
   if (contents_.size() > maxSize_)
      contents_.resize(maxSize_);

   return Success();
}

void MruList::insertItem(const std::string& item,
                         bool prepend)
{
   // remove item if it already exists
   contents_.remove(item);

   // enforce size constraints
   while (contents_.size() >= maxSize_)
   {
      if (prepend)
         contents_.pop_back();
      else
         contents_.pop_front();
   }

   // do the insert
   if (prepend)
      contents_.push_front(item);
   else
      contents_.push_back(item);

   // update the list
   return flush();
}

void MruList::prepend(const std::string& item)
{
   insertItem(item, true);
}

void MruList::append(const std::string& item)
{
   insertItem(item, false);
}

void MruList::remove(const std::string& item)
{
   contents_.remove(item);
   flush();
}

void MruList::clear()
{
   contents_.clear();
   flush();
}

size_t MruList::size() const
{
   return contents_.size();
}

std::list<std::string> MruList::contents() const
{
   return contents_;
}

} // namespace collection
} // namespace core
} // namespace rstudio

