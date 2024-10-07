package epfl.ch.lara.pdfview

object PDFViewer:
  def main(args: Array[String]): Unit =
    if args == null || args.length < 1 then
      println("Usage: pdfview <path to PDF file>")
    else
      args.map(PDFView(_))

