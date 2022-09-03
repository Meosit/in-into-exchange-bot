package org.mksn.inintobot.currency

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.mksn.inintobot.misc.toFixedScaleBigDecimal
import org.mksn.inintobot.misc.toStr
import java.math.BigDecimal

object CurrencySerializer : KSerializer<Currency> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Currency", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Currency = Currencies.forCode(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: Currency) = encoder.encodeString(value.code)
}