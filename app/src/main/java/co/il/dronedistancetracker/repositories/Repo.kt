package co.il.dronedistancetracker.repositories

import co.il.dronedistancetracker.data.dao.TravelDAO
import co.il.dronedistancetracker.data.models.DroneTravel
import javax.inject.Inject

class Repo @Inject constructor(
    val travelDoa: TravelDAO
) {

    suspend fun insertTravel(travel: DroneTravel) = travelDoa.insertTravel(travel)

    fun getAllTravels() = travelDoa.getAllTravels()
}