/*
 * SessionReticulate.hpp
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

#ifndef SESSION_MODULES_RETICULATE_HPP
#define SESSION_MODULES_RETICULATE_HPP

#include <string>

namespace rstudio {
namespace core {

class Error;

} // end namespace core
} // end namespace rstudio

namespace rstudio {
namespace session {
namespace modules {
namespace reticulate {

// What startup's background Python discovery has established so far, and
// how a terminal that needs RETICULATE_PYTHON should obtain its answer.
class PythonDiscoveryState
{
public:
   enum class TerminalAction
   {
      ProbeSynchronously,
      UseRecordedAnswer,
      RetryInBackground
   };

   // the interpreter (possibly none, when empty) is settled for the session
   void recordAnswer(const std::string& python);

   // discovery itself failed; nothing more will be learned this session
   void recordFailure();

   // discovery ran out of time, so the answer stays open for a later retry
   void recordTimeout();

   bool resolved() const { return resolved_; }
   const std::string& python() const { return python_; }
   TerminalAction terminalAction() const;

private:
   std::string python_;
   bool resolved_ = false;
   bool timedOut_ = false;
};

bool isPythonInitialized();
bool isReplActive();

core::Error initialize();

std::string reticulatePython();

} // end namespace reticulate
} // end namespace modules
} // end namespace session
} // end namespace rstudio

#endif /* SESSION_MODULES_RETICULATE_HPP */
