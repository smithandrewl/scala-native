package scala.scalanative
package codegen

import scala.collection.mutable
import scalanative.nir._
import scalanative.sema._
import scalanative.linker.{Trait, Struct, Class}

class Metadata(val linked: linker.Result, proxies: Seq[Defn]) {
  val rtti   = mutable.Map.empty[linker.Info, RuntimeTypeInformation]
  val vtable = mutable.Map.empty[linker.Class, VirtualTable]
  val layout = mutable.Map.empty[linker.Class, FieldLayout]
  val dynmap = mutable.Map.empty[linker.Class, DynamicHashMap]
  val ids    = mutable.Map.empty[linker.ScopeInfo, Int]
  val ranges = mutable.Map.empty[linker.Class, Range]

  val classes        = initClassIdsAndRanges()
  val structs        = initStructIds()
  val traits         = initTraitIds()
  val moduleArray    = new ModuleArray(this)
  val dispatchTable  = new TraitDispatchTable(this)
  val hasTraitTables = new HasTraitTables(this)

  initClassMetadata()
  initTraitMetadata()
  initStructMetadata()

  def initTraitIds(): Seq[Trait] = {
    val traits =
      linked.infos.valuesIterator
        .collect { case info: Trait => info }
        .toArray
        .sortBy(_.name.show)
    traits.zipWithIndex.foreach {
      case (node, id) =>
        ids(node) = id
    }
    traits
  }

  def initStructIds(): Seq[Struct] = {
    val structs =
      linked.infos.valuesIterator
        .collect { case info: Struct => info }
        .toArray
        .sortBy(_.name.show)
    structs.zipWithIndex.foreach {
      case (node, id) =>
        ids(node) = id
    }
    structs
  }

  def initClassIdsAndRanges(): Seq[Class] = {
    val out = mutable.UnrolledBuffer.empty[Class]
    var id  = 0

    def loop(node: Class): Unit = {
      out += node
      val start = id
      id += 1
      val directSubclasses =
        node.subclasses.filter(_.parent == Some(node)).toArray
      directSubclasses.sortBy(_.name.show).foreach { subcls =>
        loop(subcls)
      }
      val end = id - 1
      ids(node) = start
      ranges(node) = start to end
    }

    loop(linked.infos(Rt.Object.name).asInstanceOf[Class])

    out
  }

  def initClassMetadata(): Unit = {
    classes.foreach { node =>
      vtable(node) = new VirtualTable(this, node)
      layout(node) = new FieldLayout(this, node)
      dynmap(node) = new DynamicHashMap(this, node, proxies)
      rtti(node) = new RuntimeTypeInformation(this, node)
    }
  }

  def initTraitMetadata(): Unit = {
    traits.foreach { node =>
      rtti(node) = new RuntimeTypeInformation(this, node)
    }
  }

  def initStructMetadata(): Unit = {
    structs.foreach { node =>
      rtti(node) = new RuntimeTypeInformation(this, node)
    }
  }
}
