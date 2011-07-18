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

#include <boost/process/all.hpp> 

#if defined(BOOST_POSIX_API) 

#include <boost/assign/list_of.hpp> 
#include <string> 
#include <vector> 
#include <iostream> 

//[file_descriptors_main 
int main() 
{ 
    std::string exe = boost::process::find_executable_in_path("dbus-daemon"); 

    std::vector<std::string> args = boost::assign::list_of("--fork")
        ("--session")("--print-address=3")("--print-pid=4"); 

    boost::process::context ctx; 
    ctx.streams[3] = boost::process::behavior::pipe(
        boost::process::output_stream); 
    ctx.streams[4] = boost::process::behavior::pipe(
        boost::process::output_stream); 

    boost::process::child c = boost::process::create_child(exe, args, ctx); 

    boost::process::pistream isaddress(c.get_handle(3)); 
    std::cout << isaddress.rdbuf() << std::endl; 

    boost::process::pistream ispid(c.get_handle(4)); 
    std::cout << ispid.rdbuf() << std::endl; 
} 
//] 

#endif 
