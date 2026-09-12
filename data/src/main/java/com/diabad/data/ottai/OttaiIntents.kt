package com.diabad.data.ottai

/**
 * OtTai → AAPS share protocol (same as AndroidAPS Syai/Ottai worker).
 */
object OttaiIntents {
    const val ACTION_OTTAI_INTL = "info.nightscout.androidaps.action.OTTAI_APP"
    const val ACTION_OTTAI_CN = "cn.diyaps.sharing.OT_APP"

    const val EXTRA_COLLECTION = "collection"
    const val EXTRA_DATA = "data"

    const val COLLECTION_ENTRIES = "entries"
}
