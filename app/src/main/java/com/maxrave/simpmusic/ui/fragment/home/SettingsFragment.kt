package com.maxrave.simpmusic.ui.fragment.home

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.QUALITY
import com.maxrave.simpmusic.common.SUPPORTED_LOCATION
import com.maxrave.simpmusic.databinding.FragmentSettingsBinding
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@UnstableApi
@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<SettingsViewModel>()

    val backupLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            viewModel.backup(requireContext(), uri)
        }
    }
    val restoreLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.restore(requireContext(), uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        binding.topAppBarLayout.applyInsetter {
            type(statusBars = true){
                margin()
            }
        }
        return binding.root
    }


    @OptIn(ExperimentalCoilApi::class)
    @UnstableApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getLocation()
        viewModel.getQuality()
        viewModel.getPlayerCacheSize()
        viewModel.getDownloadedCacheSize()

        val diskCache = context?.imageLoader?.diskCache

        viewModel.location.observe(viewLifecycleOwner) {
            binding.tvContentCountry.text = it
        }
        viewModel.quality.observe(viewLifecycleOwner) {
            binding.tvQuality.text = it
        }
        viewModel.cacheSize.observe(viewLifecycleOwner) {
            binding.tvPlayerCache.text = getString(R.string.cache_size, bytesToMB(it).toString())
        }
        viewModel.downloadedCacheSize.observe(viewLifecycleOwner) {
            binding.tvDownloadedCache.text = getString(R.string.cache_size, bytesToMB(it).toString())
        }
        binding.tvThumbnailCache.text = getString(R.string.cache_size, if (diskCache?.size != null) {
            bytesToMB(diskCache.size)
        } else {
            0
        }.toString())

        binding.btVersion.setOnClickListener {
            val urlIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/maxrave-dev/SimpMusic")
            )
            startActivity(urlIntent)
        }

        binding.btGithub.setOnClickListener {
            val urlIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/maxrave-dev/")
            )
            startActivity(urlIntent)
        }
        binding.btDonate.setOnClickListener {
            val urlIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://paypal.me/maxraveofficial")
            )
            startActivity(urlIntent)
        }
        binding.btStoragePlayerCache.setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear Player Cache")
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("Clear") { dialog, _ ->
                    viewModel.clearPlayerCache()
                    viewModel.cacheSize.observe(viewLifecycleOwner) {
                        binding.tvPlayerCache.text = getString(R.string.cache_size, bytesToMB(it).toString())
                    }
                    dialog.dismiss()
                }
            dialog.show()
        }
        binding.btContentCountry.setOnClickListener {
            var checkedIndex = -1
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setSingleChoiceItems(SUPPORTED_LOCATION.items, -1) { _, which ->
                    checkedIndex = which
                }
                .setTitle("Content Country")
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("Change") { dialog, _ ->
                    if (checkedIndex != -1) {
                        viewModel.changeLocation(SUPPORTED_LOCATION.items[checkedIndex].toString())
                        viewModel.location.observe(viewLifecycleOwner) {
                            binding.tvContentCountry.text = it
                        }
                    }
                    dialog.dismiss()
                }
            dialog.show()
        }
        binding.btQuality.setOnClickListener {
            var checkedIndex = -1
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setSingleChoiceItems(QUALITY.items, -1) { _, which ->
                    checkedIndex = which
                }
                .setTitle("Quality")
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("Change") { dialog, _ ->
                    if (checkedIndex != -1) {
                        viewModel.changeQuality(checkedIndex)
                        viewModel.quality.observe(viewLifecycleOwner) {
                            binding.tvQuality.text = it
                        }
                    }
                    dialog.dismiss()
                }
            dialog.show()

        }

        binding.btStorageDownloadedCache.setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear Downloaded Cache")
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("Clear") { dialog, _ ->
                    viewModel.clearDownloadedCache()
                    viewModel.downloadedCacheSize.observe(viewLifecycleOwner) {
                        binding.tvPlayerCache.text = getString(R.string.cache_size, bytesToMB(it).toString())
                    }
                    dialog.dismiss()
                }
            dialog.show()
        }

        binding.btStorageThumbnailCache.setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear Thumbnail Cache")
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton("Clear") { dialog, _ ->
                    diskCache?.clear()
                    binding.tvThumbnailCache.text = getString(R.string.cache_size, if (diskCache?.size != null) {
                        bytesToMB(diskCache.size)
                    } else {
                        0
                    }.toString())
                    dialog.dismiss()
                }
            dialog.show()
        }


        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.btBackup.setOnClickListener {
            val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            backupLauncher.launch("${getString(R.string.app_name)}_${LocalDateTime.now().format(formatter)}.backup")
        }

        binding.btRestore.setOnClickListener {
            restoreLauncher.launch(arrayOf("application/octet-stream"))
        }
    }

    private fun bytesToMB(bytes: Long): Long {
        val mbInBytes = 1024 * 1024
        return bytes / mbInBytes
    }
}