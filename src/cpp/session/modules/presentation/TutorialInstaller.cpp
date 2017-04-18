/*
 * TutorialInstaller.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 */


#include "TutorialInstaller.hpp"

#include <boost/bind.hpp>
#include <boost/foreach.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/Exec.hpp>

#ifndef _WIN32
#include <core/system/FileMode.hpp>
#endif

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules { 
namespace presentation {

namespace {

bool okayToCopy(const FilePath& sourceFile,
                const FilePath& targetFile,
                bool readonly)
{
   // blast over readonly files if they aren't up to date
   if (readonly)
   {
      return !targetFile.exists() ||
             (sourceFile.lastWriteTime() > targetFile.lastWriteTime());
   }
   // only copy non-readonly files once
   else
   {
      return !targetFile.exists();
   }
}

Error changeFileModeIfNecessary(const FilePath& targetFile, bool readOnly)
{
#ifndef _WIN32
   if (readOnly)
   {
      return core::system::changeFileMode(targetFile,
                                          core::system::EveryoneReadMode);
   }
   else
   {
      return Success();
   }
#else
   return Success();
#endif
}

void doCopyFile(const FilePath& sourceFile,
                const FilePath& targetFile,
                bool readonly)
{
   if (!okayToCopy(sourceFile, targetFile, readonly))
      return;

   FilePath parentDir = targetFile.parent();
   ExecBlock copyBlock;
   copyBlock.addFunctions()
      (boost::bind(&FilePath::ensureDirectory, parentDir))
      (boost::bind(&FilePath::removeIfExists, &targetFile))
      (boost::bind(&FilePath::copy, &sourceFile, targetFile))
      (boost::bind(&changeFileModeIfNecessary, targetFile, readonly));

   Error error = copyBlock.execute();
   if (error)
      LOG_ERROR(error);
}

bool copySourceFile(const FilePath& sourceDir,
                    const FilePath& destDir,
                    const FilePath& sourceFilePath,
                    bool readOnly)
{
   // compute the target path
   std::string relativePath = sourceFilePath.relativePath(sourceDir);
   FilePath targetFilePath = destDir.complete(relativePath);

   // if the copy item is a directory just create it
   if (sourceFilePath.isDirectory())
   {
      Error error = targetFilePath.ensureDirectory();
      if (error)
         LOG_ERROR(error);
   }
   // otherwise copy it
   else
   {
      doCopyFile(sourceFilePath, targetFilePath, readOnly);
   }

   return true;
}

void doCopyDirectory(const FilePath& sourcePath,
                     const FilePath& targetPath,
                     bool readonly)
{
   // create the target directory
   Error error = targetPath.ensureDirectory();
   if (!error)
   {
      // recursively copy files from the source directory
      error = sourcePath.childrenRecursive(
        boost::bind(copySourceFile, sourcePath, targetPath, _2, readonly));
      if (error)
         LOG_ERROR(error);
   }
   else
   {
      LOG_ERROR(error);
   }
}

void installFiles(const FilePath& fromDir,
                  const std::vector<std::string>& globs,
                  const FilePath& toDir,
                  bool readOnly)
{
   // use R to expand the globs
   std::vector<std::string> files;
   {
      RestoreCurrentPathScope restorePath(module_context::safeCurrentPath());
      Error error = fromDir.makeCurrentPath();
      if (error)
      {
         LOG_ERROR(error);
         return;
      }

      error = r::exec::RFunction("Sys.glob", globs).call(&files);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }
   }

   // recursively copy the files
   BOOST_FOREACH(const std::string& file, files)
   {
      FilePath sourceFilePath = fromDir.childPath(file);
      FilePath targetFilePath = toDir.childPath(file);

      if (sourceFilePath.isDirectory())
         doCopyDirectory(sourceFilePath, targetFilePath, readOnly);
      else
         doCopyFile(sourceFilePath, targetFilePath, readOnly);
   }
}


} // anonymous namespace

void installTutorial(const FilePath& tutorialPath,
                     const Tutorial& tutorial,
                     const FilePath& targetPath)
{
   installFiles(tutorialPath, tutorial.installFiles, targetPath, false);
   installFiles(tutorialPath, tutorial.installReadonlyFiles, targetPath, true);
}

} // namespace presentation
} // namespace modules
} // namespace session
} // namespace rstudio

