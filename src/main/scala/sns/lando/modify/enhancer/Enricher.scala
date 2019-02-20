package sns.lando.modify.enhancer

import org.json4s.ParserUtil.ParseException
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}
import org.json4s.{Formats, NoTypeHints}

class Enricher {

  def enrich(textLine: String): String = {
    println(s"input: $textLine")
    implicit val formats: Formats = Serialization.formats (NoTypeHints)

    val voiceFeatures = try {
      read[VoiceFeatures](textLine)
    } catch {
      case e: ParseException => return ""
    }

    println(s"vf: ${voiceFeatures.modifyVoiceFeaturesInstruction.orderId}")

    write(voiceFeatures)

//    s"""
//              |{"orderId":"${voiceFeatures.modifyVoiceFeaturesInstruction.orderId}"}
//    """.stripMargin
  }
}
