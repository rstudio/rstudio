/*
 * SessionConsoleOutput.hpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
 *
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

#ifndef RSTUDIO_SESSION_CONSOLE_OUTPUT_HPP
#define RSTUDIO_SESSION_CONSOLE_OUTPUT_HPP

#include <boost/regex_fwd.hpp>

#include <shared_core/Error.hpp>

namespace rstudio {
namespace session {
namespace console_output {

void simulateLatency();

enum PendingOutputType {
   PendingOutputTypeUnknown,
   PendingOutputTypeError,
   PendingOutputTypeWarning,
};

PendingOutputType getPendingOutputType();
void setPendingOutputType(PendingOutputType type);

boost::regex reErrorPrefix();
boost::regex reWarningPrefix();
boost::regex reInAdditionPrefix();

bool isErrorAnnotationEnabled();
bool isWarningAnnotationEnabled();

core::Error initialize();

} // end namespace console_output
} // end namespace session
} // end namespace rstudio

#endif /* RSTUDIO_SESSION_CONSOLE_OUTPUT_HPP */
