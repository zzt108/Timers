package com.pneumasoft.multitimer.services

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class TimerSoundManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    // Tároljuk a Job-ot és a Ringtone példányt is
    private val alarmJobs = ConcurrentHashMap<String, Job>()
    private val activeRingtones = ConcurrentHashMap<String, Ringtone>()

    fun startAlarmLoop(timerId: String) {
        // Biztonság kedvéért állítsuk le, ha már futna ehhez az ID-hoz
        stopAlarmLoop(timerId)

        alarmJobs[timerId] = scope.launch {
            // 1. Előkészítjük a hangot
            val ringtone = createRingtone()

            // Ha sikerült létrehozni, elmentjük a referenciát
            if (ringtone != null) {
                activeRingtones[timerId] = ringtone

                // 2. Loop logika
                repeat(10) { // Max 5 percig zaklat
                    if (!isActive) return@launch

                    try {
                        // Csak akkor indítjuk el, ha még nem szól (vagy újra akarjuk indítani)
                        if (!ringtone.isPlaying) {
                            ringtone.play()
                        }

                        // Fontos: Az Alarm hangok általában maguktól loopolnak!
                        // Ezért itt nem indítunk újat, csak várunk.
                        // Ha a rendszer hangja rövid (pl. egy csippanás) és nem loopol,
                        // akkor ez a check a következő körben újraindítja.
                        // Ha hosszú zene, akkor hagyjuk szólni.
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // 30 másodperc várakozás a következő ellenőrzésig
                    delay(30000)
                }

                // Ha lejár a 10 kör, állítsuk le
                stopAlarmLoop(timerId)
            }
        }
    }

    fun stopAlarmLoop(timerId: String) {
        // 1. Job törlése (ne induljon újra)
        alarmJobs[timerId]?.cancel()
        alarmJobs.remove(timerId)

        // 2. Hang explicit leállítása (CRITICAL FIX)
        activeRingtones[timerId]?.let { ringtone ->
            try {
                if (ringtone.isPlaying) {
                    ringtone.stop()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        activeRingtones.remove(timerId)
    }

    fun stopAll() {
        // Minden Job törlése
        alarmJobs.values.forEach { it.cancel() }
        alarmJobs.clear()

        // Minden hang leállítása
        activeRingtones.values.forEach { ringtone ->
            try {
                if (ringtone.isPlaying) ringtone.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        activeRingtones.clear()
    }

    private fun createRingtone(): Ringtone? {
        return try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, uri)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.isLooping = true // Android 9+ explicit loop kérés
            }

            // Audio attribútumok beállítása, hogy biztosan Alarm hangerőn szóljon
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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
