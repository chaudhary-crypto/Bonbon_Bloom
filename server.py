import http.server
import socketserver
import os
import mimetypes

PORT = 3000
DIRECTORY = os.path.join(os.getcwd(), "public")

class CustomHTTPHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

    def end_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Cache-Control", "no-cache, must-revalidate")
        path = self.translate_path(self.path)
        if os.path.isfile(path):
            filename = os.path.basename(path)
            if filename.endswith(".apk"):
                self.send_header("Content-Type", "application/vnd.android.package-archive")
                self.send_header("Content-Disposition", f'attachment; filename="{filename}"')
            elif filename.endswith(".aab"):
                self.send_header("Content-Type", "application/octet-stream")
                self.send_header("Content-Disposition", f'attachment; filename="{filename}"')
            elif filename.endswith(".zip"):
                self.send_header("Content-Type", "application/zip")
                self.send_header("Content-Disposition", f'attachment; filename="{filename}"')
            elif filename.endswith(".png"):
                self.send_header("Content-Type", "image/png")
                # When download query or direct file, allow downloading
                self.send_header("Content-Disposition", f'inline; filename="{filename}"')
            elif filename.endswith(".jpg") or filename.endswith(".jpeg"):
                self.send_header("Content-Type", "image/jpeg")
                self.send_header("Content-Disposition", f'inline; filename="{filename}"')
        super().end_headers()

class ThreadingHTTPServer(socketserver.ThreadingMixIn, http.server.HTTPServer):
    daemon_threads = True
    allow_reuse_address = True

if __name__ == "__main__":
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    print(f"Serving {DIRECTORY} on port {PORT} with ThreadingHTTPServer...")
    with ThreadingHTTPServer(("", PORT), CustomHTTPHandler) as httpd:
        httpd.serve_forever()
