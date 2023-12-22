/*
 * MruList.hpp
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

#ifndef CORE_MRU_LIST_HPP
#define CORE_MRU_LIST_HPP

#include <cstddef>

#include <list>
#include <string>
#include <utility>

#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {

class Error;

namespace collection {

/**
 * Stores a list of text strings, backed by a file.
 * 
 * The file stores each item on a separate line. If an item with the same text is added to the
 * list, it is removed from its previous position and newly added to the appropriate location
 * (based on the prepend/append method used).
 * 
 * If constructed with a data separator character, the uniqueness of the list items is determined
 * by the text up to (and not including) the separator character. Everything after the separator
 * is ignored for uniqueness purposes but is still stored in the list.
 */
class MruList
{
public:
   MruList(const FilePath& file, size_t maxSize);
   MruList(const FilePath& file, size_t maxSize, wchar_t dataSeparator);

   Error initialize();
   void prepend(const std::string& item);
   void append(const std::string& item);
   void remove(const std::string& item);
   void clear();
   
   // update an item's extra data without changing its position in the list
   void updateExtraData(const std::string& item, const std::string& extraData);
   void updateExtraData(const std::string& itemWithExtraData);

   size_t size() const;
   std::list<std::string> contents() const;

private:
   void insertItem(std::string item, bool prepend);
   void flush();
   std::pair<std::string, std::string> splitItem(const std::string& item) const;
   std::string joinItem(const std::string& uniquePart, const std::string& extraData) const;

   FilePath file_;
   size_t maxSize_;
   bool haveExtraData_;
   wchar_t separator_;

   std::list<std::string> contents_;
   std::map<std::string, std::string> extraData_;
};

} // namespace collection
} // namespace core
} // namespace rstudio

#endif /* CORE_MRU_LIST_HPP */
