package co.il.dronedistancetracker.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import co.il.dronedistancetracker.R
import co.il.dronedistancetracker.databinding.FragmentHistoryBinding
import co.il.dronedistancetracker.ui.adapters.HistoryAdapter
import co.il.dronedistancetracker.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_history.*

@AndroidEntryPoint
class HistoryFragment : Fragment(R.layout.fragment_history) {

    private val mainViewModel by viewModels<MainViewModel>()
    lateinit var historyAdapter: HistoryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() = rvTravelHistory.apply {
        mainViewModel.allTravels.observe(viewLifecycleOwner, {
            historyAdapter = HistoryAdapter(it)
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(requireContext())
        })

    }
}