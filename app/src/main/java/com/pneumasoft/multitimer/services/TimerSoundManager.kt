package com.pneumasoft.multitimer.services

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class TimerSoundManager(private val context: Context, private val scope: CoroutineScope) {

    private val alarmJobs = ConcurrentHashMap<String, Job>()
    private val activeRingtones = ConcurrentHashMap<String, Ringtone>()

    // Test support properties
    val isPlaying: Boolean
        get() = activeRingtones.values.any { it.isPlaying }

    val isLooping: Boolean
        get() = alarmJobs.isNotEmpty()

    fun startAlarmLoop(timerId: String) {
        // Biztonság kedvéért állítsuk le, ha már futna ehhez az ID-hoz
        stopAlarmLoop(timerId)

        alarmJobs[timerId] = scope.launch {
            // 1. Előkészítjük a hangot
            val ringtone = createRingtone()

            // Ha sikerült létrehozni, elmentjük a referenciát
            if (ringtone != null) {
                activeRingtones[timerId] = ringtone
            }

            // 2. Loop logika
            // Max 5 percig (10 * 30mp) zaklatjuk a felhasználót, vagy amíg le nem állítják
            repeat(10) {
                if (!isActive) return@launch

                try {
                    // Csak akkor indítjuk el, ha még nem szól vagy újra akarjuk indítani
                    if (ringtone?.isPlaying == false) {
                        ringtone.play()
                    }
                    // Fontos: Az Alarm hangok általában maguktól loopolnak!
                    // Ezért itt nem indítunk újat, csak várunk.
                    // Ha a rendszer hangja rövid (pl. egy csippanás) és nem loopol,
                    // akkor ez a check a következő körben újraindítja.
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Várunk 30 másodpercet (vagy a hang hosszát, ha tudnánk)
                delay(30_000)
            }

            // Ha lejárt a loop, takarítunk
            stopAlarmLoop(timerId)
        }
    }

    fun stopAlarmLoop(timerId: String) {
        // Job törlése (ez megállítja a coroutine-t)
        alarmJobs[timerId]?.cancel()
        alarmJobs.remove(timerId)

        // Hang leállítása
        try {
            activeRingtones[timerId]?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        activeRingtones.remove(timerId)
    }

    fun stopAll() {
        alarmJobs.keys.forEach { stopAlarmLoop(it) }
    }

    private fun createRingtone(): Ringtone? {
        return try {
            val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, alarmSound)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            ringtone
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
