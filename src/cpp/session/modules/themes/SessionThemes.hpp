/*
 * SessionThemes.hpp
 *
 * Copyright (C) 2018 by RStudio, Inc.
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

#ifndef SESSION_THEMES_HPP
#define SESSION_THEMES_HPP

#include <string>

#include <boost/system/error_code.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace session {
namespace modules {
namespace themes {
namespace errc {

enum errc_t
{
   Success = 0,
   ParseError
};

} // namespace errc
} // namespace themes
} // namespace modules
} // namespace session
} // namespace rstudio


namespace RSTUDIO_BOOST_NAMESPACE {
namespace system {
template <>
struct is_error_code_enum<rstudio::session::modules::themes::errc::errc_t>
   { static const bool value = true; };
} // namespace system
} // namespace boost

namespace rstudio {
namespace session {
namespace modules {
namespace themes {

core::Error initialize();

const boost::system::error_category& sessionThemesCategory();

namespace errc {

inline boost::system::error_code make_error_code(errc_t e)
{
   return boost::system::error_code(e, sessionThemesCategory());
}

inline boost::system::error_condition make_error_caondition(errc_t e)
{
   return boost::system::error_condition(e, sessionThemesCategory());
}

} // namespace errc

} // namespace themes
} // namespace modules
} // namespace session
} // namespace rstudio


#endif
