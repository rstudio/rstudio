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

#include <boost/asio.hpp> 
#include <boost/process/all.hpp> 
#include <string> 
#include <vector> 
#include <iostream> 

#if defined(BOOST_POSIX_API) 
#   include <sys/wait.h> 
#endif 

//[async_wait 
boost::asio::io_service ioservice; 

void end_wait(const boost::system::error_code &ec, int exit_code); 

int main() 
{ 
    std::string exe = boost::process::find_executable_in_path("hostname"); 
    std::vector<std::string> args; 
    boost::process::child c = boost::process::create_child(exe, args); 
    boost::process::status s(ioservice); 
    s.async_wait(c.get_id(), end_wait); 
    ioservice.run(); 
} 

void end_wait(const boost::system::error_code &ec, int exit_code) 
{ 
    if (!ec) 
    { 
#if defined(BOOST_POSIX_API) 
        if (WIFEXITED(exit_code)) 
            exit_code = WEXITSTATUS(exit_code); 
#endif 
        std::cout << "exit code: " << exit_code << std::endl; 
    } 
} 
//] 
