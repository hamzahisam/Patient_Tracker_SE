package com.example.patienttracker.data

import android.content.Context
import java.io.File

data class PatientAccount(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val password: String
)

object PatientAccountStorage {

    private const val FILE_NAME = "PatientAccountDetails.csv"

    // Get file handle
    private fun getFile(context: Context): File {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) {
            file.writeText("PatientID,FirstName,LastName,Email,Phone,Password\n")
        }
        return file
    }

    // Read all accounts
//    fun readAccounts(context: Context): List<PatientAccount> {
//        val file = getFile(context)
//        val lines = file.readLines().drop(1)
//        return lines.mapNotNull { line ->
//            val parts = line.split(",")
//            if (parts.size == 6) {
//                PatientAccount(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5])
//            } else null
//        }
//    }

    // Check if email or phone already exists
//    fun isDuplicate(context: Context, email: String, phone: String): Boolean {
//        val accounts = readAccounts(context)
//        return accounts.any { it.email.equals(email, true) || it.phone == phone }
//    }

    // Add a new account and assign next ID
//    fun addAccount(context: Context, first: String, last: String, email: String, phone: String, password: String): String {
//        val file = getFile(context)
//        val accounts = readAccounts(context)
//
//        val nextId = String.format("%06d", accounts.size + 1)
//        val newAccount = PatientAccount(nextId, first, last, email, phone, password)
//
//        file.appendText("${newAccount.id},${newAccount.firstName},${newAccount.lastName},${newAccount.email},${newAccount.phone},${newAccount.password}\n")
//
//        return nextId
//    }

    // Validate login credentials
//    fun validateLogin(context: Context, id: String, password: String): PatientAccount? {
//        val accounts = readAccounts(context)
//        return accounts.find { it.id == id && it.password == password }
//    }
}
