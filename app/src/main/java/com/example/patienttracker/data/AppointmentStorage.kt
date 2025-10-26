package com.example.patienttracker.data

import android.content.Context
import com.example.patienttracker.ui.screens.patient.DoctorFull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object AppointmentStorage {
    private const val FILE_NAME = "PatientAppointments.json"

    fun saveAppointment(context: Context, doctor: DoctorFull, date: String, time: String) {
        val file = File(context.filesDir, FILE_NAME)
        val arr = if (file.exists()) JSONArray(file.readText()) else JSONArray()

        val obj = JSONObject().apply {
            put("doctorName", "Dr. ${doctor.firstName} ${doctor.lastName}")
            put("speciality", doctor.speciality)
            put("date", date)
            put("time", time)
        }

        arr.put(obj)
        file.writeText(arr.toString())
    }

    fun getAppointments(context: Context): List<Appointment> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()

        val arr = JSONArray(file.readText())
        return List(arr.length()) {
            val o = arr.getJSONObject(it)
            Appointment(
                o.getString("doctorName"),
                o.getString("speciality"),
                o.getString("date"),
                o.getString("time")
            )
        }
    }
}

data class Appointment(
    val doctorName: String,
    val speciality: String,
    val date: String,
    val time: String
)
