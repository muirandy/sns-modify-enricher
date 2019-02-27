package sns.lando.modify.enhancer

import java.time.Duration
import java.time.temporal.TemporalUnit
import java.util.Properties

import org.apache.kafka.common.serialization.{Serde, Serdes}
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler
import org.apache.kafka.streams.kstream._
import org.apache.kafka.streams.{KafkaStreams, StreamsBuilder, StreamsConfig, Topology}


class KafkaSetup(private val server: String, private val port: String) {

  private implicit val stringSerde: Serde[String] = Serdes.String()

  private var stream: KafkaStreams = _

  private val voiceFeaturesParser = new VoiceFeaturesParser()
  private val serviceDetailsParser = new ServiceDetailsParser()
  private val modifyVoiceFeaturesInstructionSerializer = new ModifyVoiceFeaturesInstructionSerializer()

  val emptyStringPredicate: Predicate[_ >: String, _ >: String] = (_: String, value: String) => {
    value.isEmpty
  }

  val emptyVoiceFeaturesPredicate: Predicate[_ >: String, _ >: Option[VoiceFeatures]] = (_: String, value: Option[VoiceFeatures]) => {
    value.isEmpty
  }

  val emptyServiceDetailsPredicate: Predicate[_ >: String, _ >: Option[ServiceDetails]] = (_: String, value: Option[ServiceDetails]) => {
    value.isEmpty
  }

  def start(inputTopicName: String, servicesTopicName: String, outputTopicName: String) = {

    val bootstrapServers = server + ":" + port

    val streamingConfig = {
      val settings = new Properties
      settings.put(StreamsConfig.APPLICATION_ID_CONFIG, "sns-modify-enricher")
      settings.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
      settings.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String.getClass.getName)
      settings.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String.getClass.getName)
      settings.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, classOf[LogAndContinueExceptionHandler])
      settings
    }
    val topology = build(inputTopicName, servicesTopicName, outputTopicName)
    stream = new KafkaStreams(topology, streamingConfig)
    stream.start()
  }

  def shutDown(): Unit = {
    stream.close()
  }

  def build(inputTopicName: String, servicesTopicName: String, outputTopicName: String): Topology = {
    println("building topology")
    val builder = new StreamsBuilder
    val voiceFeaturesStream: KStream[String, VoiceFeatures] = getVoiceFeaturesStream(inputTopicName, builder)
    val servicesStream: KStream[String, ServiceDetails] = getServicesStream(servicesTopicName, builder)

    val joiner = new VoipServicesJoiner()
    val oneYearWindow = JoinWindows.of(Duration.ofDays(365))

    val joinedStream: KStream[String, ModifyVoiceFeaturesInstruction] =
      voiceFeaturesStream.join(servicesStream,
        joiner,
        oneYearWindow
        )

    val outputStream: KStream[String, String] = joinedStream.mapValues(mvfi => modifyVoiceFeaturesInstructionSerializer.serialize(mvfi))

    outputStream.to(outputTopicName)

    return builder.build()
  }

  def getVoiceFeaturesStream(voiceFeaturesTopicName: String, builder: StreamsBuilder): KStream[String, VoiceFeatures] = {
//    val builder = new StreamsBuilder
    val bareInputStream: KStream[String, String] = builder.stream(voiceFeaturesTopicName, Consumed.`with`(stringSerde, stringSerde))
    val validatedInputStream: KStream[String, String] = bareInputStream.filterNot(emptyStringPredicate)
    val optionalFeaturesStream: KStream[String, VoiceFeatures] = validatedInputStream.mapValues(line => voiceFeaturesParser.parse(line))
//      .filterNot(emptyVoiceFeaturesPredicate)
//    val featuresStream: KStream[String, VoiceFeatures] = optionalFeaturesStream.mapValues(f => f.get)
    optionalFeaturesStream.selectKey((k, v) => v.modifyVoiceFeaturesInstruction.serviceId)
  }

  def getServicesStream(servicesTopicName: String, builder: StreamsBuilder): KStream[String, ServiceDetails] = {
//    val builder = new StreamsBuilder
    val bareInputStream: KStream[String, String] = builder.stream(servicesTopicName, Consumed.`with`(stringSerde, stringSerde))
    val validatedInputStream: KStream[String, String] = bareInputStream.filterNot(emptyStringPredicate)
    val optionalServicesStream: KStream[String, ServiceDetails] = validatedInputStream.mapValues(line => serviceDetailsParser.parse(line))
//      .filterNot(emptyServiceDetailsPredicate)
//    val servicesStream: KStream[String, ServiceDetails] = optionalServicesStream.mapValues(s => s.get)
    optionalServicesStream.selectKey((k,v) => v.serviceId)
  }
}