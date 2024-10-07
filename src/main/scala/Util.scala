package epfl.ch.lara.pdfview
import java.io.File
import scala.sys.process.*

object Util:

  def runScala(programText: String, logger: String => Unit): Int =
    val commandName = "scala-cli shebang --quiet"
    val saveFileName = "test.txt"
    val writer = new java.io.BufferedWriter(new java.io.FileWriter(saveFileName))
    writer.write("#!/usr/bin/env -S scala-cli shebang\n")
    writer.write(programText)
    writer.close

    val toRun = commandName + " " + saveFileName
    println(f"running:\n${toRun}")
    val exitCode = toRun ! ProcessLogger(logger)
    exitCode

end Util
