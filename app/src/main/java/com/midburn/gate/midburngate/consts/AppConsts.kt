package com.midburn.gate.midburngate.consts

object IntentExtras {
    const val EVENTS_LIST = "EVENTS_LIST"
}
object AppConsts {

    const val TAG = "MIDBURN_GATE"

    const val SERVER_URL = "spark.midburn.org"
    const val FULL_SERVER_URL = "https://spark.midburn.org"
    @Suppress("unused")
    const val SERVER_STAGING_URL = "https://spark.staging.midburn.org"

    const val ACTION_SCAN = "com.google.zxing.client.android.SCAN"
    const val RESPONSE_OK = 200

    //audio
    const val OK_MUSIC = 1
    const val ERROR_MUSIC = 2

    //errors
    const val QUOTA_REACHED_ERROR = "QUOTA_REACHED"
    const val USER_OUTSIDE_EVENT_ERROR = "USER_OUTSIDE_EVENT"
    const val GATE_CODE_MISSING_ERROR = "GATE_CODE_MISSING"
    const val BAD_SEARCH_PARAMETERS_ERROR = "BAD_SEARCH_PARAMETERS"
    const val TICKET_NOT_FOUND_ERROR = "TICKET_NOT_FOUND"
    const val ALREADY_INSIDE_ERROR = "ALREADY_INSIDE"
    const val TICKET_NOT_IN_GROUP_ERROR = "TICKET_NOT_IN_GROUP"
    const val INTERNAL_ERROR = "Internal error: Cannot read property 'attributes' of null"

    //group types
    const val GROUP_TYPE_PRODUCTION = "prod_dep"
    const val GROUP_TYPE_ART = "art_installation"
    const val GROUP_TYPE_CAMP = "theme_camp"
}