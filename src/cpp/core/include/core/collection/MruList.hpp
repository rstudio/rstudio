/*
 * MruList.hpp
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
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

#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {

class Error;

namespace collection {

class MruList
{
public:
   MruList(const FilePath& file, size_t maxSize);

   Error initialize();
   void prepend(const std::string& item);
   void append(const std::string& item);
   void remove(const std::string& item);
   void clear();

   size_t size() const;
   std::list<std::string> contents() const;

private:
   void insertItem(const std::string& item, bool prepend);
   void flush();

   FilePath file_;
   size_t maxSize_;

   std::list<std::string> contents_;
};

} // namespace collection
} // namespace core
} // namespace rstudio

#endif /* CORE_MRU_LIST_HPP */
