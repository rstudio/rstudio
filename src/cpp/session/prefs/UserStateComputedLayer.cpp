/*
 * UserPrefsComputedLayer.cpp
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

#include "UserStateComputedLayer.hpp"

#include <boost/algorithm/string/predicate.hpp>

#include <shared_core/json/Json.hpp>

#include <core/system/Environment.hpp>

#include <session/prefs/UserState.hpp>
#include <session/prefs/UserStateValues.hpp>


using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace prefs {

UserStateComputedLayer::UserStateComputedLayer():
   PrefLayer(kUserStateComputedLayer)
{
}

Error UserStateComputedLayer::readPrefs()
{
   json::Object layer;
   cache_ = boost::make_shared<core::json::Object>(layer);
   return Success();
}

} // namespace prefs
} // namespace session
} // namespace rstudio

