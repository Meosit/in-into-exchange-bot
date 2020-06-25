Конвертація сум з підтримкою 21 валюти, арифметичних та багатовалютних виразів.

Синтаксиси запиту:
1) `<число/вираз> [<валюта>] [<назва API>] [<дод. валюти>...] [#<знаків після коми>]`
2) `<валютний вираз> [<назва API>] [<дод. валюти>...] [#<знаків після коми>]`

Приклади:
- `12 + 7`
- `2.4к злотых ЦБРФ`
- `43.3 грн !CZK !NOK #4`
- `(23 + 7)*6к USD +PLN`
- `(1+2) USD + 8 EUR / 4 NBRB`
- `(120рублей + 8 злотых)*2 !фунты`

Особливості:
- Підтримувані валюти (❗підтримка залежить від обраного API): `BYN`, `USD`, `EUR`, `RUB`, `UAH`, `PLN`, `CZK`, `GBP`, `JPY`, `CNY`, `KZT`, `CHF`, `BGN`, `TRY`, `CAD`, `ISK`, `DKK`, `SEK`, `NOK`, `ILS`, `BTC`, `ETH`
- Підтримувані API з курсами валют (см. /apis): [НБ РБ](http://www.nbrb.by/), [ЦБ РФ](http://cbr.ru/), [НБ Украины](https://bank.gov.ua/), [European Central Bank](https://www.ecb.europa.eu/home/html/index.en.html), [Fixer.io](https://fixer.io/), [OpenExchangeRates.org](https://openexchangerates.org/), [TraderMade.com](https://tradermade.com), [Forex API](https://fcsapi.com/)
- За замовчуванням використовується `OpenExchangeRates` API, а також відображаються 4 валюти: `BYN`, `USD`, `EUR`, `RUB`
- Валюта `BYN` використовується за замовчуванням, якщо вона не підтримується, то використовується базова валюта обраного API 
- Числа виводяться з точністю до двох знаків (1.99) післе коми, це може бути перевизначено опцією `p<знаків післе коми>`  
- Припустимо написання названої валюти на россійсьскій мові (`евро`, `рубль`), через символи (`$`,`€`) або через коди валют (`BYN`, `CZK`), також доступні різні скорочення (`бр`, `р`, `зл`, `грн`) - див. /patterns
- Замість суми допустимі арифметичні вирази з використанням операторів `*`, `/`, `+`, `-` та дужок.
- Припустимо використання суффіксів _кіло-_ і _мега-_ (`10к`/`1kk`/`1.9M`/etc.) в числах.
- Є можливість добавляти інші валюти до результату через символ ! або словами-союзами (`в злотые`, `!JPY`, `in $`) (кожна конструкція повинна бути відокремлена пробілами).
- Для зміни налаштувань за-умовчанням використовуйте команду /settings
- Для інформації про останнє обновлення курсів використовуйте команду /apistatus

Зворотній зв'язок: @meosit