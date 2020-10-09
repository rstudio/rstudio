/*
 * SessionRmdNotebook.hpp
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


#ifndef SESSION_RMARKDOWN_NOTEBOOK_HPP
#define SESSION_RMARKDOWN_NOTEBOOK_HPP

#include <ctime>

#include <core/BoostSignals.hpp>
#include <shared_core/json/Json.hpp>

#define kChunkLibDir "lib"
#define kNotebookExt ".nb.html"

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

enum ExecMode 
{
   // a single chunk is being executed interactively
   ExecModeSingle = 0,
   // multiple chunks are being executed in a batch
   ExecModeBatch  = 1
};

enum ExecScope 
{
   // an entire chunk is being executed
   ExecScopeChunk   = 0,
   // a section of a chunk is being executed (e.g. via Ctrl+Enter)
   ExecScopePartial = 1,
   // an inline R chunk is being executed
   ExecScopeInline  = 2
};

enum CommitMode
{
   // changes should be committed to the cache immediately
   ModeCommitted   = 0,
   // changes should held for commit until save
   ModeUncommitted = 1
};

enum Condition
{
   ConditionMessage = 0,
   ConditionWarning = 1
};

core::Error initialize();

std::string notebookCtxId();

struct Events : boost::noncopyable
{
   // Document {0}, chunk {1} from context id, {2} from code, {3} label, {4} execution completed
   RSTUDIO_BOOST_SIGNAL<void(const std::string&, const std::string&,
                             const std::string&, const std::string&, const std::string&)>
                onChunkExecCompleted;

   // Document {0}, chunk {1} had console output of type {2} and text {3}
   RSTUDIO_BOOST_SIGNAL<void(const std::string&, const std::string&, int, 
                const std::string&)>
                onChunkConsoleOutput;

   RSTUDIO_BOOST_SIGNAL<void(const core::FilePath&, const core::FilePath&, 
                      const core::json::Value& metadata, unsigned ordinal)> 
                         onPlotOutput;
   RSTUDIO_BOOST_SIGNAL<void(const core::FilePath&, const core::FilePath&,
                      const core::json::Value& metadata)> onHtmlOutput;
   RSTUDIO_BOOST_SIGNAL<void(const core::json::Object&)> onErrorOutput;
   RSTUDIO_BOOST_SIGNAL<void(const core::FilePath&, const core::FilePath&,
                      const core::json::Value& metadata)> onDataOutput;
   RSTUDIO_BOOST_SIGNAL<void(Condition condition, const std::string& message)> 
                         onCondition;
};

Events& events();

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_RMARKDOWN_NOTEBOOK_HPP
