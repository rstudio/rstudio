/*
 * SessionGraphics.cpp
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

#include "SessionGraphics.hpp"

#include <boost/bind/bind.hpp>

#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/ROptions.hpp>
#include <r/RRoutines.hpp>

#include <r/session/RGraphicsConstants.h>
#include <r/session/RGraphics.hpp>

#include <session/prefs/UserPrefs.hpp>
#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace r {
namespace session {
namespace graphics {
namespace device {

extern bool s_gdTracingEnabled;

} // end namespace device
} // end namespace graphics
} // end namespace session
} // end namespace r
} // end namespace rstudio

namespace rstudio {
namespace session {
namespace modules {
namespace graphics {

namespace {

void syncWithPrefs()
{
   r::options::setOption(
            kGraphicsOptionBackend,
            prefs::userPrefs().graphicsBackend());
   
   r::options::setOption(
            kGraphicsOptionAntialias,
            prefs::userPrefs().graphicsAntialiasing());
}

void onPreferencesSaved()
{
   syncWithPrefs();
}

SEXP rs_devicePixelRatio()
{
   double ratio = r::session::graphics::device::devicePixelRatio();
   r::sexp::Protect protect;
   return r::sexp::create(ratio, &protect);
}

} // end anonymous namespace

core::json::Array supportedBackends()
{
   r::sexp::Protect protect;
   SEXP backends;
   Error error = r::exec::RFunction(".rs.graphics.supportedBackends").call(&backends, &protect);
   if (error)
   {
      LOG_ERROR(error);
      return json::Array();
   }
   
   core::json::Array backendsJson;
   error = r::json::jsonValueFromVector(backends, &backendsJson);
   if (error)
   {
      LOG_ERROR(error);
      return json::Array();
   }
   
   return backendsJson;
}

SEXP rs_traceGraphicsDevice(SEXP enableSEXP)
{
   bool enable = r::sexp::asLogical(enableSEXP);
   r::session::graphics::device::s_gdTracingEnabled = enable;
   return enableSEXP;
}

core::Error initialize()
{
   using namespace module_context;
   
   events().onPreferencesSaved.connect(onPreferencesSaved);
   
   syncWithPrefs();
   
   RS_REGISTER_CALL_METHOD(rs_devicePixelRatio);
   RS_REGISTER_CALL_METHOD(rs_traceGraphicsDevice);
   
   using boost::bind;
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionGraphics.R"));

   return initBlock.execute();
}

} // namespace graphics
} // namespace modules
} // namespace session
} // namespace rstudio
