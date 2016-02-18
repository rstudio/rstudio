/*
 * SessionRmdNotebook.hpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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


#ifndef SESSION_RMARKDOWN_NOTEBOOK_HPP
#define SESSION_RMARKDOWN_NOTEBOOK_HPP

#include <ctime>
#include <boost/signals.hpp>
#include <core/json/Json.hpp>

#define kChunkLibDir "lib"

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}
 
namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

core::Error initialize();

core::Error ensureCacheFolder(const core::FilePath& cacheFolder);

core::Error getChunkDefs(const std::string& docPath, const std::string& docId, 
      std::time_t *pDocTime, core::json::Value* pDefs);

core::Error setChunkDefs(const std::string& docPath, const std::string& docId, 
      std::time_t docTime, const core::json::Array& defs);

core::Error extractScriptTags(const std::string& contents,
                              std::vector<std::string>* pScripts);

struct Events : boost::noncopyable
{
   // Document {0}, chunk {1} from context id {3} execution completed
   boost::signal<void(const std::string&, const std::string&,
                      const std::string&)> 
                onChunkExecCompleted;

   // Document {0}, chunk {1} had console output of type {2} and text {3}
   boost::signal<void(const std::string&, const std::string&, int, 
                const std::string&)>
                onChunkConsoleOutput;
};

Events& events();

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_RMARKDOWN_NOTEBOOK_HPP
