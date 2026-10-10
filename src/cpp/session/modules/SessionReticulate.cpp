/*
 * SessionReticulate.cpp
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

#include "SessionReticulate.hpp"

#include "SessionThemes.hpp"

#include <boost/bind/bind.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <chrono>

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>
#include <core/Log.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionAsyncRProcess.hpp>

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace session {
namespace modules {
namespace reticulate {

namespace {

// has the Python session been initialized by reticulate yet?
bool s_pythonInitialized = false;

PythonDiscoveryState s_discovery;
unsigned int s_pythonDiscoveryGeneration = 0;
boost::shared_ptr<async_r::AsyncRProcess> s_pythonDiscovery;
const int kPythonDiscoveryTimeoutSeconds = 30;

void cancelPythonDiscovery()
{
   // The first supervisor poll assigns the child PID. Invalidate the probe
   // and let onContinue stop it, avoiding an interrupt of process group zero.
   ++s_pythonDiscoveryGeneration;
   s_pythonDiscovery.reset();
}

class PythonDiscovery : public async_r::AsyncRProcess
{
public:
   explicit PythonDiscovery(unsigned int generation)
      : generation_(generation),
        deadline_(std::chrono::steady_clock::now() + std::chrono::seconds(kPythonDiscoveryTimeoutSeconds))
   {
   }

protected:
   bool onContinue() override
   {
      return generation_ == s_pythonDiscoveryGeneration &&
             std::chrono::steady_clock::now() < deadline_ &&
             async_r::AsyncRProcess::onContinue();
   }

   void onStdout(const std::string& output) override
   {
      output_ += output;
   }

   void onCompleted(int exitStatus) override
   {
      // Python may have been initialized, or a terminal may have requested
      // a synchronous answer, while this child was still discovering it.
      if (generation_ != s_pythonDiscoveryGeneration)
         return;

      s_pythonDiscovery.reset();
      std::size_t marker = output_.rfind('\x1e');
      if (exitStatus == EXIT_SUCCESS && marker != std::string::npos)
      {
         std::string python = output_.substr(marker + 1);
         boost::algorithm::trim(python);
         s_discovery.recordAnswer(python);
      }
      else if (std::chrono::steady_clock::now() >= deadline_)
      {
         // A slow discovery may well succeed later (cold network home, large
         // conda install), so leave the answer open: the next terminal retries
         // in the background rather than repeating it synchronously.
         WLOGF("Python discovery did not finish within {} seconds; retrying when a terminal next needs it", kPythonDiscoveryTimeoutSeconds);
         s_discovery.recordTimeout();
      }
      else
      {
         // Record the miss: otherwise the next terminal repeats this same
         // discovery synchronously, blocking the session for as long again.
         WLOGF("Python discovery exited with status {}; terminals will not set RETICULATE_PYTHON", exitStatus);
         s_discovery.recordFailure();
      }
   }

private:
   unsigned int generation_;
   std::chrono::steady_clock::time_point deadline_;
   std::string output_;
};

void discoverPythonAsync()
{
   if (s_discovery.resolved() || s_pythonDiscovery)
      return;

   // Resolve explicit configuration and an already initialized interpreter
   // immediately. Only automatic discovery needs a child R process.
   SEXP python = R_NilValue;
   r::sexp::Protect protect;
   Error error = r::exec::RFunction(".rs.inferReticulatePython", false)
         .call(&python, &protect);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   if (python != R_NilValue)
   {
      s_discovery.recordAnswer(r::sexp::asString(python));
      return;
   }

   const char* command = R"(
config <- suppressWarnings(tryCatch(reticulate::py_discover_config(),
                                    error = function(e) NULL))
cat("\x1e", if (is.null(config$python)) "" else config$python, sep = "")
)";
   boost::shared_ptr<PythonDiscovery> discovery(new PythonDiscovery(++s_pythonDiscoveryGeneration));
   s_pythonDiscovery = discovery;
   discovery->start(
      command,
      {{"RETICULATE_MINICONDA_ENABLED", "FALSE"}},
      module_context::safeCurrentPath(),
      async_r::R_PROCESS_VANILLA);
}

void updateReticulatePython(bool forInit)
{
   if (!ASSERT_MAIN_THREAD())
   {
      return;
   }

   if (!forInit)
   {
      switch (s_discovery.terminalAction())
      {
      case PythonDiscoveryState::TerminalAction::UseRecordedAnswer:
         return;
      case PythonDiscoveryState::TerminalAction::RetryInBackground:
         // After a timed-out discovery, terminals go without RETICULATE_PYTHON
         // for now and retry in the background instead of blocking for as long again.
         discoverPythonAsync();
         return;
      case PythonDiscoveryState::TerminalAction::ProbeSynchronously:
         break;
      }
   }

   // Preserve the existing terminal behavior when an answer is needed before
   // asynchronous discovery finishes; discard any later result from the child.
   cancelPythonDiscovery();

   std::string python = core::system::getenv("RETICULATE_PYTHON");
   if (python.empty())
   {
      // Will check if RETICULATE_PYTHON_FALLBACK is set,
      // unless higher priority Python config has already been found
      Error error = r::exec::RFunction(".rs.inferReticulatePython")
            .call(&python);

      if (error)
         LOG_ERROR(error);
   }

   s_discovery.recordAnswer(python);
}

SEXP rs_reticulateInitialized()
{
   // set initialized flag
   s_pythonInitialized = true;
   
   // Python will register its own console control handler,
   // which also blocks signals from reaching any previously
   // defined handlers (including RStudio's own). re-initialize
   // RStudio's console control handler here to ensure that
   // interrupts are still handled by R as expected
   module_context::initializeConsoleCtrlHandler();

   // cache the final path to python for offline use
   updateReticulatePython(true);

   return R_NilValue;
}

void onDeferredInit(bool)
{
   Error error = r::exec::RFunction(".rs.reticulate.initialize").call();
   if (error)
      LOG_ERROR(error);

   // update python path after all R init scripts
   discoverPythonAsync();
}

} // end anonymous namespace

void PythonDiscoveryState::recordAnswer(const std::string& python)
{
   python_ = python;
   resolved_ = true;
   timedOut_ = false;
}

void PythonDiscoveryState::recordFailure()
{
   recordAnswer(std::string());
}

void PythonDiscoveryState::recordTimeout()
{
   python_.clear();
   resolved_ = false;
   timedOut_ = true;
}

PythonDiscoveryState::TerminalAction PythonDiscoveryState::terminalAction() const
{
   if (resolved_)
      return TerminalAction::UseRecordedAnswer;
   else if (timedOut_)
      return TerminalAction::RetryInBackground;
   else
      return TerminalAction::ProbeSynchronously;
}

bool isPythonInitialized()
{
   return s_pythonInitialized;
}

bool isReplActive()
{
   bool active = false;
   Error error = r::exec::RFunction(".rs.reticulate.replIsActive").call(&active);
   if (error)
      LOG_ERROR(error);
   return active;
}


std::string reticulatePython()
{
   updateReticulatePython(false);
   return s_discovery.python();
}

Error initialize()
{
   using namespace module_context;
   
   events().onDeferredInit.connect(onDeferredInit);
   events().onShutdown.connect([](bool) { cancelPythonDiscovery(); });

   RS_REGISTER_CALL_METHOD(rs_reticulateInitialized);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionReticulate.R"));
   
   return initBlock.execute();
}

} // end namespace reticulate
} // end namespace modules

namespace module_context {

bool isPythonReplActive()
{
   return modules::reticulate::isReplActive();
}

} // end namespace module_context

} // end namespace session
} // end namespace rstudio
