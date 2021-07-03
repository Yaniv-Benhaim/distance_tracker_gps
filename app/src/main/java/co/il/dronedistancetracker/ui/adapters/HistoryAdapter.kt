package co.il.dronedistancetracker.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import co.il.dronedistancetracker.R
import co.il.dronedistancetracker.data.models.DroneTravel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.item_travel.view.*
import java.text.SimpleDateFormat
import java.util.*


class HistoryAdapter(private val travels: List<DroneTravel>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        return HistoryViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_travel,
                parent,
                false
            )
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val travel = travels[position]
        holder.itemView.apply {
            tvDistance.text = "DISTANCE FLOWN: ${travel.distance}"
            tvDistance.text = "TIME FLOWN: ${travel.timeToTravel}"
        }
    }

    override fun getItemCount(): Int {
        return travels.size
    }
}