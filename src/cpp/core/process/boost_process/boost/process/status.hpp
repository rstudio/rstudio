//
// Boost.Process
// ~~~~~~~~~~~~~
//
// Copyright (c) 2006, 2007 Julio M. Merino Vidal
// Copyright (c) 2008 Ilya Sokolov, Boris Schaeling
// Copyright (c) 2009 Boris Schaeling
// Copyright (c) 2010 Felipe Tanus, Boris Schaeling
//
// Distributed under the Boost Software License, Version 1.0. (See accompanying
// file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//

/**
 * \file boost/process/status.hpp
 *
 * Includes the declaration of the status class.
 */

#ifndef BOOST_PROCESS_STATUS_HPP
#define BOOST_PROCESS_STATUS_HPP

#include <boost/process/config.hpp>
#include <boost/process/detail/basic_status.hpp>
#include <boost/process/detail/basic_status_service.hpp>

namespace boost {
namespace process {

/**
 * The status class to wait for processes to exit.
 *
 * The status class is a Boost.Asio I/O object and supports synchronous
 * and asynchronous wait operations.
 */
typedef detail::basic_status<detail::basic_status_service<> > status;

}
}

#endif
