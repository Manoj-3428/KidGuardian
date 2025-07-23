package com.example.phonelock

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.Properties
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
class ScreenshotService : Service() {

    companion object {
        var mediaProjection: MediaProjection? = null
        var resultCode: Int = 0
        var dataIntent: Intent? = null
    }

    private var virtualDisplay: VirtualDisplay? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (resultCode == Activity.RESULT_OK && dataIntent != null) {
            val channelId = "screenshot_channel"
            val notificationId = 1

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Screenshot Service",
                    NotificationManager.IMPORTANCE_LOW
                )
                val manager = getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("Capturing Screen")
                .setContentText("Screenshot in progress...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()

            startForeground(notificationId, notification)

            takeScreenshot(resultCode, dataIntent!!)
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun takeScreenshot(resultCode: Int, data: Intent) {
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = projectionManager.getMediaProjection(resultCode, data)

        if (projection == null) {
            Log.e("SCREENSHOT", "Projection is null")
            stopSelf()
            return
        }

        // ✅ Register callback (required to avoid crash)
        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Log.d("ScreenshotService", "MediaProjection stopped")
                virtualDisplay?.release()
                stopSelf()
            }
        }, null)

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(metrics)

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        virtualDisplay = projection.createVirtualDisplay(
            "ScreenCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            null
        )

        Thread {
            try {
                Thread.sleep(1000)

                val image = imageReader.acquireLatestImage() ?: return@Thread
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width

                val bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()

                val file = saveBitmap(bitmap)
                sendToEmail(file)

                projection.stop()
                virtualDisplay?.release()
            } catch (e: Exception) {
                Log.e("SCREENSHOT", "Failed: ${e.message}")
            }
        }.start()
    }

    private fun saveBitmap(bitmap: Bitmap): File {
        val file = File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "screenshot_${System.currentTimeMillis()}.png"
        )
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return file
    }

    private fun sendToEmail(screenshotFile: File) {
        val fromEmail = "kommanamanojkumar300@gmail.com"
        val appPassword = "wmjiopmujzzllpnw" // Gmail App Password
        val toEmail = "kommanamanojkumar830@gmail.com"
        val subject = "Abusive content detected - Screenshot"
        val body = "Abusive content was detected. See attached screenshot."

        Thread {
            try {
                val props = Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                }
                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(fromEmail, appPassword)
                    }
                })
                val message = MimeMessage(session)
                message.setFrom(InternetAddress(fromEmail))
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                message.subject = subject

                val mimeBodyPart = MimeBodyPart()
                mimeBodyPart.setText(body)

                val attachmentBodyPart = MimeBodyPart()
                attachmentBodyPart.dataHandler = DataHandler(FileDataSource(screenshotFile))
                attachmentBodyPart.fileName = screenshotFile.name

                val multipart: Multipart = MimeMultipart().apply {
                    addBodyPart(mimeBodyPart)
                    addBodyPart(attachmentBodyPart)
                }
                message.setContent(multipart)

                Transport.send(message)
                Log.d("SCREENSHOT", "Email sent successfully")
            } catch (e: Exception) {
                Log.e("SCREENSHOT", "Failed to send email: ${e.message}")
            } finally {
                // After sending email, block the screen
                val lockIntent = Intent(this, LockScreenActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(lockIntent)
                stopSelf()
            }
        }.start()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
