/*
 * SessionCodeSearch.cpp
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

#define RSTUDIO_DEBUG_LABEL "codesearch"
// #define RSTUDIO_ENABLE_DEBUG_MACROS

#include "SessionCodeSearch.hpp"

#include <iostream>
#include <vector>
#include <set>

#include <boost/bind.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/foreach.hpp>
#include <boost/format.hpp>
#include <boost/regex.hpp>
#include <boost/algorithm/string/predicate.hpp>
#include <boost/algorithm/string/split.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/SafeConvert.hpp>
#include <core/collection/Tree.hpp>

#include <core/r_util/RSourceIndex.hpp>

#include <core/system/FileChangeEvent.hpp>
#include <core/system/FileMonitor.hpp>

#include <r/RRoutines.hpp>
#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionAsyncRProcess.hpp>
#include <session/SessionRUtil.hpp>

#include <session/projects/SessionProjects.hpp>

#include "SessionAsyncPackageInformation.hpp"

#include "SessionSource.hpp"
#include "clang/DefinitionIndex.hpp"

#include <core/Macros.hpp>

using namespace rstudio::core ;

namespace rstudio {
namespace session {  
namespace modules {
namespace code_search {

namespace {

bool isWithinIgnoredDirectory(const FilePath& filePath)
{
   // we only index (and ignore) directories within the current project
   if (!projects::projectContext().hasProject())
      return false;
   
   FilePath projDir = projects::projectContext().directory();
   for (FilePath parentPath = filePath.parent();
        !parentPath.empty() && parentPath != projDir;
        parentPath = parentPath.parent())
   {
      // cmake build directory
      if (parentPath.childPath("cmake_install.cmake").exists())
         return true;
      
      std::string filename = parentPath.filename();
      
      // node_modules
      if (filename == "node_modules")
         return true;
      
      // packrat
      if (filename == "packrat" && parentPath.childPath("packrat.lock").exists())
         return true;

      // websites
      if (filename == module_context::websiteOutputDir())
         return true;
   }
   
   return false;
}

bool isGlobalFunctionNamed(const r_util::RSourceItem& sourceItem,
                           const std::string& name)
{
   return sourceItem.braceLevel() == 0 &&
          (sourceItem.type() == r_util::RSourceItem::Function ||
           sourceItem.type() == r_util::RSourceItem::Method) &&
          sourceItem.name() == name;
}

// return if we are past max results
template <typename T>
bool enforceMaxResults(std::size_t maxResults,
                        T* pNames,
                        T* pPaths,
                        bool* pMoreAvailable)
{
   if (pNames->size() >= maxResults)
   {
      *pMoreAvailable = true;
      pNames->resize(maxResults);
      pPaths->resize(maxResults);
      return true;
   }
   else
   {
      return false;
   }
}


// index entries we are managing
struct Entry
{
   explicit Entry()
   {
   }

   explicit Entry(const FileInfo& fileInfo)
      : fileInfo(fileInfo)
   {
   }
   
   Entry(const FileInfo& fileInfo,
         boost::shared_ptr<core::r_util::RSourceIndex> pIndex)
      : fileInfo(fileInfo), pIndex(pIndex)
   {
   }
   
   FileInfo fileInfo;
   boost::shared_ptr<core::r_util::RSourceIndex> pIndex;
   
   bool hasIndex() const { return pIndex.get() != NULL; }
   
   bool operator < (const Entry& other) const
   {
      return core::fileInfoPathLessThan(fileInfo, other.fileInfo);
   }
   
   bool operator == (const Entry& other) const
   {
      return fileInfo == other.fileInfo;
   }
   
   friend bool isSamePath(const Entry& lhs, const Entry& rhs)
   {
      return lhs.fileInfo.absolutePath() ==
             rhs.fileInfo.absolutePath();
   }
};

void print_tree(tree<Entry> const& tr)
{
#ifdef RSTUDIO_ENABLE_DEBUG_MACROS
   std::cerr << "Tree of size " << tr.size() << ".\n";
   tree<Entry>::iterator it = tr.begin();
   for (; it != tr.end(); ++it)
   {
      int depth = tr.depth(it);
      for (int i = 0; i < depth; i++)
         std::cerr << "--";
      std::cerr << (*it).fileInfo.absolutePath() << "\n";
   }
#endif
}

class EntryTree
      : public tree<Entry>
{
public:
   
   EntryTree()
   {
      // NOTE from tree.hh, line 914:
      //
      // If your program fails here you probably used 'append_child' to add the top
      // node to an empty tree. From version 1.45 the top element should be added
      // using 'insert'. See the documentation for further information, and sorry about
      // the API change.
      //
      // We work around this by explicitly inserting a root node on construction.
      Entry dummy(FileInfo("", true));
      insert(begin(), dummy);
   }
   
   // make this BOOST_FOREACH aware
   typedef iterator const_iterator;
   
   const_iterator const_begin() const
   {
      return static_cast<const_iterator>(begin());
   }
   
   const_iterator const_end() const
   {
      return static_cast<const_iterator>(end());
   }
   
   void insertEntry(const Entry& entry)
   {
      std::string absolutePath = entry.fileInfo.absolutePath();
      if (absolutePath.empty())
         return;

      DEBUG("");
      DEBUG("Begin entry insertion: '" << absolutePath << "'");
      
      // construct a tree branch from the path, with each directory forming a new node, e.g
      //
      //   foo/bar/baz
      //
      // becoming
      //
      // foo
      //     --> foo/bar
      //                  --> foo/bar/baz
      iterator parent = begin();
      DEBUG("First parent: '" << (*parent).fileInfo.absolutePath() << "'");

      std::string::size_type matchIndex = absolutePath.find('/');
      
      DEBUG("Entering directory node insertion phase");
      while (matchIndex != std::string::npos)
      {
         FileInfo path(
                  absolutePath.substr(0, matchIndex), // note: don't include the trailing '/'
                  true);
         
         Entry entry(path);
         DEBUG("Entry: '" << entry.fileInfo.absolutePath() << "'");

         if (isSamePath(*parent, entry))
         {
            DEBUG("- Node already exists as parent; skipping...");
         }

         else if (number_of_children(parent) == 0)
         {
            DEBUG("- Inserting new node as first child of '" << (*parent).fileInfo.absolutePath() << "'");

            iterator newItr = append_child(parent, entry);
            DEBUG("- Parent now has " << number_of_children(parent) << " children.");
            DEBUG("- Parent now has " << number_of_siblings(parent) << " siblings.");
            parent = newItr;
         }
         else
         {
            DEBUG("- Searching the children of this node");
            sibling_iterator it = parent.begin();
            sibling_iterator end = parent.end();

            for (; it != end; ++it)
            {
               DEBUG("-- Current node: '" << (*it).fileInfo.absolutePath() << "'");
               if (isSamePath(*it, entry))
               {
                  DEBUG("-- Found it!");
                  break;
               }
            }

            if (it == end)
            {
               DEBUG("- Adding another child to parent '" << (*parent).fileInfo.absolutePath() << "'");
               parent = append_child(parent, entry);
            }
            else
            {
               DEBUG("- Node already exists; setting parent to child (" << (*parent).fileInfo.absolutePath() << ")");
               parent = it;
            }
         }
         
         matchIndex = absolutePath.find('/', matchIndex + 1);
      }
      DEBUG("Exiting directory node insertion phase");
      
      // Now, we have the filename. We append that to the parent.
      DEBUG("Parent: '" << (*parent).fileInfo.absolutePath() << "'");
      DEBUG("Entry: '" << entry.fileInfo.absolutePath() << "'");
      if (parent.number_of_children() == 0)
      {
         DEBUG("- No children at parent node; adding child");
         parent = append_child(parent, entry);
      }

      // We check for a dummy node. If we are inserting a file
      // into a folder with just a dummy node, replace that node.
      else if (parent.number_of_children() == 1 &&
               !entry.fileInfo.isDirectory() &&
               *child(parent, 0) == dummyEntry_)
      {
         DEBUG("- Replacing dummy node");
         iterator firstChild = child(parent, 0);
         *firstChild = entry;
      }

      // Otherwise, loop through all the children and try to find
      // the node. If we don't find it, append it, otherwise no-op.
      else
      {
         sibling_iterator it = parent.begin();
         sibling_iterator end = parent.end();
         for (; it != end; ++it)
         {
            DEBUG("-- Current node: '" << (*it).fileInfo.absolutePath() << "'");
            DEBUG("-- Entry       : '" << entry.fileInfo.absolutePath() << "'");
            
            if (isSamePath(*it, entry))
            {
               DEBUG("- Found it!");
               break;
            }
         }

         if (it == end)
         {
            DEBUG("- Adding another child to parent");
            parent = append_child(parent, entry);
         }
         else
         {
            // We update the entry (because its associated index may
            // have changed)
            *it = entry;
         }
      }

      // If we just inserted a folder, we append an empty child
      // to the node. This is done to optimize lookup time.
      if (entry.fileInfo.isDirectory())
      {
         DEBUG("Inserted empty directory; adding dummy child to folder node");
         parent = append_child(parent, dummyEntry_);
      }
      DEBUG("");

      DEBUG("Tree is now:");
      print_tree(*this);
      DEBUG("");
   }

public:

   iterator find_leaf(const Entry& entry)
   {
      leaf_iterator it = begin_leaf();
      for (; is_valid(it); ++it)
         if (isSamePath(*it, entry))
            return it;
      return this->end();
   }

   iterator find_branch(const Entry& entry)
   {
      iterator parent = begin();
      iterator result = end();
      do_find_branch(entry, parent, &result);
      return result;
   }
   
private:

   void do_find_branch(const Entry& entry,
                       iterator parent,
                       iterator* pResult)
   {
      sibling_iterator it = parent.begin();
      sibling_iterator end = parent.end();
      for (; it != end; ++it)
      {
         DEBUG("- Current branch: '" << (*it).fileInfo.absolutePath() << "'");
         if (isSamePath(*it, entry))
         {
            *pResult = it;
            return;
         }

         if (number_of_children(it) > 0)
         {
            DEBUG("-- Searching children of branch");
            do_find_branch(entry, it, pResult);
         }
      }
   }

public:

   iterator find(const Entry& entry)
   {
      if (entry.fileInfo.isDirectory())
         return find_branch(entry);
      else
         return find_leaf(entry);
   }

private:

   Entry dummyEntry_;
   
};

class SourceFileIndex : boost::noncopyable
{
public:
   SourceFileIndex()
      : pEntries_(new EntryTree()), indexing_(false)
   {
   }

   virtual ~SourceFileIndex()
   {
   }

   // COPYING: prohibited
   
   boost::shared_ptr<core::r_util::RSourceIndex> get(
         const FilePath& filePath)
   {
      Entry entry(core::toFileInfo(filePath));
      EntryTree::iterator it = pEntries_->find(entry);
      if (pEntries_->is_valid(it) && it != pEntries_->end())
      {
         const Entry& entry = *it;
         return entry.pIndex;
      }
      return boost::shared_ptr<core::r_util::RSourceIndex>();
   }

   template <typename ForwardIterator>
   void enqueFiles(ForwardIterator begin, ForwardIterator end)
   {
      // add all files to the indexing queue
      using namespace rstudio::core::system;
      for ( ; begin != end; ++begin)
      {
         FileChangeEvent addEvent(FileChangeEvent::FileAdded, *begin);
         indexingQueue_.push(addEvent);
      }

      // schedule indexing if necessary. perform up to 200ms of work
      // immediately and then continue in periodic 20ms chunks until
      // we are completed.
      if (!indexingQueue_.empty() && !indexing_)
      {
         indexing_ = true;

         module_context::scheduleIncrementalWork(
                           boost::posix_time::milliseconds(200),
                           boost::posix_time::milliseconds(20),
                           boost::bind(&SourceFileIndex::dequeAndIndex, this),
                           false /* allow indexing even when non-idle */);
      }
   }

   void enqueFileChange(const core::system::FileChangeEvent& event)
   {
      // add to the queue
      indexingQueue_.push(event);

      // schedule indexing if necessary. don't index anything immediately
      // (this is to defend against large numbers of files being enqued
      // at once and typing up the main thread). rather, schedule indexing
      // to occur during idle time in 20ms chunks
      if (!indexing_)
      {
         indexing_ = true;

         module_context::scheduleIncrementalWork(
                           boost::posix_time::milliseconds(20),
                           boost::bind(&SourceFileIndex::dequeAndIndex, this),
                           false /* allow indexing even when non-idle */);
      }
   }

   bool findGlobalFunction(const std::string& functionName,
                           const std::set<std::string>& excludeContexts,
                           r_util::RSourceItem* pFunctionItem)
   {
      std::vector<r_util::RSourceItem> sourceItems;
      BOOST_FOREACH(const Entry& entry, *pEntries_)
      {
         // bail if there is no index
         if (!entry.hasIndex())
            continue;

         // bail if this is an exluded context
         if (excludeContexts.find(entry.pIndex->context()) !=
             excludeContexts.end())
         {
            continue;
         }

         // scan the next index
         sourceItems.clear();
         entry.pIndex->search(
                  boost::bind(isGlobalFunctionNamed, _1, functionName),
                  std::back_inserter(sourceItems));

         // return if we got a hit
         if (sourceItems.size() > 0)
         {
            *pFunctionItem = sourceItems[0];
            return true;
         }
      }

      // none found
      return false;
   }

   void searchSource(const std::string& term,
                     std::size_t maxResults,
                     bool prefixOnly,
                     const std::set<std::string>& excludeContexts,
                     std::vector<r_util::RSourceItem>* pItems)
   {
      BOOST_FOREACH(const Entry& entry, *pEntries_)
      {
         // skip if it has no index
         if (!entry.hasIndex())
            continue;

         // bail if this is an exluded context
         if (excludeContexts.find(entry.pIndex->context()) !=
             excludeContexts.end())
         {
            continue;
         }

         // scan the next index
         entry.pIndex->search(term,
                              prefixOnly,
                              false,
                              std::back_inserter(*pItems));

         // return if we are past maxResults
         if (pItems->size() >= maxResults)
         {
            pItems->resize(maxResults);
            return;
         }
      }
   }
   
   template <typename T>
   void searchFiles(const std::string& term,
                    std::size_t maxResults,
                    bool prefixOnly,
                    bool sourceFilesOnly,
                    const FilePath& parentPath,
                    T* pNames,
                    T* pPaths,
                    bool* pMoreAvailable)
   {
      // default to no more available
      *pMoreAvailable = false;

      // create wildcard pattern if the search has a '*'
      boost::regex pattern = regex_utils::regexIfWildcardPattern(term);
      
      // get the start and end iterators -- default to all leaves
      EntryTree::leaf_iterator it = pEntries_->begin_leaf();
      
      DEBUG("Searching for node '" << parentPath.absolutePath());
      Entry parentEntry(core::toFileInfo(parentPath));
      EntryTree::iterator parent = pEntries_->find(parentEntry);
      if (parent != pEntries_->end())
      {
         DEBUG("Found node: '" + (*parent).fileInfo.absolutePath() + "'");
         DEBUG("Node has: '" << pEntries_->number_of_children(parent) << "' children.");
         DEBUG("Node has: '" << pEntries_->number_of_siblings(parent) << "' siblings.");

         it = parent.begin();
      }
      else
      {
         DEBUG("Failed to find node.");
         LOG_ERROR_MESSAGE("Failed to find parent node when searching index");
         return;
      }
      
      // iterate over the files
      for (; pEntries_->is_valid(it); ++it)
      {
         const Entry& entry = *it;
         
         DEBUG("Node: '" << (*it).fileInfo.absolutePath() << "'");
         
         // skip if it's not a source file
         if (sourceFilesOnly && !isSourceFile(entry.fileInfo))
            continue;
         
         // get file and name
         FilePath filePath(entry.fileInfo.absolutePath());
         std::string name = filePath.filename();

         // compare for match (wildcard or standard)
         bool matches = false;
         if (!pattern.empty())
         {
            matches = regex_utils::textMatches(name,
                                               pattern,
                                               prefixOnly,
                                               false);
         }
         else
         {
            if (prefixOnly)
               matches = boost::algorithm::istarts_with(name, term);
            else
            {
               // We allow the user to submit queries of the form e.g.
               // <query>:<row><column>; make sure we only take items
               // on the query up to ':'
               std::string::size_type queryEnd = term.find(":");
               if (queryEnd == std::string::npos)
                  queryEnd = term.length();

               matches = string_utils::isSubsequence(name,
                                                     term,
                                                     queryEnd,
                                                     true);
            }
         }

         // add the file if we found a match
         if (matches)
         {
            // name and aliased path
            pNames->push_back(filePath.filename());
            pPaths->push_back(module_context::createAliasedPath(filePath));

            // return if we are past max results
            if (enforceMaxResults(maxResults, pNames, pPaths, pMoreAvailable))
               return;
         }
      }
   }
   
   template <typename T>
   void searchFolders(const std::string& term,
                      const FilePath& parentPath,
                      std::size_t maxResults,
                      T* pPaths,
                      bool* pMoreAvailable)
   {
      // Find the parent node in the tree
      Entry parentEntry(core::toFileInfo(parentPath));
      EntryTree::iterator parentItr = pEntries_->find(parentEntry);
      if (parentItr == pEntries_->end())
         return;
      
      EntryTree::iterator it = parentItr.begin();
      EntryTree::iterator end = parentItr.end();
      for (; it != end; ++it)
      {
         const FileInfo& fileInfo = (*it).fileInfo;
         if (fileInfo.isDirectory())
         {
            int lastSlashIndex = fileInfo.absolutePath().rfind('/');

            std::string fileName =
                  fileInfo.absolutePath().substr(lastSlashIndex + 1);

            bool isSubsequence =
                  string_utils::isSubsequence(fileName, term, true);

            if (isSubsequence)
            {
               pPaths->push_back(fileInfo.absolutePath());
               if (pPaths->size() >= maxResults)
               {
                  *pMoreAvailable = true;
                  return;
               }
            }

         }
      }
   }

   template <typename T>
   void searchFilesAndFolders(const std::string& term,
                              const FilePath& parentPath,
                              std::size_t maxResults,
                              T* pPaths,
                              bool* pMoreAvailable)
   {
      // Find the parent node in the tree
      Entry parentEntry(core::toFileInfo(parentPath));
      EntryTree::iterator parentItr = pEntries_->find(parentEntry);
      if (parentItr == pEntries_->end())
         return;

      EntryTree::iterator it = parentItr.begin();
      EntryTree::iterator end = parentItr.end();
      for (; it != end; ++it)
      {
         const FileInfo& fileInfo = (*it).fileInfo;

         // Avoid dummy nodes
         if (fileInfo.empty())
            continue;

         int lastSlashIndex = fileInfo.absolutePath().rfind('/');

         std::string fileName =
               fileInfo.absolutePath().substr(lastSlashIndex + 1);

         bool isSubsequence =
               string_utils::isSubsequence(fileName, term, true);

         if (isSubsequence)
         {
            pPaths->push_back(fileInfo.absolutePath());
            if (pPaths->size() >= maxResults)
            {
               *pMoreAvailable = true;
               return;
            }
         }
      }
   }
   
   void walkFiles(const FilePath& parentPath,
                  boost::function<void(const Entry&)> operation,
                  boost::function<bool(const Entry&)> filter = NULL)
   {
      Entry parentEntry(core::toFileInfo(parentPath));
      EntryTree::iterator parentItr = pEntries_->find_branch(parentEntry);
      if (parentItr == pEntries_->end())
      {
         LOG_ERROR_MESSAGE("Failed to find node '" + parentPath.absolutePath() + "'");
         return;
      }
      
      EntryTree::leaf_iterator it = parentItr.begin();
      for (; pEntries_->is_valid(it); ++it)
      {
         if (filter && filter(*it))
            continue;
         
         operation(*it);
      }
   }
   
   void clear()
   {
      indexing_ = false;
      indexingQueue_ = std::queue<core::system::FileChangeEvent>();
      pEntries_->clear();
   }

private:

   bool dequeAndIndex()
   {
      using namespace rstudio::core::system;

      if (!indexingQueue_.empty())
      {
         // remove the event from the queue
         FileChangeEvent event = indexingQueue_.front();
         indexingQueue_.pop();

         // process the change
         const FileInfo& fileInfo = event.fileInfo();
         switch(event.type())
         {
            case FileChangeEvent::FileAdded:
            case FileChangeEvent::FileModified:
            {
               updateIndexEntry(fileInfo);
               break;
            }

            case FileChangeEvent::FileRemoved:
            {
               removeIndexEntry(fileInfo);
               break;
            }

            case FileChangeEvent::None:
               break;
         }
      }

      // return status
      indexing_ = !indexingQueue_.empty();
      return indexing_;
   }

   void updateIndexEntry(const FileInfo& fileInfo)
   {
      // index the source if necessary
      boost::shared_ptr<r_util::RSourceIndex> pIndex;

      // read the file
      FilePath filePath(fileInfo.absolutePath());

      // filter certain directories (e.g. those that exist in build directories)
      if (isWithinIgnoredDirectory(filePath))
         return;

      if (isIndexableSourceFile(fileInfo))
      {
         std::string code;
         Error error = module_context::readAndDecodeFile(
                                 filePath,
                                 projects::projectContext().defaultEncoding(),
                                 true,
                                 &code);
         if (error)
         {
            // log if not path not found error (this can happen if the
            // file was removed after entering the indexing queue)
            if (!core::isPathNotFoundError(error))
            {
               error.addProperty("src-file", filePath.absolutePath());
               LOG_ERROR(error);
            }
            return;
         }

         // add index entry
         std::string context = module_context::createAliasedPath(filePath);
         pIndex.reset(new r_util::RSourceIndex(context, code));
      }

      // attempt to add the entry
      Entry entry(fileInfo, pIndex);
      pEntries_->insertEntry(entry);

      // kick off an update
      r_packages::AsyncPackageInformationProcess::update();
   }

   void removeIndexEntry(const FileInfo& fileInfo)
   {
      // create a fake entry with a null source index to pass to find
      Entry entry(fileInfo, boost::shared_ptr<r_util::RSourceIndex>());

      EntryTree::iterator it = pEntries_->find(entry);
      if (it != pEntries_->end())
         pEntries_->erase(it);
      else
      {
         DEBUG("Failed to remove index entry for file: '" << fileInfo.absolutePath() << "'");
         print_tree(*pEntries_);
      }
   }

   static bool isSourceFile(const FileInfo& fileInfo)
   {
      FilePath filePath(fileInfo.absolutePath());

      // screen directories
      if (!module_context::isUserFile(filePath))
         return false;

      // filter files by name and extension
      std::string ext = filePath.extensionLowerCase();
      std::string filename = filePath.filename();
      return !filePath.isDirectory() &&
              (ext == ".r" || ext == ".rnw" ||
               ext == ".rmd" || ext == ".rmarkdown" ||
               ext == ".rhtml" || ext == ".rd" ||
               ext == ".h" || ext == ".hpp" ||
               ext == ".c" || ext == ".cpp" ||
               ext == ".json" || ext == ".tex" ||
               ext == ".scala" ||
               filename == "DESCRIPTION" ||
               filename == "NAMESPACE" ||
               filename == "README" ||
               filename == "NEWS" ||
               filename == "Makefile" ||
               filename == "configure" ||
               filename == "configure.win" ||
               filename == "cleanup" ||
               filename == "cleanup.win" ||
               filename == "Makevars" ||
               filename == "Makevars.win" ||
               filename == "LICENSE" ||
               filename == "LICENCE" ||
               filePath.hasTextMimeType());
   }

   static bool isIndexableSourceFile(const FileInfo& fileInfo)
   {
      FilePath filePath(fileInfo.absolutePath());
      if (!filePath.isDirectory() && filePath.exists())
      {
         std::string lowerExtension = filePath.extensionLowerCase();
         return
               lowerExtension == ".r" ||
               lowerExtension == ".s" ||
               lowerExtension == ".q";
      }

      return false;

   }
   
public:
   
   boost::shared_ptr<EntryTree> entries() const { return pEntries_; }

private:
   // index entries
   boost::shared_ptr<EntryTree> pEntries_;

   // indexing queue
   bool indexing_;
   std::queue<core::system::FileChangeEvent> indexingQueue_;
};

} // anonymous namespace

void RSourceIndexes::initialize()
{
   source_database::events().onDocUpdated.connect(
       boost::bind(&RSourceIndexes::update, this, _1));
   source_database::events().onDocRemoved.connect(
       boost::bind(&RSourceIndexes::remove, this, _1, _2));
   source_database::events().onRemoveAll.connect(
       boost::bind(&RSourceIndexes::removeAll, this));
}

void RSourceIndexes::update(const boost::shared_ptr<SourceDocument>& pDoc)
{
   // is this indexable? if not then bail
   if (!pDoc->canContainRCode())
      return;

   // index the source
   std::string code;
   Error error = r_utils::extractRCode(pDoc->contents(), pDoc->type(), &code);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   boost::shared_ptr<r_util::RSourceIndex> pIndex(
       new r_util::RSourceIndex(pDoc->path(), code));
   
   // add implicitly available packages
   FilePath filePath = module_context::resolveAliasedPath(pDoc->path());
   std::set<std::string> implicitlyAvailable =
         r_utils::implicitlyAvailablePackages(filePath, pDoc->contents());
   
   BOOST_FOREACH(const std::string& package, implicitlyAvailable)
   {
      pIndex->addInferredPackage(package);
   }
   
   // insert it
   idMap_[pDoc->id()] = pIndex;
   
   // create aliases
   filePathMap_[filePath.absolutePath()] = pIndex;
   
   // kick off an update if necessary
   r_packages::AsyncPackageInformationProcess::update();
}

void RSourceIndexes::remove(const std::string& id, const std::string&)
{
   idMap_.erase(id);

   FilePath filePath;
   Error error = source_database::getPath(id, &filePath);
   if (!error)
      filePathMap_.erase(filePath.absolutePath());
}

void RSourceIndexes::removeAll()
{
   idMap_.clear();
   filePathMap_.clear();
}

RSourceIndexes& rSourceIndex()
{
   static RSourceIndexes instance;
   return instance;
}

namespace {

// if we have a project active then restrict results to the project
bool sourceDatabaseFilter(const r_util::RSourceIndex& index)
{
   if (projects::projectContext().hasProject())
   {
      // get file path
      FilePath docPath = module_context::resolveAliasedPath(index.context());
      return docPath.isWithin(projects::projectContext().directory()) &&
            !isWithinIgnoredDirectory(docPath);
   }
   else
   {
      return !index.context().empty();
   }
}

bool findGlobalFunctionInSourceDatabase(
                        const std::string& functionName,
                        r_util::RSourceItem* pFunctionItem,
                        std::set<std::string>* pContextsSearched)
{
   // get all of the source indexes
   std::vector<boost::shared_ptr<r_util::RSourceIndex> > indexes =
                                                   rSourceIndex().indexes();

   std::vector<r_util::RSourceItem> sourceItems;
   BOOST_FOREACH(boost::shared_ptr<r_util::RSourceIndex>& pIndex, indexes)
   {
      // apply the filter
      if (!sourceDatabaseFilter(*pIndex))
         continue;

      // record this context
      pContextsSearched->insert(pIndex->context());

      // scan the next index
      sourceItems.clear();
      pIndex->search(
               boost::bind(isGlobalFunctionNamed, _1, functionName),
               std::back_inserter(sourceItems));

      // return if we got a hit
      if (sourceItems.size() > 0)
      {
         *pFunctionItem = sourceItems[0];
         return true;
      }
   }

   // none found
   return false;
}

void searchSourceDatabase(const std::string& term,
                          std::size_t maxResults,
                          bool prefixOnly,
                          std::vector<r_util::RSourceItem>* pItems,
                          std::set<std::string>* pContextsSearched)
{
   // get all of the source indexes
   std::vector<boost::shared_ptr<r_util::RSourceIndex> > indexes
                                                = rSourceIndex().indexes();

   BOOST_FOREACH(boost::shared_ptr<r_util::RSourceIndex>& pIndex, indexes)
   {
      // apply the filter
      if (!sourceDatabaseFilter(*pIndex))
         continue;

      // record this context
      pContextsSearched->insert(pIndex->context());

      // scan the source index
      pIndex->search(term,
                     prefixOnly,
                     false,
                     std::back_inserter(*pItems));

      // return if we are past maxResults
      if (pItems->size() >= maxResults)
      {
         pItems->resize(maxResults);
         return;
      }
   }
}

// global source file index
SourceFileIndex s_projectIndex;

} // end anonymous namespace

boost::shared_ptr<r_util::RSourceIndex> getIndexedProjectFile(
      const FilePath& filePath)
{
   return s_projectIndex.get(filePath);
}

void searchSource(const std::string& term,
                  std::size_t maxResults,
                  bool prefixOnly,
                  std::vector<r_util::RSourceItem>* pItems,
                  bool* pMoreAvailable)
{
   // default to no more available
   *pMoreAvailable = false;

   // first search the source database
   std::set<std::string> srcDBContexts;
   searchSourceDatabase(term, maxResults, prefixOnly, pItems, &srcDBContexts);

   // we are done if we had >= maxResults
   if (pItems->size() >= maxResults)
   {
      *pMoreAvailable = true;
      pItems->resize(maxResults);
      return;
   }

   // compute project max results based on existing results
   std::size_t maxProjResults = maxResults - pItems->size();

   // now search the project (excluding contexts already searched in the source db)
   std::vector<r_util::RSourceItem> projItems;
   s_projectIndex.searchSource(term,
                               maxProjResults,
                               prefixOnly,
                               srcDBContexts,
                               &projItems);

   // add project items to the list
   BOOST_FOREACH(const r_util::RSourceItem& sourceItem, projItems)
   {
      // add the item
      pItems->push_back(sourceItem);

      // bail if we've hit the max
      if (pItems->size() >= maxResults)
      {
         *pMoreAvailable = true;
         pItems->resize(maxResults);
         break;
      }
   }
}

namespace {

template <typename T>
void searchSourceDatabaseFiles(const std::string& term,
                               std::size_t maxResults,
                               T* pNames,
                               T* pPaths,
                               bool* pMoreAvailable)
{
   // default to no more available
   *pMoreAvailable = false;

   // create wildcard pattern if the search has a '*'
   boost::regex pattern = regex_utils::regexIfWildcardPattern(term);

   // get all of the source indexes
   std::vector<boost::shared_ptr<r_util::RSourceIndex> > indexes =
                                                   rSourceIndex().indexes();

   BOOST_FOREACH(boost::shared_ptr<r_util::RSourceIndex>& pIndex, indexes)
   {
      // bail if there is no path
      std::string context = pIndex->context();
      if (context.empty())
         continue;

      // get filename
      FilePath filePath = module_context::resolveAliasedPath(context);
      std::string filename = filePath.filename();

      // compare for match (wildcard or standard)
      bool matches = false;
      if (!pattern.empty())
      {
         matches = regex_utils::textMatches(filename,
                                            pattern,
                                            true,
                                            false);
      }
      else
      {
         // Strip everything following a ':'
         std::string::size_type queryEnd = term.find(":");
         if (queryEnd == std::string::npos)
            queryEnd = term.length();

         matches = string_utils::isSubsequence(filename,
                                               term,
                                               queryEnd);
      }

      // add the file if we found a match
      if (matches)
      {
         // name and aliased path
         pNames->push_back(filename);
         pPaths->push_back(pIndex->context());

         // return if we are past max results
         if (enforceMaxResults(maxResults, pNames, pPaths, pMoreAvailable))
            return;
      }

   }
}

template <typename T>
void searchFiles(const std::string& term,
                 std::size_t maxResults,
                 bool sourceFilesOnly,
                 T* pNames,
                 T* pPaths,
                 bool* pMoreAvailable)
{
   // if we have a file monitor then search the project index
   if (session::projects::projectContext().hasFileMonitor())
   {
      s_projectIndex.searchFiles(term,
                                 maxResults,
                                 false,
                                 sourceFilesOnly,
                                 projects::projectContext().directory(),
                                 pNames,
                                 pPaths,
                                 pMoreAvailable);
   }
   else
   {
      searchSourceDatabaseFiles(term,
                                maxResults,
                                pNames,
                                pPaths,
                                pMoreAvailable);
   }
}

// NOTE: When modifying this code, you should ensure that corresponding
// changes are made to the client side scoreMatch function as well
// (See: CodeSearchOracle.java)
int scoreMatch(std::string const& suggestion,
               std::string const& query,
               bool isFile)
{
   // No penalty for perfect matches
   if (suggestion == query)
      return 0;
   
   std::vector<int> matches =
         string_utils::subsequenceIndices(suggestion, query);
   
   int totalPenalty = 0;

   // Loop over the matches and assign a score
   for (int j = 0, n = matches.size(); j < n; j++)
   {
      int matchPos = matches[j];
      int penalty = matchPos;

      // Less penalty if character follows special delim
      if (matchPos >= 1)
      {
         char prevChar = suggestion[matchPos - 1];
         if (prevChar == '_' || prevChar == '-' || (!isFile && prevChar == '.'))
         {
            penalty = j + 1;
         }
      }

      // Less penalty for perfect match (ie, reward case-sensitive match)
      penalty -= suggestion[matchPos] == query[j];
      
      // More penalty for 'uninteresting' files
      if (suggestion == "RcppExports.R" ||
          suggestion == "RcppExports.cpp")
         penalty += 6;
      
      // More penalty for 'uninteresting' extensions (e.g. .Rd)
      std::string extension = string_utils::getExtension(suggestion);
      if (boost::algorithm::to_lower_copy(extension) == ".rd")
         penalty += 6;

      totalPenalty += penalty;
   }
   
   // Penalize files
   if (isFile)
      ++totalPenalty;
   
   // Penalize unmatched characters
   totalPenalty += (query.size() - matches.size()) * query.size();

   return totalPenalty;
}

struct ScorePairComparator
{
   inline bool operator()(const std::pair<int, int> lhs,
                          const std::pair<int, int> rhs)
   {
      return lhs.second < rhs.second;
   }
};

void filterScores(std::vector< std::pair<int, int> >* pScore1,
                  std::vector< std::pair<int, int> >* pScore2,
                  int maxAmount)
{
   int s1_n = pScore1->size();
   int s2_n = pScore2->size();

   int s1Count = 0;
   int s2Count = 0;

   for (int i = 0; i < maxAmount; ++i)
   {
      if (s1Count == s1_n)
      {
         if (s2Count < s2_n)
         {
            ++s2Count;
         }
         continue;
      }
      if (s2Count == s2_n)
      {
         if (s1Count < s1_n)
         {
            ++s1Count;
         }
         continue;
      }

      if ((*pScore1)[s1Count].second <= (*pScore2)[s2Count].second)
      {
         ++s1Count;
      }
      else
      {
         ++s2Count;
      }
   }

   pScore1->resize(s1Count);
   pScore2->resize(s2Count);

}

// uniform representation of source items (spans R and C++, maps to
// SourceItem class on the client side)
class SourceItem
{
public:
   enum Type
   {
      None = 0,
      Function = 1,
      Method = 2,
      Class = 3,
      Enum = 4,
      EnumValue = 5,
      Namespace = 6
   };

   SourceItem()
      : type_(None), line_(-1), column_(-1)
   {
   }

   SourceItem(Type type,
              const std::string& name,
              const std::string& parentName,
              const std::string& extraInfo,
              const std::string& context,
              int line,
              int column)
      : type_(type),
        name_(name),
        parentName_(parentName),
        extraInfo_(extraInfo),
        context_(context),
        line_(line),
        column_(column)
   {
   }

   bool empty() const { return type_ == None; }

   Type type() const { return type_; }
   const std::string& name() const { return name_; }
   const std::string& parentName() const { return parentName_; }
   const std::string& extraInfo() const { return extraInfo_; }
   const std::string& context() const { return context_; }
   int line() const { return line_; }
   int column() const { return column_; }

private:
   Type type_;
   std::string name_;
   std::string parentName_;
   std::string extraInfo_;
   std::string context_;
   int line_;
   int column_;
};

SourceItem fromRSourceItem(const r_util::RSourceItem& rSourceItem)
{
   // calculate type
   using namespace r_util;
   SourceItem::Type type = SourceItem::None;
   switch(rSourceItem.type())
   {
   case RSourceItem::Function:
      type = SourceItem::Function;
      break;
   case RSourceItem::Method:
      type = SourceItem::Method;
      break;
   case RSourceItem::Class:
      type = SourceItem::Class;
      break;
   case RSourceItem::None:
   default:
      type = SourceItem::None;
      break;
   }

   // calcluate extra info
   std::string extraInfo;
   if (rSourceItem.signature().size() > 0)
   {
      extraInfo.append("{");
      for (std::size_t i = 0; i<rSourceItem.signature().size(); i++)
      {
         if (i > 0)
            extraInfo.append(", ");
         extraInfo.append(rSourceItem.signature()[i].type());
      }

      extraInfo.append("}");
   }

   // return source item
   return SourceItem(type,
                     rSourceItem.name(),
                     "",
                     extraInfo,
                     rSourceItem.context(),
                     rSourceItem.line(),
                     rSourceItem.column());
}

SourceItem fromCppDefinition(const clang::CppDefinition& cppDefinition)
{
   // determine type
   using namespace clang;
   SourceItem::Type type = SourceItem::None;
   switch(cppDefinition.kind)
   {
   case CppInvalidDefinition:
      type = SourceItem::None;
      break;
   case CppNamespaceDefinition:
      type = SourceItem::Namespace;
      break;
   case CppClassDefinition:
   case CppStructDefinition:
   case CppTypedefDefinition:
      type = SourceItem::Class;
      break;
   case CppEnumDefinition:
      type = SourceItem::Enum;
      break;
   case CppEnumValue:
      type = SourceItem::EnumValue;
      break;
   case CppFunctionDefinition:
      type = SourceItem::Function;
      break;
   case CppMemberFunctionDefinition:
      type = SourceItem::Method;
      break;
   default:
      type = SourceItem::None;
      break;
   }

   // return source item
   return SourceItem(
      type,
      cppDefinition.name,
      cppDefinition.parentName,
      "",
      module_context::createAliasedPath(cppDefinition.location.filePath),
      safe_convert::numberTo<int>(cppDefinition.location.line, 1),
      safe_convert::numberTo<int>(cppDefinition.location.column, 1));
}

template <typename TValue, typename TFunc>
json::Array toJsonArray(
      const std::vector<SourceItem> &items,
      TFunc memberFunc)
{
   json::Array col;
   std::transform(items.begin(),
                  items.end(),
                  std::back_inserter(col),
                  boost::bind(json::toJsonValue<TValue>,
                                 boost::bind(memberFunc, _1)));
   return col;
}



Error searchCode(const json::JsonRpcRequest& request,
                 json::JsonRpcResponse* pResponse)
{
   // get params
   std::string term;
   int maxResultsInt = 20;
   Error error = json::readParams(request.params, &term, &maxResultsInt);
   if (error)
      return error;
   std::size_t maxResults = safe_convert::numberTo<std::size_t>(maxResultsInt,
                                                                20);

   // object to return
   json::Object result;

   // search files
   std::vector<std::string> names;
   std::vector<std::string> paths;
   bool moreFilesAvailable = false;

   // TODO: Refactor searchSourceFiles, searchSource to no longer take maximum number
   // of results (since we want to grab everything possible then filter before
   // sending over the wire). Simiarly with the 'more*Available' bools
   searchFiles(term, 1E2, true, &names, &paths, &moreFilesAvailable);

   // search source and convert to source items
   std::vector<SourceItem> srcItems;
   std::vector<r_util::RSourceItem> rSrcItems;
   bool moreSourceItemsAvailable = false;
   searchSource(term, 1E2, false, &rSrcItems, &moreSourceItemsAvailable);
   std::transform(rSrcItems.begin(),
                  rSrcItems.end(),
                  std::back_inserter(srcItems),
                  fromRSourceItem);

   // search cpp source and convert to source items
   std::vector<clang::CppDefinition> cppDefinitions;
   clang::searchDefinitions(term, &cppDefinitions);
   std::transform(cppDefinitions.begin(),
                  cppDefinitions.end(),
                  std::back_inserter(srcItems),
                  fromCppDefinition);

   // typedef necessary for BOOST_FOREACH to work with pairs
   typedef std::pair<int, int> PairIntInt;

   // score matches -- returned as a pair, mapping index to score
   std::vector<PairIntInt> fileScores;
   for (std::size_t i = 0; i < paths.size(); ++i)
   {
      fileScores.push_back(std::make_pair(i, scoreMatch(names[i], term, true)));
   }

   // sort by score (lower is better)
   std::sort(fileScores.begin(), fileScores.end(), ScorePairComparator());

   std::vector<PairIntInt> srcItemScores;
   for (std::size_t i = 0; i < srcItems.size(); ++i)
   {
      const SourceItem& item = srcItems[i];
      
      // don't index auto-generated files
      const std::string& context = item.context();
      if (boost::algorithm::ends_with(context, "RcppExports.R") ||
          boost::algorithm::ends_with(context, "RcppExports.cpp"))
         continue;
         
      int score = scoreMatch(item.name(), term, false);
      srcItemScores.push_back(std::make_pair(i, score));
   }
   std::sort(srcItemScores.begin(), srcItemScores.end(), ScorePairComparator());

   // filter so we keep only the top n results -- and proactively
   // update whether there are other entries we didn't report back
   std::size_t srcItemScoresSizeBefore = srcItemScores.size();
   std::size_t fileScoresSizeBefore = fileScores.size();

   filterScores(&fileScores, &srcItemScores, maxResults);

   moreFilesAvailable = fileScoresSizeBefore > fileScores.size();
   moreSourceItemsAvailable = srcItemScoresSizeBefore > srcItemScores.size();

   // get filtered results
   std::vector<std::string> namesFiltered;
   std::vector<std::string> pathsFiltered;
   BOOST_FOREACH(PairIntInt const& pair, fileScores)
   {
      namesFiltered.push_back(names[pair.first]);
      pathsFiltered.push_back(paths[pair.first]);
   }

   std::vector<SourceItem> srcItemsFiltered;
   BOOST_FOREACH(PairIntInt const& pair, srcItemScores)
   {
      srcItemsFiltered.push_back(srcItems[pair.first]);
   }

   // fill result
   json::Object files;
   files["filename"] = json::toJsonArray(namesFiltered);
   files["path"] = json::toJsonArray(pathsFiltered);
   result["file_items"] = files;

   // return rpc array list (wire efficiency)
   json::Object src;
   src["type"] = toJsonArray<int>(srcItemsFiltered, &SourceItem::type);
   src["name"] = toJsonArray<std::string>(srcItemsFiltered, &SourceItem::name);
   src["parent_name"] = toJsonArray<std::string>(srcItemsFiltered, &SourceItem::parentName);
   src["extra_info"] = toJsonArray<std::string>(srcItemsFiltered, &SourceItem::extraInfo);
   src["context"] = toJsonArray<std::string>(srcItemsFiltered, &SourceItem::context);
   src["line"] = toJsonArray<int>(srcItemsFiltered, &SourceItem::line);
   src["column"] = toJsonArray<int>(srcItemsFiltered, &SourceItem::column);
   result["source_items"] = src;

   // set more available bit
   result["more_available"] =
         moreFilesAvailable || moreSourceItemsAvailable;

   pResponse->setResult(result);

   return Success();
}


bool namespaceIsPackage(const std::string& namespaceName,
                        std::string* pPackage)
{
   if (namespaceName.empty())
      return false;

   std::string pkgPrefix("package:");
   std::string::size_type pkgPrefixPos = namespaceName.find(pkgPrefix);
   if (pkgPrefixPos == 0 && namespaceName.length() > pkgPrefix.length())
   {
      *pPackage = namespaceName.substr(pkgPrefix.length());
      return true;
   }
   else
   {
      return false;
   }
}

bool findData(const std::string& name)
{
   std::string found;
   Error error = r::exec::RFunction(".rs.findGlobalData", name).call(&found);
   if (error)
      LOG_ERROR(error);
   return !found.empty();
}

bool findFunction(const std::string& name,
                  const std::string& fromWhere,
                  std::string* pNamespaceName)
{
   // if fromWhere is a package name then we should first directly
   // search that package (so that we can find hidden functions)
   Error error;
   std::string pkgName;
   pNamespaceName->clear();
   if (namespaceIsPackage(fromWhere, &pkgName))
   {
      r::sexp::Protect rProtect;
      SEXP functionSEXP = R_NilValue;
      r::exec::RFunction func(".rs.getPackageFunction", name, pkgName);
      error = func.call(&functionSEXP, &rProtect);
      if (!error && !r::sexp::isNull(functionSEXP))
         *pNamespaceName = fromWhere;
   }

   // if we haven't found it yet
   if (pNamespaceName->empty())
   {
      r::exec::RFunction func(".rs.findFunctionNamespace",
                              name,
                              fromWhere);
      error = func.call(pNamespaceName);
   }

   // log error and return appropriate status
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }
   else if (pNamespaceName->empty())
   {
      return false;
   }
   else
   {
      return true;
   }
}


void getFunctionSource(SEXP functionSEXP,
                       std::vector<std::string>* pLines,
                       bool* pFromSrcAttrib)
{
   // check if the function has a "srcref" attribute
   *pFromSrcAttrib = false;
   r::exec::RFunction getSrcRefFunc(".rs.functionHasSrcRef", functionSEXP);
   Error error = getSrcRefFunc.call(pFromSrcAttrib);
   if (error)
      LOG_ERROR(error);

   // deparse
   r::exec::RFunction deparseFunc(".rs.deparseFunction");
   deparseFunc.addParam(functionSEXP);
   deparseFunc.addParam(*pFromSrcAttrib);
   deparseFunc.addParam(false);
   error = deparseFunc.call(pLines);
   if (error)
      LOG_ERROR(error);
}

void getFunctionS3Methods(const std::string& methodName, json::Array* pMethods)
{
   // lookup S3 methods for that base name
   std::vector<std::string> methods;
   r::exec::RFunction rfunc(".rs.getS3MethodsForFunction", methodName);
   Error error = rfunc.call(&methods);
   if (error)
      LOG_ERROR(error);

   // provide them to the caller
   std::transform(methods.begin(),
                  methods.end(),
                  std::back_inserter(*pMethods),
                  boost::bind(json::toJsonString, _1));

}


std::string trimToken(const std::string& token)
{
   return boost::algorithm::trim_copy(token);
}

class FunctionInfo
{
public:
   explicit FunctionInfo(const std::string& name)
      : name_(name)
   {
      boost::regex pattern("^([^{]+)\\{([^}]+)\\}$");
      boost::smatch match;
      if (regex_utils::search(name_, match, pattern))
      {
         // read method name
         methodName_ = match[1];
         boost::algorithm::trim(methodName_);

         // read , separated fields
         std::string types = match[2];
         using namespace boost ;
         char_separator<char> comma(",");
         tokenizer<char_separator<char> > typeTokens(types, comma);
         std::transform(typeTokens.begin(),
                        typeTokens.end(),
                        std::back_inserter(paramTypes_),
                        trimToken);
      }
   }

   bool isS4Method() const { return !methodName_.empty(); }

   const std::string& name() const { return name_; }
   const std::string& methodName() const { return methodName_; }
   const std::vector<std::string>& paramTypes() const { return paramTypes_; }

private:
   std::string name_;
   std::string methodName_;
   std::vector<std::string> paramTypes_;
};


void getFunctionS4Methods(const std::string& methodName, json::Array* pMethods)
{
   // check if the function isGeneric
   bool generic = false;
   if (methodName != "class")
   {
      Error error = r::exec::RFunction("methods:::isGeneric", methodName).call(
                                                                     &generic);
      if (error)
         LOG_ERROR(error);
   }

   if (generic)
   {
      std::vector<std::string> methods;
      r::exec::RFunction rFunc(".rs.getS4MethodsForFunction", methodName);
      Error error = rFunc.call(&methods);
      if (error)
         LOG_ERROR(error);

      // provide them to the caller
      std::transform(methods.begin(),
                     methods.end(),
                     std::back_inserter(*pMethods),
                     boost::bind(json::toJsonString, _1));
   }
}


json::Object createErrorFunctionDefinition(const std::string& name,
                                           const std::string& namespaceName)
{
   json::Object funDef;
   funDef["name"] = name;
   funDef["namespace"] = namespaceName;
   funDef["methods"] = json::Array();
   boost::format fmt("\n# ERROR: Definition of function '%1%' not found\n"
                     "# in namespace '%2%'");
   funDef["code"] = boost::str(fmt % name % namespaceName);
   funDef["from_src_attrib"] = false;

   return funDef;
}

std::string baseMethodName(const std::string& name)
{
   // strip type qualifiers for S4 methods
   FunctionInfo functionInfo(name);
   if (functionInfo.isS4Method())
   {
      return functionInfo.methodName();
   }
   // strip content after the '.' for S3 methods
   else
   {
      // first find the base name of the function
      std::string baseName = name;
      std::size_t periodLoc = baseName.find('.');
      if (periodLoc != std::string::npos && periodLoc > 0)
         baseName = baseName.substr(0, periodLoc);
      return baseName;
   }
}

json::Object createFunctionDefinition(const std::string& name,
                                      const std::string& namespaceName,
                                      SEXP functionSEXP)
{
   // basic metadata
   json::Object funDef;
   funDef["name"] = name;
   funDef["namespace"] = namespaceName;

   // function source code
   bool fromSrcAttrib = false;
   std::vector<std::string> lines;
   getFunctionSource(functionSEXP, &lines, &fromSrcAttrib);

   // did we get some lines back?
   if (lines.size() > 0)
   {
      // append the lines to the code and set it
      std::string code;
      BOOST_FOREACH(const std::string& line, lines)
      {
         code.append(line);
         code.append("\n");
      }
      funDef["code"] = code;
      funDef["from_src_attrib"] = fromSrcAttrib;

      // methods
      std::string methodName = baseMethodName(name);
      json::Array methodsJson;
      getFunctionS4Methods(methodName, &methodsJson);
      getFunctionS3Methods(methodName, &methodsJson);
      funDef["methods"] = methodsJson;

      return funDef;
   }
   else
   {
      return createErrorFunctionDefinition(name, namespaceName);
   }
}

Error getS4Method(const FunctionInfo& functionInfo,
                  std::string* pNamespaceName,
                  SEXP* pFunctionSEXP,
                  r::sexp::Protect* pProtect)
{
   // get the method
   r::exec::RFunction rFunc("methods:::getMethod");
   rFunc.addParam(functionInfo.methodName());
   rFunc.addParam(functionInfo.paramTypes());
   Error error = rFunc.call(pFunctionSEXP, pProtect);
   if (error)
      return error;

   // get the namespace
   r::exec::RFunction rNsFunc(".rs.getS4MethodNamespaceName", *pFunctionSEXP);
   return rNsFunc.call(pNamespaceName);
}

json::Object createFunctionDefinition(const std::string& name,
                                      const std::string& namespaceName)
{
   // stuff we are trying to find
   std::string functionNamespace = namespaceName;
   r::sexp::Protect rProtect;
   SEXP functionSEXP = R_NilValue;
   Error error;

   // what type of function are we looking for?
   FunctionInfo functionInfo(name);
   if (functionInfo.isS4Method())
   {
      // check for S4 method definition
      error = getS4Method(functionInfo,
                          &functionNamespace,
                          &functionSEXP,
                          &rProtect);
   }
   else
   {
      // get the function -- if it within a package namespace then do special
      // handling to make sure we can find hidden functions as well
      std::string pkgName;
      if (namespaceIsPackage(functionNamespace, &pkgName))
      {
         r::exec::RFunction getFunc(".rs.getPackageFunction", name, pkgName);
         error = getFunc.call(&functionSEXP, &rProtect);
      }
      else
      {
         r::exec::RFunction getFunc(".rs.getFunction", name, functionNamespace);
         error = getFunc.call(&functionSEXP, &rProtect);
      }
   }

   // check find status and return appropriate definiton
   if (!error)
   {
      if (!r::sexp::isNull(functionSEXP))
         return createFunctionDefinition(name, functionNamespace, functionSEXP);
      else
         return createErrorFunctionDefinition(name, functionNamespace);
   }
   else
   {
      LOG_ERROR(error);
      return createErrorFunctionDefinition(name, functionNamespace);
   }

}

json::Value createS3MethodDefinition(const std::string& name)
{
   // first call getAnywhere to see if we can find a definition
   r::sexp::Protect rProtect;
   SEXP getAnywhereSEXP;
   r::exec::RFunction getAnywhereFunc("utils:::getAnywhere", name);
   Error error = getAnywhereFunc.call(&getAnywhereSEXP, &rProtect);
   if (error)
   {
      LOG_ERROR(error);
      return json::Value();
   }

   // access the "where" element
   std::vector<std::string> whereList;
   error = r::sexp::getNamedListElement(getAnywhereSEXP, "where", &whereList);
   if (error)
   {
      LOG_ERROR(error);
      return json::Value();
   }

   // find an element beginning with "package:" or "namespace:"
   std::string packagePrefix = "package:";
   std::string namespacePrefix = "namespace:";
   std::string namespaceName;
   BOOST_FOREACH(const std::string& where, whereList)
   {
      if (boost::algorithm::starts_with(where, packagePrefix))
      {
         namespaceName = where;
         break;
      }

      if (boost::algorithm::starts_with(where, namespacePrefix) &&
          (where.length() > namespacePrefix.length()))
      {
         namespaceName = "package:" +
                         where.substr(namespacePrefix.length());
         break;
      }
   }

   // if we found one then go through standard route, else return null
   if (!namespaceName.empty())
      return createFunctionDefinition(name, namespaceName);
   else
      return json::Value();
}


json::Value createS4MethodDefinition(const FunctionInfo& functionInfo)
{
   // lookup the method
   std::string functionNamespace ;
   r::sexp::Protect rProtect;
   SEXP functionSEXP = R_NilValue;
   Error error = getS4Method(functionInfo,
                             &functionNamespace,
                             &functionSEXP,
                             &rProtect);
   if (error)
   {
      LOG_ERROR(error);
      return json::Value();
   }
   else
   {
      return createFunctionDefinition(functionInfo.name(),
                                      functionNamespace,
                                      functionSEXP);
   }
}


struct FunctionToken
{
   std::string package;
   std::string name;
};


json::Object createFunctionDefinition(const FunctionToken& token)
{
   return createFunctionDefinition(token.name, "package:" + token.package);
}

Error guessFunctionToken(const std::string& line,
                         int pos,
                         FunctionToken* pToken)
{
   // call into R to determine the token
   std::string token;
   Error error = r::exec::RFunction(".rs.guessToken", line, pos).call(&token);
   if (error)
      return error;

   // see if it has a namespace qualifier
   boost::regex pattern("^([^:]+):{2,3}([^:]+)$");
   boost::smatch match;
   if (regex_utils::search(token, match, pattern))
   {
      pToken->package = match[1];
      pToken->name = match[2];
   }
   else
   {
      pToken->name = token;
   }

   return Success();
}

Error getFunctionDefinition(const json::JsonRpcRequest& request,
                            json::JsonRpcResponse* pResponse)
{
   // read params
   std::string line;
   int pos;
   Error error = json::readParams(request.params, &line, &pos);
   if (error)
      return error;

   // call into R to determine the token
   FunctionToken token;
   error = guessFunctionToken(line, pos, &token);
   if (error)
      return error;

   // default return value is null function name (indicating no results)
   json::Object defJson;
   defJson["name"] = json::Value();

   // if there was a package then we go straight to the search path
   if (!token.package.empty())
   {
      defJson["name"] = token.name;
      defJson["type"] = "search_path_function";
      defJson["data"] = createFunctionDefinition(token);
   }

   // if we got a name token then search the code
   else if (!token.name.empty())
   {
      // discovered a token so we have at least a function name to return
      defJson["name"] = token.name;

      // find in source database then in project index
      std::set<std::string> contexts;
      r_util::RSourceItem sourceItem;
      bool found =
         findGlobalFunctionInSourceDatabase(token.name, &sourceItem, &contexts) ||
         s_projectIndex.findGlobalFunction(token.name, contexts, &sourceItem);

      // found the file
      if (found)
      {
         // return full path to file
         json::Object fileDef;
         FilePath srcFilePath = module_context::resolveAliasedPath(
                                                      sourceItem.context());
         fileDef["file"] = module_context::createFileSystemItem(srcFilePath);

         // return location in file
         json::Object posJson;
         posJson["line"] = sourceItem.line();
         posJson["column"] = sourceItem.column();
         fileDef["position"] = posJson;

         defJson["type"] = "file_function";
         defJson["data"] = fileDef;
      }
      // didn't find the file, check the search path
      else
      {
         // find the function
         std::string namespaceName;
         if (findFunction(token.name, "", &namespaceName))
         {
            defJson["type"] = "search_path_function";
            defJson["data"] = createFunctionDefinition(token.name,
                                                       namespaceName);
         }
         else if (findData(token.name))
         {
            defJson["type"] = "data";
            defJson["data"] = "";
         }
      }
   }

   // set result
   pResponse->setResult(defJson);

   return Success();
}


Error getSearchPathFunctionDefinition(const json::JsonRpcRequest& request,
                                      json::JsonRpcResponse* pResponse)
{
   // read params
   std::string name;
   std::string namespaceName;
   Error error = json::readParams(request.params, &name, &namespaceName);
   if (error)
      return error;

   // return result
   pResponse->setResult(createFunctionDefinition(name, namespaceName));
   return Success();
}

Error getMethodDefinition(const json::JsonRpcRequest& request,
                          json::JsonRpcResponse* pResponse)
{
   // read params
   std::string name;
   Error error = json::readParam(request.params, 0, &name);
   if (error)
      return error;

   // return result (distinguish between S3 and S4 methods)
   FunctionInfo functionInfo(name);
   if (functionInfo.isS4Method())
      pResponse->setResult(createS4MethodDefinition(functionInfo));
   else
      pResponse->setResult(createS3MethodDefinition(name));

   return Success();
}

Error findFunctionInSearchPath(const json::JsonRpcRequest& request,
                               json::JsonRpcResponse* pResponse)
{
   // read params
   std::string line;
   int pos;
   json::Value fromWhereJSON;
   Error error = json::readParams(request.params, &line, &pos, &fromWhereJSON);
   if (error)
      return error;

   // handle fromWhere NULL case
   std::string fromWhere = fromWhereJSON.is_null() ? "" :
                                                     fromWhereJSON.get_str();


   // call into R to determine the token
   FunctionToken token;
   error = guessFunctionToken(line, pos, &token);
   if (error)
      return error;

   // lookup the namespace if we need to
   std::string namespaceName;
   if (!token.package.empty())
       namespaceName = "package:" + token.package;
   else
      findFunction(token.name, fromWhere, &namespaceName);

   // return either just the name or the full function
   if (!namespaceName.empty())
   {
      pResponse->setResult(createFunctionDefinition(token.name,
                                                    namespaceName));
   }
   else
   {
      json::Object funDefName;
      funDefName["name"] = token.name;
      pResponse->setResult(funDefName);
   }

   return Success();
}

void onFileMonitorEnabled(const tree<core::FileInfo>& files)
{
   s_projectIndex.enqueFiles(files.begin_leaf(), files.end_leaf());
}

void onFilesChanged(const std::vector<core::system::FileChangeEvent>& events)
{
   std::for_each(
         events.begin(),
         events.end(),
         boost::bind(&SourceFileIndex::enqueFileChange, &s_projectIndex, _1));
}

void onFileMonitorDisabled()
{
   // clear the index so we don't ever get stale results
   s_projectIndex.clear();
}

SEXP rs_scoreMatches(SEXP suggestionsSEXP,
                     SEXP querySEXP)
{
   std::string query = r::sexp::asString(querySEXP);
   std::vector<std::string> suggestions;
   if (!r::sexp::fillVectorString(suggestionsSEXP, &suggestions))
      return R_NilValue;
   
   int n = suggestions.size();
   std::vector<int> scores;
   scores.reserve(n);
   
   for (int i = 0; i < n; i++)
      scores.push_back(scoreMatch(suggestions[i], query, false));
   
   r::sexp::Protect protect;
   return r::sexp::create(scores, &protect);
}

inline SEXP pathResultsSEXP(std::vector<std::string> const& paths,
                            bool moreAvailable)
{
   r::sexp::Protect protect;
   r::sexp::ListBuilder builder(&protect);
   builder.add("paths", paths);
   builder.add("more_available", moreAvailable);
   return r::sexp::create(builder, &protect);
}

SEXP rs_listIndexedFiles(SEXP termSEXP, SEXP absolutePathSEXP, SEXP maxResultsSEXP)
{
   std::string term = r::sexp::asString(termSEXP);
   std::string absolutePath = r::sexp::asString(absolutePathSEXP);
   int maxResults = r::sexp::asInteger(maxResultsSEXP);
   
   FilePath filePath(absolutePath);
   
   std::vector<std::string> names;
   std::vector<std::string> paths;
   bool moreAvailable = false;
   
   // Bail if the file doesn't exist
   if (!filePath.exists())
      return R_NilValue;
   
   // Bail if it's not a monitored path
   if (!projects::projectContext().isMonitoringDirectory(filePath))
      return R_NilValue;
   
   s_projectIndex.searchFiles(term,
                              maxResults,
                              false,
                              false,
                              filePath,
                              &names,
                              &paths,
                              &moreAvailable);

   return pathResultsSEXP(paths, moreAvailable);
}

SEXP rs_listIndexedFolders(SEXP termSEXP,
                           SEXP absolutePathSEXP,
                           SEXP maxResultsSEXP)
{
   std::string term = r::sexp::asString(termSEXP);
   std::string absolutePath = r::sexp::asString(absolutePathSEXP);
   int maxResults = r::sexp::asInteger(maxResultsSEXP);
   
   FilePath filePath(absolutePath);
   if (!filePath.exists())
      return R_NilValue;
   
   if (!projects::projectContext().isMonitoringDirectory(filePath))
      return R_NilValue;
   
   std::vector<std::string> paths;
   bool moreAvailable = false;
   s_projectIndex.searchFolders(term,
                                filePath,
                                maxResults,
                                &paths,
                                &moreAvailable);

   return pathResultsSEXP(paths, moreAvailable);
}

SEXP rs_listIndexedFilesAndFolders(SEXP termSEXP,
                                   SEXP absolutePathSEXP,
                                   SEXP maxResultsSEXP)
{
   std::string term = r::sexp::asString(termSEXP);
   std::string absolutePath = r::sexp::asString(absolutePathSEXP);
   int maxResults = r::sexp::asInteger(maxResultsSEXP);

   FilePath filePath(absolutePath);
   if (!filePath.exists())
      return R_NilValue;

   if (!projects::projectContext().isMonitoringDirectory(filePath))
      return R_NilValue;

   std::vector<std::string> paths;
   bool moreAvailable = false;
   s_projectIndex.searchFilesAndFolders(term,
                                        filePath,
                                        maxResults,
                                        &paths,
                                        &moreAvailable);

   return pathResultsSEXP(paths, moreAvailable);
}

SEXP rs_viewFunction(SEXP functionSEXP, SEXP nameSEXP, SEXP namespaceSEXP) 
{
   json::Object func = createFunctionDefinition(
         r::sexp::safeAsString(nameSEXP),
         r::sexp::safeAsString(namespaceSEXP),
         functionSEXP);
   ClientEvent viewEvent(client_events::kViewFunction, func);
   module_context::enqueClientEvent(viewEvent);
   return R_NilValue;
}

} // anonymous namespace
   
Error initialize()
{
   // subscribe to project context file monitoring state changes
   // (note that if there is no project this will no-op)
   session::projects::FileMonitorCallbacks cb;
   cb.onMonitoringEnabled = onFileMonitorEnabled;
   cb.onFilesChanged = onFilesChanged;
   cb.onMonitoringDisabled = onFileMonitorDisabled;
   projects::projectContext().subscribeToFileMonitor("R source file indexing",
                                                     cb);
   
   // register viewFunction method
   R_CallMethodDef methodDef ;
   methodDef.name = "rs_viewFunction" ;
   methodDef.fun = (DL_FUNC) rs_viewFunction ;
   methodDef.numArgs = 3;
   r::routines::addCallMethod(methodDef);

   // register call methods
   r::routines::registerCallMethod(
            "rs_scoreMatches",
            (DL_FUNC) rs_scoreMatches,
            2);
   
   r::routines::registerCallMethod(
            "rs_listIndexedFiles",
            (DL_FUNC) rs_listIndexedFiles,
            3);
   
   r::routines::registerCallMethod(
            "rs_listIndexedFolders",
            (DL_FUNC) rs_listIndexedFolders,
            3);

   r::routines::registerCallMethod(
            "rs_listIndexedFilesAndFolders",
            (DL_FUNC) rs_listIndexedFilesAndFolders,
            3);
   
   // initialize r source indexes
   rSourceIndex().initialize();
   
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "search_code", searchCode))
      (bind(registerRpcMethod, "get_function_definition", getFunctionDefinition))
      (bind(registerRpcMethod, "get_search_path_function_definition", getSearchPathFunctionDefinition))
      (bind(registerRpcMethod, "get_method_definition", getMethodDefinition))
      (bind(registerRpcMethod, "find_function_in_search_path", findFunctionInSearchPath));

   return initBlock.execute();
}

namespace callbacks {

void addAllProjectSymbols(const Entry& entry,
                          std::set<std::string>* pSymbols)
{
   if (!entry.hasIndex())
      return;
   
   const std::vector<r_util::RSourceItem>& items = entry.pIndex->items();
   BOOST_FOREACH(const r_util::RSourceItem& item, items)
   {
      pSymbols->insert(string_utils::strippedOfQuotes(item.name()));
   } 
}

} // namespace callbacks

void addAllProjectSymbols(std::set<std::string>* pSymbols)
{
   FilePath buildTarget = projects::projectContext().buildTargetPath();
   
   if (!buildTarget.empty())
      s_projectIndex.walkFiles(
               buildTarget,
               boost::bind(callbacks::addAllProjectSymbols, _1, pSymbols));
   
   // Add in symbols made available as part of registration of native routines,
   // if this is a package project.
   if (projects::projectContext().isPackageProject())
   {
      std::string pkgName = projects::projectContext().packageInfo().name();
      if (pkgName.empty())
         return;
      std::vector<std::string> nativeRoutineNames;
      r::exec::RFunction getNativeSymbols(".rs.getNativeSymbols");
      getNativeSymbols.addParam(pkgName);
      Error error = getNativeSymbols.call(&nativeRoutineNames);
      if (error)
         LOG_ERROR(error);
      pSymbols->insert(nativeRoutineNames.begin(), nativeRoutineNames.end());
   }
}

} // namespace code_search
} // namespace modules
} // namespace session
} // namespace rstudio
