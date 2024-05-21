import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

fun ImageProxy.toBinaryBitmap(rotationDegrees: Int): BinaryBitmap {
    val yBuffer = planes[0].buffer // Extract the Y plane
    val ySize = yBuffer.remaining()
    val data = ByteArray(ySize)
    yBuffer.get(data, 0, ySize)
    val luminanceSource = PlanarYUVLuminanceSource(
        data,
        width, height,
        0, 0,
        width, height,
        rotationDegrees == 90 || rotationDegrees == 270
    )
    return BinaryBitmap(HybridBinarizer(luminanceSource))
}
