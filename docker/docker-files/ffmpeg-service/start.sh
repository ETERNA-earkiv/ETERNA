#!/bin/sh

apk add --no-cache python3 py3-pip
pip install flask --break-system-packages

python3 /roda/data/ffmpeg-service/server.py