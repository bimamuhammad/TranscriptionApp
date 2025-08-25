package com.example.android.transcriptionapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.android.transcriptionapp.databinding.FragmentFirstBinding
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechSettings
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MainScreen : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var speechClient: SpeechClient
    private lateinit var speechToText: SpeechToText
    private var isRecording =  false
    private val TAG = "MainScreen"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        speechClient = initializeSpeechClient()
        speechToText = SpeechToText(requireContext(), speechClient){
            result-> binding.editTextTextMultiLine.setText(result)
        }
        val micBtn = binding.micBtn
        micBtn.setOnClickListener{view:View ->

            if(!isRecording){
                speechToText.startRecording()
                binding.recordHint.text = requireContext().getText(R.string.tap_to_stop_recording)
            } else {
                lifecycleScope.launch {
                speechToText.stopRecording()
                binding.recordHint.text = requireContext().getText(R.string.tap_to_start_recording)
                }
            }
            isRecording = !isRecording
            view.isActivated = !view.isActivated
        }

        val saveBtn = binding.saveBtn
        saveBtn.setOnClickListener{_ -> saveText()}

        val shareButton = binding.shareBtn
        shareButton.setOnClickListener{_-> shareText()}
        return binding.root

    }

    private fun saveText(){
        val dialogView = LayoutInflater.from(context).inflate(R.layout.save_file_dialog, null)
        val text = binding.editTextTextMultiLine.text.toString().trim()
        val fileNameEditText = dialogView.findViewById<EditText>(R.id.filename_edit_text)
        displayToast(text)
        AlertDialog.Builder(requireContext())
            .setTitle("Save File")
            .setView(dialogView)
            .setPositiveButton("Save") {dialog, _ ->
                val filename = fileNameEditText.text.toString().trim()
                if(filename.isNotEmpty()){
                    val file = File(requireContext().filesDir, filename)
                    try {
                        FileOutputStream(file).use { it->
                            it.write(text.toByteArray())
                            displayToast("Success")
                        }
                    } catch (e: Exception){
                        e.printStackTrace()
                        displayToast("Failed to save file")
                    }

                }else{
                    displayToast("Filename cannot be empty")
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") {_,_ -> }
            .show()
    }

    private fun displayToast(message: String){
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    private fun shareText(){
       startActivity(getShareIntent())
    }

    private fun getShareIntent(): Intent {
        val text: Editable = binding.editTextTextMultiLine.text
        val sharedIntent: Intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)

        return sharedIntent
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spinner = binding.dropdown
        ArrayAdapter.createFromResource(this.requireContext(),
            R.array.languages,
            android.R.layout.simple_spinner_item
            ).also {
                arrayAdapter ->
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = arrayAdapter
        }


//        binding.buttonFirst.setOnClickListener {
//            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
//        }
    }

    override fun onDestroyView() {
        shutdownSpeechClient()
        super.onDestroyView()
        _binding = null
    }

    private fun initializeSpeechClient(): SpeechClient {
        // Run this in an appropriate lifecycle method (e.g., onCreate)
        val credentialsStream = requireContext().assets.open("translation-app-33286-e4764a02ebcb.json")
        val credentials = ServiceAccountCredentials.fromStream(credentialsStream)
        val credentialsProvider = FixedCredentialsProvider.create(credentials)

        val settings = SpeechSettings.newBuilder()
            .setCredentialsProvider(credentialsProvider)
            .build()

        // Initialise the client outside of the transcribe function
        return SpeechClient.create(settings)
    }

    private fun shutdownSpeechClient() {
        // Run this in an appropriate lifecycle method (e.g., onDestroy)
        speechClient.close()
        // Optional: Wait for graceful shutdown (better for long-running apps)
        // speechClient.awaitTermination(5, TimeUnit.SECONDS)
    }
}