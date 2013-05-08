#include "EnvironmentState.hpp"

#include <core/FilePath.hpp>
#include <core/Settings.hpp>

#include <r/RSexp.hpp>
#include <r/session/RSession.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

using namespace core;

namespace session {
namespace modules {
namespace environment {
namespace state {

namespace {

struct EnvironmentState
{
   EnvironmentState()
       : inBrowseMode(false)
   {
   }
   bool inBrowseMode;
};

EnvironmentState s_environmentState;
} // anonymous namespace

void saveEnvironmentState(const EnvironmentState& state)
{
   // update write-through cache
   s_environmentState = state;
}

void init(bool inBrowseMode)
{
   EnvironmentState state;
   state.inBrowseMode = inBrowseMode;
   saveEnvironmentState(state);
}

json::Value asJson()
{
   json::Object stateJson;
   stateJson["browse_mode"] = s_environmentState.inBrowseMode;
   return stateJson;
}

void loadEnvironmentState()
{
    init(r::session::browserContextActive());
}

Error initialize()
{
   // attempt to load any cached state
   loadEnvironmentState();

   return Success();
}

} // namespace state
} // namespace environment
} // namespace modules
} // namespace session
