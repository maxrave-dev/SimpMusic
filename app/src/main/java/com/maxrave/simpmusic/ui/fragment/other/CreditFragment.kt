package com.maxrave.simpmusic.ui.fragment.other

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.findNavController
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.databinding.FragmentCreditBinding
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.Insetter

@AndroidEntryPoint
class CreditFragment : Fragment() {

    private var _binding: FragmentCreditBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreditBinding.inflate(inflater, container, false)
        Insetter.builder().margin(
            WindowInsetsCompat.Type.statusBars()
        ).applyToView(binding.topAppBarLayout)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btGithub.setOnClickListener {
            val urlIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/maxrave-dev/SimpMusic")
            )
            startActivity(urlIntent)
        }
        binding.btIssue.setOnClickListener {
            val urlIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/maxrave-dev/SimpMusic/issues")
            )
            startActivity(urlIntent)
        }
        binding.btBuyMeACoffee.setOnClickListener {
            val urlIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.buymeacoffee.com/maxrave")
            )
            startActivity(urlIntent)
        }
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }
}