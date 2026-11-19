package net.hibiscusmcp;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Die MCP-Werkzeuge. Alle lesen nur. Die Abfragen sind bewusst schlicht gehalten, damit sie
 * mit H2 (Standard von Hibiscus) und MySQL/MariaDB gleich laufen; gerechnet wird in Java.
 * Neue Werkzeuge: Unterklasse von Tool anlegen und in ALL eintragen.
 */
public final class Tools
{
  public static final int MAX_ROWS = 1000;

  /** Datenzugriff, getrennt gehalten, damit die Werkzeuge ohne Jameica pruefbar sind. */
  public interface Db
  {
    List<Map<String, Object>> query(String sql, Object[] params, int limit) throws Exception;
  }

  /** Zugriff ueber die Datenbankverbindung von Hibiscus selbst. */
  public static final class HibiscusDb implements Db
  {
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> query(String sql, Object[] params, final int limit) throws Exception
    {
      return (List<Map<String, Object>>) de.willuhn.jameica.hbci.Settings.getDBService().execute(sql, params,
        new de.willuhn.datasource.rmi.ResultSetExtractor()
        {
          public Object extract(ResultSet rs) throws java.sql.SQLException
          {
            return rows(rs, limit);
          }
        });
    }
  }

  public static List<Map<String, Object>> rows(ResultSet rs, int limit) throws java.sql.SQLException
  {
    ResultSetMetaData md = rs.getMetaData();
    List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
    while (out.size() < limit && rs.next())
    {
      Map<String, Object> row = new LinkedHashMap<String, Object>();
      for (int c = 1; c <= md.getColumnCount(); c++)
      {
        Object v = rs.getObject(c);
        if (v instanceof java.sql.Clob)
        {
          java.sql.Clob clob = (java.sql.Clob) v;
          v = clob.getSubString(1, (int) Math.min(clob.length(), 100000L));
        }
        else if (v instanceof java.sql.Timestamp)
          v = v.toString().substring(0, 19);
        else if (v instanceof java.util.Date || v instanceof java.time.temporal.Temporal)
          v = v.toString();
        else if (v instanceof byte[])
          v = "(binaer)";
        row.put(md.getColumnLabel(c).toLowerCase(), v);
      }
      out.add(row);
    }
    return out;
  }

  public abstract static class Tool
  {
    public final String name;
    public final String description;
    public final Map<String, Object> properties = new LinkedHashMap<String, Object>();
    public final List<String> required = new ArrayList<String>();

    protected Tool(String name, String description)
    {
      this.name = name;
      this.description = description;
    }

    protected Tool p(String name, String type, String description)
    {
      this.properties.put(name, Json.obj("type", type, "description", description));
      return this;
    }

    public abstract Object call(Map<String, Object> a, Db db) throws Exception;
  }

  // ------------------------------------------------------------------ Hilfen

  private static Long num(Map<String, Object> a, String k)
  {
    Object v = a.get(k);
    if (v == null) return null;
    if (!(v instanceof Number)) throw new IllegalArgumentException(k + " muss eine Zahl sein.");
    return Long.valueOf(((Number) v).longValue());
  }

  private static int limit(Map<String, Object> a, int def)
  {
    Long l = num(a, "limit");
    return (int) Math.max(1, Math.min(l == null ? def : l.longValue(), MAX_ROWS));
  }

  private static java.sql.Date date(Object v, String k)
  {
    try { return java.sql.Date.valueOf(java.time.LocalDate.parse(String.valueOf(v))); }
    catch (Exception e) { throw new IllegalArgumentException(k + " muss ein Datum JJJJ-MM-TT sein."); }
  }

  private static final String[] TEXT_COLS = {"empfaenger_name", "empfaenger_name2", "empfaenger_konto", "zweck", "zweck2", "zweck3", "art", "kommentar"};

  /** Baut die WHERE-Bedingung aus den gemeinsamen Filtern. Werte gehen nur als Parameter in die Abfrage. */
  private static String where(Map<String, Object> a, List<Object> params)
  {
    List<String> c = new ArrayList<String>();
    if (a.get("konto_id") != null) { c.add("u.konto_id = ?"); params.add(num(a, "konto_id")); }
    if (a.get("von") != null) { c.add("u.datum >= ?"); params.add(date(a.get("von"), "von")); }
    if (a.get("bis") != null) { c.add("u.datum <= ?"); params.add(date(a.get("bis"), "bis")); }
    if (a.get("betrag_min") != null) { c.add("u.betrag >= ?"); params.add(dbl(a, "betrag_min")); }
    if (a.get("betrag_max") != null) { c.add("u.betrag <= ?"); params.add(dbl(a, "betrag_max")); }
    if (a.get("kategorie_id") != null) { c.add("u.umsatztyp_id = ?"); params.add(num(a, "kategorie_id")); }
    if (a.get("text") != null && String.valueOf(a.get("text")).length() > 0)
    {
      String like = "%" + String.valueOf(a.get("text")).toLowerCase() + "%";
      StringBuilder sb = new StringBuilder("(");
      for (int k = 0; k < TEXT_COLS.length; k++)
      {
        if (k > 0) sb.append(" OR ");
        sb.append("LOWER(u.").append(TEXT_COLS[k]).append(") LIKE ?");
        params.add(like);
      }
      c.add(sb.append(")").toString());
    }
    return c.isEmpty() ? "" : " WHERE " + String.join(" AND ", c);
  }

  private static Double dbl(Map<String, Object> a, String k)
  {
    Object v = a.get(k);
    if (!(v instanceof Number)) throw new IllegalArgumentException(k + " muss eine Zahl sein.");
    return Double.valueOf(((Number) v).doubleValue());
  }

  private static Tool filters(Tool t)
  {
    return t.p("konto_id", "integer", "id aus dem Werkzeug konten")
            .p("von", "string", "Buchungsdatum ab, JJJJ-MM-TT")
            .p("bis", "string", "Buchungsdatum bis, JJJJ-MM-TT")
            .p("text", "string", "Teiltext in Gegenpartei, Gegenkonto, Verwendungszweck, Art oder Kommentar")
            .p("betrag_min", "number", "Mindestbetrag (Ausgaben sind negativ)")
            .p("betrag_max", "number", "Hoechstbetrag (Ausgaben sind negativ)")
            .p("kategorie_id", "integer", "nur fest zugeordnete Kategorie, id aus kategorien");
  }

  private static int flags(Object v)
  {
    return v instanceof Number ? ((Number) v).intValue() : 0;
  }

  private static String zweck(Map<String, Object> r)
  {
    StringBuilder sb = new StringBuilder();
    for (String k : new String[] {"zweck", "zweck2", "zweck3"})
    {
      Object v = r.remove(k);
      if (v != null && v.toString().trim().length() > 0)
        sb.append(sb.length() > 0 ? " " : "").append(v.toString().trim());
    }
    return sb.toString();
  }

  private static double round(double d)
  {
    return Math.round(d * 100.0) / 100.0;
  }

  private static final String UMSATZ_FROM = " FROM umsatz u JOIN konto k ON k.id = u.konto_id LEFT JOIN umsatztyp t ON t.id = u.umsatztyp_id";

  // ------------------------------------------------------------------ Werkzeuge

  public static final List<Tool> ALL = Collections.unmodifiableList(Arrays.asList(

    new Tool("konten", "Alle Konten mit Saldo, Saldo-Datum, Zugangsweg sowie Anzahl und Zeitraum der Umsaetze.")
    {
      public Object call(Map<String, Object> a, Db db) throws Exception
      {
        List<Map<String, Object>> konten = db.query("SELECT id, bezeichnung, name AS inhaber, iban, bic, blz, kontonummer, waehrung, saldo, "
          + "saldo_available AS saldo_verfuegbar, saldo_datum, kategorie, backend_class, flags FROM konto ORDER BY id", null, MAX_ROWS);
        Map<Object, Map<String, Object>> stat = new java.util.HashMap<Object, Map<String, Object>>();
        for (Map<String, Object> r : db.query("SELECT konto_id, COUNT(*) AS anzahl, MIN(datum) AS erster, MAX(datum) AS letzter FROM umsatz GROUP BY konto_id", null, MAX_ROWS))
          stat.put(String.valueOf(r.get("konto_id")), r);
        for (Map<String, Object> k : konten)
        {
          Object backend = k.remove("backend_class");
          String b = backend == null ? "FinTS" : backend.toString();
          k.put("zugang", b.substring(b.lastIndexOf('.') + 1));
          int f = flags(k.remove("flags"));
          k.put("deaktiviert", Boolean.valueOf((f & 1) != 0));
          k.put("offline", Boolean.valueOf((f & 2) != 0));
          Map<String, Object> s = stat.get(String.valueOf(k.get("id")));
          k.put("umsaetze", s == null ? Long.valueOf(0) : s.get("anzahl"));
          k.put("erster_umsatz", s == null ? null : s.get("erster"));
          k.put("letzter_umsatz", s == null ? null : s.get("letzter"));
        }
        return Json.obj("konten", konten);
      }
    },

    filters(new Tool("umsaetze", "Umsaetze suchen und auflisten, neueste zuerst. Alle Filter sind optional und werden mit UND verknuepft.")
    {
      public Object call(Map<String, Object> a, Db db) throws Exception
      {
        List<Object> params = new ArrayList<Object>();
        String where = where(a, params);
        int limit = limit(a, 100);
        Long off = num(a, "offset");
        long offset = off == null ? 0 : Math.max(0, off.longValue());
        String order = Boolean.TRUE.equals(a.get("aufsteigend")) ? "ASC" : "DESC";
        params.add(Integer.valueOf(limit + 1));
        params.add(Long.valueOf(offset));
        List<Map<String, Object>> rows = db.query("SELECT u.id, u.konto_id, k.bezeichnung AS konto, u.datum, u.valuta, u.betrag, k.waehrung, "
          + "u.empfaenger_name AS gegenpartei, u.empfaenger_konto AS gegenkonto, u.zweck, u.zweck2, u.zweck3, u.art, "
          + "t.name AS kategorie, u.kommentar, u.flags" + UMSATZ_FROM + where
          + " ORDER BY u.datum " + order + ", u.id " + order + " LIMIT ? OFFSET ?", params.toArray(), limit + 1);
        boolean more = rows.size() > limit;
        if (more) rows = rows.subList(0, limit);
        for (Map<String, Object> r : rows)
        {
          r.put("verwendungszweck", zweck(r));
          r.put("vorgemerkt", Boolean.valueOf((flags(r.remove("flags")) & 1) != 0));
        }
        return Json.obj("umsaetze", rows, "weitere_vorhanden", Boolean.valueOf(more), "offset", Long.valueOf(offset));
      }
    }).p("limit", "integer", "hoechstens 1000, Standard 100")
      .p("offset", "integer", "zum Blaettern")
      .p("aufsteigend", "boolean", "aelteste zuerst"),

    new Tool("umsatz", "Ein Umsatz mit allen Feldern (Referenzen, Mandat, Glaeubiger-ID, Primanota).")
    {
      { p("id", "integer", "id des Umsatzes"); required.add("id"); }
      public Object call(Map<String, Object> a, Db db) throws Exception
      {
        if (num(a, "id") == null) throw new IllegalArgumentException("id fehlt.");
        List<Map<String, Object>> rows = db.query("SELECT u.*, k.bezeichnung AS konto, k.waehrung, t.name AS kategorie" + UMSATZ_FROM + " WHERE u.id = ?", new Object[] {num(a, "id")}, 1);
        if (rows.isEmpty()) throw new IllegalArgumentException("Kein Umsatz mit dieser id.");
        return rows.get(0);
      }
    },

    filters(new Tool("auswertung", "Summen der Umsaetze: Anzahl, Einnahmen, Ausgaben, Summe je monat, jahr, kategorie, gegenpartei oder konto. "
      + "kategorie zaehlt nur fest zugeordnete Kategorien, nicht die in Hibiscus per Suchmuster angezeigten.")
    {
      public Object call(Map<String, Object> a, Db db) throws Exception
      {
        final String g = a.get("gruppierung") == null ? "monat" : String.valueOf(a.get("gruppierung"));
        if (!Arrays.asList("monat", "jahr", "kategorie", "gegenpartei", "konto").contains(g))
          throw new IllegalArgumentException("gruppierung muss monat, jahr, kategorie, gegenpartei oder konto sein.");
        List<Object> params = new ArrayList<Object>();
        String where = where(a, params);
        final int cap = 200000;
        List<Map<String, Object>> rows = db.query("SELECT u.datum, u.betrag, u.empfaenger_name, t.name AS kategorie, k.id AS konto_id, k.bezeichnung AS konto"
          + UMSATZ_FROM + where, params.toArray(), cap);
        Map<String, double[]> sum = new TreeMap<String, double[]>();
        for (Map<String, Object> r : rows)
        {
          String key;
          String datum = String.valueOf(r.get("datum"));
          if (g.equals("monat")) key = datum.length() >= 7 ? datum.substring(0, 7) : datum;
          else if (g.equals("jahr")) key = datum.length() >= 4 ? datum.substring(0, 4) : datum;
          else if (g.equals("kategorie")) key = r.get("kategorie") == null ? "(ohne Kategorie)" : r.get("kategorie").toString();
          else if (g.equals("konto")) key = r.get("konto_id") + " " + (r.get("konto") == null ? "" : r.get("konto"));
          else key = r.get("empfaenger_name") == null || r.get("empfaenger_name").toString().trim().length() == 0 ? "(ohne Angabe)" : r.get("empfaenger_name").toString().trim();
          double b = r.get("betrag") instanceof Number ? ((Number) r.get("betrag")).doubleValue() : 0d;
          double[] s = sum.get(key);
          if (s == null) { s = new double[3]; sum.put(key, s); }
          s[0]++;
          if (b > 0) s[1] += b; else s[2] += b;
        }
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, double[]> e : sum.entrySet())
        {
          double[] s = e.getValue();
          out.add(Json.obj("gruppe", e.getKey(), "anzahl", Long.valueOf((long) s[0]), "einnahmen", Double.valueOf(round(s[1])),
            "ausgaben", Double.valueOf(round(s[2])), "summe", Double.valueOf(round(s[1] + s[2]))));
        }
        if (g.equals("kategorie") || g.equals("gegenpartei"))
          Collections.sort(out, new Comparator<Map<String, Object>>()
          {
            public int compare(Map<String, Object> x, Map<String, Object> y)
            {
              return Long.compare(((Long) y.get("anzahl")).longValue(), ((Long) x.get("anzahl")).longValue());
            }
          });
        int limit = limit(a, 200);
        boolean more = out.size() > limit;
        return Json.obj("gruppierung", g, "zeilen", more ? out.subList(0, limit) : out, "weitere_vorhanden", Boolean.valueOf(more),
          "ausgewertete_umsaetze", Integer.valueOf(rows.size()), "unvollstaendig", Boolean.valueOf(rows.size() >= cap));
      }
    }).p("gruppierung", "string", "monat (Standard), jahr, kategorie, gegenpartei oder konto")
      .p("limit", "integer", "hoechstens 1000, Standard 200"),

    new Tool("kategorien", "Umsatzkategorien mit Suchmuster und Anzahl fest zugeordneter Umsaetze.")
    {
      public Object call(Map<String, Object> a, Db db) throws Exception
      {
        List<Map<String, Object>> kat = db.query("SELECT id, name, nummer, parent_id, pattern AS suchmuster, isregex AS regex, umsatztyp AS typ, kommentar FROM umsatztyp ORDER BY name", null, MAX_ROWS);
        Map<String, Object> n = new java.util.HashMap<String, Object>();
        for (Map<String, Object> r : db.query("SELECT umsatztyp_id, COUNT(*) AS anzahl FROM umsatz WHERE umsatztyp_id IS NOT NULL GROUP BY umsatztyp_id", null, MAX_ROWS))
          n.put(String.valueOf(r.get("umsatztyp_id")), r.get("anzahl"));
        for (Map<String, Object> k : kat)
        {
          Object c = n.get(String.valueOf(k.get("id")));
          k.put("fest_zugeordnete_umsaetze", c == null ? Long.valueOf(0) : c);
        }
        return Json.obj("kategorien", kat);
      }
    },

    new Tool("protokoll", "Letzte Protokolleintraege der Bankabrufe, optional je Konto. typ: 1 = Erfolg, 2 = Fehler, sonst Information.")
    {
      { p("konto_id", "integer", "id aus dem Werkzeug konten"); p("limit", "integer", "Standard 50"); }
      public Object call(Map<String, Object> a, Db db) throws Exception
      {
        List<Object> params = new ArrayList<Object>();
        String where = "";
        if (a.get("konto_id") != null) { where = " WHERE p.konto_id = ?"; params.add(num(a, "konto_id")); }
        int limit = limit(a, 50);
        params.add(Integer.valueOf(limit));
        return Json.obj("protokoll", db.query("SELECT p.datum, p.konto_id, k.bezeichnung AS konto, p.typ, p.kommentar FROM protokoll p LEFT JOIN konto k ON k.id = p.konto_id"
          + where + " ORDER BY p.datum DESC, p.id DESC LIMIT ?", params.toArray(), limit));
      }
    }
  ));

  private Tools() {}
}
