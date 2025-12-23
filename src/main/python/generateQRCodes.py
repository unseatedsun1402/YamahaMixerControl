# requires Pillow, qrcode

import glob
import socket
from pathlib import Path

import qrcode


# Detect current LAN IP
def get_lan_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
    finally:
        s.close()
    return ip


# Base project root (two levels up from this script)
project_root = Path(__file__).resolve().parents[1]

# Paths
webapp_dir = project_root / "webapp"
output_dir = webapp_dir / "qrcodes"
output_dir.mkdir(parents=True, exist_ok=True)

# Build base URL
lan_ip = get_lan_ip()
base_url = f"http://{lan_ip}:8080/MidiControl/"

# Find all .html files in webapp
html_files = glob.glob(str(webapp_dir / "*.html"))

# Generate QR codes
for html_path in html_files:
    view = Path(html_path).name
    url = base_url + view
    qr = qrcode.make(url)
    output_file = output_dir / f"{view.replace('.html', '')}_qr.png"
    qr.save(output_file)
    print(f"Generated QR for {url} → {output_file}")
