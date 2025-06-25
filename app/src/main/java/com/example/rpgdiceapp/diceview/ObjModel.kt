package com.example.rpgdiceapp.diceview

import android.content.Context
import android.opengl.GLES20
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class ObjModel(
    context: Context,
    objFile: String,
    baseTextureFile: String?,
    normalMapFile: String?,
    metalnessMapFile: String?
) {

    private val vertexBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer
    private val program: Int
    private var baseTextureId: Int = 0
    private var normalTextureId: Int = 0
    private var metalnessTextureId: Int = 0
    private var indexCount = 0

    init {
        val tempVerts = mutableListOf<FloatArray>()
        val tempTex = mutableListOf<FloatArray>()
        val vertices = mutableListOf<Float>()
        val texCoords = mutableListOf<Float>()
        val indices = mutableListOf<Short>()

        val reader = BufferedReader(InputStreamReader(context.assets.open("models/$objFile")))
        reader.forEachLine { line ->
            val parts = line.trim().split("\\s+".toRegex())
            when (parts[0]) {
                "v" -> tempVerts.add(floatArrayOf(parts[1].toFloat(), parts[2].toFloat(), parts[3].toFloat()))
                "vt" -> tempTex.add(floatArrayOf(parts[1].toFloat(), 1f - parts[2].toFloat()))
                "f" -> {
                    for (i in 1..3) {
                        val indicesParts = parts[i].split("/")

                        if (indicesParts.size < 2 || indicesParts[0].isEmpty()) {
                            continue
                        }

                        if (indicesParts.size == 3) {
                            val vi = indicesParts[0].toIntOrNull() ?: continue
                            val ti = indicesParts[1].toIntOrNull() ?: continue
                            val ni = indicesParts[2].toIntOrNull() ?: continue
                            val v = tempVerts[vi - 1]
                            val t = tempTex[ti - 1]
                            vertices.addAll(v.toList())
                            texCoords.addAll(t.toList())
                            indices.add((vertices.size / 3 - 1).toShort())
                        }
                        else if (indicesParts.size == 2) {
                            val vi = indicesParts[0].toIntOrNull() ?: continue
                            val ni = indicesParts[1].toIntOrNull() ?: continue
                            val v = tempVerts[vi - 1]
                            texCoords.addAll(listOf(0f, 0f))
                            indices.add((vertices.size / 3 - 1).toShort())
                        }
                    }
                }


            }
        }

        indexCount = indices.size

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(vertices.toFloatArray())
                position(0)
            }
        }

        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(texCoords.toFloatArray())
                position(0)
            }
        }

        indexBuffer = ByteBuffer.allocateDirect(indices.size * 2).run {
            order(ByteOrder.nativeOrder())
            asShortBuffer().apply {
                put(indices.toShortArray())
                position(0)
            }
        }

        val vertexShaderCode = """
            attribute vec4 vPosition;
            attribute vec2 aTexCoord;
            uniform mat4 uMVPMatrix;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = uMVPMatrix * vPosition;
                vTexCoord = aTexCoord;
            }
        """.trimIndent()

        val fragmentShaderCode = """
            precision mediump float;
            uniform sampler2D uTexture;
            uniform sampler2D uNormalMap;
            uniform sampler2D uMetalnessMap;
            varying vec2 vTexCoord;
            void main() {
                vec4 baseColor = texture2D(uTexture, vTexCoord);
                vec4 metalColor = texture2D(uMetalnessMap, vTexCoord);
                float metal = metalColor.r;
                gl_FragColor = mix(baseColor, vec4(1.0), metal * 0.5);
            }
        """.trimIndent()

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        baseTextureFile?.let {
            baseTextureId = TextureLoader.loadTexture(context, it)
        } ?: run {
            baseTextureId = 0
        }

        normalMapFile?.let {
            normalTextureId = TextureLoader.loadTexture(context, it)
        } ?: run {
            normalTextureId = 0
        }

        metalnessMapFile?.let {
            metalnessTextureId = TextureLoader.loadTexture(context, it)
        } ?: run {
            metalnessTextureId = 0
        }

    }

    fun draw(mvpMatrix: FloatArray) {
        GLES20.glUseProgram(program)

        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        val texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        val textureHandle = GLES20.glGetUniformLocation(program, "uTexture")
        val normalHandle = GLES20.glGetUniformLocation(program, "uNormalMap")
        val metalHandle = GLES20.glGetUniformLocation(program, "uMetalnessMap")

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, baseTextureId)
        GLES20.glUniform1i(textureHandle, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, normalTextureId)
        GLES20.glUniform1i(normalHandle, 1)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE2)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, metalnessTextureId)
        GLES20.glUniform1i(metalHandle, 2)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    private fun loadShader(type: Int, code: String): Int {
        return GLES20.glCreateShader(type).also {
            GLES20.glShaderSource(it, code)
            GLES20.glCompileShader(it)
        }
    }
}