@echo off
echo Activating Virtual Environment...
call .\.venv\Scripts\activate.bat
echo Starting Django Development Server...
cd backend
python manage.py runserver
pause
