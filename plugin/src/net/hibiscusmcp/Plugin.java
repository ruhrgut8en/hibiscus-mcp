package net.hibiscusmcp;

import java.io.File;

import de.willuhn.jameica.plugin.AbstractPlugin;
import de.willuhn.jameica.system.Settings;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

/**
 * Jameica-Plugin: stellt die Hibiscus-Daten als MCP-Server bereit (nur lesend).
 * Einstellungen (Datei cfg/net.hibiscusmcp.Plugin.properties oder Umgebungsvariablen):
 *   bind  / HIBISCUS_MCP_BIND        Standard 127.0.0.1
 *   port  / HIBISCUS_MCP_PORT        Standard 8765
 *   tokenfile / HIBISCUS_MCP_TOKEN_FILE   Standard [Arbeitsverzeichnis]/hibiscus.mcp/tokens
 */
public class Plugin extends AbstractPlugin
{
  private McpServer server;

  @Override
  public void init() throws ApplicationException
  {
    try
    {
      Settings s = getResources().getSettings();
      String bind = env("HIBISCUS_MCP_BIND", s.getString("bind", "127.0.0.1"));
      int port = Integer.parseInt(env("HIBISCUS_MCP_PORT", String.valueOf(s.getInt("port", 8765))));
      String file = env("HIBISCUS_MCP_TOKEN_FILE", s.getString("tokenfile", getResources().getWorkPath() + File.separator + "tokens"));
      new File(getResources().getWorkPath()).mkdirs();
      this.server = new McpServer(bind, port, new TokenStore(new File(file)), getManifest().getVersion().toString());
      this.server.start();
      Logger.info("hibiscus.mcp: MCP-Server auf " + bind + ":" + port + ", Tokendatei " + file);
    }
    catch (Exception e)
    {
      // Ein Fehler hier darf den Start von Hibiscus nicht verhindern.
      Logger.error("hibiscus.mcp: MCP-Server nicht gestartet", e);
    }
  }

  @Override
  public void shutDown()
  {
    if (this.server != null)
      this.server.stop();
  }

  private static String env(String name, String fallback)
  {
    String v = System.getenv(name);
    return v != null && v.trim().length() > 0 ? v.trim() : fallback;
  }
}
