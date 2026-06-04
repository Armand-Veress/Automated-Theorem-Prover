cmake_minimum_required(VERSION 4.1)
project(DLX)

set(CMAKE_CXX_STANDARD 20)

add_library(dlx_solver SHARED dlx_solver.cpp)
set_target_properties(dlx_solver PROPERTIES PREFIX "")
