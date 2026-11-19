package net.hibiscusmcp;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.List;

/**
 * Prueft befristete Bearer-Tokens. Die Datei enthaelt je Zeile
 * "[SHA-256 des Tokens als Hex] [Ablauf als Unix-Zeit] [Bezeichnung]" und wird bei jeder Anfrage neu gelesen.
 * Das Token selbst wird nirgends gespeichert.
 */
public class TokenStore
{
  private final File file;

  public TokenStore(File file)
  {
    this.file = file;
  }

  /** Liefert die Bezeichnung des Tokens oder null, wenn es fehlt, unbekannt oder abgelaufen ist. */
  public String check(String header)
  {
    if (header == null || !header.startsWith("Bearer "))
      return null;
    String token = header.substring(7).trim();
    if (token.length() < 32 || token.length() > 200)
      return null;
    try
    {
      byte[] digest = hex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))).getBytes(StandardCharsets.US_ASCII);
      long now = System.currentTimeMillis() / 1000L;
      List<String> lines = Files.readAllLines(this.file.toPath(), StandardCharsets.UTF_8);
      String label = null;
      for (String line : lines)
      {
        String[] p = line.trim().split("\\s+", 3);
        if (p.length < 2)
          continue;
        long expires;
        try { expires = Long.parseLong(p[1]); } catch (NumberFormatException e) { continue; }
        // Immer alle Zeilen vergleichen (gleichmaessige Laufzeit).
        boolean same = MessageDigest.isEqual(p[0].getBytes(StandardCharsets.US_ASCII), digest);
        if (same && expires > now)
          label = p.length > 2 ? p[2] : "token";
      }
      return label;
    }
    catch (Exception e)
    {
      return null; // keine Datei, nicht lesbar: kein Zugang
    }
  }

  private static String hex(byte[] b)
  {
    StringBuilder sb = new StringBuilder(b.length * 2);
    for (byte x : b)
      sb.append(Character.forDigit((x >> 4) & 0xF, 16)).append(Character.forDigit(x & 0xF, 16));
    return sb.toString();
  }
}
