/*
 * SourceIndex.cpp
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

#include <core/libclang/SourceIndex.hpp>

#include <boost/scoped_ptr.hpp>

#include <gsl/gsl>

#include <core/Debug.hpp>
#include <core/Log.hpp>
#include <core/PerformanceTimer.hpp>

#include <core/system/Environment.hpp>
#include <core/system/ProcessArgs.hpp>

#include <core/libclang/LibClang.hpp>
#include <core/libclang/UnsavedFiles.hpp>

#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {
namespace libclang {

namespace {

inline unsigned applyTranslationUnitOptions(unsigned defaultOptions)
{
   // for now just reflect back the defaults
   return defaultOptions;
}

bool isHeaderExtension(const std::string& ex)
{
   return ex == ".h" || ex == ".hh" || ex == ".hpp";
}

} // anonymous namespace

bool SourceIndex::isSourceFile(const FilePath& filePath)
{
   std::string ex = filePath.getExtensionLowerCase();
   return  isHeaderExtension(ex) ||
           ex == ".c" || ex == ".cc" || ex == ".cpp" ||
           ex == ".m" || ex == ".mm";
}

bool SourceIndex::isSourceFile(const std::string& filename)
{
   return isSourceFile(FilePath(filename));
}

bool SourceIndex::isHeaderFile(const FilePath& filePath)
{
   return isHeaderExtension(filePath.getExtensionLowerCase());
}

SourceIndex::SourceIndex(CompilationDatabase compilationDB, int verbose)
{
   verbose_ = verbose;
   index_ = clang().createIndex(0, (verbose_ > 0) ? 1 : 0);
   compilationDB_ = compilationDB;
}

SourceIndex::~SourceIndex()
{
   try
   {
      // remove all
      removeAllTranslationUnits();

      // dispose the index
      if (index_ != nullptr)
         clang().disposeIndex(index_);
   }
   catch(...)
   {
   }
}

unsigned SourceIndex::getGlobalOptions() const
{
   return clang().CXIndex_getGlobalOptions(index_);
}

void SourceIndex::setGlobalOptions(unsigned options)
{
   clang().CXIndex_setGlobalOptions(index_, options);
}

void SourceIndex::removeTranslationUnit(const std::string& filename)
{
   TranslationUnits::iterator it = translationUnits_.find(filename);
   if (it != translationUnits_.end())
   {
      if (verbose_ > 0)
         std::cerr << "CLANG REMOVE INDEX: " << it->first << std::endl;
      clang().disposeTranslationUnit(it->second.tu);
      translationUnits_.erase(it->first);
   }
}

void SourceIndex::removeAllTranslationUnits()
{
   for(TranslationUnits::const_iterator it = translationUnits_.begin();
       it != translationUnits_.end(); ++it)
   {
      if (verbose_ > 0)
         std::cerr << "CLANG REMOVE INDEX: " << it->first << std::endl;

      clang().disposeTranslationUnit(it->second.tu);
   }

   translationUnits_.clear();
}


void SourceIndex::primeEditorTranslationUnit(const std::string& filename)
{
   // if we have no record of this translation unit then do a first pass
   if (translationUnits_.find(filename) == translationUnits_.end())
      getTranslationUnit(filename);
}

void SourceIndex::reprimeEditorTranslationUnit(const std::string& filename)
{
   // if we have already indexed this translation unit then re-index it
   if (translationUnits_.find(filename) != translationUnits_.end())
      getTranslationUnit(filename);
}


std::map<std::string,TranslationUnit>
                           SourceIndex::getIndexedTranslationUnits()
{
   std::map<std::string,TranslationUnit> units;
   for (TranslationUnits::value_type& t : translationUnits_)
   {
      TranslationUnit unit(t.first, t.second.tu, &unsavedFiles_);
      units.insert(std::make_pair(t.first, unit));
   }
   return units;
}

TranslationUnit SourceIndex::getTranslationUnit(const std::string& filename,
                                                bool alwaysReparse)
{
#ifdef __APPLE__

   // ensure SDK_ROOT is set
   boost::scoped_ptr<core::system::EnvironmentScope> sdkRootScope;
   const char* sdkRootPath("/Library/Developer/CommandLineTools/SDKs/MacOSX.sdk");
   if (core::system::getenv("SDKROOT").empty() && FilePath(sdkRootPath).exists())
      sdkRootScope.reset(new core::system::EnvironmentScope("SDKROOT", sdkRootPath));

   // ensure DEVELOPER_DIR is set
   boost::scoped_ptr<core::system::EnvironmentScope> developerDirScope;
   const char* developerDirPath = "/Library/Developer/CommandLineTools";
   if (core::system::getenv("DEVELOPER_DIR").empty() && FilePath(developerDirPath).exists())
      developerDirScope.reset(new core::system::EnvironmentScope("DEVELOPER_DIR", developerDirPath));

#endif
   
   FilePath filePath(filename);

   boost::scoped_ptr<core::PerformanceTimer> pTimer;
   if (verbose_ > 0)
   {
      std::cerr << "CLANG INDEXING: " << filePath.getAbsolutePath() << std::endl;
      pTimer.reset(new core::PerformanceTimer(filePath.getFilename()));
   }

   // get the arguments and last write time for this file
   std::vector<std::string> args;
   if (compilationDB_.compileArgsForTranslationUnit)
   {
      args = compilationDB_.compileArgsForTranslationUnit(filename, true);
      if (args.empty())
         return TranslationUnit();
   }
   std::time_t lastWriteTime = filePath.getLastWriteTime();

   // look it up
   TranslationUnits::iterator it = translationUnits_.find(filename);

   // check for various incremental processing scenarios
   if (it != translationUnits_.end())
   {
      // alias record
      StoredTranslationUnit& stored = it->second;

      // already up to date?
      if (!alwaysReparse &&
          (args == stored.compileArgs) &&
          (lastWriteTime == stored.lastWriteTime))
      {
         if (verbose_ > 0)
            std::cerr << "  (Index already up to date)" << std::endl;
         return TranslationUnit(filename, stored.tu, &unsavedFiles_);
      }

      // just needs reparse?
      else if (args == stored.compileArgs)
      {
         if (verbose_ > 0)
         {
            std::string reason = alwaysReparse ?
                                       "(Forced reparse)" :
                                       "(File changed on disk, reparsing)";

            std::cerr << "  " << reason << std::endl;
         }

         unsigned options = applyTranslationUnitOptions(
                                    clang().defaultReparseOptions(stored.tu));
         int ret = clang().reparseTranslationUnit(
                                stored.tu,
                                unsavedFiles().numUnsavedFiles(),
                                unsavedFiles().unsavedFilesArray(),
                                options);

         if (ret == 0)
         {
            // update last write time
            stored.lastWriteTime = lastWriteTime;

            // return it
            return TranslationUnit(filename, stored.tu, &unsavedFiles_);
         }
         else
         {
            LOG_ERROR_MESSAGE("Error re-parsing translation unit " + filename);
         }
      }
   }

   // if we got this far then there either was no existing translation
   // unit or we require a full rebuild. in all cases remove any existing
   // translation unit we have
   removeTranslationUnit(filename);

   // add verbose output if requested
   if (verbose_ >= 2)
     args.push_back("-v");

   // report to user if requested
   if (verbose_ > 1)
   {
      std::cerr << "COMPILATION ARGUMENTS:" << std::endl;
      core::debug::print(args);
   }

   // get the args in the fashion libclang expects (char**)
   core::system::ProcessArgs argsArray(args);

   if (verbose_ > 0)
   {
      std::cerr << "  (Creating new index)" << std::endl;
   }
   
   // create a new translation unit from the file
   unsigned options = applyTranslationUnitOptions(
                           clang().defaultEditingTranslationUnitOptions());
   
   CXTranslationUnit tu = clang().parseTranslationUnit(
                         index_,
                         filename.c_str(),
                         argsArray.args(),
                         gsl::narrow_cast<int>(argsArray.argCount()),
                         unsavedFiles().unsavedFilesArray(),
                         unsavedFiles().numUnsavedFiles(),
                         options);


   // save and return it if we succeeded
   if (tu != nullptr)
   {
      translationUnits_[filename] = StoredTranslationUnit(args,
                                                          lastWriteTime,
                                                          tu);

      TranslationUnit unit(filename, tu, &unsavedFiles_);
      if (verbose_ > 0)
         unit.printResourceUsage(std::cerr, false);
      return unit;
   }
   else
   {
      LOG_ERROR_MESSAGE("Error parsing translation unit " + filename);
      return TranslationUnit();
   }
}

Cursor SourceIndex::referencedCursorForFileLocation(const FileLocation &loc)
{
   // get the translation unit
   std::string filename = loc.filePath.getAbsolutePath();
   TranslationUnit tu = getTranslationUnit(filename, true);
   if (tu.empty())
      return Cursor();

   // get the cursor
   Cursor cursor = tu.getCursor(filename, loc.line, loc.column);
   if (!cursor.isValid())
      return Cursor();

   // follow reference if we need to
   if (cursor.isReference() || cursor.isExpression())
   {
      cursor = cursor.getReferenced();
      if (!cursor.isValid())
         return Cursor();
   }

   // return the cursor that points to the definition
   return cursor;
}

} // namespace libclang
} // namespace core
} // namespace rstudio


