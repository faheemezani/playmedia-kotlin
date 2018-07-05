package com.faheem.mediaplayer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayerView
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.upstream.BandwidthMeter
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

class SecondScreenActivity : YouTubeFailureRecoveryActivity() {

    lateinit var playerView: YouTubePlayerView
    lateinit var player: YouTubePlayer
    lateinit var exoplayer: SimpleExoPlayer
    lateinit var client: OkHttpClient

    enum class TimerState{
        Stopped, Paused, Running
    }
    var timerState = TimerState.Stopped
    lateinit var timer: CountDownTimer
    var timerLengthSeconds: Long = 0
    var secondsRemaining: Long = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second_screen)

        val decorView = window.decorView
        // Hide the status bar.
        val uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
        decorView.systemUiVisibility = uiOptions

        playerView = findViewById(R.id.player) as YouTubePlayerView
        playerView.initialize(DeveloperKey.DEVELOPER_KEY, this)

        playAudio()

        // 'Pop' simulation: send a pop after 5 seconds and listen for a pop
        timer = object : CountDownTimer(secondsRemaining * 1000, 1000) {
            override fun onFinish() {
                startWebSocket()
            }
            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = millisUntilFinished / 1000
            }
        }.start()

    }

    public override fun onBackPressed() {
        super.onBackPressed()
        exoplayer.stop()
        finish()
    }

    fun startTimer() {

        timerState = TimerState.Running

        timer = object : CountDownTimer(secondsRemaining * 1000, 1000) {
            override fun onFinish() = onTimerFinished()
            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = millisUntilFinished / 1000
            }
        }.start()

    }

    public override fun onInitializationSuccess(
            provider: YouTubePlayer.Provider, player: YouTubePlayer, wasRestored: Boolean) : Unit {

        this.player = player
        player.addFullscreenControlFlag(YouTubePlayer.FULLSCREEN_FLAG_CUSTOM_LAYOUT)
        player.setPlayerStyle(YouTubePlayer.PlayerStyle.CHROMELESS)
        player.setPlayerStateChangeListener(object : YouTubePlayer.PlayerStateChangeListener {
            override fun onLoading() {

            }

            override fun onLoaded(s: String) {

            }

            override fun onAdStarted() {

            }

            override fun onVideoStarted() {

            }

            override fun onVideoEnded() {
                player.play()
            }

            override fun onError(errorReason: YouTubePlayer.ErrorReason) {

            }
        })

        if (!wasRestored) {
            player.loadVideo("QPDX91iJ_RA", 0)
        }

    }

    protected override fun getYouTubePlayerProvider() : YouTubePlayer.Provider {
        return playerView
    }

    private fun playAudio() : Unit {

        lateinit var bandwidthMeter: BandwidthMeter
        lateinit var extractorsFactory: ExtractorsFactory
        lateinit var trackSelectionFactory: TrackSelection.Factory
        lateinit var trackSelector: TrackSelector
        lateinit var defaultBandwidthMeter: DefaultBandwidthMeter
        lateinit var dataSourceFactory: DataSource.Factory
        lateinit var mediaSource: MediaSource

        bandwidthMeter = DefaultBandwidthMeter()
        extractorsFactory = DefaultExtractorsFactory()
        trackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        trackSelector = DefaultTrackSelector(trackSelectionFactory)

        defaultBandwidthMeter = DefaultBandwidthMeter()

        dataSourceFactory = DefaultDataSourceFactory(
                this, Util.getUserAgent(
                this, "mediaPlayerSample"), defaultBandwidthMeter)

        mediaSource = ExtractorMediaSource(
                Uri.parse("http://stream.radioreklama.bg:80/radio1.opus"),
                dataSourceFactory,
                extractorsFactory,
                null,
                null)

        exoplayer = ExoPlayerFactory.newSimpleInstance(this, trackSelector)
        exoplayer.prepare(mediaSource)
        exoplayer.setPlayWhenReady(true)

    }

    private fun startWebSocket(): Unit {

        client = OkHttpClient().newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS).build()
        val request = Request.Builder().url("ws://demos.kaazing.com/echo").build()
        val listener = EchoWebSocketListener()
        val ws = client.newWebSocket(request, listener)

        client.dispatcher().executorService().shutdown()

    }

    private fun popUpRedButton() {
        val redButton = findViewById(R.id.redSquareButton) as Button
        redButton.setOnClickListener{
            exoplayer.stop()
            val intent = Intent(this.applicationContext, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        redButton.visibility = View.VISIBLE
    }

    private fun onTimerFinished(){
        timerState = TimerState.Stopped
        popUpRedButton()
    }

    class EchoWebSocketListener() : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            webSocket.send("pop")
            Log.i("Pop", "Sent a 'pop'")
        }

        override fun onMessage(webSocket: WebSocket?, text: String?): Unit {
            super.onMessage(webSocket, text)
            //if (text.equals("pop")) this@SecondScreenActivity.startTimer()
            output("Received a " + text!!)
        }

        override fun onMessage(webSocket: WebSocket?, bytes: ByteString): Unit {
            super.onMessage(webSocket, bytes)
            output("Receiving bytes : " + bytes!!.hex())
        }

        override fun onClosing(webSocket: WebSocket?, code: Int, reason: String): Unit {
            super.onClosing(webSocket, code, reason)
            if (webSocket != null) {
                webSocket.close(20, null)
            }
            output("Closing : " + code + " / " + reason)
        }

        private fun output(txt: String) {
            Log.v("WebSocket", txt)
        }

    }

}
