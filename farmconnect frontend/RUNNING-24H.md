# Run FarmConnect for 24 hours

Run `.start-24h.ps1` in PowerShell from this folder. The launcher builds the backend if needed, then starts a hidden supervisor that keeps the computer awake and restarts the frontend or backend if its process exits. Logs are written to `runtime-logs`.

Open `http://127.0.0.1:5173/` on this computer. To stop the supervisor and its services, run `.stop-24h.ps1`.

The backend uses MongoDB. If local MongoDB is unavailable, the web server can start, but database-backed features will fail until a working MongoDB service or `MONGODB_URI` is available. Keep this computer powered on and connected to the network.
