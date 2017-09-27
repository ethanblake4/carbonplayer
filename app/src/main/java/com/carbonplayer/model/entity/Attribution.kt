package com.carbonplayer.model.entity

import io.realm.RealmObject
import org.json.JSONObject

open class Attribution(
        var kind: String = "",
        var licenseUrl: String? = null,
        var licenseTitle: String? = null,
        var sourceTitle: String? = null,
        var sourceUrl: String? = null
) : RealmObject() {

    constructor(json: JSONObject) : this(
            json.getString("kind"),
            "license_url".let { if (json.has(it)) json.getString(it) else null },
            "license_title".let { if (json.has(it)) json.getString(it) else null },
            "source_title".let { if (json.has(it)) json.getString(it) else null },
            "source_url".let { if (json.has(it)) json.getString(it) else null }
    )
}