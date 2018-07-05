package com.faheem.mediaplayer

import com.google.android.youtube.player.YouTubeBaseActivity
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer

import android.content.Intent
import android.widget.Toast

abstract class YouTubeFailureRecoveryActivity : YouTubeBaseActivity(), YouTubePlayer.OnInitializedListener {

    companion object {
        const val RECOVERY_DIALOG_REQUEST = 1
    }

    override fun onInitializationFailure(
            provider: YouTubePlayer.Provider, errorReason: YouTubeInitializationResult) : Unit {

        if (errorReason.isUserRecoverableError()) {
            errorReason.getErrorDialog(this, RECOVERY_DIALOG_REQUEST).show()
        } else {
            val errorMessage = String.format("Error", errorReason.toString())
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RECOVERY_DIALOG_REQUEST) {
            getYouTubePlayerProvider().initialize(DeveloperKey.DEVELOPER_KEY, this)
        }

    }

    protected abstract fun getYouTubePlayerProvider() : YouTubePlayer.Provider

}