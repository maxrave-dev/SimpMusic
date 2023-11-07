package com.maxrave.simpmusic.ui.fragment.home

import android.app.usage.StorageStatsManager
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Bundle
import android.os.storage.StorageManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.common.LIMIT_CACHE_SIZE
import com.maxrave.simpmusic.common.LYRICS_PROVIDER
import com.maxrave.simpmusic.common.QUALITY
import com.maxrave.simpmusic.common.SPONSOR_BLOCK
import com.maxrave.simpmusic.common.SUPPORTED_LANGUAGE
import com.maxrave.simpmusic.common.SUPPORTED_LOCATION
import com.maxrave.simpmusic.data.dataStore.DataStoreManager
import com.maxrave.simpmusic.databinding.FragmentSettingsBinding
import com.maxrave.simpmusic.extension.navigateSafe
import com.maxrave.simpmusic.extension.setEnabledAll
import com.maxrave.simpmusic.viewModel.SettingsViewModel
import com.maxrave.simpmusic.viewModel.SharedViewModel
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Scanner


@UnstableApi
@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<SettingsViewModel>()
    private val sharedViewModel by activityViewModels<SharedViewModel>()

    private val backupLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            viewModel.backup(requireContext(), uri)
        }
    }
    private val restoreLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
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

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    @OptIn(ExperimentalCoilApi::class)
    @UnstableApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getLocation()
        viewModel.getLanguage()
        viewModel.getQuality()
        viewModel.getPlayerCacheSize()
        viewModel.getDownloadedCacheSize()
        viewModel.getPlayerCacheLimit()
        viewModel.getLoggedIn()
        viewModel.getNormalizeVolume()
        viewModel.getSkipSilent()
        viewModel.getSavedPlaybackState()
        viewModel.getSendBackToGoogle()
        viewModel.getSaveRecentSongAndQueue()
        viewModel.getLastCheckForUpdate()
        viewModel.getSponsorBlockEnabled()
        viewModel.getSponsorBlockCategories()
        viewModel.getTranslationLanguage() //
        viewModel.getLyricsProvider() //
        viewModel.getUseTranslation() //
        viewModel.getMusixmatchLoggedIn() //

        val diskCache = context?.imageLoader?.diskCache

        viewModel.loggedIn.observe(viewLifecycleOwner) {
            if (it == DataStoreManager.TRUE) {
                binding.tvLogInTitle.text = getString(R.string.log_out)
                binding.tvLogIn.text = getString(R.string.logged_in)
                setEnabledAll(binding.swSaveHistory, true)
            } else if (it == DataStoreManager.FALSE) {
                binding.tvLogInTitle.text = getString(R.string.log_in)
                binding.tvLogIn.text = getString(R.string.log_in_to_get_personally_data)
                setEnabledAll(binding.swSaveHistory, false)
            }
        }
        viewModel.musixmatchLoggedIn.observe(viewLifecycleOwner) {
            if (it == DataStoreManager.TRUE) {
                binding.tvMusixmatchLoginTitle.text = getString(R.string.log_out_from_musixmatch)
                binding.tvMusixmatchLogin.text = getString(R.string.logged_in)
                setEnabledAll(binding.swUseMusixmatchTranslation, true)
                setEnabledAll(binding.btTranslationLanguage, true)
            } else if (it == DataStoreManager.FALSE) {
                binding.tvMusixmatchLoginTitle.text = getString(R.string.log_in_to_Musixmatch)
                binding.tvMusixmatchLogin.text = getString(R.string.only_support_email_and_password_type)
                setEnabledAll(binding.swUseMusixmatchTranslation, false)
                setEnabledAll(binding.btTranslationLanguage, false)
            }
        }
        viewModel.mainLyricsProvider.observe(viewLifecycleOwner) {
            if (it == DataStoreManager.YOUTUBE) {
                binding.tvMainLyricsProvider.text = LYRICS_PROVIDER.items.get(1)
            } else if (it == DataStoreManager.MUSIXMATCH) {
                binding.tvMainLyricsProvider.text = LYRICS_PROVIDER.items.get(0)
            }
        }
        viewModel.translationLanguage.observe(viewLifecycleOwner) {
            binding.tvTranslationLanguage.text = it
        }
        viewModel.useTranslation.observe(viewLifecycleOwner) {
            binding.swUseMusixmatchTranslation.isChecked = it == DataStoreManager.TRUE
        }
        viewModel.sendBackToGoogle.observe(viewLifecycleOwner) {
            binding.swSaveHistory.isChecked = it == DataStoreManager.TRUE
        }
        viewModel.location.observe(viewLifecycleOwner) {
            binding.tvContentCountry.text = it
        }
        viewModel.language.observe(viewLifecycleOwner) {
            if (it != null) {
                val temp = SUPPORTED_LANGUAGE.items.getOrNull(SUPPORTED_LANGUAGE.codes.indexOf(it))
                binding.tvLanguage.text = temp
            }
        }
        viewModel.quality.observe(viewLifecycleOwner) {
            binding.tvQuality.text = it
        }
        viewModel.cacheSize.observe(viewLifecycleOwner) {
            drawDataStat()
            binding.tvPlayerCache.text = getString(R.string.cache_size, bytesToMB(it).toString())
        }
        viewModel.downloadedCacheSize.observe(viewLifecycleOwner) {
            drawDataStat()
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
        viewModel.skipSilent.observe(viewLifecycleOwner){
            binding.swSkipSilent.isChecked = it == DataStoreManager.TRUE
        }
        viewModel.savedPlaybackState.observe(viewLifecycleOwner){
            binding.swSavePlaybackState.isChecked = it == DataStoreManager.TRUE
        }
        viewModel.saveRecentSongAndQueue.observe(viewLifecycleOwner) {
            binding.swSaveLastPlayed.isChecked = it == DataStoreManager.TRUE
        }
        viewModel.sponsorBlockEnabled.observe(viewLifecycleOwner) {
            binding.swEnableSponsorBlock.isChecked = it == DataStoreManager.TRUE
        }
        viewModel.lastCheckForUpdate.observe(viewLifecycleOwner) {
            binding.tvCheckForUpdate.text = getString(R.string.last_checked_at, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(it.toLong())))
        }
        viewModel.playerCacheLimit.observe(viewLifecycleOwner) {
            binding.tvLimitPlayerCache.text = if (it != -1) "$it MB" else getString(R.string.unlimited)
        }
        binding.btLimitPlayerCache.setOnClickListener {
            var checkedIndex = -1
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setSingleChoiceItems(LIMIT_CACHE_SIZE.items, -1) { _, which ->
                    checkedIndex = which
                }
                .setTitle(getString(R.string.limit_player_cache))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.change)) { dialog, _ ->
                    if (checkedIndex != -1) {
                        viewModel.setPlayerCacheLimit(LIMIT_CACHE_SIZE.data[checkedIndex])
                        viewModel.playerCacheLimit.observe(viewLifecycleOwner) {
                            binding.tvLimitPlayerCache.text = if (it != -1) "$it MB" else getString(R.string.unlimited)
                            Toast.makeText(requireContext(), getString(R.string.restart_app), Toast.LENGTH_SHORT).show()
                        }
                    }
                    dialog.dismiss()
                }
            dialog.show()
        }
        binding.btCheckForUpdate.setOnClickListener {
            binding.tvCheckForUpdate.text = getString(R.string.checking)
            viewModel.checkForUpdate()
            viewModel.githubResponse.observe(viewLifecycleOwner) {response ->
                if (response != null) {
                    if (response.tagName != getString(R.string.version_name)) {
                        binding.tvCheckForUpdate.text = getString(R.string.last_checked_at, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.ofEpochMilli(System.currentTimeMillis())))
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
                        val formatted = response.publishedAt?.let { input ->
                            inputFormat.parse(input)
                                ?.let { outputFormat.format(it) }
                        }
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.update_available))
                            .setMessage(getString(R.string.update_message, response.tagName, formatted, response.body))
                            .setPositiveButton(getString(R.string.download)) { _, _ ->
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(response.assets?.firstOrNull()?.browserDownloadUrl))
                                startActivity(browserIntent)
                            }
                            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                    else {
                        Toast.makeText(requireContext(), getString(R.string.no_update), Toast.LENGTH_SHORT).show()
                        viewModel.getLastCheckForUpdate()
                        viewModel.lastCheckForUpdate.observe(viewLifecycleOwner) {
                            binding.tvCheckForUpdate.text = getString(R.string.last_checked_at, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                .withZone(ZoneId.systemDefault())
                                .format(Instant.ofEpochMilli(it.toLong())))
                        }
                    }
                }
            }
        }

        binding.btVersion.setOnClickListener {
            findNavController().navigateSafe(R.id.action_global_creditFragment)
        }

        binding.btLogin.setOnClickListener {
            if (viewModel.loggedIn.value == DataStoreManager.TRUE) {
                viewModel.clearCookie()
                Toast.makeText(requireContext(), getString(R.string.logged_out), Toast.LENGTH_SHORT).show()
            }
            else if (viewModel.loggedIn.value == DataStoreManager.FALSE) {
                findNavController().navigateSafe(R.id.action_global_logInFragment)
            }
        }
        
        binding.btMusixmatchLogin.setOnClickListener {
            if (viewModel.musixmatchLoggedIn.value == DataStoreManager.TRUE) {
                viewModel.clearMusixmatchCookie()
                Toast.makeText(requireContext(), getString(R.string.logged_out), Toast.LENGTH_SHORT).show()
            }
            else if (viewModel.musixmatchLoggedIn.value == DataStoreManager.FALSE) {
                findNavController().navigateSafe(R.id.action_global_musixmatchFragment)
            }
        }

        binding.btEqualizer.setOnClickListener {
            val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
            eqIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, requireContext().packageName)
            eqIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sharedViewModel.simpleMediaServiceHandler?.player?.audioSessionId)
            eqIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            val packageManager = requireContext().packageManager
            val resolveInfo: List<*> = packageManager.queryIntentActivities(eqIntent, 0)
            Log.d("EQ", resolveInfo.toString())
            if (resolveInfo.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.no_equalizer), Toast.LENGTH_SHORT).show()
            }
            else{
                resultLauncher.launch(eqIntent)
            }
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
                        drawDataStat()
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
                        val alertDialog = MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.warning)
                            .setMessage(R.string.change_language_warning)
                            .setNegativeButton(getString(R.string.cancel)) { d, _ ->
                                d.dismiss()
                                dialog.dismiss()
                            }
                            .setPositiveButton(getString(R.string.change)) { d, _ ->
                                viewModel.changeLanguage(SUPPORTED_LANGUAGE.codes[checkedIndex])
                                viewModel.language.observe(viewLifecycleOwner) {
                                    if (it != null) {
                                        val temp = SUPPORTED_LANGUAGE.items[SUPPORTED_LANGUAGE.codes.indexOf(it)]
                                        binding.tvLanguage.text = temp
                                        val localeList = LocaleListCompat.forLanguageTags(it)
                                        sharedViewModel.activityRecreate()
                                        AppCompatDelegate.setApplicationLocales(localeList)
                                    }
                                }
                                d.dismiss()
                                dialog.dismiss()
                            }
                        alertDialog.show()
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
        binding.btMainLyricsProvider.setOnClickListener {
            var checkedIndex = -1
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.main_lyrics_provider))
                .setSingleChoiceItems(LYRICS_PROVIDER.items, -1) { _, which ->
                    checkedIndex = which
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setPositiveButton(getString(R.string.change)) { dialog, _ ->
                    if (checkedIndex != -1) {
                        if (checkedIndex == 0) {
                            viewModel.setLyricsProvider(DataStoreManager.MUSIXMATCH)
                            binding.tvMainLyricsProvider.text = DataStoreManager.MUSIXMATCH
                        } else if (checkedIndex == 1){
                            viewModel.setLyricsProvider(DataStoreManager.YOUTUBE)
                            binding.tvMainLyricsProvider.text = DataStoreManager.YOUTUBE
                        }
                    }
                    viewModel.getLyricsProvider()
                    viewModel.mainLyricsProvider.observe(viewLifecycleOwner) {
                        if (it == DataStoreManager.YOUTUBE) {
                            binding.tvMainLyricsProvider.text = LYRICS_PROVIDER.items.get(1)
                        } else if (it == DataStoreManager.MUSIXMATCH) {
                            binding.tvMainLyricsProvider.text = LYRICS_PROVIDER.items.get(0)
                        }
                    }
                    dialog.dismiss()
                }
            dialog.show()
        }

        binding.btTranslationLanguage.setOnClickListener{
            val materialAlertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
            materialAlertDialogBuilder.setTitle(getString(R.string.translation_language))
            materialAlertDialogBuilder.setMessage(getString(R.string.translation_language_message))
            val editText = EditText(requireContext())
            materialAlertDialogBuilder.setView(editText)
            materialAlertDialogBuilder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            materialAlertDialogBuilder.setPositiveButton(getString(R.string.change)) { dialog, _ ->
                if (editText.text.toString().isNotEmpty()) {
                    if (editText.text.toString().length == 2) {
                        viewModel.setTranslationLanguage(editText.text.toString())
                        viewModel.translationLanguage.observe(viewLifecycleOwner) {
                            binding.tvTranslationLanguage.text = it
                        }
                    }
                    else {
                        Toast.makeText(requireContext(), getString(R.string.invalid_language_code), Toast.LENGTH_SHORT).show()
                    }
                }
                else {
                    if (viewModel.language.value != null && viewModel.language.value!!.length >= 2) {
                        viewModel.language.value?.slice(0..1)
                            ?.let { it1 -> viewModel.setTranslationLanguage(it1) }
                    }
                }
                dialog.dismiss()
            }
            materialAlertDialogBuilder.show()
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
                        drawDataStat()
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
        binding.btCategoriesSponsorBlock.setOnClickListener {
            Log.d("Check category", viewModel.sponsorBlockCategories.value.toString())
            val selectedItem: ArrayList<String> = arrayListOf()
            val item: Array<CharSequence> = Array(9) {i ->
                getString(SPONSOR_BLOCK.listName[i])
            }

            val checked = BooleanArray(9) { i ->
                if (!viewModel.sponsorBlockCategories.value.isNullOrEmpty()) {
                    viewModel.sponsorBlockCategories.value!!.contains(SPONSOR_BLOCK.list[i].toString())
                }
                else {
                    false
                }
            }

            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Category")
                .setMultiChoiceItems(item, checked) { _, i, b ->
                    if (b) {
                        if (!selectedItem.contains(SPONSOR_BLOCK.list[i].toString())) {
                            selectedItem.add(SPONSOR_BLOCK.list[i].toString())
                        }
                    } else {
                        if (selectedItem.contains(SPONSOR_BLOCK.list[i].toString())) {
                            selectedItem.remove(SPONSOR_BLOCK.list[i].toString())
                        }
                    }
                }
                .setPositiveButton(getString(R.string.save)) { dialog, _ ->
                    viewModel.setSponsorBlockCategories(selectedItem)
                    Log.d("Check category", selectedItem.toString())
                    viewModel.getSponsorBlockCategories()
                    viewModel.sponsorBlockCategories.observe(viewLifecycleOwner) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.saved),
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    }
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
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
        binding.swNormalizeVolume.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.setNormalizeVolume(true)
            } else {
                viewModel.setNormalizeVolume(false)
            }
        }
        binding.swSkipSilent.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.setSkipSilent(true)
            } else {
                viewModel.setSkipSilent(false)
            }
        }
        binding.swSavePlaybackState.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.setSavedPlaybackState(true)
            } else {
                viewModel.setSavedPlaybackState(false)
            }
        }
        binding.swSaveLastPlayed.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.setSaveLastPlayed(true)
            } else {
                viewModel.setSaveLastPlayed(false)
            }
        }
        binding.swEnableSponsorBlock.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.setSponsorBlockEnabled(true)
            } else {
                viewModel.setSponsorBlockEnabled(false)
            }
        }
        binding.swSaveHistory.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.setSendBackToGoogle(true)
            } else {
                viewModel.setSendBackToGoogle(false)
            }
        }
        binding.swUseMusixmatchTranslation.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                viewModel.setUseTranslation(true)
            } else {
                viewModel.setUseTranslation(false)
            }
        }
        binding.bt3rdPartyLibraries.setOnClickListener {

            val inputStream = requireContext().resources.openRawResource(R.raw.aboutlibraries)
            val scanner = Scanner(inputStream).useDelimiter("\\A")
            val stringBuilder = StringBuilder()
            while (scanner.hasNextLine()) {
                stringBuilder.append(scanner.nextLine())
            }
            Log.w("AboutLibraries", stringBuilder.toString())
            val localLib = Libs.Builder().withJson(stringBuilder.toString()).build()
            val intent = LibsBuilder()
                .withLicenseShown(true)
                .withVersionShown(true)
                .withActivityTitle(getString(R.string.third_party_libraries))
                .withSearchEnabled(true)
                .withEdgeToEdge(true)
                .withLibs(
                    localLib
                )
                .intent(requireContext())
            startActivity(intent)
        }
    }
    private fun browseFiles(dir: File): Long {
        var dirSize: Long = 0
        if (!dir.listFiles().isNullOrEmpty()) {
            for (f in dir.listFiles()!!) {
                dirSize += f.length()
                if (f.isDirectory) {
                    dirSize += browseFiles(f)
                }
            }
        }
        return dirSize
    }
    private fun drawDataStat() {
        val mStorageStatsManager = getSystemService(requireContext(), StorageStatsManager::class.java)
        if (mStorageStatsManager != null) {
            lifecycleScope.launch {
                val totalByte = mStorageStatsManager.getTotalBytes(StorageManager.UUID_DEFAULT)
                val freeSpace = mStorageStatsManager.getFreeBytes(StorageManager.UUID_DEFAULT)
                val usedSpace = totalByte - freeSpace
                val simpMusicSize = browseFiles(requireContext().filesDir)
                val otherApp = simpMusicSize.let { usedSpace.minus(it) }
                val databaseSize = simpMusicSize - viewModel.playerCache.cacheSpace - viewModel.downloadCache.cacheSpace
                if (totalByte == freeSpace + otherApp + databaseSize + viewModel.playerCache.cacheSpace + viewModel.downloadCache.cacheSpace) {
                    (binding.flexBox.getChildAt(0).layoutParams as FlexboxLayout.LayoutParams).flexBasisPercent = otherApp.toFloat().div(totalByte.toFloat())
                    (binding.flexBox.getChildAt(1).layoutParams as FlexboxLayout.LayoutParams).flexBasisPercent = viewModel.downloadCache.cacheSpace.toFloat().div(totalByte.toFloat())
                    (binding.flexBox.getChildAt(2).layoutParams as FlexboxLayout.LayoutParams).flexBasisPercent = viewModel.playerCache.cacheSpace.toFloat().div(totalByte.toFloat())
                    (binding.flexBox.getChildAt(3).layoutParams as FlexboxLayout.LayoutParams).flexBasisPercent = databaseSize.toFloat().div(totalByte.toFloat())
                    (binding.flexBox.getChildAt(4).layoutParams as FlexboxLayout.LayoutParams).flexBasisPercent = freeSpace.toFloat().div(totalByte.toFloat())
                }
            }
        }
    }

    private fun bytesToMB(bytes: Long): Long {
        val mbInBytes = 1024 * 1024
        return bytes / mbInBytes
    }
}