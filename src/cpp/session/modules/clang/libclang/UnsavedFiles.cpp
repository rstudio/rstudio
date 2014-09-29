/*
 * UnsavedFiles.cpp
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

#include "UnsavedFiles.hpp"

#include "LibClang.hpp"

#include <core/StringUtils.hpp>

using namespace core ;

namespace session {
namespace modules { 
namespace clang {
namespace libclang {

namespace {

void freeUnsavedFile(CXUnsavedFile unsavedFile)
{
   delete [] unsavedFile.Filename;
   delete [] unsavedFile.Contents;
}


} // anonymous namespace

UnsavedFiles::~UnsavedFiles()
{
   try
   {
      removeAll();
   }
   catch(...)
   {
   }
}

void UnsavedFiles::update(const std::string& id,
                          const core::FilePath& filePath,
                          const std::string& contents,
                          bool dirty)
{
   // always remove any existing version
   remove(id);

   // add it if it's dirty
   if (dirty)
   {
      // get a regular file path
      std::string path = filePath.absolutePath();

      // allocate an CXUnsavedFile
      CXUnsavedFile unsavedFile;
      char* filename = new char[path.length() + 1];
      std::copy(path.c_str(),
                path.data() + path.length() + 1,
                filename);
      unsavedFile.Filename = filename;
      char* buffContents = new char[contents.length()];
      std::copy(contents.data(),
                contents.data() + contents.length(),
                buffContents);
      unsavedFile.Contents = buffContents;
      unsavedFile.Length = contents.length();

      // add it to the list
      files_.push_back(unsavedFile);
      idToFilename_[id] = filename;
   }
}

void UnsavedFiles::remove(const std::string& id)
{
   // find the filename for this id then remove it from the map
   std::string filename = idToFilename_[id];
   idToFilename_.erase(id);

   if (!filename.empty())
   {
      // scan for an unsaved file with this filename, if we find it
      // then free it and remove it from the vector
      for (std::vector<CXUnsavedFile>::iterator pos = files_.begin();
           pos != files_.end(); ++pos)
      {
         if (pos->Filename == filename)
         {
            freeUnsavedFile(*pos);
            files_.erase(pos);
            break;
         }
      }
   }
}

void UnsavedFiles::removeAll()
{
   // free all of the CXUnsavedFile structures
   std::for_each(files_.begin(), files_.end(), freeUnsavedFile);

   // empty out our data structures
   files_.clear();
   idToFilename_.clear();
}

UnsavedFiles& unsavedFiles()
{
   static UnsavedFiles instance;
   return instance;
}

std::ostream& operator << (std::ostream& ostr, UnsavedFiles& unsaved)
{
   for (std::size_t i = 0; i<unsaved.numUnsavedFiles(); i++)
      ostr << unsaved.unsavedFilesArray()[i].Filename << std::endl;
   return ostr;
}


} // namespace libclang
} // namespace clang
} // namespace modules
} // namesapce session

