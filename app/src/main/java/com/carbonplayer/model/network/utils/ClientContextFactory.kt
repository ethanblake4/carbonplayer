package com.carbonplayer.model.network.utils

import android.content.Context
import com.carbonplayer.CarbonPlayerApplication
import com.carbonplayer.model.entity.proto.context.ClientContextV1Proto
import com.carbonplayer.model.entity.proto.context.ClientContextV1Proto.Capability
import com.carbonplayer.model.entity.proto.context.ClientContextV1Proto.ClientContext
import com.carbonplayer.model.entity.proto.identifiers.CapabilityIdV1Proto.CapabilityId
import com.carbonplayer.model.entity.proto.identifiers.CapabilityIdV1Proto.CapabilityId.CapabilityType
import com.carbonplayer.utils.IdentityUtils
import java.util.*

object ClientContextFactory {
    
    fun create(context: Context) : ClientContext{
        val clientContext = ClientContext.newBuilder()
        clientContext.type = ClientContextV1Proto.ClientType.ANDROID
        clientContext.buildVersion = CarbonPlayerApplication.instance.googleBuildNumberLong
        clientContext.capabilitiesVersion = 2
        clientContext.deviceId = IdentityUtils.getGservicesId(context, false)
        clientContext.timezoneOffset = IdentityUtils.getTimezoneOffsetProtoDuration()
        clientContext.requestId = UUID.randomUUID().toString()
        clientContext.locale = IdentityUtils.localeCode()
        clientContext.addAllCapabilities(capabilities())
        clientContext.gmsCoreVersion = 11509238
        clientContext.phoneskyVersion = 80430500
        return clientContext.build()
    }
    
    private fun capabilities() : List<Capability> {
        return ArrayList<Capability>().apply {
            add(enableCapability(CapabilityType.INNERJAM_WIDE_PLAYABLE_CARD))
            add(enableCapability(CapabilityType.INNERJAM_TALL_PLAYABLE_CARD))
            add(enableCapability(CapabilityType.THUMBNAILED_MODULE_NOW_CARDS))
            add(enableCapability(CapabilityType.THRILLER_NOW_COLOR_BLOCKS))
            add(enableCapability(CapabilityType.THRILLER_USER_PLAYLISTS))
            add(enableCapability(CapabilityType.LOCAL_LIBRARY_PLAYLIST_PLAYABLE_ITEMS))
            add(enableCapability(CapabilityType.SERVER_RECENTS_MODULE))
            add(enableCapability(CapabilityType.TRACK_RADIO))
            add(enableCapability(CapabilityType.USER_LOCATION_HISTORY))
            add(enableCapability(CapabilityType.USER_LOCATION_REPORTING))
            add(supportCapability(CapabilityType.FINE_GRAINED_LOCATION_PERMISSION))
        }
    }

    private fun enableCapability(type: CapabilityType): Capability =
        makeCapability(type, Capability.CapabilityStatus.ENABLED)

    private fun supportCapability(type: CapabilityType): Capability =
            makeCapability(type, Capability.CapabilityStatus.SUPPORTED)

    private fun makeCapability(type: CapabilityType,
                               status: Capability.CapabilityStatus): Capability {
        return Capability.newBuilder()
                .setId(CapabilityId.newBuilder().setType(type).build())
                .setStatus(status).build()
    }
    
}