#!/bin/sh
# Baut das Plugin und legt ZIP und plugin.xml in hibiscus.mcp/ ab (das ist der Inhalt des Jameica-Repositorys).
# Aufruf: ./build.sh /pfad/zu/jameica      (dort muss plugins/hibiscus liegen; benötigt JDK 17 oder neuer, zip)
set -eu
J="${1:?Aufruf: ./build.sh /pfad/zu/jameica}"
cd "$(dirname "$0")"
CP="$J/jameica.jar:$J/lib/de_willuhn_ds/de_willuhn_ds.jar:$J/lib/de_willuhn_util/de_willuhn_util.jar:$J/plugins/hibiscus/hibiscus.jar"
rm -rf build && mkdir -p build/classes build/pkg/hibiscus.mcp/bin hibiscus.mcp
javac --release 17 -encoding UTF-8 -nowarn -cp "$CP" -d build/classes $(find plugin/src -name '*.java')
jar cf build/pkg/hibiscus.mcp/hibiscus.mcp.jar -C build/classes .
cp plugin/plugin.xml plugin/README.txt build/pkg/hibiscus.mcp/
cp plugin/bin/mcp-token build/pkg/hibiscus.mcp/bin/ && chmod 0755 build/pkg/hibiscus.mcp/bin/mcp-token
rm -f hibiscus.mcp/hibiscus.mcp.zip
(cd build/pkg && zip -q -X -r ../../hibiscus.mcp/hibiscus.mcp.zip hibiscus.mcp)
cp plugin/plugin.xml hibiscus.mcp/plugin.xml
echo "Gebaut: hibiscus.mcp/hibiscus.mcp.zip"
