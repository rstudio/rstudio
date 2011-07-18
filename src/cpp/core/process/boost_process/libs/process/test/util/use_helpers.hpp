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

#ifndef BOOST_PROCESS_TEST_UTIL_USE_HELPERS_HPP 
#define BOOST_PROCESS_TEST_UTIL_USE_HELPERS_HPP 

#include "boost.hpp" 
#include <string> 

const std::string &get_helpers_path() 
{ 
    static const std::string hp = 
        (bfs::initial_path() / butf::master_test_suite().argv[1]).string(); 
    return hp; 
} 

void check_helpers() 
{ 
    if (butf::master_test_suite().argc < 2) 
        throw butf::setup_error("path to helper expected"); 
    if (!bfs::exists(get_helpers_path())) 
        throw butf::setup_error( 
            "helper's path '" + get_helpers_path() + "' does not exists"); 
} 

#endif 
