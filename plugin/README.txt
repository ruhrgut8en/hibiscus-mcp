hibiscus.mcp - MCP-Server für Hibiscus als Jameica-Plugin (nur lesend)

Nachrüsten: Diesen Ordner "hibiscus.mcp" in das Plugin-Verzeichnis von Jameica legen
(~/.jameica/plugins) oder die ZIP-Datei über Datei > Einstellungen > Plugins installieren,
danach Jameica neu starten. Voraussetzung: Jameica ab 2.10, Hibiscus ab 2.10, Java 17.

Der Server lauscht standardmäßig auf 127.0.0.1:8765, Endpunkt /mcp (MCP Streamable HTTP).
Zugang nur mit befristetem Token: bin/mcp-token neu 60
Einstellungen über Umgebungsvariablen HIBISCUS_MCP_BIND, HIBISCUS_MCP_PORT, HIBISCUS_MCP_TOKEN_FILE
oder in cfg/net.hibiscusmcp.Plugin.properties (bind, port, tokenfile).

Werkzeuge: konten, umsaetze, umsatz, auswertung, kategorien, protokoll.
Das Plugin ändert keine Daten und löst keine Zahlungen aus.
