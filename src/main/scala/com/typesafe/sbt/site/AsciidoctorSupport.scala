package com.typesafe.sbt
package site

import sbt._
import Keys._
import org.asciidoctor.Asciidoctor.Factory
import org.asciidoctor.Asciidoctor
import scala.collection.JavaConversions._
import org.asciidoctor.AsciiDocDirectoryWalker
import org.asciidoctor.Options
import org.asciidoctor.SafeMode

object AsciidoctorSupport {
  val Asciidoctor = config("asciidoctor")

  val settings: Seq[Setting[_]] =
    Seq(
      sourceDirectory in Asciidoctor <<= sourceDirectory(_ / "asciidoctor"),
      target in Asciidoctor <<= target(_ / "asciidoctor"),
      includeFilter in Asciidoctor := AllPassFilter) ++ inConfig(Asciidoctor)(Seq(
        mappings <<= (sourceDirectory, target, includeFilter) map AsciidoctorRunner.run))
}

object AsciidoctorRunner {
  def run(input: File, output: File, includeFilter: FileFilter): Seq[(File, String)] = {
    // this is a workaround for an sbt/jruby classloader issue.  Without it, you'll get this error
    // if you do a require 'java' in jruby:
    //   org.jruby.exceptions.RaiseException: (LoadError) no such file to load -- jruby/java
    //
    // Warning:  this workaround is an ugly hack that probably has unknown side effects.
    val oldContextClassLoader = Thread.currentThread().getContextClassLoader
    Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader())
    val asciidoctor = Factory.create()
    if (!output.exists) output.mkdirs()
    val options = new Options
    options.setToDir(output.getAbsolutePath())
    options.setDestinationDir(output.getAbsolutePath())
    options.setSafe(SafeMode.UNSAFE)
  	asciidoctor.renderDirectory(new AsciiDocDirectoryWalker(input.getAbsolutePath()), options)
    val inputImages = input / "images"
    if (inputImages.exists()) {
      val outputImages = output / "images"
      IO.copyDirectory(inputImages, outputImages, true)
    }
    Thread.currentThread().setContextClassLoader(oldContextClassLoader)
    output ** includeFilter --- output x relativeTo(output)
  }
}