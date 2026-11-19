#!/bin/bash
# Prüft den MCP-Server des Plugins gegen eine H2-Datenbank im Speicher (Hibiscus-Schema, erfundene Daten).
# Aufruf: test/run-local.sh /pfad/zu/jameica   (vorher ./build.sh ausführen)
set -u
J="${1:?Aufruf: test/run-local.sh /pfad/zu/jameica}"; cd "$(dirname "$0")/.."
H2=$(ls "$J"/lib/h2/migration-h2/enabled/h2-2*.jar | head -1)
T=$(mktemp -d); TOK=$(openssl rand -hex 32); HASH=$(printf '%s' "$TOK" | (sha256sum 2>/dev/null || shasum -a 256) | cut -d' ' -f1)
printf '%s %s test\n' "$HASH" $(( $(date +%s) + 300 )) > "$T/tokens"
OLD=$(openssl rand -hex 32); OHASH=$(printf '%s' "$OLD" | (sha256sum 2>/dev/null || shasum -a 256) | cut -d' ' -f1)
printf '%s %s abgelaufen\n' "$OHASH" $(( $(date +%s) - 5 )) >> "$T/tokens"
javac --release 17 -nowarn -cp build/classes -d "$T" test/TestMain.java || exit 1
java -cp "build/classes:$T:$H2" TestMain "$J/plugins/hibiscus/sql/h2-create.sql" test/testdaten.sql 18766 "$T/tokens" 20 > "$T/out" 2>&1 &
until grep -q -E 'bereit|Exception' "$T/out" 2>/dev/null; do :; done
grep -q bereit "$T/out" || { head -8 "$T/out"; exit 1; }
U=http://127.0.0.1:18766/mcp; rc=0
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST -d '{}' $U); echo "ohne Token: $code"; [ "$code" = 401 ] || rc=1
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST -H "Authorization: Bearer $OLD" -d '{}' $U); echo "abgelaufenes Token: $code"; [ "$code" = 401 ] || rc=1
code=$(curl -s -o /dev/null -w '%{http_code}' -X POST -H "Authorization: Bearer $TOK" -H 'Origin: https://example.org' -d '{}' $U); echo "fremder Origin: $code"; [ "$code" = 403 ] || rc=1
call(){ out=$(curl -s -m 10 -X POST -H "Authorization: Bearer $TOK" -d "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"$1\",\"arguments\":$2}}" $U)
  if echo "$out" | grep -q "\"isError\":$3"; then echo "ok      $1 $2"; else echo "FEHLER  $1 $2: $(echo "$out" | cut -c1-200)"; rc=1; fi; }
call konten '{}' false
call umsaetze '{"text":"MIETE","von":"2026-10-01"}' false
call umsaetze '{"limit":2,"offset":1,"aufsteigend":true,"betrag_max":0}' false
call umsatz '{"id":6}' false
for g in monat jahr kategorie gegenpartei konto; do call auswertung "{\"gruppierung\":\"$g\"}" false; done
call kategorien '{}' false
call protokoll '{"konto_id":1}' false
call umsaetze '{"von":"gestern"}' true
call umsatz '{"id":999}' true
call konten '{"x":1}' true
rm -rf "$T"; [ "$rc" -eq 0 ] && echo "Ergebnis: in Ordnung." || echo "Ergebnis: FEHLER."; exit $rc
