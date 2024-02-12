Money conversion with support of {currency_count} currencies, math and multi currency math expressions

Query syntax:
1) `<number/expr> [<currency>] [<extra currencies>...] [<API name>] [?<date>] [#<decimal digits>]`
2) `<currencied expr> [<extra currencies>...] [<API name>] [?<date>] [#<decimal digits>]`
3) `<currency> <union or '/'> <currency> [<API name>] [?<date>] [#<decimal digits>]`


Examples:
- `12 + 7`
- `2.4к zloty ECB #4`
- `43.3 uah !CZK !NOK`
- `(23 + 7)*6k USD &PLN`
- `(1+2) USD + 8 EUR / 4 Fixer`
- `(12keuro + 8k bucks)*2 !pounds`
- `$1 on 2022-09-14`
- `13 EUR at -2`
- `4.99 + 12%`
- `złoty into dollar ?22.09.2022`

Features:
- Supported currencies (❗support depends on chosen API): {currency_list}
- Supported rate APIs (см. /apis): {apis} 
- Default API is `OpenExchangeRates`, also 4 currencies displayed: `BYN`, `USD`, `EUR`, `RUB`
- Currency `USD` used by default, if it is not supported by the API, the API base currency used 
- Numbers are rounded for 2 decimal digits precision (1.99), this can be overridden by `#<decimal digits>`
- Allowed russian, english currency names (`евро`, `рубль`), symbols (`$`,`€`), country names (`Sweden`, `Венгрии`) or codes (`BYN`, `CZK`), also shortcuts (`бр`, `р`, `зл`, `грн`) - see /patterns
- Allowed math operators `*`, `/`, `+`, `-` and brackets.
- Suffixes _kilo-_ and _mega-_ can be used (`10к`/`1kk`/`1.9M`/etc.) with numbers.
- Additional currencies can be added by using `!` sign or union words (`into bucks`, `!JPY`, `in $`) (each addition should be separated with spaces).
- Rates on specific date can be queried by using `?` sign or union words  (`at 2022-09-14`, `?11.03.2021`), also using relative notation as `? -4` (four days from now) 
- To check when the rates were last updated use /apistatus
- Use /delete to remove your settings
- Support development: /donate

For feedback: @meosit