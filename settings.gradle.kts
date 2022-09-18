rootProject.name = "in-into-exchange-bot"
include(":common", ":fetch-rates", ":exchange-rates")
include(":platform-gcp:store", ":platform-gcp:function")
include(":platform-gcp:function-exchange-rates", ":platform-gcp:function-fetch-rates")
