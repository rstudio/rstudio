/*
 * UnsavedFiles.hpp
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

#ifndef SESSION_MODULES_CLANG_LIBCLANG_UNSAVED_FILES_HPP
#define SESSION_MODULES_CLANG_LIBCLANG_UNSAVED_FILES_HPP

#include <map>
#include <iosfwd>

#include <boost/noncopyable.hpp>

#include <session/SessionSourceDatabase.hpp>

#include "LibClang.hpp"

namespace session {
namespace modules {      
namespace clang {
namespace libclang {

class UnsavedFiles : boost::noncopyable
{
private:
   // singleton
   friend UnsavedFiles& unsavedFiles();
   UnsavedFiles() {}

public:

   virtual ~UnsavedFiles();

   void update(const std::string& id,
               const std::string& path,
               const std::string& contents,
               bool dirty);
   void update(boost::shared_ptr<source_database::SourceDocument> pDoc);
   void remove(const std::string& id);
   void removeAll();

   CXUnsavedFile* unsavedFilesArray() { return &(files_[0]); }
   unsigned numUnsavedFiles() { return files_.size(); }

private:
   // vector of unsaved files we pass to various clang functions
   std::vector<CXUnsavedFile> files_;

   // track relationship betwween source document ids and
   // filenames so we can remove them from the unsaved files
   // list when they are closed
   std::map<std::string,std::string> idToFilename_;
};

// global instance
UnsavedFiles& unsavedFiles();

//  diagnosic helpers
std::ostream& operator << (std::ostream& ostr, UnsavedFiles& unsaved);


} // namespace libclang
} // namespace clang
} // namepace handlers
} // namesapce session

#endif // SESSION_MODULES_CLANG_LIBCLANG_UNSAVED_FILES_HPP
