/*
 * UnsavedFiles.cpp
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

#include <core/libclang/UnsavedFiles.hpp>

#include <algorithm>

#include <core/StringUtils.hpp>

namespace rstudio {
namespace core {
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

void UnsavedFiles::update(const std::string& filename,
                          const std::string& contents,
                          bool dirty)
{
   // always remove any existing version
   remove(filename);

   // add it if it's dirty
   if (dirty)
   {
      // allocate an CXUnsavedFile
      CXUnsavedFile unsavedFile;
      char* Filename = new char[filename.length() + 1];
      std::copy(filename.c_str(),
                filename.data() + filename.length() + 1,
                Filename);
      unsavedFile.Filename = Filename;
      char* buffContents = new char[contents.length()];
      std::copy(contents.data(),
                contents.data() + contents.length(),
                buffContents);
      unsavedFile.Contents = buffContents;
      unsavedFile.Length = static_cast<unsigned long>(contents.length());

      // add it to the list
      files_.push_back(unsavedFile);
   }
}

void UnsavedFiles::remove(const std::string& filename)
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

void UnsavedFiles::removeAll()
{
   // free all of the CXUnsavedFile structures
   std::for_each(files_.begin(), files_.end(), freeUnsavedFile);

   // empty out our data structures
   files_.clear();
}

std::ostream& operator << (std::ostream& ostr, UnsavedFiles& unsaved)
{
   for (std::size_t i = 0; i<unsaved.numUnsavedFiles(); i++)
      ostr << unsaved.unsavedFilesArray()[i].Filename << std::endl;
   return ostr;
}


} // namespace libclang
} // namespace core
} // namespace rstudio


