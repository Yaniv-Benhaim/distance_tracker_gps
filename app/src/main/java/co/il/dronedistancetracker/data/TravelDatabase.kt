package co.il.dronedistancetracker.data

import androidx.room.Database
import androidx.room.RoomDatabase
import co.il.dronedistancetracker.data.dao.TravelDAO
import co.il.dronedistancetracker.data.models.DroneTravel


@Database(
    entities = [DroneTravel::class],
    version = 2
)


abstract class TravelDatabase : RoomDatabase() {

    abstract fun getTravelDao(): TravelDAO
}