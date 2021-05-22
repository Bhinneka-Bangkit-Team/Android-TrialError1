package com.ocha.prototype_final.CustomView.camera

import java.nio.ByteBuffer

interface ProcessorFrame {

    fun process(data:ByteBuffer,frameMetadata: FrameMetadata, graphicOverlay: GraphicOverlay)

    fun stop()
}

class FrameMetadata(val width: Int, val height: Int, val rotation: Int)