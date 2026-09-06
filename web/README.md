# Arcadia frontend showcase

The frontend is served by the Java application at `http://localhost:8080`. It uses the same MySQL database as the Java backend: register or log in, then create characters, add weapons, and submit scores from the browser.

## Connecting it to the Java application

Do not open `index.html` directly: it needs the local Java server so its `/api` calls can reach the database.
