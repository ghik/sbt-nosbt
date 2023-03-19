package com.github.ghik.sbt.nosbt
package util

final case class EnclosingValName(name: String)
object EnclosingValName {
  implicit def materialize: EnclosingValName = macro Macros.enclosingValNameImpl
}
