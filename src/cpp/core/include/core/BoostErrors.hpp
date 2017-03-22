/*
 * BoostErrors.hpp
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

#ifndef CORE_BOOST_ERRORS_HPP
#define CORE_BOOST_ERRORS_HPP

#include <boost/system/error_code.hpp>

// bridges for boost libraries without support for system::error_code
namespace RSTUDIO_BOOST_NAMESPACE {

namespace interprocess {

const boost::system::error_category& interprocess_category() ;

class interprocess_exception ;
boost::system::error_code ec_from_exception(const interprocess_exception& e) ;

} // namespace interprocess

class thread_resource_error ;

namespace thread_error {

const boost::system::error_category& thread_category() ;

boost::system::error_code ec_from_exception(
      const boost::thread_resource_error& e) ;

} // namespace thread_error

} // namespace boost

#endif // CORE_BOOST_ERRORS_HPP

