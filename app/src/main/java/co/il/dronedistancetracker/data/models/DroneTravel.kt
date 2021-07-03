package co.il.dronedistancetracker.data.models


import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "travel_table")
data class DroneTravel(
    var distance: Double,
    var timeToTravel: Double
) {

    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
}