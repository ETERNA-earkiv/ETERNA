from flask import Flask, request, jsonify
import subprocess
import os

app = Flask(__name__)

BASE_PATH = os.getenv("SHARED_VOLUME_PATH", "/roda/data")

@app.route("/convert", methods=["POST"])
def convert():
    data = request.json

    input_file = os.path.join(BASE_PATH, data["input"])
    output_file = os.path.join(BASE_PATH, data["output"])

    cmd = ["ffmpeg", "-y", "-i", input_file, output_file]

    try:
        subprocess.run(cmd, check=True)
        return jsonify({"status": "success"})
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8090)