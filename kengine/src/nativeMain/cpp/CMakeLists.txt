# src/main/cpp/CMakeLists.txt

cmake_minimum_required(VERSION 3.10)
project(Box2DWrapper)

# Set C++ standard
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Specify the path to Box2D
add_subdirectory(${CMAKE_SOURCE_DIR}/../../external/box2d build_box2d)

# Include directories
include_directories(include)
include_directories(${CMAKE_SOURCE_DIR}/../../external/box2d/include)

# Add the wrapper source
add_library(box2d_wrapper STATIC box2d_wrapper.cpp)

# Link Box2D
target_link_libraries(box2d_wrapper PRIVATE box2d)