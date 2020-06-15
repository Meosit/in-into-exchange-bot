package by.mksn.inintobot

fun main() {
//    println("Start handling request")
//    val botToken: String? = input.queryStringParameters["token"]
//    val requestBody: String? = input.body
//    if (botToken != null && requestBody != null) {
//        println("Body: ${requestBody.replace("\n", "")}")
//        try {
//            runBlocking { handleTelegramRequest(requestBody, botToken) }
//        } catch (e: Exception) {
//            val sw = StringWriter()
//            val pw = PrintWriter(sw)
//            e.printStackTrace(pw)
//            (e as? ClientRequestException)?.response?.content?.let {
//                println(runBlocking { it.readUTF8Line() })
//            }
//            println("Uncaught exception: ${sw.toString().replace("\n", "")}")
//        }
//    }
}