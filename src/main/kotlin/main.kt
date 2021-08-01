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
    val htmlText = communicator.getCache() ?: return
//    val htmlText = "<html><head><title>sample web page</title>" +
//        "</head><body>hello, world!</body></html>"
    val parser = Parser()
    parser.initTree()
    parser.findTag(htmlText, 0)
    parser.showTree(parser.dom)
    println("The title text is...: ${parser.solveNode(parser.dom, Node.Tag.TITLE)}")
    val window = Window()
    window.start()
    window.addTitle(parser.solveNode(parser.dom, Node.Tag.TITLE) ?: "unknown")
    window.addURL("localhost:8887")
    window.addBody(parser.solveNode(parser.dom, Node.Tag.BODY) ?: "unknown")
}

class Window: Thread() {
    lateinit var context: DirectContext
    private var windowHandle: Long? = null
    lateinit var canvas: Canvas
    val width = 1000
    val height = 600

    @Volatile
    var drawRectQueue = listOf<DrawRectData>()
    @Volatile
    var drawTextQueue = listOf<DrawTextData>()


    private fun init() {
        glfwInit()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
        windowHandle = glfwCreateWindow(width,height,"hys_neko_browser_kt", 0, 0)
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

    fun addTitle(text: String) {
        drawTextQueue = drawTextQueue.plus(DrawTextData(text, 10f, 20f))
    }
    fun addURL(text: String) {
        drawTextQueue = drawTextQueue.plus(DrawTextData(text, 100f, 48f))
    }
    fun addBody(text: String) {
        drawTextQueue = drawTextQueue.plus(DrawTextData(text, 10f, 75f))
    }

    override fun run() {
        super.run()
        println("thread start")
        init()

        drawRectQueue = listOf(
            DrawRectData(Rect.makeXYWH(0f,0f, 200f, 25f), 0xFFDDDDDD.toInt()),
            DrawRectData(Rect.makeXYWH(200f,0f, width.toFloat(), 25f), 0xFFAAAAAA.toInt()),
            DrawRectData(Rect.makeXYWH(0f,25f, width.toFloat(), 30f), 0xFFDDDDDD.toInt()),
            DrawRectData(Rect.makeXYWH(100f,30f, 700f, 20f), 0xFFFFFFFF.toInt()),
            DrawRectData(Rect.makeXYWH(0f,55f, width.toFloat(), height.toFloat()), 0xFFFFFFFF.toInt())
        )

        val textPaint = Paint().apply {
            color = 0xFF000000.toInt()
        }
        val typeFace = FontMgr.getDefault().matchFamilyStyle("Arial", FontStyle.NORMAL)
        val font = Font(typeFace, 16f)

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

            drawRectQueue.forEach {
                canvas.drawRect(it.rect, Paint().apply { color = it.color })
            }

//            val rectTitleTab = Rect.makeXYWH(0f,0f, 100f, 25f)
//            val paintTitleTab = Paint().apply { color = 0xFFDDDDDD.toInt() }
//            canvas.drawRect(rectTitleTab, paintTitleTab)
//            val rectEmptyTab = Rect.makeXYWH(100f,0f, width.toFloat(), 25f)
//            val paintEmptyTab = Paint().apply { color = 0xFFAAAAAA.toInt() }
//            canvas.drawRect(rectEmptyTab, paintEmptyTab)
//            val rectURLBar = Rect.makeXYWH(0f,25f, width.toFloat(), 30f)
//            val paintURLBar = Paint().apply { color = 0xFFDDDDDD.toInt() }
//            canvas.drawRect(rectURLBar, paintURLBar)
//            val rectURLBarBox = Rect.makeXYWH(100f,30f, 700f, 20f)
//            val paintURLBarBox = Paint().apply { color = 0xFFFFFFFF.toInt() }
//            canvas.drawRect(rectURLBarBox, paintURLBarBox)
//            val rectBackground = Rect.makeXYWH(0f,55f, width.toFloat(), height.toFloat())
//            val paintBackground = Paint().apply { color = 0xFFFFFFFF.toInt() }
//            canvas.drawRect(rectBackground, paintBackground)

            drawTextQueue.forEach {
                canvas.drawString(it.text, it.x, it.y, font, textPaint)
            }

//            canvas.drawString("Text", 10f, 20f, font, textPaint)
//
//            canvas.drawString("example.com", 100f, 48f, font, textPaint)
//
//            canvas.drawString("Hello, test GUI!", 10f, 75f, font, textPaint)

            context.flush()
            glfwSwapBuffers(windowHandle!!)
            glfwPollEvents()
        }
    }

    data class DrawRectData(
        val rect: Rect,
        val color: Int
    )

    data class DrawTextData(
        val text: String,
        val x: Float,
        val y: Float
    )
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

    fun getCache(): String? {
        val tmpPath = Paths.get("tmp.html")
        if(Files.exists(tmpPath)) {
            try {
                val reader = FileReader(tmpPath.toFile())
                return reader.readLines().joinToString("")
            }
            catch(e: Exception) {
                println("failed to read cache")
                return null
            }
        }
        println("cache file not found")
        return null
    }
}

class Parser {
    var dom: Node = Node(Node.Tag.ROOT)
    fun findTag(input: String, offset: Int): Int {
        var i = offset
        val buffer = mutableListOf<Char>()
        //var currentNode = dom
        while(i < input.length) {
            var c = input[i]
            if(c == '<') {
                if(buffer.isNotEmpty()) {
                    println("TEXT:${buffer.joinToString("")}")
                    dom = sortTag(buffer.joinToString(""), dom)
                    buffer.clear()
                }
                print("[find tag]")
                i = findTag(input, i+1)
            }
            else if(c == '>') {
                println(buffer.joinToString(""))
                dom = sortTag(buffer.joinToString(""), dom)
                buffer.clear()
                return i
            }
            else {
                buffer.add(c)
            }
            i++
        }
        return 0
    }

    fun sortTag(input: String, node: Node): Node {
        if(input.startsWith("html")) {
            return addNode(Node.Tag.HTML, null, node)
        }
        else if(input.startsWith("head")) {
            return addNode(Node.Tag.HEAD, null, node)
        }
        else if(input.startsWith("title")) {
            return addNode(Node.Tag.TITLE, null, node)
        }
        else if(input.startsWith("body")) {
            return addNode(Node.Tag.BODY, null, node)
        }
        else if(input.startsWith("/html")) {
            return node.parent!!
        }
        else if(input.startsWith("/head")) {
            return node.parent!!
        }
        else if(input.startsWith("/title")) {
            return node.parent!!
        }
        else if(input.startsWith("/body")) {
            return node.parent!!
        }
        else {
            return addNode(Node.Tag.TEXT, input, node).parent!!
        }
    }

    fun initTree() {
        dom = addNode(Node.Tag.ROOT, null, null)
    }

    fun addNode(tag: Node.Tag, content: String?, parent: Node?): Node {
        val node = Node(tag)
        node.content = content ?: ""
        node.parent = parent
        node.child = null
        node.next = null
        if(parent == null) return node

        if(parent.child == null) {
            parent.child = node
        }
        else {
            var last = parent.child!!
            while(last.next != null) {
                last = last.next!!
            }
            last.next = node
        }
        return node
    }

    fun showTree(node: Node?, d: Int = 0) {
        var depth = d
        if(node == null) return
        print("TAG: ${node.tag}")
        if(node.content != null) {
            print(" ${node.content}")
        }
        print("\n")

        if(node.child != null) {
            depth++
            for(i in 0 until depth) print("-")
            showTree(node.child, depth)
            depth--
        }

        if(node.next != null) {
            for(i in 0 until depth) print("-")
            showTree(node.next, depth)
        }
    }

    fun solveNode(node: Node?, tag: Node.Tag): String? {
        if(node == null) return null

        if(node.tag == tag) {
            return node.child!!.content
        }
        var ret: String? = null
        if(node.child != null) {
            ret = solveNode(node.child, tag)
        }

        if(ret == null) {
            ret = solveNode(node.next, tag)
        }
        return ret
    }
}

class Node(var tag: Tag) {
    var content: String = ""
    var parent: Node? = null
    var child: Node? = null
    var next: Node? = null
    enum class Tag {
        ROOT, HTML, HEAD, TITLE, BODY, TEXT
    }
}
