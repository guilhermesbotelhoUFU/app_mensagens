package com.example.app_mensagem.data

import android.annotation.SuppressLint
import android.content.Context
import android.provider.ContactsContract
import com.example.app_mensagem.data.model.DeviceContact

class DeviceContactsRepository {

    @SuppressLint("Range")
    fun fetchDeviceContacts(context: Context): List<DeviceContact> {
        val contacts = mutableListOf<DeviceContact>()
        val contentResolver = context.contentResolver
        // Query para buscar contatos que têm pelo menos um número de telefone
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val number = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                contacts.add(DeviceContact(name, number))
            }
        }
        // Remove contatos duplicados com o mesmo número, mantendo apenas o primeiro nome encontrado
        return contacts.distinctBy { it.phoneNumber }
    }
}