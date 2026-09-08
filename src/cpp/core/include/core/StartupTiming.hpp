/*
 * StartupTiming.hpp
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

#ifndef CORE_STARTUP_TIMING_HPP
#define CORE_STARTUP_TIMING_HPP

#include <string>

namespace rstudio {
namespace core {
namespace startup_timing {

// Startup checkpoints are recorded only when RSTUDIO_STARTUP_TIMING names a
// file. Each checkpoint appends one JSON line of the form
//
//    {"tier":"session","name":"<name>","t":<epoch ms>,"pid":<pid>}
//
// with an optional "dur" (milliseconds) for checkpoints that describe a
// completed span. The desktop front-end and the GWT client write to the same
// file, so a single timeline can be reconstructed across all three processes
// by sorting on "t". See tasks/startup-timing.ts for the report.
bool enabled();

void checkpoint(const std::string& name);
void checkpoint(const std::string& name, double durationMs);

// Records "<name>" with the elapsed time of the enclosing scope as "dur".
class ScopedCheckpoint
{
public:
   explicit ScopedCheckpoint(const std::string& name);
   ~ScopedCheckpoint();

private:
   std::string name_;
   double start_;
};

} // namespace startup_timing
} // namespace core
} // namespace rstudio

#endif // CORE_STARTUP_TIMING_HPP
