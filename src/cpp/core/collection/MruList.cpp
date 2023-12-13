/*
 * MruList.cpp
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

#include <core/collection/MruList.hpp>

#include <core/FileSerializer.hpp>
#include <core/Log.hpp>


#include <shared_core/Error.hpp>
#include <utility>

namespace rstudio {
namespace core {
namespace collection {

MruList::MruList(const FilePath& file, size_t maxSize, wchar_t dataSeparator) :
   file_(file),
   maxSize_(maxSize),
   haveExtraData_(true),
   separator_(dataSeparator)
{
}

MruList::MruList(const FilePath& file, size_t maxSize) :
   file_(file),
   maxSize_(maxSize),
   haveExtraData_(false)
{
}

void MruList::flush()
{
   Error error = writeCollectionToFile<std::list<std::string>>(file_, contents(), stringifyString);
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

   if (haveExtraData_)
   {
      // Split the items into the unique part and extra data part
      std::list<std::string> uniqueItems;
      for (const std::string& item : contents_)
      {
         std::pair<std::string, std::string> parts = splitItem(item);
         uniqueItems.push_back(parts.first);
         if (!parts.second.empty())
            extraData_[parts.first] = parts.second;
      }
      contents_ = uniqueItems;
   }
   return Success();
}

void MruList::insertItem(std::string item,
                         bool prepend)
{
   std::string extraData;
   if (haveExtraData_)
   {
      std::pair<std::string, std::string> parts = splitItem(item);
      item = parts.first;
      extraData = parts.second;
   }

   // remove item if it already exists
   contents_.remove(item);
   extraData_.erase(item);

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

   if (!extraData.empty())
      extraData_[item] = extraData;

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

void MruList::updateExtraData(const std::string& item, const std::string& extraData)
{
   if (!haveExtraData_)
      return;

   auto it = std::find(contents_.begin(), contents_.end(), item);
   if (it != contents_.end())
   {
      extraData_[item] = extraData;
      flush();
   }
}

void MruList::updateExtraData(const std::string& itemWithExtraData)
{
   auto parts = splitItem(itemWithExtraData);
   updateExtraData(parts.first, parts.second);
}

void MruList::remove(const std::string& item)
{
   if (!haveExtraData_)
      contents_.remove(item);
   else
   {
      std::pair<std::string, std::string> parts = splitItem(item);
      contents_.remove(parts.first);
      extraData_.erase(parts.first);
   }
   flush();
}

void MruList::clear()
{
   contents_.clear();
   extraData_.clear();
   flush();
}

size_t MruList::size() const
{
   return contents_.size();
}

std::list<std::string> MruList::contents() const
{
   if (haveExtraData_)
   {
      std::list<std::string> uniqueItems;
      for (const std::string& item : contents_)
      {
         std::string extraData;
         auto it = extraData_.find(item);
         if (it != extraData_.end())
            extraData = it->second;

         uniqueItems.push_back(joinItem(item, extraData));
      }
      return uniqueItems;
   } else {
      return contents_;
   }
}

std::pair<std::string, std::string> MruList::splitItem(const std::string& item) const
{
   if (!haveExtraData_)
      return std::make_pair(item, "");

   size_t pos = item.find(separator_);
   if (pos != std::string::npos)
      return std::make_pair(item.substr(0, pos), item.substr(pos + 1));
   else
      return std::make_pair(item, "");
}

std::string MruList::joinItem(const std::string& uniquePart, const std::string& extraData) const
{
   if (!haveExtraData_ || extraData.empty())
      return uniquePart;

   std::string separator(1, separator_);
   return uniquePart + separator + extraData;
}

} // namespace collection
} // namespace core
} // namespace rstudio

