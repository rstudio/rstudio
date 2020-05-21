/*
 * UnsavedFiles.hpp
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

#ifndef CORE_LIBCLANG_UNSAVED_FILES_HPP
#define CORE_LIBCLANG_UNSAVED_FILES_HPP

#include <map>
#include <iosfwd>
#include <vector>
#include <string>

#include <boost/noncopyable.hpp>

#include "clang-c/Index.h"

namespace rstudio {
namespace core {
namespace libclang {

class UnsavedFiles : boost::noncopyable
{
public:
   UnsavedFiles() {}
   virtual ~UnsavedFiles();

   void update(const std::string& filename,
               const std::string& contents,
               bool dirty);
   void remove(const std::string& filename);
   void removeAll();

   CXUnsavedFile* unsavedFilesArray() { return files_.data(); }
   unsigned numUnsavedFiles() { return static_cast<unsigned>(files_.size()); }

private:
   // vector of unsaved files we pass to various clang functions
   std::vector<CXUnsavedFile> files_;
};

//  diagnosic helpers
std::ostream& operator << (std::ostream& ostr, UnsavedFiles& unsaved);


} // namespace libclang
} // namespace core
} // namespace rstudio


#endif // CORE_LIBCLANG_UNSAVED_FILES_HPP
