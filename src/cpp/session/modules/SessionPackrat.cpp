/*
 * SessionPackrat.cpp
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

#include "SessionPackrat.hpp"

#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/Hash.hpp>
#include <core/system/FileMonitor.hpp>

#include <r/RExec.hpp>
#include <r/session/RClientState.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionModuleContext.hpp>

#include "SessionPackages.hpp"

using namespace core;

namespace session {
namespace modules { 
namespace packrat {

namespace {

enum PackratHashType
{
   HASH_TYPE_LOCKFILE = 0,
   HASH_TYPE_LIBRARY = 1
};

std::string keyOfHashType(PackratHashType hashType)
{
   return hashType == HASH_TYPE_LOCKFILE ?
      "packratLockfileHash" : 
      "packratLibraryHash";
}

std::string getStoredHash(PackratHashType hashType)
{
   json::Value hash = 
      r::session::clientState().getProjectPersistent("packrat",
                                                     keyOfHashType(hashType));
   if (hash.type() == json::StringType) 
      return hash.get_str();
   else
      return "";
}

void setStoredHash(PackratHashType hashType, const std::string& hash)
{
   r::session::clientState().putProjectPersistent(
         "packrat", 
         keyOfHashType(hashType), 
         hash);
}

// adds content from the given file to the given file if it's a 
// DESCRIPTION file (used to summarize library content for hashing)
void addDescContent(int level, const FilePath& path, std::string* pDescContent)
{
   std::string newDescContent;
   if (path.filename() == "DESCRIPTION") 
   {
      Error error = readStringFromFile(path, &newDescContent);
      pDescContent->append(newDescContent);
   }
}

// computes a hash of the content of all DESCRIPTION files in the Packrat
// private library
std::string computeLibraryHash()
{
   FilePath libraryPath = 
      projects::projectContext().directory().complete("packrat/lib");

   // find all DESCRIPTION files in the library and concatenate them to form
   // a hashable state
   std::string descFileContent;
   libraryPath.childrenRecursive(
         boost::bind(addDescContent, _1, _2, &descFileContent));

   if (descFileContent.empty())
      return "";

   return hash::crc32HexHash(descFileContent);
}

// computes the hash of the current project's lockfile
std::string computeLockfileHash()
{
   FilePath lockFilePath = 
      projects::projectContext().directory().complete("packrat/packrat.lock");

   if (!lockFilePath.exists()) 
      return "";

   std::string lockFileContent;
   Error error = readStringFromFile(lockFilePath, &lockFileContent);
   if (error)
   {
      LOG_ERROR(error);
      return "";
   }
   
   return hash::crc32HexHash(lockFileContent);
}

std::string getComputedHash(PackratHashType hashType)
{
   if (hashType == HASH_TYPE_LOCKFILE)
      return computeLockfileHash();
   else
      return computeLibraryHash();
}

void checkHashes(
      PackratHashType primary, 
      PackratHashType secondary, 
      boost::function<void(const std::string&, const std::string&)> onPrimaryMismatch)
{
   std::string oldHash = getStoredHash(primary);
   std::string newHash = getComputedHash(primary);

   // hashes match, no work needed
   if (oldHash == newHash)
      return;

   // primary hashes mismatch, secondary hashes match
   else if (getStoredHash(secondary) == getComputedHash(secondary)) 
   {
      onPrimaryMismatch(oldHash, newHash);
   }

   // primary and secondary hashes mismatch
   else 
   {
      // TODO: invoke status() to see if we're in a consistent state.
      // yes -> update both hashes
      // no -> prompt user
   }
}

void onLockfileUpdate(const std::string& oldHash, const std::string& newHash)
{
   std::cerr << "detected lockfile change (" 
      << oldHash << " -> " << newHash << ")" << std::endl;
   setStoredHash(HASH_TYPE_LOCKFILE, newHash);
}

void onLibraryUpdate(const std::string& oldHash, const std::string& newHash)
{
   std::cerr << "detected library change (" 
      << oldHash << " -> " << newHash << ")" << std::endl;
   setStoredHash(HASH_TYPE_LIBRARY, newHash);
}

void onFileChanged(FilePath sourceFilePath)
{
   FilePath libraryPath = 
      projects::projectContext().directory().complete("packrat/lib");

   if (sourceFilePath.filename() == "packrat.lock")
   {
      checkHashes(HASH_TYPE_LOCKFILE, HASH_TYPE_LIBRARY, onLockfileUpdate);
   }
   else if (sourceFilePath.isWithin(libraryPath)) 
   {
      checkHashes(HASH_TYPE_LIBRARY, HASH_TYPE_LOCKFILE, onLibraryUpdate);
   }
}

void onFilesChanged(const std::vector<core::system::FileChangeEvent>& changes)
{
   BOOST_FOREACH(const core::system::FileChangeEvent& fileChange, changes)
   {
      FilePath changedFilePath(fileChange.fileInfo().absolutePath());
      onFileChanged(changedFilePath);
   }
}
} // anonymous namespace

// returns true if we're in a project and packrat is installed
bool isPackratEligibleProject()
{
   if (!projects::projectContext().hasProject())
      return false;

   if (!module_context::isPackageVersionInstalled("packrat", "0.1.0"))
      return false;

   return true;
}

bool isPackratModeOn()
{
   if (!isPackratEligibleProject())
      return false;

   // if we are in a project, attempt to ascertain whether Packrat mode is on
   // for the project (it's OK if this fails; by default we presume packrat
   // mode to be off)
   bool packratMode = false;
   FilePath dir = projects::projectContext().directory();
   r::exec::RFunction("packrat:::isPackratModeOn", 
                      dir.absolutePath()).call(&packratMode);
   return packratMode;
}

bool isPackratManagedRPackage()
{
   if (!isPackratEligibleProject())
      return false;

   // get the current working directory
   FilePath dir = projects::projectContext().directory();

   // bail if this isn't a package
   if (!core::r_util::isPackageDirectory(dir))
      return false;

   // check if the project is packified
   bool isPackratProject;
   r::exec::RFunction("packrat:::checkPackified",
                      /* projDir = */ dir.absolutePath(),
                      /* silent = */ true).call(&isPackratProject);
   return isPackratProject;
}

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   // listen for changes to the project files 
   session::projects::FileMonitorCallbacks cb;
   cb.onFilesChanged = onFilesChanged;
   projects::projectContext().subscribeToFileMonitor("Packrat", cb);
   module_context::events().onSourceEditorFileSaved.connect(onFileChanged);

   ExecBlock initBlock;

   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionPackrat.R"));

   return initBlock.execute();
}

} // namespace packrat
} // namespace modules
} // namespace session

