import os
import logging
import json
import uuid
import socket
from http.server import SimpleHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse
import zipfile

PORT = 8081
TIMEOUT = 120  # Increased timeout from 30 to 120 seconds
BASE_DIR = os.path.dirname(os.path.abspath(__file__))  # Base directory is /server
DATA_DIR = os.path.join(BASE_DIR, "data")  # Directory for GET request files
os.makedirs(DATA_DIR, exist_ok=True)

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler()
    ]
)

class CustomHandler(SimpleHTTPRequestHandler):
    # Set timeout using the constant
    timeout = TIMEOUT
    
    def log_request_details(self):
        """Log details of the incoming request."""
        logging.info("--- Request Details ---")
        logging.info(f"Method: {self.command}")
        logging.info(f"Path: {self.path}")
        logging.info(f"Headers: {self.headers}")

    def do_GET(self):
        """Handle GET requests."""
        self.log_request_details()
        parsed_path = urlparse(self.path).path

        if parsed_path.startswith("/v1/dataspace/analysisjob/") and "/result" in parsed_path:
            self.serve_analysisjob_result(parsed_path)
        elif parsed_path.startswith("/v1/dataspace/analysisjob/") and "/status" in parsed_path:
            self.serve_analysisjob_status(parsed_path)
        else:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b"Not found")

    def serve_analysisjob_status(self, parsed_path):
        """Serve job status."""
        try:
            parts = parsed_path.split("/")
            job_id = parts[4]  # Extract job_id
        except IndexError:
            self.send_response(400)
            self.end_headers()
            self.wfile.write(b"Invalid path format.")
            return

        response = {
            "job_id": f"{job_id}",
            "state": "WAITING_FOR_DATA",
            "state_detail": "Job is waiting for data to be uploaded."
        }
        response_bytes = json.dumps(response, indent=2).encode('utf-8')

        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(response_bytes)))
        self.end_headers()
        self.wfile.write(response_bytes)

    def serve_analysisjob_result(self, parsed_path):
        """Serve the processed .zip file."""
        try:
            parts = parsed_path.split("/")
            job_id = parts[4]  # Extract job_id
        except IndexError:
            self.send_response(400)
            self.end_headers()
            self.wfile.write(b"Invalid path format.")
            return

        # Look for the corresponding .zip file
        zip_file_name = f"{job_id}.zip"
        zip_file_path = os.path.join(DATA_DIR, zip_file_name)

        if os.path.exists(zip_file_path):
            try:
                file_size = os.path.getsize(zip_file_path)
                logging.info(f"Serving zip file {zip_file_name} with size {file_size} bytes")
                
                # Check if file is empty and create dummy content if needed
                if file_size == 0:
                    logging.warning(f"Zip file {zip_file_path} is empty (0 bytes), creating dummy content")
                    # Create a dummy CSV file
                    csv_file_path = os.path.join(DATA_DIR, f"{job_id}.csv")
                    with open(csv_file_path, "w") as f:
                        f.write("dummy,data\n1,2\n")
                        f.flush()
                        os.fsync(f.fileno())
                    
                    # Create a new zip file with the dummy CSV
                    with zipfile.ZipFile(zip_file_path, "w") as zip_file:
                        zip_file.write(csv_file_path, arcname=f"{job_id}.csv")
                    
                    # Update file size
                    file_size = os.path.getsize(zip_file_path)
                    logging.info(f"Created dummy zip file with size {file_size} bytes")
                
                self.send_response(200)
                self.send_header("Content-Type", "application/zip")
                self.send_header("Content-Length", str(file_size))
                self.send_header("Content-Disposition", f"attachment; filename=\"{zip_file_name}\"")
                self.send_header("Connection", "keep-alive")
                self.end_headers()
                
                # Stream the file in chunks to avoid memory issues
                with open(zip_file_path, "rb") as file:
                    total_sent = 0
                    chunk_size = 8192  # 8KB chunks
                    while True:
                        chunk = file.read(chunk_size)
                        if not chunk:
                            break
                        try:
                            bytes_sent = self.wfile.write(chunk)
                            self.wfile.flush()  # Flush after each chunk
                            total_sent += bytes_sent
                            if total_sent % (1024 * 1024) == 0:  # Log every 1MB
                                logging.info(f"Sent {total_sent} of {file_size} bytes")
                        except (BrokenPipeError, ConnectionResetError, socket.error) as e:
                            logging.error(f"Connection error while streaming file: {str(e)}")
                            # Don't try to send more data or error responses if connection is broken
                            return
                    
                    logging.info(f"Successfully sent {total_sent} of {file_size} bytes")
            except Exception as e:
                logging.error(f"Error serving zip file: {str(e)}")
                try:
                    self.send_response(500)
                    self.send_header("Content-Type", "application/json")
                    self.end_headers()
                    response = {
                        "status": "error",
                        "message": f"Error serving file: {str(e)}"
                    }
                    self.wfile.write(json.dumps(response).encode())
                except (BrokenPipeError, ConnectionResetError, socket.error):
                    logging.error("Could not send error response - connection already closed")
        else:
            logging.warning(f"Result file not found: {zip_file_path}")
            
            # Create a dummy file if it doesn't exist
            try:
                logging.info(f"Creating dummy zip file for job_id: {job_id}")
                # Create a dummy CSV file
                csv_file_path = os.path.join(DATA_DIR, f"{job_id}.csv")
                with open(csv_file_path, "w") as f:
                    f.write("dummy,data\n1,2\n")
                    f.flush()
                    os.fsync(f.fileno())
                
                # Create a new zip file with the dummy CSV
                with zipfile.ZipFile(zip_file_path, "w") as zip_file:
                    zip_file.write(csv_file_path, arcname=f"{job_id}.csv")
                
                file_size = os.path.getsize(zip_file_path)
                logging.info(f"Created dummy zip file with size {file_size} bytes")
                
                self.send_response(200)
                self.send_header("Content-Type", "application/zip")
                self.send_header("Content-Length", str(file_size))
                self.send_header("Content-Disposition", f"attachment; filename=\"{zip_file_name}\"")
                self.send_header("Connection", "keep-alive")
                self.end_headers()
                
                # Stream the file
                with open(zip_file_path, "rb") as file:
                    total_sent = 0
                    chunk_size = 8192  # 8KB chunks
                    while True:
                        chunk = file.read(chunk_size)
                        if not chunk:
                            break
                        try:
                            bytes_sent = self.wfile.write(chunk)
                            self.wfile.flush()
                            total_sent += bytes_sent
                        except (BrokenPipeError, ConnectionResetError, socket.error) as e:
                            logging.error(f"Connection error while streaming dummy file: {str(e)}")
                            return
                    
                    logging.info(f"Successfully sent dummy file: {total_sent} bytes")
            except Exception as e:
                logging.error(f"Error creating and serving dummy file: {str(e)}")
                self.send_response(404)
                self.end_headers()
                self.wfile.write(b"Result file not found and could not create dummy file.")

    def do_POST(self):
        """Handle POST requests."""
        self.log_request_details()
        parsed_path = urlparse(self.path).path

        if parsed_path == "/v1/dataspace/analysisjob":
            self.handle_create_analysisjob()
        elif parsed_path.startswith("/v1/dataspace/analysisjob/") and "/data/file" in parsed_path:
            self.handle_analysisjob_upload(parsed_path)
        elif parsed_path == "/connector/edp":
            self.handle_daseen_create()
        elif parsed_path.startswith("/connector/edp/"):
            self.handle_daseen_upload(parsed_path)
        else:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b"Not found")

    def do_PUT(self):
        """Handle PUT requests."""
        self.log_request_details()
        parsed_path = urlparse(self.path).path

        if parsed_path.startswith("/connector/edp/"):
            self.handle_daseen_update(parsed_path)
        else:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b"Not found")

    def do_DELETE(self):
        """Handle DELETE requests."""
        self.log_request_details()
        parsed_path = urlparse(self.path).path

        if parsed_path.startswith("/connector/edp/"):
            self.handle_daseen_delete(parsed_path)
        else:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b"Not found")

    def handle_create_analysisjob(self):
        """Create a new analysis job and return mock response."""
        try:
            # Verify content type if needed
            content_type = self.headers.get('Content-Type', '')
            if content_type and 'application/json' not in content_type:
                self.send_error(415, "Unsupported Media Type. Expected application/json")
                return

            job_id = str(uuid.uuid4())

            response = {
                "job_id": job_id,
                "state": "WAITING_FOR_DATA",
                "state_detail": "Job is waiting for data to be uploaded."
            }

            response_bytes = json.dumps(response, indent=2).encode('utf-8')

            self.send_response(201)
            self.send_header("Content-Length", str(len(response_bytes)))
            self.send_header("Content-Type", "application/json")
            self.send_header("Location", "/v1/dataspace/analysisjob")
            self.end_headers()
            self.wfile.write(response_bytes)
            self.wfile.flush()

            logging.info(f"Created new analysis job with ID: {job_id}")
        except (BrokenPipeError, ConnectionResetError, socket.error) as e:
            logging.error(f"Connection error while creating analysis job: {str(e)}")
        except Exception as e:
            logging.error(f"Error creating analysis job: {str(e)}")
            try:
                self.send_error(500, f"Internal server error: {str(e)}")
            except (BrokenPipeError, ConnectionResetError, socket.error):
                logging.error("Could not send error response - connection already closed")

    def handle_analysisjob_upload(self, parsed_path):
        """Handle file uploads for analysis jobs."""
        try:
            # Expected format: /v1/dataspace/analysisjob/{job_id}/data
            parts = parsed_path.split("/")
            job_id = parts[4]  # Extract job_id
        except IndexError:
            self.send_response(400)
            self.end_headers()
            self.wfile.write(b"Invalid path format.")
            return

        # Save the file as {jobid}-{filename}.csv
        result_file_name = f"{job_id}.csv"
        result_file_path = os.path.join(DATA_DIR, result_file_name)

        try:
            # Send response headers first to prevent timeout
            self.send_response(201)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            
            # Read the entire input at once
            data = self.rfile.read()
            bytes_written = len(data)
            logging.info(f"Read {bytes_written} bytes from input")
            
            # Write the data to file
            with open(result_file_path, "wb") as output_file:
                output_file.write(data)
                output_file.flush()
                os.fsync(output_file.fileno())
            
            logging.info(f"File upload complete. Written {bytes_written} bytes to {result_file_path}")

            # Process the file if we received data
            if bytes_written > 0:
                self.process_edps(result_file_name)
                response = {
                    "status": "success",
                    "message": "File uploaded and processed successfully."
                }
            else:
                response = {
                    "status": "error",
                    "message": "No data received."
                }
            
            self.wfile.write(json.dumps(response).encode())
            self.wfile.flush()

        except Exception as e:
            logging.error(f"Error during file upload: {str(e)}")
            if not self.wfile.closed:
                response = {
                    "status": "error",
                    "message": str(e)
                }
                self.wfile.write(json.dumps(response).encode())
                self.wfile.flush()

    def process_edps(self, result_file_name):
        """Process the CSV file into a .zip file."""
        result_file_path = os.path.join(DATA_DIR, result_file_name)
        zip_file_name = result_file_name.replace(".csv", ".zip")
        zip_file_path = os.path.join(DATA_DIR, zip_file_name)

        try:
            # Check if the source file exists and has content
            if not os.path.exists(result_file_path):
                logging.error(f"Source file {result_file_path} does not exist")
                return
            
            file_size = os.path.getsize(result_file_path)
            logging.info(f"Processing file {result_file_name} with size {file_size} bytes")
            
            if file_size == 0:
                logging.error(f"Source file {result_file_path} is empty (0 bytes)")
                # Create a dummy content if the file is empty to ensure we have something to return
                with open(result_file_path, "w") as f:
                    f.write("dummy,data\n1,2\n")
                    f.flush()
                    os.fsync(f.fileno())
                logging.info(f"Added dummy content to empty file {result_file_path}")
            
            # Create the zip file with the CSV
            with zipfile.ZipFile(zip_file_path, "w") as zip_file:
                zip_file.write(result_file_path, arcname=result_file_name)
                
            # Verify the zip file was created and has content
            if os.path.exists(zip_file_path):
                zip_size = os.path.getsize(zip_file_path)
                logging.info(f"Created zip file {zip_file_name} with size {zip_size} bytes")
                if zip_size == 0:
                    logging.error(f"Created zip file is empty (0 bytes)")
            else:
                logging.error(f"Failed to create zip file {zip_file_path}")
                
        except Exception as e:
            logging.error(f"Error creating zip file: {str(e)}")

    def handle_daseen_create(self):
        """Handle creation of new daseen connector."""
        try:
            resource_id = str(uuid.uuid4())  # Generate a unique ID
            
            self.send_response(201)
            self.send_header("Content-Type", "application/json")
            self.end_headers()

            response = {
                "state": "SUCCESS",
                "id": resource_id,
                "message": "EDPS connector created"
            }
            self.wfile.write(json.dumps(response).encode())
        except (BrokenPipeError, ConnectionResetError, socket.error) as e:
            logging.error(f"Connection error while creating daseen connector: {str(e)}")
        except Exception as e:
            logging.error(f"Error creating connector: {str(e)}")
            try:
                self.send_response(500)
                self.end_headers()
                self.wfile.write(f"Error creating connector: {str(e)}".encode())
            except (BrokenPipeError, ConnectionResetError, socket.error):
                logging.error("Could not send error response - connection already closed")

    def handle_daseen_upload(self, parsed_path):
        """Handle daseen data upload to specific connector."""
        try:
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()

            response = {
                "state": "SUCCESS",
                "state_detail": "EDPS data published to Daseen"
            }
            self.wfile.write(json.dumps(response).encode())
        except (BrokenPipeError, ConnectionResetError, socket.error) as e:
            logging.error(f"Connection error while handling daseen upload: {str(e)}")
        except Exception as e:
            logging.error(f"Error uploading data: {str(e)}")
            try:
                self.send_response(500)
                self.end_headers()
                self.wfile.write(f"Error uploading data: {str(e)}".encode())
            except (BrokenPipeError, ConnectionResetError, socket.error):
                logging.error("Could not send error response - connection already closed")

    def handle_daseen_update(self, parsed_path):
        """Handle daseen connector updates."""
        try:
            connector_id = parsed_path.split("/")[-1]

            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()

            response = {
                "state": "SUCCESS",
                "state_detail": f"EDPS connector {connector_id} updated successfully"
            }
            self.wfile.write(json.dumps(response).encode())
        except (BrokenPipeError, ConnectionResetError, socket.error) as e:
            logging.error(f"Connection error while updating daseen connector: {str(e)}")
        except Exception as e:
            logging.error(f"Error updating connector: {str(e)}")
            try:
                self.send_response(500)
                self.end_headers()
                self.wfile.write(f"Error updating connector: {str(e)}".encode())
            except (BrokenPipeError, ConnectionResetError, socket.error):
                logging.error("Could not send error response - connection already closed")

    def handle_daseen_delete(self, parsed_path):
        """Handle daseen connector deletion."""
        try:
            # For 204 No Content, we only send the status code and end headers
            self.send_response(204)
            self.end_headers()
            # Don't write any response body for 204
            
        except (BrokenPipeError, ConnectionResetError, socket.error) as e:
            logging.error(f"Connection error while deleting daseen connector: {str(e)}")
        except Exception as e:
            logging.error(f"Error deleting connector: {str(e)}")
            try:
                self.send_response(500)
                self.end_headers()
                self.wfile.write(f"Error deleting connector: {str(e)}".encode())
            except (BrokenPipeError, ConnectionResetError, socket.error):
                logging.error("Could not send error response - connection already closed")


if __name__ == "__main__":
    server_address = ("", PORT)
    httpd = HTTPServer(server_address, CustomHandler)
    httpd.timeout = TIMEOUT    
    logging.info("Serving EDPS mock on http://localhost:%s with timeout of %s seconds", PORT, TIMEOUT)
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        logging.info("Server stopped by user.")
    except Exception as e:
        logging.error(f"Server error: {str(e)}")
    finally:
        httpd.server_close()
        logging.info("Server stopped.")
