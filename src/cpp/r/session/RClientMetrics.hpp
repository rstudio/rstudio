/*
 * RClientMetrics.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef R_SESSION_CLIENT_METRICS_HPP
#define R_SESSION_CLIENT_METRICS_HPP

namespace core {
   class Settings;
}

namespace r {
namespace session {
   
class RClientMetrics;
   
namespace client_metrics {

RClientMetrics get();
void set(const RClientMetrics& metrics);
void save(core::Settings* pSettings);
void restore(const core::Settings& settings);
   
} // namespace client_metrics
} // namespace session
} // namespace r

#endif // R_SESSION_CLIENT_METRICS_HPP 

