/*
 * StartupTiming.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#include <core/StartupTiming.hpp>

#include <chrono>
#include <fstream>
#include <iomanip>

#include <core/system/Environment.hpp>
#include <core/system/System.hpp>

namespace rstudio {
namespace core {
namespace startup_timing {

namespace {

// resolved once; the environment variable is inherited from the desktop
// front-end (or set by hand), so it cannot change during the process lifetime
const std::string& timingFile()
{
   static const std::string s_path = core::system::getenv("RSTUDIO_STARTUP_TIMING");
   return s_path;
}

double nowMs()
{
   using namespace std::chrono;
   return duration<double, std::milli>(system_clock::now().time_since_epoch()).count();
}

void write(const std::string& name, double t, double durationMs)
{
   // the desktop and session processes append to the same file; small
   // O_APPEND writes are atomic enough for one line per checkpoint
   std::ofstream out(timingFile(), std::ios::app);
   if (!out)
      return;

   out << std::fixed << std::setprecision(3)
       << "{\"tier\":\"session\",\"name\":\"" << name << "\""
       << ",\"t\":" << t
       << ",\"pid\":" << core::system::currentProcessId();

   if (durationMs >= 0)
      out << ",\"dur\":" << durationMs;

   out << "}\n";
}

} // anonymous namespace

bool enabled()
{
   return !timingFile().empty();
}

void checkpoint(const std::string& name)
{
   if (!enabled())
      return;

   write(name, nowMs(), -1);
}

void checkpoint(const std::string& name, double durationMs)
{
   if (!enabled())
      return;

   write(name, nowMs(), durationMs);
}

ScopedCheckpoint::ScopedCheckpoint(const std::string& name)
   : name_(name),
     start_(enabled() ? nowMs() : 0)
{
}

ScopedCheckpoint::~ScopedCheckpoint()
{
   if (!enabled())
      return;

   write(name_, nowMs(), nowMs() - start_);
}

} // namespace startup_timing
} // namespace core
} // namespace rstudio
