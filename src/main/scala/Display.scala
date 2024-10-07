import java.awt.event.{KeyAdapter, KeyEvent, MouseWheelEvent, MouseWheelListener, FocusEvent, FocusListener, MouseAdapter, MouseEvent}
import java.awt.{BorderLayout, Dimension, Image}
import javax.swing.{ImageIcon, JFrame, JLabel, JToolBar, JTextArea, JScrollPane, JButton, JPanel}
import javax.swing.text.{StyleConstants, StyleContext}
import javax.swing.text.StyledDocument
import org.fife.ui.rsyntaxtextarea.{RSyntaxTextArea, SyntaxConstants}
import org.fife.ui.rtextarea.RTextScrollPane
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import java.io.File
import java.awt.image.BufferedImage
import scala.collection.mutable

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo
import org.apache.pdfbox.pdmodel.interactive.annotation.{PDAnnotation, PDAnnotationLink}
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.{PDPageDestination,PDNamedDestination}
import org.apache.pdfbox.pdmodel.PDDocumentCatalog
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.rendering.PDFRenderer


object PDFViewer {
  def main(args: Array[String]): Unit = {
    // Check if the file path is provided as a command line argument
    if (args.length < 1) {
      println("Usage: PDFViewer <path to PDF file>")
      sys.exit(1)
    }

    val filePath = args(0)

    // Load PDF document and create PDFRenderer
    val document = PDDocument.load(new File(filePath))
    val pdfRenderer = new PDFRenderer(document)

    // Extract only the first page initially
    val dpi = 200f
    val images = mutable.Map[Int, BufferedImage]()
    images(0) = pdfRenderer.renderImageWithDPI(0, dpi).asInstanceOf[BufferedImage]

    // Creating a JFrame (the main window of the application)
    val frame = new JFrame("PDF Viewer")
    frame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE)
    frame.setSize(new Dimension(images(0).getWidth + 300, images(0).getHeight))
    frame.setResizable(true)
    frame.setLayout(new BorderLayout())

    // Create a JLabel to display the images
    var currentImageWidth = images(0).getWidth
    var currentImageHeight = images(0).getHeight
    val imageLabel = new JLabel(new ImageIcon(images(0).getScaledInstance(currentImageWidth, currentImageHeight, Image.SCALE_SMOOTH)))
    imageLabel.setPreferredSize(new Dimension(currentImageWidth, currentImageHeight))
    frame.add(imageLabel, BorderLayout.CENTER)
    imageLabel.setFocusable(true) // Allow the image label to be focusable

    // Introduce a counter for the current page
    var currentPage = 0


    // Create a toolbar to display the current page
    val toolbar = new JToolBar()
    val pageLabel = new JLabel(s"Page: ${currentPage + 1}/${document.getNumberOfPages}")
    pageLabel.setFont(pageLabel.getFont.deriveFont(16f))
    toolbar.add(pageLabel)
    frame.add(toolbar, BorderLayout.SOUTH)

    // Create a syntax-highlighted text area to the right of the image
    val textArea = new RSyntaxTextArea(20, 60)
    textArea.setFont(textArea.getFont.deriveFont(16f))
    textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SCALA)
    textArea.setCodeFoldingEnabled(true)
    textArea.setText("object Test {  }")
    val textScrollPane = new RTextScrollPane(textArea)
    textScrollPane.setPreferredSize(new Dimension(300, images(0).getHeight))
    frame.add(textScrollPane, BorderLayout.EAST)

    def renderPage = {
      if (!images.contains(currentPage)) {
        images(currentPage) = pdfRenderer.renderImageWithDPI(currentPage, dpi).asInstanceOf[BufferedImage]
      }
      imageLabel.setIcon(new ImageIcon(images(currentPage).getScaledInstance(currentImageWidth, currentImageHeight, Image.SCALE_SMOOTH)))
      pageLabel.setText(s"Page: ${currentPage + 1}/${document.getNumberOfPages}")
    }

    textArea.addKeyListener(new KeyAdapter {
      override def keyPressed(e: KeyEvent): Unit = {
        if (e.isControlDown) {
          e.getKeyCode match {
            case KeyEvent.VK_PLUS | KeyEvent.VK_EQUALS => // Handle Ctrl-'+' or Ctrl-'=' for increasing font size
              val newFontSize = textArea.getFont.getSize + 2
              textArea.setFont(textArea.getFont.deriveFont(newFontSize.toFloat))
            case KeyEvent.VK_MINUS => // Handle Ctrl-'-' for decreasing font size
              val newFontSize = Math.max(8, textArea.getFont.getSize - 2) // Ensure font size does not go below 8
              textArea.setFont(textArea.getFont.deriveFont(newFontSize.toFloat))
            case _ =>
              // Do nothing for other keys
          }
        }
      }
    })


    // Set initial focus on the PDF viewing area
    imageLabel.requestFocusInWindow()

    // Add mouse listener to switch focus when clicking on the PDF area
    imageLabel.addMouseListener(new MouseAdapter {
      override def mouseClicked(e: MouseEvent): Unit = {
        imageLabel.requestFocusInWindow()
        // hyperlinks
        val page = document.getPage(currentPage)
        val annotations = page.getAnnotations
        val scale = dpi / 72f // Scale factor used to render image at 150 DPI
        val clickX = e.getX / scale
        val clickY = page.getMediaBox.getHeight - (e.getY / scale)
        annotations.forEach {          
          case link: PDAnnotationLink =>
            val rect = link.getRectangle
            if (rect != null) {
              //println(f"Rectangle:           (${rect.getUpperRightX}, ${rect.getUpperRightY})")
              //println(f"Rectangle: (${rect.getLowerLeftX}, ${rect.getLowerLeftY})")
              //println(f"Mouse click:      (${clickX}, ${clickY}")
              if (clickX >= rect.getLowerLeftX && clickX <= rect.getUpperRightX &&
                clickY >= rect.getLowerLeftY && clickY <= rect.getUpperRightY) {                
                println("Clicked a link!!!")
                // Handle internal links (GoTo action)
                link.getAction match {
                  case action: PDActionGoTo =>
                    val destination = action.getDestination
                    println("PDFActionGoTo!")
                    destination match {
                      case pageDestination: PDPageDestination =>
                        val targetPageIndex = pageDestination.retrievePageNumber
                        println("page destination, targetPageIndex = $targetPageIndex")
                        // Navigate to the target page
                        if (targetPageIndex >= 0 && targetPageIndex < document.getNumberOfPages) {                          
                          currentPage = targetPageIndex
                          renderPage
                        }
                      case namedDestination: PDNamedDestination =>
                        val destName = namedDestination.getNamedDestination
                        val catalog: PDDocumentCatalog = document.getDocumentCatalog
                        val names = catalog.getNames
                        if (names != null) {
                          val dests = names.getDests
                          if (dests != null) {
                            val dest = dests.getValue(destName)
                            if (dest != null && dest.isInstanceOf[PDPageDestination]) {
                              val targetPageIndex = dest.asInstanceOf[PDPageDestination].retrievePageNumber
                              println(f"going to page number $targetPageIndex")
                              if (targetPageIndex >= 0 && targetPageIndex < document.getNumberOfPages) {
                                currentPage = targetPageIndex
                                renderPage
                              }
                            }
                          }
                        }

                      case _ => println("Uknown destination")
                    }
                  case _ => // Ignore other types of actions
                }
              }
            }
          case _ => // Ignore other annotations
        }
      }
    })

    // Add mouse listener to switch focus when clicking on the text area
    textArea.addMouseListener(new MouseAdapter {
      override def mouseClicked(e: MouseEvent): Unit = {
        textArea.requestFocusInWindow()
      }
    })

    // Adding a key listener to handle arrow key inputs directly to the imageLabel component
    imageLabel.addKeyListener(new KeyAdapter {
      override def keyPressed(e: KeyEvent): Unit = {
        e.getKeyCode match {
          case KeyEvent.VK_LEFT =>
            if (currentPage > 0) {
              currentPage -= 1
              renderPage
            }
          case KeyEvent.VK_RIGHT =>
            if (currentPage < document.getNumberOfPages - 1) {
              currentPage += 1
              renderPage
            }
          case KeyEvent.VK_PLUS | KeyEvent.VK_EQUALS => // Handle '+' or '=' key for zoom in
            currentImageWidth = (currentImageWidth * 1.1).toInt
            currentImageHeight = (currentImageHeight * 1.1).toInt
            imageLabel.setIcon(new ImageIcon(images(currentPage).getScaledInstance(currentImageWidth, currentImageHeight, Image.SCALE_SMOOTH)))
            imageLabel.setPreferredSize(new Dimension(currentImageWidth, currentImageHeight))
            frame.pack()
          case KeyEvent.VK_MINUS => // Handle '-' key for zoom out
            currentImageWidth = (currentImageWidth * 0.9).toInt
            currentImageHeight = (currentImageHeight * 0.9).toInt
            imageLabel.setIcon(new ImageIcon(images(currentPage).getScaledInstance(currentImageWidth, currentImageHeight, Image.SCALE_SMOOTH)))
            imageLabel.setPreferredSize(new Dimension(currentImageWidth, currentImageHeight))
            frame.pack()
          case _ =>
            // Do nothing for other keys
        }
      }
    })

    frame.addMouseWheelListener(new MouseWheelListener {
      override def mouseWheelMoved(e: MouseWheelEvent): Unit = {
        if (!textArea.hasFocus) {
          // Adding a mouse wheel listener to handle scrolling through pages only when the focus is not on the text area
          if (e.getWheelRotation < 0 && currentPage > 0) { // Scroll up
            currentPage -= 1
            if (!images.contains(currentPage)) {
              images(currentPage) = pdfRenderer.renderImageWithDPI(currentPage, dpi).asInstanceOf[BufferedImage]
            }
            imageLabel.setIcon(new ImageIcon(images(currentPage).getScaledInstance(currentImageWidth, currentImageHeight, Image.SCALE_SMOOTH)))
            pageLabel.setText(s"Page: ${currentPage + 1}/${document.getNumberOfPages}")
          } else if (e.getWheelRotation > 0 && currentPage < document.getNumberOfPages - 1) { // Scroll down
            currentPage += 1
            if (!images.contains(currentPage)) {
              images(currentPage) = pdfRenderer.renderImageWithDPI(currentPage, dpi).asInstanceOf[BufferedImage]
            }
            imageLabel.setIcon(new ImageIcon(images(currentPage).getScaledInstance(currentImageWidth, currentImageHeight, Image.SCALE_SMOOTH)))
            pageLabel.setText(s"Page: ${currentPage + 1}/${document.getNumberOfPages}")
          }
        }
      }    
    })

    frame.pack()
    frame.setVisible(true)

    // Close the document when done
    sys.addShutdownHook {
      document.close()
    }
  }
}

// Instructions:
// Run the program with a PDF file path as a command line argument.
// Example: scala PDFViewer /path/to/your/file.pdf
// This example extracts images from all pages of the PDF on demand.
