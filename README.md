# In/Into Exchange Bot

Telegram bot for easy currency conversion with extensive features.

See [@inintobot](https://t.me/inintobot)

#### Deployment

The implementation consists of the following components:

1. `buildSrc` - project with Gradle build-scripts
2. `common` - common utilities and interfaces, also objects containing list of available currencies and rate apis.
3. `fetch-rates` - function which downloads the latest rates and populates the designated store (supposed to be running on schedule)  
   See class `org.mksn.inintobot.rates.FetchRequest` for input object parameters
4. `exchange-rates` - main module which containing bot logic
5. `migrate-settings` - one-time function which designated to migrate existing settings from Postgres SQL.  
   See class `org.mksn.inintobot.migrate.MigrateInput` for input object parameters
   **To deploy:**
   ```shell
   gcloud functions deploy inintobot-exchange-rates \ 
     --entry-point=org.mksn.inintobot.gcp.function.Function \
     --memory=512MB --runtime=java11 --trigger-http --source=build/libs \
     --region=europe-central2
   ```
6. `componud-server` - project which connects `fetch-rates` and `exchange-rates` functions into a single executable server
   **Require envs from both `fetch-rates` and `exchange-rates` projects to be set**
7. `platform-gcp` - set of platform-related projects (or just configurations) to deploy the bot on GCP and save data into Firestore
   * `app-engine` - GCP version of `compund-server` project. Fill `app.yaml` with required envs.
   * `function` - adapter implementation between common function interface and GCP cloud functions interface
   * `store` - GCP Firestore implementation of common storage interfaces
   * `function-exchange-rates` - GCP version of `exchange-rates` project. **To deploy:**
     ```shell
     gcloud functions deploy inintobot-exchange-rates \ 
       --entry-point=org.mksn.inintobot.gcp.function.Function \
       --memory=512MB --runtime=java11 --trigger-http --source=build/libs \
       --region=europe-central2 --min-instances=1 \
       --set-env-vars BOT_TOKEN=<bot token>,BOT_USERNAME=<bot username>,CREATOR_ID=<id of admin>
     ```
   * `function-fetch-rates` - GCP version of `exchange-rates` project. **To deploy:**  
     ```shell
     gcloud functions deploy inintobot-fetch-rates \ 
       --entry-point=org.mksn.inintobot.gcp.function.Function \
       --memory=512MB --runtime=java11 --trigger-http --source=build/libs \
       --region=europe-central2 \
       --set-env-vars FOREX_ACCESS_KEY=<token>,FIXER_ACCESS_KEY=<token>,TRADERMADE_ACCESS_KEY=<token>,OPENEXCHANGERATES_ACCESS_KEY=<token>
     ```
   * `function-migrate-settings` - GCP version of `migrate-settings` project. **To deploy:**
     ```shell
     gcloud functions deploy inintobot-migrate-settings \ 
       --entry-point=org.mksn.inintobot.gcp.function.Function \
       --memory=512MB --runtime=java11 --trigger-http --source=build/libs \
       --region=europe-central2
     ```

To set webhook:

```shell
curl -X "POST" "https://api.telegram.org/bot<token>/setWebhook" \
  -d '{"drop_pending_updates": true, "url": "<app-link>", "allowed_updates": ["edited_message", "inline_query", "message", "chosen_inline_result", "callback_query"]}' \ 
  -H 'Content-Type: application/json; charset=utf-8'
```