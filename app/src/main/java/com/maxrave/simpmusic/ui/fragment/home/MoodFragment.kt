package com.maxrave.simpmusic.ui.fragment.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.Scaffold
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.maxrave.simpmusic.ui.screen.home.MoodScreen
import com.maxrave.simpmusic.ui.theme.AppTheme
import com.maxrave.simpmusic.viewModel.MoodViewModel

class MoodFragment : Fragment() {
    private val viewModel by viewModels<MoodViewModel>()

    private lateinit var composeView: ComposeView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
//        _binding = MoodMomentDialogBinding.inflate(inflater, container, false)
//        binding.topAppBarLayout.applyInsetter {
//            type(statusBars = true) {
//                margin()
//            }
//        }
//        return binding.root
        return ComposeView(requireContext()).also {
            composeView = it
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
//        binding.contentLayout.visibility = View.GONE
//        binding.loadingLayout.visibility = View.VISIBLE
//        moodList = ArrayList()
//        moodItemAdapter = MoodItemAdapter(arrayListOf(), requireContext(), findNavController())
//        val linearLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
//        binding.rvListPlaylist.apply {
//            adapter = moodItemAdapter
//            layoutManager = linearLayoutManager
//        }
//        val params = requireArguments().getString("params")
//        if (viewModel.moodsMomentObject.value != null){
//            binding.contentLayout.visibility = View.VISIBLE
//            binding.loadingLayout.visibility = View.GONE
//            moodList.addAll(viewModel.moodsMomentObject.value?.data?.items as ArrayList<Item>)
//            moodItemAdapter.updateData(moodList)
//        }
//        else{
//            fetchData(params)
//        }
//
//        binding.topAppBar.setNavigationOnClickListener {
//            findNavController().popBackStack()
//        }
        val params = requireArguments().getString("params")

        composeView.apply {
            setContent {
                AppTheme {
                    Scaffold {
                        MoodScreen(findNavController(), viewModel, params)
                    }
                }
            }
        }
    }

//    private fun fetchData(params: String?) {
//        if (params != null) {
//            viewModel.getMood(params)
//        }
//        viewModel.moodsMomentObject.observe(viewLifecycleOwner, Observer { response ->
//            when (response){
//                is Resource.Success ->
//                    response.data.let {
//                        binding.topAppBar.title = it?.header
//                        moodList.addAll(it?.items as ArrayList<Item>)
//                        moodItemAdapter.updateData(moodList)
//                        binding.contentLayout.visibility = View.VISIBLE
//                        binding.loadingLayout.visibility = View.GONE
//                    }
//                is Resource.Error -> {
//                    binding.contentLayout.visibility = View.GONE
//                    binding.loadingLayout.visibility = View.GONE
//                    Snackbar.make(binding.root, response.message.toString(), Snackbar.LENGTH_LONG).show()
//                    findNavController().popBackStack()
//                }
//            }
//        })
//    }
}