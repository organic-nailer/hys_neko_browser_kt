import org.jetbrains.skija.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import java.io.*
import java.lang.Exception
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Paths

fun main() {
    val communicator = Communicator()
    communicator.getHttp("localhost", 8887)
//    val window = Window()
//    println("init")
//    window.start()
//    println("after thread started")
//    Thread.sleep(10000)
//    window.interrupt()
}

class Window: Thread() {
    lateinit var context: DirectContext
    private var windowHandle: Long? = null
    lateinit var canvas: Canvas
    val width = 1000
    val height = 600

    private fun init() {
        glfwInit()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
        windowHandle = glfwCreateWindow(width,height,"Skija Sample", 0, 0)
        glfwMakeContextCurrent(windowHandle!!)
        glfwSwapInterval(1)
        glfwShowWindow(windowHandle!!)

        GL.createCapabilities()

        context = DirectContext.makeGL()

        val fbId = GL11.glGetInteger(0x8CA6)
        val renderTarget = BackendRenderTarget.makeGL(
            width,height,0,8,fbId, FramebufferFormat.GR_GL_RGBA8
        )

        val surface = Surface.makeFromBackendRenderTarget(
            context, renderTarget,
            SurfaceOrigin.BOTTOM_LEFT,
            SurfaceColorFormat.RGBA_8888,
            ColorSpace.getSRGB()
        )

        canvas = surface.canvas
    }

    override fun run() {
        super.run()
        println("thread start")
        init()

        while(!glfwWindowShouldClose(windowHandle!!)) {

//            val paint = Paint().apply { color = 0xFFFF0000.toInt() }
//
//            canvas.drawCircle(100f,100f,40f, paint)
//
//            val paint2 = Paint().apply {
//                isAntiAlias = true
//                color = 0xFF00FF00.toInt()
//            }
//            val typeFace = FontMgr.getDefault().matchFamilyStyle("Arial", FontStyle.NORMAL)
//            val font = Font(typeFace, 13f)
//            canvas.drawString("Hello", 50f,50f, font, paint2)

            val rectTitleTab = Rect.makeXYWH(0f,0f, 100f, 25f)
            val paintTitleTab = Paint().apply { color = 0xFFDDDDDD.toInt() }
            canvas.drawRect(rectTitleTab, paintTitleTab)
            val rectEmptyTab = Rect.makeXYWH(100f,0f, width.toFloat(), 25f)
            val paintEmptyTab = Paint().apply { color = 0xFFAAAAAA.toInt() }
            canvas.drawRect(rectEmptyTab, paintEmptyTab)
            val rectURLBar = Rect.makeXYWH(0f,25f, width.toFloat(), 30f)
            val paintURLBar = Paint().apply { color = 0xFFDDDDDD.toInt() }
            canvas.drawRect(rectURLBar, paintURLBar)
            val rectURLBarBox = Rect.makeXYWH(100f,30f, 700f, 20f)
            val paintURLBarBox = Paint().apply { color = 0xFFFFFFFF.toInt() }
            canvas.drawRect(rectURLBarBox, paintURLBarBox)
            val rectBackground = Rect.makeXYWH(0f,55f, width.toFloat(), height.toFloat())
            val paintBackground = Paint().apply { color = 0xFFFFFFFF.toInt() }
            canvas.drawRect(rectBackground, paintBackground)

            val textPaint = Paint().apply {
                color = 0xFF000000.toInt()
            }
            val typeFace = FontMgr.getDefault().matchFamilyStyle("Arial", FontStyle.NORMAL)
            val font = Font(typeFace, 16f)
            canvas.drawString("Text", 10f, 20f, font, textPaint)

            canvas.drawString("example.com", 100f, 48f, font, textPaint)

            canvas.drawString("Hello, test GUI!", 10f, 75f, font, textPaint)

            context.flush()
            glfwSwapBuffers(windowHandle!!)
            glfwPollEvents()
        }
    }
}

class Communicator {
    var fileWriter: PrintWriter? = null

    fun getHttp(address: String, port: Int) {
        try {
            val socket = Socket(address, port)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

            writer.write("GET / HTTP/1.0\r\n\r\n")
            writer.flush()

            val tmpPath = Paths.get("tmp.html").toAbsolutePath()

            if(!Files.exists(tmpPath)) {
                Files.createFile(tmpPath) ?: throw IOException("ファイルが作成できませんでした")
            }

            val outputFile = tmpPath.toFile()
            fileWriter = PrintWriter(outputFile)

            while(true) {
                val line = reader.readLine() ?: break

                fileWriter?.println(line)
            }
            fileWriter?.flush()
        } catch(e: Exception) {
            println("get error: ${e.message}")
        } finally {
            fileWriter?.close()
        }
    }
}
