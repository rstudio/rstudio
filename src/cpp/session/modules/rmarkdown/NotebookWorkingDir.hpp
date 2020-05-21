/*
 * NotebookWorkingDir.hpp
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


#ifndef SESSION_NOTEBOOK_WORKING_DIR_HPP
#define SESSION_NOTEBOOK_WORKING_DIR_HPP

#include "NotebookCapture.hpp"

#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {
   class Error;
}
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

class DirCapture : public NotebookCapture
{
public:
   DirCapture();
   ~DirCapture();
   core::Error connectDir(const std::string& docId, 
                          const core::FilePath& workingDir);
   void onExprComplete();
   void disconnect();
private:
   std::string prevWorkingDir_;
   core::FilePath workingDir_;
   bool warned_;
};

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

#endif


