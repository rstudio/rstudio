/*
 * ServerErrorCategory.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#ifndef SERVER_SERVER_ERROR_CATEGORY_HPP
#define SERVER_SERVER_ERROR_CATEGORY_HPP

#include <boost/system/error_code.hpp>

namespace rstudio {
namespace server {
namespace errc {

enum errc_t {
   Success = 0,
   AuthenticationError,
   SessionUnavailableError,
   InvalidSessionScopeError
};

} // namespace errc
} // namespace r
} // namespace rstudio


namespace RSTUDIO_BOOST_NAMESPACE {
namespace system {
template <>
struct is_error_code_enum<rstudio::server::errc::errc_t>
 { static const bool value = true; };
} // namespace system
} // namespace boost


#include <core/Error.hpp>

namespace rstudio {
namespace server {

const boost::system::error_category& serverCategory() ;

namespace errc {

inline boost::system::error_code make_error_code( errc_t e )
{
   return boost::system::error_code( e, serverCategory() ); }

inline boost::system::error_condition make_error_condition( errc_t e )
{
   return boost::system::error_condition( e, serverCategory() );
}

} // namespace errc


bool isAuthenticationError(const core::Error& error);

bool isSessionUnavailableError(const core::Error& error);

bool isInvalidSessionScopeError(const core::Error& error);

} // namespace server
} // namespace rstudio


#endif // SERVER_SERVER_ERROR_CATEGORY_HPP

