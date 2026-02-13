package org.example.ui.effects

import coil3.Bitmap
import coil3.size.Size
import coil3.transform.Transformation
import org.jetbrains.skia.*

fun Bitmap.asSkiaImage(): Image {
    return Image.makeFromBitmap(this)
}

fun Image.toCoilBitmap(): Bitmap {
    return Bitmap.makeFromImage(this)
}

class PreRenderBlurTransformation(
    private val radius: Float
) : Transformation() {

    override val cacheKey: String =
        "PreRenderBlurTransformation(radius=$radius)"

    override suspend fun transform(
        input: Bitmap,
        size: Size
    ): Bitmap {

        if (radius <= 0f) return input

        // convert Coil Bitmap → Skia Image
        val skiaImage = input.asSkiaImage()

        val surface = Surface.makeRasterN32Premul(
            skiaImage.width,
            skiaImage.height
        )

        val paint = Paint().apply {
            imageFilter = ImageFilter.makeBlur(
                radius,
                radius,
                FilterTileMode.CLAMP
            )
        }

        surface.canvas.drawImage(
            skiaImage,
            0f,
            0f,
            paint
        )

        val blurredSkiaImage = surface.makeImageSnapshot()

        // convert Skia Image → Coil Bitmap
        return blurredSkiaImage.toCoilBitmap()
    }
}
