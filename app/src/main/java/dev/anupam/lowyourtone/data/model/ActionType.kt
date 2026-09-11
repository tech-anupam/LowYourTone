package dev.anupam.lowyourtone.data.model

enum class ActionType(val label: String) {
    CALL_CONTACT("Call Someone"),
    CALL_EMERGENCY("Emergency Call"),
    SEND_SMS("Send Text"),
    START_AUDIO_RECORD("Record Audio"),
    START_VIDEO_RECORD("Record Video"),
    SEND_LOCATION_SMS("Send My Location"),
    OPEN_APP("Open App"),
    TOGGLE_FLASHLIGHT("Flashlight"),
    PLAY_ALARM_SOUND("Sound Alarm"),
    SEND_WHATSAPP("WhatsApp Message"),
    CUSTOM_INTENT("Custom Action"),
    LOCK_SCREEN("Lock Phone"),
    TOGGLE_SILENT("Silent Mode"),
    TOGGLE_DND("Do Not Disturb"),
    MAX_VOLUME("Max Volume"),
    STOP_MEDIA("Stop Everything"),
    DISCO_FLASH("Disco Flash")
}
