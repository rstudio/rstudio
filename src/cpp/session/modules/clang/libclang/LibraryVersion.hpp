/*
 * LibraryVersion.hpp
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

#ifndef SESSION_MODULES_CLANG_LIBCLANG_LIBRARY_VERSION_HPP
#define SESSION_MODULES_CLANG_LIBCLANG_LIBRARY_VERSION_HPP

#include <string>

#include <boost/format.hpp>

namespace session {
namespace modules {      
namespace clang {
namespace libclang {

struct LibraryVersion
{
   LibraryVersion() : major_(0), minor_(0), patch_(0) {}
   LibraryVersion(int major, int minor, int patch)
      : major_(major), minor_(minor), patch_(patch)
   {
   }


   bool empty() const { return major_ == 0; }

   int major() const { return major_; }
   int minor() const { return minor_; }
   int patch() const { return patch_; }

   bool operator<(const LibraryVersion& other) const
   {
      if (major_ == other.major_ && minor_ == other.minor_)
         return patch_ < other.patch_;
      else if (major_ == other.major_)
         return minor_ < other.minor_;
      else
         return major_ < other.major_;
   }

   bool operator==(const LibraryVersion& other) const
   {
      return major_ == other.major_ &&
             minor_ == other.minor_ &&
             patch_ == other.patch_;
   }

   bool operator!=(const LibraryVersion& other) const
   {
      return !(*this == other);
   }

   std::string asString() const
   {
      boost::format fmt("%1%.%2%.%3%");
      return boost::str(fmt % major_ % minor_ % patch_);
   }

private:
   const int major_;
   const int minor_;
   const int patch_;
};


} // namespace libclang
} // namespace clang
} // namepace handlers
} // namesapce session

#endif // SESSION_MODULES_CLANG_LIBCLANG_LIBRARY_VERSION_HPP
