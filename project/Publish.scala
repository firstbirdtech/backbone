import sbt.Keys._
import sbt._
import sbt.plugins._

object NoPublish extends AutoPlugin {
  override def requires: Plugins = JvmPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    publishArtifact := false,
    publish := {},
    publishLocal := {}
  )
}

object Publish extends AutoPlugin {
  import bintray.BintrayPlugin
  import bintray.BintrayPlugin.autoImport._

  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins      = BintrayPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    bintrayOrganization := Some("firstbird"),
    bintrayPackage := "backbone"
  )
}
