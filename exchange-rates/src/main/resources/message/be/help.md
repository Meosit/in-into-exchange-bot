Канвертацыя сум з падтрымкай {currency_count} валют, арыфметычных і шматвалютных выразаў.

Сінтаксісы запыту:
1) `<лічба/выраз> [<валюта>] [<назва API>] [<дад. валюты>...] [#<знакаў пасля коскі>]`
2) `<валютны выраз> [<назва API>] [<дад. валюты>...] [#<знакаў пасля коскі>]`

Прыклады:
- `12 + 7`
- `2.4к злотых ЦБРФ`
- `43.3 грн !CZK !NOK #4`
- `(23 + 7)*6к USD +PLN`
- `(1+2) USD + 8 EUR / 4 NBRB`
- `(120 рублёў + 8 злотых)*2 !фунты`

Асаблівасці:
- Падтрымоўваныя валюты (❗падтрымка залежыць ад абранага API): {currency_list}
- Падтрымоўныя API з курсамі валют (см. /apis): {apis}
- Па змаўчанні выкарыстоўваецца `OpenExchangeRates` API, а таксама адлюстроўваюцца 4 валюты: `BYN`, `USD`, `EUR`, `RUB`
- Валюта `USD` выкарыстоўваецца па змаўчанні, калі яна не падтрымліваецца, то выкарстоўваецца базавая валюта абранага API 
- Лічбы выводзяцца з дакладнасцю да двух знакаў (1.99)пасля коскі, гэта можа быць перавызначана опцыяй `#<знакаў пасля коскі>`  
- Дапушчальна напісанне назваў валют на рускай мове (`евро`, `рубль`), праз сімвалы (`$`,`€`) або праз коды валют (`BYN`, `CZK`), таксама дапускаюцца розныя скарачэнні (`бр`, `р`, `зл`, `грн`) - гл. /patterns
- Замест сумы дапускаюцца арыфметычныя выразы з выкарыстоўваннем аператараў `*`, `/`, `+`, `-` і дужак.
- Дапушчальна выкарыстоўванне суфіксаў _кіла-_ и _мега-_ (`10к`/`1kk`/`1.9M`/etc.) у лічбах.
- Ёсць магчымасць дадаваць іншыя валюты да выніку праз сімвал ! або словамі-саюзамі (`у злотыя`, `!JPY`, `in $`) (кожная канструкцыя павінна быць аддзеленая прабеламі).
- Для змены налад па змаўчанні выкарыстоўвайце каманду /settings
- Для інфармацыі аб апошнім абнаўленні курсаў выкарыстоўвайце каманду /apistatus

Зваротная сувязь: @meosit