package com.example.patienttracker.data

import android.content.Context

data class DoctorAccount(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val password: String
)

object DoctorAccountStorage {

//    fun readAccounts(context: Context): List<DoctorAccount> {
//        return try {
//            val inputStream = context.assets.open("DoctorAccounts.csv")
//            val lines = inputStream.bufferedReader().readLines().drop(1)
//            lines.mapNotNull { line ->
//                // Split by commas, but handle quoted fields properly
//                val parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
//                    .map { it.trim().replace("\"", "") }
//
//                // Ensure at least 6 fields (we only use the first 6)
//                if (parts.size >= 6) {
//                    DoctorAccount(
//                        id = parts[0],
//                        firstName = parts[1],
//                        lastName = parts[2],
//                        email = parts[3],
//                        phone = parts[4],
//                        password = parts[5]
//                    )
//                } else null
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            emptyList()
//        }
//    }

//    fun validateLogin(context: Context, id: String, password: String): DoctorAccount? {
//        val accounts = readAccounts(context)
//        return accounts.find { it.id == id && it.password == password }
//    }
}
