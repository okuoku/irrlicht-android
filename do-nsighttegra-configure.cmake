# FIXME: It seems CMake cannot detect one automatically
set(ENV{VCTargetsPath}
    "C:\\Program Files (x86)\\MSBuild\\Microsoft.Cpp\\v4.0\\V140")
execute_process(
    COMMAND ${CMAKE_COMMAND}
    -G "Visual Studio 14"
    -DCMAKE_SYSTEM_NAME=Android
    ${CMAKE_CURRENT_LIST_DIR}
    )
