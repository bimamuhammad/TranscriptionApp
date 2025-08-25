package com.example.android.transcriptionapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechRecognitionAlternative
import com.google.cloud.speech.v1.SpeechRecognitionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class SpeechToText(private val context: Context,
                   private val speechClient: SpeechClient, private val onResult: (String) -> Unit) {

    private var mediaRecorder: MediaRecorder ?= null
    private var audioFile: File?= null
    private var isRecording = false
    private val TAG = "SpeechToText"

    //  Handler to post results back to the main UI thread
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun hasPermission(): Boolean {
        // Check the RECORD_AUDIO permission
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    fun startRecording(){
        if(!hasPermission()){
            Log.e(TAG, context.getString(R.string.permissionDenied))
            onResult(context.getString(R.string.errorPermissionNotGranted))
            return
        }

        if(isRecording){
            Log.d(TAG, context.getString(R.string.alreadyRecording))
            return
        }

        audioFile = File(context.cacheDir, "temp_audio.3gp")
        mediaRecorder = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            MediaRecorder(context)
        } else{
            MediaRecorder()
        }

        try {
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFile?.absolutePath)
                // Add listeners here!
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaRecorder Error: what=$what, extra=$extra")
                    // Handle cleanup
                }

                setOnInfoListener { _, what, extra ->
                    Log.d(TAG, "MediaRecorder Info: what=$what, extra=$extra")
                    // Handle info events
                }
                prepare()
                start()
                isRecording = true
                Log.d(TAG, context.getString(R.string.recordingStarted))
                onResult(context.getString(R.string.recordingStarted))
            }
        } catch( e: IOException){
            Log.e(TAG, context.getString(R.string.prepareFailure))
            isRecording = false
        }
    }

    suspend fun stopRecording(){
        if(!isRecording){
            Log.d(TAG, context.getString(R.string.notRecording))
            return
        }
        Log.d(TAG, "Stop Recording 1")

        mediaRecorder?.apply {
            stop()
            reset()
            release()
        }
        mediaRecorder = null
        isRecording = false
        Log.d(TAG, context.getString(R.string.transcribing))
        onResult(context.getString(R.string.transcribing))

        transcribeAudio()
    }

    private suspend fun transcribeAudio(){

        try{
            val audioBytes = audioFile?.readBytes() ?: return
            Log.d(TAG, "Point 1 ${audioBytes.size}")

            val audio = RecognitionAudio.newBuilder()
                .setContent(com.google.protobuf.ByteString.copyFrom(audioBytes))
                .build()

            Log.d(TAG, "Point 2")

            // Configure the recognition request
            val config = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.AMR)
                .setSampleRateHertz(8000) // AMR-NB encoder has a sample rate of 8000 Hz
                .setLanguageCode("en-US")
                .build()

            Log.d(TAG, "Point 3")
            // Perform the transcription
            Log.d(TAG, "SpeechClient is Active ${speechClient.isTerminated}")
            val response = withContext(Dispatchers.IO) {
                speechClient.recognize(config, audio)
            }
            Log.d(TAG, "Point 3-1")
            val results: List<SpeechRecognitionResult> = response.resultsList
            var transcript = ""

            for (result in results) {
                // There can be several alternative transcripts for a given chunk of speech. Just use the
                // first (most likely) one here.
                val alternative: SpeechRecognitionAlternative = result.alternativesList.get(0)
                Log.d(TAG, "Transcription: ${alternative.getTranscript()}", )
                transcript+= alternative.getTranscript()
            }

            Log.d(TAG, "Point 3-1-1 ${transcript}")
            transcript = if( transcript!="" ) {transcript } else { "No transcript found."}

            Log.d(TAG, "Point 4")
            // Post the result back to the main UI thread
            withContext(Dispatchers.Main){
                onResult(transcript)
            }

        }  catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            withContext(Dispatchers.Main) {
                onResult("Transcription error: ${e.message}")
            }
        } finally {
            // Clean up the temporary audio file
            Log.d(TAG, "Point 5")
            audioFile?.delete()
        }
    }

}