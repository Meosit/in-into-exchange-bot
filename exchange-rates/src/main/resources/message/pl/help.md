Przeliczanie kwot, wyrażeń arytmetycznych i wielowalutowych dla ponad {currency_count} walut.

Składnia zapytania:
1) `<liczba/wyrażenie> [<waluta>] [<nazwa API>] [<waluty dodatkowe>...] [#<znaków po przecinku>]`
2) `<wyrażenie walutowe> [<nazwa API>] [<waluty dodatkowe>...] [#<znaków po przecinku>]`

Przykład:
- `12 + 7`
- `2.4к zloty ECB #4`
- `43.3 uah !CZK !NOK`
- `(23 + 7)*6k USD +PLN`
- `(1+2) USD + 8 EUR / 4 Fixer`
- `(12keuro + 8k bucks)*2 !pounds`

Właściwości:
- Waluty obsługiwane (❗zależy od wybranego API): {currency_list}
- Obsługiwane API wraz z kursami wymiany walut (zobacz /apis): {apis}
- Domyślnie się używa `OpenExchangeRates` API, wyświetla się 4 waluty: `BYN`, `USD`, `EUR`, `RUB`
- Waluta `USD` się używa domyślnie; jeżeli nie jest obsługiwana, używa się waluty podstawowej wybranego API 
- Liczby się wyświetlają do dwóch miejsc po przecinku (1.99), można to zmienić opcją `#<znaków po przecinku>`  
- Walutę można opisać w języku polskim (`euro`, `rubel`), używając symboli (`$`,`€`) oraz znaki walut (`BYN`, `CZK`), dodatkowo używając różnego rodzaju skrótów (`zl`, `rub`, `dol`, `grn`) - zobacz /patterns
- Oprócz sumy są dopuszczalne wyrażenia arytmetyczne z użyciem operatorów `*`, `/`, `+`, `-` oraz nawiasów.
- Dopuszczalne jest użycie przyrostków _kilo-_ oraz _mega-_ (`10к`/`1kk`/`1.9M`/etc.) przy liczbach.
- Istnieje możliwość dodania innych walut do wyniku poprzez dodanie symbolu ! lub spójnika (`na złotówki`, `!JPY`, `in $`) (każda składnia powinna być oddzielona spacjami).
- W celu zmiany domyślnych ustawień proszę używać polecenie /settings
- W celu sprawdzenie ostatniej aktualizacji kursów wymiany proszę używać polecenie /apistatus

Kontakt: @meosit (english/русский)