/*
 * NotebookCacheRenderer.hpp
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

#ifndef SESSION_NOTEBOOK_CACHE_RENDERER_HPP
#define SESSION_NOTEBOOK_CACHE_RENDERER_HPP

#include <session/SessionAsyncRProcess.hpp>

#include <map>
#include <string>
#include <sstream>

#include <boost/shared_ptr.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

class NotebookCacheRenderer : public async_r::AsyncRProcess
{
public:
   static void render(const std::string& rmdPath,
                      const std::string& cachePath,
                      const std::string& outputPath,
                      const std::string& docId,
                      const std::string& docPath,
                      const std::string& encoding);

   static bool isRunning(const std::string& docPath);

protected:
   void onStdout(const std::string& output) override;
   void onStderr(const std::string& output) override;
   void onCompleted(int exitStatus) override;

private:
   NotebookCacheRenderer(const std::string& docId,
                         const std::string& docPath,
                         const std::string& outputPath);

   std::string docId_;
   std::string docPath_;
   std::string outputPath_;
   std::stringstream stdOut_;
   std::stringstream stdErr_;
   bool cancelled_ = false;

   static std::map<std::string, boost::weak_ptr<NotebookCacheRenderer>> s_running_;
};

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

#endif
