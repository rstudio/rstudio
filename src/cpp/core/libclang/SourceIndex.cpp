/*
 * SourceIndex.cpp
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

#include <core/libclang/SourceIndex.hpp>

#include <algorithm>
#include <set>

#include <boost/scoped_ptr.hpp>

#include <gsl/gsl-lite.hpp>

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

unsigned parseTranslationUnitOptions()
{
   return (
      CXTranslationUnit_None |
      CXTranslationUnit_Incomplete |
      CXTranslationUnit_DetailedPreprocessingRecord
   );
}

// With modules enabled (C++20 and newer), libclang refuses a precompiled
// header that wasn't built with the detailed preprocessing record when the
// translation unit including it asks for one, so that option is taken from
// parseTranslationUnitOptions().
unsigned precompiledHeaderOptions()
{
   return (
      CXTranslationUnit_ForSerialization |
      (parseTranslationUnitOptions() & CXTranslationUnit_DetailedPreprocessingRecord)
   );
}

namespace {

bool isHeaderExtension(const std::string& ex)
{
   return ex == ".h" || ex == ".hh" || ex == ".hpp";
}

// fix up '-std=' arguments: remove C++ '-std' flags when compiling
// C sources, and remove C-only '-std' flags when compiling C++ sources
void removeMismatchedStdArgs(const std::string& filename, std::vector<std::string>* pArgs)
{
   if (FilePath(filename).hasExtensionLowerCase(".c"))
   {
      // remove C++ std flags (e.g. -std=c++20) from C compilations
      core::algorithm::expel_if(*pArgs, [](const std::string& arg)
      {
         return arg.find("-std") == 0 && arg.find("++") != std::string::npos;
      });
   }
   else
   {
      // remove C-only std flags (e.g. -std=c11) from C++ compilations
      core::algorithm::expel_if(*pArgs, [](const std::string& arg)
      {
         return arg.find("-std") == 0 && arg.find("++") == std::string::npos;
      });
   }
}

// remove '-include-pch <path>' from the arguments, returning the paths of
// the precompiled headers removed
std::vector<std::string> removePrecompiledHeaderArgs(std::vector<std::string>* pArgs)
{
   const std::string flag = "-include-pch";
   std::vector<std::string> paths;

   auto it = std::find(pArgs->begin(), pArgs->end(), flag);
   while (it != pArgs->end())
   {
      // the flag takes its path as the next argument
      auto last = it + 1;
      if (last != pArgs->end())
      {
         paths.push_back(*last);
         last = last + 1;
      }

      it = pArgs->erase(it, last);
      it = std::find(it, pArgs->end(), flag);
   }

   return paths;
}

// the clang driver finds the macOS SDK through these variables
class SdkEnvironmentScope : boost::noncopyable
{
public:
   SdkEnvironmentScope()
   {
#ifdef __APPLE__
      const char* sdkRootPath("/Library/Developer/CommandLineTools/SDKs/MacOSX.sdk");
      if (core::system::getenv("SDKROOT").empty() && FilePath(sdkRootPath).exists())
         sdkRootScope_.reset(new core::system::EnvironmentScope("SDKROOT", sdkRootPath));

      const char* developerDirPath = "/Library/Developer/CommandLineTools";
      if (core::system::getenv("DEVELOPER_DIR").empty() && FilePath(developerDirPath).exists())
         developerDirScope_.reset(new core::system::EnvironmentScope("DEVELOPER_DIR", developerDirPath));
#endif
   }

private:
   boost::scoped_ptr<core::system::EnvironmentScope> sdkRootScope_;
   boost::scoped_ptr<core::system::EnvironmentScope> developerDirScope_;
};

std::set<std::string>& rejectedPrecompiledHeadersImpl()
{
   static std::set<std::string> instance;
   return instance;
}

} // anonymous namespace

const std::set<std::string>& rejectedPrecompiledHeaders()
{
   return rejectedPrecompiledHeadersImpl();
}

bool takeRejectedPrecompiledHeader(const std::string& path)
{
   return rejectedPrecompiledHeadersImpl().erase(path) > 0;
}

CXTranslationUnit parseWithPrecompiledHeaderFallback(
      CXIndex index,
      const std::string& filename,
      const std::vector<std::string>& args,
      struct CXUnsavedFile* unsavedFiles,
      unsigned numUnsavedFiles,
      unsigned options)
{
   core::system::ProcessArgs argsArray(args);
   CXTranslationUnit tu = clang().parseTranslationUnit(
            index,
            filename.c_str(),
            argsArray.args(),
            gsl::narrow_cast<int>(argsArray.argCount()),
            unsavedFiles,
            numUnsavedFiles,
            options);

   if (tu != nullptr)
      return tu;

   std::vector<std::string> pchFreeArgs = args;
   std::vector<std::string> pchPaths = removePrecompiledHeaderArgs(&pchFreeArgs);
   if (pchPaths.empty())
      return nullptr;

   core::system::ProcessArgs pchFreeArgsArray(pchFreeArgs);
   tu = clang().parseTranslationUnit(
            index,
            filename.c_str(),
            pchFreeArgsArray.args(),
            gsl::narrow_cast<int>(pchFreeArgsArray.argCount()),
            unsavedFiles,
            numUnsavedFiles,
            options);

   // libclang doesn't say why a parse failed (it reports the same error for
   // a bad flag as for a bad precompiled header), so the header is only
   // known to be at fault once the parse works without it. Every full parse
   // naming that header ends up here until it's taken, so say so only when
   // it's first recorded.
   if (tu != nullptr)
   {
      for (const std::string& pchPath : pchPaths)
      {
         if (rejectedPrecompiledHeadersImpl().insert(pchPath).second)
            LOG_WARNING_MESSAGE("libclang could not load precompiled header " + pchPath + "; parsing without it");
      }
   }

   return tu;
}

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
   SdkEnvironmentScope sdkScope;

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

         int ret = clang().reparseTranslationUnit(
                                stored.tu,
                                unsavedFiles().numUnsavedFiles(),
                                unsavedFiles().unsavedFilesArray(),
                                parseTranslationUnitOptions());

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

   // the arguments to parse with. These are kept apart from the arguments
   // as fetched, which are what's stored: the next request compares its own
   // freshly fetched arguments against those to decide whether a reparse
   // will do, and would never match ones adjusted here.
   std::vector<std::string> parseArgs = args;

   // add verbose output if requested
   if (verbose_ >= 3)
     parseArgs.push_back("-v");
   
   removeMismatchedStdArgs(filename, &parseArgs);

   // report to user if requested
   if (verbose_ > 1)
   {
      std::cerr << "# COMPILATION ARGUMENTS ----" << std::endl;
      core::debug::print(parseArgs);
   }

   if (verbose_ > 0)
   {
      std::cerr << "  (Creating new index)" << std::endl;
   }
   
   // create a new translation unit from the file
   CXTranslationUnit tu = parseWithPrecompiledHeaderFallback(
                         index_,
                         filename,
                         parseArgs,
                         unsavedFiles().unsavedFilesArray(),
                         unsavedFiles().numUnsavedFiles(),
                         parseTranslationUnitOptions()
                         );

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

std::vector<std::string> SourceIndex::commandLineDiagnostics(const std::string& filename,
                                                             std::vector<std::string> args)
{
   std::vector<std::string> messages;

   // parse the same way getTranslationUnit() would, but with the file empty
   SdkEnvironmentScope sdkScope;
   removeMismatchedStdArgs(filename, &args);
   core::system::ProcessArgs argsArray(args);

   UnsavedFiles emptyFile;
   emptyFile.update(filename, "", false);

   CXTranslationUnit tu = clang().parseTranslationUnit(
                         index_,
                         filename.c_str(),
                         argsArray.args(),
                         gsl::narrow_cast<int>(argsArray.argCount()),
                         emptyFile.unsavedFilesArray(),
                         emptyFile.numUnsavedFiles(),
                         CXTranslationUnit_None);
   if (tu == nullptr)
      return messages;

   unsigned numDiagnostics = clang().getNumDiagnostics(tu);
   for (unsigned i = 0; i < numDiagnostics; i++)
   {
      Diagnostic diagnostic(clang().getDiagnostic(tu, i));
      if (diagnostic.location().empty())
         messages.push_back(diagnostic.spelling());
   }

   clang().disposeTranslationUnit(tu);
   return messages;
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


