package co.il.dronedistancetracker.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import co.il.dronedistancetracker.data.models.DroneTravel

@Dao
interface TravelDAO {


    //Insert method for inserting new run replaces a run if it already exists
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTravel(travel: DroneTravel)


    @Query("SELECT * FROM travel_table")
    fun getAllTravels(): LiveData<List<DroneTravel>>

}