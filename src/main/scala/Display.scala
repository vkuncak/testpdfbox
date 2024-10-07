import java.awt.event.{KeyAdapter, KeyEvent}
import java.awt.{BorderLayout, Dimension, Image}
import javax.swing.{ImageIcon, JFrame, JLabel, JToolBar}
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import java.io.File
import java.awt.image.BufferedImage
import scala.collection.mutable

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
    val dpi = 300
    val images = mutable.Map[Int, BufferedImage]()
    images(0) = pdfRenderer.renderImageWithDPI(0, dpi).asInstanceOf[BufferedImage]

    // Creating a JFrame (the main window of the application)
    val frame = new JFrame("PDF Viewer")
    frame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE)
    frame.setSize(new Dimension(images(0).getWidth, images(0).getHeight))
    frame.setResizable(false)
    frame.setLayout(new BorderLayout())

    // Create a JLabel to display the images
    val imageLabel = new JLabel(new ImageIcon(images(0)))
    imageLabel.setPreferredSize(new Dimension(images(0).getWidth, images(0).getHeight))
    frame.add(imageLabel, BorderLayout.CENTER)

    // Introduce a counter for the current page
    var currentPage = 0

    // Create a toolbar to display the current page
    val toolbar = new JToolBar()
    val pageLabel = new JLabel(s"Page: ${currentPage + 1}/${document.getNumberOfPages}")
    toolbar.add(pageLabel)
    frame.add(toolbar, BorderLayout.SOUTH)

    // Adding a key listener to handle arrow key inputs
    frame.addKeyListener(new KeyAdapter {
      override def keyPressed(e: KeyEvent): Unit = {
        e.getKeyCode match {
          case KeyEvent.VK_LEFT =>
            if (currentPage > 0) {
              currentPage -= 1
              if (!images.contains(currentPage)) {
                images(currentPage) = pdfRenderer.renderImageWithDPI(currentPage, dpi).asInstanceOf[BufferedImage]
              }
              imageLabel.setIcon(new ImageIcon(images(currentPage)))
              pageLabel.setText(s"Page: ${currentPage + 1}/${document.getNumberOfPages}")
            }
          case KeyEvent.VK_RIGHT =>
            if (currentPage < document.getNumberOfPages - 1) {
              currentPage += 1
              if (!images.contains(currentPage)) {
                images(currentPage) = pdfRenderer.renderImageWithDPI(currentPage, dpi).asInstanceOf[BufferedImage]
              }
              imageLabel.setIcon(new ImageIcon(images(currentPage)))
              pageLabel.setText(s"Page: ${currentPage + 1}/${document.getNumberOfPages}")
            }
          case _ =>
            // Do nothing for other keys
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
