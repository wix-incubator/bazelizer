package mvn

class Args(val cmd: Seq[String])

object Args {
  def apply(s: String*): Args = {
    new Args(s.toSeq)
  }
}
