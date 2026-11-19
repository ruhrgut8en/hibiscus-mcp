import java.io.File;
import java.sql.*;
import java.util.*;
import net.hibiscusmcp.*;

/** Startet den MCP-Server des Plugins gegen eine H2-Datenbank im Speicher (Schema von Hibiscus, erfundene Daten). */
public class TestMain
{
  public static void main(String[] args) throws Exception
  {
    Class.forName("org.h2.Driver");
    final Connection c = DriverManager.getConnection("jdbc:h2:mem:t;MODE=LEGACY", "sa", "");
    for (String f : new String[] {args[0], args[1]})
    {
      String sql = new String(java.nio.file.Files.readAllBytes(new File(f).toPath()), f.endsWith("create.sql") ? "ISO-8859-1" : "UTF-8");
      StringBuilder clean = new StringBuilder();
      for (String line : sql.split("\n")) if (!line.trim().startsWith("--")) clean.append(line).append("\n");
      for (String st : clean.toString().split(";\\s*\n"))
        if (st.trim().length() > 0) try (Statement s = c.createStatement()) { s.execute(st.replaceAll("(?i)\\b(big)?int\\(\\d+\\)", "$1int").replaceAll("(?i)IDENTITY\\(1\\)", "IDENTITY")); }
    }
    Tools.Db db = new Tools.Db()
    {
      public List<Map<String, Object>> query(String sql, Object[] p, int limit) throws Exception
      {
        try (PreparedStatement ps = c.prepareStatement(sql))
        {
          if (p != null) for (int i = 0; i < p.length; i++) ps.setObject(i + 1, p[i]);
          try (ResultSet rs = ps.executeQuery()) { return Tools.rows(rs, limit); }
        }
      }
    };
    McpServer s = new McpServer("127.0.0.1", Integer.parseInt(args[2]), new TokenStore(new File(args[3])), "test", db);
    s.start();
    System.out.println("bereit");
    Thread.sleep(Long.parseLong(args[4]) * 1000L);
    s.stop();
  }
}
