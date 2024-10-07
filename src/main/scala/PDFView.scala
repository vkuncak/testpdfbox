package epfl.ch.lara.pdfview

import scala.collection.mutable
import java.io.File

import java.awt.image.BufferedImage
import java.awt.event.{KeyAdapter, KeyEvent, MouseWheelEvent, MouseWheelListener, 
                       FocusEvent, FocusListener, MouseAdapter, MouseEvent}
import java.awt.{BorderLayout, Dimension, Image}
import javax.swing.{ImageIcon, JFrame, JLabel, JToolBar, JTextArea, JScrollPane, JButton, JPanel}
import javax.swing.text.{StyleConstants, StyleContext,StyledDocument}
import org.fife.ui.rsyntaxtextarea.{RSyntaxTextArea, SyntaxConstants}
import org.fife.ui.rtextarea.RTextScrollPane

class PDFView(fileName: String):
    val dpi = 200f
    val file = new File(fileName)
    val document = Document(file, dpi)

    var currentImageWidth = document.getWidth
    var currentImageHeight = document.getHeight

    // Extract only the first page initially
    val fontSize: Float = math.floor(dpi/8.0).toFloat

    // Creating a JFrame (the main window of the application)
    val frame = new JFrame("PDF Viewer")
    frame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE)
    frame.setSize(new Dimension(currentImageWidth + 300, currentImageHeight))
    frame.setResizable(true)
    frame.setLayout(new BorderLayout())

    // Create a JLabel to display the images
    val imageLabel = new JLabel(new ImageIcon(document.render.getScaledInstance(currentImageWidth, currentImageHeight, Image.SCALE_SMOOTH)))
    imageLabel.setPreferredSize(new Dimension(currentImageWidth, currentImageHeight))
    frame.add(imageLabel, BorderLayout.CENTER)
    imageLabel.setFocusable(true)

    // Create a toolbar to display the current page
    val toolbar = new JToolBar()
    val pageLabel = new JLabel(s"Page: ${document.pageIndex + 1}/${document.getNumberOfPages}")
    pageLabel.setFont(pageLabel.getFont.deriveFont(fontSize))
    toolbar.add(pageLabel)
    frame.add(toolbar, BorderLayout.SOUTH)

    // Create a syntax-highlighted text area to the right of the image
    val textArea = new RSyntaxTextArea(20, 60)
    textArea.setFont(textArea.getFont.deriveFont(fontSize))
    textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SCALA)
    textArea.setCodeFoldingEnabled(true)
    textArea.setText("val x = 42\nprintln(f\"Hello Scala, ${x + x}\")\n// press F5 to execute Scala")
    val textScrollPane = new RTextScrollPane(textArea)
    textScrollPane.setPreferredSize(new Dimension(currentImageWidth/2, currentImageHeight))
    frame.add(textScrollPane, BorderLayout.EAST)
    
    def renderPage = {
      val image = document.render
      imageLabel.setIcon(new ImageIcon(image.getScaledInstance(currentImageWidth, currentImageHeight, Image.SCALE_SMOOTH)))
      pageLabel.setText(s"Page: ${document.pageIndex + 1}/${document.getNumberOfPages}")
    }

    renderPage

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
        } else {
          e.getKeyCode match {
            case KeyEvent.VK_F5 => 
              def logger(line: String): Unit = 
                textArea.append("\n//" + line)
              val exitCode = Util.runScala(textArea.getText, logger)
              ()
            case _ => ()
          }
        }
      }
    })

    // Set initial focus on the PDF viewing area
    imageLabel.requestFocusInWindow

    // Add mouse listener to switch focus when clicking on the PDF area
    imageLabel.addMouseListener(new MouseAdapter {
      override def mouseClicked(e: MouseEvent): Unit = {
        imageLabel.requestFocusInWindow()
        document.followClick(e.getX, e.getY)
        renderPage
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
            document.setPageIndex(document.pageIndex - 1)
            renderPage
          case KeyEvent.VK_RIGHT =>
            document.setPageIndex(document.pageIndex + 1)
            renderPage
          case KeyEvent.VK_PLUS | KeyEvent.VK_EQUALS => // Handle '+' or '=' key for zoom in
            currentImageWidth = (currentImageWidth * 1.1).toInt
            currentImageHeight = (currentImageHeight * 1.1).toInt
            imageLabel.setIcon(new ImageIcon(document.render.getScaledInstance(currentImageWidth, currentImageHeight, Image.SCALE_SMOOTH)))
            imageLabel.setPreferredSize(new Dimension(currentImageWidth, currentImageHeight))
            frame.pack
          case KeyEvent.VK_MINUS => // Handle '-' key for zoom out
            currentImageWidth = (currentImageWidth * 0.9).toInt
            currentImageHeight = (currentImageHeight * 0.9).toInt
            imageLabel.setIcon(new ImageIcon(document.render.getScaledInstance(currentImageWidth, currentImageHeight, Image.SCALE_SMOOTH)))
            imageLabel.setPreferredSize(new Dimension(currentImageWidth, currentImageHeight))
            frame.pack
          case _ => ()
        }
      }
    })

    frame.addMouseWheelListener(new MouseWheelListener {
      override def mouseWheelMoved(e: MouseWheelEvent): Unit =
        if (!textArea.hasFocus) then
          // Adding a mouse wheel listener to handle scrolling through pages 
          // only when the focus is not on the text area
          if e.getWheelRotation < 0 then // Scroll up
            document.setPageIndex(document.pageIndex - 1)
            renderPage
          else if e.getWheelRotation > 0  then // Scroll down
            document.setPageIndex(document.pageIndex + 1)
            renderPage
    })

    frame.pack
    frame.setVisible(true)

    sys.addShutdownHook:
      document.close  

end PDFView
