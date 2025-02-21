//
// io_service.hpp
// ~~~~~~~~~~~~~~
//
// Copyright (c) 2003-2023 Christopher M. Kohlhoff (chris at kohlhoff dot com)
//
// Distributed under the Boost Software License, Version 1.0. (See accompanying
// file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//

#ifndef BOOST_ASIO_IO_SERVICE_HPP
#define BOOST_ASIO_IO_SERVICE_HPP

#ifndef RSTUDIO_BOOST_NAMESPACE
# define RSTUDIO_BOOST_NAMESPACE boost
#endif

#if defined(_MSC_VER) && (_MSC_VER >= 1200)
# pragma once
#endif // defined(_MSC_VER) && (_MSC_VER >= 1200)

#include <boost/asio/io_context.hpp>

#include <boost/asio/detail/push_options.hpp>

namespace RSTUDIO_BOOST_NAMESPACE {
namespace asio {

#if !defined(BOOST_ASIO_NO_DEPRECATED)
/// Typedef for backwards compatibility.
typedef io_context io_service;
#endif // !defined(BOOST_ASIO_NO_DEPRECATED)

} // namespace asio
} // namespace rstudio_boost

#include <boost/asio/detail/pop_options.hpp>

#endif // BOOST_ASIO_IO_SERVICE_HPP
