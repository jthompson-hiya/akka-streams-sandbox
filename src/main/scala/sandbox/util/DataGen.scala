package sandbox.util

import org.scalacheck.Gen

object DataGen {
  val strings = collection.mutable.HashMap[String, Seq[String]]()

  def stringsFor(resource: String, countryCode: String): Seq[String] = {
    val key = s"/${resource}_${countryCode}.txt"
    val fallbackKey = s"/${resource}_US.txt"
    val is = strings.get(key).orElse(strings.get(fallbackKey))

    val res = is.getOrElse(
      scala.io.Source.fromInputStream(
        Option(getClass.getResourceAsStream(key))
          .getOrElse(getClass.getResourceAsStream(fallbackKey))
      ).getLines().toSeq
    )

    if(is.isEmpty) {
      strings.put(key, res)
    }

    res
  }

  def givenName(cc: String):Gen[String] = Gen.oneOf(stringsFor("givenNames", cc))

  def familyName(cc: String): Gen[String] = Gen.oneOf(stringsFor("familyNames", cc))

  def name(cc: String): Gen[String] = for {
    first <- givenName(cc)
    last <- familyName(cc)
    middle <- Gen.alphaUpperChar
  } yield s"$first $middle. $last"

}
