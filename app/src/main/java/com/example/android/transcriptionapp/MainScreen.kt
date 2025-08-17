package com.example.android.transcriptionapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.navigation.fragment.findNavController
import com.example.android.transcriptionapp.databinding.FragmentFirstBinding
import org.w3c.dom.Text

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MainScreen : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        val micBtn = binding.micBtn
        micBtn.setOnClickListener{view:View ->
            view.isActivated = !view.isActivated
        }

        val shareButton = binding.shareBtn
        shareButton.setOnClickListener{_-> shareText()}
        return binding.root

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
        super.onDestroyView()
        _binding = null
    }
}