package net.hibiscusmcp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Kleiner JSON-Leser und -Schreiber ohne Fremdbibliothek. Objekte = Map, Listen = List. */
public final class Json
{
  private final String s;
  private int i = 0;

  private Json(String s) { this.s = s; }

  public static Object parse(String text)
  {
    Json j = new Json(text);
    Object v = j.value(0);
    j.ws();
    if (j.i != j.s.length())
      throw new IllegalArgumentException("JSON: Zeichen nach dem Ende");
    return v;
  }

  private void ws()
  {
    while (i < s.length() && " \t\r\n".indexOf(s.charAt(i)) >= 0) i++;
  }

  private Object value(int depth)
  {
    if (depth > 64) throw new IllegalArgumentException("JSON: zu tief verschachtelt");
    ws();
    if (i >= s.length()) throw new IllegalArgumentException("JSON: unerwartetes Ende");
    char c = s.charAt(i);
    if (c == '{')
    {
      i++;
      Map<String, Object> m = new LinkedHashMap<String, Object>();
      ws();
      if (peek('}')) return m;
      while (true)
      {
        ws();
        String k = string();
        ws();
        expect(':');
        m.put(k, value(depth + 1));
        ws();
        if (peek('}')) return m;
        expect(',');
      }
    }
    if (c == '[')
    {
      i++;
      List<Object> l = new ArrayList<Object>();
      ws();
      if (peek(']')) return l;
      while (true)
      {
        l.add(value(depth + 1));
        ws();
        if (peek(']')) return l;
        expect(',');
      }
    }
    if (c == '"') return string();
    if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
    if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
    if (s.startsWith("null", i)) { i += 4; return null; }
    int start = i;
    while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
    if (start == i) throw new IllegalArgumentException("JSON: unerwartetes Zeichen an Stelle " + i);
    String n = s.substring(start, i);
    try
    {
      if (n.indexOf('.') < 0 && n.indexOf('e') < 0 && n.indexOf('E') < 0) return Long.valueOf(n);
      return Double.valueOf(n);
    }
    catch (NumberFormatException e) { throw new IllegalArgumentException("JSON: ungueltige Zahl"); }
  }

  private boolean peek(char c)
  {
    if (i < s.length() && s.charAt(i) == c) { i++; return true; }
    return false;
  }

  private void expect(char c)
  {
    if (!peek(c)) throw new IllegalArgumentException("JSON: '" + c + "' erwartet an Stelle " + i);
  }

  private String string()
  {
    expect('"');
    StringBuilder sb = new StringBuilder();
    while (true)
    {
      if (i >= s.length()) throw new IllegalArgumentException("JSON: Zeichenkette nicht beendet");
      char c = s.charAt(i++);
      if (c == '"') return sb.toString();
      if (c != '\\') { sb.append(c); continue; }
      if (i >= s.length()) throw new IllegalArgumentException("JSON: Zeichenkette nicht beendet");
      char e = s.charAt(i++);
      switch (e)
      {
        case 'n': sb.append('\n'); break;
        case 't': sb.append('\t'); break;
        case 'r': sb.append('\r'); break;
        case 'b': sb.append('\b'); break;
        case 'f': sb.append('\f'); break;
        case 'u':
          if (i + 4 > s.length()) throw new IllegalArgumentException("JSON: \\u unvollstaendig");
          sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
          i += 4;
          break;
        default: sb.append(e);
      }
    }
  }

  public static String write(Object o)
  {
    StringBuilder sb = new StringBuilder();
    write(sb, o);
    return sb.toString();
  }

  private static void write(StringBuilder sb, Object o)
  {
    if (o == null) { sb.append("null"); return; }
    if (o instanceof Boolean) { sb.append(o.toString()); return; }
    if (o instanceof Double || o instanceof Float)
    {
      double d = ((Number) o).doubleValue();
      if (Double.isNaN(d) || Double.isInfinite(d)) sb.append("null");
      else sb.append(new java.math.BigDecimal(Double.toString(d)).toPlainString());
      return;
    }
    if (o instanceof java.math.BigDecimal) { sb.append(((java.math.BigDecimal) o).toPlainString()); return; }
    if (o instanceof Number) { sb.append(o.toString()); return; }
    if (o instanceof Map)
    {
      sb.append('{');
      boolean first = true;
      for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet())
      {
        if (!first) sb.append(',');
        first = false;
        str(sb, String.valueOf(e.getKey()));
        sb.append(':');
        write(sb, e.getValue());
      }
      sb.append('}');
      return;
    }
    if (o instanceof Iterable)
    {
      sb.append('[');
      boolean first = true;
      for (Object x : (Iterable<?>) o)
      {
        if (!first) sb.append(',');
        first = false;
        write(sb, x);
      }
      sb.append(']');
      return;
    }
    str(sb, o.toString());
  }

  private static void str(StringBuilder sb, String v)
  {
    sb.append('"');
    for (int k = 0; k < v.length(); k++)
    {
      char c = v.charAt(k);
      switch (c)
      {
        case '"': sb.append("\\\""); break;
        case '\\': sb.append("\\\\"); break;
        case '\n': sb.append("\\n"); break;
        case '\r': sb.append("\\r"); break;
        case '\t': sb.append("\\t"); break;
        default:
          if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
          else sb.append(c);
      }
    }
    sb.append('"');
  }

  /** Bequemer Aufbau: obj("a", 1, "b", "x"). */
  public static Map<String, Object> obj(Object... kv)
  {
    Map<String, Object> m = new LinkedHashMap<String, Object>();
    for (int k = 0; k + 1 < kv.length; k += 2)
      m.put(String.valueOf(kv[k]), kv[k + 1]);
    return m;
  }
}
