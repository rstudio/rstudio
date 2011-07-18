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
#include <boost/array.hpp> 
#include <string> 
#include <vector> 
#include <iostream> 

//[async_io 
boost::asio::io_service ioservice; 
boost::array<char, 4096> buf; 

void handler(const boost::system::error_code &ec, 
    std::size_t bytes_transferred); 

int main() 
{ 
    std::string exe = boost::process::find_executable_in_path("hostname"); 
    std::vector<std::string> args; 
    boost::process::context ctx; 
    ctx.streams[boost::process::stdout_id] = 
        boost::process::behavior::async_pipe(); 
    boost::process::child c = boost::process::create_child(exe, args, ctx); 
    boost::process::handle h = c.get_handle(boost::process::stdout_id); 
    boost::process::pipe read_end(ioservice, h.release()); 
    read_end.async_read_some(boost::asio::buffer(buf), handler); 
    ioservice.run(); 
} 

void handler(const boost::system::error_code &ec, 
    std::size_t bytes_transferred) 
{ 
    std::cout << std::string(buf.data(), bytes_transferred) << std::flush; 
} 
//] 
