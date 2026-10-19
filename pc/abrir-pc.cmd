@echo off
rem Abre o Countdown Geek para PC em modo app (sem barra de endereco).
rem Uso: abrir-pc.cmd          -> janela cheia
rem      abrir-pc.cmd -Kiosk  -> modo quiosque (sem saida facil; use Alt+F4)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0abrir-pc.ps1" %*
