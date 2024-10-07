import java.awt.event.{KeyAdapter, KeyEvent}
import java.awt.{Dimension, Image}
import javax.swing.{ImageIcon, JFrame, JLabel}
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import java.io.File
import java.awt.image.BufferedImage
import scala.collection.mutable

object PictureSwitcherApp extends App {
  // Load PDF document and create PDFRenderer
  val document = PDDocument.load(new File("swc4.pdf"))
  val pdfRenderer = new PDFRenderer(document)

  // Extract only the first page initially
  val dpi = 300
  val images = mutable.Map[Int, BufferedImage]()
  images(0) = pdfRenderer.renderImageWithDPI(0, dpi).asInstanceOf[BufferedImage]

  // Creating a JFrame (the main window of the application)
  val frame = new JFrame("Picture Switcher")
  frame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE)
  frame.setSize(new Dimension(images(0).getWidth, images(0).getHeight))
  frame.setResizable(false)

  // Create a JLabel to display the images
  val imageLabel = new JLabel(new ImageIcon(images(0)))
  imageLabel.setPreferredSize(new Dimension(images(0).getWidth, images(0).getHeight))
  frame.add(imageLabel)

  // Introduce a counter for the current page
  var currentPage = 0

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
          }
        case KeyEvent.VK_RIGHT =>
          if (currentPage < document.getNumberOfPages - 1) {
            currentPage += 1
            if (!images.contains(currentPage)) {
              images(currentPage) = pdfRenderer.renderImageWithDPI(currentPage, dpi).asInstanceOf[BufferedImage]
            }
            imageLabel.setIcon(new ImageIcon(images(currentPage)))
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

// Instructions:
// Replace "images.pdf" with the path to your actual PDF file.
// Ensure that the PDF is available in the correct path, or adjust the path accordingly.
// This example extracts images from all pages of the PDF on demand.