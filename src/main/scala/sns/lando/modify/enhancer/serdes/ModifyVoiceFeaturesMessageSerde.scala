package sns.lando.modify.enhancer.serdes

import java.util

import org.apache.kafka.common.serialization.{Deserializer, Serde, Serializer}
import sns.lando.modify.enhancer.{ModifyVoiceFeaturesMessage, VoiceFeaturesParser}

class ModifyVoiceFeaturesMessageSerde extends Serde[ModifyVoiceFeaturesMessage] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {

  }

  override def close(): Unit = {

  }

  override def serializer(): Serializer[ModifyVoiceFeaturesMessage] = {
    return new ModifyVoiceFeaturesMessageSerializer()
  }

  override def deserializer(): Deserializer[ModifyVoiceFeaturesMessage] = {
    return new ModifyVoiceFeaturesMessageDeserializer()
  }
}

class ModifyVoiceFeaturesMessageSerializer extends Serializer[ModifyVoiceFeaturesMessage] {
  val voiceFeaturesParser = new VoiceFeaturesParser()
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {

  }

  override def serialize(topic: String, voiceFeatures: ModifyVoiceFeaturesMessage): Array[Byte] = {
    return voiceFeaturesParser.parse(voiceFeatures).getBytes
  }

  override def close(): Unit = {

  }
}

class ModifyVoiceFeaturesMessageDeserializer extends Deserializer[ModifyVoiceFeaturesMessage] {
  val voiceFeaturesParser = new VoiceFeaturesParser()
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {

  }

  override def deserialize(topic: String, data: Array[Byte]): ModifyVoiceFeaturesMessage = {
    return voiceFeaturesParser.parse(new String(data))
  }

  override def close(): Unit = {

  }
}