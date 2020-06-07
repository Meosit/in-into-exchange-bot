package by.mksn.inintobot.misc

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.serialization.*

@Serializer(forClass = BigDecimal::class)
@ExperimentalUnsignedTypes
object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveDescriptor("BigDecimal", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): BigDecimal = decoder.decodeDouble().toFiniteBigDecimal()

    override fun serialize(encoder: Encoder, value: BigDecimal) = encoder.encodeDouble(value.toStr().toDouble())
}