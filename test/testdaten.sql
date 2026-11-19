-- Erfundene Testdaten für die Testumgebung. Keine echten Konten, keine echten Umsätze.
-- Das Konto ist als Offline-Konto markiert (flags = 2), es gibt keinen Bankzugang.
INSERT INTO konto (id, kontonummer, blz, name, bezeichnung, kundennummer, waehrung, saldo, saldo_datum, flags, iban, bic)
VALUES (1, '0000000001', '00000000', 'Testinhaber', 'Testkonto Giro', 'TEST', 'EUR', 1234.56, '2026-11-15 12:00:00', 2, 'DE00000000000000000001', 'TESTDEXXXXX');
INSERT INTO konto (id, kontonummer, blz, name, bezeichnung, kundennummer, waehrung, saldo, saldo_datum, flags, iban, bic)
VALUES (2, '0000000002', '00000000', 'Testinhaber', 'Testkonto Tagesgeld', 'TEST', 'EUR', 5000.00, '2026-11-15 12:00:00', 2, 'DE00000000000000000002', 'TESTDEXXXXX');
INSERT INTO umsatztyp (id, name, pattern, isregex, umsatztyp) VALUES (1, 'Miete', 'miete', 0, 0);
INSERT INTO umsatztyp (id, name, pattern, isregex, umsatztyp) VALUES (2, 'Gehalt', 'gehalt', 0, 1);
INSERT INTO umsatz (id, konto_id, empfaenger_konto, empfaenger_name, betrag, zweck, zweck2, datum, valuta, saldo, art, umsatztyp_id, flags)
VALUES (1, 1, 'DE00000000000000000099', 'Beispiel Arbeitgeber GmbH', 2500.00, 'Gehalt Oktober', 'Testbuchung', '2026-10-01', '2026-10-01', 3000.00, 'Gutschrift', 2, 0);
INSERT INTO umsatz (id, konto_id, empfaenger_konto, empfaenger_name, betrag, zweck, datum, valuta, saldo, art, umsatztyp_id, flags)
VALUES (2, 1, 'DE00000000000000000098', 'Beispiel Vermieter', -800.00, 'Miete Oktober', '2026-10-02', '2026-10-02', 2200.00, 'Dauerauftrag', 1, 0);
INSERT INTO umsatz (id, konto_id, empfaenger_konto, empfaenger_name, betrag, zweck, datum, valuta, saldo, art, flags)
VALUES (3, 1, 'DE00000000000000000097', 'Beispiel Supermarkt', -45.67, 'Einkauf 100% Bio_Test', '2026-10-15', '2026-10-15', 2154.33, 'Kartenzahlung', 0);
INSERT INTO umsatz (id, konto_id, empfaenger_konto, empfaenger_name, betrag, zweck, datum, valuta, saldo, art, umsatztyp_id, flags)
VALUES (4, 1, 'DE00000000000000000099', 'Beispiel Arbeitgeber GmbH', 2500.00, 'Gehalt November', '2026-11-01', '2026-11-01', 4654.33, 'Gutschrift', 2, 0);
INSERT INTO umsatz (id, konto_id, empfaenger_konto, empfaenger_name, betrag, zweck, datum, valuta, saldo, art, umsatztyp_id, flags)
VALUES (5, 1, 'DE00000000000000000098', 'Beispiel Vermieter', -800.00, 'Miete November', '2026-11-02', '2026-11-02', 3854.33, 'Dauerauftrag', 1, 0);
INSERT INTO umsatz (id, konto_id, empfaenger_konto, empfaenger_name, betrag, zweck, datum, valuta, art, flags)
VALUES (6, 1, NULL, 'Beispiel Tankstelle', -60.00, 'Vormerkung Tanken', '2026-11-14', '2026-11-14', 'Kartenzahlung', 1);
INSERT INTO umsatz (id, konto_id, empfaenger_konto, empfaenger_name, betrag, zweck, datum, valuta, saldo, art, flags)
VALUES (7, 2, 'DE00000000000000000001', 'Testinhaber', 5000.00, 'Umbuchung Ruecklage', '2026-09-20', '2026-09-20', 5000.00, 'Gutschrift', 0);
INSERT INTO protokoll (konto_id, kommentar, datum, typ) VALUES (1, 'Testeintrag: Umsaetze abgerufen', '2026-11-15 12:00:00', 1);
