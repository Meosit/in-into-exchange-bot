# In/Into Exchange Bot

Telegram bot for easy currency conversion with extensive features. This is re-implemented version of [Telegram-Currency-Bot](https://github.com/Meosit/Telegram-Currency-Bot)    

See [@inintobot](https://t.me/inintobot)

## Bot Description

Money conversion with support of 21 currencies, math and multi currency math expressions

#### Query syntax:
1) `<number/expr> [<currency>] [<API name>] [<extra currencies>...] [#<decimal digits>]`
2) `<currencied expr> [<API name>] [<extra currencies>...] [#<decimal digits>]`

#### Examples:
- `12 + 7`
- `2.4к zloty ECB #4`
- `43.3 uah !CZK !NOK`
- `(23 + 7)*6k USD +PLN`
- `(1+2) USD + 8 EUR / 4 Fixer`
- `(12keuro + 8k bucks)*2 !pounds`

#### Features:
- Supported currencies (❗support depends on chosen API): `BYN`, `USD`, `EUR`, `RUB`, `UAH`, `PLN`, `CZK`, `GBP`, `JPY`, `CNY`, `KZT`, `CHF`, `BGN`, `TRY`, `CAD`, `ISK`, `DKK`, `SEK`, `NOK`, `ILS`, `BTC`, `ETH`
- Supported rate APIs (см. /apis): [NBRB](http://www.nbrb.by/), [CBR](http://cbr.ru/), [NBU](https://bank.gov.ua/), [European Central Bank](https://www.ecb.europa.eu/home/html/index.en.html), [Fixer.io](https://fixer.io/), [OpenExchangeRates.org](https://openexchangerates.org/), [TraderMade.com](https://tradermade.com), [Forex API](https://fcsapi.com/)
- Default API is `OpenExchangeRates`, also 4 currencies displayed: `BYN`, `USD`, `EUR`, `RUB`
- Currency `BYN` used by default, if it is not supported by the API, the API base currency used
- Numbers are rounded for 2 decimal digits precision (1.99), this can be overridden by `#<decimal digits>`
- Allowed russian currency names (`евро`, `рубль`), symbols (`$`,`€`) or codes (`BYN`, `CZK`), also shortcuts (`бр`, `р`, `зл`, `грн`) - see /patterns
- Allowed math operators `*`, `/`, `+`, `-` and brackets.
- Suffixes _kilo-_ and _mega-_ can be used (`10к`/`1kk`/`1.9M`/etc.) with numbers.
- Additional currencies can be added by using ! sign or union words (`into bucks`, `!JPY`, `in $`) (each addition should be separated with spaces).
- To customize defaults use /settings command
- To check when the rates were last updated use /apistatus

## Deployment

This bot is deployed on [Heroku](https://www.heroku.com/what) and fits into [free dyno hours](https://devcenter.heroku.com/articles/free-dyno-hours) limit taking into account that account is verified with a credit card (it consumes ~700 hours out of 1000). Settings are stored in heroku Postgres DB and hence up 10K users is supported for free.

#### Required environment variables:

* `APP_URL` - URL of the deployed application, for example `https://inintobot.herokuapp.com/`. Used only for self-ping ability
* `USE_PING` - Whether to use self-ping logic in order to keep the Heroku dyno always available and reduce bot delay
* `ALLOWED_TOKENS_STRING` - Comma-separated list of [Telegram bot tokens](https://core.telegram.org/bots/api#authorizing-your-bot) which are allowed to query this backend. This allows to configure multiple serving bots/migrate to new one
* `DEPRECATED_TOKENS_STRING` - Comma-separated list of [Telegram bot tokens](https://core.telegram.org/bots/api#authorizing-your-bot) which are deprecated, but temporarily available. For bots in this list a deprecation notice will be displayed for every outgoing message.   
* `DATABASE_URL` - Heroku Postgres database url taken corresponding settings at [data.heroku.com](https://data.heroku.com/)
* `FIXER_ACCESS_KEY` - API key for [Fixer](https://fixer.io/) (1000 calls/month, refresh every 1 hour)
* `OPENEXCHANGERATES_ACCESS_KEY` - API key for [OpenExchangeRates](https://openexchangerates.org/) (1000 calls/month, refresh every 1 hour)
* `TRADERMADE_ACCESS_KEY` - API key for [TraderMade](https://marketdata.tradermade.com/) (1000 calls/month, refresh every 1 hour)
* `FOREX_ACCESS_KEY` - API key for [Forex](https://fcsapi.com/) (500 calls/month, refresh every 2 hours)

> ⚠️ **If you're deploying this bot**: please amend output strings (help and deprecation message) with your own username for contact.