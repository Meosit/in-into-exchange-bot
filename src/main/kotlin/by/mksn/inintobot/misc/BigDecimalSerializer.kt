package by.mksn.inintobot.misc

import kotlinx.serialization.*
import java.math.BigDecimal

@Serializer(forClass = BigDecimal::class)
object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveDescriptor("BigDecimal", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): BigDecimal = decoder.decodeDouble().toFixedScaleBigDecimal()

    override fun serialize(encoder: Encoder, value: BigDecimal) = encoder.encodeDouble(value.toStr().toDouble())
}