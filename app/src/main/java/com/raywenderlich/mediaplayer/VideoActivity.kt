package com.raywenderlich.mediaplayer

import android.content.Intent
import android.media.MediaDrm
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.raywenderlich.mediaplayer.databinding.ActivityVideoBinding

@RequiresApi(Build.VERSION_CODES.O)
class VideoActivity : AppCompatActivity(), SurfaceHolder.Callback, SeekBar.OnSeekBarChangeListener,
    MediaPlayer.OnPreparedListener, MediaPlayer.OnDrmInfoListener {
    private lateinit var binding: ActivityVideoBinding

    private val mediaPlayer = MediaPlayer()
    private lateinit var runnable: Runnable
    private var handler = Handler(Looper.getMainLooper())
    private lateinit var selectedVideoUri: Uri

    companion object {
        const val GET_VIDEO = 123
        const val SECOND = 1000
        const val URL =
            "https://res.cloudinary.com/dit0lwal4/video/upload/v1597756157/samples/elephants.mp4"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Switch to AppTheme for displaying the activity
        setTheme(R.style.Theme_Final)
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        mediaPlayer.setOnPreparedListener(this)
        mediaPlayer.setOnDrmInfoListener(this)
        binding.videoView.holder.addCallback(this)
        binding.seekBar.setOnSeekBarChangeListener(this)
        binding.playButton.isEnabled = false

        binding.playButton.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                binding.playButton.setImageResource(android.R.drawable.ic_media_play)
            } else {
                mediaPlayer.start()
                binding.playButton.setImageResource(android.R.drawable.ic_media_pause)
            }
        }
    }

    // Converting seconds to mm:ss format to display on screen
    private fun timeInString(seconds: Int): String {
        return String.format(
            "%02d:%02d",
            (seconds / 3600 * 60 + ((seconds % 3600) / 60)),
            (seconds % 60)
        )
    }

    // Initialize seekBar
    private fun initializeSeekBar() {
        binding.seekBar.max = mediaPlayer.seconds
        binding.textProgress.text = getString(R.string.default_value)
        binding.textTotalTime.text = timeInString(mediaPlayer.seconds)
        binding.progressBar.visibility = View.GONE
        binding.playButton.isEnabled = true
    }

    // Update seek bar after every 1 second
    private fun updateSeekBar() {
        runnable = Runnable {
            binding.textProgress.text = timeInString(mediaPlayer.currentSeconds)
            binding.seekBar.progress = mediaPlayer.currentSeconds
            handler.postDelayed(runnable, SECOND.toLong())
        }
        handler.postDelayed(runnable, SECOND.toLong())
    }

    // SurfaceHolder is ready
    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        mediaPlayer.apply {
            //setDataSource(applicationContext, Uri.parse("android.resource://$packageName/raw/test_video"))
            //setDataSource(applicationContext, selectedVideoUri)

            //For DRM protected video
            setOnDrmInfoListener(this@VideoActivity) //This method will invoke onDrmInfo() function
            //
            setDataSource(URL)
            setDisplay(surfaceHolder)
            prepareAsync()
        }
    }

    // Ignore
    override fun surfaceChanged(surfaceHolder: SurfaceHolder, p1: Int, p2: Int, p3: Int) {

    }

    // Ignore
    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {

    }

    // This function gets called if the file is DRM protected
    override fun onDrmInfo(mediaPlayer: MediaPlayer?, drmInfo: MediaPlayer.DrmInfo?) {
        mediaPlayer?.apply {
            val key = drmInfo?.supportedSchemes?.get(0)
            key?.let {
                prepareDrm(key)
                val keyRequest = getKeyRequest(
                    null, null, null,
                    MediaDrm.KEY_TYPE_STREAMING, null
                )
                provideKeyResponse(null, keyRequest.data)
            }
        }
    }

    // This function gets called when the media player gets ready
    override fun onPrepared(mediaPlayer: MediaPlayer?) {
        initializeSeekBar()
        updateSeekBar()
    }

    // Update media player when user changes seekBar
    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (fromUser)
            mediaPlayer.seekTo(progress * SECOND)
    }

    // Ignore
    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    // Ignore
    override fun onStopTrackingTouch(seekBar: SeekBar?) {
    }

    // Create option menu in toolbar
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.app_menu, menu)
        return true
    }

    // Invoked when an option is selected in menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.select_file -> {
                val intent = Intent()
                intent.type = "video/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(
                    Intent.createChooser(intent, getString(R.string.select_file)), GET_VIDEO
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Invoked when a video is selected from the gallery
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == GET_VIDEO) {
                selectedVideoUri = data?.data!!
                binding.videoView.holder.addCallback(this)
            }
        }
    }

    // Release the media player resources when activity gets destroyed
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
        mediaPlayer.release()
        mediaPlayer.releaseDrm()
    }


    // Creating an extension properties to get the media player total time and current duration in seconds
    private val MediaPlayer.seconds: Int
        get() {
            return this.duration / SECOND
        }

    private val MediaPlayer.currentSeconds: Int
        get() {
            return this.currentPosition / SECOND
        }
}