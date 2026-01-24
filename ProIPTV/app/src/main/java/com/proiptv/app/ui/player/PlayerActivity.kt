package com.proiptv.app.ui.player

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.proiptv.app.R
import com.proiptv.app.databinding.ActivityPlayerBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlayerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPlayerBinding
    private val viewModel: PlayerViewModel by viewModels()
    
    private var player: ExoPlayer? = null
    
    companion object {
        const val EXTRA_STREAM_ID = "stream_id"
        const val EXTRA_STREAM_NAME = "stream_name"
        const val EXTRA_STREAM_TYPE = "stream_type"
        const val EXTRA_CONTAINER_EXT = "container_ext"
        const val EXTRA_DIRECT_URL = "direct_url"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fullscreen and landscape
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val streamId = intent.getIntExtra(EXTRA_STREAM_ID, 0)
        val streamName = intent.getStringExtra(EXTRA_STREAM_NAME) ?: ""
        val streamType = intent.getStringExtra(EXTRA_STREAM_TYPE) ?: "live"
        val containerExt = intent.getStringExtra(EXTRA_CONTAINER_EXT)
        val directUrl = intent.getStringExtra(EXTRA_DIRECT_URL)
        val isLive = streamType == "live"
        
        binding.tvStreamName.text = streamName
        
        setupPlayer(isLive)
        setupControls()
        observeViewModel()
        
        // Use direct URL for M3U streams, otherwise use Xtream Codes
        if (!directUrl.isNullOrEmpty()) {
            viewModel.setDirectUrl(directUrl)
        } else {
            viewModel.getStreamUrl(streamId, streamType, containerExt)
        }
    }
    
    private fun setupPlayer(isLive: Boolean) {
        player = ExoPlayer.Builder(this).build().apply {
            binding.playerView.player = this
            
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        Player.STATE_READY -> {
                            binding.progressBar.visibility = View.GONE
                        }
                        Player.STATE_ENDED -> {
                            finish()
                        }
                        Player.STATE_IDLE -> {}
                    }
                }
                
                override fun onPlayerError(error: PlaybackException) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@PlayerActivity,
                        getString(R.string.player_error),
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }
        
        binding.playerView.apply {
            // Configure controls for live streams - hide ALL time-related controls
            if (isLive) {
                // Hide all seek controls
                setShowNextButton(false)
                setShowPreviousButton(false)
                setShowFastForwardButton(false)
                setShowRewindButton(false)
                
                // This is key: disable showing current position and duration
                findViewById<View?>(androidx.media3.ui.R.id.exo_position)?.visibility = View.GONE
                findViewById<View?>(androidx.media3.ui.R.id.exo_duration)?.visibility = View.GONE
                findViewById<View?>(androidx.media3.ui.R.id.exo_progress)?.visibility = View.GONE
                
                controllerShowTimeoutMs = 3000
                controllerHideOnTouch = true
                useController = true
            } else {
                useController = true
            }
            
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        }
        
        // Show/hide live indicator
        if (isLive) {
            binding.liveIndicator.visibility = View.VISIBLE
        } else {
            binding.liveIndicator.visibility = View.GONE
        }
    }
    
    private fun setupControls() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.btnRetry.setOnClickListener {
            player?.prepare()
            player?.play()
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.streamUrl.collectLatest { url ->
                url?.let {
                    playStream(it)
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.error.collectLatest { error ->
                error?.let {
                    Toast.makeText(this@PlayerActivity, it, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun playStream(url: String) {
        val mediaItem = MediaItem.fromUri(url)
        player?.apply {
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }
    
    override fun onResume() {
        super.onResume()
        player?.playWhenReady = true
    }
    
    override fun onPause() {
        super.onPause()
        player?.playWhenReady = false
    }
    
    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
