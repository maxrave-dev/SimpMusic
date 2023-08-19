package com.maxrave.simpmusic.ui.fragment.home

import android.app.Activity
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.PIPED_INSTANCE
import com.maxrave.simpmusic.common.QUALITY
import com.maxrave.simpmusic.common.SUPPORTED_LANGUAGE
import com.maxrave.simpmusic.common.SUPPORTED_LOCATION
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
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

    override fun onResume() {
        super.onResume()
        viewModel.getLoggedIn()
        viewModel.loggedIn.observe(viewLifecycleOwner) {
            if (it == DataStoreManager.TRUE) {
                binding.tvLogInTitle.text = getString(R.string.log_out)
                binding.tvLogIn.text = getString(R.string.logged_in)
            } else if (it == DataStoreManager.FALSE) {
                binding.tvLogInTitle.text = getString(R.string.log_in)
                binding.tvLogIn.text = getString(R.string.log_in_to_get_personally_data)
            }
        }
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK)
        {

        }
    }

    @OptIn(ExperimentalCoilApi::class)
    @UnstableApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getLocation()
        viewModel.getLanguage()
        viewModel.getQuality()
        viewModel.getPlayerCacheSize()
        viewModel.getDownloadedCacheSize()
        viewModel.getLoggedIn()
        viewModel.getNormalizeVolume()
        viewModel.getPipedInstance()

        val diskCache = context?.imageLoader?.diskCache

        viewModel.loggedIn.observe(viewLifecycleOwner) {
            if (it == DataStoreManager.TRUE) {
                binding.tvLogInTitle.text = getString(R.string.log_out)
                binding.tvLogIn.text = getString(R.string.logged_in)
            } else if (it == DataStoreManager.FALSE) {
                binding.tvLogInTitle.text = getString(R.string.log_in)
                binding.tvLogIn.text = getString(R.string.log_in_to_get_personally_data)
            }
        }
        viewModel.location.observe(viewLifecycleOwner) {
            binding.tvContentCountry.text = it
        }
        viewModel.language.observe(viewLifecycleOwner) {
            if (it != null) {
                val temp = SUPPORTED_LANGUAGE.items[SUPPORTED_LANGUAGE.codes.indexOf(it)]
                binding.tvLanguage.text = temp
            }
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

        viewModel.normalizeVolume.observe(viewLifecycleOwner){
            binding.swNormalizeVolume.isChecked = it == DataStoreManager.TRUE
        }
        viewModel.pipedInstance.observe(viewLifecycleOwner) {
            binding.tvPipedInstance.text = it
        }

        binding.btVersion.setOnClickListener {
            val urlIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/maxrave-dev/SimpMusic")
            )
            startActivity(urlIntent)
        }

        binding.btLogin.setOnClickListener {
            if (viewModel.loggedIn.value == DataStoreManager.TRUE) {
                viewModel.clearCookie()
                Toast.makeText(requireContext(), getString(R.string.logged_out), Toast.LENGTH_SHORT).show()
            }
            else if (viewModel.loggedIn.value == DataStoreManager.FALSE) {
                findNavController().navigate(R.id.action_global_logInFragment)
            }
        }

        binding.btEqualizer.setOnClickListener {
            val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
            resultLauncher.launch(eqIntent)
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
                .setTitle(getString(R.string.clear_player_cache))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.clear)) { dialog, _ ->
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
                .setTitle(getString(R.string.content_country))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.change)) { dialog, _ ->
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
        binding.btPipedInstance.setOnClickListener {
            var checkedIndex = -1
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setSingleChoiceItems(PIPED_INSTANCE.listPiped, -1) { _, which ->
                    checkedIndex = which
                }
                .setTitle(requireContext().getString(R.string.streaming_data_provider_piped))
                .setNegativeButton(requireContext().getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(requireContext().getString(R.string.change)) { dialog, _ ->
                    if (checkedIndex != -1) {
                        viewModel.setPipedInstance(PIPED_INSTANCE.listPiped[checkedIndex].toString())
                        viewModel.pipedInstance.observe(viewLifecycleOwner) {
                            binding.tvPipedInstance.text = it
                        }
                    }
                    dialog.dismiss()
                }
            dialog.show()
        }
        binding.btLanguage.setOnClickListener {
            var checkedIndex = -1
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setSingleChoiceItems(SUPPORTED_LANGUAGE.items, -1) { _, which ->
                    checkedIndex = which
                }
                .setTitle(getString(R.string.language))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.change)) { dialog, _ ->
                    if (checkedIndex != -1) {
                        viewModel.changeLanguage(SUPPORTED_LANGUAGE.codes[checkedIndex])
                        viewModel.language.observe(viewLifecycleOwner) {
                            if (it != null) {
                                val temp = SUPPORTED_LANGUAGE.items[SUPPORTED_LANGUAGE.codes.indexOf(it)]
                                binding.tvLanguage.text = temp
                                val localeList = LocaleListCompat.forLanguageTags(it)
                                AppCompatDelegate.setApplicationLocales(localeList)
                            }
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
                .setTitle(getString(R.string.quality))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.change)) { dialog, _ ->
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
                .setTitle(getString(R.string.clear_downloaded_cache))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.clear)) { dialog, _ ->
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
                .setTitle(getString(R.string.clear_thumbnail_cache))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.clear)) { dialog, _ ->
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
        binding.swNormalizeVolume.setOnCheckedChangeListener { compoundButton, checked ->
            if (checked) {
                viewModel.setNormalizeVolume(true)
            } else {
                viewModel.setNormalizeVolume(false)
            }
        }
    }

    private fun bytesToMB(bytes: Long): Long {
        val mbInBytes = 1024 * 1024
        return bytes / mbInBytes
    }
}