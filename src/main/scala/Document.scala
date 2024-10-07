package pdfview

import scala.collection.mutable

import org.apache.pdfbox.pdmodel.{PDDocument,PDPage,PDDocumentCatalog}
import org.apache.pdfbox.pdmodel.interactive.action.{PDAction, PDActionGoTo}
import org.apache.pdfbox.pdmodel.interactive.annotation.{PDAnnotation, PDAnnotationLink}
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.{PDPageDestination,PDNamedDestination}
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.pdmodel.PDDocument

import java.awt.image.BufferedImage

import java.io.File

class Document(file: File, dpi: Float):
  private val document = PDDocument.load(file)
  private val pdfRenderer = new PDFRenderer(document)
  private val images = mutable.Map[Int, BufferedImage]()
  private var currentPage: Int = 0
  render

  val numberOfPages = document.getNumberOfPages

  def pageIndex: Int = currentPage
  def setPageIndex(p: Int) =
    if 0 <= p && p < numberOfPages then
      currentPage = p

  def getNumberOfPages: Int = numberOfPages

  def render: BufferedImage =
    if !images.contains(currentPage) then {
      images(currentPage) = pdfRenderer.renderImageWithDPI(currentPage, dpi).asInstanceOf[BufferedImage]
    }
    images(currentPage)

  def getWidth: Int = images(currentPage).getWidth
  def getHeight: Int = images(currentPage).getHeight

  def close: Unit = document.close

  // change current page by following any relevant clicks to bookmarks
  def followClick(mouseX: Float, mouseY: Float): Unit = 
        // hyperlinks
    val scale = dpi / 72f // Scale factor 
    val page = document.getPage(currentPage)
    val clickX: Float = mouseX / scale
    val clickY: Float = page.getMediaBox.getHeight - (mouseY / scale)
    val annotations = page.getAnnotations
    annotations.forEach: annotation => 
        annotation match
          case link: PDAnnotationLink =>
            val rect = link.getRectangle
            if rect != null then
              if clickX >= rect.getLowerLeftX && clickX <= rect.getUpperRightX &&
                 clickY >= rect.getLowerLeftY && clickY <= rect.getUpperRightY then
                println("Clicked a link!!!")
                handleActionLink(link.getAction)                       
          case _ => // Ignore other annotations        
  end followClick

  private def handleActionLink(pda: PDAction): Unit = 
    pda match
      case action: PDActionGoTo =>
        val destination = action.getDestination
        println("PDFActionGoTo!")
        destination match 
          case pageDestination: PDPageDestination =>
            val targetPageIndex = pageDestination.retrievePageNumber
            setPageIndex(targetPageIndex)
            render
          case namedDestination: PDNamedDestination => 
            val destName = namedDestination.getNamedDestination
            goToNamedDestination(destName)
          case _  => ()
      case _ => ()

  def goToNamedDestination(destName: String): Unit =
    val catalog: PDDocumentCatalog = document.getDocumentCatalog
    val names = catalog.getNames
    if names != null then
      val dests = names.getDests
      if dests != null then
        val dest = dests.getValue(destName)
        if dest != null && dest.isInstanceOf[PDPageDestination] then
          val targetPageIndex = dest.asInstanceOf[PDPageDestination].retrievePageNumber
          setPageIndex(targetPageIndex)
          render                  
  end goToNamedDestination

end Document
