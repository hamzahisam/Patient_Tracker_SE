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

    fun readAccounts(context: Context): List<DoctorAccount> {
        return try {
            val inputStream = context.assets.open("DoctorAccounts.csv")
            val lines = inputStream.bufferedReader().readLines().drop(1)
            lines.mapNotNull { line ->
                val parts = line.split(Regex("[,\t]"))
                if (parts.size == 6)
                    DoctorAccount(
                        parts[0].trim(),
                        parts[1].trim(),
                        parts[2].trim(),
                        parts[3].trim(),
                        parts[4].trim(),
                        parts[5].trim()
                    )
                else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun validateLogin(context: Context, id: String, password: String): DoctorAccount? {
        val accounts = readAccounts(context)
        return accounts.find { it.id == id && it.password == password }
    }
}
