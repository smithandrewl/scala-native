package scala.scalanative
package nir

import scala.collection.mutable
import nir.Attr._

sealed abstract class Attr {
  final def show: String = nir.Show(this)
}

object Attr {
  sealed abstract class Inline   extends Attr
  final case object MayInline    extends Inline // no information
  final case object InlineHint   extends Inline // user hinted at inlining
  final case object NoInline     extends Inline // should never inline
  final case object AlwaysInline extends Inline // should always inline

  final case object Dyn  extends Attr
  final case object Stub extends Attr

  final case object Pure   extends Attr
  final case object Extern extends Attr

  final case class Align(value: Int) extends Attr

  // Linker attributes
  final case class Link(name: String) extends Attr
}

final case class Attrs(inline: Inline = MayInline,
                       isPure: Boolean = false,
                       isExtern: Boolean = false,
                       isDyn: Boolean = false,
                       isStub: Boolean = false,
                       links: Seq[Attr.Link] = Seq(),
                       align: Option[Int] = scala.None) {
  def toSeq: Seq[Attr] = {
    val out = mutable.UnrolledBuffer.empty[Attr]

    if (inline != MayInline) out += inline
    if (isPure) out += Pure
    if (isExtern) out += Extern
    if (isDyn) out += Dyn
    if (isStub) out += Stub
    out ++= links
    align.foreach { out += Align(_) }

    out
  }
}
object Attrs {
  val None = new Attrs()

  def fromSeq(attrs: Seq[Attr]) = {
    var inline    = None.inline
    var isPure    = false
    var isExtern  = false
    var isDyn     = false
    var isStub    = false
    var align     = Option.empty[Int]
    val overrides = mutable.UnrolledBuffer.empty[Global]
    val links     = mutable.UnrolledBuffer.empty[Attr.Link]

    attrs.foreach {
      case attr: Inline    => inline = attr
      case Pure            => isPure = true
      case Extern          => isExtern = true
      case Dyn             => isDyn = true
      case Stub            => isStub = true
      case Align(value)    => align = Some(value)
      case link: Attr.Link => links += link
    }

    new Attrs(inline, isPure, isExtern, isDyn, isStub, links, align)
  }
}
