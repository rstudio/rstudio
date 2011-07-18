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
 * \file boost/process/detail/basic_status.hpp
 *
 * Includes the declaration of the basic status class.
 */

#ifndef BOOST_PROCESS_DETAIL_BASIC_STATUS_HPP
#define BOOST_PROCESS_DETAIL_BASIC_STATUS_HPP

#include <boost/process/config.hpp>
#include <boost/process/pid_type.hpp>
#include <boost/asio.hpp>

namespace boost {
namespace process {
namespace detail {

/**
 * The basic_status class to wait for processes to exit.
 *
 * The basic_status class is a Boost.Asio I/O object and supports synchronous
 * and asynchronous wait operations. It must be instantiated with a Service.
 */
template <typename Service>
class basic_status
    : public boost::asio::basic_io_object<Service>
{
public:
    explicit basic_status(boost::asio::io_service &io_service)
    : boost::asio::basic_io_object<Service>(io_service)
    {
    }

    /**
     * Waits synchronously for a process to exit.
     */
    int wait(pid_type pid)
    {
        return this->service.wait(this->implementation, pid);
    }

    /**
     * Waits asynchronously for a process to exit.
     */
    template <typename Handler>
    void async_wait(pid_type pid, Handler handler)
    {
        this->service.async_wait(this->implementation, pid, handler);
    }
};

}
}
}

#endif 
