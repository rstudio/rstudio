/*
 * SessionCodeSearch.hpp
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

#ifndef SESSION_CODE_SEARCH_HPP
#define SESSION_CODE_SEARCH_HPP

#include <core/r_util/RSourceIndex.hpp>
#include <session/SessionSourceDatabase.hpp>
#include <boost/foreach.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace code_search {

// maintain an in-memory list of R source document indexes (for fast
// code searching)
class RSourceIndexes : boost::noncopyable
{
private:
   
   typedef session::source_database::SourceDocument SourceDocument;
   typedef core::r_util::RSourceIndex RSourceIndex;
   
public:
   
   typedef std::string DocumentId;
   typedef std::string AbsolutePath;
   
   // maps document id (as string) to associated index
   typedef std::map< DocumentId, boost::shared_ptr<RSourceIndex> > IndexMap;
   
   RSourceIndexes() {}
   virtual ~RSourceIndexes() {}

   // COPYING: boost::noncopyable

   void initialize();
   void update(boost::shared_ptr<SourceDocument> pDoc);
   boost::shared_ptr<RSourceIndex> get(const std::string& id)
   {
      if (indexes_.find(id) != indexes_.end())
         return indexes_[id];
      return boost::shared_ptr<RSourceIndex>();
   }
   
   void remove(const std::string& id);
   void removeAll();

   std::vector< boost::shared_ptr<RSourceIndex> > indexes()
   {
      std::vector< boost::shared_ptr<RSourceIndex> > indexes;
      BOOST_FOREACH(const IndexMap::value_type& index, indexes_)
      {
         indexes.push_back(index.second);
      }
      return indexes;
   }
   
   const IndexMap& indexMap() const
   {
      return indexes_;
   }

private:
  IndexMap indexes_;
  
};

RSourceIndexes& rSourceIndex();

boost::shared_ptr<core::r_util::RSourceIndex> getIndexedProjectFile(
      const core::FilePath& filePath);

void searchSource(const std::string& term,
                  std::size_t maxResults,
                  bool prefixOnly,
                  std::vector<core::r_util::RSourceItem>* pItems,
                  bool* pMoreAvailable);

void addAllProjectSymbols(std::set<std::string>* pSymbols);

core::Error initialize();
   
} // namespace code_search
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CODE_SEARCH_HPP

