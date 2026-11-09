@echo off
setlocal
pushd "%~dp0"
call gradlew.bat clean lwjgl3:run
set "game_exit_code=%errorlevel%"
popd
exit /b %game_exit_code%
