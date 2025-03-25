import os
import logging
from http.server import SimpleHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse

PORT = 8080
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DATA_DIR = os.path.join(BASE_DIR, "data")
os.makedirs(DATA_DIR, exist_ok=True)

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler()
    ]
)


class CustomHandler(SimpleHTTPRequestHandler):
    def log_request_details(self):
        """Log details of the incoming request."""
        parsed_path = urlparse(self.path).path
        logging.info("Path: %s", parsed_path)
        logging.info("Request Line: %s", self.requestline)
        logging.info("Headers:\n%s", self.headers)


    def do_GET(self):
        """Handle GET requests."""
        self.log_request_details()

        parsed_path = urlparse(self.path).path
        file_name = parsed_path.lstrip("/")  # Extract the file name from the path

        if file_name:
            self.serve_data_file(file_name)
        else:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b"Not found")

    def serve_data_file(self, file_name):
        """Serve the specified file from the data directory."""
        data_file_path = os.path.join(DATA_DIR, file_name)

        if os.path.exists(data_file_path):
            # Guess the content type based on the file extension
            content_type = self.guess_type(data_file_path)
            self.send_response(200)
            self.send_header("Content-type", content_type)
            self.end_headers()
            with open(data_file_path, "rb") as file:
                self.wfile.write(file.read())
        else:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b"File not found.")

    def do_POST(self):
        """Handle POST requests for file upload."""
        self.log_request_details()
        parsed_path = urlparse(self.path).path

        if parsed_path == "/":
            self.handle_file_upload()
        elif parsed_path.startswith("/dataplane/result") or "/dataplane/result" in parsed_path:
            logging.info("Received callback call from dataplane")
            content_length = int(self.headers.get('Content-Length', 0))
            body = self.rfile.read(content_length).decode("utf-8") if content_length else ""
            logging.info(f"Request Body: {body}")
            self.send_response(200)
            self.end_headers()
        else:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b"Not found")

    def handle_file_upload(self):
        """Handles file uploads sent as octet stream and saves them in the results directory."""
        file_name = self.headers.get("X-File-Name")

        if not file_name:
            file_name = "edps-result-data.zip"

        file_path = os.path.join(DATA_DIR, os.path.basename(file_name))

        try:
            content_length = int(self.headers.get("Content-Length", 0))

            with open(file_path, "wb") as output_file:
                output_file.write(self.rfile.read(content_length))

            self.send_response(201)
            self.end_headers()
            self.wfile.write(f"File uploaded successfully as {file_name}".encode())
        except Exception as e:
            self.send_response(500)
            self.end_headers()
            self.wfile.write(f"Error saving file: {e}".encode())


if __name__ == "__main__":
    server_address = ("", PORT)
    httpd = HTTPServer(server_address, CustomHandler)
    logging.info("Serving on http://localhost:%s", PORT)
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        httpd.server_close()
        logging.info("Server stopped.")