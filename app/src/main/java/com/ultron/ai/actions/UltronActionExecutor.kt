package com.ultron.ai.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import com.ultron.ai.domain.CommandResult
import com.ultron.ai.domain.ContactItem

class UltronActionExecutor(private val context: Context) {

    fun launchApp(packageName: String, appName: String): CommandResult {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName)
        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            CommandResult.Success("Opening $appName.", appName)
        } else {
            CommandResult.Failure("I couldn't find $appName installed on your device.")
        }
    }

    fun searchYouTube(query: String): CommandResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")).apply {
                setPackage("com.google.android.youtube")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandResult.Success("Searching YouTube for $query.", "YouTube Search")
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            CommandResult.Success("Opening YouTube search in browser.", "YouTube Web Search")
        }
    }

    fun searchMapsOrNavigate(location: String, isNavigation: Boolean): CommandResult {
        val uriStr = if (isNavigation) {
            "google.navigation:q=${Uri.encode(location)}"
        } else {
            "geo:0,0?q=${Uri.encode(location)}"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            val action = if (isNavigation) "Navigating to $location" else "Searching Maps for $location"
            CommandResult.Success("$action.", "Maps Action")
        } catch (e: Exception) {
            CommandResult.Failure("Google Maps is not available.")
        }
    }

    fun makePhoneCall(nameOrNumber: String): CommandResult {
        val contacts = findContactsByName(nameOrNumber)
        return when {
            contacts.size == 1 -> {
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${contacts[0].phoneNumber}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                    CommandResult.Success("Calling ${contacts[0].displayName}.", "Phone Call")
                } catch (e: SecurityException) {
                    CommandResult.Failure("Phone permission missing.", android.Manifest.permission.CALL_PHONE)
                }
            }
            contacts.size > 1 -> {
                CommandResult.AmbiguousContact(nameOrNumber, contacts)
            }
            else -> {
                CommandResult.Failure("I couldn't find $nameOrNumber in your saved contacts.")
            }
        }
    }

    fun searchWeb(query: String): CommandResult {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        CommandResult.Success("Searching the web for $query.", "Web Search")
    }

    fun openSystemCamera(): CommandResult {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            CommandResult.Success("Opening Camera.", "Camera")
        } catch (e: Exception) {
            CommandResult.Failure("Unable to open camera.")
        }
    }

    private fun findContactsByName(name: String): List<ContactItem> {
        val list = mutableListOf<ContactItem>()
        val cr = context.contentResolver
        val cursor = cr.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            null
        )
        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val id = if (idIndex != -1) it.getString(idIndex) else ""
                val dispName = if (nameIndex != -1) it.getString(nameIndex) else ""
                val phone = if (numberIndex != -1) it.getString(numberIndex) else ""
                list.add(ContactItem(id, dispName, phone))
            }
        }
        return list.distinctBy { it.phoneNumber }
    }
}
